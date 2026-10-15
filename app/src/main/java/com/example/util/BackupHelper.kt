package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.DailyPaymentEntity
import com.example.data.model.DebtRecordEntity
import com.example.data.model.DetailedMaintenanceEntity
import com.example.data.model.FinanceMovementEntity
import com.example.data.model.FleetUnitEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.MaintenanceTaskEntity
import com.example.data.model.UnitExpenseEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FleetBackupPayload(
    val exportDate: String,
    val userEmail: String,
    val units: List<FleetUnitEntity>,
    val dailyPayments: List<DailyPaymentEntity>,
    val debtRecords: List<DebtRecordEntity>,
    val detailedMaintenances: List<DetailedMaintenanceEntity>,
    val unitExpenses: List<UnitExpenseEntity>,
    val financeMovements: List<FinanceMovementEntity>,
    val inventoryItems: List<InventoryItemEntity>,
    val maintenanceTasks: List<MaintenanceTaskEntity>
)

object BackupHelper {

    fun serializeToJson(payload: FleetBackupPayload): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportDate", payload.exportDate)
        root.put("userEmail", payload.userEmail)

        // Units
        val unitsArray = JSONArray()
        payload.units.forEach { u ->
            val obj = JSONObject()
            obj.put("nombreMoto", u.nombreMoto)
            obj.put("placa", u.placa)
            obj.put("cliente", u.cliente)
            obj.put("telefono", u.telefono)
            obj.put("estado", u.estado)
            obj.put("cuotaDiaria", u.cuotaDiaria)
            obj.put("deudaGarantia", u.deudaGarantia)
            obj.put("deudaTotal", u.deudaTotal)
            obj.put("progresoContrato", u.progresoContrato.toDouble())
            obj.put("inicioContrato", u.inicioContrato)
            obj.put("finContrato", u.finContrato)
            obj.put("tipoDuracion", u.tipoDuracion)
            obj.put("soatInicio", u.soatInicio)
            obj.put("soatFin", u.soatFin)
            obj.put("soatVence", u.soatVence)
            obj.put("soatDiasRestantes", u.soatDiasRestantes)
            obj.put("revVence", u.revVence)
            obj.put("revDiasRestantes", u.revDiasRestantes)
            obj.put("kilometraje", u.kilometraje)
            obj.put("observaciones", u.observaciones)
            unitsArray.put(obj)
        }
        root.put("units", unitsArray)

        // Daily Payments
        val paymentsArray = JSONArray()
        payload.dailyPayments.forEach { p ->
            val obj = JSONObject()
            obj.put("fecha", p.fecha)
            obj.put("unitPlaca", p.unitPlaca)
            obj.put("nombreMoto", p.nombreMoto)
            obj.put("cuotaMonto", p.cuotaMonto)
            obj.put("pagado", p.pagado)
            obj.put("hora", p.hora)
            obj.put("timestamp", p.timestamp)
            paymentsArray.put(obj)
        }
        root.put("dailyPayments", paymentsArray)

        // Debt Records
        val debtsArray = JSONArray()
        payload.debtRecords.forEach { d ->
            val obj = JSONObject()
            obj.put("unitPlaca", d.unitPlaca)
            obj.put("nombreMoto", d.nombreMoto)
            obj.put("tipo", d.tipo)
            obj.put("monto", d.monto)
            obj.put("motivo", d.motivo)
            obj.put("fecha", d.fecha)
            obj.put("timestamp", d.timestamp)
            debtsArray.put(obj)
        }
        root.put("debtRecords", debtsArray)

        // Detailed Maintenances
        val maintArray = JSONArray()
        payload.detailedMaintenances.forEach { m ->
            val obj = JSONObject()
            obj.put("unitPlaca", m.unitPlaca)
            obj.put("nombreMoto", m.nombreMoto)
            obj.put("fecha", m.fecha)
            obj.put("mesAno", m.mesAno)
            obj.put("tipoTrabajo", m.tipoTrabajo)
            obj.put("detallesDescripcion", m.detallesDescripcion)
            obj.put("costoGeneral", m.costoGeneral)
            obj.put("costoFrenos", m.costoFrenos)
            obj.put("costoLlantas", m.costoLlantas)
            obj.put("costoPlanchado", m.costoPlanchado)
            obj.put("costoCana", m.costoCana)
            obj.put("costoBajadaMotorCompleto", m.costoBajadaMotorCompleto)
            obj.put("costoBajadaMedioMotor", m.costoBajadaMedioMotor)
            obj.put("costoOtros", m.costoOtros)
            obj.put("botellasAceite", m.botellasAceite)
            obj.put("precioBotellaAceite", m.precioBotellaAceite)
            obj.put("costoAceiteTotal", m.costoAceiteTotal)
            obj.put("llantasUsadas", m.llantasUsadas)
            obj.put("precioLlanta", m.precioLlanta)
            obj.put("costoLlantasTotal", m.costoLlantasTotal)
            obj.put("costoTotal", m.costoTotal)
            obj.put("motivoDetallado", m.motivoDetallado)
            obj.put("componenteMasCostoso", m.componenteMasCostoso)
            obj.put("timestamp", m.timestamp)
            maintArray.put(obj)
        }
        root.put("detailedMaintenances", maintArray)

        // Unit Expenses
        val expensesArray = JSONArray()
        payload.unitExpenses.forEach { e ->
            val obj = JSONObject()
            obj.put("unitPlaca", e.unitPlaca)
            obj.put("motivo", e.motivo)
            obj.put("monto", e.monto)
            obj.put("fecha", e.fecha)
            obj.put("timestamp", e.timestamp)
            expensesArray.put(obj)
        }
        root.put("unitExpenses", expensesArray)

        // Finance Movements
        val movementsArray = JSONArray()
        payload.financeMovements.forEach { m ->
            val obj = JSONObject()
            obj.put("concepto", m.concepto)
            obj.put("monto", m.monto)
            obj.put("esIngreso", m.esIngreso)
            obj.put("categoria", m.categoria)
            obj.put("fecha", m.fecha)
            obj.put("unitPlaca", m.unitPlaca ?: "")
            obj.put("timestamp", m.timestamp)
            movementsArray.put(obj)
        }
        root.put("financeMovements", movementsArray)

        // Inventory Items
        val invArray = JSONArray()
        payload.inventoryItems.forEach { i ->
            val obj = JSONObject()
            obj.put("nombre", i.nombre)
            obj.put("categoria", i.categoria)
            obj.put("stock", i.stock)
            obj.put("stockMinimo", i.stockMinimo)
            obj.put("unidadMedida", i.unidadMedida)
            obj.put("precioUnitario", i.precioUnitario)
            obj.put("descripcion", i.descripcion)
            invArray.put(obj)
        }
        root.put("inventoryItems", invArray)

        // Maintenance Tasks
        val tasksArray = JSONArray()
        payload.maintenanceTasks.forEach { t ->
            val obj = JSONObject()
            obj.put("unitPlaca", t.unitPlaca)
            obj.put("motivo", t.motivo)
            obj.put("urgencia", t.urgencia)
            obj.put("fechaProgramada", t.fechaProgramada)
            obj.put("completado", t.completado)
            obj.put("costo", t.costo)
            obj.put("timestamp", t.timestamp)
            tasksArray.put(obj)
        }
        root.put("maintenanceTasks", tasksArray)

        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): FleetBackupPayload {
        val root = JSONObject(jsonString)
        val exportDate = root.optString("exportDate", "Desconocido")
        val userEmail = root.optString("userEmail", "admin@motoflota.pe")

        val units = mutableListOf<FleetUnitEntity>()
        val unitsArray = root.optJSONArray("units") ?: JSONArray()
        for (i in 0 until unitsArray.length()) {
            val obj = unitsArray.getJSONObject(i)
            units.add(
                FleetUnitEntity(
                    nombreMoto = obj.optString("nombreMoto", "Moto"),
                    placa = obj.optString("placa", ""),
                    cliente = obj.optString("cliente", "Sin asignar"),
                    telefono = obj.optString("telefono", ""),
                    estado = obj.optString("estado", "Vigente"),
                    cuotaDiaria = obj.optDouble("cuotaDiaria", 35.0),
                    deudaGarantia = obj.optDouble("deudaGarantia", 0.0),
                    deudaTotal = obj.optDouble("deudaTotal", 0.0),
                    progresoContrato = obj.optDouble("progresoContrato", 0.5).toFloat(),
                    inicioContrato = obj.optString("inicioContrato", ""),
                    finContrato = obj.optString("finContrato", ""),
                    tipoDuracion = obj.optString("tipoDuracion", "3 Meses"),
                    soatInicio = obj.optString("soatInicio", ""),
                    soatFin = obj.optString("soatFin", ""),
                    soatVence = obj.optString("soatVence", ""),
                    soatDiasRestantes = obj.optInt("soatDiasRestantes", 30),
                    revVence = obj.optString("revVence", ""),
                    revDiasRestantes = obj.optInt("revDiasRestantes", 60),
                    kilometraje = obj.optInt("kilometraje", 0),
                    observaciones = obj.optString("observaciones", "")
                )
            )
        }

        val dailyPayments = mutableListOf<DailyPaymentEntity>()
        val paymentsArray = root.optJSONArray("dailyPayments") ?: JSONArray()
        for (i in 0 until paymentsArray.length()) {
            val obj = paymentsArray.getJSONObject(i)
            dailyPayments.add(
                DailyPaymentEntity(
                    fecha = obj.optString("fecha", ""),
                    unitPlaca = obj.optString("unitPlaca", ""),
                    nombreMoto = obj.optString("nombreMoto", ""),
                    cuotaMonto = obj.optDouble("cuotaMonto", 35.0),
                    pagado = obj.optBoolean("pagado", true),
                    hora = obj.optString("hora", "10:00 AM"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }

        val debtRecords = mutableListOf<DebtRecordEntity>()
        val debtsArray = root.optJSONArray("debtRecords") ?: JSONArray()
        for (i in 0 until debtsArray.length()) {
            val obj = debtsArray.getJSONObject(i)
            debtRecords.add(
                DebtRecordEntity(
                    unitPlaca = obj.optString("unitPlaca", ""),
                    nombreMoto = obj.optString("nombreMoto", ""),
                    tipo = obj.optString("tipo", "CARGO"),
                    monto = obj.optDouble("monto", 0.0),
                    motivo = obj.optString("motivo", ""),
                    fecha = obj.optString("fecha", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }

        val detailedMaintenances = mutableListOf<DetailedMaintenanceEntity>()
        val maintArray = root.optJSONArray("detailedMaintenances") ?: JSONArray()
        for (i in 0 until maintArray.length()) {
            val obj = maintArray.getJSONObject(i)
            detailedMaintenances.add(
                DetailedMaintenanceEntity(
                    unitPlaca = obj.optString("unitPlaca", ""),
                    nombreMoto = obj.optString("nombreMoto", ""),
                    fecha = obj.optString("fecha", ""),
                    mesAno = obj.optString("mesAno", ""),
                    tipoTrabajo = obj.optString("tipoTrabajo", "Mantenimiento General"),
                    detallesDescripcion = obj.optString("detallesDescripcion", ""),
                    costoGeneral = obj.optDouble("costoGeneral", 0.0),
                    costoFrenos = obj.optDouble("costoFrenos", 0.0),
                    costoLlantas = obj.optDouble("costoLlantas", 0.0),
                    costoPlanchado = obj.optDouble("costoPlanchado", 0.0),
                    costoCana = obj.optDouble("costoCana", 0.0),
                    costoBajadaMotorCompleto = obj.optDouble("costoBajadaMotorCompleto", 0.0),
                    costoBajadaMedioMotor = obj.optDouble("costoBajadaMedioMotor", 0.0),
                    costoOtros = obj.optDouble("costoOtros", 0.0),
                    botellasAceite = obj.optInt("botellasAceite", 2),
                    precioBotellaAceite = obj.optDouble("precioBotellaAceite", 25.0),
                    costoAceiteTotal = obj.optDouble("costoAceiteTotal", 50.0),
                    llantasUsadas = obj.optInt("llantasUsadas", 0),
                    precioLlanta = obj.optDouble("precioLlanta", 85.0),
                    costoLlantasTotal = obj.optDouble("costoLlantasTotal", 0.0),
                    costoTotal = obj.optDouble("costoTotal", 0.0),
                    motivoDetallado = obj.optString("motivoDetallado", ""),
                    componenteMasCostoso = obj.optString("componenteMasCostoso", "General"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }

        val unitExpenses = mutableListOf<UnitExpenseEntity>()
        val expensesArray = root.optJSONArray("unitExpenses") ?: JSONArray()
        for (i in 0 until expensesArray.length()) {
            val obj = expensesArray.getJSONObject(i)
            unitExpenses.add(
                UnitExpenseEntity(
                    unitPlaca = obj.optString("unitPlaca", ""),
                    motivo = obj.optString("motivo", ""),
                    monto = obj.optDouble("monto", 0.0),
                    fecha = obj.optString("fecha", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }

        val financeMovements = mutableListOf<FinanceMovementEntity>()
        val movementsArray = root.optJSONArray("financeMovements") ?: JSONArray()
        for (i in 0 until movementsArray.length()) {
            val obj = movementsArray.getJSONObject(i)
            financeMovements.add(
                FinanceMovementEntity(
                    concepto = obj.optString("concepto", ""),
                    monto = obj.optDouble("monto", 0.0),
                    esIngreso = obj.optBoolean("esIngreso", true),
                    categoria = obj.optString("categoria", "Otros"),
                    fecha = obj.optString("fecha", ""),
                    unitPlaca = obj.optString("unitPlaca").ifEmpty { null },
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }

        val inventoryItems = mutableListOf<InventoryItemEntity>()
        val invArray = root.optJSONArray("inventoryItems") ?: JSONArray()
        for (i in 0 until invArray.length()) {
            val obj = invArray.getJSONObject(i)
            inventoryItems.add(
                InventoryItemEntity(
                    nombre = obj.optString("nombre", ""),
                    categoria = obj.optString("categoria", ""),
                    stock = obj.optInt("stock", 0),
                    stockMinimo = obj.optInt("stockMinimo", 4),
                    unidadMedida = obj.optString("unidadMedida", "Unidades"),
                    precioUnitario = obj.optDouble("precioUnitario", 0.0),
                    descripcion = obj.optString("descripcion", "")
                )
            )
        }

        val maintenanceTasks = mutableListOf<MaintenanceTaskEntity>()
        val tasksArray = root.optJSONArray("maintenanceTasks") ?: JSONArray()
        for (i in 0 until tasksArray.length()) {
            val obj = tasksArray.getJSONObject(i)
            maintenanceTasks.add(
                MaintenanceTaskEntity(
                    unitPlaca = obj.optString("unitPlaca", ""),
                    motivo = obj.optString("motivo", ""),
                    urgencia = obj.optString("urgencia", "Normal"),
                    fechaProgramada = obj.optString("fechaProgramada", "Hoy"),
                    completado = obj.optBoolean("completado", false),
                    costo = obj.optDouble("costo", 0.0),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }

        return FleetBackupPayload(
            exportDate = exportDate,
            userEmail = userEmail,
            units = units,
            dailyPayments = dailyPayments,
            debtRecords = debtRecords,
            detailedMaintenances = detailedMaintenances,
            unitExpenses = unitExpenses,
            financeMovements = financeMovements,
            inventoryItems = inventoryItems,
            maintenanceTasks = maintenanceTasks
        )
    }

    fun saveLocalBackupFile(context: Context, jsonString: String): File {
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = context.filesDir
        val backupFile = File(dir, "motoflota_backup_$dateStamp.json")
        backupFile.writeText(jsonString)

        // También guardar en un archivo 'latest' para restauración rápida
        val latestFile = File(dir, "motoflota_backup_latest.json")
        latestFile.writeText(jsonString)

        return backupFile
    }

    fun getLatestLocalBackup(context: Context): String? {
        val latestFile = File(context.filesDir, "motoflota_backup_latest.json")
        return if (latestFile.exists()) latestFile.readText() else null
    }

    fun sendBackupViaEmail(
        context: Context,
        recipientEmail: String,
        jsonBackup: String,
        summary: String
    ) {
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val subject = "Copia de Seguridad MotoFlota Pro • $dateStr"
        val body = """
            Estimado Administrador,
            
            Adjuntamos el respaldo completo de la flota MotoFlota Pro.
            Fecha de generación: $dateStr
            Cuenta: $recipientEmail
            
            --- RESUMEN DE LA FLOTA ---
            $summary
            
            --- DATOS COMPLETOS DE RESPALDO (FORMATO JSON PARA RESTAURACIÓN) ---
            $jsonBackup
            
            Para restaurar estos datos en cualquier momento o teléfono:
            1. Abra MotoFlota Pro.
            2. Vaya a 'Copia de Seguridad en la Nube / Local'.
            3. Pegue este código JSON o seleccione Restaurar.
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$recipientEmail")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Enviar Respaldo por Correo"))
        } catch (_: Exception) {
            // Fallback con ACTION_SEND
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(fallback, "Compartir Respaldo"))
        }
    }

    fun shareBackupToGoogleDriveOrFiles(
        context: Context,
        jsonBackup: String,
        summary: String
    ) {
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Copia de Seguridad MotoFlota Pro • $dateStr")
            putExtra(Intent.EXTRA_TEXT, "--- RESUMEN MOTOFLOTA ---\n$summary\n\n--- ARCHIVO JSON DE RESPALDO ---\n$jsonBackup")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Guardar en Google Drive / Compartir"))
    }
}
