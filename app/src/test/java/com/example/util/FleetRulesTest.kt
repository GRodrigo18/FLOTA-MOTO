package com.example.util

import com.example.util.FleetRules.Sort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FleetRulesTest {

    private data class U(val n: String, val p: String, val e: String, val d: Double)
    private val key = { u: U -> FleetRules.SortKey(u.n, u.p, u.e, u.d) }
    private val motos = listOf(
        U("Moto 10", "B2", "Vigente", 0.0),
        U("Moto 2", "A1", "Inactivo", 50.0),
        U("Moto 1", "C3", "Por Vencer", 120.0),
        U("Moto 3", "A0", "En Taller", 0.0)
    )

    @Test
    fun `por nombre Moto 2 va antes que Moto 10`() {
        assertEquals(listOf("Moto 1", "Moto 2", "Moto 3", "Moto 10"), FleetRules.sortUnits(motos, Sort.NOMBRE, key).map { it.n })
    }

    @Test
    fun `por placa, estado y deuda`() {
        assertEquals(listOf("A0", "A1", "B2", "C3"), FleetRules.sortUnits(motos, Sort.PLACA, key).map { it.p })
        assertEquals(listOf("Por Vencer", "En Taller", "Vigente", "Inactivo"), FleetRules.sortUnits(motos, Sort.ESTADO, key).map { it.e })
        assertEquals(listOf("Moto 1", "Moto 2", "Moto 3", "Moto 10"), FleetRules.sortUnits(motos, Sort.DEUDA, key).map { it.n })
    }

    @Test
    fun `inactiva anula solo la garantia pendiente`() {
        val r = FleetRules.cancelGuarantee(deudaTotal = 150.0, deudaGarantia = 100.0)
        assertEquals(100.0, r.anulado, 0.001)
        assertEquals(50.0, r.nuevaDeudaTotal, 0.001)
        assertEquals(0.0, r.nuevaDeudaGarantia, 0.001)
        assertEquals(0.0, FleetRules.cancelGuarantee(60.0, 100.0).nuevaDeudaTotal, 0.001)
        assertEquals(0.0, FleetRules.cancelGuarantee(80.0, 0.0).anulado, 0.001)
    }

    @Test
    fun `cambio de nombre y placa`() {
        assertNull(FleetRules.validateRename("Moto 5", "abc 123", listOf("XYZ999")))
        assertNotNull(FleetRules.validateRename("Moto 5", "abc123", listOf("ABC 123")))
        assertNotNull(FleetRules.validateRename("  ", "ABC123", emptyList()))
        assertEquals("ABC123", FleetRules.normalizePlaca(" abc 123 "))
    }

    @Test
    fun `solo paga cuota la moto que trabaja`() {
        assertFalse(FleetRules.debeCuota("Inactivo"))
        assertFalse(FleetRules.debeCuota("En Taller"))
        assertTrue(FleetRules.debeCuota("Vigente"))
    }
}
