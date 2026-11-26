# AGENTS.md

Guia para trabajar en este proyecto con agentes de codigo. Aplica a todo el arbol `KamleonApp/`.

## Proyecto

- App Android multi-modulo: `:app`, `:datepicker` y `:wheelview`.
- Paquete principal: `com.dynatech2012.kamleonuserapp`.
- Stack principal: Kotlin, fragments XML/ViewBinding que alojan pantallas Compose, Hilt, Firebase, Room, Navigation Component, CameraX y ML Kit.
- Todo el codigo es Kotlin.
- SDK/JVM: `minSdk 30`, `compileSdk 36`, `targetSdk 36`, Java/Kotlin JVM target 17. Hilt y Room usan KSP (no kapt).
- Versiones de dependencias y plugins en `gradle/libs.versions.toml`.
- Los modulos `datepicker` y `wheelview` son librerias locales basadas en Compose, con configuracion de publicacion Maven local en sus `build.gradle`.

## Comandos Habituales

Usa siempre el wrapper local:

```bash
./gradlew -q projects
./gradlew :app:assembleDevelopmentDebug
./gradlew :app:testDevelopmentDebugUnitTest
./gradlew :app:lintDevelopmentDebug
```

Comandos utiles por flavor:

```bash
./gradlew :app:assembleInternalDebug
./gradlew :app:assembleDevelopmentDebug
./gradlew :app:assemblePreProductionDebug
./gradlew :app:assembleProdDebug
./gradlew :app:installDevelopmentDebug
./gradlew :app:connectedDevelopmentDebugAndroidTest
```

Notas:

- El flavor de preproduccion se llama `preProduction`.
- Para cambios pequenos de app, valida al menos `:app:assembleDevelopmentDebug` o el flavor afectado.
- Para cambios en repositorios, ViewModels, Room o logica compartida, ejecuta tambien el unit test del flavor correspondiente.
- Para cambios de UI/navegacion/permisos/camara/notificaciones, considera una prueba manual o `connected...AndroidTest` con dispositivo/emulador disponible.

## Estructura Importante

- `app/build.gradle`: configuracion principal de Android, version (`versionCode`/`versionName`), flavors, firma y lint.
- `app/lint-baseline.xml`: deuda de lint existente. Corrige los avisos nuevos en lugar de regenerarla para ocultarlos.
- `app/src/main/AndroidManifest.xml`: permisos, `InitActivity`, `MainActivity`, app links `/scan`, `FileProvider` y `NotificationService`.
- `docs/runbooks/`: build y manifest, release, logging, componentes Compose, codigos de error, CI de GitLab y politica de contrasenas.
- `app/src/main/java/com/dynatech2012/kamleonuserapp/base`: `BaseActivity` y `BaseFragment` con ViewBinding e inicializacion `initView()`/`initEvent()`.
- `app/src/main/java/com/dynatech2012/kamleonuserapp/activities`: arranque, deep links y flujo principal.
- `app/src/main/java/com/dynatech2012/kamleonuserapp/fragments`: pantallas principales. Cada `XxxFragment` aloja su `XxxScreen` Compose.
- `app/src/main/java/com/dynatech2012/kamleonuserapp/views`: componentes Compose compartidos (`KamleonComponents.kt`, `KamleonSheet.kt`, `KamleonInputBox.kt`), hojas (sheets) y la cinta de entorno (`EnvironmentRibbon.kt`).
- `app/src/main/java/com/dynatech2012/kamleonuserapp/viewmodels`: `AuthViewModel`, `MainViewModel`, `QrViewModel`, `ForgotPwdViewModel`.
- `app/src/main/java/com/dynatech2012/kamleonuserapp/repositories`: acceso a Firebase Auth, Firestore, Realtime Database, Cloud Functions, storage interno y Room.
- `app/src/main/java/com/dynatech2012/kamleonuserapp/database`: entidades/DAOs/bases Room para medidas y medias diarias/mensuales.
- `app/src/main/res/navigation`: grafos `login_nav.xml`, `main_nav.xml` y `tab_nav.xml`.
- `app/src/main/res/values/strings.xml` (ingles, por defecto), `values-es/` (castellano) y `values-ca/` (catalan).
- `app/src/<flavor>/java/.../constants/UrlConstants.kt`: URLs por entorno.
- `app/src/<flavor>/google-services.json`: configuracion Firebase por entorno.

## Flavors y Entornos

Hay una dimension `environment` con estos flavors:

- `internal`: app name `Kamleon-INT`, host `kamleon-v3-internal.vercel.app`, suffix `-int`.
- `development`: app name `Kamleon-DEV`, host `dev.kamleon.app`, suffix `-dev`.
- `prod`: app name `Kamleon`, host `kamleon.app`.
- `preProduction`: app name `Kamleon-PRE`, host `pre.kamleon.app`, suffix `-pre`.

Cada flavor define tambien `BuildConfig.APP_LINK_HOST` y `BuildConfig.ENVIRONMENT_LABEL` (`INT`, `DEV`, `PRE`; vacio en prod), que muestra la cinta de entorno en la esquina superior derecha.

Cuando cambies URLs legales, app links, Firebase o constantes de entorno, revisa todos los `UrlConstants.kt` y los `manifestPlaceholders` del flavor afectado.

## Arquitectura y Patrones Locales

- Mantener el patron existente de `BaseActivity<VB>`/`BaseFragment<VB>`:
  - `setBinding()` infla el binding.
  - `initView()` configura UI/observadores iniciales.
  - `initEvent()` registra listeners/eventos.
- Las pantallas con Hilt llevan `@AndroidEntryPoint`.
- ViewModels usan `@HiltViewModel`, `viewModelScope.launch(Dispatchers.IO)` para trabajo de red/DB y exponen `LiveData`.
- Se usa `Response<T>`/`ResponseNullable<T>` para estados de exito/error/loading.
- Para eventos de un solo consumo se usa `Event<T>` y `observeEvent(...)`.
- La navegacion se define en XML; al anadir destinos o acciones, actualizar el grafo correspondiente y usar IDs existentes cuando sea posible.
- La UI nueva se escribe en Compose dentro de los fragments existentes, reutilizando los componentes de `views/`. Ver `docs/runbooks/compose-ui-components.md`.
- Mantener constantes en `constants/`.
- Todo texto visible va en `res/values/strings.xml` en ingles y con su traduccion en `values-es/strings.xml` y `values-ca/strings.xml`. Ningun string nuevo sin las tres versiones.

## Datos, Firebase y Persistencia

- `UserRepository` encapsula Firebase Auth.
- `FirestoreDataSource`, `RealtimeDataSource` y `CloudFunctions` encapsulan servicios remotos.
- `MeasuresRepository` sincroniza medidas desde Room y Firebase.
- Room tiene tres bases separadas: `MeasureDatabase`, `AverageDailyMeasureDatabase`, `AverageMonthlyMeasureDatabase`.
- Si cambias entidades Room, incrementa versiones y define migraciones si hace falta; no uses cambios destructivos sin confirmar.
- Logs con Timber, solo en builds debug. No loguees credenciales, tokens, PINs, emails, identificadores ni payloads medicos. Ver `docs/runbooks/android-logging.md`.

## QR, Deep Links y Permisos

- `InitActivity` recibe app links `https://<host>/scan?unitId=...&sessionId=...` y pasa datos a `MainActivity`.
- `MainActivity` sube el QR mediante `QrViewModel`.
- El escaner usa `camera/QrCodeAnalyzer.kt` (ML Kit). `viewmodels/ScanLink.kt` valida los enlaces y sus IDs contra `BuildConfig.APP_LINK_HOST`.
- El manifest declara permisos de camara, notificaciones y estado de red. Revisa compatibilidad Android 13+ al tocar permisos.

## Estilo de Trabajo

- Antes de editar, revisa el contexto local con `rg` y archivos cercanos; sigue el estilo existente aunque no sea perfecto.
- Mantener cambios pequenos y enfocados. No refactorizar pantallas grandes mientras se corrige un bug puntual.
- No tocar artefactos generados como `app/development/`, `app/prod/`, `app/preProduction/`, `build/`, `.gradle/` o `.idea/` salvo peticion explicita.
- No modificar `google-services.json`, `local.properties`, certificados, artefactos de release o configuracion Firebase sin confirmacion clara.
- El repo puede estar sucio; no revertir cambios ajenos. Comprueba `git status --short --branch` antes y despues de cambios importantes.
- Preferir `rg`/`rg --files` para busquedas.
- Editar archivos manualmente con parches pequenos y revisables.

## Verificacion Antes de Dar por Hecho

Elige la verificacion segun el alcance:

- Solo documentacion: revisar el archivo y, si aplica, comprobar enlaces/rutas mencionadas.
- Cambios Kotlin/XML de app: `./gradlew :app:assembleDevelopmentDebug`.
- Cambios de flavor concreto: `./gradlew :app:assemble<Flavor>Debug`.
- Cambios de librerias locales: `./gradlew :datepicker:assembleDebug :wheelview:assembleDebug` o el modulo afectado.
- Cambios con tests unitarios: `./gradlew :app:testDevelopmentDebugUnitTest`.
- Cambios de lint/recursos: `./gradlew :app:lintDevelopmentDebug`.

Si no puedes ejecutar una verificacion por falta de emulador, credenciales o tiempo, dilo explicitamente con el comando que habrias usado.
