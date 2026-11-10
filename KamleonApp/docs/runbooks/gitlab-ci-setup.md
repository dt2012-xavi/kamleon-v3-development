# GitLab: activar comprobaciones Android sin automatizar publicaciones

Proyecto: `k2250/kamleon-android-userapp-v2` en GitLab.com. Este procedimiento corresponde al diseño de modernización de Gradle del 2026-11-10. La firma y la subida del AAB de producción siguen siendo locales y manuales. La raíz Git contiene dos proyectos hermanos, `KamleonApp/` y `GraphViewDemo/`; este pipeline comprobará solo `KamleonApp/`.

## Ahora: preparar el proyecto

1. Abre el proyecto en GitLab y entra en **Settings > CI/CD > Runners**.
2. En **Instance runners**, comprueba que **Turn on instance runners for this project** esté activado. GitLab.com normalmente los activa por defecto. Si está desactivado, actívalo. Si el grupo impide el cambio, pide a su Owner que lo habilite para el grupo/proyecto. No crees un runner propio todavía.
3. Comprueba que aparezca un runner de instancia disponible. Si no aparece, revisa permisos/ajustes del grupo y la cuota de minutos de cómputo antes de instalar infraestructura.
4. Revisa la cuota de cómputo del namespace. El pipeline se diseñará para compilar y probar, sin emulador ni despliegue, y se ajustará si el consumo es alto.

## Cuando se añada el pipeline al repositorio

5. El archivo `.gitlab-ci.yml` está en la **raíz del repositorio Git**, junto a `KamleonApp/` y `GraphViewDemo/`. El job entra en `KamleonApp/` y usa su `./gradlew`. Se crea para todos los merge requests y para los cambios en la rama predeterminada. No hace falta mover ni integrar `GraphViewDemo/`.
6. El job usa una imagen con Android SDK 36 y JDK 17, compila los cuatro flavors debug y ejecuta tests unitarios y lint de `development`. Adjunta informes JUnit y de lint durante siete días. El caché guarda solo dependencias y la distribución de Gradle. No contiene firma ni publica la app.
7. La sintaxis se validó con **GitLab CI Lint** del proyecto (`valid: true`, sin errores ni avisos). Abre **Build > Pipeline editor > Validate** para volver a comprobar la versión del merge request; también puedes simular la creación del pipeline en la rama predeterminada.
8. Abre el merge request de prueba hacia `development`. En **Build > Pipelines**, comprueba que `android_verify` toma un runner y termina en verde. Abre sus artefactos para consultar los tests y lint. Si queda en `pending`, vuelve a **Settings > CI/CD > Runners** y revisa disponibilidad, etiquetas y cuota.
9. Después de que pase al menos un pipeline real, entra en **Settings > Merge requests > Merge checks**, marca **Pipelines must succeed** y guarda. El `workflow` ya crea pipelines para todos los merge requests; verifica que el de prueba exista antes de activar el bloqueo.

## Firma y publicación

10. No subas a GitLab el keystore, sus contraseñas ni un `local.properties` con secretos. En esta fase no hacen falta variables secretas de CI: GitLab solo compila y prueba.
11. Para cada publicación, incrementa `versionCode`/`versionName`, genera y firma el AAB `prodRelease` en el ordenador que custodia la clave, verifica variante/versión/firma y súbelo manualmente a Play Console.

## Referencias oficiales

- Runners alojados y activación: https://docs.gitlab.com/ci/runners/hosted_runners/ y https://docs.gitlab.com/ci/runners/runners_scope/
- Validación del pipeline: https://docs.gitlab.com/ci/yaml/lint/
- Exigir pipeline correcto antes de merge: https://docs.gitlab.com/user/project/merge_requests/auto_merge/
- Minutos de cómputo: https://docs.gitlab.com/ci/pipelines/compute_minutes/
