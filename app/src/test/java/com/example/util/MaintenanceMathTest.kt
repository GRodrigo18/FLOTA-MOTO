package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MaintenanceMathTest {

    @Test
    fun `dos llantas de 85 cuestan 170, no 340`() {
        assertEquals(170.0, MaintenanceMath.total(servicios = 0.0, costoTrabajo = 170.0, aceite = 0.0, llantas = 170.0), 0.001)
    }

    @Test
    fun `servicio mas aceite mas llantas se suman una sola vez`() {
        assertEquals(240.0, MaintenanceMath.total(servicios = 40.0, costoTrabajo = 240.0, aceite = 30.0, llantas = 170.0), 0.001)
    }

    @Test
    fun `cables de cambios se cobran como servicio`() {
        assertEquals(35.0, MaintenanceMath.total(servicios = 35.0, costoTrabajo = 35.0, aceite = 0.0, llantas = 0.0), 0.001)
    }

    @Test
    fun `solo aceite no se duplica`() {
        assertEquals(30.0, MaintenanceMath.total(servicios = 0.0, costoTrabajo = 30.0, aceite = 30.0, llantas = 0.0), 0.001)
    }
}
