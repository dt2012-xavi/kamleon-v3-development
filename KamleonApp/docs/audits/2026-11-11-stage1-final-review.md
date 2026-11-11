# Revisión final de sostenibilidad — etapa 1

## Alcance y resultado

Revisión independiente de 5c339b8..003ba11: catálogo y publicación, validación de firma, dependencias, KSP, lint, manifest/FileProvider/backup y barras. No se identificaron regresiones de producción ni hallazgos críticos. Se identificó un hallazgo importante de integración y ningún minor que requiera cambios.

El hallazgo: .gitlab-ci.yml seguía mezclando builds multivariante y lint en una sola invocación, expuesta al fallo interno de fuentes Hilt documentado. Se separa lint en una invocación --no-daemon, idéntica a la receta validada localmente. La secuencia exacta de CI se verificó localmente: build/test con --no-daemon terminó con BUILD SUCCESSFUL (14 s), seguido de lint aislado con BUILD SUCCESSFUL (22 s). Con esta corrección verificada queda satisfecho el único hallazgo importante. No se ha ejecutado el pipeline remoto.

La revisión de barras incluyó las fuentes de Activity 1.13.0: en API 35 usa ColorProtection/ProtectionLayout para los scrims suministrados. Esto respalda la configuración; no acredita la apariencia real ni los insets en dispositivo.

## Evidencia conjunta

- Cuatro assemble debug de app, assembleDebug de ambas librerías, testDevelopmentDebugUnitTest y assembleDevelopmentDebugAndroidTest: exit 0.
- Unit suite: DebugLogTest 1 prueba, cero fallos/errores. No hay cobertura funcional suficiente de login/QR/sincronización.
- lintDevelopmentDebug aislado con --no-daemon: exit 0, sin incidencias nuevas. Baseline: 434 errores históricos y 8 hints; no se amplió.
- Los tres informes runtime de dependencias: exit 0. Se conservan Firebase BoM 34.19.0, Hilt 2.58, Room 2.8.5, CameraX 1.6.2, Compose BoM 2026.06.00 y ML Kit Barcode 17.3.0. Material 1.13.0 y ConstraintLayout 2.2.1 se comparten.
- Sin Location, ZXing, Camera Extensions ni Accompanist en los informes. Camera Video permanece transitiva desde camera-view; Vision Common sigue directa por InputImage.
- Manifests fusionados de los cuatro flavors: package/hosts preservados, /scan exacto, backup desactivado y adjustResize. Sin los permisos legacy retirados.
- KSP: las tres bases mantienen versión 2; igualdad de esquemas KAPT/KSP comprobada en la tarea de migración, sin cambios en entidades ni versiones.
- La ejecución instrumentada/recorridos no se repiten sin dispositivo; adb devices y emulator -list-avds se comprobaron durante la tarea de manifest y estaban vacíos. Las pruebas están compiladas. No se afirma runtime verde.
- No se afirma ejecución de GitLab CI en su contenedor ni validación de firma criptográfica; las comprobaciones se ejecutan localmente.

## Decisiones y límites

- Se ejecuta lint separado del build multivariante por el fallo interno ya reproducido, sin afirmar que el analizador esté corregido.
- Se mantienen las versiones de plataforma durante esta etapa; AGP 9/API 37 pertenecen a otro cambio.
- Se mantiene vision-common directa por el consumo de InputImage; camera-video solo pierde su declaración directa.
- El runtime aislado de las librerías adopta las versiones comunes; las familias principales de app se conservan.
- Se mantienen Hilt 2.58 y KSP 2.3.12 con la limitación de soporte documentada en la auditoría KSP, sin afirmar adoptar la combinación estable más reciente de Dagger.
- Se conservan reglas explícitas de exclusión cloud/D2D además de allowBackup=false por la salvedad de fabricantes. El comportamiento de herramientas propietarias requiere prueba externa.
- Se reutiliza AppContextTest para el package y se añaden límites reales de FileProvider; no se duplica el test de package.
- Se añade adjustResize y se conserva fitsSystemWindows sin padding Compose adicional. El coste de una suposición incorrecta es una regresión visual/teclado que debe detectarse en la matriz externa.
- La validación de firma se aísla de debug; los fixtures estructurales previos no prueban la validez del certificado real.
- Los cambios ajenos de .idea y los documentos de planificación locales se preservan.

## Comportamientos que el revisor no certifica

- QR-01/QR-02: duplicación de collectors y entrada inválida; preexistentes, no empeorados por esta limpieza, pendientes en [seguimiento QR](2026-11-11-qr-follow-up.md). Riesgo: subidas repetidas o interrupción del escáner. Deben corregirse antes de publicar.
- Deuda del baseline: aceptada para impedir nuevas incidencias, no saneada por esta etapa.
- Insets, teclado, listas, diálogos y contraste API 30/35/36, claro/oscuro y ambos modos de navegación: pendientes de dispositivo.
- QR físico válido, FCM, asociación de dominios y transferencia OEM: verificaciones externas.
- Secretos/certificado de firma real: fuera de la revisión; no se leyeron ni modificaron.
- Backend/reglas de autorización, logs sensibles y logout: requieren cambios y verificación separados.
- AGP 9/API 37: no migrados.

## Antes de publicación

Ejecutar connectedDevelopmentDebugAndroidTest (AppContextTest + cuatro límites FileProvider), captura/galería, arranque y reentrada del escáner, QR inválido seguido de válido sin subida indebida, app links y FCM. Probar actualización desde una instalación publicada, Android 11 físico, Android 15/16 y al menos un fabricante no Pixel. Medir memoria antes de retirar largeHeap. Una política futura de backup selectivo requiere una decisión y pruebas de restauración propias.

El cierre técnico no equivale a aprobación de publicación, merge ni despliegue automático.
