# Sostenibilidad del build y plataforma Android en dos etapas

## Intención

Cerrar la modernización de Gradle, dependencias y manifest antes de adaptar la aplicación al nuevo backend. El resultado debe reducir permisos, dependencias y configuración innecesarios, recuperar una verificación reproducible y dejar una base clara para migrar a AGP 9 sin mezclar cambios de plataforma con cambios funcionales.

La estrategia elegida separa una primera etapa de higiene sobre el toolchain actual de una segunda migración mayor. Esto permite atribuir regresiones y conservar builds utilizables durante todo el trabajo.

## Estado de partida

- Tres módulos: aplicación, `:datepicker` y `:wheelview`.
- Toolchain efectivo: AGP 8.13.2, Gradle 8.14.6, Kotlin 2.2.21, JDK 17, compile/target SDK 36.
- Hilt 2.58, Firebase BoM 34.19.0, Compose BoM 2026.06.00, Room 2.8.5 y CameraX 1.6.2.
- Los cuatro flavors actuales son `internal`, `development`, `preProduction` y `prod`.
- Los cuatro builds debug y los unit tests compilan. Lint puede fallar dentro de `lintAnalyzeDevelopmentDebugAndroidTest`; antes de la modernización se registraron 431 advertencias y ningún error.
- Hay cambios locales no confirmados en los build files, el catálogo de versiones y `.idea/`. Se deben preservar y reconciliar antes de editar los mismos archivos.
- La centralización en `libs.versions.toml` está incompleta: aún hay versiones directas y aliases separados para app y librerías locales.
- El manifest conserva permisos y opciones legacy, y el FileProvider expone `root-path`.

## Objetivos

1. Conseguir una configuración Gradle comprensible, con una única fuente de verdad para versiones compartidas.
2. Retirar permisos, dependencias y código alternativo cuya ausencia pueda demostrarse.
3. Corregir la superficie del FileProvider y dejar una política explícita de backup.
4. Conseguir que build, tests y lint sean puertas de calidad reproducibles.
5. Migrar después a AGP 9.5/Gradle 9.6 y su modelo de Kotlin integrado sin flags temporales permanentes.
6. Actualizar Firebase y las familias AndroidX que requieren el toolchain nuevo de forma aislada y verificable.
7. Mantener los cuatro entornos, sus Firebase, hosts, package, firma y proceso manual de publicación.

## No objetivos

- Adaptar todavía contratos o lógica del nuevo backend.
- Reestructurar ViewModels, repositorios o modelos de dominio.
- Migrar toda la UI a Compose o Navigation 3.
- Consolidar las tres bases Room o cambiar sus esquemas.
- Activar R8/minificación.
- Automatizar la publicación en Play Console.
- Eliminar `largeHeap` sin medición.
- Cambiar el application ID, hosts o proyectos Firebase.

## Enfoques considerados

### 1. Permanecer en AGP 8.13

Permite limpiar manifest, catálogo, dependencias y lint con riesgo bajo. Deja pendiente el salto inevitable a Kotlin integrado, Gradle 9 y las nuevas ramas de Firebase/AndroidX.

### 2. Dos etapas

Primero estabiliza y simplifica el proyecto con AGP 8.13. Después migra el toolchain completo en una rama y grupo de cambios independientes. Es el enfoque elegido porque conserva una línea base operativa y permite localizar la causa de cada fallo.

### 3. Actualización conjunta

Actualizar manifest, Firebase, AndroidX, AGP, Kotlin y procesadores a la vez reduce el número de ramas, pero no permite distinguir con claridad incompatibilidades de API, procesadores, lint o comportamiento. Se descarta por la cobertura de tests actual.

## Etapa 1: higiene sobre AGP 8.13

### Catálogo y build files

- Terminar la centralización de todas las versiones directas que sean compartidas.
- Usar la misma versión de AppCompat, ConstraintLayout, Material y AndroidX Test en los tres módulos salvo incompatibilidad demostrada.
- Mantener Groovy; no migrar a Kotlin DSL ni introducir plugins de convención.
- Mantener por ahora `maven-publish`, coordenadas y minSdk propios de `:datepicker` y `:wheelview`. Su posible retirada necesita confirmar si se publican fuera de esta app y no aporta valor suficiente a esta pasada.
- Conservar la firma local ya documentada y evitar secretos en el repositorio.
- Corregir la comprobación previa de firma para que proteja específicamente las tareas de `prodRelease`.
- Registrar dependencias resueltas antes y después de retirar artefactos.

### Dependencias

Retirar cuando el análisis y la compilación confirmen que no tienen consumidores activos:

- `play-services-location`.
- CameraX `camera-video` y `camera-extensions`.
- La declaración directa de `mlkit-vision-common` si Barcode Scanning ya la aporta transitivamente.
- ZXing, `QRCodeImageAnalyzerKotlin` y `QRCodeFoundListener`, dejando ML Kit como único analizador QR. Antes de eliminar, verificar que no queda una ruta de fallback activa.
- Comentarios Gradle que conservan dependencias alternativas antiguas.

Mantener:

- CameraX core, camera2, lifecycle y view.
- ML Kit Barcode Scanning.
- Coil y Coil GIF.
- Gson mientras Room y preferencias lo usen.
- Hilt Navigation Compose mientras `PremiumView` obtenga el ViewModel desde Compose.
- CircleImageView mientras siga referenciado desde recursos XML.

Sustituir Accompanist System UI Controller por las APIs actuales de edge-to-edge y barras de sistema. La migración debe conservar contraste y colores en las pantallas Compose existentes.

### Manifest, permisos y almacenamiento

- Eliminar `WRITE_EXTERNAL_STORAGE` y `requestLegacyExternalStorage`.
- Eliminar `ACCESS_COARSE_LOCATION` después de retirar el launcher/código comentado asociado.
- Eliminar el permiso inexistente `com.dynatech2012.kamleonuserapp.fileprovider.READ`.
- Cambiar la autoridad fija del FileProvider a `${applicationId}.fileprovider` en manifest y código.
- Sustituir `root-path` por una ruta específica para `getExternalFilesDir(DIRECTORY_PICTURES)/my_images`, además de las rutas internas que realmente se utilicen.
- Restringir el app link a la ruta exacta `/scan` y conservar un host por flavor.
- Mantener Camera y POST_NOTIFICATIONS.
- Desactivar backup de aplicación en esta etapa como opción segura por defecto para datos de salud, sesión y bases locales. Una futura política de backup selectivo requerirá decisión de producto y pruebas de restauración.
- Mantener `largeHeap` como deuda explícita y crear una medición de memoria antes de decidir su retirada.
- No cambiar orientación ni componentes exportados salvo la restricción del app link. `InitActivity` continúa exportada por ser entrada launcher/app link; los demás componentes permanecen privados.

### Lint y calidad del build

- Reproducir y corregir la excepción de `lintAnalyzeDevelopmentDebugAndroidTest`.
- Crear un baseline de lint para deuda histórica revisada, sin ocultar errores de seguridad o corrección.
- Hacer que CI falle ante nuevos errores y nuevas advertencias seleccionadas de alta señal.
- Eliminar tests de ejemplo sin valor y corregir el package del instrumentado antes de usarlo como smoke test.
- No hacer una eliminación masiva de recursos sin revisar variantes y recursos referenciados dinámicamente.

## Etapa 2: migración de plataforma

Esta etapa empieza solo con la etapa 1 integrada y verde.

### Toolchain

- Comprobar que la versión instalada de Android Studio soporta AGP 9.5 y actualizar el IDE si hace falta; es un prerrequisito de entorno, no un cambio del repositorio.
- Ejecutar después AGP Upgrade Assistant en Android Studio, como exige el flujo oficial para un proyecto que parte de AGP 8.
- Migrar a AGP 9.5 y Gradle 9.6 con JDK 17.
- Adoptar Kotlin integrado de AGP y el nuevo DSL, evitando mantener opt-outs temporales como estado final.
- Actualizar Hilt a 2.59.2 o posterior compatible con la versión elegida de AGP.
- Migrar procesadores de Room y Hilt de KAPT a KSP cuando la combinación seleccionada esté soportada. Si Hilt bloquea KSP, usar legacy-kapt solo como transición documentada.
- Conservar Groovy y los módulos actuales.
- Subir primero compileSdk a 37. Mantener targetSdk 36 durante la migración técnica y subir targetSdk a 37 como último bloque de la etapa 2, en un cambio separado con revisión de cambios de comportamiento y prueba manual.

### Dependencias mayores

Actualizar por familias, con una verificación entre ellas:

1. Toolchain, Hilt y procesadores.
2. Compose BoM y AndroidX base.
3. Navigation, Lifecycle, Core y ConstraintLayout.
4. Firebase BoM 35.1.x y productos Firebase.
5. Plugins Google Services y Crashlytics solo si existen versiones compatibles posteriores.

La migración Firebase debe comprobar Auth, Firestore, Realtime Database, Functions, Storage, Messaging, Analytics y Crashlytics. No se cambia simultáneamente el contrato del backend.

## Verificación y puertas de aceptación

### Etapa 1

- `./gradlew help`.
- Builds debug de los cuatro flavors.
- Unit tests de development.
- Lint de development sin excepción interna.
- Assemble de `:datepicker` y `:wheelview`.
- Comparación de dependencias resueltas antes/después.
- Comprobación de manifests fusionados: package, hosts, permisos y Firebase por flavor.
- Prueba en emulador disponible de captura de foto, selección de galería, QR inválido y app link. Si no hay emulador configurado o una capacidad no puede simularse con fiabilidad, registrar el comando y la limitación.
- Confirmar mediante prueba automatizada o inspección verificable que ninguna URI del FileProvider puede representar archivos fuera de las rutas previstas.
- La prueba con hardware Android 11 y Android 15/16, QR real válido y recepción de FCM se registra como verificación externa. No bloquea el cierre técnico de esta etapa, pero sí debe completarse antes de publicar.

### Etapa 2

- Sync de Android Studio.
- `./gradlew help`.
- `./gradlew build --dry-run`.
- Las mismas compilaciones, tests y lint de la etapa 1.
- `bundleProdRelease` firmado localmente y verificación de certificado/manifest, sin publicar.
- Pruebas en emulador de login, medidas, QR inválido, cámara y navegación cuando el entorno y las cuentas disponibles lo permitan.
- Actualización de token, recepción FCM, QR real y cámara en hardware quedan como verificación externa previa a publicación.
- Sin flags de compatibilidad temporales de AGP 9 en el estado final.

## Orden de entrega

Cada bloque se entrega como un diff revisable:

1. Reconciliar cambios Gradle locales y cerrar catálogo.
2. Limpiar dependencias sin uso.
3. Endurecer manifest, permisos, FileProvider y backup.
4. Sustituir Accompanist y limpiar permisos/código asociado.
5. Estabilizar lint y su baseline.
6. Ejecutar la matriz de verificación de la etapa 1.
7. Abrir la migración AGP 9 como trabajo separado.
8. Actualizar familias AndroidX/Firebase una vez estabilizado el toolchain.
9. Cerrar la etapa 2 con la subida independiente de targetSdk 36 a 37 y su validación de comportamiento.

## Riesgos y mitigaciones

- La retirada de ZXing puede eliminar un fallback no documentado: verificar referencias y el flujo con ML Kit en emulador; la lectura de un QR real queda como verificación externa previa a publicación.
- El cambio de FileProvider puede romper la cámara si la ruta XML no coincide exactamente: añadir prueba de URI y prueba manual.
- Desactivar backup cambia restauraciones existentes: documentarlo en release notes y validar que los datos remotos se recuperan tras login.
- AGP 9 puede romper plugins, KAPT y DSL: usar Upgrade Assistant, migrar procesadores explícitamente y no mezclar Firebase.
- Firebase 35 contiene versiones mayores: revisar notas por producto y mantener pruebas de contrato antes de adaptar el backend.
- El catálogo actual tiene cambios locales: nunca sobrescribirlos; revisar el diff y preservar intención antes de consolidar.
