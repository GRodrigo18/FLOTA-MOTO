package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyReportTest {

    @Test
    fun `campos con comas o comillas se escapan`() {
        assertEquals("simple", MonthlyReport.field("simple"))
        assertEquals("\"a, b\"", MonthlyReport.field("a, b"))
        assertEquals("\"dijo \"\"hola\"\"\"", MonthlyReport.field("dijo \"hola\""))
    }

    @Test
    fun `reporte con totales y balance`() {
        val csv = MonthlyReport.build(
            "Noviembre 2026",
            listOf(
                MonthlyReport.UnitRow("Moto 1", "AAA", "Juan", "Vigente", 4, 140.0, 50.0, 0.0),
                MonthlyReport.UnitRow("Moto 2", "BBB", "Ana", "En Taller", 2, 70.0, 0.0, 30.0)
            ),
            listOf(
                MonthlyReport.MovementRow("2026-11-02", true, "Pago Diario", "Cuota", "AAA", 35.0),
                MonthlyReport.MovementRow("2026-11-01", false, "Mantenimiento Moto", "Frenos, pastillas", "AAA", 50.0)
            )
        )
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("TOTAL,,,,6,210.00,50.00,30.00"))
        assertTrue(csv.contains("\"Frenos, pastillas\""))
        assertTrue(csv.contains("Balance del mes,-15.00"))
        // Movimientos ordenados por fecha
        assertTrue(csv.indexOf("2026-11-01") < csv.indexOf("2026-11-02"))
    }
}
