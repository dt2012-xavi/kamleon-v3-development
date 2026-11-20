# Handoff: arranque, capa de red, sesión y foto de perfil (estado a 2026-11-20)

Para un agente con contexto nuevo. Rama `codex/gradle-modernization`, worktree
`/Users/xavi/.codex/worktrees/ea6d/android-java-kotlin-kamleon-userapp-development/KamleonApp`,
172 commits por delante de `main`, **nada subido** (sin push ni PR). Último commit de este trabajo:
`e9153f3`. El árbol solo tiene cambios ajenos sin commitear en `.idea/*`: no los incluyas.

Lee antes `AGENTS.md`. Este documento **resume y cierra** el trabajo de
`2026-11-17-initactivity-review.md` (ahí está el detalle histórico caso por caso, las instrucciones
de prueba en el dispositivo y los hallazgos de Tutorial y Ajustes). Diseño de la capa de red:
`docs/superpowers/specs/2026-11-19-network-layer-design.md` y su plan
`docs/superpowers/plans/2026-11-19-network-layer.md`. Códigos de error compartidos con iOS/web:
`docs/runbooks/error-codes.md`.

## 1. Qué se ha hecho

### 1.1 Arranque (`InitActivity`, `AuthViewModel`, `StartupRoute`)

La lista de casos del arranque está **cerrada**. `AuthViewModel.checkLogin()` decide un
`StartupState` y `StartupRoute` (JVM-testeable) lo traduce a una acción de pantalla:

| Estado | Qué ve el usuario |
|---|---|
| `Ready` | Abre Main (fundido). |
| `NeedsLogin` / `NeedsPolicy` / `NeedsProfile` / `NeedsVerification` | Login, política, registro o hoja de verificación de correo (con «ya verifiqué», reenviar y salir). |
| `RetryableError` | Hoja «sin conexión» con «Retry»; con cuenta completa en caché también «Continue offline». Reintenta solo al volver la red. |
| `ServerError` | «Something went wrong» con Retry y Log Out y `KL-AND-110`. |
| `SessionRejected` | «Your session has ended» con código; OK borra datos locales, cierra sesión y va a Login. |

Detalles que conviene saber: la comprobación va envuelta en `guardedStartup` (una excepción inesperada
ya no deja el splash eterno: caso 11); la cancelación de corrutinas se propaga y solo la comprobación
vigente (`request == startupCheck`) aplica estado; «Retry» llama de verdad aunque el monitor diga
offline (`trustMonitor = false`); sin red el arranque falla al instante en vez de esperar 10 s.
Otros puntos ya resueltos: enlaces `/scan` (caducan a los 10 min, se reutiliza `MainActivity`), permiso de
notificaciones explicado antes de pedirlo, restauración tras muerte de proceso, política mostrada de
inmediato.

### 1.2 Capa de red (`network/`)

- `NetworkMonitor` (sin exigir `VALIDATED`), `NetworkGate.safeCall` / `safeCallNullable`: **único punto**
  donde una llamada remota se convierte en `Response`/`ResponseNullable`, con monitor y tiempo límite
  (`CallKind.Read` 10 s, `Action` 15 s; una acción que agota el tiempo puede aplicarse después, por eso
  deben ser idempotentes). Todos los repositorios usan la pasarela.
- `FailureKind` (Network, Busy, SessionRejected, NotFound, Server, Unknown) y `failureKindOf`:
  **una sola definición** de qué significa un fallo; `isConnectionProblem()` es el criterio común de
  «revisa tu conexión» en todas las pantallas. Clasifica Firestore, Auth, Storage y, desde el último
  commit, `FirebaseFunctionsException`.
- `ActionResult` + `showActionFailedSheet` / `actionFailureText`: la misma hoja de fallo en selectores,
  Ajustes, QR, foto, contraseña y PIN.
- `ErrorCode` (`viewmodels/ErrorCode.kt`, 101–104 y 110) y `errorCodeOf`. Se guarda en Crashlytics como
  clave `error_code`.

### 1.3 Sesión terminada dentro de la app (nuevo hoy)

`NetworkGate` avisa a `SessionWatcher` (singleton, `network/SessionWatcher.kt`) cuando `endsSession(e)`:
`FirebaseAuthInvalidUserException`, Firestore `UNAUTHENTICATED`, Functions `UNAUTHENTICATED`, Storage
`NOT_AUTHENTICATED`. **Decisión deliberada:** `PERMISSION_DENIED` de una operación suelta NO cierra la
sesión (puede ser una regla de ese documento); en el arranque sí cuenta porque ahí es la lectura del
perfil. `MainActivity` observa `MainViewModel.sessionEnded`, muestra la hoja con el código y, al pulsar
OK, ejecuta `viewModel.logout()` (limpia datos, cierra sesión) y abre `InitActivity` con la pila vaciada.
Solo se guarda el primer aviso hasta `reset()` (que se llama al cerrar sesión y al acabar la comprobación
del arranque).

### 1.4 Foto de perfil

`ImageLoader` único de Coil por Hilt (`AppModule`), `MainViewModel.userImageUrl` (URL, no `Drawable`),
`ImageView.showProfileImage` / `ImageLoader.forgetCachedImages` en `views/ProfileImage.kt`. Cachés
vaciadas tras subir/quitar (la ruta de Storage es constante). Fallos de subida/borrado con hoja; borrar una
foto que ya no está en Storage no falla; el evento de cerrar la hoja lleva `Unit` (con `null` nunca se
entregaba). **Sin crossfade:** `CircleImageView` copia el drawable una vez y el de fundido aún está vacío.

### 1.5 Limpieza de logs y código muerto

`FirestoreDataSource` y `MeasuresRepository` ya usan `DebugLog` (solo debug; antes escribían recuentos y la
puntuación de hidratación en todos los builds). Borrados `getUserLastMeasure`, el `getUserMeasures`
paginado y el parámetro `limit` de `getUserMeasuresNoPag`: los `Query` de Firestore son inmutables y esas
llamadas no hacían nada.

## 2. Verificación del estado actual

Comando habitual (todo en verde a `e9153f3`):

```bash
./gradlew -q :app:assembleDevelopmentDebug :app:compileDevelopmentDebugAndroidTestKotlin \
  :app:testDevelopmentDebugUnitTest :app:lintDevelopmentDebug
```

129 tests unitarios (`app/src/test/java/com/kamleonapp/`, JVM) y 9 instrumentados en
`SessionRejectedTest` (excepciones reales de Firebase; los de Firestore/Functions/Storage no se pueden
construir en la JVM). Lint con baseline y `warningsAsErrors`.

**Visto en el Samsung `RZCX21APAGW`:** todos los estados del arranque (con fallos forzados temporales),
Continue offline, subir/cambiar/quitar foto, fallo sin red, hoja de sesión terminada en Main (`KL-AND-103`
y vuelta a Login), fundido Login→Main (continuo, sin parpadeo; no requiere cambios).

**NO verificado en dispositivo** (falta de sesión, red real o ganas de romper cuentas):
- Subida de QR por enlace `/scan` con la red cortada; medidas y analítica sin conexión; logout sin red.
- Hojas de fallo en selectores de datos/fecha y Ajustes.
- `reload()` real de verificación de correo.
- Arranque en frío sin red con foto de perfil (cubierto indirectamente por el flujo offline).
- Clasificación real de `StorageException` más allá de «no encontrado» y «sin red»; clasificación real de
  `FirebaseFunctionsException` (solo tests con excepciones construidas).
- Sesión terminada en Main provocada por un fallo real (se forzó con una mutación temporal ya revertida).

## 3. Cómo probar en el dispositivo

Samsung `RZCX21APAGW`, flavor `development`, paquete `com.dynatech2012.kamleonuserapp`. **No cierres
sesión ni crees cuentas sin pedirlo; no escribas contraseñas** (el usuario inicia sesión a mano). El
dispositivo se quedó **sin sesión** tras la prueba de la hoja de sesión terminada.
`connectedDevelopmentDebugAndroidTest` desinstala la app y con ella la sesión.

Técnica que funcionó: **mutación temporal** marcada con `// MUTATION` (forzar un fallo, un aviso o abrir
Main), instalar, observar con `adb exec-out screencap`, y revertir con
`sed -i '' '/MUTATION/d'` + `grep -rc MUTATION app/src` (debe dar 0) + reinstalar el build limpio. Para
ver transiciones: `adb shell settings put global transition_animation_scale 10` (y volver a `1.0`).
Corte de red: `adb shell svc wifi/data disable|enable`. Instrucciones de enlaces y muerte de proceso en
`2026-11-17-initactivity-review.md`, sección «Prueba en el dispositivo».

## 4. Lo que queda abierto, por prioridad

1. **Verificar en dispositivo lo de la lista «NO verificado»** cuando haya sesión (sobre todo QR sin red y
   medidas/analítica offline, que son flujos principales).
2. **Nada vuelve a comprobar la cuenta al recuperar la red tras «Continue offline».** Hoy la primera
   llamada que el servidor rechace con error de sesión dispara el aviso; una comprobación explícita al
   volver la red sería más limpia.
3. **`MainViewModel.logout()`** registra `observeForever` en cada cierre de sesión y «quita» el observador
   con una lambda nueva (`removeObserver { ... }`), que no elimina nada: se acumulan observadores. Quedó
   sin tocar; conviene guardar la referencia y quitarla.
4. **Logs fuera de `DebugLog`:** quedan ~140 llamadas `Log.d/w/i` directas (`MainViewModel` 36,
   `KamleonGraphView` 13, `TabFragment` 12, …). `AGENTS.md` pide no loguear datos médicos ni
   credenciales y no replicar los logs verbosos: revisar cuáles escriben valores y pasarlos a `DebugLog`.
5. **`MainActivity` / pantallas pendientes de la ronda anterior:** `HomeFragment` y `HomeItemFragment`
   (estados vacío/carga/error), `TabFragment` (332 líneas, XML; necesita plan propio).
6. **Tutorial desfasado** (capturas raster antiguas) y **columnas ORG./CENTER/TEAM que se pisan** en
   Ajustes → Account: detalle en el handoff anterior. El tutorial es decisión de Xavi: no empezarlo sin
   preguntar.
7. **`endsSession` es una lista cerrada** (ver 1.3). Si el backend devolviera otro código para una sesión
   caducada, no se detectaría dentro de la app: amplía `endsSession` en `network/Failures.kt` y su test.
8. **Textos nuevos solo en inglés** (`strings.xml`); Xavi los revisa y traduce aparte.

## 5. Reglas del proyecto (resumen)

- No tocar `google-services.json`, `local.properties`, `.idea/`, certificados ni artefactos generados.
- Commits pequeños, uno por cambio coherente, verificación completa antes de cada uno. Los commits de este
  trabajo terminan con la línea `Co-Authored-By` indicada por la sesión.
- Sin push ni PR sin que Xavi lo pida. Informa con honestidad de lo que no pudiste probar.
- Pantallas existentes en XML + ViewBinding siguen así salvo petición de migrar; Compose solo donde ya
  existe.
