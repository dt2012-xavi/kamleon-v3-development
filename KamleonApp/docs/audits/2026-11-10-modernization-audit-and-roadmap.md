# Auditoría inicial y hoja de ruta — 2026-11-10

## Alcance y nivel de certeza

Revisión estática de los tres módulos, Gradle, manifest, almacenamiento, entrada QR, Firebase y pruebas. Se contrastaron las auditorías del 2026-05-26 con el estado actual. Esta revisión no incluye acceso a Firebase Console, reglas de Firestore/Realtime Database/Storage, Cloud Functions, Play Console, datos reales ni prueba en dispositivo. Las conclusiones de servidor y de comportamiento en producción quedan pendientes.

El árbol ya estaba modificado: `app/build.gradle` tiene un cambio local de `versionCode` 2026 a 2028 y hay cambios en `.idea/` y `../.DS_Store`. No forman parte de esta auditoría.

Línea base ejecutada el 2026-11-10: `./gradlew :app:assembleDevelopmentDebug :app:testDevelopmentDebugUnitTest :app:lintDevelopmentDebug --console=plain` terminó correctamente. Lint informó 431 advertencias y 0 errores; destacan 117 recursos sin uso y 30 avisos de dependencias. Gradle avisa de funciones obsoletas incompatibles con Gradle 9. No se ejecutaron pruebas instrumentadas ni builds de otros flavors.

## Hallazgos priorizados

| Prioridad | Evidencia actual | Riesgo / acción |
| --- | --- | --- |
| P0 | `res/xml/file_paths.xml` permite `<root-path path="." />`; `MainActivity.takePicture()` escribe en `getExternalFilesDir(DIRECTORY_PICTURES)/my_images`, fuera de la ruta específica `external-files-path path="images/"`. | El proveedor puede conceder acceso a cualquier archivo que se represente por una URI. Acotar la ruta al directorio real de fotos y probar captura, vista y compartición en dispositivo. |
| P0 | `InitActivity.handleIntent()` solo comprueba `path == "/scan"`; `QrViewModel.parseQrString()` solo comprueba que existan dos parámetros; `FirestoreDataSource.uploadQrId()` escribe el documento `QR_COLLECTION/<unitId>` con `sessionId` y `userId`. | Validar esquema, host, ruta, tamaño y formato de ambos identificadores en un único parser; probar que una URI inválida no sube nada. Confirmar en backend que el usuario autenticado puede escribir *solo* la unidad/sesión autorizada. La autorización real no se puede demostrar desde la app. |
| P0 | `QrViewModel.startScanner()` crea dos colectores nuevos en cada llamada y `parseQrString()` puede lanzar una excepción en el colector. | Riesgo de colectores duplicados, subidas repetidas y escáner que deja de procesar tras un QR inválido. Hacer el flujo idempotente, capturar errores de parseo y probar reaperturas, QR malformado y una sola subida. |
| P1 | `UserRepository.login()` registra email y contraseña mediante `DebugLog`; `AuthViewModel` registra datos de registro; `MainViewModel` registra hashes de PIN; hay `Log.d` sin filtro con datos de usuario y medidas en repositorios y pantallas. | El filtro `DebugLog` evita esos mensajes en builds release, pero siguen apareciendo en debug; los `Log.*` directos pueden aparecer en release. Eliminar secretos y datos de salud de logs, redactar identificadores y revisar mensajes de excepción/Crashlytics. |
| P1 | `allowBackup="true"`; `backup_rules.xml` y `data_extraction_rules.xml` no contienen exclusiones efectivas. Hay tres bases Room y preferencias de usuario. | Definir política de backup y transferencia para datos médicos y sesión, probar restauración en dispositivo y documentar la decisión. `PREF_USER_PASSWORD` existe como constante/limpieza, pero esta revisión no encontró una escritura activa: verificar datos de instalaciones antiguas antes de migrar. |
| P1 | La app declara `firebase-messaging-ktx:24.1.2` fuera de BoM 34.2.0; Firebase dejó de publicar KTX y retiró esos módulos del BoM. `app/build.gradle` declara CameraX core y Material dos veces; mezcla BoM Compose con versiones explícitas. | Sustituir Messaging KTX por el módulo principal, alinear versiones mediante BoM/catálogo y actualizar familias por lotes pequeños. Resolver y probar cada flavor. |
| P1 | Solo hay dos unit tests de app (`DebugLogTest` y ejemplo aritmético) y un instrumentado de ejemplo que espera el paquete `com.kamleonapp`, distinto del `applicationId`. | La compilación no acredita login, QR, datos, notificaciones ni captura. Crear pruebas de comportamiento y smoke tests; corregir/eliminar el test de ejemplo. |
| P2 | `MainViewModel.logout()` usa `observeForever()` y llama `removeObserver` con una lambda distinta; tres bases Room separadas; KAPT; Compose solo en partes de una UI XML. | Revisar fuga y logout primero. Después simplificar fronteras de datos y estado de forma incremental. Evaluar KSP y consolidación Room solo con pruebas de migración; no reescribir toda la UI en Compose por defecto. |
| P2 | `WRITE_EXTERNAL_STORAGE`, `requestLegacyExternalStorage`, `largeHeap`, implementaciones CameraX opcionales, ZXing alternativo y Accompanist system UI permanecen en el proyecto. | Retirar cada pieza tras demostrar que ninguna ruta activa la necesita. El permiso legacy y Accompanist merecen prioridad dentro de la limpieza; `largeHeap` requiere medición de memoria. |

## Estado técnico y dependencias

- Build actual: AGP 8.13.0, Gradle 8.14.3, Kotlin 2.2.10, Java 17, compile/target SDK 36; Firebase BoM 34.2.0, Room 2.8.0, CameraX 1.5.0.
- `build.gradle` raíz conserva variables `ext` antiguas (Hilt 2.44, Kotlin 1.8.20, CameraX 1.2.1, etc.) que no representan las versiones usadas. Los dos módulos de fecha/rueda aún declaran Compose 1.7.4 y `kotlinCompilerExtensionVersion 1.5.7`, aunque aplican el plugin Compose de Kotlin 2.2.10.
- En la documentación oficial consultada hoy, Firebase BoM figura en 35.1.0 y Room en 2.8.5. Son referencias de evaluación, no una propuesta de subir todo directamente: hay que comprobar compatibilidad y notas de ruptura antes de fijar versiones. Fuentes: https://firebase.google.com/support/release-notes/android y https://developer.android.com/jetpack/androidx/releases/room.
- `google-services.json` está versionado en `main` y en los cuatro flavors. No equivale por sí solo a una credencial privada, pero se debe comprobar que cada archivo pertenece al entorno correcto y que sus API keys tienen restricciones apropiadas en la consola. No modificar esos archivos sin autorización explícita.

## Plan por entregas independientes

### 0. Línea base reproducible

1. Registrar el estado de `assembleDevelopmentDebug`, `testDevelopmentDebugUnitTest` y `lintDevelopmentDebug`; repetir en `internal`, `preProduction` y `prod` antes de cambios de publicación.
2. Preparar cuentas de prueba y datos ficticios por entorno. Definir una matriz mínima: instalación limpia, upgrade desde versión publicada, login/registro/logout, PIN, carga/sincronización de medidas, QR válido/inválido, app link, notificaciones, cámara y copia/restauración.
3. Consultar Crashlytics/Play Console y reglas/configuración Firebase con responsables de backend. Convertir fallos reales en tickets con frecuencia e impacto.

**Salida:** build y pruebas reproducibles, matriz con responsable y resultado; lista de riesgos de backend validada.

### 1. Seguridad y fiabilidad de flujos críticos

1. Acotar `FileProvider` y verificar captura/compartición.
2. Introducir parser único y validación de app links/QR; gestionar errores sin cancelar el escáner; garantizar una subida por lectura/sesión.
3. Auditar y retirar logs sensibles; definir política de backup; revisar permisos de almacenamiento.
4. Revisar reglas de Firestore/Realtime Database/Storage, Cloud Functions, App Check y restricciones de API keys con el equipo dueño del backend. La aceptación requiere pruebas de acceso permitido y denegado con usuarios distintos.

**Salida:** pruebas de entradas maliciosas y repetidas, pruebas en dispositivo, ausencia de datos sensibles en logcat de release y autorización backend demostrada.

### 2. Dependencias y toolchain

1. Quitar duplicados, versiones `ext` obsoletas y dependencias sin uso demostrable; migrar `firebase-messaging-ktx` al módulo principal.
2. Centralizar versiones y BoM Compose/Firebase/CameraX; actualizar grupos de bajo riesgo, luego CameraX/Room/Hilt y por último AGP/Kotlin/Gradle, con una rama/PR y verificación por grupo.
3. Evaluar KSP frente a KAPT y actualizar módulos locales de Compose. Probar compilación de todos los flavors, lint y flujos afectados tras cada grupo; revisar el árbol de dependencias transitivas y alertas de vulnerabilidad.

**Salida:** versiones actuales soportadas y registradas, sin duplicados ni módulos discontinuados, con smoke tests y sin regresiones en los cuatro flavors.

### 3. Arquitectura y limpieza funcional

1. Cubrir con tests el comportamiento de Auth, QR, sincronización/Room y logout; corregir observadores sin ciclo de vida y estados de un solo consumo donde causen fallos.
2. Delimitar responsabilidades de ViewModels, repositorios y fuentes de datos; migrar una ruta de datos cada vez. Decidir si las tres bases Room deben consolidarse solo tras medir coste y definir migraciones sin pérdida.
3. Inventariar componentes, dependencias, permisos, recursos y pantallas con referencias reales; retirar código alternativo o muerto mediante PRs pequeños. Medir memoria antes de retirar `largeHeap`.

**Salida:** cobertura de los recorridos críticos, migraciones probadas desde datos existentes, inventario de eliminaciones con evidencia y menor complejidad observable.

## Puertas de aceptación generales

- En cada PR: compilación del flavor afectado, tests de comportamiento correspondientes y lint sin nuevas incidencias relevantes.
- Antes de release: `assemble` de los cuatro flavors, instalación y recorrido manual en Android 13, 15/16 y al menos un fabricante no Pixel; prueba de actualización de datos y revisión de Crashlytics.
- No publicar cambios de Room, backend o permisos sin prueba de actualización desde una versión real y plan de reversión.

## Pendientes que cambian la prioridad

- Fecha de release: no hay compromiso próximo; se prioriza por riesgo.
- Acceso a reglas y consola Firebase, Play Console, cuentas de prueba, dispositivos y última versión publicada. Sin ello, la auditoría de seguridad y funcionalidad seguirá siendo parcial.
- Política de retención y backup de datos médicos: requiere decisión de producto/legal antes de modificar la extracción de datos.

## Referencias externas para los criterios de seguridad

- Android, FileProvider y rutas demasiado amplias: https://developer.android.com/privacy-and-security/risks/file-providers
- Android, copia y restauración: https://developer.android.com/identity/data/autobackup
- Firebase, condiciones de autorización Firestore: https://firebase.google.com/docs/firestore/security/rules-conditions
