package com.example.util

/**
 * Días en que una moto debía pagar su cuota y no hay pago registrado ("faltas").
 * Sin Android: se prueba en app/src/test.
 *
 * Un día cuenta como resuelto si tiene un pago, o si se marcó "No trabajó" (taller, moto parada,
 * descanso): esos días se guardan como un registro de cobro con pagado = false y monto 0.
 */
object MissedPayments {

    /** Como máximo se revisan los últimos 60 días (lo más viejo ya se maneja como deuda). */
    const val MAX_DIAS = 60

    /** Desde qué hora el día de hoy ya cuenta como falta si no pagó (igual que el aviso de cobros). */
    const val HORA_CIERRE = 17

    /** Etiqueta (campo "hora") de los días marcados "No trabajó". */
    const val LABEL_NO_TRABAJO = "No trabajó"

    /**
     * Fechas (yyyy-MM-dd, de la más antigua a la más reciente) sin pago de una moto.
     *
     * @param today hoy, yyyy-MM-dd
     * @param hourOfDay hora actual: hoy solo cuenta desde las [HORA_CIERRE]
     * @param inicioContrato inicio del contrato de la moto (antes de eso no debía nada)
     * @param desde la app empezó a llevar las faltas desde este día (no se inventan faltas de antes de usarla)
     * @param agregada día en que se registró la moto en la app, si se conoce
     * @param registradas días con pago o con "No trabajó"
     */
    fun missedDays(
        today: String,
        hourOfDay: Int,
        inicioContrato: String,
        desde: String,
        agregada: String?,
        registradas: Set<String>
    ): List<String> {
        val hoy = FleetDates.normalize(today) ?: return emptyList()
        val limite = FleetDates.plus(hoy, days = -(MAX_DIAS - 1)) ?: return emptyList()
        val inicio = listOfNotNull(
            FleetDates.normalize(inicioContrato),
            FleetDates.normalize(desde),
            agregada?.let { FleetDates.normalize(it) },
            limite
        ).max()
        val fin = if (hourOfDay >= HORA_CIERRE) hoy else FleetDates.plus(hoy, days = -1) ?: return emptyList()
        if (inicio > fin) return emptyList()
        val out = mutableListOf<String>()
        var d = inicio
        while (d <= fin) {
            if (d !in registradas) out.add(d)
            d = FleetDates.plus(d, days = 1) ?: break
        }
        return out
    }

    /** Días entre [desde] y [hasta] (ambos incluidos, máx. [MAX_DIAS]) que aún no tienen registro. */
    fun daysToExcuse(desde: String, hasta: String, registradas: Set<String>): List<String> {
        val a = FleetDates.normalize(desde) ?: return emptyList()
        val b = FleetDates.normalize(hasta) ?: return emptyList()
        if (a > b) return emptyList()
        val out = mutableListOf<String>()
        var d = a
        while (d <= b && out.size < MAX_DIAS) {
            if (d !in registradas) out.add(d)
            d = FleetDates.plus(d, days = 1) ?: break
        }
        return out
    }

    private val DIAS = listOf("dom", "lun", "mar", "mié", "jue", "vie", "sáb")

    /** "2026-11-12" → "jue 12 nov". */
    fun label(date: String): String {
        val n = FleetDates.normalize(date) ?: return date
        val c = java.util.Calendar.getInstance()
        c.set(n.substring(0, 4).toInt(), n.substring(5, 7).toInt() - 1, n.substring(8, 10).toInt())
        val dia = DIAS[c.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        val p = FleetDates.pretty(n) // "12 nov 2026"
        return "$dia ${p.substring(0, p.lastIndexOf(' '))}"
    }

    /** Lista corta para el aviso, los más recientes: "jue 12 nov, vie 13 nov (y 3 días antes)". */
    fun summary(dates: List<String>, max: Int = 4): String {
        if (dates.isEmpty()) return ""
        val recientes = dates.takeLast(max).map { label(it) }
        val resto = dates.size - recientes.size
        return recientes.joinToString(", ") + if (resto > 0) " (y $resto día${if (resto == 1) "" else "s"} antes)" else ""
    }

    /**
     * Faltas de todas las motos a la vez (lo usan la app y el aviso diario, así dicen lo mismo).
     * @param units placa → inicio de contrato (solo las motos que deben cuota)
     * @param registros pares (placa, fecha) de cobros pagados o "No trabajó"
     */
    fun byPlaca(
        today: String,
        hourOfDay: Int,
        desde: String,
        agregadas: Map<String, String>,
        units: Map<String, String>,
        registros: List<Pair<String, String>>
    ): Map<String, List<String>> {
        val porMoto = registros.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
        return units.mapValues { (placa, inicio) ->
            missedDays(today, hourOfDay, inicio, desde.ifBlank { today }, agregadas[placa], porMoto[placa].orEmpty())
        }.filterValues { it.isNotEmpty() }
    }
}
