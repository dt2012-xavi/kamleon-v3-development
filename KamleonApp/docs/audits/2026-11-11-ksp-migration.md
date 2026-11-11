# Migración de Room y Hilt a KSP

## Decisión y compatibilidad

Se adelanta la migración de procesadores por petición del usuario. Se mantiene AGP 8.13.2, Gradle 8.14.6, Kotlin 2.2.21, Hilt 2.58, Room 2.8.5 y SDK 36 mientras se comprueba KSP 2.3.12.

- [KSP 2.3.12](https://github.com/google/ksp/releases/tag/2.3.12) exige AGP 8.12 o posterior; el proyecto usa 8.13.2.
- Se evita [KSP 2.3.13](https://github.com/google/ksp/releases/tag/2.3.13): la release publica una regresión de ejecución concurrente.
- [Hilt 2.59+](https://github.com/google/dagger/releases/tag/dagger-2.59) exige AGP 9 para su plugin. Actualizar Hilt se tratará con el salto de toolchain.
- La documentación actual de Dagger considera estable su soporte KSP a partir de [Dagger 2.60 y KSP 2.3.9](https://dagger.dev/dev-guide/ksp.html). Esta migración mantiene Hilt 2.58 para AGP 8, por lo que la validación de código generado y los builds son obligatorios; no equivale a adoptar la combinación estable más reciente.

## Alcance y comprobación

1. Exportar esquemas versión 2 de las tres bases con KAPT antes de cambiar procesadores.
2. Sustituir los procesadores Room y Hilt por KSP y retirar el plugin KAPT.
3. Comparar esquemas exportados: entidades, índices, SQL e identityHash deben permanecer iguales.
4. Verificar generación Room/Hilt, builds de los cuatro flavors, unit tests y lint aislado.

No se cambia la versión de las bases ni se introducen migraciones destructivas. La exportación de esquemas se configura mediante el plugin Room, compartiendo la versión del runtime/compiler.

## Revisión de manifest

KSP es una herramienta de compilación y no requiere permisos adicionales ni cambios de componentes. Siguen pendientes del bloque de manifest ya planificado:

- Retirar ACCESS_COARSE_LOCATION y WRITE_EXTERNAL_STORAGE, el permiso inexistente del FileProvider y requestLegacyExternalStorage.
- Sustituir root-path por el directorio específico de fotos; conservar el provider privado y sus grants temporales.
- Usar authority basada en applicationId en manifest y captura.
- Restringir el enlace a /scan exacto y aplicar la política de backup aprobada.
- Revisar recuperación ante denegación permanente de cámara/notificaciones cuando se trabaje en esos flujos.

La revisión de permisos usa android-permissions-security. No se interpreta como una auditoría completa de todas las rutas IPC.

## Resultado

- Los cuatro assemble debug (internal, development, preProduction, prod) pasan, así como assembleDevelopmentDebugAndroidTest.
- testDevelopmentDebugUnitTest pasa (DebugLogTest 1/1).
- Las implementaciones Room se generan como Kotlin en build/generated/ksp/<variant>/kotlin; las factories de Hilt se generan en el árbol KSP y el grafo Hilt compila.
- No quedan referencias al plugin o dependencias KAPT en la configuración de build.
- Los JSON de las tres bases versión 2 coinciden completamente entre KAPT y KSP, también después de compilar los cuatro flavors:
  - MeasureDatabase: 97a14ad8add07ba429e534e38a22d857
  - AverageDailyMeasureDatabase: 5d8d34a792a4127584e6d0912d56f702
  - AverageMonthlyMeasureDatabase: f36c11e3241c86d7757525da31b3a811
- No se modificaron entidades, DAOs ni versiones de bases. Los esquemas se incorporan al repositorio para futuras migraciones.
- Lint development aislado pasa sin incidencias nuevas; se retira solamente la entrada de baseline del plugin KAPT eliminado. Quedan 443 entradas (435 errores y 8 hints).
- La sincronización Gradle de Android Studio termina correctamente.
- connectedDevelopmentDebugAndroidTest se intentó: APK compilado, ejecución bloqueada únicamente por No connected devices. No se afirma verificación runtime de las bases ni de la inyección en dispositivo.
- El manifest permanece igual: el saneamiento indicado sigue pendiente, pero no bloquea el cambio de procesador.
- AGP 9 sigue siendo una migración separada para poder actualizar Hilt a su rama con soporte KSP declarado estable. No se afirma que Hilt 2.58 sea esa rama.

