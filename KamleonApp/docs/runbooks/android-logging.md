# Logging en Android

Timber 5.0.1 se declara en `gradle/libs.versions.toml` y se instala con `Timber.DebugTree()` en `MyApplication` solo cuando `BuildConfig.DEBUG` es verdadero. No hay árbol de producción. `DebugLog` conserva su API para código existente, pero ahora delega en Timber y mantiene su comprobación de debug.

## Convención para nuevas trazas

- Registrar transiciones y resultados que ayuden a localizar el paso que falló: inicio de sesión, validación, creación de cuenta, carga de perfil, petición de correo, recepción de enlace y navegación.
- Escribir mensajes breves y estables, en inglés. Preferir el nombre de la operación y su resultado; usar la clase de excepción si hace falta clasificarla.
- No incluir contraseñas, PIN ni hashes, tokens, email, nombre, UID, identificadores de sesión/unidad, URL o consulta de un enlace, payload de notificación/QR, objeto de usuario, consentimiento, medidas ni fechas de nacimiento. Tampoco registrar excepciones completas en estos recorridos, porque sus mensajes pueden contener datos de la petición.
- Evitar logs en bucles por cada registro. Un recuento agregado puede ser suficiente si no identifica a una persona.
- Un log solo confirma un evento observado por el cliente; no implica que el backend haya completado una operación posterior.

## Alcance de esta pasada

Se han migrado a Timber los mensajes activos del arranque, login, registro, privacidad, onboarding, repositorio de autenticación y Cloud Functions, y se han saneado trazas sensibles detectadas en QR, FCM, PIN, perfil y medidas. Persisten llamadas directas a `android.util.Log` en otras pantallas; su migración requiere una auditoría independiente para no cambiar diagnósticos no relacionados. Por ello, `LogNotTimber` está desactivada de forma específica en `app/build.gradle`; las demás reglas de lint y de Timber siguen activas. Retirar esa excepción cuando terminen las migraciones de llamadas directas. No añadir un árbol de producción sin definir antes destino, retención y política de datos.

Validar tras cambios de logging con `./gradlew :app:assembleDevelopmentDebug :app:testDevelopmentDebugUnitTest` y ejecutar lint por separado. Para revisar el uso en dispositivo, filtrar Logcat por la clase o por un evento sin introducir datos reales en los mensajes.
