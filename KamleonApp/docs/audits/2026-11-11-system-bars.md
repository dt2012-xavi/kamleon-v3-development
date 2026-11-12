# Barras del sistema sin Accompanist

## Implementación

- BaseActivity configura enableEdgeToEdge de AndroidX Activity antes de inflar el binding y setContentView.
- Status/navigation usan `SystemBarStyle.light(TRANSPARENT, TRANSPARENT)` (cambio del 2026-11-12): barras transparentes con iconos oscuros, de modo que se ve el fondo de la activity y no hay corte visual. Solo tema claro; el modo oscuro futuro necesitará `SystemBarStyle.auto`. (Antes: `SystemBarStyle.dark` con kamleon_dark_grey.)
- InitActivity y MainActivity heredan esa configuración. AppTheme conserva únicamente el esquema Material y la tipografía; se eliminan los controladores y efectos vacíos de ViewPager/PremiumView.
- Se retiran dependencia, alias y versión de Accompanist. Se mantienen las versiones actuales de Activity/Core y el resto del catálogo.
- Los insets de las pantallas XML los aplica `android:fitsSystemWindows="true"` en la raíz de `activity_init.xml` y `activity_main.xml`; no se añade padding Compose que pueda duplicarlo. (Ver corrección de bandas más abajo.)
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

## Corrección: bandas oscuras desplazadas (2026-11-12)

Verificado en dispositivo real (SM-A556B, Android 16, navegación por tres botones): con `android:fitsSystemWindows=true` en `Theme.KamleonApp`, la plataforma aplicaba ese valor por defecto a todas las vistas, incluida la capa interna que dibuja el fondo de las barras. Esa capa recibía padding de una altura de barra y el scrim de `SystemBarStyle.dark(kamleon_dark_grey)` se pintaba dentro de la app (y=89–178 px arriba y y=2070–2205 px abajo) en vez de bajo las barras reales; además tapaba la parte inferior del contenido.

Cambio: se elimina `android:fitsSystemWindows` del tema y se declara solo en las raíces de `activity_init.xml` y `activity_main.xml`. Resultado verificado en la pantalla de login: barras oscuras en su posición real con iconos claros y contenido completo entre ellas.

Pendiente: verificar `MainActivity` (recibe el mismo atributo pero no se ha podido abrir sin sesión), teclado abierto con `adjustResize`, diálogos/hojas y modo gestos.

## Login: teclado y foco (2026-11-12)

- `KamleonInputBox` ya no sobreescribe `KeyboardActions`: "Siguiente" mueve el foco al campo siguiente y "Hecho" cierra el teclado, igual que los EditText del XML (que no lanzaban el login con "Hecho").
- `LoginScreen` hace scroll: con espacio suficiente el botón queda anclado abajo como en el XML; con el teclado abierto el contenido se desplaza y el botón queda bajo el formulario en lugar de tapar los inputs.
- Verificado en dispositivo (SM-A556B): barras continuas, botón bajo el formulario con el teclado, Siguiente → contraseña, Hecho cierra el teclado.
