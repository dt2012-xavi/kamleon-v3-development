# Componentes y tipografía de las pantallas Compose

Fuente única: `views/KamleonComponents.kt`. Las pantallas Compose (`LoginScreen`, `ForgotPwdScreen`, y las siguientes) deben usar estos bloques en lugar de estilos ad hoc.

## Escala tipográfica

Todo el texto usa la familia Circular a través de `MatTheme.fontFamily` (Regular, Medium, Bold).

| Función | Peso y tamaño | Uso |
| --- | --- | --- |
| `kamleonTitle()` | Bold 20sp | Títulos de pantalla y de cada pregunta (centrados) |
| `kamleonBody()` | Regular 16sp | Descripciones y texto principal |
| `kamleonBodySmall()` | Regular 14sp | Texto secundario y de campos |
| `kamleonCaption()` | Regular 13sp | Enlaces, ayudas y pies (sustituye los 12sp del XML) |
| `kamleonButtonText()` | Medium 16sp | Etiquetas de botón |

La cara *Book* no forma parte de la escala. En las pantallas XML el estilo `CircularBookFont` ya se dibuja con la cara Regular: `TextAppearance.Kamleon` define `app:fontFamily`, que AppCompat prioriza sobre el `android:fontFamily` de los estilos `CircularBookFont`/`CircularMediumFont` (causa inferida por comparación de capturas en un SM-A556B, no verificada en otros dispositivos). Si se quiere usar Book, hay que decidirlo explícitamente.

Los tamaños se leen de `ts_*` con `spResource`, de modo que respetan la escala de fuente del usuario.

## Componentes

- `KamleonTextField`: campo de una línea en forma de píldora. Placeholder dentro (desaparece al escribir), texto a la izquierda, contorno visible (`kamleon_outline`) que pasa a teal con foco, y botón de ojo si `secure`. Acciones de teclado por defecto: Siguiente mueve el foco y Hecho cierra el teclado. Admite `errorText` (mensaje bajo el campo con contorno rojo, `liveRegion` para TalkBack y `bringIntoView` si tiene foco), `focusRequester` (mover el foco al primer campo inválido), `onImeAction` (Hecho/Ir envía el formulario), `isError` (contorno rojo), mayúsculas (`capitalization`), `autoCorrect`, `contentType` de autocompletado y `onFocusChange`. Sustituye al `KamleonInputBox` en Compose; la vista XML `KamleonInputBox` sigue existiendo para Register/Privacy/Onboarding hasta migrarlos.
- `KamleonRoundButton`: botón píldora teal; translúcido al pulsar, con contorno al estar deshabilitado, y con spinner si `loading`.
- `KamleonProgressBar`: barra de progreso lineal de 4dp con extremos rectos.
- `KamleonTextLink`: texto pulsable con color de reposo/pulsado y subrayado opcional.

## Layout

Los márgenes de cada pantalla siguen los valores literales del XML correspondiente (por ejemplo 90dp de cabecera y 44dp laterales) mientras exista el XML. Las pantallas con contenido que puede crecer (teclado abierto, fuentes grandes) hacen scroll en lugar de anclar bloques al borde inferior.

## Decisiones de diseño (2026-11-12)

- El campo de texto deja de replicar el `KamleonInputBox` del XML (etiqueta fija a la izquierda y valor a la derecha, relleno #FAFAFA sin contorno, que se perdía sobre fondos del mismo color) para ser un campo convencional.
- Se elimina la pequeña flecha decorativa junto al ojo.
- Los textos de 12sp pasan a 13sp.

## Mejoras de UX (2026-11-16)

- Register: los errores marcan el campo con contorno rojo; el formulario explica por sí mismo por qué no se puede enviar (email inválido al salir del campo, contraseña corta, contraseñas distintas); mayúsculas automáticas en nombre y apellido; Siguiente pasa de Contraseña a Repetir; sugerencias de autocompletado (nombre, email, contraseña nueva).
- Privacy: título coherente (Bold 20 centrado), filas de al menos 48dp, ítem de consentimiento separado en dos párrafos; se elimina un observador de `uiState` heredado que podía saltar a Onboarding.
- Onboarding: el botón Atrás del sistema retrocede un paso; contador "Paso X de 5" y una frase con el motivo bajo cada pregunta (textos pendientes de revisión de producto); el estado "email de verificación enviado" ya no avanza pasos por sí solo.

- `KamleonFormLayout`: layout de los formularios de acceso. El contenido hace scroll y la acción principal vive en una barra fija inferior que queda sobre el teclado; recibe `imeVisible` para ocultar acciones secundarias (enlace «Register»/«Log in») mientras se escribe. Los botones de Login/Register no se deshabilitan en silencio: al pulsar con datos faltantes o inválidos muestran el error junto al campo y mueven el foco al primero.

## Cambio de contraseña (2026-11-17)

`PwdScreen` mantiene 1:1 el aspecto de Ajustes del XML (`activity_pwd.xml`): botón atrás redondo de 36dp con sombra, título Bold 14 y tarjetas `bg_setting_list_shadow` (fondo #FAFAFA sobre seis anillos de 1dp de negro translúcido) con etiqueta a la izquierda y valor a la derecha. No usa `KamleonTextField`: las filas son propias de la pantalla (`PasswordRow`), pero siguen los criterios de Login/Register: error bajo la fila (etiqueta en rojo), foco al primer campo inválido, «Hecho» en «Confirmar» guarda, el botón Guardar ya no se deshabilita y vive en la barra fija de `KamleonFormLayout` (ahora con `barHorizontalPadding`), y barra de fuerza bajo «Nueva» (`KamleonPasswordStrengthMeter` acepta `inset`). El botón atrás tiene etiqueta accesible («Back»), que en el XML no tenía. La validación es la función pura `pwdFieldErrors`, con test unitario.

Corrección aplicada también a `RegisterScreen`: los errores que solo se evalúan al pulsar (longitud, contraseña débil) se calculaban pero no se pintaban; ahora, tras el primer envío, todas las reglas se evalúan en vivo y el mensaje que bloqueó el envío queda junto a su campo hasta corregirlo. En el teclado Samsung la etiqueta de la tecla de acción va un cambio de foco por detrás (muestra «Sig.» en el último campo); por eso `KamleonTextField` y `PasswordRow` tratan también Next como envío cuando tienen `onImeAction`.

## Splash y transiciones (2026-11-17)

- `SplashScreen`/`SplashFragment` pasan a Compose. El GIF (`drawable-xxhdpi/splash.gif`, 1920x1080) se dibuja con Coil dentro de un `ImageView` (`AndroidView`): Compose no tiene painter para GIF animado y el proyecto no usa `coil-compose`. El layout 60 % de ancho, cuadrado y recortado al centro replica el XML. El fragment solo espera `MIN_DURATION_MS` (2 s) y avisa al ViewModel.
- Arranque: `InitActivity` lanza `checkLogin()` al crearse, en paralelo con el splash; `route()` espera a ambos. Antes la comprobación empezaba tras los 2 s y se sumaban (medido en el Samsung: ahora la salida a Main ocurre ~2,0 s tras mostrar el splash).
- Transiciones: `res/anim/fade_in|fade_hold|fade_out` (250 ms). La pantalla entrante hace fundido encima de la saliente, que se mantiene opaca (`fade_hold`): desvanecer ambas a la vez baja la opacidad combinada y, con la ventana translúcida de `InitActivity`, dejaría ver lo que hay detrás. Todas las acciones de `login_nav.xml` las llevan, y `InitActivity.openMain()` usa `ActivityOptions.makeCustomAnimation` (verificado grabando el arranque: fundido limpio Splash → Main, sin destello).
- Navegación de arranque: `InitActivity` navega con las acciones globales `action_global_loginFragment`/`action_global_registerFragment`, que sacan el splash de la pila. Antes se navegaba al destino sin `popUpTo`, así que el splash seguía debajo de Login.

## Onboarding: defaults, edad mínima y animación (2026-11-17)

- Los selectores arrancan en valores típicos (170 cm, 70 kg) y el género en «Prefer not to say»: pulsar «Siguiente» sin tocar la rueda ya no registra 140 cm, 30 kg ni «Male». Los valores usan el formato `valor.decimal` que devuelve `WheelDataPicker`.
- La edad mínima (`OnboardingSteps.MIN_AGE`, 14) se explica bajo el título de la fecha de nacimiento y gobierna el valor por defecto y la fecha máxima.
- Los pasos hacen un fundido (`AnimatedContent`, 90 ms de salida y 220 ms de entrada) en vez de cambiar de golpe.
