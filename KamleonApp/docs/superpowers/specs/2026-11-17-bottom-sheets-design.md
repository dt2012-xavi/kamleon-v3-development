# Diálogos como bottom sheets: diseño

Aprobado por Xavi el 2026-11-17 en conversación. Documento corto: recoge lo acordado y las decisiones de implementación.

## Intención

Los 14 diálogos vivos de la app (16 llamadas a `AlertDialog`, dos de ellas código muerto; 6 layouts XML en `layout_dialog_*`) pasan a ser hojas inferiores. Motivos: coherencia con las hojas que ya existen (selectores de fecha/altura/peso, intro del escáner, invitaciones, detalles de recomendación), alcance con el pulgar en móviles altos y un único componente en lugar de seis layouts.

## Objetivos

- Un componente compartido, `KamleonSheet`, con el estilo de las pantallas Compose (tipografía Circular, botón pill, colores del sistema de diseño).
- Los diálogos críticos no se pueden cerrar sin querer.
- El resultado sobrevive a una recreación de pantalla (los `AlertDialog` actuales se pierden).
- Se mantienen los textos actuales; esta tarea no los revisa.

## No objetivos

- No se tocan las hojas que ya existen (`ScanIntroFragment`, `DataPickerFragment`, `DatePickerFragment`, `ImagePickFragment`, `InvitationFragment`, ni los `ModalBottomSheet` de las tarjetas).
- No se cambia cuándo ni por qué se muestra cada diálogo, salvo los avisos de éxito (abajo).

## Anti-objetivos

- Que un usuario cierre arrastrando una hoja que exige una decisión (desactivar cuenta, cerrar sesión, verificar email, aceptar la política).
- Que una recreación de la pantalla deje una hoja sin su acción asociada sin que nadie lo note.

## Decisiones

1. **Componente.** `KamleonSheet : BottomSheetDialogFragment` con contenido Compose. Parámetros: título opcional, mensaje (admite texto con marcado), acción principal, acción secundaria opcional, `destructive` (acción principal en rojo) y `dismissible`.
2. **Resultado.** Por la API de resultados de fragmentos (`setFragmentResult`); un helper en `Fragment` y en `FragmentActivity` registra el listener y entrega `PRIMARY`, `SECONDARY` o `DISMISSED`. Un cierre por arrastre o toque exterior entrega `DISMISSED`.
3. **No cancelables** (`dismissible = false`: sin asa, sin arrastre, sin toque exterior ni botón Atrás; solo se sale con sus botones): desactivar cuenta, cerrar sesión, verificar email, aceptar política y el aviso de notificaciones bloqueadas (que ya era no cancelable). El resto (errores, información) es cancelable.
4. **Avisos de éxito como Snackbar**, no como hoja: «cambios guardados» de Ajustes, PIN cambiado y contraseña cambiada (todos los avisos «Success» sin decisión). Los errores sí son hoja.
5. **Aspecto.** Hoja clara (superficie blanca, texto oscuro) como las pantallas Compose, no la tarjeta oscura de los diálogos antiguos. Es decisión mía; se cambia en un solo sitio.
6. **Migración en tres tandas**, cada una con su commit y prueba en el Samsung: (1) pantallas Compose (Pwd, Forgot, Login, Onboarding), (2) Ajustes, (3) Main, Tab, Pin y Help. Al terminar se borran los layouts `layout_dialog_*` y los `bg_modal_*` que queden sin uso.

## Aproximaciones consideradas

- **Compose `ModalBottomSheet` en cada pantalla.** Descartada: la mitad de las pantallas con diálogos son de Views (Ajustes, Pin, Help, Tab, Main) y no tienen un árbol Compose donde alojarla.
- **`BottomSheetDialogFragment` con layouts XML.** Descartada: repite los seis layouts y no usa el sistema de diseño Compose.
- **`BottomSheetDialogFragment` con contenido Compose (elegida).** Sirve igual desde Views y desde Compose y reutiliza los componentes de `KamleonComponents.kt`.

## Pendiente para quien construya

- Mapa exacto de los 14 sitios y su tipo (información, confirmación, bloqueante), en el plan.
- Cómo comportar el teclado y las barras del sistema transparentes dentro de la hoja.
- Si el marcado de los textos (`<b>`, `<u>`) se conserva con `CharSequence` en el `Bundle`.
