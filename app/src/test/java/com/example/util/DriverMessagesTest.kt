package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverMessagesTest {

    private fun st(chofer: String = "Juan Pérez", sinPago: Int = 2, garantia: Double = 0.0) = DriverMessages.Statement(
        moto = "Moto 3", placa = "ABC123", chofer = chofer, mesLabel = "Noviembre 2026",
        diasPagados = 3, totalPagado = 105.0, diasSinPago = sinPago,
        deudaTotal = 50.0, garantiaPendiente = garantia, finContrato = "2027-01-31"
    )

    @Test
    fun `estado de cuenta con cuotas, dias sin pago y deuda`() {
        val t = DriverMessages.statement(st())
        assertTrue(t.contains("Noviembre 2026"))
        assertTrue(t.contains("Chofer: Juan Pérez"))
        assertTrue(t.contains("3 día(s) • S/ 105.00"))
        assertTrue(t.contains("Días sin pago registrado: 2"))
        assertTrue(t.contains("Deuda pendiente: S/ 50.00"))
    }

    @Test
    fun `sin chofer ni dias pendientes no muestra esas lineas`() {
        val t = DriverMessages.statement(st(chofer = "Sin asignar", sinPago = 0))
        assertFalse(t.contains("Chofer:"))
        assertFalse(t.contains("sin pago"))
    }

    @Test
    fun `muestra la garantia pendiente`() {
        assertTrue(DriverMessages.statement(st(garantia = 100.0)).contains("garantía: S/ 100.00"))
    }

    @Test
    fun `dias sin pago del mes`() {
        // Hoy 5 de noviembre, pagó el 1, 2 y 4 → faltan el 3 y el 5
        assertEquals(2, DriverMessages.unpaidDays("2026-11", "2026-11-05", "2026-08-01", setOf("2026-11-01", "2026-11-02", "2026-11-04")))
        // Contrato empezó el 4: solo cuentan 4 y 5
        assertEquals(1, DriverMessages.unpaidDays("2026-11", "2026-11-05", "2026-11-04", setOf("2026-11-04")))
        // Mes pasado completo (30 días)
        assertEquals(30, DriverMessages.unpaidDays("2026-09", "2026-11-05", "", emptySet()))
    }
}
