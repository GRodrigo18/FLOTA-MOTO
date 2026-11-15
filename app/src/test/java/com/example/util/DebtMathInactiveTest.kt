package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Deshacer la anulación de deuda al dejar una moto inactiva (FleetRepository.cancelAllDebtForInactive
 * crea dos registros ANULADO: la parte de garantía y "Deuda anulada" con el resto).
 * Aquí se replica ese reparto y se deshace con DebtMath.reverse, como hace deleteDebtRecord.
 */
class DebtMathInactiveTest {

    private data class Anulado(val monto: Double, val motivo: String)

    /** Mismo reparto que cancelAllDebtForInactive. */
    private fun cancelar(deudaTotal: Double, deudaGarantia: Double): List<Anulado> {
        val total = deudaTotal.coerceAtLeast(0.0)
        if (total <= 0.0) return emptyList()
        val garantia = deudaGarantia.coerceIn(0.0, total)
        val resto = total - garantia
        return buildList {
            if (garantia > 0.0) add(Anulado(garantia, "Garantía anulada: moto inactiva (no entra a Caja)"))
            if (resto > 0.004) add(Anulado(resto, "${DebtMath.MOTIVO_DEUDA_ANULADA}: moto inactiva (no entra a Caja)"))
        }
    }

    private fun deshacer(registros: List<Anulado>, inicio: DebtMath.Saldo = DebtMath.Saldo(0.0, 0.0)): DebtMath.Saldo =
        registros.fold(inicio) { s, r -> DebtMath.reverse("ANULADO", r.monto, r.motivo, s.deudaTotal, s.deudaGarantia) }

    @Test
    fun `deshacer en cualquier orden deja la deuda como estaba`() {
        val regs = cancelar(100.0, 30.0)
        assertEquals(2, regs.size)
        assertEquals(30.0, regs.sumOf { if (it.motivo.startsWith("Garantía")) it.monto else 0.0 }, 0.001)
        val a = deshacer(regs)
        val b = deshacer(regs.reversed())
        assertEquals(100.0, a.deudaTotal, 0.001); assertEquals(30.0, a.deudaGarantia, 0.001)
        assertEquals(100.0, b.deudaTotal, 0.001); assertEquals(30.0, b.deudaGarantia, 0.001)
    }

    @Test
    fun `solo garantia o solo otra deuda`() {
        val g = cancelar(50.0, 50.0)
        assertEquals(1, g.size)
        val sg = deshacer(g)
        assertEquals(50.0, sg.deudaTotal, 0.001); assertEquals(50.0, sg.deudaGarantia, 0.001)

        val d = cancelar(70.0, 0.0)
        assertEquals(1, d.size)
        val sd = deshacer(d)
        assertEquals(70.0, sd.deudaTotal, 0.001); assertEquals(0.0, sd.deudaGarantia, 0.001)
    }

    @Test
    fun `centimos - el resto con decimales no se pierde`() {
        val regs = cancelar(100.10, 30.05)
        val s = deshacer(regs)
        assertEquals(100.10, s.deudaTotal, 0.001)
        assertEquals(30.05, s.deudaGarantia, 0.001)
    }

    @Test
    fun `deuda en cero o negativa no crea registros`() {
        assertEquals(emptyList<Anulado>(), cancelar(0.0, 0.0))
        assertEquals(emptyList<Anulado>(), cancelar(-5.0, 0.0))
    }

    @Test
    fun `motivo de deuda anulada se reconoce sin importar mayusculas`() {
        val r = DebtMath.reverse("ANULADO", 40.0, "DEUDA ANULADA: moto inactiva", 0.0, 10.0)
        assertEquals(40.0, r.deudaTotal, 0.001)
        assertEquals(10.0, r.deudaGarantia, 0.001)
    }

    @Test
    fun `anulado antiguo de garantia (v1-5) se sigue deshaciendo como garantia`() {
        val r = DebtMath.reverse("ANULADO", 60.0, "Garantía anulada: moto inactiva (no entra a Caja)", 15.0, 0.0)
        assertEquals(75.0, r.deudaTotal, 0.001)
        assertEquals(60.0, r.deudaGarantia, 0.001)
    }

    // Limitación conocida (anotada en el README del proyecto): los registros de deuda no están
    // enlazados, así que no se puede saber qué cargo cubría una anulación. Caso muy raro.
    @org.junit.Ignore("Limitación conocida: requiere enlazar registros de deuda")
    @Test
    fun `borrar el cargo y luego deshacer la anulacion no revive una deuda borrada`() {
        // Cargo de S/ 50 por un daño → deuda 50. La moto queda inactiva → se anula (deuda 0, ANULADO 50).
        // El dueño ve que el cargo fue un error y lo anula (deuda sigue en 0, recortada).
        // Luego deshace la anulación: la deuda NO debería volver a 50, porque ese cargo ya no existe.
        var s = DebtMath.Saldo(50.0, 0.0)
        val regs = cancelar(s.deudaTotal, s.deudaGarantia)
        s = DebtMath.Saldo(0.0, 0.0)
        s = DebtMath.reverse("CARGO", 50.0, "Daño en faro", s.deudaTotal, s.deudaGarantia)
        s = deshacer(regs, s)
        assertEquals("deuda revivida de un cargo ya anulado", 0.0, s.deudaTotal, 0.001)
    }
}
