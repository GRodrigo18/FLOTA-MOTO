package com.example.util

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Recuerda, por día, qué alertas ya se avisaron al celular y cuáles ocultó el usuario.
 *
 * - Avisadas: así cada alerta suena UNA vez al día (sea por el aviso de las 8:00, el de las 19:30
 *   o al abrir la app), sin repetirse cada vez que cambia algo.
 * - Ocultas: antes se borraban al cerrar la app y volvían a aparecer; ahora quedan ocultas hasta
 *   el día siguiente, y vuelven solas si el problema sigue sin resolverse.
 */
object AlertMemory {
    private const val KEY_NOTIFIED_DAY = "alerts_notified_day"
    private const val KEY_NOTIFIED_IDS = "alerts_notified_ids"
    private const val KEY_DISMISSED_DAY = "alerts_dismissed_day"
    private const val KEY_DISMISSED_IDS = "alerts_dismissed_ids"

    fun today(now: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))

    /** Ids que aún no se avisaron hoy. Función pura (sin Android), fácil de probar. */
    fun pendingToNotify(
        today: String,
        storedDay: String?,
        storedIds: Set<String>,
        currentIds: Collection<String>
    ): List<String> {
        val alreadyToday = if (storedDay == today) storedIds else emptySet()
        return currentIds.filter { it !in alreadyToday }.distinct()
    }

    fun notifiedToday(prefs: SharedPreferences, now: Long = System.currentTimeMillis()): Set<String> =
        readSet(prefs, KEY_NOTIFIED_DAY, KEY_NOTIFIED_IDS, today(now))

    fun markNotified(prefs: SharedPreferences, ids: Collection<String>, now: Long = System.currentTimeMillis()) {
        if (ids.isEmpty()) return
        val day = today(now)
        val merged = readSet(prefs, KEY_NOTIFIED_DAY, KEY_NOTIFIED_IDS, day) + ids
        writeSet(prefs, KEY_NOTIFIED_DAY, KEY_NOTIFIED_IDS, day, merged)
    }

    fun dismissedToday(prefs: SharedPreferences, now: Long = System.currentTimeMillis()): Set<String> =
        readSet(prefs, KEY_DISMISSED_DAY, KEY_DISMISSED_IDS, today(now))

    fun saveDismissed(prefs: SharedPreferences, ids: Set<String>, now: Long = System.currentTimeMillis()) {
        writeSet(prefs, KEY_DISMISSED_DAY, KEY_DISMISSED_IDS, today(now), ids)
    }

    private fun readSet(prefs: SharedPreferences, dayKey: String, idsKey: String, day: String): Set<String> {
        if (prefs.getString(dayKey, null) != day) return emptySet()
        return (prefs.getString(idsKey, "") ?: "").split("\n").filter { it.isNotBlank() }.toSet()
    }

    private fun writeSet(prefs: SharedPreferences, dayKey: String, idsKey: String, day: String, ids: Set<String>) {
        prefs.edit().putString(dayKey, day).putString(idsKey, ids.joinToString("\n")).apply()
    }
}
