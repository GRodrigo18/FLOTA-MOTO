package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MissedPrefsTest {

    @Test
    fun `recuerda fechas por placa y las mueve al cambiar de placa`() {
        val prefs = FakeSharedPreferences()
        MissedPrefs.setDesde(prefs, "2026-11-01")
        MissedPrefs.setAgregada(prefs, "ABC123", "2026-11-05")
        MissedPrefs.setParadas(prefs, mapOf("ABC123" to "2026-11-10"))
        MissedPrefs.rename(prefs, "ABC123", "XYZ789")
        assertEquals(mapOf("XYZ789" to "2026-11-05"), MissedPrefs.agregadas(prefs))
        assertEquals(mapOf("XYZ789" to "2026-11-10"), MissedPrefs.paradas(prefs))
        MissedPrefs.remove(prefs, "XYZ789")
        assertEquals(emptyMap<String, String>(), MissedPrefs.agregadas(prefs))
        MissedPrefs.clear(prefs)
        assertNull(MissedPrefs.desde(prefs))
    }
}
