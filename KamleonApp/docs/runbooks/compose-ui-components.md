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
