# Modernización incremental del build Android

## Intención

Hacer que las dependencias y la configuración de build sean actuales, comprensibles y repetibles sin introducir una migración de build que no aporte valor al tamaño del proyecto. El proyecto tiene cuatro entornos; solo `prod` distribuye la app pública. No hay fecha de release inmediata: la prioridad es reducir riesgo y deuda verificable.

## Decisiones confirmadas

- Mantener los archivos Gradle en Groovy y modernizarlos por pasos. No convertir ahora a Kotlin DSL ni crear plugins de convención.
- Subir el mínimo de la app a Android 11 / API 30. Conservar compile/target SDK 36 salvo necesidad demostrada de cambio.
- Renombrar el flavor `stagging` a `preProduction` en Gradle, árbol de fuentes, comandos y documentación vigente. Conservar sus valores actuales (`Kamleon-PRE`, sufijo `-pre`, host `pre.kamleon.app`, proyecto Firebase de preproducción), moviendo el `google-services.json` sin cambiar su contenido.
- Conservar un único `applicationId` en `internal`, `development`, `preProduction` y `prod`; no se necesitan instalaciones simultáneas. Conservar los cuatro proyectos Firebase separados.
- Mantener manual la subida a Google Play. El AAB de producción se genera y firma localmente con la clave de subida existente en este ordenador; secretos y rutas de firma no se versionan.
- GitLab es el repositorio principal. Usar GitLab-hosted/instance runners para un CI mínimo cuando estén habilitados; no instalar ni mantener un runner propio como primera opción. CI no recibe la clave de firma ni publica en Play Console.
- Respetar el cambio local existente de `versionCode` a 2028 y los cambios ajenos en `.idea/` y `../.DS_Store`.

## Objetivos

1. Unificar versiones y limpiar declaraciones duplicadas/obsoletas en los tres módulos, con una sola fuente de verdad para versiones compartidas. Elegir versiones estables compatibles mediante notas oficiales, una familia a la vez; evitar subir versiones solo por perseguir el último número.
2. Sustituir el módulo Firebase Messaging KTX discontinuado por el módulo principal y adaptar imports/API si es necesario.
3. Mantener cada flavor compilable y vinculado a su Firebase/host correctos, especialmente después del renombre.
4. Añadir una comprobación GitLab sencilla: builds debug de los cuatro flavors, unit tests y lint de `development`; resultados y fallos visibles en merge requests. Reducir el coste del pipeline si las cuotas de GitLab lo requieren, sin perder un gate de los cuatro entornos antes de integrar.
5. Documentar un proceso de release reproducible: incrementar manualmente `versionCode` y `versionName`, compilar y probar, generar AAB firmado localmente, verificar variante/versión/certificado y subirlo manualmente a Play Console.

## Restricciones y límites

- No cambiar IDs de paquete, hosts, valores Firebase ni credenciales. El traslado del JSON de preproducción es un cambio de ruta, no de contenido.
- No tocar lógica de negocio salvo adaptaciones necesarias de API y tests que las validen.
- No activar R8/minificación en esta fase; requiere un pase separado de reglas y pruebas de release. No introducir publicación automática.
- No reescribir la UI ni consolidar Room. Esos cambios pertenecen a fases posteriores.
- Usar el wrapper local y no editar artefactos generados. Antes de cada grupo, registrar el árbol sucio y preservar cambios ajenos.
- Para minSdk 30, comprobar el impacto sobre usuarios activos en Play Console antes de publicar, aunque el valor técnico ya está decidido para la rama de modernización.

## Alternativas consideradas

- **Elegida: modernización incremental en Groovy.** Menos superficie de cambio; permite atribuir fallos a un grupo de dependencias y preservar releases manuales.
- **Descartada por ahora: Kotlin DSL y plugins de convención junto con el upgrade.** Mejoraría tipado en builds grandes, pero aquí suma migración y debugging sin resolver las versiones duplicadas ni los tests faltantes.

## Forma de trabajo y verificación

1. Registrar baseline en los cuatro flavors; comprobar los `google-services.json` y el manifiesto fusionado por variante.
2. Renombrar preproducción y subir `minSdk`, con verificación de compilación/test. Actualizar `AGENTS.md` y comandos vigentes; los documentos históricos quedan como registro de su fecha.
3. Limpiar Gradle: variables `ext` no usadas, duplicados, dependencias sin uso demostrado y versiones mezcladas de Compose/Firebase/CameraX. Introducir un catálogo de versiones si reduce duplicación sin complicar publicación de las librerías locales.
4. Actualizar dependencias por familias, adaptando código y tests cuando compense. Verificar `assemble` de los cuatro flavors, unit tests de `development` y lint tras cada grupo relevante; probar manualmente QR/cámara/notificaciones cuando cambien sus librerías.
5. Añadir `.gitlab-ci.yml` en la raíz real del repositorio, donde `KamleonApp/` y `GraphViewDemo/` son carpetas hermanas, con `cd KamleonApp` para usar el wrapper. El CI de esta fase no construye `GraphViewDemo`. Probar CI Lint y un pipeline real antes de hacerlo gate de merge.
6. Documentar la firma local mediante archivo ignorado o variables de entorno; no almacenar clave ni contraseñas en el repositorio o en GitLab CI. Probar generación de `bundleProdRelease` local y validación del AAB.

## Criterios de aceptación

- Los cuatro flavors tienen nombres/tareas coherentes y resuelven sus proyectos Firebase esperados; `prod` conserva el `applicationId` y host actuales.
- `minSdk 30` en la app y sin conflicto en módulos locales; builds debug de los cuatro flavors y unit tests/lint de `development` pasan. El release bundle de producción se genera y firma localmente.
- No quedan dependencias duplicadas ni módulos Firebase KTX discontinuados; las versiones efectivamente resueltas están documentadas.
- Un merge request en GitLab ejecuta las comprobaciones sin secreto de firma y muestra un resultado claro. Publicar sigue requiriendo una acción humana en Play Console.

## Decisiones delegadas al implementador

- Versiones estables exactas y orden de los grupos según compatibilidad y notas de release vigentes al ejecutar.
- Formato final de centralización de versiones (catálogo TOML o constantes compartidas), favoreciendo la opción que deje los tres módulos más claros.
- Imagen y caché de CI que funcionen con API 36, JDK y Gradle del proyecto y con las cuotas disponibles.
