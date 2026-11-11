# Manifest, FileProvider y backup

## Cambios

- Retirados ACCESS_COARSE_LOCATION, WRITE_EXTERNAL_STORAGE, el permiso inexistente fileprovider.READ y requestLegacyExternalStorage. La producción ya no consume ubicación ni escritura en almacenamiento compartido.
- Camera y POST_NOTIFICATIONS se conservan, así como las features de cámara opcionales.
- FileProvider privado con grants temporales: authority basada en ${applicationId}, usada mediante packageName en MainActivity.
- Única raíz compartible: getExternalFilesDir(DIRECTORY_PICTURES)/my_images, publicada como profile_images. Se eliminan root-path y las rutas images sin consumidores.
- App link restringido a /scan exacto. Hosts, application ID y configuración Firebase sin cambios.
- allowBackup=false. Se conservan los XML con exclusiones explícitas para todos los dominios de almacenamiento, tanto cloud-backup como device-transfer. Se evita depender solo del flag en fabricantes que permiten D2D aunque esté desactivado.
- largeHeap, orientación, launcher exportado y componentes privados se conservan.

## Ajustes al plan

El spec exige desactivar el backup; el plan proponía borrar las reglas vacías. Se sustituyen por exclusiones explícitas porque [Android documenta que allowBackup=false puede no desactivar D2D](https://developer.android.com/identity/data/autobackup). No se garantiza el comportamiento de herramientas propietarias de migración de fabricantes fuera de las APIs Android.

Las pruebas de FileProvider cubren la URI de perfil, archivos privados, un directorio hermano y traversal con '..'. AppContextTest ya cubre el application ID, por lo que no se duplica esa aserción. No necesitan crear ni borrar fotos reales: FileProvider resuelve la ruta canónica de un archivo representado.

Se sigue [la recomendación oficial de eliminar root-path y limitar directorios](https://developer.android.com/privacy-and-security/risks/file-providers) y la guía local android-permissions-security.

## Verificación

- assembleInternalDebug, assembleDevelopmentDebug, assemblePreProductionDebug y assembleProdDebug: exit 0.
- assembleDevelopmentDebugAndroidTest y testDevelopmentDebugUnitTest: exit 0.
- Inspeccionados los cuatro manifests fusionados: package común, hosts correctos, ruta exacta, backup desactivado, authority común derivada del application ID y ausencia de los permisos legacy retirados.
- connectedDevelopmentDebugAndroidTest se intentó antes de modificar producción: No connected devices. No se observó un fallo funcional RED ni una ejecución GREEN de las pruebas instrumentadas. adb devices y emulator -list-avds no muestran dispositivos ni AVDs. Ejecución externa pendiente.
- ./gradlew --no-daemon :app:lintDevelopmentDebug --console=plain: exit 0, sin incidencias nuevas. Retirada solo la entrada ScopedStorage resuelta; baseline 442 (434 errores y 8 hints), sin entradas obsoletas reportadas.

## Pendiente externo y seguimiento

- Ejecutar ./gradlew :app:connectedDevelopmentDebugAndroidTest con emulador/dispositivo.
- Probar captura/selección de foto y /scan?unitId=...&sessionId=... con un enlace válido; comprobar que /scan/extra no pertenece al filtro de la app.
- Verificar la asociación de dominios en cada entorno; el manifest no prueba los assetlinks del servidor.
- Confirmar en hardware recepción FCM y recuperación de permisos.
- El manifest fusionado conserva permisos añadidos por librerías (entre ellos AD_ID y AdServices). Su necesidad y el comportamiento de Analytics requieren una revisión separada, sin asumir que la ausencia de declaraciones propias elimina la recopilación del SDK.
