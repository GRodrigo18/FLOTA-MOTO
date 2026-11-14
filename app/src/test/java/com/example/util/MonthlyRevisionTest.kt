package com.example.util

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyRevisionTest {

    private fun at(y: Int, m: Int, d: Int) =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, 10, 0) }.timeInMillis

    @Test
    fun `la primera vez no reinicia (no inventa pendientes)`() {
        val prefs = FakeSharedPreferences()
        assertFalse(MonthlyRevision.checkAndMark(prefs, at(2026, 10, 15)))
    }

    @Test
    fun `en el mismo mes no reinicia`() {
        val prefs = FakeSharedPreferences()
        MonthlyRevision.checkAndMark(prefs, at(2026, 10, 1))
        assertFalse(MonthlyRevision.checkAndMark(prefs, at(2026, 10, 30)))
    }

    @Test
    fun `al empezar un mes nuevo reinicia una sola vez`() {
        val prefs = FakeSharedPreferences()
        MonthlyRevision.checkAndMark(prefs, at(2026, 10, 20))
        assertTrue(MonthlyRevision.checkAndMark(prefs, at(2026, 11, 1)))
        assertFalse(MonthlyRevision.checkAndMark(prefs, at(2026, 11, 2)))
    }

    @Test
    fun `cambio de año tambien cuenta`() {
        assertTrue(MonthlyRevision.shouldReset("2026-12", "2027-01"))
        assertEquals("2026-10", MonthlyRevision.currentMonth(at(2026, 10, 15)))
    }
}
