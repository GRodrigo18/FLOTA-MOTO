package com.example.util

/**
 * Días en que una moto debía pagar su cuota y no hay pago registrado ("faltas").
 * Sin Android: se prueba en app/src/test.
 *
 * Un día cuenta como resuelto si tiene un pago, o si se marcó "No trabajó" (taller, moto parada,
 * descanso): esos días se guardan como un registro de cobro con pagado = false y monto 0.
 */
object MissedPayments {

    /** Como máximo se revisan los últimos 60 días (lo más viejo ya no se avisa). */
    const val MAX_DIAS = 60

    /** Si el dueño elige a mano "avisar desde" una fecha, se permite ir hasta 6 meses atrás. */
    const val MAX_DIAS_ELEGIDO = 183

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
     * @param avisarDesde fecha elegida por el dueño para ESTA moto ("avisar pagos desde"): si está,
     *   manda sobre [inicioContrato], [desde] y [agregada] (hasta [MAX_DIAS_ELEGIDO] días atrás)
     */
    fun missedDays(
        today: String,
        hourOfDay: Int,
        inicioContrato: String,
        desde: String,
        agregada: String?,
        registradas: Set<String>,
        avisarDesde: String? = null
    ): List<String> {
        val hoy = FleetDates.normalize(today) ?: return emptyList()
        val elegido = avisarDesde?.let { FleetDates.normalize(it) }
        val inicio = if (elegido != null) {
            maxOf(elegido, FleetDates.plus(hoy, days = -(MAX_DIAS_ELEGIDO - 1)) ?: return emptyList())
        } else {
            val limite = FleetDates.plus(hoy, days = -(MAX_DIAS - 1)) ?: return emptyList()
            // Un inicio de contrato en el futuro (renovación anticipada) no borra los días ya debidos
            listOfNotNull(
                FleetDates.normalize(inicioContrato)?.takeIf { it <= hoy },
                FleetDates.normalize(desde),
                agregada?.let { FleetDates.normalize(it) },
                limite
            ).max()
        }
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

    /**
     * Días entre [desde] y [hasta] (ambos incluidos) que aún no tienen registro. Si la parada fue más
     * larga que [MAX_DIAS], solo hacen falta los últimos [MAX_DIAS] días (los más viejos ya no se avisan).
     */
    fun daysToExcuse(desde: String, hasta: String, registradas: Set<String>): List<String> {
        val b = FleetDates.normalize(hasta) ?: return emptyList()
        val a0 = FleetDates.normalize(desde) ?: return emptyList()
        val limite = FleetDates.plus(b, days = -(MAX_DIAS - 1)) ?: return emptyList()
        val a = maxOf(a0, limite)
        if (a > b) return emptyList()
        val out = mutableListOf<String>()
        var d = a
        while (d <= b) {
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
        registros: List<Pair<String, String>>,
        avisarDesde: Map<String, String> = emptyMap()
    ): Map<String, List<String>> {
        val porMoto = registros.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
        return units.mapValues { (placa, inicio) ->
            missedDays(today, hourOfDay, inicio, desde.ifBlank { today }, agregadas[placa], porMoto[placa].orEmpty(), avisarDesde[placa])
        }.filterValues { it.isNotEmpty() }
    }
}
