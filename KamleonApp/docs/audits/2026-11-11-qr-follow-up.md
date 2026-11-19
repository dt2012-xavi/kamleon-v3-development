# Correcciones QR pendientes tras la limpieza de dependencias

Estado: pendientes. Acordadas para después de la tarea 2; no forman parte de su limpieza de dependencias.

## QR-01: evitar collectors duplicados al reabrir el escáner

Ubicación: app/src/main/java/com/dynatech2012/kamleonuserapp/viewmodels/QrViewModel.kt, startScanner().

Hallazgo: cada llamada configura el analizador y lanza dos nuevas coroutines de collect en viewModelScope. Salir del escáner llama stopAnalyzing(), pero no cancela esos collectors. Reentrar puede acumular observadores y repetir procesamiento/eventos.

Resultado requerido: una única observación de cada flujo durante el ciclo de vida correspondiente, incluso tras abrir/cerrar repetidamente el escáner. Definir ownership y cancelación antes de implementar.

Verificación: prueba que reproduzca aperturas repetidas y demuestre que un resultado se procesa una sola vez; comprobar salida, reentrada y liberación del ViewModel.

## QR-02: manejar QR con parámetros ausentes o URI inválida

Ubicación: mismo archivo, parseQrString() y collector de qrStingFlow.

Hallazgo: parseQrString() lanza IllegalArgumentException cuando faltan unitId/sessionId; el collector invoca el método sin capturar esa excepción. Una URI no jerárquica también requiere manejo explícito.

Resultado requerido: convertir entrada inválida en un estado de error controlado; no iniciar ninguna subida y permitir seguir escaneando.

Verificación: parámetros ausentes/vacíos, URI malformada/no jerárquica y QR válido posterior a uno inválido. El QR válido se procesa una sola vez. No registrar identificadores o payloads sensibles en las pruebas ni en nuevos logs.

## Orden

Completar primero la tarea 2 y su matriz de compilación. Abordar después estas dos correcciones en un cambio funcional separado con pruebas. Las tareas de manifest/FileProvider y calidad siguen en el plan de sostenibilidad.
