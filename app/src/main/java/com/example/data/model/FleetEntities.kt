package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fleet_units")
data class FleetUnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombreMoto: String = "Moto 1", // Identificación principal: Moto 1, Moto 2, etc.
    val placa: String,
    val cliente: String = "Sin asignar", // Chofer asignado (cambiante)
    val telefono: String = "+51 987 654 321",
    val estado: String = "Vigente", // "Vigente", "Por Vencer", "En Taller", "Inactivo"
    val cuotaDiaria: Double = 35.0, // Fijo S/ 35.00
    val deudaGarantia: Double = 0.0, // Garantía total es 200; si dejó 100, debe 100
    val deudaTotal: Double = 0.0, // Saldo deudor pendiente
    val progresoContrato: Float = 0.5f,
    val inicioContrato: String = "2026-08-01",
    val finContrato: String = "2026-11-01",
    val tipoDuracion: String = "3 Meses", // "1 Semana (Provisional)", "1 Mes", "3 Meses", etc.
    val soatInicio: String = "2026-09-01",
    val soatFin: String = "2027-09-01",
    val soatVence: String = "365 Días",
    val soatDiasRestantes: Int = 365,
    val revVence: String = "Al día este mes",
    val revDiasRestantes: Int = 180,
    val revisionTecnicaMesRealizada: Boolean = true,
    val kilometraje: Int = 12500,
    val observaciones: String = "Unidad en operación normal"
)

@Entity(tableName = "daily_payments")
data class DailyPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: String, // YYYY-MM-DD
    val unitPlaca: String,
    val nombreMoto: String,
    val cuotaMonto: Double = 35.0,
    val pagado: Boolean = true,
    val hora: String = "10:00 AM",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "debt_records")
data class DebtRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitPlaca: String,
    val nombreMoto: String,
    val tipo: String, // "CARGO" (+ deuda, ej. falta garantía o daño) o "ABONO" (- deuda, pago en partes)
    val monto: Double,
    val motivo: String,
    val fecha: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "detailed_maintenances")
data class DetailedMaintenanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitPlaca: String,
    val nombreMoto: String,
    val fecha: String, // "15 Ago 2026"
    val mesAno: String, // "2026-08", "2026-09"
    val tipoTrabajo: String = "Mantenimiento General", // "Mantenimiento General", "Frenos", "Caña", "Bajada de motor completo", "Bajada medio motor", "Planchado"
    val detallesDescripcion: String = "",
    val costoGeneral: Double = 0.0,
    val costoFrenos: Double = 0.0,
    val costoLlantas: Double = 0.0,
    val costoPlanchado: Double = 0.0,
    val costoCana: Double = 0.0, // Caña / Horquilla delantera donde va la llanta delantera
    val costoBajadaMotorCompleto: Double = 0.0,
    val costoBajadaMedioMotor: Double = 0.0,
    val costoOtros: Double = 0.0,
    val botellasAceite: Int = 2, // 2 botellas si es Mantenimiento General
    val precioBotellaAceite: Double = 25.0,
    val costoAceiteTotal: Double = 50.0,
    val llantasUsadas: Int = 0, // Cantidad de llantas tomadas del inventario de taller
    val precioLlanta: Double = 85.0,
    val costoLlantasTotal: Double = 0.0,
    val costoTotal: Double = 0.0,
    val motivoDetallado: String = "",
    val componenteMasCostoso: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "unit_expenses")
data class UnitExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitPlaca: String,
    val motivo: String,
    val monto: Double,
    val fecha: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "finance_movements")
data class FinanceMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val concepto: String,
    val monto: Double,
    val esIngreso: Boolean,
    val categoria: String, // "Pago Diario S/35", "Abono Garantía", "Mantenimiento Moto", "Compra Aceites", "Compra Repuestos", "Otros"
    val fecha: String,
    val unitPlaca: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val categoria: String, // "Fluidos", "Neumáticos", "Frenos", "Repuestos"
    val stock: Int, // En botellas para aceite!
    val stockMinimo: Int = 4,
    val unidadMedida: String = "Botellas",
    val precioUnitario: Double = 25.0,
    val descripcion: String = ""
)

@Entity(tableName = "maintenance_tasks")
data class MaintenanceTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitPlaca: String,
    val motivo: String,
    val urgencia: String = "Normal", // "Alta", "Media", "Normal"
    val fechaProgramada: String = "Hoy",
    val completado: Boolean = false,
    val costo: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory_batches")
data class InventoryBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoria: String, // "Fluidos", "Neumáticos"
    val nombre: String,
    val cantidadInicial: Int,
    val cantidadDisponible: Int,
    val precioUnitario: Double,
    val fecha: String,
    val timestamp: Long = System.currentTimeMillis()
)

