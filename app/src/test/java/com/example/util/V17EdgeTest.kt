package com.example.util

import com.example.ui.AlertEngine
import com.example.ui.AlertType
import com.example.ui.AlertUnit
import com.example.ui.BackupStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** QA v1.7: bordes de "avisar pagos desde", MissedPrefs y recordatorios de copia. */
class V17EdgeTest {

    private val hoy = "2026-11-24"
    private fun dia(offset: Int) = FleetDates.plus(hoy, days = offset)!!

    // ---------------- avisarDesde en MissedPayments ----------------

    @Test
    fun `fecha elegida en el futuro no genera faltas`() {
        val f = MissedPayments.missedDays(hoy, 20, "", "2026-01-01", null, emptySet(), avisarDesde = dia(3))
        assertTrue(f.isEmpty())
    }

    @Test
    fun `fecha elegida hoy cuenta solo desde las 17`() {
        assertTrue(MissedPayments.missedDays(hoy, 16, "", hoy, null, emptySet(), avisarDesde = hoy).isEmpty())
        assertEquals(listOf(hoy), MissedPayments.missedDays(hoy, 17, "", hoy, null, emptySet(), avisarDesde = hoy))
    }

    @Test
    fun `fecha elegida invalida vuelve al modo automatico`() {
        val auto = MissedPayments.missedDays(hoy, 10, "", dia(-3), null, emptySet())
        val basura = MissedPayments.missedDays(hoy, 10, "", dia(-3), null, emptySet(), avisarDesde = "no-es-fecha")
        assertEquals(auto, basura)
        assertEquals(3, auto.size)
    }

    @Test
    fun `fecha elegida en formato dd-MM-yyyy se acepta`() {
        val f = MissedPayments.missedDays(hoy, 10, "", hoy, null, emptySet(), avisarDesde = "20/11/2026")
        assertEquals(listOf("2026-11-20", "2026-11-21", "2026-11-22", "2026-11-23"), f)
    }

    @Test
    fun `fecha elegida posterior a la automatica deja de avisar los dias viejos`() {
        // Automático desde hace 10 días; el dueño elige desde hace 2 días
        val f = MissedPayments.missedDays(hoy, 10, "", dia(-10), dia(-10), emptySet(), avisarDesde = dia(-2))
        assertEquals(listOf(dia(-2), dia(-1)), f)
    }

    @Test
    fun `tope exacto de 183 dias`() {
        val justo = MissedPayments.missedDays(hoy, 17, "", "", null, emptySet(), avisarDesde = dia(-(MissedPayments.MAX_DIAS_ELEGIDO - 1)))
        assertEquals(MissedPayments.MAX_DIAS_ELEGIDO, justo.size)
        val masAtras = MissedPayments.missedDays(hoy, 17, "", "", null, emptySet(), avisarDesde = dia(-400))
        assertEquals(justo, masAtras)
        assertEquals(dia(-(MissedPayments.MAX_DIAS_ELEGIDO - 1)), masAtras.first())
    }

    @Test
    fun `byPlaca ignora fechas elegidas de motos que ya no deben cuota`() {
        val m = MissedPayments.byPlaca(
            today = hoy, hourOfDay = 10, desde = hoy, agregadas = emptyMap(),
            units = mapOf("A" to ""), registros = emptyList(),
            avisarDesde = mapOf("A" to dia(-2), "ZZZ" to dia(-30))
        )
        assertEquals(setOf("A"), m.keys)
        assertEquals(listOf(dia(-2), dia(-1)), m["A"])
    }

    @Test
    fun `marcar todos No trabajo resuelve todas las faltas`() {
        val faltas = MissedPayments.missedDays(hoy, 10, "", "", null, emptySet(), avisarDesde = dia(-90))
        assertEquals(90, faltas.size)
        val despues = MissedPayments.missedDays(hoy, 10, "", "", null, faltas.toSet(), avisarDesde = dia(-90))
        assertTrue(despues.isEmpty())
    }

    /**
     * BUG: la moto estuvo 100 días en taller; al volver, daysToExcuse solo marca "No trabajó" los
     * últimos MAX_DIAS (60) días. Con "avisar desde" hasta 183 días atrás, los días 61..100 del taller
     * vuelven a salir como "sin pagar".
     */
    @Test
    fun `dias de taller largos no salen como faltas con fecha elegida`() {
        val inicioTaller = dia(-100)
        val ayer = dia(-1)
        val excusados = MissedPayments.daysToExcuse(inicioTaller, ayer, emptySet())
        val faltas = MissedPayments.missedDays(hoy, 10, "", "", null, excusados.toSet(), avisarDesde = dia(-150))
        // Solo deben quedar los días ANTES del taller (de hace 150 a hace 101)
        assertEquals(50, faltas.size)
        assertTrue(faltas.all { it < inicioTaller })
    }

    // ---------------- MissedPrefs ----------------

    @Test
    fun `avisarDesde se guarda, se quita, se renombra y se borra`() {
        val prefs = FakeSharedPreferences()
        MissedPrefs.setAvisarDesde(prefs, "A1", "2026-10-01")
        MissedPrefs.setAvisarDesde(prefs, "B2", "2026-09-01")
        assertEquals(mapOf("A1" to "2026-10-01", "B2" to "2026-09-01"), MissedPrefs.avisarDesde(prefs))

        MissedPrefs.setAvisarDesde(prefs, "A1", null)
        assertEquals(mapOf("B2" to "2026-09-01"), MissedPrefs.avisarDesde(prefs))
        // quitar una que no existe no rompe nada
        MissedPrefs.setAvisarDesde(prefs, "NOEXISTE", null)
        assertEquals(mapOf("B2" to "2026-09-01"), MissedPrefs.avisarDesde(prefs))

        MissedPrefs.rename(prefs, "B2", "C3")
        assertEquals(mapOf("C3" to "2026-09-01"), MissedPrefs.avisarDesde(prefs))

        MissedPrefs.remove(prefs, "C3")
        assertTrue(MissedPrefs.avisarDesde(prefs).isEmpty())

        MissedPrefs.setAvisarDesde(prefs, "D4", "2026-11-01")
        MissedPrefs.clear(prefs)
        assertTrue(MissedPrefs.avisarDesde(prefs).isEmpty())
    }

    @Test
    fun `renombrar no toca las fechas de otras motos ni otras listas`() {
        val prefs = FakeSharedPreferences()
        MissedPrefs.setAvisarDesde(prefs, "A1", "2026-10-01")
        MissedPrefs.setAgregada(prefs, "X9", "2026-11-01")
        MissedPrefs.rename(prefs, "X9", "Y8")
        assertEquals(mapOf("A1" to "2026-10-01"), MissedPrefs.avisarDesde(prefs))
        assertEquals(mapOf("Y8" to "2026-11-01"), MissedPrefs.agregadas(prefs))
    }

    @Test
    fun `avisarDesde corrupto se lee como vacio`() {
        val prefs = FakeSharedPreferences()
        prefs.edit().putString("motos_avisar_pagos_desde", "{roto").apply()
        assertTrue(MissedPrefs.avisarDesde(prefs).isEmpty())
        MissedPrefs.setAvisarDesde(prefs, "A1", "2026-10-01")
        assertEquals(mapOf("A1" to "2026-10-01"), MissedPrefs.avisarDesde(prefs))
    }

    // ---------------- Recordatorios de copia ----------------

    private val now = 1_800_000_000_000L
    private val hora = 60 * 60 * 1000L
    private fun build(b: BackupStatus, units: List<AlertUnit> = emptyList(), missed: Map<String, List<String>> = emptyMap()) =
        AlertEngine.build(units, emptySet(), null, 10, now = now, backup = b, missedByPlaca = missed)

    @Test
    fun `copia en Drive justo en el limite de 48 horas`() {
        val mail = now
        assertTrue(build(BackupStatus(true, now - 47 * hora, mail, true)).isEmpty())
        assertEquals(listOf("respaldo_drive"), build(BackupStatus(true, now - 48 * hora, mail, true)).map { it.id })
    }

    @Test
    fun `reloj atrasado (copia en el futuro) no avisa`() {
        assertTrue(build(BackupStatus(true, now + 5 * hora, now + 5 * hora, true)).isEmpty())
    }

    @Test
    fun `sin archivo avisa aunque la ultima copia sea reciente`() {
        val a = build(BackupStatus(false, now - hora, now, true)).single()
        assertEquals("Copia en Drive sin configurar", a.titulo)
        assertEquals(AlertType.RESPALDO, a.tipo)
    }

    @Test
    fun `nunca se envio al correo avisa`() {
        val a = build(BackupStatus(true, now, 0L, true)).single()
        assertEquals("respaldo_correo", a.id)
        assertTrue(a.descripcion.contains("Nunca"))
    }

    @Test
    fun `correo justo en 7 dias avisa y en 6 no`() {
        assertTrue(build(BackupStatus(true, now, now - 6 * 24 * hora, true)).isEmpty())
        assertEquals(listOf("respaldo_correo"), build(BackupStatus(true, now, now - 7 * 24 * hora, true)).map { it.id })
    }

    @Test
    fun `sin motos no hay aviso de copia aunque falte todo`() {
        assertTrue(build(BackupStatus(false, 0L, 0L, false)).isEmpty())
    }

    @Test
    fun `aviso de copia cambia de clave al pasar de sin archivo a atrasada`() {
        val a = build(BackupStatus(false, 0L, now, true)).single()
        val b = build(BackupStatus(true, 0L, now, true)).single()
        assertEquals(a.id, b.id)
        assertNotEquals(a.hideKey, b.hideKey)
    }

    @Test
    fun `copias van despues de las faltas y antes del stock`() {
        val moto = AlertUnit(
            placa = "A1", nombre = "Moto A", estado = "Vigente", cliente = "Juan",
            soatDias = 200, soatFin = "", revisionPendiente = false, finContrato = "",
            deuda = 0.0, cuota = 30.0
        )
        val alerts = AlertEngine.sortByUrgency(
            AlertEngine.build(
                listOf(moto), emptySet(), stockAceite = 0, hourOfDay = 10, now = now,
                missedByPlaca = mapOf("A1" to listOf("2026-11-20")),
                backup = BackupStatus(false, 0L, 0L, true)
            )
        )
        val ids = alerts.map { it.id }
        val faltas = ids.indexOfFirst { it.startsWith(AlertEngine.FALTAS_PREFIX) }
        val drive = ids.indexOf("respaldo_drive")
        val aceite = ids.indexOf("stock_aceite")
        assertTrue("faltas=$faltas drive=$drive en $ids", faltas in 0 until drive)
        assertTrue("drive=$drive aceite=$aceite en $ids", aceite == -1 || drive < aceite)
        assertFalse(ids.contains("respaldo_correo").not())
    }

    // ---------------- Vista previa de copias ----------------

    @Test
    fun `un JSON que no es de MotoFlota queda sin motos y lo rechaza el control de copia vacia`() {
        // buildPreview acepta cualquier objeto JSON: la única defensa es que restore rechaza 0 motos.
        assertTrue(BackupHelper.parseFromJson("""{"hola":"mundo"}""").units.isEmpty())
    }
}
