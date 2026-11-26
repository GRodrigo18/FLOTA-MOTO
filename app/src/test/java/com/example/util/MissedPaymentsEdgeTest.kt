package com.example.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

/**
 * Casos límite de los días sin pagar (pruebas de QA independientes de v1.6).
 */
class MissedPaymentsEdgeTest {

    private val hoy = "2026-11-14"
    private lateinit var tzOriginal: TimeZone

    @Before
    fun guardarZona() {
        tzOriginal = TimeZone.getDefault()
    }

    @After
    fun restaurarZona() {
        TimeZone.setDefault(tzOriginal)
    }

    // ---------------- Ventana de 60 días ----------------

    @Test
    fun `ventana de 60 dias - antes de las 17 son 59 dias hasta ayer`() {
        val f = MissedPayments.missedDays(hoy, 10, "2020-01-01", "2020-01-01", null, emptySet())
        assertEquals(59, f.size)
        assertEquals("2026-09-16", f.first()) // hoy - 59
        assertEquals("2026-11-13", f.last())
    }

    @Test
    fun `ventana de 60 dias - el dia 60 hacia atras ya no cuenta`() {
        val f = MissedPayments.missedDays(hoy, 18, "2020-01-01", "2020-01-01", null, emptySet())
        assertTrue("2026-09-15" !in f)
        assertTrue("2026-09-16" in f)
    }

    @Test
    fun `hora 23 y hora 0`() {
        assertEquals(listOf(hoy), MissedPayments.missedDays(hoy, 23, hoy, hoy, null, emptySet()))
        assertTrue(MissedPayments.missedDays(hoy, 0, hoy, hoy, null, emptySet()).isEmpty())
    }

    // ---------------- Fechas raras ----------------

    @Test
    fun `fecha de hoy invalida no revienta`() {
        assertTrue(MissedPayments.missedDays("hoy", 18, "", "", null, emptySet()).isEmpty())
        assertTrue(MissedPayments.missedDays("", 18, "", "", null, emptySet()).isEmpty())
    }

    @Test
    fun `inicio de contrato vacio o mal escrito se ignora`() {
        val f = MissedPayments.missedDays(hoy, 10, "", "2026-11-12", null, emptySet())
        assertEquals(listOf("2026-11-12", "2026-11-13"), f)
        val g = MissedPayments.missedDays(hoy, 10, "basura", "2026-11-12", "tampoco", emptySet())
        assertEquals(listOf("2026-11-12", "2026-11-13"), g)
    }

    @Test
    fun `inicio escrito como dd-MM-yyyy tambien cuenta`() {
        val f = MissedPayments.missedDays(hoy, 10, "13/11/2026", "2026-11-01", null, emptySet())
        assertEquals(listOf("2026-11-13"), f)
    }

    @Test
    fun `cruza fin de anio y 29 de febrero`() {
        val a = MissedPayments.missedDays("2027-01-02", 10, "2026-12-30", "2026-12-01", null, emptySet())
        assertEquals(listOf("2026-12-30", "2026-12-31", "2027-01-01"), a)
        val b = MissedPayments.missedDays("2028-03-01", 10, "2028-02-28", "2028-01-01", null, emptySet())
        assertEquals(listOf("2028-02-28", "2028-02-29"), b)
    }

    @Test
    fun `desde en el futuro no inventa faltas`() {
        assertTrue(MissedPayments.missedDays(hoy, 18, "2026-01-01", "2026-12-01", null, emptySet()).isEmpty())
    }

    @Test
    fun `byPlaca sin cobros cuenta desde el inicio del contrato`() {
        // v1.8: sin cobros registrados se cuenta desde el inicio del contrato (máx. 60 días)
        val m = MissedPayments.byPlaca(hoy, 18, "", emptyMap(), mapOf("A" to "2026-11-10"), emptyList())
        assertEquals(mapOf("A" to listOf("2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13", hoy)), m)
    }

    @Test
    fun `byPlaca no mezcla registros de otra moto ni de motos que no deben cuota`() {
        val m = MissedPayments.byPlaca(
            today = hoy, hourOfDay = 10, desde = "2026-11-12",
            agregadas = mapOf("B" to "2026-11-13"),
            // B sin contrato iniciado: se cuenta desde que se registró
            units = mapOf("A" to "2026-01-01", "B" to ""),
            registros = listOf("Z" to "2026-11-12", "Z" to "2026-11-13", "A" to "2026-11-12")
        )
        assertEquals(mapOf("A" to listOf("2026-11-13"), "B" to listOf("2026-11-13")), m)
    }

    // ---------------- Zona horaria con cambio de hora (DST) ----------------

    @Test
    fun `dias seguidos aunque la zona tenga cambio de hora a medianoche`() {
        // Chile adelanta la hora a las 00:00 (la medianoche no existe ese día)
        for (zona in listOf("America/Santiago", "America/Asuncion", "America/Havana", "Europe/Madrid")) {
            TimeZone.setDefault(TimeZone.getTimeZone(zona))
            for (dia in listOf("2026-03-20", "2026-04-20", "2026-09-30", "2026-10-20", "2026-11-14")) {
                val f = MissedPayments.missedDays(dia, 10, "2025-01-01", "2025-01-01", null, emptySet())
                // Sin días repetidos ni saltados
                assertEquals("zona $zona hoy $dia", 59, f.size)
                assertEquals("zona $zona hoy $dia", f.size, f.toSet().size)
                assertEquals("zona $zona hoy $dia", FleetDates.plus(dia, days = -1), f.last())
            }
            assertEquals("zona $zona: 2026-09-06 debe existir", "2026-09-06", FleetDates.normalize("2026-09-06"))
        }
    }

    @Test
    fun `etiqueta del dia de la semana correcta en otra zona horaria`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"))
        assertEquals("dom 06 set", MissedPayments.label("2026-09-06")) // la app abrevia setiembre como "set"
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
        assertEquals("sáb 14 nov", MissedPayments.label("2026-11-14"))
    }

    // ---------------- Resumen ----------------

    @Test
    fun `resumen con exactamente el maximo y uno mas`() {
        val cuatro = listOf("2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13")
        assertEquals("mar 10 nov, mié 11 nov, jue 12 nov, vie 13 nov", MissedPayments.summary(cuatro))
        val cinco = listOf("2026-11-09") + cuatro
        assertTrue(MissedPayments.summary(cinco).endsWith("(y 1 día antes)"))
        assertEquals("", MissedPayments.summary(emptyList()))
    }

    // ---------------- Días para excusar (taller / inactiva) ----------------

    @Test
    fun `excusar un solo dia`() {
        assertEquals(listOf("2026-11-13"), MissedPayments.daysToExcuse("2026-11-13", "2026-11-13", emptySet()))
    }

    @Test
    fun `al volver de una parada de mas de 60 dias no queda ninguna falta visible`() {
        // La moto estuvo en el taller desde el 1 de agosto y vuelve hoy (14 nov) a las 10:00.
        // Las faltas se llevan desde julio. El VM hace exactamente esto: daysToExcuse(parada, ayer).
        val excusados = MissedPayments.daysToExcuse("2026-08-01", "2026-11-13", emptySet())
        val faltas = MissedPayments.missedDays(hoy, 10, "2026-01-01", "2026-07-01", null, excusados.toSet())
        assertEquals("días del taller salen como 'no pagó'", emptyList<String>(), faltas)
    }

    @Test
    fun `excusar una parada larga incluye siempre ayer`() {
        val excusados = MissedPayments.daysToExcuse("2026-06-01", "2026-11-13", emptySet())
        assertTrue("ayer (último día parado) no quedó excusado", "2026-11-13" in excusados)
    }

    // ---------------- Renovación de contrato ----------------

    @Test
    fun `renovar antes de tiempo no borra los dias que no pago`() {
        // El contrato terminaba el 30 nov y se renovó el 14 nov: renewalStart pone inicioContrato = 2026-11-30.
        // El chofer igual no pagó del 10 al 13 de noviembre (dentro del contrato anterior).
        val faltas = MissedPayments.missedDays(hoy, 10, "2026-11-30", "2026-11-10", null, emptySet())
        assertEquals(listOf("2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13"), faltas)
    }

    // ---------------- Inactiva y vuelta a trabajar ----------------

    @Test
    fun `dias sin pagar de antes de quedar inactiva no vuelven al reactivarla`() {
        // No pagó del 1 al 5; el 6 se dejó Inactiva (su deuda se anula); el 14 vuelve a trabajar
        // con el mismo contrato. El VM, al quedar inactiva, anula sus días sin pagar pendientes
        // (los marca "Anulado (inactiva)") y al volver excusa desde el día en que paró hasta ayer.
        val pendientesAlInactivar = MissedPayments.missedDays("2026-11-06", 10, "2026-10-01", "2026-11-01", null, emptySet())
        assertEquals(listOf("2026-11-01", "2026-11-02", "2026-11-03", "2026-11-04", "2026-11-05"), pendientesAlInactivar)
        val excusados = pendientesAlInactivar + MissedPayments.daysToExcuse("2026-11-06", "2026-11-13", pendientesAlInactivar.toSet())
        val faltas = MissedPayments.missedDays(hoy, 10, "2026-10-01", "2026-11-01", null, excusados.toSet())
        assertEquals("al quedar inactiva sus deudas debían desaparecer", emptyList<String>(), faltas)
    }
}
