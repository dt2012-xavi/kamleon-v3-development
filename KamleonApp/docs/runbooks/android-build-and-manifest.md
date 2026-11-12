# Guía de Gradle y AndroidManifest

Revisión: 2026-11-12. Esta guía explica decisiones de mantenimiento; las versiones vigentes se consultan en el catálogo. Los comentarios añadidos no cambian el comportamiento de la aplicación.

## Dónde se configura cada cosa

| Archivo | Responsabilidad |
| --- | --- |
| build.gradle raíz | Registrar los plugins compartidos con apply false; no compila el APK. |
| settings.gradle | Repositorios de plugins/dependencias e inclusión de app, datepicker y wheelview. FAIL_ON_PROJECT_REPOS evita repositorios propios en los módulos. |
| gradle/libs.versions.toml | Versiones y aliases de plugins, librerías y compileSdk/targetSdk compartidos. |
| app/build.gradle | APK: namespace/applicationId, flavors, versión publicada, SDK mínimo, firma, lint y dependencias. |
| datepicker/build.gradle y wheelview/build.gradle | Librerías locales: mantener sus minSdk y publicación Maven mientras tengan consumidores. |
| app/src/main/AndroidManifest.xml | Componentes, permisos, características opcionales, enlaces y configuración de ventanas. |
| app/src/main/res/values/themes.xml y BaseActivity.kt | Tema XML y configuración AndroidX de barras; Compose AppTheme no debe modificar la ventana. |

Usar aliases del catálogo. Compose y Firebase se alinean mediante sus BoM; CameraX mantiene una versión de familia. Los compiladores de Room/Hilt van en ksp. Room exporta esquemas en app/schemas: versionarlos y definir migraciones cuando cambie el modelo; no incrementar la versión de DB por actualizar el compilador.

## Entornos y firma

Los cuatro flavors comparten applicationId: instalar otro entorno reemplaza la instalación de ese paquete. El sufijo de versionName no permite instalar ambos a la vez.

| Flavor | Nombre visible | Host de app links |
| --- | --- | --- |
| internal | Kamleon-INT | kamleon-v3-internal.vercel.app |
| development | Kamleon-DEV | dev.kamleon.app |
| preProduction | Kamleon-PRE | pre.kamleon.app |
| prod | Kamleon | kamleon.app |

El host entra mediante manifestPlaceholders.appLinkHost. Revisar también UrlConstants del flavor al cambiar URLs. No modificar applicationId, hosts o configuración Firebase como parte de una limpieza de dependencias.

La configuración de firma es local y opcional al configurar Gradle. La tarea validateProdReleaseSigning exige propiedades completas y un keystore legible para prodRelease; no obliga a tener secretos para desarrollar en debug. La firma efectiva depende de las reglas de precedencia de AGP y de la variante: no inferir el certificado usado solo del nombre del flavor. Seguir [android-release.md](android-release.md). No guardar ni imprimir contraseñas/certificados en esta guía.

R8 está desactivado en release. Tener proguard-rules.pro declarado no significa que se esté minificando/ofuscando. Activarlo requiere una tarea propia con pruebas de la app release.

## Manifest: decisiones que se mantienen

| Configuración | Criterio actual |
| --- | --- |
| CAMERA y POST_NOTIFICATIONS | Mantener. La declaración no concede el permiso: manejar aceptación, denegación y revocación en runtime. |
| Cámara/autofocus required=false | Mantener para no excluir instalaciones sin ese hardware; el flujo de cámara debe tolerar su ausencia. |
| InitActivity exported=true | Necesaria como launcher y entrada de app links. Validar entradas en código aunque coincidan con el filtro. |
| MainActivity exported=false | Mantener interna; InitActivity canaliza entradas externas. |
| FileProvider privado y grants | Mantener authority basada en applicationId y únicamente Pictures/my_images/. No añadir root-path ni permisos de almacenamiento para este directorio. |
| /scan exacto y https | Mantener; verificar assetlinks en el servidor y autenticación/autorización aparte del filtro. |
| allowBackup=false + reglas XML | Mantener la decisión actual de excluir sesión/medidas de cloud y D2D. No borrar las exclusiones ni prometer comportamiento de herramientas OEM ajenas a Android. |
| NotificationService exported=false | Mantener privado. Los metadatos FCM son valores por defecto, no una prueba de recepción real. |
| hardwareAccelerated=true | Mantener; no confundir renderizado por GPU con aumento del heap disponible. |
| largeHeap=true | Deuda conservada: medir memoria, bitmaps y posibles fugas antes de retirarlo. No demuestra que haya una necesidad real ni corrige una fuga. |
| supportsRtl=true | Mantener y verificar los layouts/direcciones; el flag por sí solo no certifica soporte RTL. |
| tools:targetApi=31 | Anotación para herramientas en recursos/atributos del manifest; no fija targetSdk ni activa compatibilidad de runtime. El target real está en Gradle. |

Los permisos aportados por dependencias también cuentan. Inspeccionar Merged Manifest tras actualizar SDKs; por ejemplo, el árbol actual incorpora permisos de Analytics/AdServices. Retirar una declaración propia no prueba que un SDK deje de recopilar datos.

## Orientación, recreación y ventanas

**Decisión actual: mantener portrait temporalmente en teléfonos; preparar su retirada como cambio funcional probado.** No usar esa restricción como garantía de que la Activity nunca se recrea.

Con targetSdk 36, Android 16 ignora determinadas restricciones de orientación/redimensionado en pantallas de al menos 600 dp, salvo las excepciones documentadas. Este riesgo ya existe con el target actual y no empieza con una futura subida de SDK. Véase [cambios para target 36](https://developer.android.com/about/versions/16/behavior-changes-16).

La lectura del código detecta dos rutas que deben corregirse antes de ampliar orientación; no se afirma haber reproducido sus efectos en dispositivo:

1. MainActivity.currentImageUri es lateinit y no se guarda/restaura. Si se recrea la Activity mientras la cámara externa está abierta, el resultado puede volver sin URI inicializada. Guardar/restaurar la URI pendiente y manejar el resultado con estado ausente, sin perder la foto.
2. MainActivity.onCreate vuelve a llamar handleIntent(intent), que puede subir el QR de los extras. Recrear la Activity con el mismo intent puede repetir el efecto. Distinguir restauración de estado de un evento nuevo y asegurar consumo/idempotencia por sesión, también para onNewIntent. Completar con QR-01/QR-02 del seguimiento existente.

Orden recomendado: reproducir esos casos, corregir persistencia/consumo, probar restauración de navegación/formularios/hojas y cámara, después retirar portrait y revisar layouts horizontal/multiventana. No añadir configChanges, resizeableActivity=false ni opt-outs de plataforma como sustituto de restaurar estado.

| Propiedad | Decisión y comprobación |
| --- | --- |
| singleTop en ambas Activities | Mantener. Reutiliza una instancia solo si está en la parte superior de su tarea; no garantiza una sola instancia global ni evita repetir eventos. Ambas implementan onNewIntent. Cambiar a singleTask alteraría el back stack. |
| adjustResize | Mantener para formularios con teclado. Comprobar campos y CTA visibles con edge-to-edge; no añadir padding de IME en varios niveles a la vez. |
| configChanges ausente | Mantener ausente. Dejar que Android recree la pantalla y restaurar estado correctamente. |
| Tema MainActivity | Mantener por ahora Theme.KamleonApp y la configuración central de barras de BaseActivity. Revisar junto a la UI XML/Compose, no cambiando flags aislados. |
| Tema Splash de InitActivity | Revisar en una tarea de arranque. Tiene windowIsTranslucent=true y hereda de Theme.KamleonApp; no se encontró installSplashScreen en producción. InitActivity aloja también el grafo de login, por lo que no debe tratarse como una ventana efímera sin comprobar ese recorrido. |
| Flags claros/oscuros del tema | El tema Splash declara barra blanca/iconos oscuros y BaseActivity configura barras oscuras/iconos claros después de crear la Activity. Verificar transición de arranque y formularios antes de unificarlos; no se certifica ausencia de parpadeo. |

No cambiar translucent o el tema padre sin revisar arranque frío/caliente, vuelta desde cámara, pantalla anterior visible, login y Back. Tampoco desactivar predictive back por precaución; comprobar el flujo real con las APIs AndroidX.

## Verificaciones de mantenimiento

Desde la raíz KamleonApp, con el wrapper local:

- Build: ./gradlew :app:assembleDevelopmentDebug
- Lógica compartida: ./gradlew :app:testDevelopmentDebugUnitTest
- Otros entornos: assembleInternalDebug, assemblePreProductionDebug y assembleProdDebug.
- Lint separado del build multivariante: ./gradlew --no-daemon :app:lintDevelopmentDebug --console=plain
- Dispositivo: ./gradlew :app:connectedDevelopmentDebugAndroidTest

Lint usa un baseline fijo: la deuda histórica no equivale a una app sin problemas. El fallo intermitente del analizador con fuentes Hilt de varios flavors tiene una receta aislada en CI; no se afirma que esté corregido.

Para cambios de orientación/tema: probar Activity.recreate y muerte de proceso controlada, cámara pendiente, app link con un solo efecto, formularios con IME, listas y hojas. Matriz mínima: teléfono vertical/horizontal, dispositivo sw600dp, plegado/redimensionado, API 30 y 35/36, tema claro/oscuro y navegación por gestos/tres botones. Comprobar que navegación, estado y scroll se conservan; no utilizar datos médicos reales.

Verificación de esta pasada: ./gradlew help :app:processDevelopmentDebugMainManifest --console=plain --quiet terminó con exit 0. Se revisó el diff para confirmar que solo cambian comentarios, sin valores Gradle ni atributos XML. No se repitió la suite de compilación para estos cambios documentales.

En esta pasada solo se añaden comentarios y documentación; no se han cambiado atributos ni flags. Las pruebas runtime siguen siendo externas. El [cierre de etapa 1](../audits/2026-11-11-stage1-final-review.md) conserva las evidencias anteriores; no sustituye estas comprobaciones funcionales.

## Referencias

- [Manifest application: heap, hardware y backup](https://developer.android.com/guide/topics/manifest/application-element)
- [Manifest activity: orientación, launchMode y ventanas](https://developer.android.com/guide/topics/manifest/activity-element)
- [Cambios de Android 16](https://developer.android.com/about/versions/16/behavior-changes-16)
