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
    fun `dias sin pagar avisan por moto y con las fechas`() {
        val motos = listOf(
            unit("A"), // debe dos días
            unit("B"), // al día
            unit("C", cliente = "Sin asignar"), // sin chofer: no debe cuota
            unit("D", estado = "En Taller") // en taller: no debe cuota
        )
        val faltas = mapOf(
            "A" to listOf("2026-10-14", "2026-10-15"),
            "C" to listOf("2026-10-15"),
            "D" to listOf("2026-10-15")
        )
        val alerts = AlertEngine.build(motos, emptySet(), null, hourOfDay = 18, now = now, missedByPlaca = faltas)
        assertEquals(listOf("faltas_A"), alerts.map { it.id })
        val a = alerts.single()
        assertEquals("No pagó 2 días • Moto A", a.titulo)
        assertTrue(a.descripcion.contains("mié 14 oct"))
        assertTrue(a.descripcion.contains("jue 15 oct"))
        assertEquals("A", a.unitPlaca)
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
        val alerts = AlertEngine.build(
            motos, emptySet(), stockAceite = 2, hourOfDay = 18, now = now, stockLlantas = 0,
            missedByPlaca = mapOf("B" to listOf("2026-10-15"))
        )
        assertEquals(
            setOf("soat_A", "rev_A", "contrato_A", "deuda_A", "faltas_B", "stock_llantas", "stock_aceite"),
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
        alerts.forEach { assertEquals(it.id, it.hideKey) }
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
            listOf("soat_C", "contrato_B", "soat_A", "contrato_A", "deuda_A", "rev_A", "stock_aceite"),
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
    fun `moto inactiva no avisa nada (su deuda se anula al dejarla inactiva)`() {
        val u = unit("A", soatDias = 0, finContrato = "2026-10-01", deuda = 120.0, revisionPendiente = true, estado = "Inactivo")
        val alerts = AlertEngine.build(listOf(u), emptySet(), null, hourOfDay = 18, now = now,
            missedByPlaca = mapOf("A" to listOf("2026-10-15")))
        assertTrue(alerts.isEmpty())
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
    fun `un dia nuevo sin pagar cambia la clave y vuelve a Inicio`() {
        val ayer = AlertEngine.build(listOf(unit("A")), emptySet(), null, hourOfDay = 10, now = now,
            missedByPlaca = mapOf("A" to listOf("2026-10-14"))).single()
        val hoy = AlertEngine.build(listOf(unit("A")), emptySet(), null, hourOfDay = 18, now = now,
            missedByPlaca = mapOf("A" to listOf("2026-10-14", "2026-10-15"))).single()
        assertNotEquals(ayer.hideKey, hoy.hideKey)
        assertEquals("faltas_A", hoy.id)
    }

    @Test
    fun `solo hoy sin pagar dice No pago hoy`() {
        val a = AlertEngine.build(listOf(unit("A")), emptySet(), null, hourOfDay = 18, now = now,
            missedByPlaca = mapOf("A" to listOf("2026-10-15"))).single()
        assertEquals("No pagó hoy • Moto A", a.titulo)
    }

    @Test
    fun `copias de seguridad pendientes van a la campana`() {
        val hora = 60 * 60 * 1000L
        val sinArchivo = BackupStatus(driveLinked = false, lastDriveAt = 0L, lastEmailAt = now, hasData = true)
        assertEquals(listOf("respaldo_drive"), AlertEngine.build(emptyList(), emptySet(), null, 10, now = now, backup = sinArchivo).map { it.id })

        val vieja = BackupStatus(driveLinked = true, lastDriveAt = now - 72 * hora, lastEmailAt = now - 10 * 24 * hora, hasData = true)
        val alerts = AlertEngine.build(emptyList(), emptySet(), null, 10, now = now, backup = vieja)
        assertEquals(setOf("respaldo_drive", "respaldo_correo"), alerts.map { it.id }.toSet())
        assertTrue(alerts.all { it.tipo == AlertType.RESPALDO })

        val alDia = BackupStatus(driveLinked = true, lastDriveAt = now - hora, lastEmailAt = now - 24 * hora, hasData = true)
        assertTrue(AlertEngine.build(emptyList(), emptySet(), null, 10, now = now, backup = alDia).isEmpty())
        // Sin motos no hay nada que respaldar
        assertTrue(AlertEngine.build(emptyList(), emptySet(), null, 10, now = now, backup = sinArchivo.copy(hasData = false)).isEmpty())
    }

    @Test
    fun `elegir el archivo de Drive cambia la clave del aviso de copia`() {
        val a = AlertEngine.build(emptyList(), emptySet(), null, 10, now = now,
            backup = BackupStatus(false, 0L, now, true)).single()
        val b = AlertEngine.build(emptyList(), emptySet(), null, 10, now = now,
            backup = BackupStatus(true, now - 100 * 60 * 60 * 1000L, now, true)).single()
        assertNotEquals(a.hideKey, b.hideKey)
    }
}
