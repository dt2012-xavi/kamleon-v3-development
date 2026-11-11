# Etapa 1: baseline de dependencias y configuración

## Alcance

Se conserva AGP 8.13.2, Gradle 8.14.6, Kotlin 2.2.21, JDK 17 y SDK 36. La tarea 1 reconcilia los cambios locales de sintaxis Groovy y catálogo, unifica aliases y conecta explícitamente la validación de firma a assembleProdRelease y bundleProdRelease.

Los cambios previos de los cuatro archivos Gradle/catálogo eran centralización mecánica y sintaxis de asignación. Se preservaron. Los cambios de .idea quedan fuera.

## Resolución anterior

Informes ejecutados con éxito antes de editar: app developmentDebugRuntimeClasspath y debugRuntimeClasspath de datepicker/wheelview.

| Familia | App developmentDebug | Librerías aisladas |
| --- | --- | --- |
| Compose BoM | 2026.06.00 | 2026.06.00 |
| Compose UI | 1.11.3 | 1.11.3 |
| Compose Material3 | 1.4.0 | 1.4.0 |
| AppCompat | 1.8.0 | 1.8.0 |
| ConstraintLayout | 2.2.1 | 2.1.4 |
| Material Views | 1.13.0 | 1.12.0 |
| AndroidX Test JUnit (declarado) | 1.3.0 | 1.2.1 |
| Espresso (declarado) | 3.7.0 | 3.6.1 |
| Firebase BoM | 34.19.0 | — |
| Hilt | 2.58 | — |
| Room | 2.8.5 | — |
| CameraX | 1.6.2 | — |
| ML Kit Barcode | 17.3.0 | — |

AndroidX Test/Espresso no pertenecen al runtime de producción; su resolución se verificará en el classpath AndroidTest.

## Cambios intencionados

- Core KTX 1.17.0 y Hilt Navigation Compose 1.3.0 consumidos desde aliases.
- Eliminados aliases/versiones con sufijo Library.
- Todos los módulos declaran ConstraintLayout 2.2.1, Material 1.13.0, AndroidX Test JUnit 1.3.0 y Espresso 3.7.0.
- Se preservan minSdk y publicación Maven de las librerías.

La expectativa del plan de que todas las versiones resueltas permanezcan iguales necesita precisión: el runtime final de la app ya resolvía las versiones compartidas más altas; los classpaths aislados de las librerías sí cambian deliberadamente al unificarse. Esto sigue el diseño aprobado. No se actualizan las familias principales de la app.

## Verificación

- help, assembleDevelopmentDebug, assembleDebug de datepicker/wheelview y testDevelopmentDebugUnitTest: exit 0. Dos tests, cero fallos.
- bundleProdRelease --dry-run: exit 0; incluye validateProdReleaseSigning.
- Informes posteriores de los tres runtime classpaths: exit 0. App conserva las versiones principales; las librerías resuelven ConstraintLayout 2.2.1 y Material 1.13.0.
- git diff --check: exit 0.
- Aviso de entorno: herramientas SDK con lector XML v3 frente a metadatos XML v4. No impide estos builds; revisar Command-line Tools durante la actualización del entorno.

## Tarea 2: dependencias sin uso

- Eliminados QRCodeImageAnalyzerKotlin y QRCodeFoundListener, así como los callbacks comentados y la propiedad sin uso de TabFragment. El analizador ML Kit activo y su wiring permanecen intactos.
- Retirados launchers/comentarios muertos de ubicación de InitActivity/MainActivity. El permiso del manifest se revisará en la tarea 3.
- Retiradas declaraciones y aliases de play-services-location, ZXing, camera-video y camera-extensions.
- Conservada vision-common 17.3.0 explícita por el uso directo de InputImage.
- Runtime development posterior: sin play-services-location, com.google.zxing ni camera-extensions. camera-video 1.6.2 permanece transitiva desde camera-view.
- Firebase BoM 34.19.0, Hilt 2.58, Room 2.8.5, Compose BoM 2026.06.00 y ML Kit Barcode 17.3.0 conservan sus versiones.
- Verificación: assembleInternalDebug, assembleDevelopmentDebug, assemblePreProductionDebug, assembleProdDebug y testDevelopmentDebugUnitTest terminaron con exit 0; dos tests, cero fallos.
- Sin referencias de producción a las clases borradas o APIs retiradas; git diff --check pasa.
- Pendiente la comprobación interactiva del escáner en emulador; el QR real válido sigue siendo verificación externa previa a publicación.
- Las correcciones funcionales QR-01/QR-02 están documentadas en [QR follow-up](2026-11-11-qr-follow-up.md).


