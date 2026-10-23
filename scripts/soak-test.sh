#!/usr/bin/env bash
# Prueba de resistencia del reproductor en un emulador/dispositivo conectado por adb.
#
# Reproduce y salta pistas sin parar (con el ecualizador paramétrico y ReplayGain con subida activos, para
# ejercitar el procesador de audio) y registra cada 10 s la memoria (PSS) y el nº de hilos. Al final avisa si la
# memoria creció de forma sostenida, señal de fugas. Uso:
#
#   scripts/soak-test.sh [segundos=300] [x_primera_cancion=300] [y_primera_cancion=455]
#
# Requisitos: la app instalada con permiso de audio y la pestaña "Canciones" visible al abrirla
# (las coordenadas son de un móvil 720x1560; ajústalas con los argumentos 2 y 3).
set -euo pipefail

PKG=io.github.maikelstiven.dezznuts
DURATION=${1:-300}
TAP_X=${2:-300}
TAP_Y=${3:-455}
OUT=${OUT:-/tmp/soak-$(date +%H%M%S).csv}

adb shell am force-stop $PKG
# Ecualizador (+6 dB a 500 Hz) y ReplayGain con subida activos.
printf "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n<boolean name=\"hifi_replaygain\" value=\"true\" />\n<boolean name=\"hifi_rg_boost\" value=\"true\" />\n<boolean name=\"eq_enabled\" value=\"true\" />\n<string name=\"eq_bands_v2\">500.0:6.0:1.0;3600.0:-3.0:1.0</string>\n</map>\n" \
  | adb shell "run-as $PKG sh -c 'cat > shared_prefs/dezznuts_prefs.xml'"

adb shell am start -n $PKG/.MainActivity >/dev/null
sleep 8
adb shell input tap 408 222        # pestaña "Canciones"
sleep 2
adb shell input tap "$TAP_X" "$TAP_Y"   # empieza a sonar
sleep 4

echo "segundo,pss_kb,java_kb,native_kb,hilos" > "$OUT"
sample() {
  local mem threads
  mem=$(adb shell dumpsys meminfo $PKG | tr -s ' ')
  threads=$(adb shell "ls /proc/\$(pidof $PKG)/task 2>/dev/null | wc -l" | tr -d '\r')
  echo "$1,$(echo "$mem" | awk '/TOTAL PSS:/{print $3}'),$(echo "$mem" | awk '/Java Heap:/{print $3; exit}'),$(echo "$mem" | awk '/Native Heap:/{print $3; exit}'),$threads" >> "$OUT"
}

START=$(date +%s); i=0
while [ $(( $(date +%s) - START )) -lt "$DURATION" ]; do
  i=$((i+1))
  adb shell input keyevent KEYCODE_MEDIA_NEXT
  [ $((i % 7)) -eq 0 ] && adb shell input keyevent KEYCODE_MEDIA_PREVIOUS
  if [ $((i % 11)) -eq 0 ]; then adb shell input keyevent KEYCODE_MEDIA_PAUSE; sleep 1; adb shell input keyevent KEYCODE_MEDIA_PLAY; fi
  [ $((i % 3)) -eq 0 ] && sample $(( $(date +%s) - START ))
  sleep 3
done
sample $(( $(date +%s) - START ))

echo "--- $OUT"; cat "$OUT"
adb logcat -d | grep -E "FATAL|ANR in|ExoPlayerImplInternal.*Playback error" | head -3 || true
awk -F, 'NR==2{first=$2} END{printf "PSS inicial %d KB -> final %d KB (%+.1f%%)\n", first, $2, ($2-first)*100/first}' "$OUT"
