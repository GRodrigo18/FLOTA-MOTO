package com.example.util

/**
 * Cuentas de deudas sin Android (se prueban en app/src/test).
 */
object DebtMath {

    data class Saldo(val deudaTotal: Double, val deudaGarantia: Double)

    /** Inicio del motivo de una deuda (no garantía) anulada al dejar la moto inactiva. */
    const val MOTIVO_DEUDA_ANULADA = "Deuda anulada"

    private fun r2(v: Double) = Math.round(v * 100.0) / 100.0

    /** Lo que falta para completar la cuota (0 si pagó completo o de más). */
    fun faltante(cuota: Double, pagado: Double): Double = r2((cuota - pagado).coerceAtLeast(0.0))

    /**
     * Saldo de la moto después de ANULAR un registro de deuda.
     * - CARGO anulado: baja la deuda (y la garantía pendiente si el cargo era de garantía).
     * - ABONO anulado: la deuda vuelve a subir.
     * - ANULADO deshecho: la garantía anulada vuelve a ser deuda.
     */
    fun reverse(tipo: String, monto: Double, motivo: String, deudaTotal: Double, deudaGarantia: Double): Saldo {
        val m = monto.coerceAtLeast(0.0)
        return when (tipo) {
            "CARGO" -> {
                val esGarantia = motivo.startsWith("Saldo garantía", ignoreCase = true)
                Saldo(
                    deudaTotal = r2((deudaTotal - m).coerceAtLeast(0.0)),
                    deudaGarantia = if (esGarantia) r2((deudaGarantia - m).coerceAtLeast(0.0)) else deudaGarantia
                )
            }
            "ABONO" -> Saldo(r2(deudaTotal + m), deudaGarantia)
            // Deshacer: si lo anulado era garantía, vuelve a ser garantía; si era otra deuda, solo la deuda
            "ANULADO" -> if (motivo.startsWith(MOTIVO_DEUDA_ANULADA, ignoreCase = true)) Saldo(r2(deudaTotal + m), deudaGarantia)
            else Saldo(r2(deudaTotal + m), r2(deudaGarantia + m))
            else -> Saldo(deudaTotal, deudaGarantia)
        }
    }
}
