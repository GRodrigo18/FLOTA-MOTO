package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Preferencias de días sin pagar: casos límite (QA v1.6). */
class MissedPrefsEdgeTest {

    @Test
    fun `sin nada guardado devuelve vacio`() {
        val p = FakeSharedPreferences()
        assertNull(MissedPrefs.desde(p))
        assertEquals(emptyMap<String, String>(), MissedPrefs.agregadas(p))
        assertEquals(emptyMap<String, String>(), MissedPrefs.paradas(p))
    }

    @Test
    fun `json roto no revienta`() {
        val p = FakeSharedPreferences()
        p.edit().putString("motos_agregadas", "{no es json").putString("motos_paradas_desde", "[]").apply()
        assertEquals(emptyMap<String, String>(), MissedPrefs.agregadas(p))
        assertEquals(emptyMap<String, String>(), MissedPrefs.paradas(p))
        // Y se puede volver a escribir encima
        MissedPrefs.setAgregada(p, "A", "2026-11-01")
        assertEquals(mapOf("A" to "2026-11-01"), MissedPrefs.agregadas(p))
    }

    @Test
    fun `agregar una moto no borra las demas y re-agregar pisa la fecha`() {
        val p = FakeSharedPreferences()
        MissedPrefs.setAgregada(p, "A", "2026-11-01")
        MissedPrefs.setAgregada(p, "B", "2026-11-02")
        MissedPrefs.setAgregada(p, "A", "2026-11-05")
        assertEquals(mapOf("A" to "2026-11-05", "B" to "2026-11-02"), MissedPrefs.agregadas(p))
    }

    @Test
    fun `renombrar una placa que no esta no cambia nada`() {
        val p = FakeSharedPreferences()
        MissedPrefs.setAgregada(p, "A", "2026-11-01")
        MissedPrefs.rename(p, "NOEXISTE", "Z")
        MissedPrefs.rename(p, "A", "A")
        assertEquals(mapOf("A" to "2026-11-01"), MissedPrefs.agregadas(p))
    }

    @Test
    fun `renombrar solo mueve la placa pedida`() {
        val p = FakeSharedPreferences()
        MissedPrefs.setAgregada(p, "A", "2026-11-01")
        MissedPrefs.setAgregada(p, "B", "2026-11-02")
        MissedPrefs.setParadas(p, mapOf("B" to "2026-11-10"))
        MissedPrefs.rename(p, "A", "C")
        assertEquals(mapOf("C" to "2026-11-01", "B" to "2026-11-02"), MissedPrefs.agregadas(p))
        assertEquals(mapOf("B" to "2026-11-10"), MissedPrefs.paradas(p))
    }

    @Test
    fun `placas con caracteres raros se guardan bien`() {
        val p = FakeSharedPreferences()
        MissedPrefs.setAgregada(p, "AB-12\"3", "2026-11-01")
        MissedPrefs.setAgregada(p, "ÑU 77", "2026-11-02")
        assertEquals(mapOf("AB-12\"3" to "2026-11-01", "ÑU 77" to "2026-11-02"), MissedPrefs.agregadas(p))
    }

    @Test
    fun `clear borra todo y setDesde despues vuelve a quedar`() {
        val p = FakeSharedPreferences()
        MissedPrefs.setDesde(p, "2026-01-01")
        MissedPrefs.setAgregada(p, "A", "2026-11-01")
        MissedPrefs.setParadas(p, mapOf("A" to "2026-11-10"))
        MissedPrefs.clear(p)
        assertNull(MissedPrefs.desde(p))
        assertEquals(emptyMap<String, String>(), MissedPrefs.agregadas(p))
        assertEquals(emptyMap<String, String>(), MissedPrefs.paradas(p))
        MissedPrefs.setDesde(p, "2026-11-14")
        assertEquals("2026-11-14", MissedPrefs.desde(p))
    }

    @Test
    fun `setParadas vacio deja el mapa vacio`() {
        val p = FakeSharedPreferences()
        MissedPrefs.setParadas(p, mapOf("A" to "2026-11-10"))
        MissedPrefs.setParadas(p, emptyMap())
        assertEquals(emptyMap<String, String>(), MissedPrefs.paradas(p))
    }
}
