# Puerta de calidad de Android Lint

## Política

La tarea `./gradlew :app:lintDevelopmentDebug` falla ante errores y advertencias nuevas. `app/lint-baseline.xml` registra incidencias existentes; no desactiva detectores. La configuración incluye las librerías locales y las fuentes de tests (`checkDependencies` y `checkTestSources`).

Ejecutar esa tarea en las comprobaciones previas a integrar cambios. Este cambio configura Gradle; no añade un pipeline CI.

No regenerar el baseline para hacer pasar una regresión. Corregir el código o justificar una excepción concreta revisada. Cuando se resuelva deuda, eliminar sus entradas obsoletas de forma revisable. Actualizaciones de herramientas/dependencias pueden cambiar las incidencias: comparar los informes antes de actualizar el baseline.

## Estado observado

- El fallo interno FIR histórico no se reprodujo: lintAnalyzeDevelopmentDebugAndroidTest y lintDevelopmentDebug pasaron antes de modificar los tests. No se atribuye su desaparición a un cambio de package.
- Reemplazado ExampleInstrumentedTest, que esperaba el application ID equivocado, por AppContextTest con el package real.
- Retirado ExampleUnitTest (aritmética de plantilla). Se conserva DebugLogTest.
- Baseline inicial: 450 entradas, reportadas con la política estricta como 442 errores y 8 hints. La mayor parte son advertencias promovidas a error; la cifra no representa 442 fallos de compilación.
- Revisión de IDs: sin LintError, SecurityException, UnsafeProtectedBroadcastReceiver, ExportedService, ExportedReceiver ni entradas de FileProvider/file_paths.
- Esto no sustituye la revisión de seguridad pendiente del manifest/FileProvider.

## Deuda prioritaria visible

- MutableCollectionMutableState: datepicker/DataPickerComposeView usa mutableStateOf con ArrayList. Revisar cómo actualiza y expone values para garantizar recomposición, preservando la API pública; requiere corrección y prueba funcional separadas. Queda registrada en el baseline histórico, no resuelta.
- ScopedStorage: WRITE_EXTERNAL_STORAGE obsoleto; su retirada corresponde al bloque de manifest.
- QueryPermissionsNeeded: revisar resolveActivity y visibilidad de paquetes en HelpFragment.
- ExtraText: dos recursos XML contienen un carácter > sobrante.
- Accesibilidad: ContentDescription, LabelFor y ClickableViewAccessibility.
- Resto principal: recursos sin uso, layout, internacionalización, APIs obsoletas y versiones de dependencias.

## Evidencia de la puerta

Se creó temporalmente lint_gate_probe.xml con texto literal. Con el baseline fijo, lintDevelopmentDebug falló (exit 1) por HardcodedText y UnusedResources. Se retiró el archivo temporal sin incorporarlo al baseline.

Verificación final:
- lintDevelopmentDebug + lintAnalyzeDevelopmentDebugAndroidTest + assembleDevelopmentDebug + testDevelopmentDebugUnitTest: BUILD SUCCESSFUL. DebugLogTest: 1 test, cero fallos.
- AndroidTest se compila, pero connectedDevelopmentDebugAndroidTest termina con DeviceException: No connected devices. Ejecución en dispositivo pendiente.
- El archivo temporal se eliminó y no aparece en el baseline.
- No se añadieron supresiones de detectores.
- FileProviderPathsTest sigue pendiente del bloque de manifest; este trabajo se adelantó por petición del usuario.

