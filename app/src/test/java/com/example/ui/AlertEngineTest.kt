package com.example.ui

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEngineTest {

    private fun at(y: Int, m: Int, d: Int): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, 10, 0, 0) }.timeInMillis

    private fun unit(
        placa: String,
        soatDias: Int = 200,
        finContrato: String = "2027-01-01",
        deuda: Double = 0.0,
        revisionPendiente: Boolean = false,
        estado: String = "Vigente",
        cliente: String = "Juan"
    ) = AlertUnit(placa, "Moto $placa", estado, cliente, soatDias, "2026-12-31", revisionPendiente, finContrato, deuda)

    private val now = at(2026, 10, 15)

    @Test
    fun `moto sin problemas no genera alertas`() {
        val alerts = AlertEngine.build(listOf(unit("A")), emptySet(), null, hourOfDay = 10, now = now)
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `SOAT avisa dentro de los 15 dias, no antes`() {
        assertEquals(1, AlertEngine.build(listOf(unit("A", soatDias = 10)), emptySet(), null, 10, now = now).size)
        assertTrue(AlertEngine.build(listOf(unit("A", soatDias = 16)), emptySet(), null, 10, now = now).isEmpty())
    }

    @Test
    fun `SOAT vencido usa el titulo mas urgente`() {
        val alert = AlertEngine.build(listOf(unit("A", soatDias = 0)), emptySet(), null, 10, now = now).first()
        assertTrue(alert.titulo.startsWith("SOAT VENCIDO"))
    }

    @Test
    fun `antes el contrato solo avisaba si era semanal, ahora avisa por fecha real`() {
        val cerca = AlertEngine.build(listOf(unit("A", finContrato = "2026-10-17")), emptySet(), null, 10, now = now)
        assertEquals(1, cerca.size)
        assertEquals(AlertType.CONTRATO, cerca[0].tipo)

        val lejos = AlertEngine.build(listOf(unit("A", finContrato = "2026-11-30")), emptySet(), null, 10, now = now)
        assertTrue(lejos.isEmpty())
    }

    @Test
    fun `avisa cobros pendientes solo por la tarde y solo de quien debe cuota`() {
        val motos = listOf(
            unit("A"), // sin pagar
            unit("B"), // sin pagar
            unit("C", cliente = "Sin asignar"), // sin chofer: no debe cuota
            unit("D", estado = "En Taller") // en taller: no debe cuota
        )
        val pagados = setOf("A")

        val temprano = AlertEngine.build(motos, pagados, null, hourOfDay = 10, now = now)
        assertTrue(temprano.none { it.id == "cobros_hoy" })

        val tarde = AlertEngine.build(motos, pagados, null, hourOfDay = 18, now = now)
        val cobros = tarde.first { it.id == "cobros_hoy" }
        assertEquals("Cobros pendientes hoy: 1", cobros.titulo)
        assertTrue(cobros.descripcion.contains("Moto B"))
    }

    @Test
    fun `una alerta descartada no vuelve a aparecer`() {
        val alerts = AlertEngine.build(
            listOf(unit("A", soatDias = 3)), emptySet(), null, 10,
            dismissed = setOf("soat_A"), now = now
        )
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `stock bajo solo avisa si hay inventario registrado`() {
        assertEquals(1, AlertEngine.build(emptyList(), emptySet(), stockAceite = 4, hourOfDay = 10, now = now).size)
        assertTrue(AlertEngine.build(emptyList(), emptySet(), stockAceite = null, hourOfDay = 10, now = now).isEmpty())
    }

    @Test
    fun `motos inactivas no generan ninguna alerta`() {
        val alerts = AlertEngine.build(listOf(unit("A", soatDias = 1, estado = "Inactivo")), emptySet(), null, 10, now = now)
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `contrato vencido hace dias dice vencido, no vence hoy`() {
        val alert = AlertEngine.build(listOf(unit("A", finContrato = "2026-10-10")), emptySet(), null, 10, now = now).first()
        assertTrue(alert.titulo.startsWith("Contrato vencido"))
        assertTrue(alert.descripcion.contains("hace 5 días"))
    }

    @Test
    fun `avisa cuando quedan pocas llantas`() {
        val alerts = AlertEngine.build(emptyList(), emptySet(), null, 10, now = now, stockLlantas = 1, minimoLlantas = 2)
        assertEquals(listOf("stock_llantas"), alerts.map { it.id })
        assertTrue(AlertEngine.build(emptyList(), emptySet(), null, 10, now = now, stockLlantas = 5, minimoLlantas = 2).isEmpty())
    }
}
