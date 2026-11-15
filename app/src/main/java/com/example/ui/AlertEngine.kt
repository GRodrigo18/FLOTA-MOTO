package com.example.ui

import com.example.util.FleetDates

enum class AlertType {
    SOAT,
    REVISION,
    CONTRATO,
    STOCK,
    DEUDA
}

data class UrgentAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val tipo: AlertType,
    val titulo: String,
    val descripcion: String,
    val unitPlaca: String?,
    val hora: String = "Ahora",
    /**
     * Clave para "Quitar de Inicio": id + etapa de gravedad. Si el problema empeora
     * (p. ej. contrato "por vencer" → "vencido") la clave cambia y vuelve a verse en Inicio.
     */
    val hideKey: String = id
) {
    /** true si es un SOAT o contrato ya vencido (lo más urgente). */
    val isVencido: Boolean get() = hideKey.endsWith(AlertEngine.STAGE_SEPARATOR + AlertEngine.STAGE_VENCIDO)
}

/** Datos mínimos de una moto para evaluar alertas (sin depender de Room). */
data class AlertUnit(
    val placa: String,
    val nombre: String,
    val estado: String,
    val cliente: String,
    val soatDias: Int,
    val soatFin: String,
    val revisionPendiente: Boolean,
    val finContrato: String,
    val deuda: Double
)

/**
 * Reglas de alertas de la flota. Se usa tanto en la pantalla como en el aviso diario
 * en segundo plano, para que ambos digan siempre lo mismo.
 */
object AlertEngine {
    const val SOAT_AVISO_DIAS = 15
    const val CONTRATO_AVISO_DIAS = 3
    const val STOCK_ACEITE_MINIMO = 10
    const val HORA_COBROS = 17
    const val STOCK_LLANTAS_MINIMO = 2

    // Etapas de gravedad que forman parte de la clave para ocultar en Inicio
    const val STAGE_SEPARATOR = "#"
    const val STAGE_VENCIDO = "vencido"
    const val STAGE_PRONTO = "pronto"

    /** Clave de ocultar: el id solo, o "id#etapa" si la alerta tiene etapas (SOAT y contrato). */
    fun hideKeyFor(id: String, stage: String = ""): String =
        if (stage.isBlank()) id else "$id$STAGE_SEPARATOR$stage"

    /**
     * Ordena de más urgente a menos: vencidos primero, luego SOAT, contratos, deudas de cada moto,
     * revisión del mes, cobros del día y al final el stock. Mantiene el orden original en empates.
     */
    fun sortByUrgency(alerts: List<UrgentAlert>): List<UrgentAlert> =
        alerts.sortedWith(compareBy<UrgentAlert>({ if (it.isVencido) 0 else 1 }, { urgencyRank(it) }))

    private fun urgencyRank(a: UrgentAlert): Int = when {
        a.tipo == AlertType.SOAT -> 0
        a.tipo == AlertType.CONTRATO -> 1
        a.tipo == AlertType.DEUDA && a.unitPlaca != null -> 2
        a.tipo == AlertType.REVISION -> 3
        a.tipo == AlertType.DEUDA -> 4 // cobros_hoy (sin moto)
        else -> 5 // STOCK
    }

    /** Una moto debe cuota diaria si está en operación y tiene chofer asignado. */
    fun debeCuotaDiaria(u: AlertUnit): Boolean =
        (u.estado == "Vigente" || u.estado == "Por Vencer") &&
            u.cliente.isNotBlank() && !u.cliente.equals("Sin asignar", ignoreCase = true)

    fun build(
        units: List<AlertUnit>,
        paidTodayPlacas: Set<String>,
        stockAceite: Int?,
        hourOfDay: Int,
        dismissed: Set<String> = emptySet(),
        now: Long = System.currentTimeMillis(),
        stockLlantas: Int? = null,
        minimoLlantas: Int = STOCK_LLANTAS_MINIMO
    ): List<UrgentAlert> = buildList {
        // [dismissed] se mantiene por compatibilidad (acepta id o hideKey); la app ya no lo usa:
        // los recordatorios solo desaparecen cuando se resuelven.
        fun addIfNew(alert: UrgentAlert) {
            if (alert.id !in dismissed && alert.hideKey !in dismissed) add(alert)
        }

        units.forEach { u ->
            // Una moto inactiva ya no tiene SOAT, revisión ni contrato que avisar,
            // pero si el chofer quedó debiendo, la deuda sigue pendiente hasta que se pague.
            val activa = u.estado != "Inactivo"
            if (activa) {

                // Días reales hasta el vencimiento (negativo = ya venció). El último día válido NO es "vencido".
                // soatDias viene recortado a 0 (hoy y vencido valen 0): se mira la fecha para distinguirlos.
                val diasSoat = if (u.soatDias > 0) u.soatDias
                else FleetDates.daysSigned(u.soatFin, now)?.takeIf { it <= 0 } ?: -1
                if (diasSoat <= SOAT_AVISO_DIAS) {
                    val vencido = diasSoat < 0
                    addIfNew(
                        UrgentAlert(
                            id = "soat_${u.placa}",
                            tipo = AlertType.SOAT,
                            titulo = if (vencido) "SOAT VENCIDO • ${u.nombre} (${u.placa})"
                            else "SOAT por vencer • ${u.nombre} (${u.placa})",
                            descripcion = when {
                                vencido -> "Venció el ${u.soatFin}. ¡Renovar de inmediato!"
                                diasSoat == 0 -> "Vence HOY (${u.soatFin}). ¡Renovar hoy!"
                                diasSoat == 1 -> "Vence mañana (${u.soatFin}). ¡Renovar pronto!"
                                else -> "Vence en $diasSoat días (${u.soatFin}). ¡Renovar pronto!"
                            },
                            unitPlaca = u.placa,
                            hora = if (vencido) "Vencido" else "Ahora",
                            hideKey = hideKeyFor("soat_${u.placa}", if (vencido) STAGE_VENCIDO else STAGE_PRONTO)
                        )
                    )
                }

                if (u.revisionPendiente) {
                    addIfNew(
                        UrgentAlert(
                            id = "rev_${u.placa}",
                            tipo = AlertType.REVISION,
                            titulo = "Revisión Técnica del Mes • ${u.nombre}",
                            descripcion = "Revisión técnica o mantenimiento mensual pendiente este mes para ${u.nombre} (${u.placa}).",
                            unitPlaca = u.placa,
                            hora = "Este Mes"
                        )
                    )
                }

                val diasContrato = FleetDates.daysSigned(u.finContrato, now)
                if (diasContrato != null && diasContrato <= CONTRATO_AVISO_DIAS) {
                    addIfNew(
                        UrgentAlert(
                            id = "contrato_${u.placa}",
                            tipo = AlertType.CONTRATO,
                            titulo = if (diasContrato < 0) "Contrato vencido • ${u.nombre}" else "Contrato por vencer • ${u.nombre}",
                            descripcion = when {
                                diasContrato < 0 -> "El contrato venció el ${u.finContrato} (hace ${-diasContrato} días) • ${u.placa}. Renuévalo o finalízalo."
                                diasContrato == 0 -> "El contrato vence hoy (${u.finContrato}) • ${u.placa}."
                                diasContrato == 1 -> "El contrato vence mañana (${u.finContrato}) • ${u.placa}."
                                else -> "El contrato vence en $diasContrato días (${u.finContrato}) • ${u.placa}."
                            },
                            unitPlaca = u.placa,
                            hora = "Pronto",
                            hideKey = hideKeyFor("contrato_${u.placa}", if (diasContrato < 0) STAGE_VENCIDO else STAGE_PRONTO)
                        )
                    )
                }

            } // fin de: solo motos activas

            if (u.deuda > 0.0) {
                addIfNew(
                    UrgentAlert(
                        id = "deuda_${u.placa}",
                        tipo = AlertType.DEUDA,
                        titulo = "Deuda/Garantía Pendiente • ${u.nombre}",
                        descripcion = "${u.nombre} (${u.placa}) adeuda S/ ${"%.2f".format(u.deuda)} a la fecha.",
                        unitPlaca = u.placa,
                        hora = "Pendiente"
                    )
                )
            }
        }

        // Cuotas del día sin cobrar (solo a partir de la tarde, para no avisar cuando aún es temprano)
        if (hourOfDay >= HORA_COBROS) {
            val sinPagar = units.filter { debeCuotaDiaria(it) && it.placa !in paidTodayPlacas }
            if (sinPagar.isNotEmpty()) {
                val nombres = sinPagar.take(5).joinToString(", ") { it.nombre }
                val resto = sinPagar.size - 5
                addIfNew(
                    UrgentAlert(
                        id = "cobros_hoy",
                        tipo = AlertType.DEUDA,
                        titulo = "Cobros pendientes hoy: ${sinPagar.size}",
                        descripcion = "Aún no pagan la cuota: $nombres" + if (resto > 0) " y $resto más." else ".",
                        unitPlaca = null,
                        hora = "Hoy",
                        // Clave por día: si se quitó de Inicio hoy, mañana vuelve a aparecer
                        hideKey = hideKeyFor("cobros_hoy", FleetDates.today(now))
                    )
                )
            }
        }

        if (stockLlantas != null && stockLlantas <= minimoLlantas) {
            addIfNew(
                UrgentAlert(
                    id = "stock_llantas",
                    tipo = AlertType.STOCK,
                    titulo = "Llantas en almacén: $stockLlantas",
                    descripcion = "Quedan $stockLlantas llanta(s) de repuesto. Compra antes de que una moto se quede parada.",
                    unitPlaca = null,
                    hora = "Taller"
                )
            )
        }

        if (stockAceite != null && stockAceite <= STOCK_ACEITE_MINIMO) {
            addIfNew(
                UrgentAlert(
                    id = "stock_aceite",
                    tipo = AlertType.STOCK,
                    titulo = "Stock de Aceite 4T: $stockAceite botellas",
                    descripcion = "Se requieren 2 botellas por mantenimiento. Stock disponible: $stockAceite.",
                    unitPlaca = null,
                    hora = "Taller"
                )
            )
        }
    }
}
