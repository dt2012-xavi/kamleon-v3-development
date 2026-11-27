# Handoff para la web: eventos Mixpanel de la app Android

**Fecha:** 2026-11-27
**Para:** el agente/equipo de `web-react-kamleon-v3` que hará el trabajo del dashboard (§8 del spec).
**Qué tienes que hacer:** la lista de tareas está en §6. Todo lo de antes es la referencia de lo que envía la app, ya acordada con la web, y no hace falta pedir nada más a Android para empezar.
**Contrato de partida:** `docs/superpowers/specs/2026-11-26-mixpanel-android-app-events-design.md` (repo web).
**Implementación Android:** `app/src/main/java/com/dynatech2012/kamleonuserapp/analytics/` y `docs/runbooks/analytics.md` (repo Android, rama `development`).

## 1. Estado

- La app Android envía eventos según el contrato desde la versión **1.1.0 (versionCode 2029+)**, ya en `development`.
- Los intentos de acceso (`Auth Flow *` y `flowInstanceId`, §4.4) se añadieron el 2026-11-27 con los criterios acordados con la web. Llegan desde el siguiente build de dev; un build anterior envía los eventos de acceso sin `flowInstanceId`.
- El dashboard **todavía no los lee**: todas las consultas exigen `platform === "web"`. No hay riesgo de que inflen las cifras web. Cuando se haga el §8, esto es lo que hay que saber.
- No he comprobado en Mixpanel Live view que los eventos lleguen. Lo primero que conviene hacer es abrir el proyecto **dev** → Events y confirmar las cinco super properties (§6).

## 2. Conexión por entorno

| Flavor Android | Proyecto Mixpanel | `environment` |
|---|---|---|
| `development` | el de `.env.development` | `dev` |
| `preProduction` | el de `.env.staging` (pre y staging comparten proyecto) | `pre` |
| `prod` | el de `.env.production` | `prod` |
| `internal` | ninguno: no envía nada | — |

Región EU, sin IP (`ip=0`) y sin eventos automáticos del SDK: **solo llegan los eventos con nombre de la lista de abajo.** En el proyecto compartido de pre/staging, la app manda `environment: pre`; la regla de entorno del dashboard tiene que aceptar `pre` además de `staging`.

## 3. Lo que es igual que en la web

- **Identidad:** `distinct_id` = uid de Firebase.
  - `identify` tras un login correcto y al crear la cuenta.
  - `reset()` al cerrar sesión, y se vuelven a registrar las super properties.
  - Las cuentas `@devices.kamleon.com` no envían nada.
- **Super properties:**
  - `platform: app_android`;
  - `appVersion` (`X.Y.Z` estricto, sin sufijo `-dev`/`-pre`);
  - `appVersionCode` (entero);
  - `definitionVersion: 1`;
  - `environment`.
- **Por evento:** `localHour` y `localWeekday` (lunes = 0).
- **Perfil (`people.set`):** `userTypes`, `userStatus`, `languageID` y `lastLoginAt`.
  - `lastLoginAt` va en ISO UTC con milisegundos y solo se escribe en el login, no en el registro ni al restaurar sesión.
- **Acceso:** los 14 eventos del §5.5 con los nombres de `lib/analytics/events.ts`.
  - `errorCode` usa los códigos web (`auth/wrong-password`, `auth/network-request-failed`…). Los códigos de Firebase Android (`ERROR_WRONG_PASSWORD`) se traducen antes de enviar.
  - `errorLabel` usa las etiquetas de `error-labels.ts`.
  - Lo desconocido va como `unknown`. Nunca se envía el mensaje del error.
- **Invitaciones:** `Invitation Accepted` / `Declined` con `invitationID`, `invitedRole`, `target*ID` si existen, `timeSinceSentMs` y `viaEmailLink: false`. Nunca el email.
- **`attribution: personal`** en todos los eventos de función (`Page Viewed`, `Drawer *`, `QR Flow *`, `Invitation *`).

## 4. Lo que el contrato dejaba a Android, ya decidido

### 4.1 `Page Viewed`

| Pantalla | `section` | `view` | `feature` | Extra |
|---|---|---|---|---|
| Home | `home` | — | `home` | |
| Analytics | `my-data` | `overview` | `my-data` | |
| Gráfica de una métrica | `my-data` | `metric-detail` | `my-data` | `metric` + acción de valor `my-data:consult` (`isValueAction`, `valueActionFeature`, `valueActionType`) |
| Escáner QR | `qr-scan` | — | `qr-scan` | |
| Ajustes | `account` | `profile` | `account` | |

- **`metric`** es una lista cerrada: `hydration` · `electrolytes` · `volume`.
- **`isFeatureEntry`** vale `true` al entrar en la función desde otra, y `false` al moverse dentro de ella (de `overview` a `metric-detail`).
- **Volver a la misma pantalla** (otro toque en su pestaña) no envía nada.
- **Pantallas de acceso:** no envían `Page Viewed`, igual que en la web.
- **`navSource` no se envía.** El campo es opcional en el contrato, así que la app no tiene que inventarlo.

### 4.2 Panel de notificaciones

- Al abrirse se envía `Drawer Opened`, y al cerrarse `Drawer Closed`, ambos con `drawer: notifications` y `feature: notifications`.
- `Drawer Closed` lleva además `outcome`, `attempts` y `durationMs`:
  - `outcome` es `completed` si se aceptó, rechazó o descartó alguna invitación, y `cancelled` si no;
  - `attempts` es el número de acciones;
  - `invitationsAccepted` e `invitationsDeclined` son enteros: invitaciones aceptadas y rechazadas mientras el panel estuvo abierto. Descartar una invitación caducada no suma en ninguno (sí en `attempts`). Se envían siempre, también con valor 0, y solo con `drawer: notifications`.
- **Para el dashboard:** `notifications` como `feature` debe existir en `FEATURES` si no existe ya.

### 4.3 Proceso QR

- **Eventos de un intento:** `QR Flow Started`, opcionalmente uno o varios `QR Flow Step`, y exactamente un `QR Flow Finished`.
- Todos llevan el mismo `flowInstanceId` (UUID) y `feature: qr-scan`.

| Momento | Evento |
|---|---|
| El escáner de la app empieza a leer | `QR Flow Started` · `entry: in-app` · `pendingSession: false` |
| La cámara del móvil abre un enlace `/scan` | `QR Flow Started` · `entry: native-camera` · `pendingSession` = no había sesión |
| Se lee un enlace Kamleon válido | `QR Flow Step` · `step: code-read` |
| Tras el login, continúa un intento que esperaba sesión | `QR Flow Step` · `step: signed-in` |
| Fin | `QR Flow Finished` · `outcome` · `stepsCompleted` · `durationMs` · `pendingSession` |

`entry` va en los tres eventos (`Started`, `Step` y `Finished`), así que cada resultado se puede separar por origen sin cruzar eventos.

- **`step`** es una lista cerrada: `code-read` · `signed-in`.
- **`outcome`:**
  - `success`: vinculado. Lleva la acción de valor `qr-scan:scan`.
  - `invalid`: enlace incompleto, o caducado mientras se iniciaba sesión.
  - `cancelled`: se salió del escáner sin leer un código.
  - `abandoned`: la app se cerró o el intento quedó sin terminar. Un intento de cámara pendiente de login sigue vivo 10 minutos; pasado ese tiempo es `abandoned`.
  - **`failed` (valor nuevo, no está en el contrato):** el código era válido pero no se pudo vincular, por falta de red o un error del servidor. Separarlo de `invalid` distingue "el QR estaba mal" de "nuestro sistema falló". **Hay que añadirlo a la lista cerrada de `outcome`**; si no, el dashboard lo descarta o lo manda a `other`.
- **Privacidad:** nunca se envía el contenido del QR, ni `unitId` ni `sessionId`.

### 4.4 Intentos de acceso

Port de `lib/analytics/auth-flow-tracker.ts` (en Android: `AuthFlowTracker.kt`). Mismos nombres de evento y mismas propiedades que la web:

| Evento | Propiedades |
|---|---|
| `Auth Flow Started` | `flow`, `flowInstanceId`, y en `signup` `viaInvitation: false` |
| `Auth Step Completed` | `flow`, `flowInstanceId`, `step`, `stepDurationMs` |
| `Auth Flow Completed` | `flow`, `flowInstanceId`, (`viaInvitation`), `durationMs`, `stepsCompleted`, `outcome` (`success` · `error`) |
| `Auth Flow Abandoned` | `flow`, `flowInstanceId`, (`viaInvitation`), `durationMs`, `stepsCompleted`, `lastStep` (`null` sin pasos), `exitType` (`closed` · `navigated` · `switched-flow`), `switchedTo` con `switched-flow` |

Ninguno lleva `hasAccount` ni `viaEmailLink`, ni tampoco `attribution`, igual que en la web.

**Flujos.** La app tiene tres; `verify-email` y la contraseña nueva ocurren en la web, que los cuenta.

| `flow` | Empieza | Pasos (`step`) | `success` | `error` |
|---|---|---|---|---|
| `signin` | al mostrarse el login a alguien sin sesión | — | en `Login Succeeded` | en `Login Blocked Unverified Email` |
| `signup` | al mostrarse el registro | `account` al crearse la cuenta, `profile` al guardar el cuestionario | junto con `profile` (equivale a `check-inbox`) | — |
| `forgot-password` | al mostrarse «¿Has olvidado la contraseña?» del login | `request` al enviarse el enlace | junto con `request` | — |

- **Una contraseña incorrecta no cierra el intento:** los reintentos llevan el mismo `flowInstanceId`. `Login Failed` no termina nada.
- **En la app la cuenta se crea más tarde que en la web:** formulario → privacidad → fecha de nacimiento (aquí se crea, paso `account`) → cuestionario opcional (paso `profile`). Sin paso `consent`: un abandono antes de crear la cuenta queda con `lastStep: null`, como en la web.
- **Volver a pulsar** «Iniciar sesión» o «Enviar» tras un intento ya cerrado empieza otro.

**Eventos de acceso con `flowInstanceId`** (el del intento en curso, como `trackAuthEvent`): `Login Attempted`, `Login Failed`, `Login Blocked Unverified Email`, `Login Succeeded`, `Signup Completed`, `Signup Failed`, `Forgot Password Requested` y `Forgot Password Failed`. **Sin él**, como en la web: `Signup Started`, `Signup Step2 Viewed`, `Signup Profile Step Completed`, `Verification Email Sent`, `Verification Resend Clicked` y `Email Verified`.

**Abandonos: lo único que no es idéntico a la web.**
- `switched-flow`: ir a otro flujo (del login al registro, etc.).
- `navigated`: salir de las pantallas de acceso sin terminar (Atrás hasta salir de la app).
- `closed`: Android no avisa cuando se mata la app, así que el intento se guarda en el móvil y **se envía al siguiente arranque**. Consecuencias para el dashboard:
  - la **hora del evento es la del arranque**, no la del abandono (puede caer otro día); `durationMs` y `stepsCompleted` sí son los del intento mientras se usó;
  - solo se envía si el intento estuvo activo en los **últimos 30 minutos**. Uno más antiguo se descarta, y uno parado más de 30 minutos en segundo plano también, sin enviar nada;
  - por tanto la **tasa de abandono de la app es una cota inferior**: hay intentos que no terminan en `Completed` ni en `Abandoned`.
- Mandar la app a segundo plano menos de 30 minutos (por ejemplo, para ir al email) no abandona el intento.

**Para comparar con la web:** en la app la sesión de Firebase se mantiene entre arranques y una sesión restaurada no abre un `signin`. Los ratios (éxito/intento, abandono, tiempo hasta el éxito) son comparables; los logins por persona no.

## 5. Diferencias que afectan al dashboard

1. **Intentos de acceso:** la app los envía como la web, salvo el abandono `closed` (§4.4): llega con la hora del siguiente arranque y solo si el intento tiene 30 minutos o menos parado.
2. **`Forgot Password Requested` / `Failed` ya no se envían desde Ajustes** (corrección acordada con la web): solo salen de la recuperación del login.
3. **`Forgot Password Failed`** lleva solo `errorCode`, como la web. Los demás `*Failed` llevan `errorCode` y `errorLabel`.
4. **`Signup Completed`** lleva `consentGiven: true` y `viaInvitation: false`. **`Email Verified`** lleva `viaInvitation: false`. La app no tiene registro por invitación.
5. **`flowInstanceId` también lo llevan los `QR Flow *`** (§4.3), con otro significado. Las consultas de acceso tienen que filtrar por los nombres de evento de acceso, no solo por la existencia de `flowInstanceId`.
6. **`Language Changed` no se envía.** La app sigue el idioma del sistema y no tiene selector propio.
7. **`Screen Time` no se envía** (fase 2 del contrato).
8. **No hay eventos de sistema** (medida recibida, notificación recibida), así que nada de la app infla "persona activa" sin uso real.

## 6. Checklist para el §8 del dashboard

- [ ] Parametrizar `platform` (`web` · `app_android` · `app_ios`) en `WEB`/JQL, `isOurWebEvent` y la regla de entorno, aceptando `pre`.
- [ ] Filtro de plataforma en la barra de filtros.
- [ ] Taxonomía:
  - secciones `home`, `qr-scan` y `account`;
  - vistas `overview` y `metric-detail` en `my-data`, y `profile` en `account`;
  - funciones `home`, `qr-scan` y `notifications` (si falta) en `FEATURES` y `PERSONAL_FEATURES`, con regla `everyone`;
  - `metric` ∈ `hydration` · `electrolytes` · `volume`;
  - etiquetas en/es/cat de todo lo nuevo.
- [ ] Invitaciones: contar las respuestas de la app con `invitationsAccepted`/`invitationsDeclined` de `Drawer Closed` (§4.2).
- [ ] Acciones de valor: `my-data:consult` y `qr-scan:scan` (ya vienen marcadas en el evento con `isValueAction`).
- [ ] Proceso QR nuevo:
  - `outcome` ∈ `success` · `invalid` · `cancelled` · `abandoned` · **`failed`**;
  - `step` ∈ `code-read` · `signed-in`;
  - `entry` ∈ `in-app` · `native-camera`.
- [ ] Acceso (§4.4):
  - leer los `Auth Flow *` y los eventos de acceso de `app_android` con la misma lógica que los de la web, agrupando por `flowInstanceId` **y por plataforma**;
  - flujos de la app: `signin`, `signup`, `forgot-password` (no habrá `verify-email`, `invitation-accept` ni `workspace-select` de `app_android`);
  - un `Auth Flow Abandoned` con `exitType: closed` se cuenta en el intento aunque su hora sea posterior (no usar su hora como la del abandono);
  - un intento sin `Completed` ni `Abandoned` no es un error: es un abandono no medido;
  - mostrar entre plataformas solo ratios (éxito/intento, abandono, tiempo hasta el éxito), nunca logins por persona, y avisar de que el abandono de la app es una cota inferior.
- [ ] Recuperación: los `Forgot Password *` de la app solo salen del login; no hay que excluir nada de Ajustes.
- [ ] Decidir si "perfil activo" cuenta una vez por persona o por plataforma.
- [ ] Subir la versión de caché de los resúmenes afectados.

## 7. Cómo comprobarlo

1. Instalar un build `development` de la app (1.1.0 o superior) e iniciar sesión con una cuenta de pruebas que no sea `@devices.kamleon.com`.
2. En Mixpanel, proyecto **dev** → Events (en vivo), filtrar por `platform = app_android`.
3. Recorrer Home → Analytics → una gráfica → QR → Ajustes, y abrir y cerrar las notificaciones.
4. Comprobar que llegan los `Page Viewed` de §4.1, los `Drawer *` y un `QR Flow Started`/`Finished` (salir del escáner da `cancelled`), cada uno con las cinco super properties más `localHour` y `localWeekday`.
5. Cerrar sesión y volver a entrar: tiene que aparecer `Auth Flow Started` → `Login Attempted` → `Login Succeeded` → `Auth Flow Completed` (`flow: signin`, `outcome: success`), todos con el mismo `flowInstanceId`, el mismo `distinct_id` (el uid) y el perfil con `lastLoginAt`.
6. Con una contraseña incorrecta antes del login correcto: el `Login Failed` lleva el mismo `flowInstanceId` que el resto del intento.
7. Desde el login, ir a «Crear cuenta»: `Auth Flow Abandoned` (`flow: signin`, `exitType: switched-flow`, `switchedTo: signup`) y un `Auth Flow Started` nuevo con `flow: signup`.

Si algo no cuadra con el contrato, el cambio se hace en Android: los nombres y valores salen de `AnalyticsEvents.kt`, `AnalyticsProps.kt`, `ScreenTracker.kt`, `QrFlowTracker.kt` y `AuthFlowTracker.kt`, con tests en `app/src/test/.../AnalyticsTest.kt` y `AuthFlowTrackerTest.kt`.
