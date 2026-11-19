# Capa de red Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Un `NetworkMonitor`, un clasificador único de fallos (`FailureKind`) y un `safeCall` que envuelve las llamadas remotas de los repositorios, adoptados por etapas empezando por el arranque.

**Architecture:** Paquete `network` con el monitor, los fallos tipados y `NetworkGate` (inyectado en los repositorios remotos). `safeCall` aplica el monitor y un tiempo máximo y devuelve `Response.Failure`; el `FailureKind` se calcula al leer la excepción. Las acciones sin red fallan al momento, solo las lecturas pueden reintentarse al volver la red.

**Tech Stack:** Kotlin, Hilt (`di/AppModule.kt`, `@Provides` manuales), Firebase (Firestore, Auth, Functions, Realtime Database), `ConnectivityManager.registerDefaultNetworkCallback`, JUnit 4 (sin `kotlinx-coroutines-test`: los tests usan `runBlocking` con tiempos cortos, como `ProfileTimeoutTest`).

**Spec:** `docs/superpowers/specs/2026-11-19-network-layer-design.md` (aprobado). Allí `safeCall` aparece como extensión de `NetworkGate`; aquí es un método miembro, equivalente.

## Global Constraints

- `minSdk = 30`. Flavor de trabajo: `development`; paquete `com.dynatech2012.kamleonuserapp`; tests JVM en `app/src/test/java/com/kamleonapp/` (paquete `com.kamleonapp`), de dispositivo en `app/src/androidTest/java/com/dynatech2012/kamleonuserapp/`.
- `Online` = red por defecto con `NET_CAPABILITY_INTERNET` **y** `NET_CAPABILITY_VALIDATED`. Se declara `ACCESS_NETWORK_STATE` en el manifest.
- Tiempos máximos: `Read` 10 s, `Action` 15 s. Dos tipos de llamada: `Read` y `Action`.
- Una acción sin red falla al momento; no se encola. Solo las lecturas se reintentan solas, y solo desde ViewModels/UI, nunca dentro del repositorio.
- El monitor se consulta, no se obedece: un «Retry» del usuario llama con `trustMonitor = false`.
- `Response`/`ResponseNullable` no cambian. `ErrorCode` (`KL-AND-NNN`) no cambia. Room e `InternalStorage` no se envuelven.
- No registrar datos del usuario ni de salud: `safeCall` solo registra `kind` y el nombre de la clase de la excepción. Nunca tragar `CancellationException`.
- Las excepciones de Firebase no se construyen en tests JVM: clasificador inyectable en JVM; clasificación real con `adb shell am instrument` (nunca `connected…AndroidTest`, desinstala la app y pierde la sesión). Tras instrumentar: `adb uninstall com.dynatech2012.kamleonuserapp.test`.
- Pruebas con la red: `adb shell svc wifi disable|enable` y `adb shell svc data disable|enable`; **siempre restaurar** y comprobar `adb shell settings get global wifi_on` y `mobile_data` valen `1`.
- Verificación de cada etapa: `./gradlew -q :app:testDevelopmentDebugUnitTest :app:lintDevelopmentDebug :app:assembleDevelopmentDebugAndroidTest` sin fallos; recuento de tests en `app/build/test-results/testDevelopmentDebugUnitTest/*.xml`.
- Commits terminan con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Textos de interfaz nuevos solo en inglés, en `res/values/strings.xml`.

## Review Focus

1. Un falso `Offline` (VPN o portal cautivo sin validar) no debe impedir un «Retry» del usuario: Task 4 (`trustMonitor = false` llama aunque el monitor diga `Offline`) y Task 5.
2. La lectura de caché del arranque (`Source.CACHE`, para «Continue offline») debe funcionar con el monitor en `Offline`: Task 5 (`trustMonitor = false`, comprobación en dispositivo).
3. Una pantalla que se cierra a mitad de una llamada cancela la corrutina: la `CancellationException` se relanza, no se convierte en fallo: Task 4.
4. Reconexión intermitente con la hoja de error abierta: el reintento automático se dispara una vez, no por cada cambio de estado: Task 6.
5. El mensaje de una excepción puede llevar datos del usuario (un correo): no debe aparecer en los logs de `safeCall`: Task 4.

---

### Task 1: Verificar la suposición de las escrituras encoladas

Decide si la política de `Action` del spec es correcta antes de construir nada sobre ella. Prueba desechable, no se commitea.

**Files:**
- Create (desechable, sin commit): `app/src/androidTest/java/com/dynatech2012/kamleonuserapp/QueuedWriteProbeTest.kt`
- Modify: `docs/superpowers/specs/2026-11-19-network-layer-design.md` (sección «Restricciones»: dejar escrito el resultado)

**Interfaces:**
- Produces: un hecho confirmado o refutado: «sin red, `set().await()` de Firestore no completa y queda encolada».

- [ ] **Step 1: Escribir la sonda** `QueuedWriteProbeTest.queuedWriteDoesNotCompleteOffline`: con la red cortada desde `adb`, lanza `FirebaseFirestore.getInstance().collection("network_probe").document("probe").set(mapOf("a" to 1))`, espera 5 s con `withTimeoutOrNull(5_000) { task.await() }` y afirma que devuelve `null` (no completó ni falló). Al terminar: `FirebaseFirestore.getInstance().terminate().await()` y `clearPersistence().await()` para no dejar la escritura encolada.
- [ ] **Step 2: Ejecutar con la red cortada.** `adb shell svc wifi disable; adb shell svc data disable`, instalar y `am instrument -w -e class com.dynatech2012.kamleonuserapp.QueuedWriteProbeTest …`; después restaurar la red y comprobar `wifi_on=1`, `mobile_data=1`, y desinstalar `.test`. Esperado: `OK (1 test)`.
- [ ] **Step 3: Registrar el resultado** en el spec (una línea bajo «Restricciones»: «Verificado el <fecha> en el Samsung: …»). Si la sonda **falla** (la escritura sí falla sin red), parar y revisar con el usuario la política de `Action` antes de seguir.
- [ ] **Step 4: Borrar la sonda y commitear solo el spec.**

```bash
rm app/src/androidTest/java/com/dynatech2012/kamleonuserapp/QueuedWriteProbeTest.kt
git add docs/superpowers/specs/2026-11-19-network-layer-design.md
git commit -m "docs: record the verified behaviour of offline Firestore writes"
```

---

### Task 2: `NetworkMonitor`

**Files:**
- Create: `app/src/main/java/com/dynatech2012/kamleonuserapp/network/NetworkMonitor.kt`, `…/network/AndroidNetworkMonitor.kt`
- Create (test): `app/src/test/java/com/kamleonapp/FakeNetworkMonitor.kt`, `app/src/test/java/com/kamleonapp/NetworkMonitorTest.kt`
- Create (dispositivo): `app/src/androidTest/java/com/dynatech2012/kamleonuserapp/NetworkMonitorDeviceTest.kt`
- Modify: `app/src/main/AndroidManifest.xml` (añadir `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`), `di/AppModule.kt`

**Interfaces:**
- Produces:
  - `enum class NetworkStatus { Online, Offline }`
  - `interface NetworkMonitor { val status: StateFlow<NetworkStatus>; val isOnline: Boolean get() = status.value == NetworkStatus.Online }`
  - `suspend fun NetworkMonitor.awaitOnline()` (vuelve de inmediato si ya está `Online`)
  - `internal fun networkStatusFor(hasInternet: Boolean, validated: Boolean): NetworkStatus`
  - `class AndroidNetworkMonitor(context: Context) : NetworkMonitor`
  - Test: `class FakeNetworkMonitor(initial: NetworkStatus = NetworkStatus.Online) : NetworkMonitor { fun set(status: NetworkStatus) }`
  - DI: `networkMonitorProvider(@ApplicationContext appContext: Context): NetworkMonitor` en `AppModule` (`@Provides @Singleton`); el proveedor del `NetworkGate` llega en la Task 4.

- [ ] **Step 1: Test que falla** (`NetworkMonitorTest`): `networkStatusFor(true, true) == Online`; `networkStatusFor(true, false) == Offline`; `networkStatusFor(false, true) == Offline`; `awaitOnline` con el falso en `Online` vuelve ya (`withTimeout(500)`); con `Offline` no vuelve hasta que `set(Online)` (lanzar `awaitOnline` en una corrutina, comprobar que sigue activa a los 50 ms, `set(Online)`, comprobar que termina).
- [ ] **Step 2: Ver que falla** — `./gradlew -q :app:testDevelopmentDebugUnitTest --tests 'com.kamleonapp.NetworkMonitorTest'`. Esperado: FAIL (no compila: referencias sin resolver).
- [ ] **Step 3: Implementar** las firmas de arriba. `AndroidNetworkMonitor`: `MutableStateFlow` inicial desde `activeNetwork`/`getNetworkCapabilities`; `registerDefaultNetworkCallback` una vez en `init`, con `onCapabilitiesChanged` y `onLost` actualizando el flujo (`onLost` → `Offline`). Singleton para toda la vida de la app, sin `unregister`.
- [ ] **Step 4: Ver que pasa** el test anterior. Esperado: PASS.
- [ ] **Step 5: Test de dispositivo** `NetworkMonitorDeviceTest`: con la red activa, `AndroidNetworkMonitor(appContext).isOnline == true`. Instalar y `am instrument -e class …NetworkMonitorDeviceTest`; esperado `OK (1 test)`; desinstalar `.test`.
- [ ] **Step 6: Añadir** el `networkMonitorProvider` a `AppModule` y comprobar que la app compila (`./gradlew -q :app:assembleDevelopmentDebug`).
- [ ] **Step 7: Commit** `feat: add the network monitor`.

---

### Task 3: Fallos tipados y `FailureKind`

**Files:**
- Create: `…/network/Failures.kt`
- Modify: `…/viewmodels/StartupDecision.kt` (`isSessionRejected`, `isTransientFailure`), `…/viewmodels/AuthErrors.kt` (`authErrorKindOf`)
- Test: `app/src/test/java/com/kamleonapp/FailureKindTest.kt`; ampliar `app/src/androidTest/…/SessionRejectedTest.kt`

**Interfaces:**
- Produces:
  - `class NetworkUnavailableException : IOException("No network connection")`
  - `class NetworkTimeoutException(timeoutMs: Long) : IOException("Timed out after $timeoutMs ms")`
  - `enum class FailureKind { Network, Busy, SessionRejected, NotFound, Server, Unknown }`
  - `internal fun failureKindOf(e: Exception, firebaseKind: (Exception) -> FailureKind? = ::firebaseFailureKind): FailureKind`
  - `internal fun firebaseFailureKind(e: Exception): FailureKind?` (Firestore `UNAVAILABLE`/`DEADLINE_EXCEEDED` → `Network`; `ABORTED`/`RESOURCE_EXHAUSTED`/`CANCELLED` → `Busy`; `PERMISSION_DENIED`/`UNAUTHENTICATED` y `FirebaseAuthInvalidUserException` → `SessionRejected`; `FirebaseNetworkException` → `Network`; los códigos se comparan dentro de la función, no en una constante, porque el enum de Firestore no carga en JVM)
- Consumes: `UserProfileMissingException` (repositories).

- [ ] **Step 1: Tests que fallan** (`FailureKindTest`, solo excepciones planas y clasificador inyectado): `NetworkUnavailableException`, `NetworkTimeoutException(1)`, `TimeoutException`, `IOException` → `Network`; `UserProfileMissingException` → `NotFound`; `Exception("x")` con `firebaseKind = { null }` → `Unknown`; `firebaseKind = { FailureKind.Busy }` sobre una excepción cualquiera → `Busy` (el inyectado manda sobre `Unknown`, nunca sobre los tipos planos).
- [ ] **Step 2: Ver que falla** con `--tests 'com.kamleonapp.FailureKindTest'`. Esperado: FAIL.
- [ ] **Step 3: Implementar** `Failures.kt`. Orden de `failureKindOf`: tipos planos (`NetworkUnavailableException`, `NetworkTimeoutException`, `TimeoutException`, `IOException`, `UserProfileMissingException`) y después `firebaseKind(e)`, y si es `null`, `Server` solo si la excepción es de Firestore o Functions (`FirebaseFirestoreException`, `FirebaseFunctionsException`), si no `Unknown`.
- [ ] **Step 4: Migrar** `isTransientFailure(e) = failureKindOf(e).let { it == Network || it == Busy }`, `isSessionRejected(e) = failureKindOf(e) == SessionRejected` y la rama de red de `authErrorKindOf` (`failureKindOf(e) == Network` → `AuthErrorKind.Network`). Los tests `StartupFailureTest`, `StartupDecisionTest`, `LoginStateTest`, `OfflineEntryTest` deben pasar **sin cambiar**; si alguno falla, el comportamiento cambió y hay que corregir la migración, no el test.
- [ ] **Step 5: Ampliar `SessionRejectedTest`** (dispositivo) con una prueba `failureKindsOfRealFirebaseExceptions` que afirme el `FailureKind` de las excepciones reales: `UNAVAILABLE`→`Network`, `ABORTED`→`Busy`, `PERMISSION_DENIED`→`SessionRejected`, `FirebaseNetworkException`→`Network`, `INTERNAL`→`Server`. Instalar y ejecutar con `am instrument -e class …SessionRejectedTest`; esperado `OK`; desinstalar `.test`.
- [ ] **Step 6: Suite completa** (verificación global) y commit `refactor: one classifier for network failures`.

---

### Task 4: `NetworkGate` y `safeCall`

**Files:**
- Create: `…/network/NetworkGate.kt`
- Modify: `di/AppModule.kt` (`networkGateProvider`)
- Test: `app/src/test/java/com/kamleonapp/NetworkGateTest.kt`

**Interfaces:**
- Consumes: `NetworkMonitor`, `NetworkUnavailableException`, `NetworkTimeoutException` (Tasks 2 y 3); `Response`, `ResponseNullable`.
- Produces:
  - `enum class CallKind(val defaultTimeoutMs: Long) { Read(10_000), Action(15_000) }`
  - `class NetworkGate(val monitor: NetworkMonitor)` con
    - `suspend fun <T : Any> safeCall(kind: CallKind, trustMonitor: Boolean = true, timeoutMs: Long = kind.defaultTimeoutMs, block: suspend () -> T): Response<T>`
    - `suspend fun safeCallNullable(kind: CallKind, trustMonitor: Boolean = true, timeoutMs: Long = kind.defaultTimeoutMs, block: suspend () -> Unit): ResponseNullable<Nothing>`

- [ ] **Step 1: Tests que fallan** (`NetworkGateTest`, `FakeNetworkMonitor`, `runBlocking`, tiempos de 50 ms): 
  - monitor `Offline` y `trustMonitor = true`: devuelve `Failure(NetworkUnavailableException)` y el `block` **no se ejecuta** (contador en 0), también con `CallKind.Read`;
  - monitor `Offline` y `trustMonitor = false`: el `block` **sí** se ejecuta y su resultado se devuelve (Review Focus 1);
  - monitor `Online`: devuelve `Success` con el valor del `block`;
  - `block` que lanza `IOException("x")`: `Failure` con esa misma excepción;
  - `block` que no termina con `timeoutMs = 50`: `Failure(NetworkTimeoutException)`; una segunda llamada inmediata con `block` rápido devuelve `Success` (Review Focus, escritura colgada);
  - `block` que lanza `CancellationException`: `safeCall` la relanza (`assertThrows`), no devuelve `Failure` (Review Focus 3);
  - logs (Review Focus 5): plantar un `Timber.Tree` que acumule mensajes, hacer fallar el `block` con `IOException("secret@mail.com")` y afirmar que ningún mensaje registrado contiene `secret@mail.com` y que alguno contiene `IOException`.
- [ ] **Step 2: Ver que falla** con `--tests 'com.kamleonapp.NetworkGateTest'`. Esperado: FAIL.
- [ ] **Step 3: Implementar** `NetworkGate`: reglas 1 a 4 del spec (comprobar monitor si `trustMonitor`; `withTimeoutOrNull`; relanzar `CancellationException`; registrar solo `kind` y la clase de la excepción con `Timber.w`). `safeCallNullable` envuelve el mismo núcleo y devuelve `ResponseNullable.Success()`/`Failure(e)`.
- [ ] **Step 4: Añadir** `networkGateProvider(monitor: NetworkMonitor): NetworkGate` a `AppModule` (`@Provides @Singleton`; el del monitor ya existe desde la Task 2).
- [ ] **Step 5: Ver que pasa** `NetworkGateTest`, luego la suite completa. Esperado: PASS.
- [ ] **Step 6: Commit** `feat: add NetworkGate.safeCall`.

---

### Task 5: Arranque con fallo rápido sin conexión

**Files:**
- Modify: `…/repositories/FirestoreDataSource.kt` (constructor `FirestoreDataSource(userRepository: UserRepository, private val gate: NetworkGate)`; `getUserData(source: Source = Source.DEFAULT, trustMonitor: Boolean = true)` sobre `gate.safeCall(CallKind.Read, trustMonitor)`; dentro del bloque: `UserProfileMissingException()` si el documento no existe, `IllegalStateException("User profile could not be parsed")` si no se puede leer), `di/AppModule.kt` (pasar `gate` a `FirestoreDataSource`), `…/viewmodels/AuthViewModel.kt` (`checkLogin(trustMonitor: Boolean = true)`), `…/activities/InitActivity.kt` (`retryStartupCheck` y `recheckVerification` llaman con `trustMonitor = false`)
- Test: `app/src/test/java/com/kamleonapp/StartupFailureTest.kt` (nueva prueba)

**Interfaces:**
- Consumes: `NetworkGate.safeCall`, `NetworkUnavailableException`.
- Produces: `FirestoreDataSource.getUserData(source, trustMonitor)`; `AuthViewModel.checkLogin(trustMonitor: Boolean = true)`.

- [ ] **Step 1: Test que falla**: en `StartupFailureTest`, `startupStateFor(authenticated = true, verified = true, profile = Response.Failure(NetworkUnavailableException()))` es `RetryableError` con los clasificadores por defecto.
- [ ] **Step 2: Ejecutar** `--tests 'com.kamleonapp.StartupFailureTest'`. Esperado: PASS (tras la Task 3 ya clasifica bien; la prueba fija ese comportamiento para el arranque).
- [ ] **Step 3: Implementar** los cambios de la lista. En `checkLogin(trustMonitor)`, la lectura de perfil usa `getUserData(Source.SERVER, trustMonitor)`; la lectura de caché para «Continue offline» usa `getUserData(Source.CACHE, trustMonitor = false)` siempre (Review Focus 2). Mantener `profileOrTimeout` como está (ambos tiempos son de 10 s y los dos son fallos de red). Buscar con `grep -rn "FirestoreDataSource(" app/src` los demás constructores y ajustarlos.
- [ ] **Step 4: Suite completa** y compilar el APK de tests.
- [ ] **Step 5: Comprobación en el Samsung** con una mutación temporal (copia de seguridad en el scratchpad, restaurar después): forzar `authenticated = true` en `checkLogin` y cortar wifi y datos; arrancar y medir con `adb logcat` que la hoja de error sale en menos de 2 s, no a los 10 s. Con la red cortada, pulsar «Retry» debe hacer la llamada de verdad (log de la llamada). Restaurar red y código; comprobar `wifi_on=1`, `mobile_data=1` y que no queda `MUTATION` en el árbol.
- [ ] **Step 6: Commit** `feat: fail fast at startup without a network`.

---

### Task 6: Reintento automático al volver la red

**Files:**
- Modify: `…/views/KamleonSheet.kt` (`fun FragmentManager.dismissKamleonSheet(requestKey: String)`), `…/activities/InitActivity.kt`
- Create: `…/network/NetworkRetry.kt`
- Test: `app/src/test/java/com/kamleonapp/NetworkRetryTest.kt`

**Interfaces:**
- Consumes: `NetworkMonitor.awaitOnline()`, `checkLogin(trustMonitor)`.
- Produces: `suspend fun NetworkMonitor.retryWhenOnline(pauseMs: Long = 600L, action: suspend () -> Unit)` (espera `awaitOnline`, la pausa, y ejecuta `action` **una vez**); `dismissKamleonSheet` cierra la hoja con esa clave si está en pantalla y entrega `SheetResult.DISMISSED`.

- [ ] **Step 1: Tests que fallan** (`NetworkRetryTest`): con el monitor en `Offline`, `retryWhenOnline { count++ }` no ejecuta; tras `set(Online)` ejecuta una vez; alternar `Online → Offline → Online` tres veces rápido antes de que corra la pausa ejecuta **una** vez (Review Focus 4: la corrutina espera una sola vez).
- [ ] **Step 2: Ver que falla** con `--tests 'com.kamleonapp.NetworkRetryTest'`. Esperado: FAIL.
- [ ] **Step 3: Implementar** `retryWhenOnline` y `dismissKamleonSheet`. En `InitActivity`: al mostrar la hoja `STARTUP_ERROR_SHEET`, lanzar `lifecycleScope.launch { monitor.retryWhenOnline { dismissKamleonSheet(STARTUP_ERROR_SHEET); viewModel.checkLogin() } }` y cancelar ese `Job` al pulsar un botón o al parar la actividad; `NetworkMonitor` llega a la actividad por Hilt (`@Inject lateinit var networkMonitor`). Solo para `RetryableError`, no para `ServerError`.
- [ ] **Step 4: Ver que pasa** y suite completa.
- [ ] **Step 5: Samsung**, mutación temporal que fuerza la hoja de error del arranque con la red cortada: reactivar wifi y datos y comprobar que la hoja se cierra sola y se repite la comprobación (log). Restaurar red y código.
- [ ] **Step 6: Commit** `feat: retry the startup check when the network returns`.

---

### Task 7: Perfil y autenticación sobre `safeCall`

**Files:**
- Modify: `…/repositories/FirestoreDataSource.kt` (`createUserStep1`, `createUserStep2`, `updateUser`, `updateUserImage`, `removeUserImage`, `updateToken`, `deleteToken`, `updateLegal`, `getNewInvitations`), `…/repositories/CloudFunctions.kt` (todas), `…/repositories/UserRepository.kt` (`signup`, `login`, `changeEmail`, `changePwd`, `deleteUser`; constructor `UserRepository(gate: NetworkGate)`), `di/AppModule.kt` (`userRepositoryProvider(gate)`, `Realtime/Cloud` con `gate`), `…/viewmodels/AuthViewModel.kt`
- Test: `app/src/test/java/com/kamleonapp/LoginStateTest.kt`, `AuthErrorKindTest` (dispositivo)

**Interfaces:**
- Consumes: `NetworkGate.safeCall`/`safeCallNullable`. Todas son `CallKind.Action` salvo `getUserProfiles` y `getInvitations` y `getNewInvitations` (`Read`).
- Produces: sin firmas públicas nuevas; las funciones mantienen su tipo de retorno.

- [ ] **Step 1: Tests que fallan**: `authErrorKindOf(NetworkUnavailableException()) == AuthErrorKind.Network`; `loginStateForProfileFailure(NetworkUnavailableException(), sessionRejected = { false }) == LoginState.PROFILE_RETRY`; `startupStateFor` con `NetworkTimeoutException(1)` es `RetryableError`.
- [ ] **Step 2: Ejecutar** `--tests 'com.kamleonapp.LoginStateTest' --tests 'com.kamleonapp.StartupFailureTest'`. Esperado: PASS (fijan la clasificación de los fallos de red nuevos antes de migrar los repositorios; si alguna falla, corregir la clasificación de la Task 3).
- [ ] **Step 3: Migrar función a función**, sin cambiar la forma de sus resultados: sustituir el `try/catch` o el `suspendCoroutine` + listeners por `gate.safeCall(kind) { …await() }`; las que usan `suspendCoroutine` con `addOnSuccessListener/addOnFailureListener` (`updateToken`, `deleteToken`, `getUserProfiles`) pasan a `Task.await()`. Conservar los efectos (`user = profile`, guardado en preferencias) dentro del bloque. Una función por commit local mientras se avanza; ejecutar la suite tras cada archivo.
- [ ] **Step 4: Suite completa** y APK de tests.
- [ ] **Step 5: Samsung sin sesión**: con la red cortada, intentar iniciar sesión con credenciales falsas: debe salir «No connection» **al instante** (antes tardaba). Con la red, la hoja habitual de credenciales incorrectas. Restaurar la red.
- [ ] **Step 6: Commit** `refactor: profile and auth repositories use safeCall`.

---

### Task 8: Medidas y QR

**Files:**
- Modify: `…/repositories/FirestoreDataSource.kt` (`getUserMeasuresNoPag`, el `Flow` `getUserMeasures`, `uploadQrId` como `Action`), `…/repositories/RealtimeDataSource.kt` (`getDaysAverages`, `getMonthsAverages` como `Read`; constructor `RealtimeDataSource(userRepository, gate)`), `…/repositories/MeasuresRepository.kt` (solo sus llamadas remotas), `di/AppModule.kt`
- Test: `app/src/test/java/com/kamleonapp/AveragesResultTest.kt`

**Interfaces:**
- Consumes: `NetworkGate.safeCall`.
- Produces: `internal fun <T : Any> averagesResult(results: List<Result<T>>): Response<ArrayList<T>>` (en `…/repositories/RealtimeDataSource.kt`): `Success` con los valores obtenidos si hubo al menos uno o la lista estaba vacía; `Failure(primera excepción)` si **todas** las lecturas fallaron.

Hallazgos que cambian el alcance respecto al spec: los `callbackFlow` de `MeasuresRepository` están comentados (código muerto) y el único `Flow` real es `FirestoreDataSource.getUserMeasures`, que ya emite `Response.Failure` ante un error y termina; no hay listeners de larga vida, así que no existe la suposición de «reconexión de listeners». Además `getDaysAverages`/`getMonthsAverages` hoy devuelven `Success` con una lista vacía aunque **todas** las lecturas fallen (el error de cada `async` se descarta), así que sin red parece «sin datos».

- [ ] **Step 1: Tests que fallan** (`AveragesResultTest`): `averagesResult(listOf(Result.success(1), Result.failure(IOException("x"))))` es `Success([1])`; `averagesResult(listOf(Result.failure<Int>(IOException("x"))))` es `Failure` con esa excepción; `averagesResult(emptyList<Result<Int>>())` es `Success([])`.
- [ ] **Step 2: Ver que falla** con `--tests 'com.kamleonapp.AveragesResultTest'`. Esperado: FAIL.
- [ ] **Step 3: Implementar** `averagesResult` y usarla en `getDaysAverages`/`getMonthsAverages`, cada lectura envuelta con `gate.safeCall(CallKind.Read) { ref…get().await() }` y convertida a `Result`. Revisar los llamadores (`MeasuresRepository.getUserDailyAverages`, `getUserMonthlyAverages` y `MainViewModel`) para que traten el `Failure` como ya tratan los de Firestore (no deben borrar datos locales ni mostrar «sin datos»).
- [ ] **Step 4: Migrar** `getUserMeasuresNoPag` y el `Flow` `getUserMeasures` (la lectura `task.get().await()` pasa a `gate.safeCall(CallKind.Read)`; el `emit(Response.Failure(e))` y el `break` se mantienen) y `uploadQrId` (`Action`; tiempo agotado = resultado incierto, según el spec). Mantener el orden de `MeasuresRepository` (leer la BD local, pedir lo nuevo, guardar).
- [ ] **Step 5: Suite completa** y APK de tests.
- [ ] **Step 6: Samsung con sesión real** (pedir al usuario que entre): subir un QR con la red cortada debe dar la hoja de error de escaneo **al momento** y, al restaurar la red, **no** debe haber aparecido ninguna escritura retrasada (comprobar en la consola de Firebase del entorno de desarrollo); abrir la pantalla de analítica sin red y comprobar que no sale «sin datos» sino los datos locales. Restaurar la red.
- [ ] **Step 7: Commit** `refactor: measures and QR use safeCall`.

---

### Task 9: Cierre y documentación

**Files:**
- Modify: `docs/handoffs/2026-11-17-initactivity-review.md` (caso 10 hecho, capa de red, notas de `MainActivity` siguen vigentes), `docs/superpowers/specs/2026-11-19-network-layer-design.md` (estado «implementado», resultados de las dos verificaciones)

- [ ] **Step 1: Suite completa**, `./gradlew -q :app:assembleDevelopmentDebug`, lint, y recuento de tests.
- [ ] **Step 2: Actualizar** handoff y spec con lo implementado, lo verificado y lo no verificado.
- [ ] **Step 3: Comprobar** `git status --short` y que no queda `MUTATION` en el árbol (`grep -rn MUTATION app/src`); red restaurada en el móvil.
- [ ] **Step 4: Commit** `docs: record the network layer in the handoff`.
