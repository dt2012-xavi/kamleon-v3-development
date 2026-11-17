# Diálogos como bottom sheets: plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sustituir los 14 diálogos vivos de la app por un único componente `KamleonSheet` (bottom sheet con contenido Compose) y pasar los avisos de éxito a Snackbar.

**Architecture:** `KamleonSheet : BottomSheetDialogFragment` con contenido Compose (`KamleonSheetContent`), que entrega su resultado por la API de resultados de fragmentos. Un helper de `FragmentManager` la muestra y un helper de `Fragment`/`FragmentActivity` registra el listener. Migración en tres tandas con un commit cada una.

**Tech Stack:** Kotlin, Compose (`KamleonComponents.kt`), Material `BottomSheetDialogFragment`, Fragment Result API, Snackbar.

**Spec:** `docs/superpowers/specs/2026-11-17-bottom-sheets-design.md`

## Global Constraints

- Paquete `com.dynatech2012.kamleonuserapp`; el componente vive en `views/KamleonSheet.kt`.
- Compose solo donde ya hay Compose o en componentes nuevos: la hoja es Compose, las pantallas de Views no se migran.
- Comentarios de código en inglés. Los textos existentes no se cambian; los nuevos van en `res/values/strings.xml`.
- Hoja clara: superficie `@color/white`, texto `kamleon_dark_grey`; botón primario `KamleonRoundButton`; secundario `KamleonTextLink`.
- No cancelables (`dismissible = false`): cerrar sesión, verificar email (Login y Onboarding), aceptar política, notificaciones bloqueadas y el aviso «QR scanned» (su OK navega).
- Éxito → Snackbar (`Snackbar.make(view, text, Snackbar.LENGTH_SHORT)`); error y confirmación → hoja.
- Verificación por tanda: `./gradlew :app:assembleDevelopmentDebug :app:compileDevelopmentDebugAndroidTestKotlin :app:testDevelopmentDebugUnitTest :app:lintDevelopmentDebug` y prueba en el Samsung (`export ANDROID_SERIAL=RZCX21APAGW`; comprobar la fecha del APK antes de instalar).
- Commits con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Sin push. No se tocan `.idea/` ni los docs sin trackear de Xavi.

## Review Focus

1. Mensaje largo con fuente grande: el contenido hace scroll y los botones siguen visibles.
2. Texto con marcado (`<u>`, `<b>`) en el mensaje: se conserva tras pasar por el `Bundle`.
3. Recreación de la pantalla con la hoja abierta: el resultado sigue llegando al listener.
4. Doble toque en el botón principal: el resultado se entrega una sola vez.
5. Hoja no cancelable: ni Atrás, ni arrastre, ni toque exterior la cierran.

---

### Task 1: Componente `KamleonSheet`

**Files:**
- Create: `app/src/main/java/com/dynatech2012/kamleonuserapp/views/KamleonSheet.kt`
- Modify: `app/src/main/java/com/dynatech2012/kamleonuserapp/views/KamleonComponents.kt` (`KamleonRoundButton`)
- Test: `app/src/androidTest/java/com/dynatech2012/kamleonuserapp/KamleonSheetContentTest.kt`

**Interfaces:**
- Produces:
  - `enum class SheetResult { PRIMARY, SECONDARY, DISMISSED }`
  - `data class SheetSpec(val message: CharSequence, val primary: String, val title: CharSequence? = null, val secondary: String? = null, val destructive: Boolean = false, val dismissible: Boolean = true, val linkUrl: String? = null)`: con `linkUrl`, tocar el mensaje abre esa URL con `Intent.ACTION_VIEW` (como hacía el texto de la política).
  - `@Composable fun KamleonSheetContent(spec: SheetSpec, onAction: (SheetResult) -> Unit)`; test tags `sheet_title`, `sheet_message`, `sheet_primary`, `sheet_secondary`, `sheet_handle`.
  - `class KamleonSheet : BottomSheetDialogFragment`
  - `fun FragmentManager.showKamleonSheet(requestKey: String, spec: SheetSpec)`
  - `fun Fragment.onKamleonSheetResult(requestKey: String, onResult: (SheetResult) -> Unit)`: registra en `childFragmentManager` con `viewLifecycleOwner`; llamar en `onViewCreated`. Mostrar con `childFragmentManager.showKamleonSheet`.
  - `fun FragmentActivity.onKamleonSheetResult(requestKey: String, onResult: (SheetResult) -> Unit)`: registra en `supportFragmentManager`; llamar en `onCreate`. Mostrar con `supportFragmentManager.showKamleonSheet`.
  - `KamleonRoundButton(..., destructive: Boolean = false)`: con `destructive` el contenedor es `kmln_red` (pulsado: mismo color al 80 % de opacidad).

- [ ] **Step 1: Write the failing test** `KamleonSheetContentTest` (compile-only, como los demás androidTest): `primaryAndSecondaryReportTheirResult` (clic en `sheet_primary` → `PRIMARY`; en `sheet_secondary` → `SECONDARY`), `secondaryIsOnlyShownWhenGiven` (`sheet_secondary` no existe sin `secondary`), `handleIsOnlyShownWhenDismissible` (`sheet_handle` existe con `dismissible = true`, no existe con `false`), `titleIsOptional` (`sheet_title` no existe sin título).
- [ ] **Step 2: Run to verify it fails**: `./gradlew :app:compileDevelopmentDebugAndroidTestKotlin` → error de compilación (`KamleonSheetContent` sin definir).
- [ ] **Step 3: Implement `KamleonSheetContent`** con los tipos de la sección Interfaces: asa (36x4dp, `kamleon_secondary_grey_40`) solo si `dismissible`; título `kamleonTitle()` centrado; mensaje `kamleonBody()` en `Modifier.verticalScroll` con el marcado convertido a `AnnotatedString` (negrita y subrayado, como `annotatedStringResource` de `PwdScreen.kt`, que se extrae a un helper compartido en este archivo y `PwdScreen` pasa a usarlo); botón primario con `destructive`; secundario con `KamleonTextLink` (azul, 16dp de separación); `navigationBarsPadding()`; padding lateral 24dp, superior 20dp.
- [ ] **Step 4: Implement `KamleonSheet`** (`BottomSheetDialogFragment`): argumentos en `Bundle` (`putCharSequence` conserva el marcado), `requestKey`; `onCreateView` devuelve un `ComposeView` con `AppTheme(useDarkTheme = false)`; ventana con fondo transparente y esquinas superiores de 24dp en la propia hoja; si `!dismissible`: `isCancelable = false` y `behavior.isDraggable = false`, `STATE_EXPANDED`, `skipCollapsed = true`; una bandera `delivered` evita entregar dos veces; `onAction` entrega `setFragmentResult(requestKey, bundleOf("result" to result.name))` y `dismiss()`; `onCancel` (si no se entregó) entrega `DISMISSED`.
- [ ] **Step 5: Implement los tres helpers** de Interfaces; `onKamleonSheetResult` decodifica `"result"` con `SheetResult.valueOf`.
- [ ] **Step 6: Extend `KamleonRoundButton`** con `destructive`.
- [ ] **Step 7: Verify**: `./gradlew :app:assembleDevelopmentDebug :app:compileDevelopmentDebugAndroidTestKotlin :app:testDevelopmentDebugUnitTest :app:lintDevelopmentDebug` → BUILD SUCCESSFUL, «no new issues».
- [ ] **Step 8: Commit** `Add KamleonSheet, a Compose bottom sheet to replace the dialogs`.

---

### Task 2: Tanda 1, pantallas Compose

**Files:**
- Modify: `fragments/PwdFragment.kt`, `fragments/ForgotPwdFragment.kt`, `fragments/LoginFragment.kt`, `fragments/OnboardingFragment.kt`, `res/values/strings.xml` (si hace falta)

**Interfaces:**
- Consumes: Task 1 (`SheetSpec`, `showKamleonSheet`, `onKamleonSheetResult`, `SheetResult`).

Mapa y decisiones (los textos son los actuales):

| Sitio | Hoy | Pasa a |
|---|---|---|
| `PwdFragment` éxito (`dialog_reset_pwd_success_description`) | `layout_dialog_ok` | Snackbar |
| `PwdFragment` error (`pwd_error_generic`, mensajes de Firebase) | `layout_dialog_ok` | Hoja: título `pwd_error_title`, mensaje, primario `dialog_ok` |
| `ForgotPwdFragment` éxito | `layout_dialog_ok` | Snackbar |
| `ForgotPwdFragment` error (`dialog_reset_pwd_error_*`) | `layout_dialog_ok` | Hoja con título y OK |
| `LoginFragment.showReadyDialog(errorType)` | `layout_dialog_ok` | Hoja con título y OK (mismos textos por código) |
| `LoginFragment.showVerificationDialog` (-5) | `layout_dialog_email_verification` | Hoja no cancelable: título `dialog_verify_title`, mensaje `login_alert_description_not_verified_email`, primario `dialog_ok`, secundario `dialog_verify_resend`; `SECONDARY` reenvía como hoy |
| `LoginFragment.showPolicyDialog` (6) | `layout_dialog_policy` | Hoja no cancelable: título `dialog_policy_title`, mensaje `dialog_policy_desc`, primario `dialog_policy_agree` (→ `viewModel.acceptPolicy()`), secundario `dialog_policy_disagree` (→ `finishAffinity`); `linkUrl = UrlConstants.URL_POLICY_APP` |
| `OnboardingFragment.showVerificationDialog` | `layout_dialog_email_verification` | Hoja no cancelable con `dialog_verify_desc` formateado con el email; `PRIMARY` = lo que hacía OK, `SECONDARY` = reenviar |

- [ ] **Step 1: Registrar los listeners** en `onViewCreated` de cada fragmento con `onKamleonSheetResult` (claves distintas por sitio: `"login_error"`, `"login_verify"`, `"login_policy"`, `"onboarding_verify"`, `"pwd_error"`, `"forgot_error"`).
- [ ] **Step 2: Sustituir** cada `AlertDialog.Builder` por `childFragmentManager.showKamleonSheet(key, SheetSpec(...))` y los éxitos por Snackbar (`Snackbar.make(requireView(), ...)`).
- [ ] **Step 3: Política** (`login_policy`): pasar `linkUrl` y comprobar en el Samsung, si se puede provocar el estado 6, que el mensaje abre la política; si no, queda verificado solo por compilación (decirlo en el informe).
- [ ] **Step 4: Verify** (comando de Global Constraints) y prueba en el Samsung con la sesión cerrada: error de Login (email inválido y contraseña incorrecta, solo escribiendo datos locales; **sin credenciales reales**), recuperar contraseña con el campo vacío. Con permiso expreso de Xavi: nada más contra Firebase.
- [ ] **Step 5: Commit** `Show the login-flow dialogs as bottom sheets`.

---

### Task 3: Tanda 2, Ajustes

**Files:**
- Modify: `fragments/SettingFragment.kt`

**Interfaces:**
- Consumes: Task 1.

| Sitio | Pasa a |
|---|---|
| `showSavedDialog` (`dialog_saved_text`) | Snackbar |
| `showLogoutDialog` | Hoja no cancelable, destructiva: mensaje `dialog_logout_msg`, primario `dialog_logout_y`, secundario `dialog_logout_n`; `PRIMARY` ejecuta el cierre de sesión actual |
| `showDateNotAllowedDialog` (`dialog_profile_date_not_allowed`) | Hoja cancelable con OK |
| `showNotificationsBlockedDialog` | Hoja no cancelable: mensaje `setting_noti_blocked_msg`, primario `setting_noti_open_settings`, secundario `dialog_deactive_n` |
| `showReadyDialog` y `showDeleteAccountDialog` | Eliminar: sin llamadas (código muerto) |

- [ ] **Step 1: Registrar listeners** en `initView` (`"logout"`, `"noti_blocked"`, `"date_not_allowed"`).
- [ ] **Step 2: Migrar los cuatro sitios y borrar los dos muertos** con sus imports sin uso.
- [ ] **Step 3: Verify** (comando de Global Constraints) y prueba en el Samsung con sesión de Xavi: interruptor de notificaciones (revocar el permiso con `adb shell pm revoke` y volver a concederlo después), cambiar un dato y ver el Snackbar. **No pulsar «Log Out»** salvo permiso expreso; comprobar que la hoja de cierre aparece y se cancela con su botón.
- [ ] **Step 4: Commit** `Show the settings dialogs as bottom sheets`.

---

### Task 4: Tanda 3, Main, Tab, Pin y Help; limpieza

**Files:**
- Modify: `activities/MainActivity.kt`, `fragments/TabFragment.kt`, `fragments/PinFragment.kt`, `fragments/HelpFragment.kt`
- Delete: `res/layout/layout_dialog_*.xml`, y los `res/drawable/bg_modal_*.xml` y colores que queden sin referencias (comprobar con `rg`)

**Interfaces:**
- Consumes: Task 1 (`FragmentActivity.onKamleonSheetResult` para `MainActivity`).

| Sitio | Pasa a |
|---|---|
| `MainActivity.showScannedDialog` | Hoja no cancelable: título `dialog_scan_scanned`, mensaje vacío admitido, primario `dialog_ok` |
| `TabFragment.showScannedDialog` | Igual; `PRIMARY` ejecuta la navegación de hoy (cierra `qrFragment`, `selectTab(0)`, vuelve a Home) |
| `PinFragment` éxito | Snackbar (`dialog_reset_pin_success_description`) |
| `PinFragment` errores | Hoja con título `pin_error_title` y OK |
| `HelpFragment` éxito | Snackbar |
| `HelpFragment` error (`dialog_reset_pin_error_*`) | Hoja con título y OK |

- [ ] **Step 1: Migrar** los sitios de la tabla con los listeners (`"scanned"`, `"pin_error"`, `"help_error"`).
- [ ] **Step 2: Borrar** los seis layouts y, tras `rg` de referencias, los `bg_modal_*` y colores huérfanos; actualizar la entrada de `lint-baseline.xml` solo si lint lo pide.
- [ ] **Step 3: Verify** (comando de Global Constraints) y prueba en el Samsung: PIN (solo validación local: PIN que no coincide), ayuda sin enviar nada.
- [ ] **Step 4: Documentar** una sección «Hojas en lugar de diálogos» en `docs/runbooks/compose-ui-components.md` (cuándo hoja, cuándo Snackbar, cuándo no cancelable) y **commit** `Show the remaining dialogs as bottom sheets and remove the old layouts`.

## Self-Review

- **Spec:** componente (T1), resultado por Fragment Result (T1), no cancelables (T1 `dismissible`, T2-T4 mapa), Snackbar (T2-T4), aspecto claro (T1), tres tandas (T2-T4), borrado de layouts (T4). Cubierto.
- **Tipos:** `SheetSpec`, `SheetResult`, `showKamleonSheet` y `onKamleonSheetResult` se nombran igual en todas las tareas.
- **Review Focus:** 1 y 2 en T1 (scroll y marcado, a verificar en el Samsung con una hoja de Ajustes y fuente grande); 3, 4 y 5 en T1 `KamleonSheet` (bandera `delivered`, `isCancelable`, listener en `childFragmentManager`).
- **Observación fuera de alcance:** `ForgotPwdFragment` y `PwdFragment` reutilizan `dialog_reset_pwd_success_description` («You've successfully changed your password») también cuando solo se ha enviado el email de recuperación. No se cambia el texto aquí; queda para la revisión de textos.
