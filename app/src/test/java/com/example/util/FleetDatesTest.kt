package com.example.util

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FleetDatesTest {

    private fun at(y: Int, m: Int, d: Int, hour: Int = 15): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, hour, 30, 0) }.timeInMillis

    @Test
    fun `dias hasta una fecha futura`() {
        assertEquals(10, FleetDates.daysUntil("2026-10-25", at(2026, 10, 15)))
        assertEquals(0, FleetDates.daysUntil("2026-10-15", at(2026, 10, 15)))
    }

    @Test
    fun `fecha vencida o invalida`() {
        assertEquals(0, FleetDates.daysUntil("2026-10-01", at(2026, 10, 15)))
        assertNull(FleetDates.daysUntil("15/10/2026", at(2026, 10, 15)))
        assertNull(FleetDates.daysUntil("2026-13-40", at(2026, 10, 15)))
    }

    @Test
    fun `el SOAT de un ano ya no queda en 365 dias con el tiempo`() {
        val dias = FleetDates.daysUntil("2027-09-01", at(2027, 8, 28))
        assertEquals(4, dias)
        assertTrue(dias!! <= 7)
    }

    @Test
    fun `etiquetas de vencimiento`() {
        assertEquals("Vencido", FleetDates.expiryLabel(0))
        assertEquals("1 Día", FleetDates.expiryLabel(1))
        assertEquals("12 Días", FleetDates.expiryLabel(12))
        assertEquals("3 Meses", FleetDates.expiryLabel(95))
    }

    @Test
    fun `avance del contrato`() {
        assertEquals(0.5f, FleetDates.contractProgress("2026-01-01", "2026-01-11", at(2026, 1, 6))!!, 0.001f)
        assertEquals(1f, FleetDates.contractProgress("2026-01-01", "2026-01-11", at(2026, 3, 1))!!, 0.001f)
        assertEquals(0f, FleetDates.contractProgress("2026-01-01", "2026-01-11", at(2025, 12, 1))!!, 0.001f)
        assertNull(FleetDates.contractProgress("2026-01-11", "2026-01-01"))
    }

    @Test
    fun `renovar contrato acepta fechas escritas a mano`() {
        assertEquals("2026-11-15", FleetDates.normalize("15/11/2026"))
        assertEquals("2026-11-15", FleetDates.normalize("2026-11-15"))
        assertNull(FleetDates.normalize("15 Nov 2026"))
        assertNull(FleetDates.normalize("2026-02-30"))
    }

    @Test
    fun `renovar contrato sugiere la misma modalidad`() {
        val hoy = at(2026, 11, 5)
        // Vencido: el nuevo contrato empieza hoy
        assertEquals("2026-11-05", FleetDates.renewalStart("2026-08-01", hoy))
        assertEquals("2027-02-05", FleetDates.suggestedRenewalEnd("2026-11-01", "3 Meses", hoy))
        // Aún vigente: empieza donde termina el actual
        assertEquals("2027-01-01", FleetDates.suggestedRenewalEnd("2026-12-01", "1 Mes", hoy))
        assertEquals("2026-12-08", FleetDates.suggestedRenewalEnd("2026-12-01", "1 Semana (Provisional)", hoy))
        // La fecha sugerida sí la entienden las alertas
        assertEquals(92, FleetDates.daysUntil("2027-02-05", hoy))
    }
}
