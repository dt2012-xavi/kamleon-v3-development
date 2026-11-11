# Publicación Android local

La CI de GitLab comprueba los cuatro entornos, pero no recibe la clave de subida. El AAB `prodRelease` se genera y firma en el ordenador que guarda esa clave. No se publica automáticamente.

## 1. Preparar la firma una sola vez

1. Copia `signing.properties.example` a `signing.properties` en la raíz de `KamleonApp/`. Este último archivo está ignorado por Git.
2. Rellena `storeFile` con la ruta **absoluta** del keystore de subida, `keyAlias`, `storePassword` y `keyPassword`. No pongas estos datos en comandos, tickets, GitLab ni este documento. Restringe los permisos del archivo a tu usuario (`chmod 600 signing.properties`).
3. Hay varias claves locales con nombre Kamleon. Usa la clave cuyo certificado público coincida con el AAB de producción ya firmado. Su SHA-256 es `A2:44:20:A3:28:9C:E0:84:DD:86:1D:C3:C2:DE:FA:12:A1:00:00:9D:54:77:99:BC:5D:FC:36:9C:EC:93:DC:2B`. Comprueba además en **Play Console > Configuración > Integridad de la app > Certificado de clave de subida** que esta es la huella esperada. No confundas la clave de subida con la clave de firma que gestiona Google Play.

Los builds debug y los otros entornos no necesitan la clave: pueden configurarse aunque `signing.properties` falte, esté incompleto o tenga sintaxis inválida. La tarea `validateProdReleaseSigning`, conectada también a `preProdReleaseBuild`, comprueba para prod release que el archivo pueda leerse, que estén las cuatro propiedades y que el keystore exista y sea legible. Los errores muestran nombres de campos, nunca sus valores. La validación de contraseñas, alias y contenido criptográfico la realiza después el proceso de firma de AGP; esta comprobación previa no la sustituye.

`assembleProdRelease` y `bundleProdRelease` dependen de esta validación. Así, una configuración incompleta bloquea prod release antes de generar el artefacto. Un dry-run solo muestra el grafo; no ejecuta ni demuestra la validez de la firma.

## 2. Generar y verificar

Desde `KamleonApp/`:

```bash
./gradlew :app:bundleProdRelease --console=plain
jarsigner -verify app/build/outputs/bundle/prodRelease/app-prod-release.aab
keytool -printcert -jarfile app/build/outputs/bundle/prodRelease/app-prod-release.aab
```

Comprueba que `jarsigner` indique `jar verified` y que la huella SHA-256 de `keytool` coincida con la indicada arriba y con Play Console. Comprueba también el manifest fusionado `app/build/intermediates/merged_manifests/prodRelease/processProdReleaseManifest/AndroidManifest.xml`: paquete `com.dynatech2012.kamleonuserapp`, host `kamleon.app`, `minSdk` 30, `targetSdk` 36, `versionCode` 2028 y `versionName` 1.0.15 para esta entrega. `app/build/intermediates/bundle_ide_model/prodRelease/produceProdReleaseBundleIdeListingFile/output-metadata.json` identifica el AAB generado. Incrementa código y nombre de versión en futuras publicaciones.

Antes de subirlo, prueba en un dispositivo Android 11 o posterior el arranque, login, lectura de QR, deep link `/scan`, notificaciones y el flujo principal de medidas. No había un dispositivo conectado durante la preparación de esta entrega.

## 3. Publicar manualmente

1. Revisa que el merge request esté aprobado y su pipeline verde.
2. En Play Console, abre la pista de publicación elegida y crea una versión nueva.
3. Sube el AAB verificado desde `app/build/outputs/bundle/prodRelease/`, revisa avisos, notas y países, y publica según el proceso del equipo.
4. Tras la publicación, confirma en un dispositivo que la versión instalada, el login, los enlaces `/scan`, el QR y las notificaciones funcionan con producción.

No copies el AAB firmado, el keystore ni `signing.properties` al repositorio o a los artefactos de GitLab.
