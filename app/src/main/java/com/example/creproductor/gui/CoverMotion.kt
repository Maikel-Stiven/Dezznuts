package com.example.creproductor.gui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

/**
 * Portada con movimiento (opcional, apagada por defecto): al inclinar el teléfono la portada se inclina con él, como
 * en los reproductores de escritorio tipo "glassy". Usa solo el acelerómetro (todos los teléfonos lo tienen) y solo
 * mientras el reproductor está abierto. Respeta [rememberVisualEffectsEnabled]: en equipos modestos queda fija.
 */
private const val PREFS = "dezznuts_prefs"
private const val KEY_COVER_MOTION = "cover_motion"

/** Inclinación máxima de la portada, en grados. */
private const val MAX_TILT_DEG = 9f

fun isCoverMotionOn(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_COVER_MOTION, false)

fun setCoverMotion(context: Context, on: Boolean) =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_COVER_MOTION, on) }

/** Convierte la aceleración (m/s²) en grados de inclinación respecto a la postura habitual, sin pasarse de [MAX_TILT_DEG]. */
internal fun tiltDegrees(delta: Float): Float = (delta * 1.6f).coerceIn(-MAX_TILT_DEG, MAX_TILT_DEG)

/**
 * Devuelve (rotaciónX, rotaciónY) en grados, vivos mientras [active]. Leerlos dentro de `graphicsLayer { }` evita
 * recomponer: el sensor solo redibuja la capa.
 */
@Composable
fun rememberCoverTilt(active: Boolean): Pair<State<Float>, State<Float>> {
    val rotX = remember { mutableFloatStateOf(0f) }
    val rotY = remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val sensorsAllowed = rememberVisualEffectsEnabled()
    DisposableEffect(active && sensorsAllowed) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!(active && sensorsAllowed) || accel == null) {
            rotX.floatValue = 0f; rotY.floatValue = 0f
            return@DisposableEffect onDispose { }
        }
        // Dos filtros: uno rápido (suaviza el temblor) y uno muy lento que es la "postura de reposo". La inclinación
        // es la diferencia, así que la portada se recentra sola aunque se lea acostado o de pie.
        var fastX = 0f; var fastY = 0f; var slowX = 0f; var slowY = 0f; var primed = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val x = e.values[0]; val y = e.values[1]
                if (!primed) { fastX = x; fastY = y; slowX = x; slowY = y; primed = true }
                fastX += (x - fastX) * 0.2f; fastY += (y - fastY) * 0.2f
                slowX += (x - slowX) * 0.005f; slowY += (y - slowY) * 0.005f
                rotY.floatValue = tiltDegrees(fastX - slowX)   // izquierda/derecha
                rotX.floatValue = tiltDegrees(fastY - slowY)   // arriba/abajo
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
    return rotX to rotY
}
