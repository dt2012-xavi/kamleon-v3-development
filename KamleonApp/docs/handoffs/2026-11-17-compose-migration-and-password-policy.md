# Handoff: migración a Compose y política de contraseñas (2026-11-17)

Para el agente que continúe. Rama `codex/gradle-modernization`, worktree `/Users/xavi/.codex/worktrees/ea6d/android-java-kotlin-kamleon-userapp-development/KamleonApp` (el checkout `development` de `/Users/xavi/Developer/android/...` no se ha tocado). Último commit: `fef6d32`. **Nada está pushed ni mergeado.**

## Objetivo del trabajo
Modernizar la app Android Kamleon (`com.dynatech2012.kamleonuserapp`) migrando pantallas XML a Jetpack Compose **sin cambiar funcionalidad**, replicando estilos 1:1 donde tiene sentido y mejorando UI/UX donde Xavi lo ha aprobado. Después, trasladar a la app la política de contraseñas de la web.

## Estado
Migradas a Compose (fragments finos con `ComposeView` + `*Screen.kt`): Login, Forgot password, Register, Privacy, Onboarding. Siguen en XML: Splash, los diálogos `layout_dialog_*` y `PwdFragment` (cambio de contraseña).

Hecho en esta ronda (commits más recientes primero):
- `fef6d32` margen de feedback 14dp; Login solo informa del primer campo vacío.
- `0228eef` / `218123d` el indicador de fuerza queda dentro del bloque del campo y se mantiene visible al escribir; la fila «¿No tienes cuenta? Register» pasa a `FlowRow` (se rompía en vertical con letra grande).
- `b3a418a` política de contraseñas (ver `docs/runbooks/password-policy.md`).
- `a5738b9` errores junto al campo, botón principal en barra fija sobre el teclado, foco al primer campo inválido, «Hecho» envía el formulario, botones ya no se deshabilitan en silencio.
- Antes: Register/Privacy/Onboarding/Forgot/Login en Compose, barras del sistema transparentes, fix del selector de fecha, etc.

## Piezas clave
- `views/KamleonComponents.kt`: sistema de diseño compartido: escala tipográfica (`kamleonTitle/Body/BodySmall/Caption/ButtonText`), `KamleonTextField` (`errorText`, `focusRequester`, `onImeAction`, `supportingContent`, `bringIntoView` con 14dp extra), `KamleonRoundButton`, `KamleonTextLink`, `KamleonProgressBar`, `KamleonFormLayout` (contenido con scroll + barra inferior fija, recibe `imeVisible`), `KamleonSwitchRow`, `KamleonPasswordStrengthMeter`.
- `security/`: `PasswordPolicy` (longitud 8–128 → zxcvbn ≤1 débil → HIBP k-anonymity, timeout 1500 ms, **falla en abierto**), `ZxcvbnPasswordStrengthEstimator`, `HibpPasswordBreachChecker` (`HttpURLConnection`, `Add-Padding: false`), `PasswordPolicyMessages.kt`. Provisto en `di/AppModule.kt`.
- `AuthViewModel.checkRegisterInputs` evalúa la política (códigos `registerInputs`: 4 corta, 5 larga, 7 débil, 8 filtrada, 6 ok) y expone `registerChecking`; `MainViewModel.changePwd` valida antes de reautenticar.
- Docs: `docs/runbooks/compose-ui-components.md`, `docs/runbooks/password-policy.md`, `docs/audits/2026-11-11-system-bars.md`.

## Decisiones tomadas (no reabrir sin hablar con Xavi)
- Campo de texto unificado; 12sp XML → 13sp en Compose; fuentes: Circular Regular/Medium/Bold (los «Book» del XML se pintaban en realidad como Regular).
- La política de contraseñas vive **en el cliente** (la web la aplica en su backend Node). Protege el flujo de la app, no impide crear cuentas llamando directo a Firebase Auth. Mantener `Add-Padding: false` y el fail-open de HIBP salvo decisión explícita.
- `HttpURLConnection` en vez de OkHttp: OkHttp 5.5.1 exige compileSdk 37 y lint (`NewerVersionAvailable`) rechaza versiones anteriores.
- Los botones de Login/Register no se deshabilitan: al pulsar con algo mal explican el error junto al campo y mueven el foco.
- No se añadió `imePadding`: `adjustResize` + `fitsSystemWindows` en las activities ya recortan la ventana; se duplicaría.
- Register muestra todos los avisos de campos vacíos a la vez; Login solo el del primero.

## Verificación (todo en verde al cierre)
```bash
./gradlew :app:assembleDevelopmentDebug :app:compileDevelopmentDebugAndroidTestKotlin :app:testDevelopmentDebugUnitTest :app:lintDevelopmentDebug
```
Lint con baseline (432 errores filtrados, `warningsAsErrors`). Los tests instrumentados **solo compilan**: nunca se han ejecutado en dispositivo porque `connected…AndroidTest` desinstala la app y borra sus datos.

Dispositivo de pruebas: Samsung `RZCX21APAGW` (1080x2340) por adb. Tras construir, **comprueba la fecha del APK** (`stat -f "%Sm"`) antes de instalar: un APK obsoleto ya confundió una verificación. Paquete instalado: `com.dynatech2012.kamleonuserapp`. En zsh no uses variables con varias palabras para `adb -s ...`; usa `export ANDROID_SERIAL=...`. Si cambias `font_scale` para probar, restáuralo a 1.0.

## Reglas de seguridad y de trabajo
- No ejecutar acciones reales contra el backend Firebase dev salvo permiso expreso de Xavi (alta de cuenta, «Send login link», login con credenciales). En la última ronda Xavi autorizó **una** alta en dev: queda la cuenta `pwpolicy.check.1117@example.com` («Ana Prueba») en Auth + `users/<uid>` en Firestore; **Xavi la borra él**. La anterior, `ana@test.co`, también la borra él.
- No tocar `google-services.json`, `local.properties`, certificados ni release. No revertir cambios ajenos: `.idea/` modificado y los docs sin trackear `docs/audits/2026-11-11-qr-follow-up.md`, `docs/superpowers/plans/2026-11-11-build-sustainability-stage-1.md`, `docs/superpowers/specs/2026-11-11-build-sustainability-design.md` son de Xavi y se dejan sin commitear.
- Comentarios en código en inglés; docs pueden ir en español. No loguear credenciales, tokens ni datos médicos.
- Commits con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. No push ni merge sin que lo pida.
- Convenciones del proyecto en `AGENTS.md` (flavors: `stagging` se escribe así).

## Pendiente / siguientes pasos
1. **Revisión de producto de textos nuevos** (solo inglés, sin traducir): `password_*`, `field_error_*`, `signup_error_*_required`, `pwd_error_same_as_current`, `onboard_why_*`, `onboard_step_counter`.
2. **`PwdFragment` a Compose**: hoy solo tiene el rechazo de política por diálogo y «igual que la actual»; falta la barra de fuerza, y de paso el patrón de error junto al campo.
3. Splash y diálogos XML restantes (no pedidos aún).
4. Ejecutar los tests instrumentados en un dispositivo/emulador **dedicado** (borrarán datos de la app).
5. Decidir si Register debe informar solo del primer campo vacío como Login (hoy no).
6. Reset de contraseña por enlace (`sendPasswordEmail`) está fuera del alcance de la política; confirmar con Xavi cómo funciona en la web.
7. Límite conocido: sin protección en servidor, la política es saltable llamando directo a Firebase Auth.
