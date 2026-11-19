# Capa de red: monitor, clasificador de fallos y `safeCall`

Estado: **borrador pendiente de aprobación** (2026-11-19). Nada de esto está implementado.

## Intención y por qué

Los fallos de red se tratan hoy caso por caso. Cada sitio deduce «esto es la red» de la excepción que
recibe, y cada uno lo hace a su manera:

- arranque: `isTransientFailure`, `isSessionRejected` (`viewmodels/StartupDecision.kt`);
- login: `authErrorKindOf`, `LoginState.NETWORK`, `PROFILE_RETRY`, `POLICY_RETRY` (`viewmodels/AuthErrors.kt`);
- registro y onboarding: el `-6` suelto de `uiState`;
- unos 65 `try/catch` en los repositorios que devuelven `Response.Failure(e)`.

No hay ninguna noción de «estado de la red» (ni `ConnectivityManager`, ni `NetworkCallback`, ni el permiso
`ACCESS_NETWORK_STATE` declarado). Consecuencias ya vistas en los casos 9 y 10 de
`docs/handoffs/2026-11-17-initactivity-review.md`: arrancar sin red espera hasta 10 s antes de avisar, y
nada se entera de que vuelve la red.

Se quiere una sola capa, sostenible, que decida qué es un fallo de red, lo detecte rápido y deje a cada
pantalla reaccionar igual.

## Objetivos

1. Una única definición de «fallo de red / servidor ocupado / sesión rechazada / otro» (`FailureKind`).
2. Un `NetworkMonitor` (estado de la red del sistema, observable) inyectable y falseable en tests.
3. Un `safeCall` que envuelva las llamadas remotas de los repositorios: aplica el monitor, un tiempo
   máximo, convierte excepciones a `Response.Failure` tipado y elimina los `try/catch` repetidos.
4. Una acción del usuario sin red **falla al momento** con un error claro (decisión del usuario).
5. Las lecturas pueden reintentarse solas al volver la red; las acciones nunca.
6. Migración por etapas, cada una publicable y con tests, sin tocar los ~65 sitios de golpe.

## No objetivos y antiobjetivos

No objetivos: cola de acciones sin red (offline-first, WorkManager); reintentos automáticos de
escrituras; cambiar OkHttp o el SDK de Firebase; cambiar la forma de `Response`/`ResponseNullable`;
envolver Room o `InternalStorage` (son locales); traducciones.

Antiobjetivos (el cambio fracasa aunque «funcione» si pasa esto):

- un falso `Offline` del monitor deja a un usuario sin poder entrar ni reintentar;
- una escritura se aplica dos veces, o se aplica después de que el usuario ya vio un error y no se sabe;
- se traga una `CancellationException`, o se registran datos del usuario o de salud en logs.

## Restricciones

- `minSdk = 30`: `registerDefaultNetworkCallback` está disponible. Hilt con `@Provides` manuales en
  `di/AppModule.kt` (los repositorios se construyen allí a mano; añadir parámetros exige tocarlo).
- `AGENTS.md`: no registrar credenciales ni payloads médicos; verificar con tests y el build del flavor.
- Las excepciones de Firebase (`FirebaseFirestoreException.Code`, `FirebaseAuthInvalidUserException`) no
  se pueden construir en tests JVM: la clasificación es **inyectable** en los tests unitarios y la
  clasificación real se prueba en dispositivo con `am instrument` (nunca `connected…AndroidTest`, que
  desinstala la app y pierde la sesión). Mismo patrón ya usado en `SessionRejectedTest`.
- Firestore con persistencia por defecto: una escritura sin red se **encola** y su `await()` no termina
  hasta que el servidor la confirma (comportamiento documentado del SDK; **no probado aquí**). Cloud
  Functions (HTTP) y Firebase Auth sí fallan sin red.

## Alternativas consideradas

- **A. Solo el monitor.** Barato, pero la clasificación y los `try/catch` siguen repartidos.
- **B. Monitor + clasificador único,** adoptado caso a caso. Resuelve el desorden de clasificación sin
  tocar los repositorios.
- **C. Envolver las llamadas de los repositorios con `safeCall`** (monitor + clasificador + tiempo
  máximo + política por tipo de llamada). El riesgo es cambiar el comportamiento de ~65 llamadas y las
  escrituras encoladas de Firestore.

**Elegida por el usuario: C**, con la decisión «una acción sin red falla al momento» (no se encola). C
incluye A y B como capas internas, y los riesgos se acotan con la política de tipos y la migración por
etapas de más abajo.

## Diseño

Paquete nuevo `com.dynatech2012.kamleonuserapp.network`.

### 1. `NetworkMonitor`

```kotlin
enum class NetworkStatus { Online, Offline }

interface NetworkMonitor {
    val status: StateFlow<NetworkStatus>
    val isOnline: Boolean get() = status.value == NetworkStatus.Online
}
```

`AndroidNetworkMonitor` (singleton Hilt, `@ApplicationContext`): `registerDefaultNetworkCallback` una vez
para toda la vida de la app. `Online` = red por defecto con `NET_CAPABILITY_INTERNET` **y**
`NET_CAPABILITY_VALIDATED`. El estado inicial sale de `activeNetwork` y sus capacidades. Declarar
`ACCESS_NETWORK_STATE` en el manifest (permiso normal; Firebase ya lo añade al manifest fusionado, se
hace explícito). Un `FakeNetworkMonitor` para tests.

El monitor es una **pista, no la verdad**: Firestore tiene su propio estado de conexión y puede discrepar
del sistema (portal cautivo → el sistema dice sin Internet; Firestore caído → dice conectado).

### 2. `FailureKind` (clasificador único)

```kotlin
enum class FailureKind { Network, Busy, SessionRejected, NotFound, Server, Unknown }
fun failureKindOf(e: Exception, classify: FirebaseClassifier = RealFirebaseClassifier): FailureKind
```

- `Network`: `NetworkUnavailableException` (nuestra), `NetworkTimeoutException` (nuestra, ver política),
  `TimeoutException`, `IOException`, `FirebaseNetworkException`, Firestore `UNAVAILABLE` /
  `DEADLINE_EXCEEDED`.
- `Busy`: Firestore `ABORTED`, `RESOURCE_EXHAUSTED`, `CANCELLED` (hoy cuentan como transitorios).
- `SessionRejected`: lo de `isSessionRejected` hoy.
- `NotFound`: `UserProfileMissingException`.
- `Server`/`Unknown`: el resto.

`isTransientFailure` pasa a `kind == Network || kind == Busy`; `isSessionRejected` a
`kind == SessionRejected`; `authErrorKindOf` delega su rama de red; `errorCodeOf` (`ErrorCode`, códigos
`KL-AND-NNN`) no cambia. Los tests existentes (`StartupFailureTest`, `StartupDecisionTest`,
`SessionRejectedTest`, `AuthErrorKindTest`) deben seguir pasando sin cambiar su intención.

### 3. `safeCall`

Clase `NetworkGate(monitor: NetworkMonitor)` inyectada en los repositorios remotos
(`FirestoreDataSource`, `RealtimeDataSource`, `CloudFunctions`, `UserRepository`, y la parte remota de
`MeasuresRepository`).

```kotlin
enum class CallKind { Read, Action }

suspend fun <T : Any> NetworkGate.safeCall(
    kind: CallKind,
    trustMonitor: Boolean = true,
    timeoutMs: Long = kind.defaultTimeoutMs,
    block: suspend () -> T,
): Response<T>          // y safeCallNullable(...) para ResponseNullable
```

Reglas:

1. Si `trustMonitor` y el monitor dice `Offline`: devuelve `Response.Failure(NetworkUnavailableException)`
   **sin llamar** (falla al momento, también para `Read`).
2. Si no, ejecuta `block` con `withTimeoutOrNull(timeoutMs)`. Al agotarse, `NetworkTimeoutException`.
3. Cualquier otra excepción (salvo `CancellationException`, que se relanza) se devuelve como
   `Response.Failure(e)` tal cual: el `FailureKind` se calcula al leerla, no al capturarla.
4. No registra el contenido de la llamada: solo `kind` y el nombre de la clase de la excepción.

**Política por tipo:**

- `Read`: lecturas. Los llamadores (ViewModels/UI) pueden reintentarlas al volver la red con
  `retryWhenOnline`. El repositorio nunca reintenta por su cuenta.
- `Action`: toda acción disparada por el usuario o que cambie estado (subir QR, aceptar la política,
  editar el perfil, enviar el correo de verificación, login, cambios de correo y contraseña). Falla al
  momento sin red y **nunca** se reintenta sola.
- Escrituras de Firestore (`Action`): como `set().await()` no falla sin red y queda encolada, el
  monitor se consulta **antes** (regla 1) y existe un tiempo máximo (regla 2) para «conectado pero
  muerto». Un tiempo agotado de una escritura se documenta como **resultado incierto**: la escritura
  puede aplicarse después. Esto es aceptable para las escrituras actuales porque son idempotentes
  (`updateLegal` y `updateUser` fijan valores; `uploadQrId` hace `set` del mismo documento) y Firestore
  conserva el orden de su cola, así que un reintento posterior del usuario queda detrás de la anterior.
  (Razonamiento a verificar en el paso de implementación.)

**`trustMonitor = false`:** lo pasan los reintentos que pulsa el usuario («Retry»), para que un falso
`Offline` nunca impida intentar la llamada real. Solo las funciones de repositorio que usa un
«Retry» reciben un parámetro `trustMonitor: Boolean = true` que reenvían a `safeCall` (al empezar,
`FirestoreDataSource.getUserData`); el resto lo deja en su valor por defecto. Los reintentos
automáticos al volver la red (sección 4) sí usan el monitor, porque solo se disparan cuando ya dice `Online`.

### 4. Reintento al volver la red (solo lecturas)

`suspend fun NetworkMonitor.awaitOnline()` y un helper `retryWhenOnline` para ViewModels/Activities.
`KamleonSheet` gana `FragmentManager.dismissKamleonSheet(requestKey)` (hoy solo se cierra con botones).
Primer uso: la hoja «We couldn't load your account» del arranque, para cerrarse y repetir la
comprobación sola al volver la red (tras la pausa de 600 ms que ya se usa para que el cierre de la hoja
termine antes de poder volver a mostrarla). Solo `RetryableError`; `ServerError` no es de red.

### 5. Llamadas con listeners y con `Flow`

- Las que usan `suspendCoroutine` + listeners (`getUserProfiles`, `updateToken`, `deleteToken`,
  `UserRepository` en parte) se pasan a `Task.await()` dentro de `safeCall`; es un cambio pequeño y deja
  un solo camino.
- Los `Flow`/`callbackFlow` (medidas y medias, Realtime Database) **no necesitan** volver a suscribirse:
  los listeners del SDK se reconectan solos. Solo se mapea el error terminal a `Response.Failure`. (A
  verificar en la implementación.)

### 6. Arranque (cierra el caso 10)

`AuthViewModel.checkLogin`: con `Offline`, no lee el perfil y salta a `RetryableError` al instante (con la
comprobación de caché para «Continue offline», que ya existe). «Retry» llama con `trustMonitor = false`.
Más el reintento automático de la sección 4.

## Migración por etapas

Cada etapa: tests primero, build + lint + tests de dispositivo, comprobación en el Samsung, commit propio.

1. **Cimientos:** `NetworkMonitor` + `FakeNetworkMonitor`, `FailureKind`, `NetworkGate`/`safeCall`,
   `AppModule`, permiso. Las funciones de clasificación existentes pasan a `failureKindOf` sin cambiar
   comportamiento.
2. **Arranque** (sección 6) y `dismissKamleonSheet` + `retryWhenOnline`.
3. **Perfil y autenticación:** `FirestoreDataSource` (usuario), `CloudFunctions`, `UserRepository`.
   Las hojas de login/política/verificación usan `FailureKind` y los errores tipados.
4. **Medidas y QR:** lecturas de Realtime Database y Firestore, `uploadQrId` como `Action`.
5. **`Flow`:** mapeo de errores terminales.

## Pruebas

- **Unitarias (JVM):** `safeCall` con `FakeNetworkMonitor` y tiempo virtual (`runTest`): matriz de
  política (`Read`/`Action`, `trustMonitor`, `Online`/`Offline`, tiempo agotado, cancelación relanzada);
  `failureKindOf` con clasificador inyectado.
- **Dispositivo (`am instrument`):** clasificación real con excepciones de Firebase (ampliar
  `SessionRejectedTest`) y el monitor real con `adb shell svc wifi|data disable|enable`, restaurando la
  red y comprobando `wifi_on=1` / `mobile_data=1`.
- **Mutación:** comprobar que cada test nuevo falla si se rompe la regla que protege.
- **Manual (Samsung):** arranque sin red (hoja inmediata), reconexión con la hoja abierta (se cierra y
  reintenta), «Retry» con el monitor en `Offline` (llama de verdad), subir un QR sin red (error al momento).

## Decisiones del usuario

- Se hace **C** (envolver las llamadas de los repositorios).
- Una acción del usuario sin red **falla al momento**; no se encola.
- El monitor se **consulta, no se obedece**; solo se reintentan solas las **lecturas**.

## Decisiones mías (a revisar)

- Paquete `network` y nombres (`NetworkMonitor`, `NetworkGate`, `safeCall`, `CallKind`, `FailureKind`).
- `Online` = `INTERNET` + `VALIDATED`; el monitor vive toda la app.
- Solo dos tipos de llamada (`Read`, `Action`); login y envío de correo cuentan como `Action`.
- `Response`/`ResponseNullable` no cambian; el `FailureKind` se calcula al leer, no al capturar.
- Tiempo agotado de una escritura = resultado incierto, aceptado por ser idempotente.
- Los `Flow` no se vuelven a suscribir.
- Migración en cinco etapas, empezando por el arranque.

## Lo que queda al implementador

Valores de los tiempos máximos (partir de 10 s para `Read`, el límite actual del arranque, y 15 s para
`Action`, ajustables); si conviene amortiguar el parpadeo `Online`→`Offline` en redes inestables;
comprobar los dos puntos marcados «a verificar» (cola de escrituras de Firestore y reconexión de los
listeners); y revisar cada llamada de los repositorios para asignarle `Read` o `Action`.
