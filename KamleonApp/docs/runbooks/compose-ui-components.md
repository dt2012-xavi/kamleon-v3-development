# Componentes y tipografía de las pantallas Compose

Fuente única: `views/KamleonComponents.kt`. Las pantallas Compose (`LoginScreen`, `ForgotPwdScreen`, y las siguientes) deben usar estos bloques en lugar de estilos ad hoc.

## Escala tipográfica

Todo el texto usa la familia Circular a través de `MatTheme.fontFamily` (Regular, Medium, Bold).

| Función | Peso y tamaño | Uso |
| --- | --- | --- |
| `kamleonTitle()` | Bold 20sp | Títulos de pantalla |
| `kamleonBody()` | Regular 16sp | Descripciones y texto principal |
| `kamleonBodySmall()` | Regular 14sp | Texto secundario y de campos |
| `kamleonCaption()` | Regular 13sp | Enlaces, ayudas y pies (sustituye los 12sp del XML) |
| `kamleonButtonText()` | Medium 16sp | Etiquetas de botón |

La cara *Book* no forma parte de la escala. En las pantallas XML el estilo `CircularBookFont` ya se dibuja con la cara Regular: `TextAppearance.Kamleon` define `app:fontFamily`, que AppCompat prioriza sobre el `android:fontFamily` de los estilos `CircularBookFont`/`CircularMediumFont` (causa inferida por comparación de capturas en un SM-A556B, no verificada en otros dispositivos). Si se quiere usar Book, hay que decidirlo explícitamente.

Los tamaños se leen de `ts_*` con `spResource`, de modo que respetan la escala de fuente del usuario.

## Componentes

- `KamleonTextField`: campo de una línea en forma de píldora. Placeholder dentro (desaparece al escribir), texto a la izquierda, contorno visible (`kamleon_outline`) que pasa a teal con foco, y botón de ojo si `secure`. Acciones de teclado por defecto: Siguiente mueve el foco y Hecho cierra el teclado. Sustituye al `KamleonInputBox` en Compose; la vista XML `KamleonInputBox` sigue existiendo para Register/Privacy/Onboarding hasta migrarlos.
- `KamleonRoundButton`: botón píldora teal; translúcido al pulsar, con contorno al estar deshabilitado, y con spinner si `loading`.
- `KamleonTextLink`: texto pulsable con color de reposo/pulsado y subrayado opcional.

## Layout

Los márgenes de cada pantalla siguen los valores literales del XML correspondiente (por ejemplo 90dp de cabecera y 44dp laterales) mientras exista el XML. Las pantallas con contenido que puede crecer (teclado abierto, fuentes grandes) hacen scroll en lugar de anclar bloques al borde inferior.

## Decisiones de diseño (2026-11-12)

- El campo de texto deja de replicar el `KamleonInputBox` del XML (etiqueta fija a la izquierda y valor a la derecha, relleno #FAFAFA sin contorno, que se perdía sobre fondos del mismo color) para ser un campo convencional.
- Se elimina la pequeña flecha decorativa junto al ojo.
- Los textos de 12sp pasan a 13sp.
