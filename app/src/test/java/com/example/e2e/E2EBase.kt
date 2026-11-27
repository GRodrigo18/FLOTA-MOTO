package com.example.e2e

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DbSafety
import com.example.data.model.FleetUnitEntity
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.util.FleetDates
import com.example.util.MissedPayments
import com.example.util.PinAuth
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.util.Calendar

/**
 * Base de las pruebas de punta a punta: el FleetViewModel REAL con la base de datos Room REAL
 * (archivo en disco de Robolectric) y las preferencias reales. Cada prueba empieza de cero.
 *
 * Hilos: en Robolectric el hilo de la prueba es el hilo principal. viewModelScope usa
 * Dispatchers.Main (Handler del main looper), así que se espera con [await], que va
 * ejecutando el looper principal hasta que el estado cumple la condición.
 */
abstract class E2EBase {
    protected lateinit var app: Application
    protected lateinit var vm: FleetViewModel
    protected val prefs get() = app.getSharedPreferences("moto_flota_prefs", Context.MODE_PRIVATE)

    protected val today: String get() = FleetDates.today()
    protected fun d(offset: Int): String = FleetDates.plus(today, days = offset)!!

    /** Hoy ya cuenta como día sin pagar desde las 17:00. */
    protected val todayCounts: Boolean get() = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= MissedPayments.HORA_CIERRE

    @Before
    fun baseSetUp() {
        app = ApplicationProvider.getApplicationContext()
        resetDatabase(app)
        prefs.edit().clear().commit()
        app.filesDir.listFiles()?.filter { it.name.startsWith("motoflota_backup_") }?.forEach { it.delete() }
        vm = FleetViewModel(app)
        await("datos cargados") { it.dataLoaded }
    }

    @After
    fun baseTearDown() {
        if (::vm.isInitialized) vm.viewModelScope.cancel()
        idle()
        closeDatabase()
    }

    protected fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Ejecuta el looper hasta que el estado cumple [pred] (o falla con un mensaje claro). */
    protected fun await(what: String, timeoutMs: Long = 10_000, pred: (FleetUiState) -> Boolean): FleetUiState {
        val end = System.currentTimeMillis() + timeoutMs
        while (true) {
            idle()
            val s = vm.uiState.value
            if (pred(s)) {
                // Cada tabla de la base llega por su propio flujo: se deja que lleguen las demás
                // (Caja, deudas, recordatorios) antes de afirmar, y se confirma que sigue cumpliéndose.
                val quieto = settle(250)
                if (pred(quieto)) return quieto
            }
            if (System.currentTimeMillis() > end) {
                throw AssertionError(
                    "Tiempo agotado esperando: $what\n" +
                        "units=${s.units.map { "${it.placa}:${it.estado}:deuda=${it.deudaTotal}:gar=${it.deudaGarantia}" }}\n" +
                        "pagos=${s.dailyPayments.map { "${it.unitPlaca}@${it.fecha}:${it.pagado}:${it.cuotaMonto}" }}\n" +
                        "deudas=${s.debtRecords.map { "${it.unitPlaca}:${it.tipo}:${it.monto}" }}\n" +
                        "missed=${s.missedByPlaca}\nalerts=${s.urgentAlerts.map { it.id }}"
                )
            }
            Thread.sleep(5)
        }
    }

    /** Deja correr el trabajo pendiente un rato (para afirmar que algo NO cambió). */
    protected fun settle(ms: Long = 400): FleetUiState {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) {
            idle()
            Thread.sleep(5)
        }
        idle()
        return vm.uiState.value
    }

    protected fun state(): FleetUiState = vm.uiState.value

    protected fun unit(placa: String): FleetUnitEntity = state().units.first { it.placa == placa }

    protected fun loginAdmin(name: String = "Dueño", pin: String = "4321") {
        val r = vm.createFirstAdmin(name, "dueno@example.com", pin)
        assertEquals(com.example.util.UserStore.AddResult.OK, r)
        // La guía se abre sola al primer ingreso: se termina para que no tape nada
        vm.completeInteractiveTour()
        await("sesión iniciada") { it.isLoggedIn && it.isAdmin }
    }

    /** Registra una moto con chofer y contrato que empezó hace [inicioHaceDias] días. */
    protected fun addMoto(
        nombre: String,
        placa: String,
        chofer: String,
        cuota: Double = 35.0,
        inicioHaceDias: Int = 30,
        garantiaInicial: Double = 200.0,
        deudaGarantia: Double = 0.0
    ): FleetUnitEntity {
        val before = state().units.size
        vm.addUnit(
            nombreMoto = nombre,
            placa = placa,
            cliente = chofer,
            telefono = "987654321",
            cuotaDiaria = cuota,
            garantiaTotal = 200.0,
            garantiaInicial = garantiaInicial,
            deudaGarantia = deudaGarantia,
            inicioContrato = d(-inicioHaceDias),
            finContrato = d(90),
            tipoDuracion = "3 Meses",
            soatInicio = d(-30),
            soatFin = d(300),
            observaciones = ""
        )
        // El mensaje final sale cuando terminó todo (moto, garantía en Caja y cargo de garantía)
        val st = await("moto $placa registrada") { s ->
            s.units.size == before + 1 && s.units.any { it.placa == placa } &&
                s.messageSnackbar?.contains("($placa) registrada") == true
        }
        vm.dismissSnackbar()
        return st.units.first { it.placa == placa }
    }

    /** Cobra la cuota de [placa] en cada fecha usando la pestaña Pagos (fecha elegida + marcar). */
    protected fun payOn(placa: String, vararg fechas: String) {
        for (f in fechas) {
            vm.setSelectedDate(f)
            await("fecha elegida $f") { it.selectedDate == f }
            vm.toggleDailyPayment(unit(placa), true)
            await("pago $placa $f") { s -> s.dailyPayments.any { it.unitPlaca == placa && it.fecha == f && it.pagado } }
        }
    }

    /** Fechas de [from] a [to] (incluidas). */
    protected fun range(from: String, to: String): List<String> {
        val out = mutableListOf<String>()
        var x = from
        while (x <= to) {
            out += x
            x = FleetDates.plus(x, days = 1)!!
        }
        return out
    }

    /** Último día que ya cuenta como "sin pagar" (hoy desde las 17:00, si no ayer). */
    protected val lastCountedDay: String get() = if (todayCounts) today else d(-1)

    protected fun assertPinOk(userId: String, pin: String) {
        assertEquals(PinAuth.Result.OK, vm.verifyUserPin(userId, pin))
    }

    protected fun assertNoAlertsFor(st: FleetUiState, placa: String) {
        val forUnit = st.urgentAlerts.filter { it.unitPlaca == placa }
        assertTrue("No debe haber recordatorios de $placa, hay: ${forUnit.map { it.id + " / " + it.titulo }}", forUnit.isEmpty())
    }

    companion object {
        /** Cierra y suelta el singleton de Room y borra el archivo: cada prueba con base nueva. */
        fun resetDatabase(ctx: Context) {
            closeDatabase()
            listOf("", "-wal", "-shm", "-journal").forEach { File(ctx.getDatabasePath(DbSafety.DB_NAME).path + it).delete() }
        }

        fun closeDatabase() {
            val f = AppDatabase::class.java.getDeclaredField("INSTANCE")
            f.isAccessible = true
            (f.get(null) as? AppDatabase)?.let { runCatching { it.close() } }
            f.set(null, null)
        }
    }
}
