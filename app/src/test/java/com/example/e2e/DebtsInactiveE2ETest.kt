package com.example.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.util.MissedPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Punta a punta: pagos parciales, deudas, moto inactiva (y deshacer) y taller. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DebtsInactiveE2ETest : E2EBase() {

    @Test
    fun `pago parcial crea la deuda, el abono la baja y anular cada registro la deshace`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        val ingresos0 = state().totalIngresos

        vm.recordPaymentWithRest(unit("ABC123"), 20.0, restoADeuda = true)
        var st = await("deuda 15") { it.units.first { u -> u.placa == "ABC123" }.deudaTotal == 15.0 }
        val pagoHoy = st.dailyPayments.single { it.unitPlaca == "ABC123" && it.fecha == today }
        assertTrue(pagoHoy.pagado)
        assertEquals(20.0, pagoHoy.cuotaMonto, 0.0)
        val cargo = st.debtRecords.single { it.unitPlaca == "ABC123" && it.tipo == "CARGO" }
        assertEquals(15.0, cargo.monto, 0.0)
        assertEquals(ingresos0 + 20.0, st.totalIngresos, 0.001)
        assertTrue(st.urgentAlerts.any { it.id == "deuda_ABC123" })
        assertEquals(15.0, st.totalDeudaPendiente, 0.001)

        // Abono mayor que la deuda: se rechaza
        vm.addDebtAbono(unit("ABC123"), 50.0, "Abono")
        assertEquals(15.0, settle(150).units.first { it.placa == "ABC123" }.deudaTotal, 0.0)

        vm.addDebtAbono(unit("ABC123"), 5.0, "Abono parcial")
        st = await("deuda 10") { it.units.first { u -> u.placa == "ABC123" }.deudaTotal == 10.0 }
        assertEquals(ingresos0 + 25.0, st.totalIngresos, 0.001)
        val abono = st.debtRecords.single { it.tipo == "ABONO" }

        vm.deleteDebtRecord(abono)
        st = await("abono anulado") { s -> s.debtRecords.none { it.tipo == "ABONO" } && s.units.first { it.placa == "ABC123" }.deudaTotal == 15.0 }
        assertEquals("El dinero del abono sale de Caja", ingresos0 + 20.0, st.totalIngresos, 0.001)

        vm.deleteDebtRecord(st.debtRecords.single { it.tipo == "CARGO" })
        st = await("cargo anulado") { s -> s.debtRecords.isEmpty() && s.units.first { it.placa == "ABC123" }.deudaTotal == 0.0 }
        assertTrue(st.urgentAlerts.none { it.id == "deuda_ABC123" })
    }

    private fun setEstado(placa: String, estado: String, chofer: String? = null) {
        val u = unit(placa)
        vm.updateUnitContractAndSoat(
            unit = u, inicioContrato = u.inicioContrato, finContrato = u.finContrato, tipoDuracion = u.tipoDuracion,
            soatInicio = u.soatInicio, soatFin = u.soatFin, chofer = chofer ?: u.cliente, telefono = u.telefono,
            cuotaDiaria = u.cuotaDiaria, nuevoEstado = estado, revisionTecnicaRealizada = true
        )
        await("$placa en $estado") { it.units.first { x -> x.placa == placa }.estado == estado }
    }

    @Test
    fun `moto Inactiva anula toda la deuda sin tocar Caja y sin recordatorios y deshacer devuelve la deuda`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan", garantiaInicial = 100.0, deudaGarantia = 100.0)
        vm.addDebtCargo(unit("ABC123"), 50.0, "Espejo roto")
        await("deuda 150") { it.units.first { u -> u.placa == "ABC123" }.deudaTotal == 150.0 }
        payOn("ABC123", d(-6), d(-5), d(-2))
        var st = await("faltas -4 y -3") { s -> s.missedByPlaca["ABC123"].orEmpty().containsAll(listOf(d(-4), d(-3))) }
        assertTrue(st.urgentAlerts.any { it.id == "deuda_ABC123" })
        val ingresosAntes = st.totalIngresos

        setEstado("ABC123", "Inactivo")
        st = await("inactiva sin deuda") { s ->
            val x = s.units.first { it.placa == "ABC123" }
            x.deudaTotal == 0.0 && x.deudaGarantia == 0.0
        }
        val anulados = st.debtRecords.filter { it.tipo == "ANULADO" }
        assertEquals(2, anulados.size)
        assertEquals(150.0, anulados.sumOf { it.monto }, 0.001)
        assertEquals("Anular no toca Caja", ingresosAntes, st.totalIngresos, 0.001)
        assertEquals(0.0, st.totalDeudaPendiente, 0.001)
        assertNull(st.missedByPlaca["ABC123"])
        assertNoAlertsFor(st, "ABC123")

        // Deshacer las anulaciones: la deuda vuelve (garantía incluida)
        anulados.forEach { vm.deleteDebtRecord(it) }
        await("deuda devuelta") { s ->
            val x = s.units.first { it.placa == "ABC123" }
            x.deudaTotal == 150.0 && x.deudaGarantia == 100.0 && s.debtRecords.none { it.tipo == "ANULADO" }
        }
    }

    /** README 1.6: "Al quedar inactiva también se anulan sus días sin pagar (no vuelven a aparecer si se reactiva)". */
    @Test
    fun `dias sin pagar de antes de quedar inactiva no vuelven al reactivarla`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        payOn("ABC123", d(-6), d(-5), d(-2))
        await("faltas -4 y -3") { s -> s.missedByPlaca["ABC123"].orEmpty().containsAll(listOf(d(-4), d(-3))) }

        setEstado("ABC123", "Inactivo")
        settle()
        setEstado("ABC123", "Vigente")
        val st = settle()
        val vuelven = st.missedByPlaca["ABC123"].orEmpty().filter { it < today }
        assertTrue("Días de antes de quedar inactiva que volvieron al reactivar: $vuelven", vuelven.isEmpty())
    }

    @Test
    fun `finalizar contrato anula los dias sin pagar y no vuelven al asignar otro chofer`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        payOn("ABC123", d(-6), d(-5), d(-2))
        await("faltas") { s -> s.missedByPlaca["ABC123"].orEmpty().containsAll(listOf(d(-4), d(-3))) }
        vm.breakContract(unit("ABC123"))
        await("inactiva") { it.units.first { u -> u.placa == "ABC123" }.estado == "Inactivo" }
        settle()
        setEstado("ABC123", "Vigente", chofer = "Carlos")
        val st = settle()
        val vuelven = st.missedByPlaca["ABC123"].orEmpty().filter { it < today }
        assertTrue("Días del chofer anterior que se le avisan al nuevo: $vuelven", vuelven.isEmpty())
    }

    @Test
    fun `finalizar contrato deja la moto inactiva sin chofer, sin deuda y sin recordatorios`() {
        loginAdmin()
        addMoto("Moto 2", "XYZ789", "Pedro", garantiaInicial = 150.0, deudaGarantia = 50.0)
        payOn("XYZ789", d(-3))
        await("falta -2") { s -> d(-2) in s.missedByPlaca["XYZ789"].orEmpty() }

        vm.breakContract(unit("XYZ789"))
        val st = await("contrato finalizado") { s ->
            val x = s.units.first { it.placa == "XYZ789" }
            x.estado == "Inactivo" && x.deudaTotal == 0.0 && x.cliente == "Sin asignar"
        }
        assertEquals(50.0, st.debtRecords.filter { it.tipo == "ANULADO" }.sumOf { it.monto }, 0.001)
        assertNoAlertsFor(st, "XYZ789")
        assertEquals(0, st.urgentAlerts.count { it.id.startsWith("faltas_") })
    }

    @Test
    fun `En Taller y luego a trabajar - los dias parada quedan No trabajo y no se cobran`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        payOn("ABC123", d(-7), d(-6))

        vm.markUnitInWorkshop(unit("ABC123"))
        var st = await("en taller") { it.units.first { u -> u.placa == "ABC123" }.estado == "En Taller" }
        // Mientras está en el taller no avisa días sin pagar
        assertNull(st.missedByPlaca["ABC123"])
        assertTrue(st.urgentAlerts.none { it.id == "faltas_ABC123" })
        // Se espera a que la app anote desde cuándo está parada y se simula que entró hace 5 días
        await("parada anotada") { MissedPrefs.paradas(prefs).containsKey("ABC123") }
        assertEquals("$today|En taller", MissedPrefs.paradas(prefs)["ABC123"])
        MissedPrefs.setParadas(prefs, mapOf("ABC123" to "${d(-5)}|En taller"))
        val ingresos = state().totalIngresos

        vm.markUnitAsOperational(unit("ABC123"))
        st = await("vuelve y se excusan -5..-1") { s ->
            s.units.first { it.placa == "ABC123" }.estado == "Vigente" &&
                range(d(-5), d(-1)).all { f -> s.dailyPayments.any { it.unitPlaca == "ABC123" && it.fecha == f && !it.pagado } }
        }
        assertEquals(ingresos, st.totalIngresos, 0.001)
        assertTrue(st.dailyPayments.filter { it.unitPlaca == "ABC123" && !it.pagado }.all { it.hora == "En taller" })
        assertEquals(if (todayCounts) listOf(today) else emptyList<String>(), st.missedByPlaca["ABC123"].orEmpty())
        assertTrue(MissedPrefs.paradas(prefs).isEmpty())
    }

    @Test
    fun `a una moto inactiva no se le cargan deudas ni pasa a deuda el resto de un pago`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        vm.breakContract(unit("ABC123"))
        await("inactiva") { it.units.first { u -> u.placa == "ABC123" }.estado == "Inactivo" }
        vm.addDebtCargo(unit("ABC123"), 30.0, "Daño")
        vm.recordPaymentWithRest(unit("ABC123"), 10.0, restoADeuda = true)
        val st = settle()
        assertEquals(0.0, st.units.first { it.placa == "ABC123" }.deudaTotal, 0.0)
        assertTrue(st.debtRecords.none { it.tipo == "CARGO" })
    }
}
