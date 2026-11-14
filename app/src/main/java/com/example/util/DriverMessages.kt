package com.example.util

/**
 * Estado de cuenta del chofer (texto para ver en pantalla o compartir).
 * Sin Android: se prueba en app/src/test.
 */
object DriverMessages {

    private fun soles(v: Double) = "S/ " + "%.2f".format(v)

    private fun tieneChofer(chofer: String): Boolean =
        chofer.isNotBlank() && !chofer.trim().equals("Sin asignar", ignoreCase = true)

    data class Statement(
        val moto: String,
        val placa: String,
        val chofer: String,
        val mesLabel: String,
        val diasPagados: Int,
        val totalPagado: Double,
        val diasSinPago: Int,
        val deudaTotal: Double,
        val garantiaPendiente: Double,
        val finContrato: String
    )

    /** Estado de cuenta del mes (para el dueño o para mandarlo al chofer). */
    fun statement(s: Statement): String = buildString {
        append("📋 Estado de cuenta • ${s.mesLabel}\n")
        append("Moto: ${s.moto} (${s.placa})\n")
        if (tieneChofer(s.chofer)) append("Chofer: ${s.chofer.trim()}\n")
        append("Cuotas pagadas: ${s.diasPagados} día(s) • ${soles(s.totalPagado)}\n")
        if (s.diasSinPago > 0) append("Días sin pago registrado: ${s.diasSinPago}\n")
        append("Deuda pendiente: ${soles(s.deudaTotal)}")
        if (s.garantiaPendiente > 0.0) append(" (garantía: ${soles(s.garantiaPendiente)})")
        append("\n")
        append("Contrato hasta: ${s.finContrato}")
    }

    /**
     * Días del mes (hasta hoy) en que la moto debía pagar y no hay pago registrado.
     * Cuenta desde el inicio del contrato si empezó este mes. Fechas en yyyy-MM-dd.
     */
    fun unpaidDays(mes: String, hoy: String, inicioContrato: String, fechasPagadas: Set<String>): Int {
        if (hoy < "$mes-01") return 0
        val ultimo = if (hoy.startsWith(mes)) hoy.substring(8, 10).toIntOrNull() ?: return 0 else diasDelMes(mes)
        var count = 0
        for (d in 1..ultimo) {
            val f = "%s-%02d".format(mes, d)
            if (inicioContrato.isNotBlank() && f < inicioContrato) continue
            if (f !in fechasPagadas) count++
        }
        return count
    }

    private fun diasDelMes(mes: String): Int {
        val y = mes.substring(0, 4).toIntOrNull() ?: return 30
        val m = mes.substring(5, 7).toIntOrNull() ?: return 30
        return when (m) {
            2 -> if ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
}
