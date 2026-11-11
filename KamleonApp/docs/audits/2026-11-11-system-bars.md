# Barras del sistema sin Accompanist

## Implementación

- BaseActivity configura enableEdgeToEdge de AndroidX Activity antes de inflar el binding y setContentView.
- Status/navigation usan SystemBarStyle.dark con kamleon_dark_grey: iconos claros independientemente del tema.
- InitActivity y MainActivity heredan esa configuración. AppTheme conserva únicamente el esquema Material y la tipografía; se eliminan los controladores y efectos vacíos de ViewPager/PremiumView.
- Se retiran dependencia, alias y versión de Accompanist. Se mantienen las versiones actuales de Activity/Core y el resto del catálogo.
- Se conserva el manejo XML de insets existente (fitsSystemWindows del tema); no se añade padding Compose que pueda duplicarlo.
- Ambas actividades declaran adjustResize para los formularios XML. No se encontraron TextField/OutlinedTextField/BasicTextField Compose en producción.

## Alcance y límites

Se aplica la guía local edge-to-edge y la [API AndroidX Activity](https://developer.android.com/jetpack/androidx/releases/activity) compatible con Activity 1.13.0. El cambio de API propuesto en Activity 1.14 alpha no se introduce en esta tarea; no se actualizan dependencias a alpha. Al actualizar Activity, revisar la sustitución por WindowCompat/ProtectionLayout según la API estable de entonces.

No se afirma una verificación visual: no hay dispositivo conectado ni AVD configurado, comprobado durante la tarea previa del manifest. Se conserva esta verificación como externa según instrucciones del usuario. Los tests unitarios existentes no cubren color, insets ni teclado.

Pendiente en emulador/dispositivo antes de publicar: Init/Main en claro/oscuro, API 30 y 35/36, gestos y tres botones, formularios con teclado abierto, menús, listas, escáner y hojas Compose. Confirmar iconos legibles, fondo gris y ausencia de solapamiento o doble padding. Verificar también ventanas de diálogos/hojas que gestionan sus propios insets.

## Verificación

- ./gradlew :app:assembleDevelopmentDebug :app:testDevelopmentDebugUnitTest :app:assembleDevelopmentDebugAndroidTest --console=plain --quiet: exit 0; unit test 1/1, sin fallos.
- dependencyInsight --dependency accompanist --configuration developmentDebugRuntimeClasspath: sin dependencias coincidentes.
- Sin referencias a rememberSystemUiController, accompanist.systemuicontroller o setSystemBarsColor en producción, ni alias/dependencia de Accompanist en app/catálogo.
- ./gradlew --no-daemon :app:lintDevelopmentDebug --console=plain --quiet: exit 0, sin incidencias nuevas; baseline existente 434 errores + 8 hints, sin ampliarlo.
- Build y lint muestran un aviso de entorno SDK XML v4 frente a herramientas que comprenden hasta v3. No bloquea estas comprobaciones. Revisar la coherencia de SDK/command-line tools en la actualización del entorno, sin atribuirlo al código de barras.
- Sincronización Gradle de Android Studio: correcta.
- Inspección IDE: sin avisos en las nuevas llamadas de AndroidX; se conserva deuda previa ajena en BaseActivity.
- Revisión visual y pruebas runtime pendientes; no se afirma una suite instrumentada ejecutada ni una comprobación RED/GREEN de la UI.
