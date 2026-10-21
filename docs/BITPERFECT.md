# Bit-perfect con DAC USB (rama `bitperfect`)

Función opt-in (apagada por defecto): Ajustes → *Audio Hi/Res* → **Bit-perfect con DAC USB**.

## Qué hace
En Android 14+ (API 34) un DAC USB puede ofrecer un **mezclador bit-perfect** (`AudioMixerAttributes`): el sistema deja de
remuestrear y de mezclar, pero solo para un `AudioTrack` cuyo formato coincide **exactamente** con el formato de mezcla elegido.
Esta función lo negocia por pista:

1. Al cambiar de pista (`onTracksChanged`) se anota su frecuencia, canales y profundidad.
2. Se calcula el formato en que ExoPlayer abrirá el `AudioTrack` (`BitPerfect.trackFormat`):
   - **float de 32 bits** si la salida float está activa y la fuente es de alta resolución;
   - **16 bits** en cualquier otro caso (así lo hace `DefaultAudioSink`; comprobado en el emulador: un FLAC 24/96 sale como PCM 16 bits).
   - Si la profundidad de la fuente no se conoce, se infiere por la frecuencia (>48 kHz = alta resolución).
3. Se pide al DAC su lista de formatos `MIXER_BEHAVIOR_BIT_PERFECT` (`getSupportedMixerAttributes`) y se elige con
   `BitPerfect.choose`: **coincidencia exacta** de frecuencia, profundidad/float y canales. No se elige "el más cercano":
   un formato mayor no daría bit-perfect y podría degradar la ruta.
4. Si hay coincidencia → `setPreferredMixerAttributes`. Si no → se queda el modo normal.
5. Se limpia (`clearPreferredMixerAttributes`) cuando la función se apaga, el DAC se desconecta, el DAC rechaza el formato,
   o entra algo que modifica la señal.

La ruta real que muestra el reproductor dice `Bit-perfect 96 kHz / 16 bit` solo si el `AudioTrack` que se abrió coincide con el
formato de mezcla elegido (`onAudioTrackInitialized`).

## Cuándo no se aplica (`BitPerfect.allowed`)
Android < 14, función apagada, sin DAC USB conectado, **procesador de audio activo** (ecualizador paramétrico o subida de
ReplayGain, que trabajan en float y alteran la señal) u **offload**. Se desactiva solo y vuelve a aplicarse al apagar eso.

## Rendimiento
Sin sondeos ni temporizadores: solo se recalcula al cambiar de pista, de dispositivo (`AudioDeviceCallback`) o de ajustes, y
solo se llama al sistema si el resultado cambió. Con la función apagada el coste es nulo (una comprobación de preferencia).

## Limitaciones
- **Solo DAC USB + Android 14+.** En altavoz y Bluetooth el sistema siempre remuestrea.
- **Alta resolución en enteros (24 bit):** ExoPlayer no abre el `AudioTrack` en PCM de 24 bits (lo baja a 16 o lo pasa a float), así que
  el bit-perfect de 24 bits solo es posible si el DAC ofrece formato **float** (raro). Con 16 bits funciona con cualquier DAC que
  ofrezca esa frecuencia.
- **Reglas por formato para offload/float:** no se añadieron (ya las decide ExoPlayer y cambiar la política no es simple ni segura sin
  probar con hardware real).
- **No probado en hardware**: el emulador no soporta mezcladores bit-perfect. Verificado con tests unitarios (`BitPerfectTest`): formato
  del `AudioTrack` según salida y fuente, elección exacta, condiciones de activación y ruta real. Falta probar con un DAC USB real
  (que lo acepte, que no corte al cambiar de pista, que se limpie al desconectar).
