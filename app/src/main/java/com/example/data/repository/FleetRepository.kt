package com.example.data.repository

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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FleetRepository(private val dao: FleetDao) {

    val allUnits: Flow<List<FleetUnitEntity>> = dao.getAllUnits()
    val allMovements: Flow<List<FinanceMovementEntity>> = dao.getAllMovements()
    val allInventory: Flow<List<InventoryItemEntity>> = dao.getAllInventory()
    val allInventoryBatches: Flow<List<InventoryBatchEntity>> = dao.getAllInventoryBatches()
    val allMaintenanceTasks: Flow<List<MaintenanceTaskEntity>> = dao.getAllMaintenanceTasks()
    val allDailyPayments: Flow<List<DailyPaymentEntity>> = dao.getAllDailyPayments()
    val allDebtRecords: Flow<List<DebtRecordEntity>> = dao.getAllDebtRecords()
    val allDetailedMaintenances: Flow<List<DetailedMaintenanceEntity>> = dao.getAllDetailedMaintenances()

    fun getDailyPaymentsForDate(fecha: String): Flow<List<DailyPaymentEntity>> =
        dao.getDailyPaymentsForDate(fecha)

    fun getMaintenancesForMonth(mesAno: String): Flow<List<DetailedMaintenanceEntity>> =
        dao.getMaintenancesForMonth(mesAno)

    fun getExpensesForUnit(placa: String): Flow<List<UnitExpenseEntity>> =
        dao.getExpensesForUnit(placa)

    suspend fun seedDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        val inventory = dao.getAllInventory().first()
        if (inventory.isEmpty()) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dao.insertInventoryItem(
                InventoryItemEntity(
                    nombre = "Aceite 4T Castrol Actevo (Botellas)",
                    categoria = "Fluidos",
                    stock = 50,
                    stockMinimo = 6,
                    unidadMedida = "Botellas",
                    precioUnitario = 19.0,
                    descripcion = "Aceite 4T para mantenimientos (2 botellas por servicio)"
                )
            )
            dao.insertBatch(
                InventoryBatchEntity(
                    categoria = "Fluidos",
                    nombre = "Lote Inicial Aceite Castrol 4T (50 botellas a S/ 19.00)",
                    cantidadInicial = 50,
                    cantidadDisponible = 50,
                    precioUnitario = 19.0,
                    fecha = dateStr
                )
            )
            dao.insertInventoryItem(
                InventoryItemEntity(
                    nombre = "Llantas Torito Bajaj (Neumáticos)",
                    categoria = "Neumáticos",
                    stock = 10,
                    stockMinimo = 4,
                    unidadMedida = "Llantas",
                    precioUnitario = 80.0,
                    descripcion = "Neumáticos Torito Bajaj para la flota"
                )
            )
            dao.insertBatch(
                InventoryBatchEntity(
                    categoria = "Neumáticos",
                    nombre = "Lote Inicial Llantas Torito (10 llantas a S/ 80.00)",
                    cantidadInicial = 10,
                    cantidadDisponible = 10,
                    precioUnitario = 80.0,
                    fecha = dateStr
                )
            )
        }
    }

    suspend fun getFullBackupPayload(userEmail: String): FleetBackupPayload = withContext(Dispatchers.IO) {
        val units = allUnits.first()
        val dailyPayments = allDailyPayments.first()
        val debtRecords = allDebtRecords.first()
        val detailedMaintenances = allDetailedMaintenances.first()
        val expenses = dao.getAllUnitExpenses().first()
        val movements = allMovements.first()
        val inventory = allInventory.first()
        val tasks = allMaintenanceTasks.first()

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
            maintenanceTasks = tasks
        )
    }

    suspend fun restoreFromBackup(payload: FleetBackupPayload) = withContext(Dispatchers.IO) {
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

    suspend fun updateUnit(unit: FleetUnitEntity) = withContext(Dispatchers.IO) {
        dao.updateUnit(unit)
    }

    suspend fun deleteUnit(unit: FleetUnitEntity) = withContext(Dispatchers.IO) {
        dao.deleteUnit(unit)
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
        val nuevaDeuda = unit.deudaTotal + monto
        dao.updateUnitDebt(unit.placa, nuevaDeuda)
    }

    suspend fun addDebtAbono(unit: FleetUnitEntity, monto: Double, motivo: String) = withContext(Dispatchers.IO) {
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
        val nuevaDeuda = (unit.deudaTotal - monto).coerceAtLeast(0.0)
        val nuevaDeudaGarantia = (unit.deudaGarantia - monto).coerceAtLeast(0.0)
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
        costoBajadaMotorCompleto: Double = 0.0,
        costoBajadaMedioMotor: Double = 0.0,
        costoOtros: Double = 0.0
    ): DetailedMaintenanceEntity = withContext(Dispatchers.IO) {
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

        val sumaServicios = costoGeneral + costoFrenos + costoLlantas + costoCana + costoPlanchado +
                costoBajadaMotorCompleto + costoBajadaMedioMotor + costoOtros

        val manoObraEfectiva = if (sumaServicios > 0.0) sumaServicios else costoTrabajo
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
            costoLlantas = costoLlantas + costoLlantasTotal,
            costoCana = costoCana,
            costoPlanchado = costoPlanchado,
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

    suspend fun registerOilPurchase(botellas: Int, precioUnitario: Double, fecha: String = "") = withContext(Dispatchers.IO) {
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

    suspend fun registerTirePurchase(llantas: Int, precioUnitario: Double, fecha: String = "") = withContext(Dispatchers.IO) {
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
}

