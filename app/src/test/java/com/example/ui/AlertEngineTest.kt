package com.example.ui

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
    fun `por compatibilidad, dismissed todavia filtra por id`() {
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

    @Test
    fun `sin dismissed se devuelven todos los recordatorios`() {
        val motos = listOf(
            unit("A", soatDias = 3, finContrato = "2026-10-16", deuda = 50.0, revisionPendiente = true),
            unit("B")
        )
        val alerts = AlertEngine.build(motos, emptySet(), stockAceite = 2, hourOfDay = 18, now = now, stockLlantas = 0)
        assertEquals(
            setOf("soat_A", "rev_A", "contrato_A", "deuda_A", "cobros_hoy", "stock_llantas", "stock_aceite"),
            alerts.map { it.id }.toSet()
        )
    }

    @Test
    fun `hideKey del SOAT cambia de pronto a vencido`() {
        val pronto = AlertEngine.build(listOf(unit("A", soatDias = 5)), emptySet(), null, 10, now = now).first()
        val vencido = AlertEngine.build(listOf(unit("A", soatDias = 0)), emptySet(), null, 10, now = now).first()
        assertEquals("soat_A#pronto", pronto.hideKey)
        assertEquals("soat_A#vencido", vencido.hideKey)
        assertEquals(pronto.id, vencido.id)
        assertFalse(pronto.isVencido)
        assertTrue(vencido.isVencido)
    }

    @Test
    fun `hideKey del contrato cambia al vencer, vence hoy sigue siendo pronto`() {
        val hoy = AlertEngine.build(listOf(unit("A", finContrato = "2026-10-15")), emptySet(), null, 10, now = now).first()
        val vencido = AlertEngine.build(listOf(unit("A", finContrato = "2026-10-14")), emptySet(), null, 10, now = now).first()
        assertEquals("contrato_A#pronto", hoy.hideKey)
        assertEquals("contrato_A#vencido", vencido.hideKey)
        assertNotEquals(hoy.hideKey, vencido.hideKey)
    }

    @Test
    fun `los demas recordatorios usan el id como hideKey`() {
        val alerts = AlertEngine.build(
            listOf(unit("A", deuda = 10.0, revisionPendiente = true)), emptySet(), stockAceite = 1, hourOfDay = 18, now = now
        )
        // "cobros_hoy" lleva la fecha en la clave (ver la prueba de cobros del día)
        alerts.filter { it.id != "cobros_hoy" }.forEach { assertEquals(it.id, it.hideKey) }
    }

    @Test
    fun `quitar de Inicio por contrato por vencer vuelve a mostrarlo al vencer`() {
        val hidden = setOf("contrato_A#pronto")
        val luego = AlertEngine.build(listOf(unit("A", finContrato = "2026-10-10")), emptySet(), null, 10, now = now)
        val enInicio = luego.filter { it.hideKey !in hidden }
        assertEquals(listOf("contrato_A"), enInicio.map { it.id })
    }

    @Test
    fun `sortByUrgency pone vencidos primero y luego SOAT, contrato, deuda, revision, cobros y stock`() {
        val motos = listOf(
            unit("A", soatDias = 10, finContrato = "2026-10-16", deuda = 20.0, revisionPendiente = true),
            unit("B", finContrato = "2026-10-01"), // contrato vencido
            unit("C", soatDias = -2) // SOAT vencido
        )
        val alerts = AlertEngine.build(motos, emptySet(), stockAceite = 3, hourOfDay = 18, now = now)
        val ordenados = AlertEngine.sortByUrgency(alerts.shuffled(java.util.Random(7)))
        assertEquals(
            listOf("soat_C", "contrato_B", "soat_A", "contrato_A", "deuda_A", "rev_A", "cobros_hoy", "stock_aceite"),
            ordenados.map { it.id }
        )
    }

    @Test
    fun `sortByUrgency mantiene el orden original en empates`() {
        val alerts = AlertEngine.build(
            listOf(unit("A", deuda = 1.0), unit("B", deuda = 2.0)), emptySet(), null, 10, now = now
        )
        assertEquals(listOf("deuda_A", "deuda_B"), AlertEngine.sortByUrgency(alerts).map { it.id })
    }

    @Test
    fun `moto inactiva con deuda sigue recordando la deuda (y nada mas)`() {
        val u = unit("A", soatDias = 0, finContrato = "2026-10-01", deuda = 120.0, revisionPendiente = true, estado = "Inactivo")
        val alerts = AlertEngine.build(listOf(u), emptySet(), null, hourOfDay = 10, now = now)
        assertEquals(listOf("deuda_A"), alerts.map { it.id })
    }

    @Test
    fun `SOAT que vence hoy no se llama vencido`() {
        val hoy = AlertUnit("A", "Moto A", "Vigente", "Juan", 0, "2026-10-15", false, "2027-01-01", 0.0)
        val alert = AlertEngine.build(listOf(hoy), emptySet(), null, hourOfDay = 10, now = now).single()
        assertTrue(alert.titulo.startsWith("SOAT por vencer"))
        assertTrue(alert.hideKey.endsWith(AlertEngine.STAGE_PRONTO))
        val ayer = hoy.copy(soatFin = "2026-10-14")
        val vencida = AlertEngine.build(listOf(ayer), emptySet(), null, hourOfDay = 10, now = now).single()
        assertTrue(vencida.titulo.startsWith("SOAT VENCIDO"))
        assertTrue(vencida.hideKey.endsWith(AlertEngine.STAGE_VENCIDO))
    }

    @Test
    fun `cobros del dia tienen una clave por dia`() {
        val hoy = AlertEngine.build(listOf(unit("A")), emptySet(), null, hourOfDay = 18, now = now).single { it.id == "cobros_hoy" }
        val manana = AlertEngine.build(listOf(unit("A")), emptySet(), null, hourOfDay = 18, now = at(2026, 10, 16)).single { it.id == "cobros_hoy" }
        assertTrue(hoy.hideKey != manana.hideKey)
    }
}
