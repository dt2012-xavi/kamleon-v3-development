# Analítica (Mixpanel) en la app Android

Implementa el contrato de la web `docs/superpowers/specs/2026-11-26-mixpanel-android-app-events-design.md` (repo `web-react-kamleon-v3`). Aquí solo está lo que la app decide por su cuenta y lo que la web necesita saber.

## Conexión

| Flavor | Proyecto (token de la web) | `environment` |
|---|---|---|
| `development` | `.env.development` | `dev` |
| `preProduction` | `.env.staging` (pre y staging comparten proyecto) | `pre` |
| `prod` | `.env.production` | `prod` |
| `internal` | sin token: no envía nada | `int` |

- Región EU (`https://api-eu.mixpanel.com`), sin IP (`ip=0`) y sin eventos automáticos del SDK.
- Super properties: `platform: app_android`, `appVersion` (`X.Y.Z` sin sufijo), `appVersionCode`, `definitionVersion: 1`, `environment`. Cada evento añade `localHour` y `localWeekday` (lunes = 0).
- Identidad: uid de Firebase. `identify` tras un login correcto (con `lastLoginAt` en el perfil) y al crear la cuenta (sin `lastLoginAt`). Una sesión que Firebase restaura se identifica al cargar el perfil, sin contar como login. Al cerrar sesión: `reset()` y se vuelven a registrar las super properties.
- Perfil: `userTypes`, `userStatus`, `languageID`, `lastLoginAt`. Las cuentas `@devices.kamleon.com` no envían nada.

## Pantallas (`Page Viewed`)

| Pantalla | `section` | `view` | `feature` |
|---|---|---|---|
| Home | `home` | — | `home` |
| Analytics | `my-data` | `overview` | `my-data` |
| Gráfica de una métrica | `my-data` | `metric-detail` | `my-data` |
| Escáner QR | `qr-scan` | — | `qr-scan` |
| Ajustes | `account` | `profile` | `account` |

- `metric` ∈ `hydration` · `electrolytes` · `volume`.
- Entrar en `metric-detail` lleva `isValueAction: true`, `valueActionFeature: my-data` y `valueActionType: consult`, como hace la web.
- No se envía `navSource`. Volver a la misma pantalla (otro toque en su pestaña) no envía nada.
- Panel de notificaciones: `Drawer Opened` y `Drawer Closed` con `drawer: notifications`. `outcome` es `completed` si se aceptó, rechazó o descartó alguna invitación, y `cancelled` si no; también lleva `attempts` y `durationMs`, y los contadores enteros `invitationsAccepted` e `invitationsDeclined` (invitaciones aceptadas y rechazadas con el panel abierto; descartar una caducada no suma en ninguno). Se envían siempre, también con valor 0, y solo con `drawer: notifications`.

## Proceso QR

`QR Flow Started` y un único `QR Flow Finished` por intento, con el mismo `flowInstanceId`.

| Momento | Evento |
|---|---|
| El escáner de la app empieza a leer | `QR Flow Started` (`entry: in-app`, `pendingSession: false`) |
| La cámara del móvil abre un enlace `/scan` | `QR Flow Started` (`entry: native-camera`, `pendingSession` = no había sesión) |
| Se lee un enlace Kamleon válido | `QR Flow Step` (`step: code-read`) |
| Tras el login, continúa un intento que esperaba sesión | `QR Flow Step` (`step: signed-in`) |
| Fin | `QR Flow Finished` (`outcome`, `stepsCompleted`, `durationMs`, `pendingSession`) |

Valores de `outcome`:
- `success`: vinculado. Lleva la acción de valor `qr-scan:scan`.
- `invalid`: enlace incompleto, o caducado mientras se iniciaba sesión.
- `cancelled`: se salió del escáner sin leer un código.
- `abandoned`: la app se cerró, o el intento quedó sin terminar.
- `failed` (**valor nuevo, no está en el contrato**): el código era válido pero no se pudo vincular (sin red o error del servidor).

Un intento pendiente de login se guarda en el móvil y es el mismo intento cuando continúa. Al arrancar la app, un intento del proceso anterior se da por `abandoned`, salvo uno de cámara de menos de 10 minutos, que aún puede continuar.

## Acceso

Son los 14 eventos de la web, con sus mismas propiedades (`errorCode` y `errorLabel` con los códigos `auth/...` de la web). Los intentos se miden como en la web (`lib/analytics/auth-flow-tracker.ts`), con `AuthFlowTracker`. Criterios acordados con la web el 2026-11-27:

- Eventos: `Auth Flow Started`, `Auth Step Completed`, `Auth Flow Completed` (`outcome`: `success` · `error`) y `Auth Flow Abandoned` (`exitType`: `closed` · `navigated` · `switched-flow`, con `switchedTo`), todos con `flow` y un `flowInstanceId` (UUID).
- Flujos de la app: `signin`, `signup` y `forgot-password`. `verify-email` y la contraseña nueva ocurren en la web, que los cuenta.
- `signin` empieza al mostrarse el login a alguien sin sesión. Una contraseña incorrecta no cierra el intento. Se cierra con `success` en `Login Succeeded` y con `error` en `Login Blocked Unverified Email`.
- `signup` empieza al mostrarse el registro (lleva `viaInvitation: false`). Paso `account` al crearse la cuenta (fecha de nacimiento), paso `profile` al guardar el cuestionario, y `success` en ese momento (equivale a `check-inbox`). Sin paso `consent`: un abandono antes de crear la cuenta queda con `lastStep: null`.
- `forgot-password` empieza al mostrarse «¿Has olvidado la contraseña?» del login. Paso `request` y `success` al enviarse el enlace. Desde Ajustes no se envía ningún evento de recuperación.
- Llevan el `flowInstanceId` del intento en curso (`Analytics.trackAuth`): `Login Attempted`, `Login Failed`, `Login Blocked Unverified Email`, `Login Succeeded`, `Signup Completed`, `Signup Failed`, `Forgot Password Requested` y `Forgot Password Failed`. Van sin él, como en la web: `Signup Started`, `Signup Step2 Viewed`, `Signup Profile Step Completed`, `Verification Email Sent`, `Verification Resend Clicked` y `Email Verified`.
- Otro flujo abandona el que estaba en curso (`switched-flow`). Salir de las pantallas de acceso sin terminar (Atrás hasta salir de la app) es `navigated`.
- Android no avisa cuando se mata la app: el intento se guarda en el móvil y al siguiente arranque se envía como `closed`, con la duración mientras se usó. Solo si estuvo activo en los últimos 30 minutos. Uno parado más de 30 minutos (también en segundo plano) se descarta sin enviar nada, así que la tasa de abandono de la app es una cota inferior.

## Comprobar

En un build `development`, abrir el proyecto dev de Mixpanel → Events (en vivo) y comprobar que cada evento lleva las cinco super properties. Un login correcto da `Auth Flow Started` → `Login Attempted` → `Login Succeeded` → `Auth Flow Completed`, con el mismo `flowInstanceId`. Para ver el envío en el móvil, activar temporalmente `setEnableLogging(true)` en `MixpanelSender` y filtrar Logcat por `MixpanelAPI`. No hay que commitear ese cambio.
