# Política de contraseñas (app Android)

Traduce a la app la política de la web (longitud + fuerza zxcvbn + breach-check HIBP, sin reglas de composición). En la web se aplica en el servidor Node; en la app la aplica el propio cliente antes de crear la cuenta o cambiar la contraseña.

## Reglas
1. Longitud 8..128 (`PasswordLength`). Se evalúa primero, sin trabajo extra.
2. Fuerza con zxcvbn4j: puntuación ≤ 1 = `WEAK` = rechazo; 2 = `GOOD`; ≥ 3 = `STRONG`. Local, sin red.
3. HIBP Pwned Passwords con k-anonymity (solo salen 5 caracteres del SHA-1), `Add-Padding: false`, timeout total 1500 ms. **Falla en abierto**: timeout, error de red o no 2xx se registra con `Timber.w` (sin datos de la contraseña) y se omite el chequeo.
4. Sin mayúscula/número/símbolo obligatorios.

## Código (`security/`)
- `PasswordPolicy.validate()` devuelve `Valid` o `Rejected(reason)` con `TOO_SHORT | TOO_LONG | WEAK | BREACHED`; `estimateStrength()` alimenta la barra de fuerza.
- `PasswordStrengthEstimator` (`ZxcvbnPasswordStrengthEstimator`) y `PasswordBreachChecker` (`HibpPasswordBreachChecker`, `HttpURLConnection`) son interfaces inyectables; `AppModule` provee `PasswordPolicy`.
- `PasswordPolicyException` lleva el motivo por `Response.Failure`; `messageRes()` lo traduce a texto.

## Dónde se aplica
- **Registro** (`AuthViewModel.checkRegisterInputs`): evalúa la política antes de crear la cuenta (códigos `registerInputs`: 4 corta, 5 larga, 7 débil, 8 filtrada, 6 ok) y expone `registerChecking` para el spinner del botón. `RegisterScreen` muestra la barra de fuerza, el error junto al campo y bloquea débiles/cortas al enviar.
- **Cambio de contraseña** (`MainViewModel.changePwd`): valida la nueva antes de reautenticar y expone `pwdChanging` para el spinner del botón. `PwdScreen` (Compose) muestra la barra de fuerza bajo «nueva contraseña», el motivo del rechazo junto al campo, «igual que la actual» y «no coinciden» mientras se escribe, y la contraseña actual incorrecta (`FirebaseAuthInvalidCredentialsException`) junto a su campo. Otros fallos siguen en diálogo.
- Fuera de alcance: el reset por enlace.

## Límites conocidos
- La política vive en el cliente: protege el flujo de la app, no impide crear cuentas llamando directamente a Firebase Auth.
- Textos nuevos en `strings.xml` (`password_*`, `pwd_error_same_as_current`, `pwd_error_new_required`, `nav_back`) pendientes de revisión de producto y de traducción.
- OkHttp 5.5.1 exige compileSdk 37, por eso se usa `HttpURLConnection`.
