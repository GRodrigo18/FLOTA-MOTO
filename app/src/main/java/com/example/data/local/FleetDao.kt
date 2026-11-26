package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyPaymentEntity
import com.example.data.model.DebtRecordEntity
import com.example.data.model.DetailedMaintenanceEntity
import com.example.data.model.FinanceMovementEntity
import com.example.data.model.FleetUnitEntity
import com.example.data.model.InventoryBatchEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.MaintenanceTaskEntity
import com.example.data.model.UnitExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FleetDao {

    // --- UNITS ---
    @Query("SELECT * FROM fleet_units ORDER BY id ASC")
    fun getAllUnits(): Flow<List<FleetUnitEntity>>

    @Query("SELECT * FROM fleet_units WHERE id = :id LIMIT 1")
    suspend fun getUnitById(id: Long): FleetUnitEntity?

    @Query("SELECT * FROM fleet_units WHERE placa = :placa LIMIT 1")
    suspend fun getUnitByPlaca(placa: String): FleetUnitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: FleetUnitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<FleetUnitEntity>)

    @Update
    suspend fun updateUnit(unit: FleetUnitEntity)

    @Delete
    suspend fun deleteUnit(unit: FleetUnitEntity)

    @Query("UPDATE fleet_units SET estado = :nuevoEstado WHERE placa = :placa")
    suspend fun updateUnitStatus(placa: String, nuevoEstado: String)

    @Query("UPDATE fleet_units SET progresoContrato = :progreso, finContrato = :nuevoFin WHERE placa = :placa")
    suspend fun renewContract(placa: String, progreso: Float, nuevoFin: String)

    @Query("UPDATE fleet_units SET deudaTotal = :nuevaDeuda WHERE placa = :placa")
    suspend fun updateUnitDebt(placa: String, nuevaDeuda: Double)

    @Query("UPDATE fleet_units SET deudaTotal = :nuevaDeuda, deudaGarantia = :nuevaDeudaGarantia WHERE placa = :placa")
    suspend fun updateUnitDebtAndGarantia(placa: String, nuevaDeuda: Double, nuevaDeudaGarantia: Double)

    @Query("UPDATE fleet_units SET revisionTecnicaMesRealizada = :realizada WHERE placa = :placa")
    suspend fun updateRevisionTecnicaMes(placa: String, realizada: Boolean)

    @Query("UPDATE fleet_units SET revisionTecnicaMesRealizada = 0")
    suspend fun resetRevisionTecnicaMesTodas()

    @Query("UPDATE fleet_units SET cuotaDiaria = :cuota WHERE id = :id")
    suspend fun updateUnitCuota(id: Long, cuota: Double)

    @Query("UPDATE fleet_units SET cuotaDiaria = :cuota")
    suspend fun updateAllUnitsCuota(cuota: Double)

    // --- DAILY PAYMENTS ---
    @Query("SELECT * FROM daily_payments ORDER BY timestamp DESC")
    fun getAllDailyPayments(): Flow<List<DailyPaymentEntity>>

    @Query("SELECT * FROM daily_payments WHERE fecha = :fecha")
    fun getDailyPaymentsForDate(fecha: String): Flow<List<DailyPaymentEntity>>

    @Query("SELECT * FROM daily_payments WHERE fecha = :fecha AND unitPlaca = :placa LIMIT 1")
    suspend fun getPaymentForUnitAndDate(fecha: String, placa: String): DailyPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPayment(payment: DailyPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPayments(payments: List<DailyPaymentEntity>)

    @Query("DELETE FROM daily_payments WHERE fecha = :fecha AND unitPlaca = :placa")
    suspend fun deleteDailyPayment(fecha: String, placa: String)

    @Query("DELETE FROM finance_movements WHERE unitPlaca = :placa AND fecha = :fecha AND (categoria LIKE 'Pago Diario%' OR categoria LIKE 'Pago Adelantado%')")
    suspend fun deleteDailyPaymentMovement(placa: String, fecha: String)

    // --- DEBTS ---
    @Query("SELECT * FROM debt_records ORDER BY timestamp DESC")
    fun getAllDebtRecords(): Flow<List<DebtRecordEntity>>

    @Query("SELECT * FROM debt_records WHERE unitPlaca = :placa ORDER BY timestamp DESC")
    fun getDebtsForUnit(placa: String): Flow<List<DebtRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtRecord(record: DebtRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtRecords(records: List<DebtRecordEntity>)

    // --- DETAILED MAINTENANCES ---
    @Query("SELECT * FROM detailed_maintenances ORDER BY timestamp DESC")
    fun getAllDetailedMaintenances(): Flow<List<DetailedMaintenanceEntity>>

    @Query("SELECT * FROM detailed_maintenances WHERE mesAno = :mesAno ORDER BY costoTotal DESC")
    fun getMaintenancesForMonth(mesAno: String): Flow<List<DetailedMaintenanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetailedMaintenance(maintenance: DetailedMaintenanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetailedMaintenances(maintenances: List<DetailedMaintenanceEntity>)

    // --- UNIT EXPENSES ---
    @Query("SELECT * FROM unit_expenses WHERE unitPlaca = :placa ORDER BY timestamp DESC")
    fun getExpensesForUnit(placa: String): Flow<List<UnitExpenseEntity>>

    @Query("SELECT * FROM unit_expenses ORDER BY timestamp DESC")
    fun getAllUnitExpenses(): Flow<List<UnitExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnitExpense(expense: UnitExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnitExpenses(expenses: List<UnitExpenseEntity>)

    // --- FINANCE MOVEMENTS ---
    @Query("SELECT * FROM finance_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<FinanceMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: FinanceMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<FinanceMovementEntity>)

    @Delete
    suspend fun deleteMovement(movement: FinanceMovementEntity)

    // --- INVENTORY ---
    @Query("SELECT * FROM inventory_items ORDER BY id ASC")
    fun getAllInventory(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE categoria = 'Fluidos' OR nombre LIKE '%Aceite%' LIMIT 1")
    suspend fun getOilInventoryItem(): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE categoria = 'Neumáticos' OR nombre LIKE '%Llanta%' OR nombre LIKE '%Neumático%' LIMIT 1")
    suspend fun getTireInventoryItem(): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<InventoryItemEntity>)

    @Update
    suspend fun updateInventoryItem(item: InventoryItemEntity)

    @Query("UPDATE inventory_items SET stock = :newStock WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Int)

    // --- MAINTENANCE TASKS ---
    @Query("SELECT * FROM maintenance_tasks ORDER BY completado ASC, timestamp DESC")
    fun getAllMaintenanceTasks(): Flow<List<MaintenanceTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceTask(task: MaintenanceTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceTasks(tasks: List<MaintenanceTaskEntity>)

    @Query("UPDATE maintenance_tasks SET completado = :completado WHERE id = :id")
    suspend fun setMaintenanceCompleted(id: Long, completado: Boolean)

    @Delete
    suspend fun deleteMaintenanceTask(task: MaintenanceTaskEntity)

    @Query("DELETE FROM fleet_units")
    suspend fun deleteAllUnits()

    @Query("DELETE FROM daily_payments")
    suspend fun deleteAllDailyPayments()

    @Query("DELETE FROM debt_records")
    suspend fun deleteAllDebtRecords()

    @Query("DELETE FROM detailed_maintenances")
    suspend fun deleteAllDetailedMaintenances()

    @Query("DELETE FROM unit_expenses")
    suspend fun deleteAllUnitExpenses()

    @Query("DELETE FROM finance_movements")
    suspend fun deleteAllMovements()

    @Query("DELETE FROM inventory_items")
    suspend fun deleteAllInventory()

    @Query("DELETE FROM maintenance_tasks")
    suspend fun deleteAllMaintenanceTasks()

    // --- INVENTORY BATCHES (LOTES DE COMPRA) ---
    @Query("SELECT * FROM inventory_batches ORDER BY timestamp DESC")
    fun getAllInventoryBatches(): Flow<List<InventoryBatchEntity>>

    @Query("SELECT * FROM inventory_batches WHERE categoria = :categoria AND cantidadDisponible > 0 ORDER BY timestamp ASC")
    suspend fun getAvailableBatchesForCategory(categoria: String): List<InventoryBatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: InventoryBatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<InventoryBatchEntity>)

    @Query("UPDATE inventory_batches SET cantidadDisponible = :nuevaCantidad WHERE id = :id")
    suspend fun updateBatchAvailableQuantity(id: Long, nuevaCantidad: Int)

    @Query("DELETE FROM inventory_batches")
    suspend fun deleteAllBatches()

    // --- CAMBIAR NOMBRE / PLACA DE UNA MOTO (se arrastra a todo su historial) ---
    @Query("UPDATE fleet_units SET nombreMoto = :nombre, placa = :nuevaPlaca WHERE placa = :placa")
    suspend fun renameUnitRow(placa: String, nombre: String, nuevaPlaca: String)

    @Query("UPDATE daily_payments SET nombreMoto = :nombre, unitPlaca = :nuevaPlaca WHERE unitPlaca = :placa")
    suspend fun renameInPayments(placa: String, nombre: String, nuevaPlaca: String)

    @Query("UPDATE debt_records SET nombreMoto = :nombre, unitPlaca = :nuevaPlaca WHERE unitPlaca = :placa")
    suspend fun renameInDebts(placa: String, nombre: String, nuevaPlaca: String)

    @Query("UPDATE detailed_maintenances SET nombreMoto = :nombre, unitPlaca = :nuevaPlaca WHERE unitPlaca = :placa")
    suspend fun renameInMaintenances(placa: String, nombre: String, nuevaPlaca: String)

    @Query("UPDATE unit_expenses SET unitPlaca = :nuevaPlaca WHERE unitPlaca = :placa")
    suspend fun renameInExpenses(placa: String, nuevaPlaca: String)

    @Query("UPDATE finance_movements SET unitPlaca = :nuevaPlaca WHERE unitPlaca = :placa")
    suspend fun renameInMovements(placa: String, nuevaPlaca: String)

    @Query("UPDATE maintenance_tasks SET unitPlaca = :nuevaPlaca WHERE unitPlaca = :placa")
    suspend fun renameInTasks(placa: String, nuevaPlaca: String)

    // --- ELIMINAR UNA MOTO REGISTRADA POR ERROR (con todos sus registros) ---
    @Query("SELECT COUNT(*) FROM daily_payments WHERE unitPlaca = :placa")
    suspend fun countPayments(placa: String): Int

    @Query("SELECT COUNT(*) FROM detailed_maintenances WHERE unitPlaca = :placa")
    suspend fun countMaintenances(placa: String): Int

    @Query("SELECT COUNT(*) FROM finance_movements WHERE unitPlaca = :placa")
    suspend fun countMovements(placa: String): Int

    @Query("DELETE FROM fleet_units WHERE placa = :placa")
    suspend fun deleteUnitByPlaca(placa: String)

    @Query("DELETE FROM daily_payments WHERE unitPlaca = :placa")
    suspend fun deletePaymentsOf(placa: String)

    @Query("DELETE FROM debt_records WHERE unitPlaca = :placa")
    suspend fun deleteDebtsOf(placa: String)

    @Query("DELETE FROM detailed_maintenances WHERE unitPlaca = :placa")
    suspend fun deleteMaintenancesOf(placa: String)

    @Query("DELETE FROM unit_expenses WHERE unitPlaca = :placa")
    suspend fun deleteExpensesOf(placa: String)

    @Query("DELETE FROM finance_movements WHERE unitPlaca = :placa")
    suspend fun deleteMovementsOf(placa: String)

    @Query("DELETE FROM maintenance_tasks WHERE unitPlaca = :placa")
    suspend fun deleteTasksOf(placa: String)

    // --- CORREGIR / ANULAR UN REGISTRO HECHO POR ERROR (v1.4) ---
    @Query("DELETE FROM detailed_maintenances WHERE id = :id")
    suspend fun deleteDetailedMaintenanceById(id: Long)

    @Query("DELETE FROM finance_movements WHERE id = :id")
    suspend fun deleteMovementById(id: Long)

    @Query("DELETE FROM debt_records WHERE id = :id")
    suspend fun deleteDebtRecordById(id: Long)

    /** Cargos "Cuota incompleta del <fecha>" de una moto (se quitan si se borra o cambia ese pago). */
    @Query("SELECT * FROM debt_records WHERE unitPlaca = :placa AND tipo = 'CARGO' AND motivo LIKE :prefijo || '%'")
    suspend fun getCargosByMotivoPrefix(placa: String, prefijo: String): List<DebtRecordEntity>

    /**
     * Borra el movimiento de Caja que se creó junto con otro registro (mismo monto, moto y categoría),
     * eligiendo el más cercano en el tiempo. Devuelve cuántas filas borró (0 o 1).
     */
    @Query(
        "DELETE FROM finance_movements WHERE id = (SELECT id FROM finance_movements " +
            "WHERE unitPlaca = :placa AND categoria = :categoria AND ABS(monto - :monto) < 0.005 " +
            "ORDER BY ABS(timestamp - :timestamp) LIMIT 1)"
    )
    suspend fun deleteLinkedMovement(placa: String, categoria: String, monto: Double, timestamp: Long): Int

    /** Lote más reciente de una categoría (para devolver insumos al anular un trabajo). */
    @Query("SELECT * FROM inventory_batches WHERE categoria = :categoria ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBatch(categoria: String): InventoryBatchEntity?

    @Query("SELECT * FROM daily_payments WHERE unitPlaca = :placa AND fecha LIKE :mesPrefix || '%'")
    suspend fun getPaymentsOfMonth(placa: String, mesPrefix: String): List<DailyPaymentEntity>

    /** Días que ya tienen registro (pagado o "No trabajó") de una moto. */
    @Query("SELECT fecha FROM daily_payments WHERE unitPlaca = :placa")
    suspend fun getPaymentDatesOf(placa: String): List<String>
}
