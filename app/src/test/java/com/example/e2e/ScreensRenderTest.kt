package com.example.e2e

import android.graphics.Bitmap
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.MainAppScaffold
import com.example.ui.DialogType
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * "Modo de prueba" de pantallas: dibuja la app real (MainAppScaffold con el tema de la app) en un
 * celular de 360 dp de ancho, con datos realistas, comprueba textos clave y guarda capturas PNG en
 * app/build/qa-screens/.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreensRenderTest : E2EBase() {

    @get:Rule
    val compose = createComposeRule()

    private val outDir = File(System.getProperty("user.dir") ?: ".", "build/qa-screens").apply { mkdirs() }

    private fun populate() {
        loginAdmin("Rosa Quispe")
        addMoto("Moto 1", "ABC123", "Juan Pérez Huamán", garantiaInicial = 100.0, deudaGarantia = 100.0)
        addMoto("Moto 2", "XYZ789", "Pedro", cuota = 40.0)
        addMoto("Bajaj Roja Grande", "M3K456", "Luis Alberto Mamani Condori")
        addMoto("Moto 4", "TAL111", "Carlos")
        payOn("ABC123", d(-6), d(-5), d(-2))
        payOn("XYZ789", d(-3), d(-2), d(-1), today)
        payOn("M3K456", d(-1))
        vm.markUnitInWorkshop(unit("TAL111"))
        vm.addDebtCargo(unit("M3K456"), 120.0, "Espejo y faro rotos")
        vm.buyOil(4, 25.0)
        await("datos de pantalla") { s ->
            s.units.size == 4 && s.units.first { it.placa == "M3K456" }.deudaTotal == 120.0 &&
                s.inventory.isNotEmpty() && s.units.first { it.placa == "TAL111" }.estado == "En Taller"
        }
        vm.dismissSnackbar()
    }

    private fun showApp() {
        compose.setContent {
            val st by vm.uiState.collectAsState()
            MyApplicationTheme(darkTheme = false) {
                MainAppScaffold(state = st, viewModel = vm, snackbarHostState = remember { SnackbarHostState() })
            }
        }
        compose.waitForIdle()
    }

    private val verticalList = hasScrollToNodeAction() and
        androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)

    private fun hasText(t: String): Boolean =
        compose.onAllNodesWithText(t, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun assertText(vararg ts: String) {
        compose.waitForIdle()
        ts.forEach { assertTrue("No se ve el texto «$it» en pantalla", hasText(it)) }
    }

    /**
     * Guarda cada ventana (la pantalla y, si hay, el diálogo encima) como PNG. Se dibuja la vista de
     * cada ventana en un Bitmap (captureToImage se queda esperando un cuadro en Robolectric).
     */
    private fun shot(name: String, sync: Boolean = true) {
        if (sync) compose.waitForIdle() else idle()
        val views = windowRootViews().filter { it.isShown && it.width > 0 && it.height > 0 }
        assertTrue("No hay ventanas para capturar", views.isNotEmpty())
        views.forEachIndexed { i, v ->
            val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            val c = android.graphics.Canvas(bmp)
            c.drawColor(android.graphics.Color.DKGRAY) // fondo detrás de los diálogos
            v.draw(c)
            val f = File(outDir, if (views.size == 1) "$name.png" else "${name}_$i.png")
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun windowRootViews(): List<android.view.View> {
        val cls = Class.forName("android.view.WindowManagerGlobal")
        val wmg = cls.getMethod("getInstance").invoke(null)
        val f = cls.getDeclaredField("mViews").apply { isAccessible = true }
        return (f.get(wmg) as List<android.view.View>).toList()
    }

    private fun tab(i: Int) {
        vm.onTabSelected(i)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000) // animación de cambio de pestaña
        compose.waitForIdle()
    }

    @Test
    fun `Inicio, Pagos, Deudas, Flota, Caja y Taller se dibujan con los datos`() {
        populate()
        showApp()

        tab(0)
        assertText("Hola, Rosa Quispe", "Cobros de hoy", "No pagó")
        shot("01_inicio")

        tab(1)
        vm.setSelectedDate(today)
        compose.waitForIdle()
        assertText("Cobros Diarios S/ 35", "CONTROL DIARIO DE CUOTAS")
        shot("02_pagos_cobro_diario")
        compose.onAllNodes(verticalList).onFirst().performScrollToNode(hasText("Moto 2", substring = true))
        assertText("Moto 2")
        shot("02b_pagos_lista")

        compose.onNodeWithTag("subtab_debt_management").performClick()
        compose.waitForIdle()
        shot("03_pagos_deudas")
        assertText("Deuda Total Flota", "CUOTAS SIN PAGAR")

        tab(2)
        shot("04_flota")
        assertText("ABC123", "M3K456")
        compose.onAllNodes(verticalList).onFirst().performScrollToNode(hasText("XYZ789", substring = true))
        assertText("XYZ789")
        shot("04b_flota_abajo")

        tab(3)
        shot("05_caja")

        tab(4)
        shot("06_taller")
    }

    @Test
    fun `campana se dibuja`() {
        populate()
        showApp()
        vm.openDialog(DialogType.EMERGENT_ALERTS)
        assertText("Recordatorios pendientes", "No pagó", "Deuda/Garantía Pendiente")
        shot("07_campana")
    }

    @Test
    fun `dias sin pagar de una moto se dibuja`() {
        populate()
        showApp()
        // Un OutlinedTextField dentro de un diálogo nunca queda "quieto" para el arnés de Compose en
        // Robolectric (pasa con cualquier AlertDialog + campo de texto, no es de la app): aquí se avanza
        // el reloj a mano y se captura sin esperar inactividad.
        compose.mainClock.autoAdvance = false
        vm.openDialogFromAlert(DialogType.MISSED_PAYMENTS, unit("ABC123"))
        repeat(60) { idle(); compose.mainClock.advanceTimeByFrame(); Thread.sleep(2) }
        assertTrue("Se abrió la ventana del diálogo", windowRootViews().count { it.isShown } >= 2)
        shot("08_dias_sin_pagar", sync = false)
        vm.closeDialog()
        repeat(10) { idle(); compose.mainClock.advanceTimeByFrame() }
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun `respaldos se dibuja`() {
        populate()
        showApp()
        vm.openDialog(DialogType.BACKUP_CLOUD)
        assertText("Respaldos", "Recuperar datos")
        shot("09_respaldos")
    }

    @Test
    fun `ficha de una moto en Flota se dibuja`() {
        populate()
        showApp()
        tab(2)
        vm.selectUnit(unit("M3K456"))
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        assertText("Estado del Contrato")
        shot("10_ficha_moto")
    }

    @Test
    fun `modo oscuro - Inicio y Pagos se dibujan`() {
        populate()
        compose.setContent {
            val st by vm.uiState.collectAsState()
            MyApplicationTheme(darkTheme = true) {
                MainAppScaffold(state = st, viewModel = vm, snackbarHostState = remember { SnackbarHostState() })
            }
        }
        tab(0)
        assertText("Cobros de hoy")
        shot("11_inicio_oscuro")
        tab(1)
        shot("12_pagos_oscuro")
    }
}
