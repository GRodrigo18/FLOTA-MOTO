package com.example.util

/**
 * Reporte del mes en CSV (se abre en Excel o Google Sheets). Sin Android: se prueba en app/src/test.
 */
object MonthlyReport {

    data class UnitRow(
        val nombre: String,
        val placa: String,
        val chofer: String,
        val estado: String,
        val diasPagados: Int,
        val cobrado: Double,
        val gastoTaller: Double,
        val deuda: Double
    )

    data class MovementRow(
        val fecha: String,
        val esIngreso: Boolean,
        val categoria: String,
        val concepto: String,
        val placa: String?,
        val monto: Double
    )

    /** Escapa un campo para CSV (comillas, comas y saltos de línea). */
    fun field(value: String): String {
        val v = value.replace("\r", " ").replace("\n", " ")
        return if (v.contains(',') || v.contains('"') || v.contains(';')) "\"" + v.replace("\"", "\"\"") + "\"" else v
    }

    private fun money(v: Double) = "%.2f".format(java.util.Locale.US, v)

    private fun line(vararg cells: String) = cells.joinToString(",") { field(it) } + "\r\n"

    fun build(mesLabel: String, units: List<UnitRow>, movements: List<MovementRow>): String = buildString {
        append('\uFEFF') // para que Excel muestre bien las tildes
        append(line("MotoFlota Pro - Reporte de $mesLabel"))
        append("\r\n")
        append(line("RESUMEN POR MOTO"))
        append(line("Moto", "Placa", "Chofer", "Estado", "Cuotas pagadas (días)", "Cobrado S/", "Gasto taller S/", "Deuda actual S/"))
        units.forEach { u ->
            append(line(u.nombre, u.placa, u.chofer, u.estado, u.diasPagados.toString(), money(u.cobrado), money(u.gastoTaller), money(u.deuda)))
        }
        append(
            line(
                "TOTAL", "", "", "",
                units.sumOf { it.diasPagados }.toString(),
                money(units.sumOf { it.cobrado }),
                money(units.sumOf { it.gastoTaller }),
                money(units.sumOf { it.deuda })
            )
        )
        append("\r\n")
        append(line("MOVIMIENTOS DE CAJA"))
        append(line("Fecha", "Tipo", "Categoría", "Concepto", "Placa", "Monto S/"))
        movements.sortedBy { it.fecha }.forEach { m ->
            append(line(m.fecha, if (m.esIngreso) "Ingreso" else "Egreso", m.categoria, m.concepto, m.placa ?: "", money(if (m.esIngreso) m.monto else -m.monto)))
        }
        val ingresos = movements.filter { it.esIngreso }.sumOf { it.monto }
        val egresos = movements.filter { !it.esIngreso }.sumOf { it.monto }
        append("\r\n")
        append(line("Total ingresos", money(ingresos)))
        append(line("Total egresos", money(egresos)))
        append(line("Balance del mes", money(ingresos - egresos)))
    }
}
