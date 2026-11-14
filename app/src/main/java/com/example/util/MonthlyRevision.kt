package com.example.util

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * La "revisión técnica del mes" vuelve a quedar pendiente al empezar cada mes.
 * Antes, una vez marcada como hecha, quedaba así para siempre y la alerta mensual no volvía.
 */
object MonthlyRevision {
    private const val KEY_MONTH = "revision_mensual_mes"

    fun currentMonth(now: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date(now))

    /**
     * Función pura: ¿hay que reiniciar? Solo si ya se había registrado un mes y cambió.
     * La primera vez (al instalar esta versión) no reinicia, para no inventar pendientes
     * de un mes en el que quizá ya se hizo la revisión.
     */
    fun shouldReset(storedMonth: String?, currentMonth: String): Boolean =
        storedMonth != null && storedMonth != currentMonth

    /** Devuelve true si toca reiniciar este mes, y deja anotado el mes actual. */
    fun checkAndMark(prefs: SharedPreferences, now: Long = System.currentTimeMillis()): Boolean {
        val current = currentMonth(now)
        val stored = prefs.getString(KEY_MONTH, null)
        if (stored != current) prefs.edit().putString(KEY_MONTH, current).apply()
        return shouldReset(stored, current)
    }
}
