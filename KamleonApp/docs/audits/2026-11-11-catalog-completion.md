# Cierre del catálogo y deprecaciones Gradle

## Cambios

- Todas las dependencias Maven directas de app, datepicker y wheelview usan aliases. Esto incluye artefactos sin versión individual gestionados por Compose BoM y Firebase BoM.
- Los plugins externos de los tres módulos usan aliases; se añade org.jetbrains.kotlin.kapt al catálogo y al bloque raíz. maven-publish conserva su ID porque es un plugin integrado de Gradle, sin versión externa.
- compileSdk y targetSdk compartidos se leen de androidCompileSdk/androidTargetSdk en libs.versions.toml. Ambos siguen en 36; los minSdk propios permanecen intactos.
- compileSdk de datepicker pasa al nivel android.
- groupId, artifactId y version de las publicaciones usan asignación explícita. Sus coordenadas y versiones no cambian.
- Las rutas AAR usan layout.buildDirectory. No se ha publicado ningún artefacto.
- No se actualizan versiones de dependencias ni del toolchain.

## SDK pendiente en etapa 2

El diseño ya contempla compileSdk 37 primero y targetSdk 37 al final, con revisión de comportamiento y prueba manual. Centralizar los valores prepara ese cambio; no significa que la migración esté hecha.

Referencia oficial consultada el 11-11-2026: [configurar Android 17/API 37](https://developer.android.com/about/versions/17/setup-sdk). La guía distingue compilar contra API 37 de optar a sus comportamientos al subir targetSdk.

La app fija orientación portrait y tiene flujos de permisos/cámara que deben validarse durante esa migración. Mantener este paso separado permite atribuir los cambios de comportamiento y de toolchain.

## Verificación

- help --warning-mode all: exit 0, sin las deprecaciones de asignación de publicaciones.
- Assemble debug de internal, development, preProduction y prod, assembleDebug de ambas librerías y testDevelopmentDebugUnitTest: exit 0. DebugLogTest: 1/1.
- Lint development en proceso independiente (--no-daemon): BUILD SUCCESSFUL, sin incidencias nuevas.
- Comparación del catálogo anterior/actual: ninguna versión existente cambia; solo se añaden las claves de SDK con valor 36. Informe runtime actual comprobado: Hilt 2.58, Compose BoM 2026.06.00, Firebase BoM 34.19.0, Room 2.8.5, CameraX 1.6.2 y ML Kit Barcode 17.3.0.
- Búsqueda en los tres módulos: ninguna coordenada Maven directa; solo maven-publish conserva ID literal como plugin integrado.
- git diff --check: exit 0.

## Ajuste puntual de lint

Se eliminan seis entradas UseTomlInstead resueltas. Se retira también una entrada OldTargetApi que ya no se emite al leer el SDK desde el catálogo: esto es un cambio de detección, no una actualización de targetSdk, que sigue siendo 36.

Se añade exclusivamente NewerVersionAvailable para org.jetbrains.kotlin.kapt 2.2.21. El plugin ya se utilizaba con esa versión; su alias explícito expone el aviso de actualización. Mantenerlo con Kotlin 2.2.21 conserva la combinación actual hasta la migración de toolchain/procesadores. No se ha regenerado el baseline completo ni desactivado detectores. Baseline resultante: 444 entradas (436 bloqueantes y 8 hints).

