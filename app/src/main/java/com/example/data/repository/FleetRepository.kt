package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.FleetDao
import com.example.data.model.DailyPaymentEntity
import com.example.data.model.DebtRecordEntity
import com.example.data.model.DetailedMaintenanceEntity
import com.example.data.model.FinanceMovementEntity
import com.example.data.model.FleetUnitEntity
import com.example.data.model.InventoryBatchEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.MaintenanceTaskEntity
import com.example.data.model.UnitExpenseEntity
import com.example.util.FleetBackupPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.example.util.FleetDates
import com.example.util.FleetRules
import com.example.util.MaintenanceMath
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FleetRepository(
    private val dao: FleetDao,
    /** Si se pasa, la restauración se hace en una transacción: o se aplica completa o no se toca nada. */
    private val db: AppDatabase? = null
) {

    // Los días de SOAT, el avance del contrato y el estado "Por Vencer" se recalculan al leer,
    // porque antes se guardaban una sola vez al crear la moto y quedaban desactualizados.
    // Además, todas las listas salen ordenadas por nombre como lo haría una persona (Moto 2 antes que Moto 10).
    val allUnits: Flow<List<FleetUnitEntity>> = dao.getAllUnits().map { list ->
        FleetRules.sortUnits(list.map { refreshDerivedFields(it) }, FleetRules.Sort.NOMBRE) {
            FleetRules.SortKey(it.nombreMoto, it.placa, it.estado, it.deudaTotal)
        }
    }
    val allMovements: Flow<List<FinanceMovementEntity>> = dao.getAllMovements()
    val allInventory: Flow<List<InventoryItemEntity>> = dao.getAllInventory()
    val allInventoryBatches: Flow<List<InventoryBatchEntity>> = dao.getAllInventoryBatches()
    val allMaintenanceTasks: Flow<List<MaintenanceTaskEntity>> = dao.getAllMaintenanceTasks()
    val allDailyPayments: Flow<List<DailyPaymentEntity>> = dao.getAllDailyPayments()
    val allDebtRecords: Flow<List<DebtRecordEntity>> = dao.getAllDebtRecords()
    val allDetailedMaintenances: Flow<List<DetailedMaintenanceEntity>> = dao.getAllDetailedMaintenances()

    private fun refreshDerivedFields(unit: FleetUnitEntity): FleetUnitEntity {
        val now = System.currentTimeMillis()
        var result = unit

        FleetDates.daysUntil(unit.soatFin, now)?.let { dias ->
            val estado = when {
                unit.estado == "Vigente" && dias <= 7 -> "Por Vencer"
                unit.estado == "Por Vencer" && dias > 7 -> "Vigente"
                else -> unit.estado
            }
            result = result.copy(
                soatDiasRestantes = dias,
                soatVence = FleetDates.expiryLabel(dias),
                estado = estado
            )
        }

        FleetDates.contractProgress(unit.inicioContrato, unit.finContrato, now)?.let { progreso ->
            result = result.copy(progresoContrato = progreso)
        }

        return result
    }

    fun getDailyPaymentsForDate(fecha: String): Flow<List<DailyPaymentEntity>> =
        dao.getDailyPaymentsForDate(fecha)

    fun getMaintenancesForMonth(mesAno: String): Flow<List<DetailedMaintenanceEntity>> =
        dao.getMaintenancesForMonth(mesAno)

    fun getExpensesForUnit(placa: String): Flow<List<UnitExpenseEntity>> =
        dao.getExpensesForUnit(placa)

    suspend fun getFullBackupPayload(userEmail: String): FleetBackupPayload = withContext(Dispatchers.IO) {
        val units = allUnits.first()
        val dailyPayments = allDailyPayments.first()
        val debtRecords = allDebtRecords.first()
        val detailedMaintenances = allDetailedMaintenances.first()
        val expenses = dao.getAllUnitExpenses().first()
        val movements = allMovements.first()
        val inventory = allInventory.first()
        val tasks = allMaintenanceTasks.first()
        val batches = allInventoryBatches.first()

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        FleetBackupPayload(
            exportDate = dateStr,
            userEmail = userEmail,
            units = units,
            dailyPayments = dailyPayments,
            debtRecords = debtRecords,
            detailedMaintenances = detailedMaintenances,
            unitExpenses = expenses,
            financeMovements = movements,
            inventoryItems = inventory,
            maintenanceTasks = tasks,
            inventoryBatches = batches
        )
    }

    suspend fun restoreFromBackup(payload: FleetBackupPayload) = withContext(Dispatchers.IO) {
        // Todo-o-nada: antes, si algo fallaba a mitad de camino, se borraban los datos y no se
        // cargaban los de la copia. Ahora, si falla, la base queda exactamente como estaba.
        if (db != null) db.withTransaction { replaceAllData(payload) } else replaceAllData(payload)
    }

    private suspend fun replaceAllData(payload: FleetBackupPayload) {
        dao.deleteAllUnits()
        dao.deleteAllDailyPayments()
        dao.deleteAllDebtRecords()
        dao.deleteAllDetailedMaintenances()
        dao.deleteAllUnitExpenses()
        dao.deleteAllMovements()
        dao.deleteAllInventory()
        dao.deleteAllMaintenanceTasks()
        dao.deleteAllBatches()

        if (payload.units.isNotEmpty()) dao.insertUnits(payload.units)
        if (payload.dailyPayments.isNotEmpty()) dao.insertDailyPayments(payload.dailyPayments)
        if (payload.debtRecords.isNotEmpty()) dao.insertDebtRecords(payload.debtRecords)
        if (payload.detailedMaintenances.isNotEmpty()) dao.insertDetailedMaintenances(payload.detailedMaintenances)
        if (payload.unitExpenses.isNotEmpty()) dao.insertUnitExpenses(payload.unitExpenses)
        if (payload.financeMovements.isNotEmpty()) dao.insertMovements(payload.financeMovements)
        if (payload.inventoryItems.isNotEmpty()) dao.insertInventoryItems(payload.inventoryItems)
        if (payload.maintenanceTasks.isNotEmpty()) dao.insertMaintenanceTasks(payload.maintenanceTasks)
        if (payload.inventoryBatches.isNotEmpty()) dao.insertBatches(payload.inventoryBatches)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        dao.deleteAllUnits()
        dao.deleteAllDailyPayments()
        dao.deleteAllDebtRecords()
        dao.deleteAllDetailedMaintenances()
        dao.deleteAllUnitExpenses()
        dao.deleteAllMovements()
        dao.deleteAllInventory()
        dao.deleteAllMaintenanceTasks()
        dao.deleteAllBatches()
    }

    suspend fun insertUnit(unit: FleetUnitEntity): Long = withContext(Dispatchers.IO) {
        dao.insertUnit(unit)
    }

    /**
     * Guarda los datos de la moto. Las deudas NUNCA se toman de [unit]: se conservan las de la
     * base, porque solo cambian con cargos, abonos o anulaciones. Antes, al pasar una moto al
     * taller con la ficha abierta desde hace rato, se podía borrar una deuda registrada después.
     */
    suspend fun updateUnit(unit: FleetUnitEntity) = withContext(Dispatchers.IO) {
        atomically {
            val enBase = dao.getUnitByPlaca(unit.placa)
            dao.updateUnit(
                if (enBase == null) unit
                else unit.copy(deudaTotal = enBase.deudaTotal, deudaGarantia = enBase.deudaGarantia)
            )
        }
    }

    /** Solo cambia el estado (Vigente, En Taller, Inactivo...), sin tocar nada más. */
    suspend fun updateUnitEstado(placa: String, estado: String) = withContext(Dispatchers.IO) {
        dao.updateUnitStatus(placa, estado)
    }

    suspend fun deleteUnit(unit: FleetUnitEntity) = withContext(Dispatchers.IO) {
        dao.deleteUnit(unit)
    }

    // Cada operación de varias tablas va en una transacción: o se hace completa o no se toca nada.
    private suspend fun atomically(block: suspend () -> Unit) {
        if (db != null) db.withTransaction { block() } else block()
    }

    private suspend fun <T> atomicResult(block: suspend () -> T): T =
        if (db != null) db.withTransaction { block() } else block()

    /** Cambia nombre y placa de una moto y los arrastra a pagos, deudas, taller, gastos y caja. */
    suspend fun renameUnit(placaActual: String, nuevoNombre: String, nuevaPlaca: String) = withContext(Dispatchers.IO) {
        atomically {
            dao.renameUnitRow(placaActual, nuevoNombre, nuevaPlaca)
            dao.renameInPayments(placaActual, nuevoNombre, nuevaPlaca)
            dao.renameInDebts(placaActual, nuevoNombre, nuevaPlaca)
            dao.renameInMaintenances(placaActual, nuevoNombre, nuevaPlaca)
            dao.renameInExpenses(placaActual, nuevaPlaca)
            dao.renameInMovements(placaActual, nuevaPlaca)
            dao.renameInTasks(placaActual, nuevaPlaca)
        }
    }

    data class UnitHistory(val pagos: Int, val trabajosTaller: Int, val movimientosCaja: Int) {
        val isEmpty: Boolean get() = pagos == 0 && trabajosTaller == 0
    }

    /** La moto tal como está guardada ahora mismo (con fechas y estado recalculados). */
    suspend fun getUnitByPlaca(placa: String): FleetUnitEntity? = withContext(Dispatchers.IO) {
        dao.getUnitByPlaca(placa)
    }

    suspend fun unitHistory(placa: String): UnitHistory = withContext(Dispatchers.IO) {
        UnitHistory(dao.countPayments(placa), dao.countMaintenances(placa), dao.countMovements(placa))
    }

    /** Borra la moto y TODOS sus registros (incluida la garantía que entró a Caja al registrarla). */
    suspend fun deleteUnitCompletely(placa: String) = withContext(Dispatchers.IO) {
        atomically {
            dao.deletePaymentsOf(placa)
            dao.deleteDebtsOf(placa)
            dao.deleteMaintenancesOf(placa)
            dao.deleteExpensesOf(placa)
            dao.deleteMovementsOf(placa)
            dao.deleteTasksOf(placa)
            dao.deleteUnitByPlaca(placa)
        }
    }

    /**
     * Moto inactiva: el saldo de garantía que faltaba pagar deja de ser deuda (no entra dinero a Caja).
     * Lee la moto fresca de la base de datos. Devuelve el monto anulado (0 si no había).
     */
    suspend fun cancelPendingGuarantee(placa: String): Double = withContext(Dispatchers.IO) {
        val unit = dao.getUnitByPlaca(placa) ?: return@withContext 0.0
        val result = FleetRules.cancelGuarantee(unit.deudaTotal, unit.deudaGarantia)
        if (result.anulado <= 0.0) return@withContext 0.0
        atomically {
            dao.insertDebtRecord(
                DebtRecordEntity(
                    unitPlaca = unit.placa,
                    nombreMoto = unit.nombreMoto,
                    tipo = "ANULADO",
                    monto = result.anulado,
                    motivo = "Garantía anulada: moto inactiva (no entra a Caja)",
                    fecha = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                )
            )
            dao.updateUnitDebtAndGarantia(unit.placa, result.nuevaDeudaTotal, result.nuevaDeudaGarantia)
        }
        result.anulado
    }

    /**
     * Moto inactiva: se anula TODA su deuda (garantía pendiente y el resto), sin tocar Caja.
     * Queda en el historial de Deudas como "ANULADO" y se puede deshacer desde ahí.
     * Devuelve el total anulado.
     */
    suspend fun cancelAllDebtForInactive(placa: String): Double = withContext(Dispatchers.IO) {
        atomicResult {
            val unit = dao.getUnitByPlaca(placa) ?: return@atomicResult 0.0
            val total = unit.deudaTotal.coerceAtLeast(0.0)
            if (total <= 0.0) {
                if (unit.deudaGarantia != 0.0) dao.updateUnitDebtAndGarantia(unit.placa, 0.0, 0.0)
                return@atomicResult 0.0
            }
            val fecha = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val garantia = unit.deudaGarantia.coerceIn(0.0, total)
            val resto = total - garantia
            if (garantia > 0.0) {
                dao.insertDebtRecord(
                    DebtRecordEntity(
                        unitPlaca = unit.placa, nombreMoto = unit.nombreMoto, tipo = "ANULADO", monto = garantia,
                        motivo = "Garantía anulada: moto inactiva (no entra a Caja)", fecha = fecha
                    )
                )
            }
            if (resto > 0.004) {
                dao.insertDebtRecord(
                    DebtRecordEntity(
                        unitPlaca = unit.placa, nombreMoto = unit.nombreMoto, tipo = "ANULADO", monto = resto,
                        motivo = "${com.example.util.DebtMath.MOTIVO_DEUDA_ANULADA}: moto inactiva (no entra a Caja)", fecha = fecha
                    )
                )
            }
            dao.updateUnitDebtAndGarantia(unit.placa, 0.0, 0.0)
            total
        }
    }

    /**
     * Marca días como "No trabajó" (taller, moto parada, descanso): no se cobran ni entran a Caja,
     * pero dejan de contar como días sin pagar. No pisa días que ya tienen un pago.
     */
    suspend fun markNotWorked(unit: FleetUnitEntity, fechas: List<String>, etiqueta: String = com.example.util.MissedPayments.LABEL_NO_TRABAJO) =
        withContext(Dispatchers.IO) {
            atomically {
                val ya = dao.getPaymentDatesOf(unit.placa).toSet()
                fechas.filter { it !in ya }.forEach { f ->
                    dao.insertDailyPayment(
                        DailyPaymentEntity(
                            fecha = f, unitPlaca = unit.placa, nombreMoto = unit.nombreMoto,
                            cuotaMonto = 0.0, pagado = false, hora = etiqueta
                        )
                    )
                }
            }
        }

    /** Nuevo mes: la revisión técnica de todas las motos vuelve a quedar pendiente. */
    suspend fun resetMonthlyRevision() = withContext(Dispatchers.IO) {
        dao.resetRevisionTecnicaMesTodas()
    }

    suspend fun updateRevisionTecnicaMes(placa: String, realizada: Boolean) = withContext(Dispatchers.IO) {
        dao.updateRevisionTecnicaMes(placa, realizada)
    }

    suspend fun updateDefaultCuotaDiaria(cuota: Double) = withContext(Dispatchers.IO) {
        dao.updateAllUnitsCuota(cuota)
    }

    suspend fun updateUnitCuota(id: Long, cuota: Double) = withContext(Dispatchers.IO) {
        dao.updateUnitCuota(id, cuota)
    }

    suspend fun toggleDailyPayment(unit: FleetUnitEntity, fecha: String, isPagado: Boolean, customMonto: Double? = null) = withContext(Dispatchers.IO) {
        val montoCobrado = customMonto ?: unit.cuotaDiaria
        if (isPagado) {
            val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            // Primero eliminar registro previo para evitar duplicados en la misma moto y fecha
            dao.deleteDailyPayment(fecha, unit.placa)
            dao.deleteDailyPaymentMovement(unit.placa, fecha)

            dao.insertDailyPayment(
                DailyPaymentEntity(
                    fecha = fecha,
                    unitPlaca = unit.placa,
                    nombreMoto = unit.nombreMoto,
                    cuotaMonto = montoCobrado,
                    pagado = true,
                    hora = nowTime
                )
            )
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val esAdelantado = fecha > todayStr
            val conceptoMov = if (esAdelantado) {
                "Pago adelantado ($fecha) - ${unit.nombreMoto} (${unit.placa})"
            } else {
                "Cuota ($fecha) - ${unit.nombreMoto} (${unit.placa})"
            }
            val categoriaMov = if (esAdelantado) "Pago Adelantado" else "Pago Diario"

            // Se registra el movimiento con la fecha correspondiente al cobro para que pagos adelantados salgan con su fecha real
            dao.insertMovement(
                FinanceMovementEntity(
                    concepto = conceptoMov,
                    monto = montoCobrado,
                    esIngreso = true,
                    categoria = categoriaMov,
                    fecha = fecha,
                    unitPlaca = unit.placa
                )
            )
        } else {
            // Al desmarcar, borrar el pago diario Y borrar el movimiento financiero correspondiente para NO generar historial falso
            dao.deleteDailyPayment(fecha, unit.placa)
            dao.deleteDailyPaymentMovement(unit.placa, fecha)
        }
    }

    suspend fun addDebtCargo(unit: FleetUnitEntity, monto: Double, motivo: String) = withContext(Dispatchers.IO) {
        atomically {
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            dao.insertDebtRecord(
                DebtRecordEntity(
                    unitPlaca = unit.placa,
                    nombreMoto = unit.nombreMoto,
                    tipo = "CARGO",
                    monto = monto,
                    motivo = motivo,
                    fecha = dateStr
                )
            )
            // Saldo leído de la base en el momento (la ficha en pantalla puede estar desactualizada)
            val actual = dao.getUnitByPlaca(unit.placa) ?: unit
            dao.updateUnitDebt(unit.placa, actual.deudaTotal + monto)
        }
    }

    suspend fun addDebtAbono(unit: FleetUnitEntity, monto: Double, motivo: String) = withContext(Dispatchers.IO) {
        atomically {
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            dao.insertDebtRecord(
                DebtRecordEntity(
                    unitPlaca = unit.placa,
                    nombreMoto = unit.nombreMoto,
                    tipo = "ABONO",
                    monto = monto,
                    motivo = motivo,
                    fecha = dateStr
                )
            )
            val actual = dao.getUnitByPlaca(unit.placa) ?: unit
            val nuevaDeuda = (actual.deudaTotal - monto).coerceAtLeast(0.0)
            val nuevaDeudaGarantia = (actual.deudaGarantia - monto).coerceAtLeast(0.0)
            dao.updateUnitDebtAndGarantia(unit.placa, nuevaDeuda, nuevaDeudaGarantia)

            // Registrar ingreso en Finanzas
            val nowFull = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dao.insertMovement(
                FinanceMovementEntity(
                    concepto = "Abono Deuda/Garantía - ${unit.nombreMoto} (${unit.placa})",
                    monto = monto,
                    esIngreso = true,
                    categoria = "Abono Garantía",
                    fecha = nowFull,
                    unitPlaca = unit.placa
                )
            )
        }
    }

    suspend fun registerDetailedMaintenance(
        unit: FleetUnitEntity,
        tipoTrabajo: String,
        detallesDescripcion: String,
        costoTrabajo: Double,
        fecha: String,
        botellasAceite: Int = 0,
        precioBotellaAceite: Double? = null,
        llantasUsadas: Int = 0,
        precioLlantaCustom: Double? = null,
        costoGeneral: Double = 0.0,
        costoFrenos: Double = 0.0,
        costoLlantas: Double = 0.0,
        costoCana: Double = 0.0,
        costoPlanchado: Double = 0.0,
        costoCablesCambio: Double = 0.0,
        costoBajadaMotorCompleto: Double = 0.0,
        costoBajadaMedioMotor: Double = 0.0,
        costoOtros: Double = 0.0
    ): DetailedMaintenanceEntity = withContext(Dispatchers.IO) {
        atomicResult {
            val oilItem = dao.getOilInventoryItem()
            val tireItem = dao.getTireInventoryItem()

            // Precio de botella de aceite: si se pasó personalizado se usa, sino del almacén (por defecto S/ 19.00)
            val precioBotella = precioBotellaAceite ?: (oilItem?.precioUnitario ?: 19.0)
            val costoAceiteTotal = botellasAceite * precioBotella

            // Descontar botellas de los lotes disponibles (FIFO)
            if (botellasAceite > 0) {
                var botellasPorDescontar = botellasAceite
                val availableBatches = dao.getAvailableBatchesForCategory("Fluidos")
                for (batch in availableBatches) {
                    if (botellasPorDescontar <= 0) break
                    val tomar = minOf(batch.cantidadDisponible, botellasPorDescontar)
                    dao.updateBatchAvailableQuantity(batch.id, batch.cantidadDisponible - tomar)
                    botellasPorDescontar -= tomar
                }
                if (oilItem != null) {
                    val nuevoStock = (oilItem.stock - botellasAceite).coerceAtLeast(0)
                    dao.updateStock(oilItem.id, nuevoStock)
                }
            }

            // Precio y descuento de llantas (FIFO)
            val precioLlanta = precioLlantaCustom ?: (tireItem?.precioUnitario ?: 80.0)
            val costoLlantasTotal = llantasUsadas * precioLlanta
            if (llantasUsadas > 0) {
                var llantasPorDescontar = llantasUsadas
                val availableBatches = dao.getAvailableBatchesForCategory("Neumáticos")
                for (batch in availableBatches) {
                    if (llantasPorDescontar <= 0) break
                    val tomar = minOf(batch.cantidadDisponible, llantasPorDescontar)
                    dao.updateBatchAvailableQuantity(batch.id, batch.cantidadDisponible - tomar)
                    llantasPorDescontar -= tomar
                }
                if (tireItem != null) {
                    val nuevoStockLlantas = (tireItem.stock - llantasUsadas).coerceAtLeast(0)
                    dao.updateStock(tireItem.id, nuevoStockLlantas)
                }
            }

            // Las llantas se cobran SOLO como "llantas usadas" (costoLlantasTotal), nunca también como servicio.
            val sumaServicios = costoGeneral + costoFrenos + costoCana + costoPlanchado + costoCablesCambio +
                    costoBajadaMotorCompleto + costoBajadaMedioMotor + costoOtros +
                    (if (llantasUsadas > 0) 0.0 else costoLlantas)

            val manoObraEfectiva = MaintenanceMath.manoDeObra(sumaServicios, costoTrabajo, costoAceiteTotal, costoLlantasTotal)
            val costoTotal = manoObraEfectiva + costoAceiteTotal + costoLlantasTotal

            val mesAno = try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fecha)
                SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(parsed ?: Date())
            } catch (e: Exception) {
                SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            }

            val motivoParts = mutableListOf<String>()
            if (costoGeneral > 0) motivoParts.add("General S/$costoGeneral")
            if (costoCana > 0) motivoParts.add("Caña S/$costoCana")
            if (costoFrenos > 0) motivoParts.add("Frenos S/$costoFrenos")
            if (costoPlanchado > 0) motivoParts.add("Planchado S/$costoPlanchado")
            if (costoCablesCambio > 0) motivoParts.add("Cables de cambios S/$costoCablesCambio")
            if (costoBajadaMotorCompleto > 0) motivoParts.add("Motor completo S/$costoBajadaMotorCompleto")
            if (costoBajadaMedioMotor > 0) motivoParts.add("Medio motor S/$costoBajadaMedioMotor")
            if (botellasAceite > 0) motivoParts.add("$botellasAceite botellas aceite (a S/ ${"%.2f".format(precioBotella)})")
            if (llantasUsadas > 0) motivoParts.add("$llantasUsadas llantas (a S/ ${"%.2f".format(precioLlanta)})")
            if (detallesDescripcion.isNotBlank()) motivoParts.add(detallesDescripcion)

            val motivoCompleto = if (motivoParts.isNotEmpty()) motivoParts.joinToString(" + ") else tipoTrabajo

            val maintenance = DetailedMaintenanceEntity(
                unitPlaca = unit.placa,
                nombreMoto = unit.nombreMoto,
                fecha = fecha,
                mesAno = mesAno,
                tipoTrabajo = tipoTrabajo,
                detallesDescripcion = detallesDescripcion,
                costoGeneral = costoGeneral,
                costoFrenos = costoFrenos,
                costoLlantas = if (llantasUsadas > 0) costoLlantasTotal else costoLlantas,
                costoCana = costoCana,
                costoPlanchado = costoPlanchado,
                costoCablesCambio = costoCablesCambio,
                costoBajadaMotorCompleto = costoBajadaMotorCompleto,
                costoBajadaMedioMotor = costoBajadaMedioMotor,
                costoOtros = costoOtros,
                botellasAceite = botellasAceite,
                precioBotellaAceite = precioBotella,
                costoAceiteTotal = costoAceiteTotal,
                llantasUsadas = llantasUsadas,
                precioLlanta = precioLlanta,
                costoLlantasTotal = costoLlantasTotal,
                costoTotal = costoTotal,
                motivoDetallado = motivoCompleto,
                componenteMasCostoso = when {
                    costoPlanchado >= manoObraEfectiva && costoPlanchado > 0 -> "Planchado"
                    costoCana >= manoObraEfectiva && costoCana > 0 -> "Caña"
                    costoBajadaMotorCompleto > 0 -> "Motor Completo"
                    costoLlantasTotal > manoObraEfectiva -> "Llantas"
                    else -> tipoTrabajo
                }
            )

            dao.insertDetailedMaintenance(maintenance)

            // Registrar egreso en Finanzas identificando claramente la moto y los servicios
            dao.insertMovement(
                FinanceMovementEntity(
                    concepto = "Mantenimiento ${unit.nombreMoto} (${unit.placa}) - $motivoCompleto",
                    monto = costoTotal,
                    esIngreso = false,
                    categoria = "Mantenimiento Moto",
                    fecha = fecha,
                    unitPlaca = unit.placa
                )
            )

            maintenance
        }
    }

    suspend fun registerOilPurchase(botellas: Int, precioUnitario: Double, fecha: String = "") = withContext(Dispatchers.IO) {
        atomically {
            val oilItem = dao.getOilInventoryItem()
            val totalGasto = botellas * precioUnitario
            val fechaUso = if (fecha.isNotBlank()) fecha else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (oilItem != null) {
                val nuevoStock = oilItem.stock + botellas
                dao.updateInventoryItem(oilItem.copy(stock = nuevoStock, precioUnitario = precioUnitario))
            } else {
                dao.insertInventoryItem(
                    InventoryItemEntity(
                        nombre = "Aceite 4T Castrol Actevo (Botellas)",
                        categoria = "Fluidos",
                        stock = botellas,
                        stockMinimo = 6,
                        unidadMedida = "Botellas",
                        precioUnitario = precioUnitario,
                        descripcion = "Aceite para mantenimientos (2 botellas por servicio)"
                    )
                )
            }

            // Registrar lote de compra para seguimiento de precios escalonados
            dao.insertBatch(
                InventoryBatchEntity(
                    categoria = "Fluidos",
                    nombre = "Lote Aceite 4T ($botellas botellas a S/ ${"%.2f".format(precioUnitario)})",
                    cantidadInicial = botellas,
                    cantidadDisponible = botellas,
                    precioUnitario = precioUnitario,
                    fecha = fechaUso
                )
            )

            dao.insertMovement(
                FinanceMovementEntity(
                    concepto = "Compra $botellas Botellas Aceite 4T (Lote a S/ ${"%.2f".format(precioUnitario)} c/u)",
                    monto = totalGasto,
                    esIngreso = false,
                    categoria = "Compra Aceites",
                    fecha = fechaUso
                )
            )
        }
    }

    suspend fun registerTirePurchase(llantas: Int, precioUnitario: Double, fecha: String = "") = withContext(Dispatchers.IO) {
        atomically {
            val tireItem = dao.getTireInventoryItem()
            val totalGasto = llantas * precioUnitario
            val fechaUso = if (fecha.isNotBlank()) fecha else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (tireItem != null) {
                val nuevoStock = tireItem.stock + llantas
                dao.updateInventoryItem(tireItem.copy(stock = nuevoStock, precioUnitario = precioUnitario))
            } else {
                dao.insertInventoryItem(
                    InventoryItemEntity(
                        nombre = "Llantas Torito Bajaj (Neumáticos)",
                        categoria = "Neumáticos",
                        stock = llantas,
                        stockMinimo = 4,
                        unidadMedida = "Llantas",
                        precioUnitario = precioUnitario,
                        descripcion = "Neumáticos de repuesto para la flota (se usan a requerimiento de cada moto)"
                    )
                )
            }

            // Registrar lote de compra de neumáticos
            dao.insertBatch(
                InventoryBatchEntity(
                    categoria = "Neumáticos",
                    nombre = "Lote Llantas Torito ($llantas unids a S/ ${"%.2f".format(precioUnitario)})",
                    cantidadInicial = llantas,
                    cantidadDisponible = llantas,
                    precioUnitario = precioUnitario,
                    fecha = fechaUso
                )
            )

            dao.insertMovement(
                FinanceMovementEntity(
                    concepto = "Compra $llantas Llantas Torito (Lote a S/ ${"%.2f".format(precioUnitario)} c/u)",
                    monto = totalGasto,
                    esIngreso = false,
                    categoria = "Compra Repuestos",
                    fecha = fechaUso
                )
            )
        }
    }

    suspend fun updateStock(id: Long, newStock: Int) = withContext(Dispatchers.IO) {
        dao.updateStock(id, newStock.coerceAtLeast(0))
    }

    suspend fun updateInventoryItem(item: InventoryItemEntity) = withContext(Dispatchers.IO) {
        dao.updateInventoryItem(item)
    }

    suspend fun insertInventoryItem(item: InventoryItemEntity): Long = withContext(Dispatchers.IO) {
        dao.insertInventoryItem(item)
    }

    suspend fun renewContract(placa: String, newEndDate: String) = withContext(Dispatchers.IO) {
        dao.renewContract(placa, 0.1f, newEndDate)
    }

    suspend fun breakContract(placa: String) = withContext(Dispatchers.IO) {
        dao.updateUnitStatus(placa, "Inactivo")
    }

    suspend fun scheduleMaintenance(
        unitPlaca: String,
        motivo: String,
        urgencia: String,
        fecha: String,
        costo: Double
    ) = withContext(Dispatchers.IO) {
        dao.insertMaintenanceTask(
            MaintenanceTaskEntity(
                unitPlaca = unitPlaca,
                motivo = motivo,
                urgencia = urgencia,
                fechaProgramada = fecha,
                costo = costo,
                completado = false
            )
        )
    }

    suspend fun toggleMaintenanceCompletion(task: MaintenanceTaskEntity): Unit = withContext(Dispatchers.IO) {
        dao.setMaintenanceCompleted(task.id, !task.completado)
    }

    suspend fun deleteMaintenance(task: MaintenanceTaskEntity): Unit = withContext(Dispatchers.IO) {
        dao.deleteMaintenanceTask(task)
    }

    suspend fun insertMovement(movement: FinanceMovementEntity): Long = withContext(Dispatchers.IO) {
        dao.insertMovement(movement)
    }

    // ------------------------------------------------------------------
    // CORREGIR ERRORES (v1.4): anular registros hechos por equivocación.
    // Cada anulación deshace TODO lo que hizo el registro original, en una sola operación.
    // ------------------------------------------------------------------

    /** Anula un trabajo de taller: borra el registro, su gasto en Caja y devuelve aceite/llantas al almacén. */
    suspend fun deleteMaintenanceRecord(record: DetailedMaintenanceEntity) = withContext(Dispatchers.IO) {
        atomically {
            dao.deleteDetailedMaintenanceById(record.id)
            dao.deleteLinkedMovement(record.unitPlaca, "Mantenimiento Moto", record.costoTotal, record.timestamp)
            if (record.botellasAceite > 0) devolverAlAlmacen("Fluidos", record.botellasAceite, dao.getOilInventoryItem())
            if (record.llantasUsadas > 0) devolverAlAlmacen("Neumáticos", record.llantasUsadas, dao.getTireInventoryItem())
        }
    }

    private suspend fun devolverAlAlmacen(categoria: String, cantidad: Int, item: InventoryItemEntity?) {
        if (item != null) dao.updateStock(item.id, item.stock + cantidad)
        dao.getLatestBatch(categoria)?.let { lote ->
            val nueva = (lote.cantidadDisponible + cantidad).coerceAtMost(maxOf(lote.cantidadInicial, lote.cantidadDisponible + cantidad))
            dao.updateBatchAvailableQuantity(lote.id, nueva)
        }
    }

    /**
     * Anula un movimiento de Caja. Si era una cuota diaria, también se desmarca el pago del día
     * (si no, el cobro diario seguiría diciendo "pagado" sin dinero en Caja).
     */
    suspend fun deleteMovement(mov: FinanceMovementEntity) = withContext(Dispatchers.IO) {
        atomically {
            dao.deleteMovementById(mov.id)
            val placa = mov.unitPlaca
            if (placa != null && (mov.categoria.startsWith("Pago Diario") || mov.categoria.startsWith("Pago Adelantado"))) {
                dao.deleteDailyPayment(mov.fecha, placa)
            }
        }
    }

    /**
     * Anula un cargo o un abono de deuda y corrige el saldo de la moto.
     * Un abono anulado también saca de Caja el dinero que había entrado.
     */
    suspend fun deleteDebtRecord(record: DebtRecordEntity) = withContext(Dispatchers.IO) {
        atomically {
            val unit = dao.getUnitByPlaca(record.unitPlaca)
            dao.deleteDebtRecordById(record.id)
            if (unit != null) {
                val ajuste = com.example.util.DebtMath.reverse(
                    tipo = record.tipo,
                    monto = record.monto,
                    motivo = record.motivo,
                    deudaTotal = unit.deudaTotal,
                    deudaGarantia = unit.deudaGarantia
                )
                dao.updateUnitDebtAndGarantia(unit.placa, ajuste.deudaTotal, ajuste.deudaGarantia)
            }
            if (record.tipo == "ABONO") {
                dao.deleteLinkedMovement(record.unitPlaca, "Abono Garantía", record.monto, record.timestamp)
            }
        }
    }

    /**
     * Cobro de cuota de un monto distinto a la cuota. Si pagó menos y [restoADeuda] es true,
     * lo que falta se registra como deuda de la moto (todo en una sola operación).
     */
    suspend fun recordPayment(unit: FleetUnitEntity, fecha: String, monto: Double, restoADeuda: Boolean): Double =
        withContext(Dispatchers.IO) {
            atomicResult {
                val falta = com.example.util.DebtMath.faltante(unit.cuotaDiaria, monto)
                toggleDailyPayment(unit, fecha, true, monto)
                if (restoADeuda && falta > 0.0) {
                    addDebtCargo(unit, falta, "Cuota incompleta del $fecha (pagó S/ ${"%.2f".format(monto)} de S/ ${"%.2f".format(unit.cuotaDiaria)})")
                    falta
                } else 0.0
            }
        }

    /** Pagos registrados de una moto en un mes ("2026-11"). */
    suspend fun paymentsOfMonth(placa: String, mes: String): List<DailyPaymentEntity> = withContext(Dispatchers.IO) {
        dao.getPaymentsOfMonth(placa, mes)
    }
}
