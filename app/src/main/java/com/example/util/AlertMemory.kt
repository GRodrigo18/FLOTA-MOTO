package com.example.util

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Memoria de los recordatorios.
 *
 * - Avisadas (por día): así cada recordatorio suena UNA vez al día (sea por el aviso de las 8:00,
 *   el de las 19:30 o al abrir la app) mientras no se resuelva, sin repetirse cada vez que cambia algo.
 *   Los ocultos en Inicio también se avisan: ocultar no silencia.
 * - Ocultos en Inicio (permanente, por [com.example.ui.UrgentAlert.hideKey]): solo se quitan de
 *   Inicio, siguen en la campana. Cuando el problema se resuelve, su clave se olvida ([prune]),
 *   así si vuelve a pasar se ve otra vez en Inicio.
 */
object AlertMemory {
    private const val KEY_NOTIFIED_DAY = "alerts_notified_day"
    private const val KEY_NOTIFIED_IDS = "alerts_notified_ids"
    private const val KEY_HOME_HIDDEN = "alerts_home_hidden"

    // Claves de la v1.4 (ocultas por día). Ya no se usan; se borran al guardar.
    private const val OLD_KEY_DISMISSED_DAY = "alerts_dismissed_day"
    private const val OLD_KEY_DISMISSED_IDS = "alerts_dismissed_ids"

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

    /** Claves (hideKey) quitadas de Inicio. No caducan: se olvidan solo al resolverse ([prune]). */
    fun homeHidden(prefs: SharedPreferences): Set<String> =
        (prefs.getString(KEY_HOME_HIDDEN, "") ?: "").split("\n").filter { it.isNotBlank() }.toSet()

    fun saveHomeHidden(prefs: SharedPreferences, keys: Set<String>) {
        prefs.edit()
            .putString(KEY_HOME_HIDDEN, keys.filter { it.isNotBlank() }.joinToString("\n"))
            .remove(OLD_KEY_DISMISSED_DAY)
            .remove(OLD_KEY_DISMISSED_IDS)
            .apply()
    }

    /**
     * Deja solo las claves ocultas que siguen activas. Función pura: lo resuelto se olvida,
     * y una etapa nueva (p. ej. "vencido") no está oculta aunque la anterior sí lo estuviera.
     */
    fun prune(hidden: Set<String>, activeKeys: Collection<String>): Set<String> {
        val active = activeKeys.toSet()
        return hidden.filterTo(LinkedHashSet()) { it in active }
    }

    private fun readSet(prefs: SharedPreferences, dayKey: String, idsKey: String, day: String): Set<String> {
        if (prefs.getString(dayKey, null) != day) return emptySet()
        return (prefs.getString(idsKey, "") ?: "").split("\n").filter { it.isNotBlank() }.toSet()
    }

    private fun writeSet(prefs: SharedPreferences, dayKey: String, idsKey: String, day: String, ids: Set<String>) {
        prefs.edit().putString(dayKey, day).putString(idsKey, ids.joinToString("\n")).apply()
    }
}
