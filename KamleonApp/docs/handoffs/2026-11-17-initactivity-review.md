# Handoff: revisar `InitActivity` y el arranque hasta la home

Fecha: 2026-11-17. Rama: `codex/gradle-modernization` (worktree
`/Users/xavi/.codex/worktrees/ea6d/android-java-kotlin-kamleon-userapp-development/KamleonApp`),
unos 46 commits por delante de `development`, **nada subido**. Último commit: `5db2a4a`.

## Contexto

La modernización de la app va pantalla a pantalla. Ya están migradas a Compose y revisadas
(con sus diálogos como `KamleonSheet`): Splash, Login, Register, Privacy, Onboarding, Forgot
password y Pwd (cambio de contraseña). Lee primero
`docs/handoffs/2026-11-17-compose-migration-and-password-policy.md` y
`docs/runbooks/compose-ui-components.md` (sistema de diseño, hojas, transiciones).

Xavi quiere repasar **lo que falta desde el splash hasta la home**. Este handoff cubre el primer
punto: `InitActivity`. No tiene pantalla propia, pero decide a dónde va el usuario.

## Qué hace hoy `InitActivity`

Fichero: `app/src/main/java/com/dynatech2012/kamleonuserapp/activities/InitActivity.kt`.

- `onCreate` lanza `viewModel.checkLogin()` mientras suena el splash (mínimo 2 s). `route()` espera
  a que acaben las dos cosas y decide según `AuthViewModel.StartupState`:
  `Ready` abre Main; `NeedsLogin`/`NeedsPolicy` van a Login; `NeedsProfile` va a Register;
  `RetryableError` muestra un Snackbar indefinido con «Reintentar».
- Enlaces `https://<host>/scan?unitId=…&sessionId=…`: `handleIntent` los guarda en
  `viewModel.pendingScan` y `openMain()` los pasa a `MainActivity` (`action = "scan"` y extras).
- Permiso de notificaciones: el onboarding avisa por Fragment Result
  (`Constants.GRANT_NOTIFICATION`) y `askNotificationPermission()` lo pide. Siempre marca
  `notificationPermissionAsked = true` (`utils/NotificationPermissionPrefs.kt`).
- `resetRestoredFlow` cubre la restauración tras muerte de proceso: si la actividad se restaura sin
  que el splash esté en la pila, se manda a Login. (Hasta `f47987b` esto hacía crash.)

## Qué hay que revisar (hipótesis a verificar, no hechos)

1. ~~**Enlace `/scan` con la sesión cerrada.**~~ **Hecho (2026-11-17).** El escaneo no se perdía
   (login y onboarding salen por `openMain()` y vive en `SavedStateHandle`), pero cualquier
   restauración tras muerte de proceso hacía crash en `InitActivity.onCreate` (`f47987b`). Además:
   el escaneo caduca a los 10 minutos (`PENDING_SCAN_MAX_AGE_MS`, `250ec36`) y Login avisa de que
   hay uno esperando (`5418b9f`). Probado en el Samsung con sesión iniciada (arranque y muerte de
   proceso) y sin sesión (enlace → Login → login → Main, con el escaneo subido); **falta** probar
   registro → Main con un escaneo pendiente (exige crear una cuenta). También salieron y se
   arreglaron: la subida de QR ignoraba el resultado de Firestore (`6164e08`), cada enlace apilaba
   otra `MainActivity` y el cierre de sesión dejaba las anteriores debajo (`b119b5a`).
2. ~~**`onNewIntent` con la app ya abierta.**~~ **Hecho (2026-11-17).** `onNewIntent` solo corre con
   `InitActivity` viva (splash, Login, registro, onboarding); con sesión hecha el enlace crea una
   `InitActivity` nueva que sigue por `onCreate` (probado). Se mantiene el `checkLogin()`, pero
   reiniciaba `profileCreated`/`recoveringProfile`, lo que levantaba el bloqueo de «atrás» tras
   crear la cuenta (`ec7593e`); ahora lo decide `registrationFlagsAfterCheck` (`249f4eb`, con
   tests). Probado en el móvil: enlace con Login abierto. **Sin probar en el móvil** el enlace a
   mitad del onboarding (exige crear una cuenta). Nota: `checkLogin()` no recarga
   `FirebaseUser`, así que `isEmailVerified` puede estar desfasado hasta que Firebase refresque
   el token.
   - Hallazgo al probar con una cuenta real (Xavi, 17:52): la hoja «Verify your Email» salió estando
     aún en el paso del género, sin confirmarlo ni saltarlo. El correo de verificación pedido al
     crear la cuenta tardó 9 s y su confirmación (estado 3) terminaba el onboarding si el usuario
     estaba en el último paso. Arreglado en `5638c96`: solo la confirmación de un «Reenviar» lo hace
     (`finishAfterVerificationEmailSent`, con tests). La temporización no se reprodujo en el móvil
     y el arreglo no está instalado en el Samsung (instalarlo mataría el onboarding en curso).
   - Sigue sin probarse en el móvil el bloqueo de «atrás» con un enlace entre los pasos 1 y 4: la
     hoja final no se puede cerrar con Atrás, así que hay que mandar el enlace antes de que salga.
3. ~~**`RetryableError`.**~~ **Hecho (2026-11-17, `7e8ab90`).** Sustituido el Snackbar por una
   `KamleonSheet` no descartable con «Retry» (decisión de Xavi: hoja, no pantalla de error). Al
   reintentar se espera 600 ms: `showKamleonSheet` ignora una hoja con la misma clave que aún se
   está cerrando, y un fallo inmediato no habría dejado ningún mensaje. La consulta del perfil no
   tenía límite de tiempo: ahora se corta a los 10 s (`profileOrTimeout`, con tests). En el móvil
   (Wi‑Fi y datos cortados) un fallo tarda de 0,1 s a más de 10 s según el estado de reconexión de
   Firestore, y durante ese tiempo solo se ve el logo. Probado: la hoja vuelve tras cada reintento
   y con red «Retry» lleva a Login. **Ojo con las mediciones:** si Xavi toca el móvil a la vez, las
   comprobaciones extra del log engañan; mide con el móvil quieto. **Pendiente:** los errores de
   cuenta que no son de red (permiso denegado, usuario desactivado) siguen mostrándose como «revisa
   tu conexión» (punto 7).
4. ~~**`mainOpened` no se guarda** en el estado.~~ **Hecho (2026-11-17).** Pasa al `AuthViewModel`,
   junto a `appInitializationStarted`. Era un riesgo real pero estrecho (recreación entre
   `startActivity` y que el sistema marque la actividad como terminada) y, desde `b119b5a`
   (`CLEAR_TOP | SINGLE_TOP`), un segundo arranque reutilizaría la `MainActivity` abierta en vez de
   apilar otra: el cambio es defensivo. No reproducido (llegar a Main exige iniciar sesión).
   Probado en el Samsung: cambio de tamaño de fuente en Login (recrea la actividad): se redibuja,
   no repite la comprobación de sesión y no hay crash.
5. ~~**Complejidad de `route()`.**~~ **Hecho (2026-11-17).** La decisión vive en
   `startupRouteFor(estado, pantalla actual, flujo restaurado)` (`activities/StartupRoute.kt`), una
   función pura; `route()` solo la aplica. Sin cambio de comportamiento: un test temporal comparó la
   función nueva con una copia de la lógica antigua en las 56 combinaciones (7 estados × 4
   pantallas × restaurado sí/no) y coincidían; se borró y queda `StartupRouteTest` (9 tests) como
   tabla de referencia. Probado en el Samsung: arranque → Login y enlace con Login abierto.
   Si cambias la lógica, ajusta ese test: es la única documentación de la tabla.
6. ~~**Permiso de notificaciones en Android 13+.**~~ **Revisado por código (2026-11-17), sin
   cambios.** `MainActivity.askNotificationPermission()` sale si `notificationPermissionAsked` ya
   está puesto; `InitActivity` lo pone tanto si se concede como si se deniega, y el cierre de sesión
   (`removeUser()`) no toca esa bandera (el único `clear()` de `SharedPrefUtil` no lo llama nadie).
   Ajustes muestra `perfil && areNotificationsEnabled()` y se refresca en `onResume`; si ya no se
   puede preguntar (`asked` y sin `shouldShowRequestPermissionRationale`) abre los ajustes del
   sistema. Nada de este código ha cambiado desde la verificación en el Samsung. **No repetido en
   el móvil** (sesión cerrada, no se llega a Main/Ajustes). Notas menores, sin arreglar:
   `AuthViewModel.notificationPermissionGranted()` se llama también al denegar (nombre engañoso,
   mejor `...Answered`), y `MainActivity.initView()` pide el permiso en cada creación hasta que hay
   respuesta, así que una recreación con el diálogo abierto lo lanzaría dos veces (teórico).
   - Añadido a petición de Xavi (2026-11-17): quien entra con una cuenta ya creada (otro móvil,
     reinstalación) no ve el paso del onboarding, y `MainActivity` mostraba el aviso del sistema sin
     explicación. Ahora sale antes una hoja («Activate your notifications» / qué se recibe /
     «Activate» y «Not now»; «Not now» deja la bandera sin marcar y se vuelve a ofrecer). Decisión
     en `notificationAskFor()` (`utils/NotificationAsk.kt`, 5 tests); no sale si Main se abre por un
     enlace `/scan`, para no apilarla sobre la hoja del escaneo. **No visto en el móvil** (Main
     exige sesión verificada). También hecho: el paso del onboarding ya dice qué se recibe (la
     puntuación tras cada uso; `onboard_noti_sub2`, el mismo texto que usa la hoja de Main) y se
     corrigió la errata «notifcation» de Preferencias. Textos solo en inglés, sin revisar.
7. ~~**Estados de `checkLogin()`**~~ **Hecho (2026-11-17, `d74323f`).** Revisado cada caso:
   *sin sesión* → `NeedsLogin`; *correo sin verificar* → `NeedsLogin` (sale por Login; el registro a
   medias se conserva, punto 2); *perfil inexistente* → `NeedsProfile`; *red/servidor
   (UNAVAILABLE, DEADLINE_EXCEEDED, timeout…)* → `RetryableError` (hoja con «Retry», punto 3). **El
   callejón sin salida era el resto:** cuenta desactivada/borrada o sesión revocada daban
   PERMISSION_DENIED/UNAUTHENTICATED (o `FirebaseAuthInvalidUserException`) y se mostraban como «revisa
   tu conexión» con solo «Retry». Ahora `isSessionRejected()` los manda a `NeedsLogin`
   (`StartupFailureTest` en JVM con clasificador inyectado + `SessionRejectedTest` en dispositivo con
   las excepciones reales; se comprobó con una mutación que el segundo falla si se rompe). **No
   reproducido de punta a punta:** exige desactivar un usuario en el proyecto Firebase. Logs del
   arranque revisados: solo nombres de clase y booleanos, nada sensible.
   Pendiente / a decidir: (a) un fallo persistente que no sea de red ni de sesión (INTERNAL, perfil
   que no se puede parsear) sigue siendo «Retry» sin salida; una salida genérica («Cerrar sesión» en
   la hoja) necesita limpiar también los datos locales (`MainViewModel.logout()` borra Room y prefs)
   y es peligrosa si el fallo es solo de red; (b) al mandar a Login por sesión rechazada no se
   explica por qué; (c) no se cierra la sesión de Firebase ni se limpia Room, así que otro usuario
   que entre en ese móvil heredaría datos locales (`MainViewModel.logout()` sí los limpia).
   Aparte, fuera del arranque: `FirestoreDataSource` registra con `Log.d` fechas, ids y precisión de
   medidas (líneas ~171–243), contra la regla de AGENTS.md de no registrar payloads médicos.
   - **Primera tanda de casos sin salida (2026-11-17, `da0ce19`).** Cuenta desactivada
     (`ERROR_USER_DISABLED`, antes «User not found»), sin red y demasiados intentos al iniciar
     sesión (antes «Unknown error»), perfil ilegible tras iniciar sesión (rechazo → hoja + logout con
     limpieza; otro fallo → «Retry» o «Log out», con tiempo máximo de 10 s) y `acceptPolicy()` que
     no respondía al fallar (el botón giraba para siempre). Clasificación en `AuthErrors.kt`
     (`authErrorKindOf` probado en dispositivo con `AuthErrorKindTest`; `LoginState` fija los
     códigos de `uiState`, los nuevos empiezan en -8). `SessionCleaner` comparte con el logout de
     Ajustes el borrado de token FCM, Room y preferencias, con 3 s de límite al paso remoto
     (`deleteToken` esperaba la confirmación del servidor: sin red, el logout se colgaba; por
     lectura de Firestore, no probado). Soporte: solo texto («Contact your organization or Kamleon
     support»), la app no tiene canal. Probado en el Samsung: sin red + credenciales falsas →
     «No connection». **No visto en el móvil:** cuenta desactivada, demasiados intentos y las hojas
     posteriores al login (exigen que Firebase responda así; para la desactivada, desactiva un
     usuario de prueba en la consola).
   - **Caso 1 de la lista (2026-11-18): fallo persistente en el arranque que no es de red.** Nuevo
     estado `StartupState.ServerError` (cualquier fallo de lectura que no sea «perfil inexistente»,
     «rechazado» ni transitorio) con su hoja «Something went wrong»: «Retry» y «Log Out»
     (`SessionCleaner` + cierre de sesión + nueva comprobación, que lleva a Login). Transitorios,
     que siguen con la hoja de solo «Retry»: timeout, `IOException`, `FirebaseNetworkException` y
     Firestore UNAVAILABLE / DEADLINE_EXCEEDED / ABORTED / RESOURCE_EXHAUSTED / CANCELLED
     (`isTransientFailure`; el enum de Firestore no carga en JVM, así que los códigos reales los
     prueba `SessionRejectedTest` en dispositivo, con mutación comprobada). Probado en el Samsung con
     un fallo forzado temporal: hoja, «Retry» y «Log Out» → Login. **Sigue pendiente de la lista:**
     6 (sesión rechazada en el arranque: va a Login sin explicar ni limpiar, aunque `SessionCleaner`
     ya permite hacerlo), 7, 8, 9, 10, 11.
   - **Caso 6 + códigos de error (2026-11-18, `e41e8c0`).** Una sesión rechazada en el arranque ya no
     va a Login sin explicar: nuevo `StartupState.SessionRejected` → hoja no descartable «Your session
     has ended» con un solo mensaje neutro y «OK», que hace `logOut(restartStartupCheck = true)`
     (`SessionCleaner` + cierre de sesión + nueva comprobación → Login). El escaneo pendiente se
     conserva (vive en el `SavedStateHandle`). Solo sale desde el splash o un flujo restaurado.
     **Códigos de error** para los casos sin salida, con la política acordada: el número nombra la
     CAUSA y se comparte con iOS/web; solo cambia el prefijo de plataforma (`KL-AND-102`). Se
     muestra en la hoja («Error code: …»), se escribe en el log y va a Crashlytics como clave
     `error_code`. Código en `viewmodels/ErrorCode.kt` (`ErrorCodeTest` fija los números);
     **tabla compartida en `docs/runbooks/error-codes.md`** (hay que moverla a un sitio común a
     todas las plataformas). Llevan código: arranque rechazado (101–104), error de servidor del
     arranque (110), cuenta rechazada tras el login (101–104) y cuenta desactivada en el login (103).
     Probado en el Samsung con un rechazo forzado temporal: hoja con `KL-AND-102`, Back no hace nada,
     OK → Login; `SessionRejectedTest` (5 tests con las excepciones reales) en dispositivo.
     **No visto:** un rechazo real del servidor, ni la hoja de cuenta desactivada con código. Ojo: un
     despliegue roto de reglas de Firestore ahora cerraría la sesión y borraría la caché local de
     quien abra la app. **Sigue pendiente:** 7, 8, 9, 10, 11.
   - **Caso 7 (2026-11-18): política sin aceptar en el arranque.** Antes abría Login vacío y había que
     escribir la contraseña de una cuenta ya abierta solo para ver la hoja. Ahora el arranque
     (`StartupAction.ShowPolicy`, solo desde el splash o un flujo restaurado) abre Login con la hoja
     de la política ya encima (`viewModel.showPolicy()` pone `uiState` a 6 una vez en Login). «Disagree»
     ya no cierra la app (`finishAffinity`, mala práctica: no cierra el proceso y deja al usuario en el
     mismo punto) sino que hace logout: «Disagree, log out». Si falla guardar la respuesta, nueva hoja
     no descartable «We couldn't save your answer» con «Retry» y «Log Out» (`LoginState.POLICY_RETRY`
     = -12). **Lección:** un reintento que falla al instante vuelve antes de que la hoja se cierre y
     `showKamleonSheet` lo ignora (misma etiqueta): hace falta una pausa de 600 ms
     (`retryAfterSheetCloses`, también aplicada a «Retry» de la hoja de perfil ilegible). Probado en
     el Samsung con un `NeedsPolicy` forzado temporal: hoja directa, «Agree» sin sesión → hoja de
     reintento, «Retry» → vuelve a salir, «Log Out» → Login limpio. **No visto:** aceptar de verdad
     (exige una cuenta verificada con la política sin aceptar). Ya no queda ningún botón que cierre la
     app. **Sigue pendiente:** 8, 9, 10, 11.
   - **Caso 8 (2026-11-19): correo sin verificar en el arranque.** Antes abría Login vacío sin decir
     nada, y nadie hacía `reload()`: `isEmailVerified` lee el usuario en caché y el correo se verifica
     fuera de la app (enlace + Cloud Function), así que un correo recién verificado podía seguir
     saliendo «sin verificar» (deducido del código, **no visto en un dispositivo**). Ahora, si consta
     sin verificar, el arranque llama a `reloadEmailVerified()` con 5 s de límite
     (`verifiedAfterReload`, pura e inyectable; sin respuesta cuenta como no verificado). Si sigue sin
     verificar: `StartupState.NeedsVerification` → `StartupAction.ShowVerification` (solo desde el
     splash o un flujo restaurado; un `/scan` en mitad del registro no lo interrumpe y las banderas
     del registro se conservan) → Login con una hoja no descartable de tres opciones: «I've verified my
     email» (repite la comprobación con `recheckVerification()`, spinner en el botón de Login y, si
     sigue sin verificar, la hoja vuelve diciéndolo), «Resend verification Email» (vuelve con un aviso
     de enviado o fallido) y «Log Out» (limpia y deja Login; salida para un correo mal escrito).
     Para el tercer botón `KamleonSheet` gana `SheetSpec.tertiary` y `SheetResult.TERTIARY` (los `when`
     existentes los ignoran). `sendVerificationEmail()` ahora usa `requestVerificationEmail()`. Probado
     en el Samsung con `NeedsVerification` forzado dos veces: hoja, «I've verified» → vuelve con
     aviso, «Resend» sin sesión → aviso de fallo, «Log Out» → Login limpio; 7 tests de dispositivo
     de la hoja (incluido el tercer botón) y 5 de `SessionRejectedTest`. **No visto:** el `reload()`
     real, que exige una cuenta no verificada. Sin comprobación automática al volver del correo: lo
     sustituye el botón. **Sigue pendiente:** 9, 10, 11.
   - **Caso 9 (2026-11-19): sin red y con sesión válida.** El arranque lee el perfil con
     `Source.SERVER` a propósito (detecta cuentas desactivadas, casos 1 y 6); sin red caía en la hoja
     de solo «Retry» y no dejaba abrir la app aunque hubiera perfil en caché y medidas en Room. Se
     eligió la opción B (decisión de producto): si el fallo es de red (`RetryableError`) y el perfil en
     caché (`Source.CACHE`) está completo (correo verificado y política aceptada), la hoja añade
     «Continue offline», que abre Main con esos datos (`canContinueOffline`, 4 tests; el estado lo
     expone `AuthViewModel.continueOffline`). Con el servidor sin responder se salta su comprobación
     hasta que haya red: una cuenta desactivada podría abrir la app sin conexión, pero solo vería los
     datos que ya tiene el teléfono. Probado en el Samsung con un fallo forzado temporal (sin sesión
     real, así que la caché se forzó): hoja con «Retry» y «Continue offline», «Retry» vuelve a
     comprobar y «Continue offline» abre `MainActivity`. **No visto:** una caída de red real con sesión
     y caché reales, ni Main funcionando sin conexión (deducido del código).
   - **A TENER EN CUENTA CUANDO SE TOQUE `MainActivity`:** nada vuelve a comprobar la cuenta al
     recuperar la red tras «Continue offline». Main no trata una sesión rechazada (el equivalente al
     caso 6): si el servidor la rechaza entonces (PERMISSION_DENIED / UNAUTHENTICATED /
     `FirebaseAuthInvalidUserException` en cualquier llamada), el usuario no recibe explicación, ni
     limpieza, ni vuelta a Login. Hay que añadirlo: hoja «Your session has ended» con su código
     (`ErrorCode`) y `SessionCleaner` + cierre de sesión. La misma nota está en el KDoc de
     `canContinueOffline`. **Sigue pendiente:** 10, 11.
8. **Transiciones.** El fundido a Main usa `ActivityOptions.makeCustomAnimation` (`fade_in`,
   `fade_hold`). Verificar que Main no parpadea en blanco con la barra de estado transparente.

## Prueba en el dispositivo

Samsung `RZCX21APAGW` (API 37), flavor `development`, paquete
`com.dynatech2012.kamleonuserapp` (el flavor no añade sufijo al paquete, solo `-dev` a la
versión). **No cierres sesión ni crees cuentas sin pedirlo.**

**Cuidado:** `connectedDevelopmentDebugAndroidTest` desinstala la app al terminar y con ella la
sesión. Si hay que conservarla, no lo lances o avisa antes.

```bash
P=com.dynatech2012.kamleonuserapp
adb shell am start -a android.intent.action.VIEW \
  -d "https://dev.kamleon.app/scan?unitId=TEST&sessionId=TEST" $P
# Muerte de proceso: `am kill` no mata la app recién mandada a segundo plano; esto sí.
adb shell input keyevent KEYCODE_HOME
adb shell run-as $P kill -9 $(adb shell pidof $P)
# Volver como desde recientes (restaura la pila con su estado guardado):
adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n $P/.activities.InitActivity
```

Cada prueba con el enlace sube un escaneo `TEST` a Firestore de desarrollo.

Verificación habitual:

```bash
./gradlew -q :app:assembleDevelopmentDebug :app:compileDevelopmentDebugAndroidTestKotlin \
  :app:testDevelopmentDebugUnitTest :app:lintDevelopmentDebug
```

Lint tiene baseline y `warningsAsErrors`; los recursos sin uso son error.

## Qué queda después de `InitActivity` (para el siguiente agente o la siguiente ronda)

En orden propuesto:

1. `MainActivity`: entrega del QR por enlace, receptor de mensajes de Firebase, hoja «scanned».
2. `HomeFragment` y `HomeItemFragment` (ya tienen `ComposeView`; revisar estados vacío, carga,
   error y sin datos con el criterio de Login/Register).
3. `TabFragment` (332 líneas, XML + ViewBinding): barra de pestañas, escáner QR y permiso de
   cámara. Es lo más grande; conviene un plan propio.

### Hallazgos de la prueba de `TutorialFragment` (2026-11-17, Samsung)

Se alcanza de dos formas: primer login (`TabFragment.getFirstLogin()`) y Ajustes → Help → App
guide. Desde Help, «Start!» vuelve a Help (correcto). Funciona, pero está desfasado:

- Son capturas **raster** de una versión antigua (`bg_tutorial_1…6`): fondo borroso, marco negro
  alrededor, reloj «19:02» y avatar de otro usuario dentro de la imagen, y etiquetas
  «HOMEPAGE»/«USER QR» que ya no existen (la app dice «HOME»/«SCAN»).
- La flecha de atrás se superpone al logo y la barra de progreso al texto del avatar; «Skip» cae
  encima de la barra de pestañas de la captura.
- Cambiar la UI real deja el tutorial incorrecto sin que nada lo avise. Opciones: rehacerlo con
  superposiciones sobre la pantalla real (coach marks) o regenerar las capturas. Es decisión de
  Xavi; no empezarlo sin preguntar.
- No se comprobó si «primer login» se marca como visto al terminar; mirar `getFirstLogin()` en
  `MainViewModel`.

### Hallazgo en Ajustes

En Ajustes → Account → «Account information» las columnas ORG./CENTER/TEAM se **pisan el texto**
con nombres largos («Federación española de remo», «Mega Volatile Celia…»). Es XML en
`SettingFragment`; arreglar con anchos ponderados y `maxLines`/elipsis.

## Reglas del proyecto

- Ver `AGENTS.md`. No tocar `google-services.json`, `local.properties`, ni `.idea/`. El árbol
  tiene cambios ajenos sin commitear (`.idea/*` y varios docs sin seguimiento): no los incluyas en
  tus commits.
- Textos visibles en `res/values/strings.xml`; los nuevos están solo en inglés y Xavi los revisa
  aparte.
- Commits pequeños, uno por cambio coherente, y verificación completa antes de cada uno.
- No hagas push ni abras PR sin que Xavi lo pida.
- Informa con honestidad de lo que no pudiste probar en el dispositivo.
