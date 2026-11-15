package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissedPaymentsTest {

    private val hoy = "2026-11-14"

    @Test
    fun `cuenta los dias sin pago desde que se lleva el control`() {
        // Control desde el 10; pagó el 10 y el 12 → faltan 11 y 13 (hoy aún no, son las 10:00)
        val faltas = MissedPayments.missedDays(hoy, 10, "2026-09-01", "2026-11-10", null, setOf("2026-11-10", "2026-11-12"))
        assertEquals(listOf("2026-11-11", "2026-11-13"), faltas)
    }

    @Test
    fun `hoy cuenta solo desde las 17`() {
        val temprano = MissedPayments.missedDays(hoy, 16, "2026-11-14", "2026-11-01", null, emptySet())
        val tarde = MissedPayments.missedDays(hoy, 17, "2026-11-14", "2026-11-01", null, emptySet())
        assertTrue(temprano.isEmpty())
        assertEquals(listOf("2026-11-14"), tarde)
    }

    @Test
    fun `no se cuentan dias antes del contrato ni antes de registrar la moto`() {
        assertEquals(listOf("2026-11-13"), MissedPayments.missedDays(hoy, 10, "2026-11-13", "2026-11-01", null, emptySet()))
        assertEquals(listOf("2026-11-13"), MissedPayments.missedDays(hoy, 10, "2026-01-01", "2026-11-01", "2026-11-13", emptySet()))
    }

    @Test
    fun `un dia marcado No trabajo ya no es falta`() {
        // Los días "No trabajó" llegan en los registros igual que los pagos
        val faltas = MissedPayments.missedDays(hoy, 10, "2026-11-01", "2026-11-12", null, setOf("2026-11-12"))
        assertEquals(listOf("2026-11-13"), faltas)
    }

    @Test
    fun `como maximo 60 dias hacia atras`() {
        val faltas = MissedPayments.missedDays(hoy, 18, "2025-01-01", "2025-01-01", null, emptySet())
        assertEquals(MissedPayments.MAX_DIAS, faltas.size)
        assertEquals(hoy, faltas.last())
    }

    @Test
    fun `dias para marcar al volver del taller`() {
        val dias = MissedPayments.daysToExcuse("2026-11-10", "2026-11-13", setOf("2026-11-11"))
        assertEquals(listOf("2026-11-10", "2026-11-12", "2026-11-13"), dias)
        assertTrue(MissedPayments.daysToExcuse("2026-11-14", "2026-11-13", emptySet()).isEmpty())
    }

    @Test
    fun `etiquetas de dia y resumen corto`() {
        assertEquals("sáb 14 nov", MissedPayments.label("2026-11-14"))
        val dias = listOf("2026-11-08", "2026-11-09", "2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13")
        assertEquals("mar 10 nov, mié 11 nov, jue 12 nov, vie 13 nov (y 2 días antes)", MissedPayments.summary(dias))
    }

    @Test
    fun `por moto usa los registros de cada una`() {
        val m = MissedPayments.byPlaca(
            today = hoy, hourOfDay = 10, desde = "2026-11-12", agregadas = emptyMap(),
            units = mapOf("A" to "2026-01-01", "B" to "2026-01-01"),
            registros = listOf("A" to "2026-11-12", "A" to "2026-11-13", "B" to "2026-11-13")
        )
        assertEquals(mapOf("B" to listOf("2026-11-12")), m)
    }

    @Test
    fun `sin fecha de control no inventa faltas viejas`() {
        val m = MissedPayments.byPlaca(hoy, 10, "", emptyMap(), mapOf("A" to "2026-01-01"), emptyList())
        assertTrue(m.isEmpty())
    }
}
