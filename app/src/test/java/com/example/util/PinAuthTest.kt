package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinAuthTest {

    @Test
    fun `acepta PIN de 4 a 6 digitos`() {
        assertTrue(PinAuth.isValidPin("1234"))
        assertTrue(PinAuth.isValidPin("12345"))
        assertTrue(PinAuth.isValidPin("123456"))
    }

    @Test
    fun `rechaza PIN corto, largo o con letras`() {
        assertFalse(PinAuth.isValidPin("123"))
        assertFalse(PinAuth.isValidPin("1234567"))
        assertFalse(PinAuth.isValidPin("12a4"))
        assertFalse(PinAuth.isValidPin(""))
    }

    @Test
    fun `el hash es estable para el mismo PIN y sal`() {
        val salt = "00112233445566778899aabbccddeeff"
        assertEquals(PinAuth.hashPin("4821", salt), PinAuth.hashPin("4821", salt))
    }

    @Test
    fun `PIN distinto o sal distinta dan hash distinto`() {
        val salt = "00112233445566778899aabbccddeeff"
        val otherSalt = "ffeeddccbbaa99887766554433221100"
        assertNotEquals(PinAuth.hashPin("4821", salt), PinAuth.hashPin("4822", salt))
        assertNotEquals(PinAuth.hashPin("4821", salt), PinAuth.hashPin("4821", otherSalt))
    }

    @Test
    fun `el hash no contiene el PIN y mide 64 caracteres hex`() {
        // Sal fija: con una sal al azar, el hash podía contener "4821" por casualidad (prueba inestable)
        val hash = PinAuth.hashPin("4821", "0f1e2d3c4b5a69788796a5b4c3d2e1f0")
        assertEquals(64, hash.length)
        assertFalse(hash.contains("4821"))
    }
}
