package com.example.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.AlertEngine
import com.example.util.MissedPayments
import com.example.util.PinAuth
import com.example.util.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Punta a punta: usuarios, cobro diario y días sin pagar con el ViewModel y Room reales. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PaymentsE2ETest : E2EBase() {

    @Test
    fun `crear administrador, cerrar sesion y volver a entrar con el PIN`() {
        assertFalse(state().isLoggedIn)
        loginAdmin("Dueño", "4321")
        val st = state()
        assertTrue(st.isAdmin)
        assertEquals("Dueño", st.userName)
        assertEquals("Administrador", st.userRoleLabel)

        vm.logout()
        await("sesión cerrada") { !it.isLoggedIn }
        val users = vm.loginUsers()
        assertEquals(1, users.size)
        val id = users.single().id
        assertEquals(PinAuth.Result.WRONG, vm.verifyUserPin(id, "0000"))
        await("sigue fuera") { !it.isLoggedIn }
        assertPinOk(id, "4321")
        await("dentro otra vez") { it.isLoggedIn && it.currentUserId == id }
    }

    @Test
    fun `un cobrador no ve Caja ni puede cambiar avisos ni anular deudas`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        assertNull(vm.addUser("Rosa", "1111", UserRole.COBRADOR))
        val rosa = vm.loginUsers().first { it.name == "Rosa" }
        vm.logout()
        assertPinOk(rosa.id, "1111")
        val st = await("cobrador dentro") { it.isLoggedIn && !it.isAdmin }
        vm.onTabSelected(3)
        assertEquals(0, settle(100).activeTab)

        vm.setPaymentAlertStart(st.units.first(), d(-20))
        assertTrue(settle(100).avisarDesde.isEmpty())
    }

    @Test
    fun `dias sin pagar por moto con las fechas exactas, se resuelven al pagar o marcar No trabajo`() {
        loginAdmin()
        // v1.9: los días sin pagar se cuentan desde el inicio del contrato (aquí, el día del primer cobro)
        addMoto("Moto 1", "ABC123", "Juan", inicioHaceDias = 10)
        addMoto("Moto 2", "XYZ789", "Pedro", cuota = 40.0, inicioHaceDias = 3)

        // Moto 1: pagos con huecos el -8, -7 y -4
        payOn("ABC123", d(-10), d(-9), d(-6), d(-5), d(-3), d(-2), d(-1))
        // Moto 2: empieza a cobrarse el -3 (primer cobro registrado)
        payOn("XYZ789", d(-3), d(-2), d(-1))

        val esperadoA = listOf(d(-8), d(-7), d(-4)) + if (todayCounts) listOf(today) else emptyList()
        val esperadoB = if (todayCounts) listOf(today) else emptyList()
        var st = await("faltas calculadas") { it.missedByPlaca["ABC123"] == esperadoA }
        assertEquals(esperadoB, st.missedByPlaca["XYZ789"].orEmpty())

        val alertaA = st.urgentAlerts.firstOrNull { it.id == "${AlertEngine.FALTAS_PREFIX}ABC123" }
        assertNotNull("Debe existir el recordatorio faltas_ABC123", alertaA)
        assertTrue(alertaA!!.titulo, alertaA.titulo.startsWith("No pagó ${esperadoA.size} días"))
        esperadoA.forEach { assertTrue(alertaA.descripcion, alertaA.descripcion.contains(MissedPayments.label(it))) }
        assertTrue(alertaA.descripcion, alertaA.descripcion.contains("S/ %.2f".format(35.0 * esperadoA.size)))

        // Cobros y caja antes de resolver
        val ingresosAntes = st.totalIngresos
        val movsAntes = st.movements.size

        // Pagó el -8 (desde el aviso) → entra a Caja con esa fecha y desaparece
        vm.payMissedDay(unit("ABC123"), d(-8))
        st = await("-8 pagado") { s -> s.missedByPlaca["ABC123"].orEmpty().none { it == d(-8) } }
        assertEquals(esperadoA - d(-8), st.missedByPlaca["ABC123"])
        assertEquals(ingresosAntes + 35.0, st.totalIngresos, 0.001)
        assertTrue(st.movements.any { it.unitPlaca == "ABC123" && it.fecha == d(-8) && it.esIngreso && it.monto == 35.0 })

        // No trabajó el -7 → desaparece y no cambia la caja
        vm.markDayNotWorked(unit("ABC123"), d(-7))
        st = await("-7 no trabajó") { s -> s.missedByPlaca["ABC123"].orEmpty().none { it == d(-7) } }
        assertEquals(ingresosAntes + 35.0, st.totalIngresos, 0.001)
        assertEquals(movsAntes + 1, st.movements.size)
        val noTrabajo = st.dailyPayments.single { it.unitPlaca == "ABC123" && it.fecha == d(-7) }
        assertFalse(noTrabajo.pagado)
        assertEquals(0.0, noTrabajo.cuotaMonto, 0.0)
        // El cobro diario del -7 no lo cuenta como pagado
        vm.setSelectedDate(d(-7))
        st = await("fecha -7") { it.selectedDate == d(-7) }
        assertEquals(0, st.pagosDelDiaSeleccionado.count { it.unitPlaca == "ABC123" && it.pagado })

        // Resolver el resto → el recordatorio se quita solo
        st.missedByPlaca["ABC123"].orEmpty().forEach { vm.markDayNotWorked(unit("ABC123"), it) }
        st = await("ABC123 al día") { it.missedByPlaca["ABC123"] == null }
        assertTrue(st.urgentAlerts.none { it.id == "${AlertEngine.FALTAS_PREFIX}ABC123" })
    }

    @Test
    fun `desmarcar un pago vuelve a mostrar ese dia sin pagar y lo saca de Caja`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan", inicioHaceDias = 4)
        payOn("ABC123", d(-4), d(-3), d(-2), d(-1))
        await("sin faltas antes de hoy") { s -> s.missedByPlaca["ABC123"].orEmpty().none { it < today } }
        val ingresos = state().totalIngresos

        vm.setSelectedDate(d(-3))
        await("fecha") { it.selectedDate == d(-3) }
        vm.toggleDailyPayment(unit("ABC123"), false)
        val st = await("-3 vuelve como falta") { s -> d(-3) in s.missedByPlaca["ABC123"].orEmpty() }
        assertEquals(ingresos - 35.0, st.totalIngresos, 0.001)
    }

    @Test
    fun `Marcar todas cobra solo a las motos que trabajan y no a las marcadas No trabajo`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        addMoto("Moto 2", "XYZ789", "Pedro", cuota = 40.0)
        addMoto("Moto 3", "TAL111", "Luis")
        vm.markUnitInWorkshop(unit("TAL111"))
        await("Moto 3 en taller") { it.units.first { u -> u.placa == "TAL111" }.estado == "En Taller" }

        val fecha = d(-2)
        vm.markDayNotWorked(unit("XYZ789"), fecha)
        await("XYZ no trabajó") { s -> s.dailyPayments.any { it.unitPlaca == "XYZ789" && it.fecha == fecha } }

        vm.setSelectedDate(fecha)
        await("fecha") { it.selectedDate == fecha }
        val ingresos = settle().totalIngresos
        vm.markAllDailyPayments(true)
        var st = await("marcar todas") { s ->
            s.pagosDelDiaSeleccionado.any { it.unitPlaca == "ABC123" && it.pagado } && kotlin.math.abs(s.totalIngresos - (ingresos + 35.0)) < 0.001
        }
        st = settle()
        assertEquals("Solo Moto 1 debía pagar ese día", 1, st.conteoPagadosHoy)
        assertEquals(35.0, st.totalCobradoHoy, 0.001)
        assertEquals(ingresos + 35.0, st.totalIngresos, 0.001)
        assertFalse(st.pagosDelDiaSeleccionado.single { it.unitPlaca == "XYZ789" }.pagado)
        assertTrue(st.pagosDelDiaSeleccionado.none { it.unitPlaca == "TAL111" })

        // Desmarcar todas: quita los pagos pero conserva el "No trabajó"
        vm.markAllDailyPayments(false)
        // Cobros y Caja llegan por flujos distintos: se esperan los dos
        st = await("desmarcar todas") { s -> s.pagosDelDiaSeleccionado.none { it.pagado } && kotlin.math.abs(s.totalIngresos - ingresos) < 0.001 }
        assertEquals(ingresos, st.totalIngresos, 0.001)
        assertTrue(st.pagosDelDiaSeleccionado.any { it.unitPlaca == "XYZ789" && !it.pagado })
    }

    @Test
    fun `Avisar pagos desde una fecha anterior al primer cobro agrega esos dias, y automatico los quita`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan", inicioHaceDias = 5)
        payOn("ABC123", d(-5), d(-4), d(-3), d(-2), d(-1))
        await("sin faltas antes de hoy") { s -> s.missedByPlaca["ABC123"].orEmpty().none { it < today } }

        vm.setPaymentAlertStart(unit("ABC123"), d(-8))
        val extra = listOf(d(-8), d(-7), d(-6)) + if (todayCounts) listOf(today) else emptyList()
        var st = await("avisar desde -8") { it.missedByPlaca["ABC123"] == extra }
        assertEquals(d(-8), st.avisarDesde["ABC123"])

        // Marcar todos «No trabajó» (solo admin) limpia de una vez
        vm.markAllMissedNotWorked(unit("ABC123"))
        st = await("todos no trabajó") { it.missedByPlaca["ABC123"] == null }
        assertEquals(35.0 * 5, st.totalIngresos - 200.0, 0.001) // garantía 200 + 5 cuotas

        vm.setPaymentAlertStart(unit("ABC123"), null)
        st = await("automático") { it.avisarDesde.isEmpty() }
        assertNull(st.missedByPlaca["ABC123"]?.firstOrNull { it < today })
    }

    /**
     * Moto que todavía no tiene cobros: se avisan los días desde el inicio del contrato (README 1.8).
     * Si después se registra SOLO el pago de hoy, los días anteriores que nunca pagó no deberían
     * desaparecer: siguen sin pagarse.
     */
    @Test
    fun `registrar el pago de hoy no borra los dias anteriores que la moto nunca pago`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan", inicioHaceDias = 5)
        val antes = await("faltas desde el contrato") { it.missedByPlaca["ABC123"].orEmpty().contains(d(-5)) }
        val viejos = antes.missedByPlaca["ABC123"]!!.filter { it < today }
        assertEquals(range(d(-5), d(-1)), viejos)

        payOn("ABC123", today)
        val st = settle()
        assertEquals(
            "Pagar hoy no debe borrar los 5 días anteriores sin pago",
            viejos, st.missedByPlaca["ABC123"].orEmpty()
        )
    }

    /**
     * En «Cuotas sin pagar» la fecha más reciente sale arriba. Tocar «Pagó» en ella (moto sin
     * cobros todavía) no debe hacer desaparecer los días anteriores que siguen sin pagarse.
     */
    @Test
    fun `Pago en el dia mas reciente de una moto sin cobros no borra los dias anteriores`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan", inicioHaceDias = 5)
        await("faltas desde el contrato") { it.missedByPlaca["ABC123"].orEmpty().contains(d(-5)) }

        vm.payMissedDay(unit("ABC123"), d(-1))
        await("-1 pagado") { s -> s.dailyPayments.any { it.unitPlaca == "ABC123" && it.fecha == d(-1) && it.pagado } }
        val st = settle()
        assertEquals(range(d(-5), d(-2)), st.missedByPlaca["ABC123"].orEmpty().filter { it < today })
    }

    @Test
    fun `Marcar todas no cobra a una moto sin chofer`() {
        loginAdmin()
        addMoto("Moto 1", "ABC123", "Juan")
        addMoto("Moto 2", "SIN000", "", garantiaInicial = 0.0)
        assertEquals("Sin asignar", unit("SIN000").cliente)
        vm.setSelectedDate(d(-1))
        await("fecha") { it.selectedDate == d(-1) }
        val ingresos = state().totalIngresos
        vm.markAllDailyPayments(true)
        await("marcar todas") { s -> s.pagosDelDiaSeleccionado.any { it.unitPlaca == "ABC123" && it.pagado } }
        val st = settle()
        assertTrue(
            "Una moto sin chofer no paga cuota (el aviso y el Inicio tampoco la cuentan)",
            st.pagosDelDiaSeleccionado.none { it.unitPlaca == "SIN000" && it.pagado }
        )
        assertEquals(ingresos + 35.0, st.totalIngresos, 0.001)
    }
}
