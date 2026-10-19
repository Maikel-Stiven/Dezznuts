// Perfiles de sonido por tipo de salida (altavoz, cable, Bluetooth, DAC USB).
//
// Cada salida suena distinto: lo que va bien en un DAC USB con auriculares puede no ir en el altavoz.
// Al cambiar de salida se guardan los ajustes de la anterior y se cargan los de la nueva (si existen).
// Lógica pura (sin Android) para probarla; el servicio solo lee/escribe las preferencias.

package com.example.creproductor.data

enum class OutputClass { SPEAKER, WIRED, BLUETOOTH, USB }

/** Ajustes que forman un perfil. */
data class ProfileSettings(
    val replayGain: Boolean,
    val replayGainAlbum: Boolean,
    val replayGainBoost: Boolean,
    val eqEnabled: Boolean,
    val eqBands: String
) {
    fun encode() = listOf(replayGain, replayGainAlbum, replayGainBoost, eqEnabled).joinToString("") { if (it) "1" else "0" } + "|" + eqBands

    companion object {
        fun decode(raw: String?): ProfileSettings? {
            val flags = raw?.substringBefore('|')?.takeIf { it.length == 4 && it.all { c -> c == '0' || c == '1' } } ?: return null
            return ProfileSettings(flags[0] == '1', flags[1] == '1', flags[2] == '1', flags[3] == '1', raw.substringAfter('|', ""))
        }
    }
}

object OutputProfiles {

    // Valores de AudioDeviceInfo.TYPE_* (se copian para que la lógica no dependa de Android).
    private val USB = setOf(11, 12, 22)                  // DEVICE, ACCESSORY, HEADSET
    private val WIRED = setOf(3, 4, 5, 6, 9, 10, 13, 19) // headset, headphones, line, digital, HDMI…
    private val BLUETOOTH = setOf(7, 8, 23, 26, 27, 30)  // SCO, A2DP, hearing aid, BLE

    /** La salida "más específica" conectada manda: USB > cable > Bluetooth > altavoz. */
    fun classify(deviceTypes: Collection<Int>): OutputClass = when {
        deviceTypes.any { it in USB } -> OutputClass.USB
        deviceTypes.any { it in WIRED } -> OutputClass.WIRED
        deviceTypes.any { it in BLUETOOTH } -> OutputClass.BLUETOOTH
        else -> OutputClass.SPEAKER
    }
}
