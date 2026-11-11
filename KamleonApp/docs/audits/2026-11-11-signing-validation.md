# Aislamiento de la validación de firma

## Cambio

La carga de signing.properties ya no lanza una excepción global al encontrar campos incompletos o sintaxis inválida. Solo se crea la configuración upload cuando el archivo puede leerse y contiene las cuatro propiedades.

validateProdReleaseSigning comprueba existencia/lectura, campos obligatorios y que storeFile apunte a un archivo legible. Es dependencia de preProdReleaseBuild, validateSigningProdRelease, assembleProdRelease y bundleProdRelease. La validación temprana cubre también la preparación de prodRelease, en lugar de depender únicamente de las tareas agregadas.

Los mensajes no incluyen valores de propiedades. No se modificaron las credenciales locales, claves ni certificado.

## Comprobaciones en copia temporal

Se copiaron exclusivamente los build scripts y el catálogo, con datos sintéticos, a un directorio temporal. Se utilizó el wrapper local del proyecto.

| Caso | Resultado |
| --- | --- |
| Gradle anterior, propiedades incompletas, help | Falla durante configuración: campos ausentes |
| Gradle corregido, propiedades incompletas, help y dry-run de development/prod debug | Exit 0; la validación release no aparece |
| Propiedades incompletas, preProdReleaseBuild | Rechazado por validateProdReleaseSigning con nombres de campos |
| Escape Unicode inválido, help | Exit 0 |
| Escape Unicode inválido, validateProdReleaseSigning | Rechazado con mensaje genérico de lectura/sintaxis |
| Archivo properties ausente, help seguido de preProdReleaseBuild | help se ejecuta; prod release se rechaza por ausencia |
| Campos completos y keystore inexistente | Rechazado con mensaje de archivo legible requerido |
| Campos sintéticos completos y archivo de prueba legible | Validación estructural pasa |

El archivo de prueba no es una clave. La última comprobación demuestra la validación estructural; no demuestra contraseñas, alias ni firma criptográfica. AGP valida estos aspectos al firmar. No se generó un AAB firmado de producción.

## Verificación del proyecto

assembleDevelopmentDebug, assembleProdDebug y testDevelopmentDebugUnitTest terminaron con exit 0.

Durante una ejecución combinada con lint, lintAnalyzeDevelopmentDebugUnitTest falló internamente al intentar leer una fuente Hilt generada de prodDebug que no existía. Esto es una limitación de análisis que reapareció; no se ocultó con baseline ni supresiones. La repetición con `./gradlew --no-daemon :app:lintDevelopmentDebug --console=plain` pasó sin incidencias nuevas (exit 0). Ver la auditoría de lint para el seguimiento de esta limitación.
