# Puerta de calidad de Android Lint

## Política

La tarea `./gradlew :app:lintDevelopmentDebug` falla ante errores y advertencias nuevas. `app/lint-baseline.xml` registra incidencias existentes; no desactiva detectores. La configuración incluye las librerías locales y las fuentes de tests (`checkDependencies` y `checkTestSources`).

Ejecutar esa tarea en las comprobaciones previas a integrar cambios. Este cambio configura Gradle; no añade un pipeline CI.

No regenerar el baseline para hacer pasar una regresión. Corregir el código o justificar una excepción concreta revisada. Cuando se resuelva deuda, eliminar sus entradas obsoletas de forma revisable. Actualizaciones de herramientas/dependencias pueden cambiar las incidencias: comparar los informes antes de actualizar el baseline.

## Estado observado

- En la recuperación inicial, el fallo interno FIR histórico no se reprodujo: lintAnalyzeDevelopmentDebugAndroidTest y lintDevelopmentDebug pasaron antes de modificar los tests. No se atribuye su desaparición a un cambio de package.
- En una verificación posterior combinando builds development/prod y lint, reapareció un error interno de lint en DebugLogTest: FileNotFoundException de una fuente Hilt generada de prodDebug. La repetición independiente con `./gradlew --no-daemon :app:lintDevelopmentDebug --console=plain` pasó sin incidencias nuevas. Usar esta invocación aislada para las comprobaciones de lint mientras se investiga la interacción multivariante; una ejecución aislada correcta no demuestra que el bug del analizador esté corregido.
- Reemplazado ExampleInstrumentedTest, que esperaba el application ID equivocado, por AppContextTest con el package real.
- Retirado ExampleUnitTest (aritmética de plantilla). Se conserva DebugLogTest.
- Baseline inicial: 450 entradas; tras completar el catálogo queda en 444 (436 errores y 8 hints). Se retiran seis UseTomlInstead resueltos y una entrada OldTargetApi ya no emitida al centralizar el SDK; se añade el aviso de versión del KAPT ya existente, previsto para la migración. Ver [cierre del catálogo](2026-11-11-catalog-completion.md). Inicialmente se reportaban 442 errores y 8 hints. La mayor parte son advertencias promovidas a error; la cifra no representa 442 fallos de compilación.
- Tras migrar Room/Hilt a KSP, se elimina la entrada obsoleta de KAPT: quedan 443 entradas (435 errores y 8 hints), sin incidencias nuevas. Ver [migración KSP](2026-11-11-ksp-migration.md).
- Revisión de IDs: sin LintError, SecurityException, UnsafeProtectedBroadcastReceiver, ExportedService, ExportedReceiver ni entradas de FileProvider/file_paths.
- Tras el saneamiento del manifest se retira ScopedStorage resuelto: quedan 442 entradas (434 errores y 8 hints), sin incidencias nuevas. Ver [manifest y FileProvider](2026-11-11-manifest-hardening.md); las pruebas instrumentadas siguen pendientes de dispositivo.

## Deuda prioritaria visible

- MutableCollectionMutableState: datepicker/DataPickerComposeView usa mutableStateOf con ArrayList. Revisar cómo actualiza y expone values para garantizar recomposición, preservando la API pública; requiere corrección y prueba funcional separadas. Queda registrada en el baseline histórico, no resuelta.
- ScopedStorage: resuelto al retirar WRITE_EXTERNAL_STORAGE en el bloque de manifest.
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

