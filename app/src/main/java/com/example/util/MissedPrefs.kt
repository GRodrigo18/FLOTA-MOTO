package com.example.util

import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Lo que la app recuerda para calcular los días sin pagar (en las preferencias, que también
 * entran en la copia de seguridad de Android):
 * - desde cuándo se llevan las faltas (no se inventan faltas de antes de usar esta versión),
 * - el día en que se registró cada moto,
 * - desde cuándo una moto está parada (taller o inactiva), para no cobrarle esos días al volver.
 */
object MissedPrefs {
    private const val KEY_DESDE = "faltas_desde"
    private const val KEY_AGREGADAS = "motos_agregadas"
    private const val KEY_PARADAS = "motos_paradas_desde"
    private const val KEY_AVISAR = "motos_avisar_pagos_desde"

    fun desde(prefs: SharedPreferences): String? = prefs.getString(KEY_DESDE, null)

    fun setDesde(prefs: SharedPreferences, fecha: String) {
        prefs.edit().putString(KEY_DESDE, fecha).apply()
    }

    fun agregadas(prefs: SharedPreferences): Map<String, String> = readMap(prefs, KEY_AGREGADAS)
    fun paradas(prefs: SharedPreferences): Map<String, String> = readMap(prefs, KEY_PARADAS)

    fun setAgregada(prefs: SharedPreferences, placa: String, fecha: String) =
        writeMap(prefs, KEY_AGREGADAS, agregadas(prefs) + (placa to fecha))

    fun setParadas(prefs: SharedPreferences, map: Map<String, String>) = writeMap(prefs, KEY_PARADAS, map)

    /** Fecha elegida por el dueño para avisar los pagos de cada moto (placa → yyyy-MM-dd). */
    fun avisarDesde(prefs: SharedPreferences): Map<String, String> = readMap(prefs, KEY_AVISAR)

    /** [fecha] = null vuelve a "automático". */
    fun setAvisarDesde(prefs: SharedPreferences, placa: String, fecha: String?) {
        val m = avisarDesde(prefs)
        writeMap(prefs, KEY_AVISAR, if (fecha == null) m - placa else m + (placa to fecha))
    }

    /** La moto cambió de placa: lo recordado pasa a la placa nueva. */
    fun rename(prefs: SharedPreferences, vieja: String, nueva: String) {
        if (vieja == nueva) return
        listOf(KEY_AGREGADAS, KEY_PARADAS, KEY_AVISAR).forEach { key ->
            val m = readMap(prefs, key)
            m[vieja]?.let { writeMap(prefs, key, (m - vieja) + (nueva to it)) }
        }
    }

    /** La moto se eliminó: se olvida lo recordado. */
    fun remove(prefs: SharedPreferences, placa: String) {
        listOf(KEY_AGREGADAS, KEY_PARADAS, KEY_AVISAR).forEach { key ->
            val m = readMap(prefs, key)
            if (placa in m) writeMap(prefs, key, m - placa)
        }
    }

    /** "Empezar de cero": se olvida todo. */
    fun clear(prefs: SharedPreferences) {
        prefs.edit().remove(KEY_DESDE).remove(KEY_AGREGADAS).remove(KEY_PARADAS).remove(KEY_AVISAR).apply()
    }

    private fun readMap(prefs: SharedPreferences, key: String): Map<String, String> = try {
        val o = JSONObject(prefs.getString(key, null) ?: return emptyMap())
        o.keys().asSequence().associateWith { o.optString(it) }.filterValues { it.isNotBlank() }
    } catch (_: Exception) {
        emptyMap()
    }

    private fun writeMap(prefs: SharedPreferences, key: String, map: Map<String, String>) {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v) }
        prefs.edit().putString(key, o.toString()).apply()
    }
}
