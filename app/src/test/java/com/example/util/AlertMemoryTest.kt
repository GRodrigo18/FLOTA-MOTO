package com.example.util

import java.util.Calendar
import org.junit.Assert.assertEquals
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
    fun `una alerta oculta sigue oculta al reabrir la app y vuelve al dia siguiente`() {
        val prefs = FakeSharedPreferences()
        AlertMemory.saveDismissed(prefs, setOf("soat_ABC"), day1)
        assertEquals(setOf("soat_ABC"), AlertMemory.dismissedToday(prefs, day1))
        assertTrue(AlertMemory.dismissedToday(prefs, day2).isEmpty())
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
