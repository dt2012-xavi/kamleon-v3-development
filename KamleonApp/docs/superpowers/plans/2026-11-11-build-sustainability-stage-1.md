# Build Sustainability Stage 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Estabilizar Gradle, retirar dependencias y permisos obsoletos, endurecer FileProvider y convertir lint en una puerta fiable antes de migrar a AGP 9.

**Architecture:** La etapa conserva AGP 8.13.2, Gradle 8.14.6, Kotlin 2.2.21, JDK 17 y los tres módulos Groovy. Los cambios se separan en catálogo, superficie de dependencias, manifest/FileProvider, barras de sistema y calidad para que cada diff pueda validarse y revertirse por separado.

**Tech Stack:** Groovy Gradle, version catalog TOML, Android SDK 36, Kotlin, XML/ViewBinding, Compose embebido, Hilt, Firebase, Room, CameraX, ML Kit, Android Lint.

**Spec:** `docs/superpowers/specs/2026-11-11-build-sustainability-design.md`

## Global Constraints

- Mantener AGP 8.13.2, Gradle 8.14.6, Kotlin 2.2.21, JDK 17, compileSdk/targetSdk 36 y minSdk 30 en la app.
- Mantener Groovy, los módulos `:app`, `:datepicker` y `:wheelview`, y los cuatro flavors actuales.
- No cambiar application ID, hosts, proyectos Firebase, versionCode 2028, versionName 1.0.15 ni firma.
- Preservar cambios locales ajenos de `.idea/` y no incluirlos en commits de esta etapa.
- No modificar `google-services.json`, lógica del backend, esquemas Room, UI funcional ni publicación.
- Mantener `largeHeap` hasta disponer de medición.
- Mantener `maven-publish` y los minSdk propios de las librerías locales.
- Hardware real, QR válido y recepción FCM son verificaciones externas previas a publicación.
- Usar el wrapper local y no ejecutar `clean`.

## Review Focus

- Una foto ubicada en `getExternalFilesDir(DIRECTORY_PICTURES)/my_images` debe producir una URI válida; cualquier archivo fuera de esa carpeta debe ser rechazado por FileProvider. Task 3 añade ambas pruebas.
- Los cuatro flavors deben conservar package, host y proyecto Firebase; Task 6 inspecciona sus manifests y builds.
- Eliminar ZXing no debe desactivar ML Kit ni producir dos analizadores QR; Task 2 verifica referencias y Task 6 recorre el escáner en emulador.
- Quitar Accompanist no debe dejar barras ilegibles ni contenido oculto; Task 4 comprueba estilos y Task 6 revisa visualmente Init/Main.
- Lint debe analizar también AndroidTest sin volver a fallar en FIR y debe distinguir deuda histórica de avisos nuevos; Task 5 ejecuta la tarea específica y genera el baseline.

---

### Task 1: Reconciliar el catálogo y cerrar la configuración Gradle actual

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle`
- Modify: `datepicker/build.gradle`
- Modify: `wheelview/build.gradle`
- Create: `docs/audits/2026-11-11-stage1-dependency-baseline.md`

**Interfaces:**
- Consumes: cambios locales existentes en los cuatro build files y el catálogo.
- Produces: aliases únicos `libs.core.ktx`, `libs.hilt.navigation.compose`, `libs.constraintlayout`, `libs.material`, `libs.androidx.test.junit` y `libs.espresso.core` usados por todos los módulos compatibles.

- [x] **Step 1: Registrar el árbol sucio y el baseline de dependencias**

Run:
`git status --short --branch`

Run:
`./gradlew :app:dependencies --configuration developmentDebugRuntimeClasspath --console=plain`

Run:
`./gradlew :datepicker:dependencies --configuration debugRuntimeClasspath :wheelview:dependencies --configuration debugRuntimeClasspath --console=plain`

Guardar en el documento de auditoría las versiones resueltas de Compose, Material, AppCompat, ConstraintLayout, AndroidX Test, Firebase, Hilt, Room, CameraX y ML Kit. No copiar credenciales ni contenido de JSON Firebase.

- [x] **Step 2: Revisar el diff local antes de editar**

Run:
`git diff -- app/build.gradle datepicker/build.gradle wheelview/build.gradle gradle/libs.versions.toml`

Confirmar que los cambios locales son centralización mecánica y sintaxis Groovy moderna. Preservar cualquier diferencia funcional no descrita por el spec y anotarla en la auditoría.

- [x] **Step 3: Consolidar aliases y versiones compartidas**

En `libs.versions.toml`:

- Añadir aliases para Core KTX 1.17.0 y Hilt Navigation Compose 1.3.0.
- Mantener una sola versión de ConstraintLayout 2.2.1, Material 1.13.0, AndroidX Test JUnit 1.3.0 y Espresso 3.7.0.
- Eliminar los aliases y versiones con sufijo `Library`.
- Mantener las versiones de toolchain y familias principales fijadas por Global Constraints.

Actualizar los tres build files para consumir esos aliases. No cambiar versiones durante esta tarea.

- [x] **Step 4: Centralizar las declaraciones directas restantes**

Reemplazar en `app/build.gradle` las coordenadas con versión de Core KTX y Hilt Navigation Compose por sus aliases. Mantener bajo BoM las dependencias Compose y Firebase sin versión individual.

- [x] **Step 5: Hacer explícita la validación de firma prodRelease**

Sustituir el matcher implícito de `preProdReleaseBuild` por una tarea `validateProdReleaseSigning` que compruebe `signing.properties` y sea dependencia de `assembleProdRelease` y `bundleProdRelease`. No conectarla a debug ni a release de otros flavors.

Run:
`./gradlew :app:bundleProdRelease --dry-run --console=plain`

Expected: el grafo contiene `validateProdReleaseSigning`.

- [x] **Step 6: Verificar configuración y módulos**

Run:
`./gradlew help :app:assembleDevelopmentDebug :datepicker:assembleDebug :wheelview:assembleDebug :app:testDevelopmentDebugUnitTest --console=plain`

Expected: `BUILD SUCCESSFUL` sin exigir propiedades de firma.

- [x] **Step 7: Comparar resolución y actualizar la auditoría**

Repetir los tres informes de dependencias del Step 1. Expected: las versiones efectivas permanecen iguales; solo desaparecen declaraciones duplicadas.

- [x] **Step 8: Commit**

Completado: 3224993. La escritura del índice volvió a responder; no se reprodujo el bloqueo previo.

```bash
git add gradle/libs.versions.toml app/build.gradle datepicker/build.gradle wheelview/build.gradle docs/audits/2026-11-11-stage1-dependency-baseline.md
git commit -m "build: finish shared dependency catalog"
```

No añadir archivos de `.idea/`.

### Task 2: Retirar dependencias y la implementación QR alternativa sin uso

Completada en 6ea99bb: cuatro builds debug y dos unit tests pasan; classpath y referencias comprobados. Smoke test del escáner en emulador pendiente de la verificación integral.

**Files:**
- Modify: `app/build.gradle`
- Modify: `gradle/libs.versions.toml`
- Modify: `app/src/main/java/com/dynatech2012/kamleonuserapp/fragments/TabFragment.kt`
- Modify: `app/src/main/java/com/dynatech2012/kamleonuserapp/activities/InitActivity.kt`
- Modify: `app/src/main/java/com/dynatech2012/kamleonuserapp/activities/MainActivity.kt`
- Delete: `app/src/main/java/com/dynatech2012/kamleonuserapp/camera/QRCodeImageAnalyzerKotlin.kt`
- Delete: `app/src/main/java/com/dynatech2012/kamleonuserapp/camera/QRCodeFoundListener.java`

**Interfaces:**
- Consumes: `QrViewModel.imageAnalysis` y `QRCodeImageAnalyzerMLKitKotlin` como implementación activa.
- Produces: una única ruta QR basada en ML Kit y declaraciones directas sin Location, Camera Video, Camera Extensions ni ZXing. Comprobar por separado las transitivas: camera-video llega también mediante camera-view y no se promete su ausencia del classpath.

- [x] **Step 1: Probar que ML Kit es la única ruta activa**

Buscar usos de `QRCodeImageAnalyzerKotlin`, `QRCodeFoundListener`, ZXing, `VideoCapture`, `ExtensionsManager` y APIs de localización. Expected: ZXing aparece solo en la clase alternativa y código comentado; vídeo/extensiones/localización no tienen consumidores funcionales.

- [x] **Step 2: Confirmar que Vision Common es transitiva**

Run:
`./gradlew :app:dependencyInsight --dependency com.google.mlkit:vision-common --configuration developmentDebugRuntimeClasspath --console=plain`

Expected: `vision-common` llega transitivamente desde `barcode-scanning`. Conservar su declaración explícita porque el analizador usa directamente InputImage; retirarla no reduce el classpath.

- [x] **Step 3: Eliminar el fallback QR antiguo**

Borrar los dos archivos de cámara alternativos. En `TabFragment`, eliminar el import, propiedad `qrListener`, callback comentado y comentarios que instancian analizadores antiguos. Conservar `qrViewModel.startScanner()` y el binding de `qrViewModel.imageAnalysis`.

- [x] **Step 4: Eliminar el código de localización muerto**

En `InitActivity` y `MainActivity`, borrar launchers, funciones y bloques comentados que solo manejan `ACCESS_COARSE_LOCATION`. No tocar el flujo de notificaciones.

- [x] **Step 5: Retirar dependencias y aliases**

Eliminar de app y catálogo:

- `play-services-location`
- `camera-video`
- `camera-extensions`
- `zxing-core`

Mantener `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`, `mlkit-barcode-scanning` y `mlkit-vision-common`.

Correcciones funcionales posteriores: ver [QR follow-up](../../audits/2026-11-11-qr-follow-up.md), QR-01 (collectors duplicados) y QR-02 (entrada QR inválida).

- [x] **Step 6: Verificar compilación y ausencia de referencias**

Run:
`./gradlew :app:assembleInternalDebug :app:assembleDevelopmentDebug :app:assemblePreProductionDebug :app:assembleProdDebug :app:testDevelopmentDebugUnitTest --console=plain`

Expected: `BUILD SUCCESSFUL`.

Buscar de nuevo los símbolos del Step 1. Expected: ninguna referencia de producción a la implementación borrada o los artefactos retirados.

- [x] **Step 7: Commit**

```bash
git add app/build.gradle gradle/libs.versions.toml app/src/main/java/com/dynatech2012/kamleonuserapp/activities/InitActivity.kt app/src/main/java/com/dynatech2012/kamleonuserapp/activities/MainActivity.kt app/src/main/java/com/dynatech2012/kamleonuserapp/fragments/TabFragment.kt app/src/main/java/com/dynatech2012/kamleonuserapp/camera
git commit -m "build: remove unused camera and location dependencies"
```

### Task 3: Endurecer manifest, app links, backup y FileProvider

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/xml/file_paths.xml`
- Modify: `app/src/main/java/com/dynatech2012/kamleonuserapp/activities/MainActivity.kt`
- Delete: `app/src/main/res/xml/backup_rules.xml`
- Delete: `app/src/main/res/xml/data_extraction_rules.xml`
- Delete: `app/src/androidTest/java/com/kamleonapp/ExampleInstrumentedTest.kt`
- Create: `app/src/androidTest/java/com/dynatech2012/kamleonuserapp/provider/FileProviderPathsTest.kt`

**Interfaces:**
- Consumes: `MainActivity.takePicture()` y `FileProvider.getUriForFile(Context, String, File)`.
- Produces: authority `${applicationId}.fileprovider` y única ruta compartible `Pictures/my_images/`.

- [ ] **Step 1: Escribir las pruebas instrumentadas de FileProvider**

Crear `FileProviderPathsTest` con:

- `profileImagePath_isShareable()`: crea/representa un archivo bajo `getExternalFilesDir(DIRECTORY_PICTURES)/my_images` y afirma que la URI usa esquema `content` y authority `${context.packageName}.fileprovider`.
- `fileOutsideProfileImagePath_isRejected()`: intenta representar un archivo bajo `context.filesDir` y afirma `IllegalArgumentException`.
- `applicationId_matchesExpectedPackage()`: afirma `com.dynatech2012.kamleonuserapp`.

- [ ] **Step 2: Ejecutar la prueba para demostrar el problema actual**

Run:
`./gradlew :app:connectedDevelopmentDebugAndroidTest --console=plain`

Expected con emulador: la prueba exterior falla porque `root-path` permite representar el archivo, o la prueba queda pendiente si no hay dispositivo.

- [ ] **Step 3: Restringir FileProvider y su authority**

En `file_paths.xml`, dejar únicamente:

```xml
<external-files-path
    name="profile_images"
    path="Pictures/my_images/" />
```

En el manifest, usar `android:authorities="${applicationId}.fileprovider"`. En `MainActivity.takePicture()`, usar `"$packageName.fileprovider"`.

- [ ] **Step 4: Limpiar permisos y opciones legacy**

Eliminar del manifest:

- `ACCESS_COARSE_LOCATION`
- `WRITE_EXTERNAL_STORAGE`
- `com.dynatech2012.kamleonuserapp.fileprovider.READ`
- `requestLegacyExternalStorage`

Mantener Camera, POST_NOTIFICATIONS y los dos `uses-feature` opcionales.

- [ ] **Step 5: Restringir el app link y desactivar backup**

Cambiar `android:pathPrefix="/scan"` por `android:path="/scan"`.

Establecer `android:allowBackup="false"`, eliminar `dataExtractionRules` y `fullBackupContent`, y borrar los dos XML de reglas vacías.

- [ ] **Step 6: Ejecutar pruebas y verificar manifest fusionado**

Run con emulador:
`./gradlew :app:connectedDevelopmentDebugAndroidTest --console=plain`

Expected: tres pruebas pasan.

Run:
`./gradlew :app:processInternalDebugMainManifest :app:processDevelopmentDebugMainManifest :app:processPreProductionDebugMainManifest :app:processProdDebugMainManifest --console=plain`

Inspeccionar los manifests fusionados. Expected: package común, host correcto por flavor, ruta exacta `/scan`, solo Camera y POST_NOTIFICATIONS entre los permisos propios del proyecto, backup desactivado y provider privado con authority del application ID.

- [ ] **Step 7: Verificar builds de los cuatro entornos**

Run:
`./gradlew :app:assembleInternalDebug :app:assembleDevelopmentDebug :app:assemblePreProductionDebug :app:assembleProdDebug --console=plain`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/res/xml app/src/main/java/com/dynatech2012/kamleonuserapp/activities/MainActivity.kt app/src/androidTest
git commit -m "security: scope file sharing and legacy permissions"
```

### Task 4: Sustituir Accompanist por la configuración de barras de AndroidX

**Files:**
- Modify: `app/src/main/java/com/dynatech2012/kamleonuserapp/base/BaseActivity.kt`
- Modify: `app/src/main/java/com/dynatech2012/kamleonuserapp/application/AppTheme.kt`
- Modify: `app/src/main/java/com/dynatech2012/kamleonuserapp/views/cards/PremiumView.kt`
- Modify: `app/src/main/java/com/dynatech2012/kamleonuserapp/views/cards/ViewPager.kt`
- Modify: `app/build.gradle`
- Modify: `gradle/libs.versions.toml`

**Interfaces:**
- Consumes: todas las Activities existentes heredan de `BaseActivity<VB>`.
- Produces: `BaseActivity.configureSystemBars()`, ejecutado antes de `setContentView`, con barras gris oscuro e iconos claros.

- [ ] **Step 1: Implementar la configuración central de barras**

Añadir a `BaseActivity`:

`private fun configureSystemBars(): Unit`

Debe llamar `enableEdgeToEdge` con `SystemBarStyle.dark(getColor(R.color.kamleon_dark_grey))` para status y navigation bar. Invocarla en `onCreate` después de `super.onCreate` y antes de inflar/asignar la vista.

- [ ] **Step 2: Retirar controladores Compose duplicados**

En `AppTheme`, quitar `rememberSystemUiController` y las dos llamadas idénticas a `setSystemBarsColor`; conservar únicamente la elección de color scheme y `MaterialTheme`.

En `ViewPager` y `PremiumView`, eliminar bloques/comentarios e imports de System UI Controller que ya no produzcan comportamiento.

- [ ] **Step 3: Retirar Accompanist**

Eliminar el alias y la versión de Accompanist del catálogo y su dependencia de app.

- [ ] **Step 4: Verificar build y classpath**

Run:
`./gradlew :app:assembleDevelopmentDebug :app:testDevelopmentDebugUnitTest --console=plain`

Expected: `BUILD SUCCESSFUL`.

Buscar `rememberSystemUiController` y `accompanist.systemuicontroller`. Expected: cero resultados.

- [ ] **Step 5: Verificar visualmente en emulador**

Abrir InitActivity y MainActivity en tema claro y oscuro. Expected: barras gris oscuro, iconos legibles y contenido sin quedar oculto. Registrar cualquier limitación del emulador.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/dynatech2012/kamleonuserapp/base/BaseActivity.kt app/src/main/java/com/dynatech2012/kamleonuserapp/application/AppTheme.kt app/src/main/java/com/dynatech2012/kamleonuserapp/views/cards/PremiumView.kt app/src/main/java/com/dynatech2012/kamleonuserapp/views/cards/ViewPager.kt app/build.gradle gradle/libs.versions.toml
git commit -m "refactor: replace accompanist system bar control"
```

### Task 5: Recuperar lint y fijar la deuda histórica

Commit fc8a6c9. Adelantada por petición del usuario antes de Tasks 3/4. Lint, build development y unit test verificados; instrumentación compilada, ejecución pendiente por ausencia de dispositivos. Ver [auditoría de lint](../../audits/2026-11-11-lint-quality-gate.md).

**Files:**
- Modify: `app/build.gradle`
- Create: `app/lint-baseline.xml`
- Delete: `app/src/test/java/com/kamleonapp/ExampleUnitTest.kt`
- Keep: `app/src/test/java/com/kamleonapp/DebugLogTest.kt`
- Test: `app/src/androidTest/java/com/dynatech2012/kamleonuserapp/provider/FileProviderPathsTest.kt`

**Interfaces:**
- Consumes: AppContextTest como smoke test actual; FileProviderPathsTest se incorporará en Task 3.
- Produces: lint AndroidTest operativo y baseline que permite convertir nuevas advertencias en fallos.

- [x] **Step 1: Eliminar el unit test de plantilla**

Borrar `ExampleUnitTest.kt`. Mantener `DebugLogTest`.

- [x] **Step 2: Reproducir de forma aislada el análisis que fallaba**

Run:
`./gradlew :app:lintAnalyzeDevelopmentDebugAndroidTest --console=plain --stacktrace`

Resultado observado: éxito incluso antes de cambiar el test; el fallo FIR histórico no se reproduce. Se corrige además la expectativa errónea del application ID y se sustituye la plantilla por AppContextTest. Expected: análisis sin error interno. Si reaparece el error FIR, repetir una vez tras `./gradlew --stop`; no desactivar el análisis de test sources ni añadir supresiones globales.

- [x] **Step 3: Configurar el baseline**

Añadir en `android.lint`:

- `baseline = file("lint-baseline.xml")`
- `abortOnError = true`
- `warningsAsErrors = true`
- `checkDependencies = true`

Generar el baseline con:

`./gradlew :app:lintDevelopmentDebug -Dlint.baselines.continue=true --console=plain`

Expected: se crea `app/lint-baseline.xml` con la deuda existente revisable.

- [x] **Step 4: Verificar que el baseline no oculta regresiones**

Run:
`./gradlew :app:lintDevelopmentDebug --console=plain`

Expected: `BUILD SUCCESSFUL` y ningún error interno.

Revisar que el baseline no contenga `SecurityException`, `UnsafeProtectedBroadcastReceiver`, `ExportedService`, `ExportedReceiver` ni incidencias nuevas del FileProvider. Si aparecen, corregirlas antes de aceptar el baseline.

- [x] **Step 5: Verificar tests**

Run:
`./gradlew :app:testDevelopmentDebugUnitTest --console=plain`

Expected: `DebugLogTest` pasa.

Run con emulador:
`./gradlew :app:connectedDevelopmentDebugAndroidTest --console=plain`

Expected: AppContextTest pasa si hay dispositivo; pendiente con No connected devices en esta sesión. Las pruebas de FileProvider se añadirán y ejecutarán en Task 3.

- [x] **Step 6: Commit**

```bash
git add app/build.gradle app/lint-baseline.xml app/src/test app/src/androidTest
git commit -m "test: establish actionable Android lint baseline"
```

### Task 6: Verificación integral y cierre de la etapa 1

**Files:**
- Modify: `docs/audits/2026-11-11-stage1-dependency-baseline.md`
- Modify: `docs/audits/2026-11-10-modernization-audit-and-roadmap.md`
- Modify: `docs/superpowers/plans/2026-11-11-build-sustainability-stage-1.md`

**Interfaces:**
- Consumes: entregables de Tasks 1-5.
- Produces: evidencia de cierre y lista explícita de validaciones externas previas a publicación.

- [ ] **Step 1: Ejecutar la verificación Gradle completa**

Run:
`./gradlew :app:assembleInternalDebug :app:assembleDevelopmentDebug :app:assemblePreProductionDebug :app:assembleProdDebug :datepicker:assembleDebug :wheelview:assembleDebug :app:testDevelopmentDebugUnitTest :app:lintDevelopmentDebug --console=plain`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Ejecutar pruebas instrumentadas si hay emulador**

Comprobar dispositivo/emulador disponible. Si existe:

`./gradlew :app:connectedDevelopmentDebugAndroidTest --console=plain`

Expected: pruebas de FileProvider en verde.

Si no existe, registrar la limitación y el comando pendiente; no bloquear el cierre técnico.

- [ ] **Step 3: Recorrer los flujos disponibles en emulador**

Comprobar captura de foto, selección de galería, apertura de un app link `https://dev.kamleon.app/scan` con parámetros inválidos y apertura del escáner. Expected: no crash, URI de foto válida, host/ruta correctos y QR inválido sin subida.

- [ ] **Step 4: Comparar el classpath final**

Repetir los informes de dependencias de Task 1. Documentar las dependencias retiradas y confirmar que Firebase, Hilt, Room, CameraX y Compose conservan las versiones de esta etapa.

- [ ] **Step 5: Actualizar documentación y checklist**

Marcar Tasks 1-6 según evidencia. En la auditoría registrar como externas antes de publicación:

- Android 11 físico.
- Android 15/16 físico, incluyendo un fabricante no Pixel si está disponible.
- QR real válido.
- Recepción y apertura de FCM.
- Contraste de barras y cámara en hardware.
- Política futura de backup selectivo, si producto desea restauración.

- [ ] **Step 6: Revisar estado final**

Run:
`git status --short --branch`

Expected: solo permanecen cambios ajenos de `.idea/` si no fueron incluidos. No deben aparecer archivos generados, secretos ni cambios Firebase.

- [ ] **Step 7: Commit**

```bash
git add docs/audits/2026-11-11-stage1-dependency-baseline.md docs/audits/2026-11-10-modernization-audit-and-roadmap.md docs/superpowers/plans/2026-11-11-build-sustainability-stage-1.md
git commit -m "docs: record build sustainability stage one"
```

## Self-Review

- Spec coverage: catálogo, dependencias, manifest, FileProvider, backup, Accompanist, lint, emulador y verificación externa están asignados a Tasks 1-6.
- Etapa 2 queda deliberadamente fuera; se planificará tras integrar esta etapa.
- Los cambios de comportamiento están limitados a permisos muertos, sharing de fotos, app link exacto, backup y barras del sistema.
- Los símbolos producidos y consumidos son consistentes: ML Kit queda como único analizador, la authority es siempre `${applicationId}.fileprovider` y la ruta es siempre `Pictures/my_images/`.
- Los cinco riesgos de Review Focus tienen prueba o verificación asignada.
