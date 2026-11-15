package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DebtMathTest {

    @Test
    fun `lo que falta de la cuota`() {
        assertEquals(15.0, DebtMath.faltante(35.0, 20.0), 0.001)
        assertEquals(0.0, DebtMath.faltante(35.0, 35.0), 0.001)
        assertEquals(0.0, DebtMath.faltante(35.0, 50.0), 0.001)
    }

    @Test
    fun `anular un cargo baja la deuda`() {
        val r = DebtMath.reverse("CARGO", 50.0, "Daño en faro", 120.0, 70.0)
        assertEquals(70.0, r.deudaTotal, 0.001)
        assertEquals(70.0, r.deudaGarantia, 0.001) // no era de garantía
    }

    @Test
    fun `anular el cargo de garantia baja tambien la garantia`() {
        val r = DebtMath.reverse("CARGO", 100.0, "Saldo garantía por pagar (S/ 100.00 pagado de S/ 200.00)", 100.0, 100.0)
        assertEquals(0.0, r.deudaTotal, 0.001)
        assertEquals(0.0, r.deudaGarantia, 0.001)
    }

    @Test
    fun `nunca queda deuda negativa`() {
        assertEquals(0.0, DebtMath.reverse("CARGO", 80.0, "x", 30.0, 0.0).deudaTotal, 0.001)
    }

    @Test
    fun `anular un abono sube la deuda`() {
        assertEquals(90.0, DebtMath.reverse("ABONO", 40.0, "Abono", 50.0, 0.0).deudaTotal, 0.001)
    }

    @Test
    fun `deshacer una garantia anulada la vuelve deuda`() {
        val r = DebtMath.reverse("ANULADO", 100.0, "Garantía anulada", 20.0, 0.0)
        assertEquals(120.0, r.deudaTotal, 0.001)
        assertEquals(100.0, r.deudaGarantia, 0.001)
    }

    @Test
    fun `deshacer una deuda anulada (no garantia) solo vuelve a la deuda`() {
        val r = DebtMath.reverse("ANULADO", 80.0, "Deuda anulada: moto inactiva (no entra a Caja)", 0.0, 0.0)
        assertEquals(80.0, r.deudaTotal, 0.001)
        assertEquals(0.0, r.deudaGarantia, 0.001)
    }
}
