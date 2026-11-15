package com.example.ui

import com.example.util.FleetRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Recordatorio "días sin pagar" por moto: casos límite (QA v1.6). */
class AlertEngineMissedTest {

    private fun at(y: Int, m: Int, d: Int, h: Int = 18): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, 0, 0) }.timeInMillis

    private val now = at(2026, 11, 14)

    private fun unit(placa: String, estado: String = "Vigente", cliente: String = "Juan", cuota: Double = 35.0, deuda: Double = 0.0) =
        AlertUnit(placa, "Moto $placa", estado, cliente, 200, "2027-06-01", false, "2027-06-01", deuda, cuota)

    private fun build(units: List<AlertUnit>, faltas: Map<String, List<String>>, hour: Int = 18) =
        AlertEngine.build(units, emptySet(), null, hour, now = now, missedByPlaca = faltas)

    @Test
    fun `solo hoy dice No pago hoy`() {
        val a = build(listOf(unit("A")), mapOf("A" to listOf("2026-11-14"))).single()
        assertEquals("No pagó hoy • Moto A", a.titulo)
        assertEquals("Hoy", a.hora)
        assertTrue(a.descripcion.contains("S/ 35.00"))
    }

    @Test
    fun `un dia que no es hoy usa singular`() {
        val a = build(listOf(unit("A")), mapOf("A" to listOf("2026-11-12"))).single()
        assertEquals("No pagó 1 día • Moto A", a.titulo)
        assertEquals("1 día", a.hora)
    }

    @Test
    fun `monto total es cuota por dias y sin cuota no se muestra monto`() {
        val dias = listOf("2026-11-11", "2026-11-12", "2026-11-13")
        assertTrue(build(listOf(unit("A")), mapOf("A" to dias)).single().descripcion.contains("S/ 105.00"))
        assertFalse(build(listOf(unit("A", cuota = 0.0)), mapOf("A" to dias)).single().descripcion.contains("S/"))
    }

    @Test
    fun `moto Por Vencer tambien avisa, inactiva y en taller no`() {
        val f = listOf("2026-11-13")
        val ids = build(
            listOf(unit("A", estado = "Por Vencer"), unit("B", estado = "Inactivo", deuda = 80.0), unit("C", estado = "En Taller")),
            mapOf("A" to f, "B" to f, "C" to f)
        ).map { it.id }
        assertEquals(listOf("faltas_A"), ids)
    }

    @Test
    fun `chofer en blanco no avisa`() {
        assertTrue(build(listOf(unit("A", cliente = "  ")), mapOf("A" to listOf("2026-11-13"))).isEmpty())
        assertTrue(build(listOf(unit("A", cliente = "SIN ASIGNAR")), mapOf("A" to listOf("2026-11-13"))).isEmpty())
    }

    @Test
    fun `placa sin faltas o con lista vacia no avisa`() {
        assertTrue(build(listOf(unit("A")), mapOf("A" to emptyList())).isEmpty())
        assertTrue(build(listOf(unit("A")), mapOf("OTRA" to listOf("2026-11-13"))).isEmpty())
    }

    @Test
    fun `clave de ocultar cambia con un dia nuevo y no con uno viejo resuelto`() {
        val antes = build(listOf(unit("A")), mapOf("A" to listOf("2026-11-10", "2026-11-12"))).single()
        val viejoResuelto = build(listOf(unit("A")), mapOf("A" to listOf("2026-11-12"))).single()
        val diaNuevo = build(listOf(unit("A")), mapOf("A" to listOf("2026-11-10", "2026-11-12", "2026-11-14"))).single()
        assertEquals("faltas_A#2026-11-12", antes.hideKey)
        assertEquals(antes.hideKey, viejoResuelto.hideKey)
        assertTrue(antes.hideKey != diaNuevo.hideKey)
        assertFalse(antes.isVencido)
    }

    @Test
    fun `faltas van despues de SOAT y contrato y antes de la revision y el stock`() {
        val u = AlertUnit("A", "Moto A", "Vigente", "Juan", 3, "2026-11-17", true, "2026-11-15", 0.0, 35.0)
        val sorted = AlertEngine.sortByUrgency(
            AlertEngine.build(listOf(u), emptySet(), 2, 18, now = now, missedByPlaca = mapOf("A" to listOf("2026-11-13")))
        ).map { it.id }
        assertEquals(listOf("soat_A", "contrato_A", "faltas_A", "rev_A", "stock_aceite"), sorted)
    }

    @Test
    fun `la alerta y el cobro diario usan la misma regla de quien debe cuota`() {
        // La pantalla de cobro (missedByPlaca) usa FleetRules.debeCuota; la alerta usa debeCuotaDiaria.
        // Con cualquier estado (p. ej. uno que llegó de una copia vieja) deben coincidir; si no, la moto
        // muestra «Sin pagar: N días» en Cobro pero nunca sale el recordatorio.
        for (estado in listOf("Vigente", "Por Vencer", "En Taller", "Inactivo", "Activo", "Vencido")) {
            assertEquals(
                "estado $estado",
                FleetRules.debeCuota(estado),
                AlertEngine.debeCuotaDiaria(unit("A", estado = estado))
            )
        }
    }
}
