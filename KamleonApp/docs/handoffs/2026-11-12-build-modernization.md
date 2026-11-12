# Handoff — sostenibilidad Android y siguientes tareas

Fecha: 2026-11-12. Documento para continuar con otro agente sin repetir la etapa ya realizada.

## Objetivo del usuario y preferencias

El objetivo general es mejorar sostenibilidad/calidad de arquitectura y después adaptar la app a cambios de backend. El usuario priorizó primero una pasada de dependencias, Gradle/AGP y manifest.

- Conversación y documentación pueden estar en español. **Todos los comentarios de código nuevos deben estar en inglés**, incluidos Groovy y XML.
- Trabajar de forma autónoma dentro de lo acordado, con cambios pequeños y verificables.
- Las verificaciones de hardware/backend se han aceptado como externas. Se permite usar emulador si hay uno disponible; no afirmar pruebas runtime que no se han ejecutado.
- Conservar XML/ViewBinding y patrones locales; no introducir una migración general a Compose.
- Leer AGENTS.md antes de continuar. No registrar secretos, identificadores sensibles ni datos médicos.
- Este handoff no pide implementar automáticamente las tareas propuestas ni publicar, hacer merge o push.

## Workspace y Git

Proyecto:
`/Users/xavi/.codex/worktrees/ea6d/android-java-kotlin-kamleon-userapp-development/KamleonApp`

El repositorio Git está en el directorio padre. Rama: `codex/gradle-modernization`. HEAD antes del commit de este handoff: `a73e4b8`, 11 commits por delante de la referencia local `gitlab/codex/gradle-modernization`. No se ha hecho push ni merge.

Estado comprobado al preparar este documento:

- Cambios locales ajenos: .idea/compiler.xml, .idea/deploymentTargetSelector.xml y .idea/misc.xml. No revertir ni incluir en commits.
- Documentos locales todavía sin seguimiento:
  - docs/audits/2026-11-11-qr-follow-up.md
  - docs/superpowers/plans/2026-11-11-build-sustainability-stage-1.md
  - docs/superpowers/specs/2026-11-11-build-sustainability-design.md
- Estos tres documentos no viajan con un clone mientras sigan sin seguimiento. Preservarlos si se cambia de workspace. El resumen operativo de lo pendiente también figura aquí.
- El ledger detallado está en build/superpowers/2026-11-11-build-sustainability-stage-1/progress.md. Es material ignorado/local, no la única fuente de continuidad. Sus anotaciones históricas anteriores se complementan con las últimas entradas.
- Los checkboxes del plan original pueden estar desactualizados. Consultar commits y auditorías antes de repetir trabajo.

Usar los MCP studio_* de Android Studio para archivos y shell: leer buffers mediante studio_read_file, editar con replace/multi_replace/write_file; buscar mediante studio_grep/find_files. No editar ni leer archivos mediante sed/cat/redirecciones. Siempre wrapper local, sin clean. Builds, tests, shell y dispositivos secuenciales; búsquedas/lecturas independientes en paralelo. No leer ni modificar credenciales, google-services.json, certificados o local.properties.

## Trabajo ya terminado

| Commit | Resultado |
| --- | --- |
| 3224993 | Catálogo compartido y aliases reconciliados. |
| 6ea99bb | Retirados ZXing alternativo y dependencias/código de ubicación sin uso. |
| fc8a6c9 | Lint como puerta con baseline fijo y limpieza de tests de plantilla. |
| 7a7e084 | Validación de firma prodRelease aislada de configuración/debug. |
| 27fb61e | Catálogo completo y DSL de publicación modernizado. |
| 69f289b | Room/Hilt migrados de KAPT a KSP; esquemas Room versionados. |
| d544c25 | Manifest, permisos legacy, FileProvider, enlaces exactos y backup. |
| 003ba11 | Accompanist eliminado; barras centralizadas en BaseActivity y adjustResize. |
| 01829c3 | Revisión independiente de etapa 1 y corrección de receta CI. |
| 6e8c0c4 | Guía y comentarios sobre Gradle/manifest y decisiones de ciclo de vida. |
| a73e4b8 | Comentarios de código traducidos a inglés, sin cambios funcionales. |

### Configuración actual

- Módulos: app, datepicker y wheelview; Groovy, Java/JVM 17.
- AGP 8.13.2, wrapper Gradle 8.14.6, Kotlin 2.2.21; compileSdk/targetSdk 36, app minSdk 30.
- KSP 2.3.12, Hilt 2.58, Room 2.8.5. No queda KAPT en la configuración.
- KSP 2.3.13 se evitó por su regresión concurrente publicada. Hilt 2.58 se mantuvo por compatibilidad con AGP 8; no afirmar que sea la combinación con soporte KSP estable más reciente. Ver auditoría KSP.
- Los tres esquemas Room versión 2 están en app/schemas. Se compararon por JSON completo antes/después de KAPT/KSP; no cambiaron entidades, versiones ni datos.
- Firebase BoM 34.19.0, Compose BoM 2026.06.00, CameraX 1.6.2, ML Kit Barcode 17.3.0.
- Retirados Location, ZXing, Camera Extensions y Accompanist. Camera Video sigue transitiva desde camera-view; Vision Common se mantiene directa por InputImage.
- Los cuatro flavors comparten applicationId y mantienen sus hosts. No cambiar IDs/Firebase/firma para resolver un problema de build.
- R8 permanece desactivado en release; largeHeap se conserva hasta medir.
- FileProvider privado: authority basada en applicationId y única ruta Pictures/my_images/. Hay cuatro tests instrumentados de límites y AppContextTest.
- Backup desactivado con exclusiones cloud/D2D explícitas. No se eliminaron los XML por la salvedad de fabricantes que permiten D2D pese a allowBackup=false.
- BaseActivity usa Activity.enableEdgeToEdge/SystemBarStyle.dark; la revisión comprobó las protecciones de color de Activity 1.13.0 en API 35.

## Evidencia y límites de verificación

La revisión independiente cubrió 5c339b8..003ba11: ningún crítico, un importante, ningún minor. El importante fue CI mezclando build multivariante y lint; se corrigió en 01829c3 y se verificaron las dos invocaciones resultantes.

Última verificación conjunta funcional de etapa 1:

- assembleInternalDebug, assembleDevelopmentDebug, assemblePreProductionDebug y assembleProdDebug: exit 0.
- datepicker:assembleDebug y wheelview:assembleDebug: exit 0.
- testDevelopmentDebugUnitTest: exit 0; actualmente DebugLogTest, 1 prueba sin fallos. Los informes históricos de 2 tests son anteriores a retirar el ejemplo aritmético.
- assembleDevelopmentDebugAndroidTest: exit 0. **Compilar el APK no significa ejecutar las pruebas.**
- Lint aislado: exit 0, sin incidencias nuevas; baseline fijo de 434 errores históricos + 8 hints. No ampliarlo para hacer pasar una regresión.
- Tres informes runtime y manifests fusionados de los cuatro flavors revisados: familias principales/hosts/paquete conservados.
- Receta exacta de CI validada localmente: build/test --no-daemon pasó, seguido de lint aislado --no-daemon. El pipeline remoto no se ejecutó.
- Tras cambios únicamente documentales: help + processDevelopmentDebugMainManifest y git diff --check pasaron. No se repitió toda la suite por traducir comentarios.

connectedDevelopmentDebugAndroidTest se intentó y falló únicamente por No connected devices. adb devices y emulator -list-avds estaban vacíos cuando se comprobaron; verificar disponibilidad de nuevo si procede. No hubo validación visual, cámara física, QR válido, FCM, assetlinks ni transferencia OEM. No se certificaron credenciales reales ni publicación.

Limitación conocida: el analizador lint puede fallar al combinar flavors y lint en un mismo proceso por una fuente Hilt generada ausente. Ejecutar lint separado; no se afirma corregido el bug interno. También se observó un aviso SDK XML v4 frente a herramientas lectoras v3; revisar coherencia de herramientas al actualizar el entorno.

## Siguiente trabajo recomendado

La última recomendación al usuario fue corregir recreación/consumo de eventos antes de retirar portrait. Es una propuesta de prioridad; no está implementada.

### 1. Estado de captura pendiente y consumo de intents

En [MainActivity](../../app/src/main/java/com/dynatech2012/kamleonuserapp/activities/MainActivity.kt):

- currentImageUri es lateinit y no se restaura. Una recreación/muerte de proceso con cámara externa pendiente puede devolver un resultado sin URI inicializada. Reproducir, conservar/restaurar estado y manejar estado ausente sin perder la foto.
- onCreate llama handleIntent(intent), que puede subir el QR de los extras otra vez al recrear. Separar restauración de un evento nuevo y definir consumo/idempotencia por sesión, incluyendo onNewIntent.
- Son riesgos identificados por lectura de código, todavía sin reproducción runtime. Añadir pruebas de comportamiento antes de corregir y comprobar efecto único tras recreación/reentrega.
- Revisar también restauración de navegación, formularios y observers de InitActivity al volver de cámara o recrear.

### 2. Correcciones QR ya acordadas para otro cambio

En QrViewModel:

- QR-01: startScanner crea nuevos collectors en cada llamada; stopAnalyzing no los cancela. Evitar acumulación y procesamiento/subidas repetidas al salir/reentrar.
- QR-02: parseQrString puede lanzar por parámetros ausentes o URI no jerárquica, sin manejo en el collector. Entrada inválida debe producir error controlado, ninguna subida y permitir leer después un QR válido.
- Probar parámetros ausentes/vacíos, URI inválida y secuencia inválido-válido, reaperturas y una sola subida por lectura/sesión.
- Definir el contrato esperado con backend antes de fijar una validación nueva de hosts/IDs. No acceder a Firebase/reglas reales como parte incidental de esta corrección.

### 3. Orientación y tema

- Mantener portrait temporalmente en teléfonos; no se ha liberado orientación.
- Con target 36, Android 16 ya puede ignorar restricciones en pantallas sw600dp o mayores. Portrait no elimina la necesidad de restaurar estado.
- No añadir configChanges, singleTask, resizeableActivity=false ni opt-outs para esconder los problemas.
- singleTop y adjustResize se mantienen. No garantizan idempotencia ni por sí solos un IME correcto.
- InitActivity usa tema Splash translúcido y también aloja login. No se encontró installSplashScreen. Revisar arranque frío/caliente, Back y vuelta de cámara antes de cambiar tema/translucidez.
- Medir memoria antes de quitar largeHeap. hardwareAccelerated no corrige fugas.
- Tras arreglar estado, probar teléfono horizontal, sw600dp, plegado/resize, teclado, hojas, claro/oscuro y gestos/tres botones; después retirar portrait como cambio funcional separado.

### 4. Plataforma pendiente

AGP 9/API 37 **no están migrados**. Si el usuario prioriza este bloque, leer el skill agp-9-upgrade disponible en el IDE y la sección etapa 2 del diseño, verificar versiones/compatibilidades actuales en fuentes oficiales y disponibilidad del Upgrade Assistant. No aplicar a ciegas versiones propuestas en un documento anterior. Coordinar AGP/Gradle/Kotlin integrado/Hilt; compileSdk y targetSdk se suben en pasos separados con pruebas de comportamiento. No introducir flags de transición como estado final.

Los cambios de backend, logs sensibles, logout/observeForever, autorización remota y arquitectura de repositorios también siguen pendientes. No están certificados por el build verde.

## Fuentes para retomar

1. [Guía de mantenimiento Gradle/manifest](../runbooks/android-build-and-manifest.md).
2. [Revisión final y límites](../audits/2026-11-11-stage1-final-review.md).
3. [KSP](../audits/2026-11-11-ksp-migration.md), [manifest](../audits/2026-11-11-manifest-hardening.md), [barras](../audits/2026-11-11-system-bars.md).
4. [Lint](../audits/2026-11-11-lint-quality-gate.md) y [firma](../runbooks/android-release.md).
5. Plan/diseño/QR locales sin seguimiento indicados arriba; conservarlos.
6. AGENTS.md del proyecto y git status/log actuales.

No repetir tareas marcadas aquí como realizadas. Empezar por comprobar el estado del workspace y acordar el bloque siguiente usando esta evidencia, no por regenerar baseline o actualizar todas las versiones a la vez.
