package com.example.e2e

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.FleetViewModel
import com.example.util.AlertMemory
import com.example.util.BackupHelper
import com.example.util.MissedPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** Punta a punta: Inicio vs campana, copia de seguridad y recuperación, cambio de placa y borrar moto. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AlertsBackupRenameE2ETest : E2EBase() {

    @Test
    fun `quitar de Inicio deja el recordatorio en la campana y al resolverlo se olvida`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        vm.addDebtCargo(unit("ABC123"), 40.0, "Daño")
        var st = await("deuda") { s -> s.urgentAlerts.any { it.id == "deuda_ABC123" } }
        val alerta = st.urgentAlerts.first { it.id == "deuda_ABC123" }
        val total = st.urgentAlerts.size

        vm.hideAlertFromHome(alerta)
        st = await("oculto en Inicio") { alerta.hideKey in it.homeHiddenKeys }
        assertTrue(st.homeAlerts.none { it.id == "deuda_ABC123" })
        assertTrue("Sigue en la campana", st.urgentAlerts.any { it.id == "deuda_ABC123" })
        assertEquals(total, st.urgentAlerts.size)
        assertTrue(alerta.hideKey in AlertMemory.homeHidden(prefs))


        vm.addDebtAbono(unit("ABC123"), 40.0, "Pagó todo")
        st = await("resuelto y olvidado") { s -> s.urgentAlerts.none { it.id == "deuda_ABC123" } && alerta.hideKey !in s.homeHiddenKeys }
        assertFalse(alerta.hideKey in AlertMemory.homeHidden(prefs))
    }

    @Test
    fun `quitar de Inicio el aviso de dias sin pagar - vuelve a Inicio si se suma otro dia`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        payOn("ABC123", d(-5), d(-4), d(-2), d(-1))
        var st = await("falta -3") { s -> d(-3) in s.missedByPlaca["ABC123"].orEmpty() }
        val alerta = st.urgentAlerts.first { it.id == "faltas_ABC123" }
        vm.hideAlertFromHome(alerta)
        st = await("oculto") { s -> s.homeAlerts.none { it.id == "faltas_ABC123" } }
        assertTrue(st.urgentAlerts.any { it.id == "faltas_ABC123" })

        // Se desmarca el pago del -1: hay un día nuevo sin pagar más reciente → vuelve a Inicio
        vm.setSelectedDate(d(-1))
        await("fecha") { it.selectedDate == d(-1) }
        vm.toggleDailyPayment(unit("ABC123"), false)
        st = await("vuelve a Inicio") { s -> s.homeAlerts.any { it.id == "faltas_ABC123" } }
        assertTrue(d(-1) in st.missedByPlaca["ABC123"]!!)
    }

    @Test
    fun `copia de seguridad - recuperar en una app vacia trae todo igual, con avisar desde`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan", garantiaInicial = 100.0, deudaGarantia = 100.0)
        addMoto("Moto 2", "XYZ789", "Pedro", cuota = 40.0)
        payOn("ABC123", d(-6), d(-5), d(-3))
        payOn("XYZ789", d(-2), d(-1))
        vm.markDayNotWorked(unit("ABC123"), d(-4))
        vm.recordPaymentWithRest(unit("XYZ789"), 30.0, true)
        vm.setPaymentAlertStart(unit("XYZ789"), d(-4))
        val antes = await("datos listos") { s ->
            s.units.first { it.placa == "XYZ789" }.deudaTotal == 10.0 &&
                s.dailyPayments.any { it.unitPlaca == "ABC123" && it.fecha == d(-4) } &&
                s.avisarDesde["XYZ789"] == d(-4)
        }

        vm.saveLocalBackup(app)
        val json = run {
            val end = System.currentTimeMillis() + 10_000
            var j: String? = null
            while (j == null && System.currentTimeMillis() < end) { idle(); j = BackupHelper.getLatestLocalBackup(app); Thread.sleep(5) }
            assertNotNull("No se guardó la copia local", j)
            j!!
        }
        val payload = BackupHelper.parseFromJson(json)
        assertEquals(2, payload.units.size)
        assertEquals(mapOf("XYZ789" to d(-4)), payload.avisarDesde)

        // Vista previa desde un archivo (como el que se elige en Drive o en el correo)
        val f = File(app.cacheDir, "copia_prueba.json").apply { writeText(json) }
        var preview: FleetViewModel.BackupPreview? = null
        var called = false
        vm.previewFileBackup(app, Uri.fromFile(f)) { preview = it; called = true }
        val end = System.currentTimeMillis() + 10_000
        while (!called && System.currentTimeMillis() < end) { idle(); Thread.sleep(5) }
        assertNotNull("Vista previa", preview)
        assertEquals(2, preview!!.motos)
        assertEquals(antes.dailyPayments.count { it.pagado }, preview!!.pagos)
        assertEquals(antes.movements.size, preview!!.movimientos)

        // App vacía: se borra todo (base y ajustes de faltas)
        vm.clearAllDatabaseData()
        await("vacía") { it.units.isEmpty() && it.dailyPayments.isEmpty() && it.avisarDesde.isEmpty() }

        vm.restorePreview(app, preview!!, null)
        val st = await("recuperada") { it.units.size == 2 && it.dailyPayments.size == antes.dailyPayments.size }
        assertEquals(antes.debtRecords.size, st.debtRecords.size)
        assertEquals(antes.movements.size, st.movements.size)
        assertEquals(antes.totalIngresos, st.totalIngresos, 0.001)
        assertEquals(antes.totalDeudaPendiente, st.totalDeudaPendiente, 0.001)
        assertEquals(antes.units.map { it.placa to it.deudaGarantia }, st.units.map { it.placa to it.deudaGarantia })
        assertEquals(mapOf("XYZ789" to d(-4)), st.avisarDesde)
        assertEquals("Los días sin pagar son los mismos que antes de la copia", antes.missedByPlaca, st.missedByPlaca)
        assertFalse(st.dailyPayments.single { it.unitPlaca == "ABC123" && it.fecha == d(-4) }.pagado)
    }

    @Test
    fun `restaurar una copia sin motos o un archivo que no es copia no borra nada`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        vm.restoreFromJson("{\"hola\": 1}")
        vm.restoreFromJson("esto no es json")
        val st = settle()
        assertEquals(1, st.units.size)
    }

    @Test
    fun `cambiar la placa conserva pagos, deudas, avisar desde y dias sin pagar`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan", garantiaInicial = 150.0, deudaGarantia = 50.0)
        payOn("ABC123", d(-5), d(-4), d(-2))
        vm.setPaymentAlertStart(unit("ABC123"), d(-7))
        val antes = await("listo") { s -> s.avisarDesde["ABC123"] == d(-7) && s.missedByPlaca["ABC123"] != null }
        val faltas = antes.missedByPlaca["ABC123"]!!
        val pagos = antes.dailyPayments.count { it.unitPlaca == "ABC123" }
        val deudas = antes.debtRecords.count { it.unitPlaca == "ABC123" }
        val movs = antes.movements.count { it.unitPlaca == "ABC123" }

        val u = unit("ABC123")
        vm.updateUnitContractAndSoat(
            unit = u, inicioContrato = u.inicioContrato, finContrato = u.finContrato, tipoDuracion = u.tipoDuracion,
            soatInicio = u.soatInicio, soatFin = u.soatFin, chofer = u.cliente, telefono = u.telefono,
            cuotaDiaria = u.cuotaDiaria, revisionTecnicaRealizada = true,
            nuevoNombre = "Moto Roja", nuevaPlaca = "new 999"
        )
        await("renombrada") { s -> s.units.singleOrNull()?.placa == "NEW999" && s.dailyPayments.all { it.unitPlaca == "NEW999" } }
        settle()
        val s2 = state()
        assertEquals("Moto Roja", s2.units.single().nombreMoto)
        assertEquals(50.0, s2.units.single().deudaTotal, 0.0)
        assertEquals(pagos, s2.dailyPayments.count { it.unitPlaca == "NEW999" && it.nombreMoto == "Moto Roja" })
        assertEquals(deudas, s2.debtRecords.count { it.unitPlaca == "NEW999" })
        assertEquals(movs, s2.movements.count { it.unitPlaca == "NEW999" })
        assertEquals(mapOf("NEW999" to d(-7)), s2.avisarDesde)
        assertEquals(faltas, s2.missedByPlaca["NEW999"])
        assertNull(s2.missedByPlaca["ABC123"])
        assertTrue(s2.urgentAlerts.any { it.id == "faltas_NEW999" })
        assertTrue(s2.urgentAlerts.none { it.unitPlaca == "ABC123" })
        assertTrue(MissedPrefs.agregadas(prefs).containsKey("NEW999"))
        assertFalse(MissedPrefs.agregadas(prefs).containsKey("ABC123"))
    }

    @Test
    fun `no se puede cambiar a una placa que ya usa otra moto`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        addMoto("Moto 2", "XYZ789", "Pedro")
        val u = unit("ABC123")
        vm.updateUnitContractAndSoat(
            unit = u, inicioContrato = u.inicioContrato, finContrato = u.finContrato, tipoDuracion = u.tipoDuracion,
            soatInicio = u.soatInicio, soatFin = u.soatFin, chofer = u.cliente, telefono = u.telefono,
            cuotaDiaria = u.cuotaDiaria, revisionTecnicaRealizada = true, nuevaPlaca = "xyz 789"
        )
        val st = settle()
        assertEquals(setOf("ABC123", "XYZ789"), st.units.map { it.placa }.toSet())
    }

    @Test
    fun `eliminar una moto borra sus pagos, deudas, caja y lo recordado, sin tocar la otra`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan", garantiaInicial = 150.0, deudaGarantia = 50.0)
        addMoto("Moto 2", "XYZ789", "Pedro")
        payOn("ABC123", d(-3), d(-1))
        payOn("XYZ789", d(-1))
        vm.setPaymentAlertStart(unit("ABC123"), d(-5))
        await("avisar") { it.avisarDesde.containsKey("ABC123") }
        val otros = state().dailyPayments.count { it.unitPlaca == "XYZ789" }

        vm.deleteUnit(unit("ABC123"))
        await("borrada") { s -> s.units.map { it.placa } == listOf("XYZ789") }
        settle()
        val s2 = state()
        assertTrue(s2.dailyPayments.none { it.unitPlaca == "ABC123" })
        assertTrue(s2.debtRecords.none { it.unitPlaca == "ABC123" })
        assertTrue(s2.movements.none { it.unitPlaca == "ABC123" })
        assertEquals(otros, s2.dailyPayments.count { it.unitPlaca == "XYZ789" })
        assertTrue(s2.avisarDesde.isEmpty())
        assertFalse(MissedPrefs.agregadas(prefs).containsKey("ABC123"))
        assertTrue(s2.urgentAlerts.none { it.unitPlaca == "ABC123" })
        // Antes de borrar se guardó una copia interna con la moto
        val copia = BackupHelper.getLatestLocalBackup(app)
        assertNotNull(copia)
        assertTrue(BackupHelper.parseFromJson(copia!!).units.any { it.placa == "ABC123" })
    }

    /** Pagar el día más reciente (quedan otros sin pagar) no es "empeorar": no debe volver a Inicio. */
    @Test
    fun `aviso de dias sin pagar oculto en Inicio no vuelve a Inicio al pagar uno de los dias`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        payOn("ABC123", d(-6), d(-5), d(-3))
        var st = await("faltas -4 -2 -1") { s -> s.missedByPlaca["ABC123"].orEmpty().containsAll(listOf(d(-4), d(-2), d(-1))) }
        vm.hideAlertFromHome(st.urgentAlerts.first { it.id == "faltas_ABC123" })
        await("oculto") { s -> s.homeAlerts.none { it.id == "faltas_ABC123" } }

        val ultimo = st.missedByPlaca["ABC123"]!!.last()
        vm.payMissedDay(unit("ABC123"), ultimo)
        st = await("pagado $ultimo") { s -> ultimo !in s.missedByPlaca["ABC123"].orEmpty() }
        st = settle()
        assertTrue(st.urgentAlerts.any { it.id == "faltas_ABC123" })
        assertTrue("Mejoró (pagó un día) y aun así volvió a Inicio", st.homeAlerts.none { it.id == "faltas_ABC123" })
    }

    /** Copia recuperada (p. ej. en un celular nuevo) con una moto en el taller desde hace días. */
    @Test
    fun `recuperar una copia con una moto en el taller conserva desde cuando esta parada`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        payOn("ABC123", d(-8), d(-7), d(-6))
        vm.markUnitInWorkshop(unit("ABC123"))
        await("en taller") { it.units.first { u -> u.placa == "ABC123" }.estado == "En Taller" }
        await("parada anotada") { MissedPrefs.paradas(prefs).containsKey("ABC123") }
        MissedPrefs.setParadas(prefs, mapOf("ABC123" to "${d(-5)}|En taller")) // entró al taller hace 5 días

        vm.saveLocalBackup(app)
        var json: String? = null
        val end = System.currentTimeMillis() + 10_000
        while (json == null && System.currentTimeMillis() < end) { idle(); json = BackupHelper.getLatestLocalBackup(app); Thread.sleep(5) }
        vm.restoreFromJson(json!!)
        await("recuperada") { it.messageSnackbar?.startsWith("Datos recuperados") == true }
        settle()

        vm.markUnitAsOperational(unit("ABC123"))
        await("vigente") { it.units.first { u -> u.placa == "ABC123" }.estado == "Vigente" }
        val st = settle()
        val faltas = st.missedByPlaca["ABC123"].orEmpty().filter { it < today }
        assertTrue("Días en el taller que salen como días sin pagar tras recuperar la copia: $faltas", faltas.isEmpty())
    }
}
