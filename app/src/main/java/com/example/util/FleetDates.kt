package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Cálculos de fechas de la flota (SOAT, contratos).
 * Se calculan al leer los datos, así nunca quedan desactualizados con el paso de los días.
 * Formato de fecha esperado: yyyy-MM-dd.
 */
object FleetDates {
    private const val DAY_MS = 86_400_000L

    private fun parse(value: String): Long? = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value.trim())?.time
    } catch (e: Exception) {
        null
    }

    private fun startOfDay(now: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = now
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** Días que faltan hasta [date] (mínimo 0). Devuelve null si la fecha no es válida. */
    fun daysUntil(date: String, now: Long = System.currentTimeMillis()): Int? {
        val end = parse(date) ?: return null
        return Math.round((end - startOfDay(now)).toDouble() / DAY_MS).toInt().coerceAtLeast(0)
    }

    fun expiryLabel(days: Int): String = when {
        days <= 0 -> "Vencido"
        days == 1 -> "1 Día"
        days < 30 -> "$days Días"
        else -> "${days / 30} Meses"
    }

    /** Avance del contrato entre 0 y 1. Devuelve null si las fechas no son válidas. */
    fun contractProgress(start: String, end: String, now: Long = System.currentTimeMillis()): Float? {
        val s = parse(start) ?: return null
        val e = parse(end) ?: return null
        if (e <= s) return null
        return ((startOfDay(now) - s).toFloat() / (e - s).toFloat()).coerceIn(0f, 1f)
    }

    private fun fmt(time: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(time))

    /**
     * Acepta lo que escriba una persona (2026-11-15, 15/11/2026, 15-11-2026)
     * y lo devuelve como yyyy-MM-dd. Devuelve null si no es una fecha real.
     */
    fun normalize(input: String): String? {
        val v = input.trim()
        parse(v)?.let { return fmt(it) }
        for (p in listOf("dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "d-M-yyyy")) {
            try {
                val t = SimpleDateFormat(p, Locale.US).apply { isLenient = false }.parse(v)?.time
                if (t != null) return fmt(t)
            } catch (_: Exception) {}
        }
        return null
    }

    /** Fecha + [months] meses (o + [days] días), en yyyy-MM-dd. */
    fun plus(date: String, months: Int = 0, days: Int = 0): String? {
        val t = parse(date) ?: return null
        val c = Calendar.getInstance()
        c.timeInMillis = t
        c.add(Calendar.MONTH, months)
        c.add(Calendar.DAY_OF_MONTH, days)
        return fmt(c.timeInMillis)
    }

    fun today(now: Long = System.currentTimeMillis()): String = fmt(startOfDay(now))

    /** Meses que dura cada modalidad ("1 Semana" cuenta como 0 meses y 7 días). */
    fun durationOf(tipoDuracion: String): Pair<Int, Int> {
        val t = tipoDuracion.lowercase()
        val n = Regex("\\d+").find(t)?.value?.toIntOrNull() ?: 1
        return when {
            "semana" in t -> 0 to 7 * n
            "año" in t || "ano" in t -> 12 * n to 0
            else -> n to 0
        }
    }

    /**
     * Al renovar, el contrato nuevo empieza donde terminó el anterior;
     * si ya venció hace tiempo, empieza hoy.
     */
    fun renewalStart(finAnterior: String, now: Long = System.currentTimeMillis()): String {
        val hoy = today(now)
        val fin = normalize(finAnterior) ?: return hoy
        return if (fin >= hoy) fin else hoy
    }

    /** Fecha de fin sugerida al renovar: inicio + la misma modalidad. */
    fun suggestedRenewalEnd(finAnterior: String, tipoDuracion: String, now: Long = System.currentTimeMillis()): String {
        val start = renewalStart(finAnterior, now)
        val (m, d) = durationOf(tipoDuracion)
        return plus(start, m, d) ?: start
    }

    /** Días hasta [date]: negativo si ya pasó (ej. -5 = venció hace 5 días). null si no es fecha válida. */
    fun daysSigned(date: String, now: Long = System.currentTimeMillis()): Int? {
        val end = parse(date) ?: return null
        return Math.round((end - startOfDay(now)).toDouble() / DAY_MS).toInt()
    }

    /** Modalidades de contrato que ofrece la app (el fin se calcula solo con estas). */
    val MODALIDADES = listOf("1 Mes" to 1, "3 Meses" to 3, "6 Meses" to 6, "1 Año" to 12)

    /** Fin del contrato = inicio + modalidad (ej. 2026-11-05 + "3 Meses" = 2027-02-05). */
    fun contractEnd(inicio: String, tipoDuracion: String): String? {
        val start = normalize(inicio) ?: return null
        val (m, d) = durationOf(tipoDuracion)
        return plus(start, m, d)
    }

    /** El SOAT dura 1 año: vencimiento = inicio + 12 meses. */
    fun soatEnd(inicio: String): String? = normalize(inicio)?.let { plus(it, months = 12) }

    /** Meses entre dos fechas si son exactos (para reconocer la modalidad de un contrato), si no null. */
    fun exactMonthsBetween(inicio: String, fin: String): Int? {
        val a = normalize(inicio) ?: return null
        val b = normalize(fin) ?: return null
        for (m in 1..36) if (plus(a, months = m) == b) return m
        return null
    }

    /** Nombre de la modalidad para [months] meses ("3 Meses", "1 Año"...). */
    fun modalidadLabel(months: Int): String = when {
        months == 12 -> "1 Año"
        months % 12 == 0 -> "${months / 12} Años"
        months == 1 -> "1 Mes"
        else -> "$months Meses"
    }

    private val MESES_CORTOS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "set", "oct", "nov", "dic")

    /** Fecha para mostrar: "2026-11-05" → "05 nov 2026". Si no es una fecha válida, se devuelve tal cual. */
    fun pretty(date: String): String {
        val n = normalize(date) ?: return date
        val m = n.substring(5, 7).toInt()
        return "${n.substring(8, 10)} ${MESES_CORTOS[m - 1]} ${n.substring(0, 4)}"
    }
}
