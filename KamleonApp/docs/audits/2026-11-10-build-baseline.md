# Línea base de build — 2026-11-10

## Estado inicial

- Rama de trabajo aislada: `codex/gradle-modernization`, creada desde `bf0e90e`.
- El checkout original ya contenía `app/build.gradle` con `versionCode 2028` (frente a 2026 en HEAD), además de cambios ajenos en `.idea/` y `.DS_Store`. El valor 2028 se conservó en la copia de trabajo.
- AGP 8.13.0, Gradle 8.14.3, Kotlin 2.2.10, Java target 17, `compileSdk`/`targetSdk` 36, `minSdk` 26, `versionName` 1.0.15.
- SHA-256 de `app/src/stagging/google-services.json`: `347382585973379c53a9b904b8e78d047243d232e0baf5f00c3157f30ee25c5a`.

| Flavor | Proyecto Firebase del JSON | Package fusionado | Host `/scan` |
| --- | --- | --- | --- |
| `internal` | `kamleon-internal` | `com.dynatech2012.kamleonuserapp` | `kamleon-v3-internal.vercel.app` |
| `development` | `kamleon-development` | `com.dynatech2012.kamleonuserapp` | `dev.kamleon.app` |
| `stagging` | `kamleon-preproduction` | `com.dynatech2012.kamleonuserapp` | `pre.kamleon.app` |
| `prod` | `kamleon-production` | `com.dynatech2012.kamleonuserapp` | `kamleon.app` |

El manifest fusionado de `prodDebug` confirma `minSdkVersion=26`, `targetSdkVersion=36`, package y host anteriores. El JSON de `main` también apunta a producción; los builds por flavor resolvieron su JSON específico.

## Verificación ejecutada

Desde `KamleonApp/`:

```bash
./gradlew :app:assembleInternalDebug :app:assembleDevelopmentDebug :app:assembleStaggingDebug :app:assembleProdDebug :app:testDevelopmentDebugUnitTest :app:lintDevelopmentDebug --console=plain
```

Resultado: `BUILD SUCCESSFUL in 1m 53s`, 298 tareas ejecutadas; 2 unit tests, 0 fallos. Lint: 431 advertencias, 0 errores. Son hallazgos anteriores a la modernización. Gradle avisa que se usan funciones obsoletas incompatibles con Gradle 9. La compilación también mostró advertencias Kotlin preexistentes, entre ellas casts no comprobados en `KamleonGraphView`.

No se ejecutaron pruebas instrumentadas ni manuales de cámara/QR/notificaciones en esta línea base.

## Actualización Firebase (tarea 4)

- `firebase-messaging-ktx:24.1.2` se sustituyó por `firebase-messaging` sin versión propia, gestionado por el BoM. Firebase dejó de publicar los módulos KTX independientes; las extensiones Kotlin se integraron en los módulos principales.
- BoM objetivo: **34.19.0**, la última versión de la serie 34 antes del salto mayor 35. Se mantienen Kotlin 2.2.10 y AGP 8.13.0.
- Plugins: Google Services **4.5.0** y Crashlytics Gradle **3.0.8**.
- Se intentó BoM 35.1.0: Firebase Auth y Play Services Measurement aportaron metadatos Kotlin 2.4 incompatibles con el compilador 2.2.10. Kotlin 2.4.21 eliminó ese error, pero Hilt 2.57.1 falló al procesar metadatos 2.4. Se descartó combinar esta subida con la migración mayor de Hilt/AGP; se deja para una tarea separada.

## Actualización AndroidX, Compose, Room, Hilt y CameraX (tarea 5)

- Compose BoM **2026.06.00** en `app`, `datepicker` y `wheelview`; se retiraron las versiones Compose fijas de las dos librerías locales. BoM 2026.09.00 exige Android 37 y AGP 9.1, por lo que no corresponde al toolchain actual.
- Room runtime/compiler/ktx **2.8.5**, Hilt plugin/runtime/compiler **2.58** y todos los artefactos CameraX declarados **1.6.2**. Hilt 2.59 y posteriores exigen AGP 9; 2.58 es la última rama apta para AGP 8 según sus notas de versión. Room 2.8.5 corrige una operación sobre bases cerradas; CameraX 1.6.2 incluye cambios de la pila de cámara desde 1.5.
- Activity **1.13.0** en app y datepicker, Fragment **1.9.1**, AppCompat **1.8.0** en los tres módulos y Core SplashScreen **1.2.0**. Navigation sigue en **2.9.4**: 2.10.2 arrastra Navigation Compose y Lifecycle Compose que exigen AGP 9.1/Android 37. Core KTX sigue en 1.17.0 y Lifecycle runtime en 2.9.3 por el mismo límite de API/AGP observado en sus versiones nuevas.
- La compilación de los cuatro flavors y las pruebas locales verifican APIs y empaquetado. `adb devices` no encontró dispositivo; quedan pendientes las pruebas manuales de cámara/QR y notificaciones en Android 11 o superior antes de publicar.

## Toolchain y CI (tarea 6)

- AGP **8.13.2**, Kotlin **2.2.21**, Gradle wrapper **8.14.6**, JDK **17**, `compileSdk`/`targetSdk` **36**. Son revisiones de mantenimiento dentro de las ramas ya usadas. AGP 8.13 admite API 36.1 como máximo. El salto a AGP 9/Android 37 se separa porque afecta al plugin Hilt, al procesamiento de anotaciones y a las dependencias Compose/Navigation recientes.
- Gradle 8.14.6 corrige vulnerabilidades de la comunicación cliente/daemon y de resolución de repositorios presentes en 8.14.3. Se regeneró el wrapper con su propia tarea y se fijó el SHA-256 oficial de la distribución binaria.
- `.gitlab-ci.yml` se colocó en la raíz Git, fuera de `KamleonApp/`. Usa una imagen Android SDK 36/JDK 17 con fecha fija, ejecuta cuatro builds debug, tests y lint en merge requests y en la rama predeterminada. GitLab CI Lint del proyecto respondió `valid: true` sin errores ni avisos. El primer pipeline real terminó en verde (924 segundos), con dos informes JUnit y lint adjuntos. Se activó **Pipelines must succeed** en el proyecto.

## Firma local de producción (tarea 7)

- El AAB de producción anterior en el checkout original está firmado con certificado SHA-256 `A2:44:20:A3:28:9C:E0:84:DD:86:1D:C3:C2:DE:FA:12:A1:00:00:9D:54:77:99:BC:5D:FC:36:9C:EC:93:DC:2B` (comprobado con `keytool -printcert -jarfile`). Se debe contrastar con el certificado de **clave de subida** en Play Console.
- Se añadió `signing.properties.example`, se ignora `signing.properties` y `prodRelease` falla pronto si falta el archivo local. Los cuatro builds debug, tests y lint siguen en verde sin credenciales.
- La generación de un nuevo AAB firmado queda pendiente de identificar la clave local correcta y cargar sus contraseñas en `signing.properties` sin exponerlas en Git o en el chat. Las pruebas manuales en dispositivo también siguen pendientes.
