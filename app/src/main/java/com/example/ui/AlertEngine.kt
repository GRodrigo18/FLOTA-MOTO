package com.example.ui

import com.example.util.FleetDates

enum class AlertType {
    SOAT,
    REVISION,
    CONTRATO,
    STOCK,
    DEUDA,
    /** Copia de seguridad sin guardar (Drive) o sin enviar al correo. */
    RESPALDO
}

/** Estado de las copias de seguridad, para avisarlo en la campana como un recordatorio más. */
data class BackupStatus(
    /** Hay un archivo de copia en Drive elegido en este teléfono. */
    val driveLinked: Boolean,
    /** Última copia guardada con éxito en Drive (ms), 0 = nunca. */
    val lastDriveAt: Long,
    /** Última vez que se envió la copia al correo (ms), 0 = nunca. */
    val lastEmailAt: Long,
    /** Hay datos que respaldar (al menos una moto). */
    val hasData: Boolean
)

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
    val deuda: Double,
    val cuota: Double = 0.0
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
    private const val HORA_MS = 60 * 60 * 1000L
    /** Sin copia en Drive desde hace más de esto → recordatorio (la app copia sola tras cada cambio). */
    const val RESPALDO_DRIVE_HORAS = 48L
    /** Sin enviar la copia al correo desde hace estos días → recordatorio. */
    const val RESPALDO_CORREO_DIAS = 7L

    // Etapas de gravedad que forman parte de la clave para ocultar en Inicio
    const val STAGE_SEPARATOR = "#"
    const val STAGE_VENCIDO = "vencido"
    const val STAGE_PRONTO = "pronto"

    /** Prefijo del recordatorio "días sin pagar" de cada moto. */
    const val FALTAS_PREFIX = "faltas_"

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
        a.id.startsWith(FALTAS_PREFIX) -> 2 // días sin pagar de cada moto
        a.tipo == AlertType.DEUDA && a.unitPlaca != null -> 2
        a.tipo == AlertType.REVISION -> 3
        a.tipo == AlertType.DEUDA -> 4 // cobros_hoy (sin moto)
        a.tipo == AlertType.RESPALDO -> 4
        else -> 5 // STOCK
    }

    /** Una moto debe cuota diaria si está en operación y tiene chofer asignado. */
    fun debeCuotaDiaria(u: AlertUnit): Boolean =
        com.example.util.FleetRules.debeCuota(u.estado) &&
            u.cliente.isNotBlank() && !u.cliente.equals("Sin asignar", ignoreCase = true)

    fun build(
        units: List<AlertUnit>,
        paidTodayPlacas: Set<String>,
        stockAceite: Int?,
        hourOfDay: Int,
        dismissed: Set<String> = emptySet(),
        now: Long = System.currentTimeMillis(),
        stockLlantas: Int? = null,
        minimoLlantas: Int = STOCK_LLANTAS_MINIMO,
        /** Días sin pago de cada moto (ver MissedPayments); la clave es la placa. */
        missedByPlaca: Map<String, List<String>> = emptyMap(),
        /** Copias de seguridad: si falta guardar, también es un recordatorio de la campana. */
        backup: BackupStatus? = null
    ): List<UrgentAlert> = buildList {
        // [dismissed] se mantiene por compatibilidad (acepta id o hideKey); la app ya no lo usa:
        // los recordatorios solo desaparecen cuando se resuelven.
        fun addIfNew(alert: UrgentAlert) {
            if (alert.id !in dismissed && alert.hideKey !in dismissed) add(alert)
        }

        units.forEach { u ->
            // Una moto inactiva no avisa nada: al dejarla inactiva su deuda se anula (queda en el historial).
            if (u.estado == "Inactivo") return@forEach
            run {

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

        // Días sin pagar, POR MOTO y con las fechas (hoy cuenta desde las 17:00).
        // Se quita solo cuando cada día queda pagado o marcado "No trabajó".
        units.forEach { u ->
            if (!debeCuotaDiaria(u)) return@forEach
            val faltas = missedByPlaca[u.placa].orEmpty()
            if (faltas.isEmpty()) return@forEach
            val hoy = FleetDates.today(now)
            val soloHoy = faltas.size == 1 && faltas[0] == hoy
            val monto = if (u.cuota > 0.0) " • S/ ${"%.2f".format(u.cuota * faltas.size)}" else ""
            addIfNew(
                UrgentAlert(
                    id = "$FALTAS_PREFIX${u.placa}",
                    tipo = AlertType.DEUDA,
                    titulo = if (soloHoy) "No pagó hoy • ${u.nombre}"
                    else "No pagó ${faltas.size} día${if (faltas.size == 1) "" else "s"} • ${u.nombre}",
                    descripcion = "Sin pago: ${com.example.util.MissedPayments.summary(faltas)}$monto. " +
                        "Toca para marcar «Pagó» o «No trabajó».",
                    unitPlaca = u.placa,
                    hora = if (soloHoy) "Hoy" else "${faltas.size} día${if (faltas.size == 1) "" else "s"}",
                    // Si se suma un día nuevo sin pago, la clave cambia y vuelve a verse en Inicio
                    hideKey = hideKeyFor("$FALTAS_PREFIX${u.placa}", faltas.last())
                )
            )
        }

        // Copias de seguridad (antes solo salían en el aviso de la mañana; ahora están en la campana)
        if (backup != null && backup.hasData) {
            val horasSinCopia = if (backup.lastDriveAt > 0L) (now - backup.lastDriveAt) / HORA_MS else Long.MAX_VALUE
            if (!backup.driveLinked || horasSinCopia >= RESPALDO_DRIVE_HORAS) {
                addIfNew(
                    UrgentAlert(
                        id = "respaldo_drive",
                        tipo = AlertType.RESPALDO,
                        titulo = if (!backup.driveLinked) "Copia en Drive sin configurar" else "Copia en Drive sin guardar",
                        descripcion = when {
                            !backup.driveLinked -> "Tus datos solo están en este celular. Toca para elegir el archivo de copia en Drive."
                            backup.lastDriveAt == 0L -> "Aún no se guardó ninguna copia. Toca y elige «Copiar ahora»."
                            else -> "La última copia en Drive es de hace ${horasSinCopia / 24} día(s). Toca y elige «Copiar ahora»."
                        },
                        unitPlaca = null,
                        hora = "Respaldo",
                        hideKey = hideKeyFor("respaldo_drive", if (!backup.driveLinked) "sin_archivo" else "atrasada")
                    )
                )
            }
            val diasSinCorreo = if (backup.lastEmailAt > 0L) (now - backup.lastEmailAt) / (24 * HORA_MS) else Long.MAX_VALUE
            if (diasSinCorreo >= RESPALDO_CORREO_DIAS) {
                addIfNew(
                    UrgentAlert(
                        id = "respaldo_correo",
                        tipo = AlertType.RESPALDO,
                        titulo = "Envía la copia a tu correo",
                        descripcion = if (backup.lastEmailAt == 0L) "Nunca se envió una copia al correo. Toca y elige «Enviar a mi correo»."
                        else "Hace $diasSinCorreo días que no se envía. Toca y elige «Enviar a mi correo».",
                        unitPlaca = null,
                        hora = "Respaldo"
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
