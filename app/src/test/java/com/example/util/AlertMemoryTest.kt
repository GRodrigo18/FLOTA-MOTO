package com.example.util

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertMemoryTest {

    private val day1 = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 15, 10, 0) }.timeInMillis
    private val day2 = day1 + 24 * 60 * 60 * 1000L

    @Test
    fun `cada alerta se avisa una sola vez por dia`() {
        assertEquals(listOf("b"), AlertMemory.pendingToNotify("2026-10-15", "2026-10-15", setOf("a"), listOf("a", "b")))
        assertEquals(listOf("a", "b"), AlertMemory.pendingToNotify("2026-10-16", "2026-10-15", setOf("a"), listOf("a", "b")))
    }

    @Test
    fun `quitado de Inicio se recuerda sin caducar al dia siguiente`() {
        val prefs = FakeSharedPreferences()
        AlertMemory.saveHomeHidden(prefs, setOf("contrato_ABC#pronto", "deuda_ABC"))
        assertEquals(setOf("contrato_ABC#pronto", "deuda_ABC"), AlertMemory.homeHidden(prefs))
        AlertMemory.saveHomeHidden(prefs, emptySet())
        assertTrue(AlertMemory.homeHidden(prefs).isEmpty())
    }

    @Test
    fun `las claves viejas por dia de la v1_4 se ignoran y se borran`() {
        val prefs = FakeSharedPreferences()
        prefs.edit().putString("alerts_dismissed_day", "2026-10-15").putString("alerts_dismissed_ids", "soat_ABC").apply()
        assertTrue(AlertMemory.homeHidden(prefs).isEmpty())
        AlertMemory.saveHomeHidden(prefs, setOf("x"))
        assertFalse(prefs.contains("alerts_dismissed_ids"))
        assertFalse(prefs.contains("alerts_dismissed_day"))
    }

    @Test
    fun `prune olvida lo resuelto y conserva lo que sigue activo`() {
        val hidden = setOf("deuda_A", "contrato_B#pronto", "stock_aceite")
        val active = listOf("deuda_A", "contrato_B#pronto", "soat_C#pronto")
        assertEquals(setOf("deuda_A", "contrato_B#pronto"), AlertMemory.prune(hidden, active))
    }

    @Test
    fun `prune quita la etapa anterior cuando el contrato pasa a vencido`() {
        val hidden = setOf("contrato_B#pronto")
        assertTrue(AlertMemory.prune(hidden, listOf("contrato_B#vencido")).isEmpty())
    }

    @Test
    fun `prune sin recordatorios activos lo olvida todo`() {
        assertTrue(AlertMemory.prune(setOf("a", "b"), emptyList()).isEmpty())
    }

    @Test
    fun `las alertas avisadas se acumulan durante el dia`() {
        val prefs = FakeSharedPreferences()
        AlertMemory.markNotified(prefs, listOf("x"), day1)
        AlertMemory.markNotified(prefs, listOf("y"), day1)
        assertEquals(setOf("x", "y"), AlertMemory.notifiedToday(prefs, day1))
        assertTrue(AlertMemory.notifiedToday(prefs, day2).isEmpty())
    }
}
