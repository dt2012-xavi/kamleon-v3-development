# Android Build Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Modernizar Gradle y las dependencias, mantener cuatro entornos coherentes y añadir comprobaciones GitLab sin automatizar la publicación.

**Architecture:** Conservar Groovy y los tres módulos actuales. Agrupar cambios por frontera verificable: identidad de entornos, catálogo de versiones, familias de dependencias, CI y release local. Un único proyecto Android vive en `KamleonApp/`; `.gitlab-ci.yml` vive en la raíz Git junto a `GraphViewDemo/`.

**Tech Stack:** AGP/Gradle/Kotlin, Android SDK 36, Firebase, Compose, Room, CameraX, Hilt, GitLab CI.

**Spec:** `KamleonApp/docs/superpowers/specs/2026-11-10-gradle-modernization-design.md`

## Global Constraints

- `minSdk 30` en la app; mantener `compileSdk` y `targetSdk` 36 salvo requisito técnico probado.
- Flavors: `internal`, `development`, `preProduction`, `prod`. Un solo `applicationId`; proyectos Firebase y hosts actuales sin cambios.
- `preProduction` conserva nombre visible `Kamleon-PRE`, sufijo `-pre`, host `pre.kamleon.app`; mover su JSON Firebase sin cambiar bytes.
- Conservar `versionCode 2028` que ya está modificado localmente; preservar `.idea/` y `../.DS_Store`.
- Firma y Play Console locales/manuales. No versionar keystore, contraseñas o rutas privadas; no entregar secretos al CI.
- No activar R8, migrar Kotlin DSL, reescribir UI, consolidar Room ni tocar `GraphViewDemo`.

## Review Focus

- Flavor renombrado pero Firebase de otro entorno: verificar `processPreProductionDebugGoogleServices` y el `project_id` del JSON movido antes/después.
- `prod` cambia de paquete/host/versionCode por accidente: comparar merged manifest y configuración Gradle antes/después.
- Aumento de `minSdk` deja fuera a un usuario activo: consultar versión mínima observada en Play Console antes de publicar.
- Upgrade de CameraX/Firebase compila pero rompe QR o notificaciones: prueba manual dirigida en dispositivo y unit tests de adaptaciones.
- CI sin pipeline en merge requests o job bloqueado por runner/cuota: validar CI Lint, ejecutar MR real y activar `Pipelines must succeed` solo tras un resultado correcto.

---

### Task 1: línea base de los cuatro entornos

**Archivos:** crear `KamleonApp/docs/audits/2026-11-10-build-baseline.md`; leer `KamleonApp/app/build.gradle`, los JSON por flavor y manifests fusionados. No editar configuración.

- [ ] Registrar `git status --short --branch`, versiones Gradle/AGP/Kotlin, `versionCode` local y hash del JSON de `stagging`.
- [ ] Ejecutar desde `KamleonApp/`: `./gradlew :app:assembleInternalDebug :app:assembleDevelopmentDebug :app:assembleStaggingDebug :app:assembleProdDebug :app:testDevelopmentDebugUnitTest :app:lintDevelopmentDebug --console=plain`.
- [ ] Anotar éxito/fallo, advertencias existentes y `project_id` esperado por flavor. Registrar `applicationId` y host de `prod` del manifest fusionado.
- [ ] Revisar el documento y confirmar que los fallos preexistentes quedan diferenciados de los futuros.

### Task 2: nombre de preproducción y API mínima

**Archivos:** modificar `KamleonApp/app/build.gradle`, `KamleonApp/AGENTS.md`, `KamleonApp/docs/audits/2026-11-10-modernization-audit-and-roadmap.md`; mover todo `KamleonApp/app/src/stagging/` a `app/src/preProduction/` (incluidos `UrlConstants.kt`, `google-services.json` y dos recursos de icono), sin editar el contenido de esos archivos.

- [ ] Cambiar solo `minSdk 26` a `30` y el nombre del flavor `stagging` a `preProduction`; preservar los demás valores y el `versionCode 2028` local.
- [ ] Mover los dos archivos de fuente/configuración; comprobar mismo hash del JSON y misma URL `https://pre.kamleon.app/`.
- [ ] Actualizar comandos y nombres vigentes de documentación; mantener documentos históricos de mayo como registro, sin reescribirlos.
- [ ] Ejecutar `./gradlew :app:assembleInternalDebug :app:assembleDevelopmentDebug :app:assemblePreProductionDebug :app:assembleProdDebug :app:testDevelopmentDebugUnitTest --console=plain`. Resultado esperado: éxito y ausencia de tareas `Stagging` en `./gradlew :app:tasks --all`.
- [ ] Comparar `project_id`, package y host por flavor con la línea base. Resolver cualquier desviación antes de seguir.

### Task 3: Gradle legible y versiones coherentes

**Archivos:** modificar `KamleonApp/build.gradle`, `KamleonApp/settings.gradle` si el catálogo lo requiere, `KamleonApp/app/build.gradle`, `KamleonApp/datepicker/build.gradle`, `KamleonApp/wheelview/build.gradle`; crear `KamleonApp/gradle/libs.versions.toml` si es la opción más clara.

- [ ] Inventariar versiones efectivamente resueltas y declaraciones usadas; localizar `ext` muertos, CameraX core/Material duplicados, versiones Compose explícitas junto al BoM y `composeOptions.kotlinCompilerExtensionVersion` heredado en librerías.
- [ ] Centralizar versiones compartidas; quitar solo declaraciones duplicadas u obsoletas probadas. No eliminar artefactos CameraX u otros componentes sin verificar referencias o árbol de dependencias.
- [ ] Ejecutar `./gradlew :app:assembleDevelopmentDebug :datepicker:assembleDebug :wheelview:assembleDebug :app:testDevelopmentDebugUnitTest --console=plain`.
- [ ] Comparar dependencias resueltas antes/después; si esta tarea solo organiza Gradle, deben permanecer iguales salvo duplicados declarativos.

### Task 4: Firebase y dependencias de bajo riesgo

**Archivos:** modificar `KamleonApp/app/build.gradle`, `KamleonApp/gradle/libs.versions.toml` si existe; adaptar imports de Firebase solo en los archivos que deje de compilar, empezando por `app/src/main/java/com/dynatech2012/kamleonuserapp/service/NotificationService.kt` y `repositories/FirestoreDataSource.kt`.

- [ ] Sustituir `firebase-messaging-ktx:24.1.2` por `firebase-messaging` gestionado por el BoM; verificar usos de API y compilar antes de actualizar el BoM.
- [ ] Elegir una versión estable de Firebase BoM y plugins Google Services/Crashlytics basada en release notes oficiales; registrar versiones objetivo y cambios incompatibles en el inventario.
- [ ] Actualizar una familia a la vez. Ejecutar builds debug de cuatro flavors, unit tests y lint de `development` tras el grupo Firebase.
- [ ] Confirmar que Messaging, Auth, Firestore, Database, Functions, Storage, Analytics y Crashlytics resuelven desde el BoM y que no queda un módulo Firebase KTX discontinuado declarado directamente.

### Task 5: AndroidX, Compose, Room, Hilt y CameraX

**Archivos:** modificar los tres `build.gradle`, catálogo si existe y los archivos Kotlin afectados por API incompatibles. Los puntos probables son `app/src/main/java/com/dynatech2012/kamleonuserapp/viewmodels/QrViewModel.kt`, `camera/QRCodeImageAnalyzerMLKitKotlin.kt`, `fragments/TabFragment.kt` y módulos `datepicker`/`wheelview`.

- [ ] Seleccionar versiones estables compatibles y documentar diferencias por familia. Alinear Compose con BoM en los tres módulos, Room runtime/compiler/ktx entre sí, CameraX entre artefactos y Hilt plugin/runtime/compiler.
- [ ] Actualizar primero AndroidX/Compose, luego Room/Hilt, luego CameraX; mantener cada grupo como diff revisable y no mezclar cambios de comportamiento sin test.
- [ ] Para cambios de API, escribir tests que prueben la conducta afectada antes de adaptar código; omitir tests que solo repitan una constante Gradle.
- [ ] Ejecutar `./gradlew :app:assembleInternalDebug :app:assembleDevelopmentDebug :app:assemblePreProductionDebug :app:assembleProdDebug :app:testDevelopmentDebugUnitTest :app:lintDevelopmentDebug --console=plain` tras cada familia que afecte a app; compilar librerías locales tras Compose.
- [ ] Si CameraX o Messaging cambian, probar manualmente cámara/QR y entrega/recepción de notificaciones en un dispositivo Android 11+; registrar modelo, versión, resultado y limitación si no hay dispositivo.

### Task 6: toolchain y pipeline GitLab

**Archivos:** modificar `KamleonApp/build.gradle`, `KamleonApp/gradle/wrapper/gradle-wrapper.properties` solo si release notes justifican upgrade; crear `.gitlab-ci.yml` en raíz Git (`../.gitlab-ci.yml` desde `KamleonApp/`); actualizar `KamleonApp/docs/runbooks/gitlab-ci-setup.md`.

- [ ] Revisar compatibilidad de AGP/Kotlin/Gradle/JDK y las advertencias Gradle 9. Elegir y registrar versiones estables compatibles; actualizar wrapper con `./gradlew wrapper`, nunca editando el JAR manualmente.
- [ ] Construir CI sin credenciales de firma: `cd KamleonApp`, `./gradlew` para cuatro debug builds, unit tests y lint de `development`; ejecutar en merge requests. Elegir imagen Android SDK/JDK 17 comprobada y caché limitada a dependencias.
- [ ] Validar YAML con GitLab CI Lint y ejecutar un MR real en runner alojado. Si el runner ya está activo, no crear otro; si el job queda pendiente, revisar ajuste de instance runners/cuota.
- [ ] Activar `Pipelines must succeed` solo después de un MR exitoso y con reglas que produzcan pipeline en todos los MRs.

### Task 7: release local reproducible

**Archivos:** modificar `KamleonApp/app/build.gradle` solo si hace falta configuración de firma local sin secretos; modificar `KamleonApp/.gitignore` para ignorar archivo de propiedades de firma si se usa; crear `KamleonApp/docs/runbooks/android-release.md`.

- [ ] Documentar dónde se proporciona localmente la clave de subida y las contraseñas (archivo ignorado o variables de entorno), sin incluir valores ni rutas privadas en Git.
- [ ] Ejecutar localmente `./gradlew :app:bundleProdRelease --console=plain` con los datos de firma existentes. Verificar que el AAB está firmado con la clave de subida esperada y contiene `applicationId`, `versionCode`, `versionName` y host de `prod` correctos.
- [ ] Registrar pasos manuales de Play Console y de comprobación posterior. No subir el AAB ni publicar como parte de este plan sin petición específica.
- [ ] Ejecutar verificación final de cuatro builds debug, tests, lint, bundle prod firmado y `git status --short --branch`; separar cambios ajenos del diff de modernización.

## Autoevaluación del plan

- Cobertura de la especificación: renombre, API 30, dependencias, CI, firma local y cuatro entornos aparecen en tareas 1–7.
- Las decisiones de versión, catálogo e imagen de CI están expresamente delegadas por la especificación y se documentan al ejecutarlas.
- `GraphViewDemo` queda fuera del alcance; los cambios en raíz Git se limitan a `.gitlab-ci.yml`.
