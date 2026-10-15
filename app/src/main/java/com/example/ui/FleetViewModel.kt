package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.DailyPaymentEntity
import com.example.data.model.DebtRecordEntity
import com.example.data.model.DetailedMaintenanceEntity
import com.example.data.model.FinanceMovementEntity
import com.example.data.model.FleetUnitEntity
import com.example.data.model.InventoryBatchEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.MaintenanceTaskEntity
import com.example.data.model.UnitExpenseEntity
import com.example.data.repository.FleetRepository
import com.example.util.BackupHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class DialogType {
    ADD_UNIT,
    ADD_MOVEMENT,
    RECORD_PAYMENT,
    SCHEDULE_MAINTENANCE,
    RESTOCK_ITEM,
    ADD_UNIT_EXPENSE,
    ADD_DEBT_CARGO,
    ADD_DEBT_ABONO,
    REGISTER_DETAILED_MAINTENANCE,
    EDIT_CONTRACT_SOAT,
    BUY_OIL,
    BUY_TIRES,
    TUTORIAL,
    TOUR_GUIDE,
    BACKUP_CLOUD,
    GOOGLE_ACCOUNT_SWITCHER,
    EDIT_DAILY_QUOTA,
    EMERGENT_ALERTS
}

data class FleetUiState(
    val units: List<FleetUnitEntity> = emptyList(),
    val movements: List<FinanceMovementEntity> = emptyList(),
    val inventory: List<InventoryItemEntity> = emptyList(),
    val inventoryBatches: List<InventoryBatchEntity> = emptyList(),
    val maintenanceTasks: List<MaintenanceTaskEntity> = emptyList(),
    val dailyPayments: List<DailyPaymentEntity> = emptyList(),
    val debtRecords: List<DebtRecordEntity> = emptyList(),
    val detailedMaintenances: List<DetailedMaintenanceEntity> = emptyList(),
    val selectedUnit: FleetUnitEntity? = null,
    val selectedUnitExpenses: List<UnitExpenseEntity> = emptyList(),
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val selectedMonth: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
    val searchQuery: String = "",
    val filterStatus: String = "Todas",
    val activeTab: Int = 0, // 0: Inicio, 1: Pagos/Deudas, 2: Flota, 3: Taller, 4: Finanzas
    val isLoggedIn: Boolean = true,
    val activeDialog: DialogType? = null,
    val selectedInventoryItem: InventoryItemEntity? = null,
    val messageSnackbar: String? = null,
    val isDarkMode: Boolean = false,
    val dismissedAlertIds: Set<String> = emptySet(),
    val userEmail: String = "rodrigogrados414@gmail.com",
    val userName: String = "Rodrigo Grados",
    val hasUsedHistory: Boolean = false,
    val lastCloudSync: String = "Sincronizado hoy",
    val lastLocalBackup: String = "Respaldo local automático activo",
    val notificationsEnabled: Boolean = true,
    val defaultCuotaDiaria: Double = 35.0,
    val isInteractiveTourActive: Boolean = false,
    val interactiveTourStep: Int = 0
) {
    val totalIngresos: Double = movements.filter { it.esIngreso }.sumOf { it.monto }
    val totalEgresos: Double = movements.filter { !it.esIngreso }.sumOf { it.monto }
    val totalBalance: Double = totalIngresos - totalEgresos

    // Aceite en inventario
    val aceiteItem: InventoryItemEntity? = inventory.firstOrNull {
        it.categoria == "Fluidos" || it.nombre.contains("Aceite", ignoreCase = true)
    }
    val stockAceiteBotellas: Int = aceiteItem?.stock ?: 0
    val precioAceiteBotella: Double = aceiteItem?.precioUnitario ?: 25.0
    val valorTotalAceite: Double = stockAceiteBotellas * precioAceiteBotella

    // Llantas en inventario
    val llantaItem: InventoryItemEntity? = inventory.firstOrNull {
        it.categoria == "Neumáticos" || it.nombre.contains("Llanta", ignoreCase = true) || it.nombre.contains("Neumático", ignoreCase = true)
    }
    val stockLlantas: Int = llantaItem?.stock ?: 0
    val precioLlanta: Double = llantaItem?.precioUnitario ?: 85.0
    val valorTotalLlantas: Double = stockLlantas * precioLlanta

    // Deudas de la flota
    val totalDeudaPendiente: Double = units.sumOf { it.deudaTotal }

    // Pagos del día seleccionado
    val pagosDelDiaSeleccionado: List<DailyPaymentEntity> = dailyPayments.filter { it.fecha == selectedDate }
    val conteoPagadosHoy: Int = pagosDelDiaSeleccionado.size
    val totalCobradoHoy: Double = pagosDelDiaSeleccionado.sumOf { it.cuotaMonto }

    // Mantenimientos del mes seleccionado
    val mantenimientosDelMes: List<DetailedMaintenanceEntity> = detailedMaintenances.filter { it.mesAno == selectedMonth }
    val totalGastoMantenimientosMes: Double = mantenimientosDelMes.sumOf { it.costoTotal }
    val totalMantenimientoMes: Double get() = totalGastoMantenimientosMes
    val motoMantenimientoMasCaro: DetailedMaintenanceEntity? = mantenimientosDelMes.maxByOrNull { it.costoTotal }
    val motoMasCostosaMesTexto: String get() {
        val moto = motoMantenimientoMasCaro ?: return "Sin mantenimientos registrados este mes."
        return "${moto.nombreMoto} (${moto.unitPlaca}) - S/ ${"%.2f".format(moto.costoTotal)} (${moto.motivoDetallado})"
    }
    val gastosPorCategoria: Map<String, Double> get() {
        val egresos = movements.filter { !it.esIngreso }
        return if (egresos.isEmpty()) {
            mapOf("Taller" to 0.0)
        } else {
            egresos.groupBy { it.categoria }.mapValues { entry -> entry.value.sumOf { it.monto } }
        }
    }

    // Alertas operativas tipo notificaciones del teléfono (descartables para evitar desorden)
    val urgentAlerts: List<UrgentAlert> get() = buildList {
        units.forEach { unit ->
            if (unit.soatDiasRestantes <= 7) {
                val alertId = "soat_${unit.placa}"
                if (alertId !in dismissedAlertIds) {
                    add(
                        UrgentAlert(
                            id = alertId,
                            tipo = AlertType.SOAT,
                            titulo = "SOAT por vencer • ${unit.nombreMoto} (${unit.placa})",
                            descripcion = "Vence en ${unit.soatDiasRestantes} días (${unit.soatFin}). ¡Renovar urgente!",
                            unitPlaca = unit.placa,
                            hora = "Ahora"
                        )
                    )
                }
            }
            if (!unit.revisionTecnicaMesRealizada) {
                val alertId = "rev_${unit.placa}"
                if (alertId !in dismissedAlertIds) {
                    add(
                        UrgentAlert(
                            id = alertId,
                            tipo = AlertType.REVISION,
                            titulo = "Revisión Técnica del Mes • ${unit.nombreMoto}",
                            descripcion = "Revisión técnica o mantenimiento mensual pendiente este mes para ${unit.nombreMoto} (${unit.placa}).",
                            unitPlaca = unit.placa,
                            hora = "Este Mes"
                        )
                    )
                }
            }
            if (unit.progresoContrato >= 0.9f || unit.tipoDuracion.contains("Semana", ignoreCase = true)) {
                val alertId = "contrato_${unit.placa}"
                if (alertId !in dismissedAlertIds) {
                    add(
                        UrgentAlert(
                            id = alertId,
                            tipo = AlertType.CONTRATO,
                            titulo = "Contrato de ${unit.nombreMoto} (${unit.tipoDuracion})",
                            descripcion = "Vence el ${unit.finContrato} (${unit.placa}).",
                            unitPlaca = unit.placa,
                            hora = "Hoy"
                        )
                    )
                }
            }
            if (unit.deudaTotal > 0.0) {
                val alertId = "deuda_${unit.placa}"
                if (alertId !in dismissedAlertIds) {
                    add(
                        UrgentAlert(
                            id = alertId,
                            tipo = AlertType.DEUDA,
                            titulo = "Deuda/Garantía Pendiente • ${unit.nombreMoto}",
                            descripcion = "${unit.nombreMoto} (${unit.placa}) adeuda S/ ${"%.2f".format(unit.deudaTotal)} a la fecha.",
                            unitPlaca = unit.placa,
                            hora = "Pendiente"
                        )
                    )
                }
            }
        }
        if (inventory.isNotEmpty() && stockAceiteBotellas <= 10) {
            val alertId = "stock_aceite"
            if (alertId !in dismissedAlertIds) {
                add(
                    UrgentAlert(
                        id = alertId,
                        tipo = AlertType.STOCK,
                        titulo = "Stock de Aceite 4T: $stockAceiteBotellas botellas",
                        descripcion = "Se requieren 2 botellas por mantenimiento. Stock disponible: $stockAceiteBotellas.",
                        unitPlaca = null,
                        hora = "Taller"
                    )
                )
            }
        }
    }
}

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
    val hora: String = "Ahora"
)

class FleetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FleetRepository

    init {
        val dao = AppDatabase.getInstance(application).fleetDao()
        repository = FleetRepository(dao)
        viewModelScope.launch {
            repository.seedDefaultDataIfEmpty()
            val hasDoneTour = prefs.getBoolean("has_completed_interactive_tour_v2", false)
            if (!hasDoneTour) {
                _isInteractiveTourActive.value = true
                _interactiveTourStep.value = 0
            }
        }
    }

    private val prefs = application.getSharedPreferences("moto_flota_prefs", Context.MODE_PRIVATE)

    private val _searchQuery = MutableStateFlow("")
    private val _filterStatus = MutableStateFlow("Todas")
    private val _activeTab = MutableStateFlow(0)
    private val _isLoggedIn = MutableStateFlow(false)
    private val _selectedUnit = MutableStateFlow<FleetUnitEntity?>(null)
    private val _activeDialog = MutableStateFlow<DialogType?>(null)
    private val _selectedInventoryItem = MutableStateFlow<InventoryItemEntity?>(null)
    private val _messageSnackbar = MutableStateFlow<String?>(null)
    private val _isDarkMode = MutableStateFlow(false)
    private val _dismissedAlertIds = MutableStateFlow<Set<String>>(emptySet())
    private val _selectedDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    private val _selectedMonth = MutableStateFlow(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "rodrigogrados414@gmail.com") ?: "rodrigogrados414@gmail.com")
    private val _userName = MutableStateFlow(prefs.getString("user_name", "Rodrigo Grados") ?: "Rodrigo Grados")
    private val _hasUsedHistory = MutableStateFlow(prefs.getBoolean("has_used_history", false))
    private val _lastCloudSync = MutableStateFlow(prefs.getString("last_cloud_sync", "Sincronizado hoy") ?: "Sincronizado hoy")
    private val _lastLocalBackup = MutableStateFlow(prefs.getString("last_local_backup", "Respaldo local automático activo") ?: "Respaldo local automático activo")
    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    private val _defaultCuotaDiaria = MutableStateFlow(prefs.getFloat("default_cuota_diaria", 35.0f).toDouble())
    private val _isInteractiveTourActive = MutableStateFlow(false)
    private val _interactiveTourStep = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _unitExpenses = _selectedUnit.flatMapLatest { unit ->
        if (unit != null) {
            repository.getExpensesForUnit(unit.placa)
        } else {
            flowOf(emptyList())
        }
    }

    val uiState: StateFlow<FleetUiState> = combine(
        combine(
            repository.allUnits,
            repository.allMovements,
            repository.allInventory,
            repository.allMaintenanceTasks,
            repository.allDailyPayments
        ) { units, movements, inventory, maintenance, dailyPayments ->
            Tuple5(units, movements, inventory, maintenance, dailyPayments)
        },
        combine(
            combine(
                repository.allDebtRecords,
                repository.allDetailedMaintenances,
                _selectedUnit
            ) { debts, detailed, unit -> Triple(debts, detailed, unit) },
            _unitExpenses,
            _selectedDate,
            repository.allInventoryBatches
        ) { (debts, detailed, unit), expenses, date, batches ->
            Tuple6(debts, detailed, unit, expenses, date, batches)
        },
        combine(
            _selectedMonth,
            _searchQuery,
            _filterStatus,
            _activeTab,
            _isLoggedIn
        ) { month, query, filter, tab, loggedIn ->
            Tuple5(month, query, filter, tab, loggedIn)
        },
        combine(
            _activeDialog,
            _selectedInventoryItem,
            _messageSnackbar,
            _isDarkMode,
            _dismissedAlertIds
        ) { dialog, invItem, snackbar, isDark, dismissed ->
            Tuple5(dialog, invItem, snackbar, isDark, dismissed)
        },
        combine(
            combine(
                _userEmail,
                _userName
            ) { email, name -> Pair(email, name) },
            _hasUsedHistory,
            _lastCloudSync,
            _defaultCuotaDiaria,
            combine(_notificationsEnabled, _isInteractiveTourActive, _interactiveTourStep) { notif, isTour, tourStep ->
                Triple(notif, isTour, tourStep)
            }
        ) { (email, name), usedHist, cloudSync, defaultCuota, (notifEnabled, isTour, tourStep) ->
            Tuple7(email, name, usedHist, cloudSync, defaultCuota, notifEnabled, Pair(isTour, tourStep))
        }
    ) { t1, t2, t3, t4, t5 ->
        val units = t1.v1
        val movements = t1.v2
        val inventory = t1.v3
        val maintenance = t1.v4
        val dailyPayments = t1.v5

        val debts = t2.v1
        val detailed = t2.v2
        val selectedUnit = t2.v3
        val expenses = t2.v4
        val date = t2.v5
        val batches = t2.v6

        val month = t3.v1
        val query = t3.v2
        val filter = t3.v3
        val tab = t3.v4
        val loggedIn = t3.v5

        val dialog = t4.v1
        val invItem = t4.v2
        val snackbar = t4.v3
        val isDark = t4.v4
        val dismissed = t4.v5

        val email = t5.v1
        val name = t5.v2
        val usedHist = t5.v3
        val cloudSync = t5.v4
        val defaultCuota = t5.v5
        val notifEnabled = t5.v6
        val tourInfo = t5.v7

        val filteredUnits = units.filter { unit ->
            val matchesQuery = unit.nombreMoto.contains(query, ignoreCase = true) ||
                    unit.placa.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                "Todas" -> true
                "Vigente" -> unit.estado == "Vigente"
                "Por Vencer" -> unit.estado == "Por Vencer"
                "En Taller" -> unit.estado == "En Taller"
                else -> true
            }
            matchesQuery && matchesFilter
        }

        FleetUiState(
            units = filteredUnits,
            movements = movements,
            inventory = inventory,
            inventoryBatches = batches,
            maintenanceTasks = maintenance,
            dailyPayments = dailyPayments,
            debtRecords = debts,
            detailedMaintenances = detailed,
            selectedUnit = selectedUnit,
            selectedUnitExpenses = expenses,
            selectedDate = date,
            selectedMonth = month,
            searchQuery = query,
            filterStatus = filter,
            activeTab = tab,
            isLoggedIn = loggedIn,
            activeDialog = dialog,
            selectedInventoryItem = invItem,
            messageSnackbar = snackbar,
            isDarkMode = isDark,
            dismissedAlertIds = dismissed,
            userEmail = email,
            userName = name,
            hasUsedHistory = usedHist || units.isNotEmpty() || dailyPayments.isNotEmpty(),
            lastCloudSync = cloudSync,
            lastLocalBackup = _lastLocalBackup.value,
            notificationsEnabled = notifEnabled,
            defaultCuotaDiaria = defaultCuota,
            isInteractiveTourActive = tourInfo.first,
            interactiveTourStep = tourInfo.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FleetUiState()
    )

    fun markHistoryUsed() {
        if (!_hasUsedHistory.value) {
            _hasUsedHistory.value = true
            prefs.edit().putBoolean("has_used_history", true).apply()
        }
    }

    fun loginWithGoogle(email: String, name: String) {
        _userEmail.value = email
        _userName.value = name
        prefs.edit()
            .putString("user_email", email)
            .putString("user_name", name)
            .apply()
        _isLoggedIn.value = true
        showSnackbar("Sesión iniciada como $name ($email)")
        val hasDoneTour = prefs.getBoolean("has_completed_interactive_tour_v2", false)
        if (!hasDoneTour) {
            _isInteractiveTourActive.value = true
            _interactiveTourStep.value = 0
            _activeTab.value = 0
        }
    }

    fun startInteractiveTour() {
        _isInteractiveTourActive.value = true
        _interactiveTourStep.value = 0
        _activeTab.value = 0
    }

    fun advanceTourStep() {
        val next = _interactiveTourStep.value + 1
        if (next >= 7) {
            completeInteractiveTour()
        } else {
            _interactiveTourStep.value = next
        }
    }

    fun completeInteractiveTour() {
        _isInteractiveTourActive.value = false
        prefs.edit().putBoolean("has_completed_interactive_tour_v2", true).apply()
        _activeTab.value = 0
        showSnackbar("🎉 ¡Capacitación completada! Ya dominas la interconexión de MotoFlota Pro.")
    }

    fun dismissTourGuide() {
        _isInteractiveTourActive.value = false
    }

    fun switchGoogleAccount(email: String, name: String) {
        _userEmail.value = email
        _userName.value = name
        prefs.edit()
            .putString("user_email", email)
            .putString("user_name", name)
            .apply()
        showSnackbar("Cuenta activa vinculada a: $email")
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        showSnackbar(if (enabled) "Notificaciones móviles activadas." else "Notificaciones móviles silenciadas.")
    }

    fun testSystemNotification(context: Context) {
        val success = NotificationHelper.sendSystemAlertNotification(
            context = context,
            title = "⚠️ MotoFlota Pro: Alerta Operativa",
            message = "¡Notificación emergente y sonido funcionando! El sistema avisará de SOAT por vencer, cobros y taller."
        )
        if (success) {
            showSnackbar("🔔 Alerta emergente con sonido enviada al teléfono.")
        } else {
            showSnackbar("Se requiere conceder permisos de notificación en el móvil.")
        }
    }

    fun sendUrgentAlertNotification(context: Context, alert: UrgentAlert) {
        val success = NotificationHelper.sendSystemAlertNotification(
            context = context,
            title = alert.titulo,
            message = alert.descripcion
        )
        if (success) {
            showSnackbar("Alerta enviada a la barra de notificaciones del celular.")
        } else {
            showSnackbar("Active los permisos de notificación para recibir avisos en el celular.")
        }
    }

    fun sendAllUrgentAlertsNotification(context: Context) {
        val alerts = uiState.value.urgentAlerts
        if (alerts.isEmpty()) {
            showSnackbar("Todo en orden: no hay alertas críticas en la flota.")
            return
        }
        var count = 0
        alerts.forEachIndexed { idx, alert ->
            val sent = NotificationHelper.sendSystemAlertNotification(
                context = context,
                title = "🚨 AVISO EMERGENTE • ${alert.titulo}",
                message = alert.descripcion,
                notificationId = 3000 + idx
            )
            if (sent) count++
        }
        if (count > 0) {
            showSnackbar("Se enviaron $count alertas emergentes prioritarias al teléfono.")
        } else {
            showSnackbar("Active los permisos de notificación del móvil para avisos emergentes.")
        }
    }

    fun exportAndEmailBackup(context: Context) {
        viewModelScope.launch {
            val payload = repository.getFullBackupPayload(_userEmail.value)
            val json = BackupHelper.serializeToJson(payload)
            val summary = """
                • Motos registradas: ${payload.units.size}
                • Pagos diarios en registro: ${payload.dailyPayments.size}
                • Mantenimientos de taller: ${payload.detailedMaintenances.size}
                • Movimientos financieros: ${payload.financeMovements.size}
                • Repuestos en almacén: ${payload.inventoryItems.size}
            """.trimIndent()
            BackupHelper.sendBackupViaEmail(context, _userEmail.value, json, summary)
            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            _lastCloudSync.value = "Enviado a correo ($nowStr)"
            prefs.edit().putString("last_cloud_sync", _lastCloudSync.value).apply()
            showSnackbar("Abriendo correo para enviar respaldo a ${_userEmail.value}")
        }
    }

    fun exportAndShareBackup(context: Context) {
        viewModelScope.launch {
            val payload = repository.getFullBackupPayload(_userEmail.value)
            val json = BackupHelper.serializeToJson(payload)
            val summary = "Motos: ${payload.units.size} | Pagos: ${payload.dailyPayments.size} | Taller: ${payload.detailedMaintenances.size}"
            BackupHelper.shareBackupToGoogleDriveOrFiles(context, json, summary)
            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            _lastCloudSync.value = "Google Drive ($nowStr)"
            prefs.edit().putString("last_cloud_sync", _lastCloudSync.value).apply()
        }
    }

    fun saveLocalBackup(context: Context) {
        viewModelScope.launch {
            val payload = repository.getFullBackupPayload(_userEmail.value)
            val json = BackupHelper.serializeToJson(payload)
            val file = BackupHelper.saveLocalBackupFile(context, json)
            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            _lastLocalBackup.value = "Guardado local: ${file.name} ($nowStr)"
            prefs.edit().putString("last_local_backup", _lastLocalBackup.value).apply()
            showSnackbar("Copia guardada en memoria interna: ${file.name}")
        }
    }

    fun restoreFromLatestLocal(context: Context) {
        viewModelScope.launch {
            val json = BackupHelper.getLatestLocalBackup(context)
            if (json != null) {
                restoreFromJson(json)
            } else {
                showSnackbar("No se encontró ningún archivo de respaldo previo en el teléfono.")
            }
        }
    }

    fun restoreFromJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val payload = BackupHelper.parseFromJson(jsonString)
                repository.restoreFromBackup(payload)
                markHistoryUsed()
                showSnackbar("¡Restauración exitosa! Se cargaron ${payload.units.size} motos y su historial.")
            } catch (e: Exception) {
                showSnackbar("Error al restaurar respaldo: ${e.localizedMessage}")
            }
        }
    }

    fun syncToCloud() {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            _lastCloudSync.value = "Nube sincronizada ($nowStr)"
            prefs.edit().putString("last_cloud_sync", _lastCloudSync.value).apply()
            showSnackbar("¡Sincronización en la nube completada! Datos respaldados con éxito.")
        }
    }

    fun dismissAlert(id: String) {
        _dismissedAlertIds.value = _dismissedAlertIds.value + id
    }

    fun dismissAllAlerts(alertIds: List<String>) {
        _dismissedAlertIds.value = _dismissedAlertIds.value + alertIds.toSet()
    }

    fun clearAllDatabaseData() {
        viewModelScope.launch {
            repository.clearAllData()
            _dismissedAlertIds.value = emptySet()
            _messageSnackbar.value = "Base de datos restaurada correctamente."
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun onTabSelected(tabIndex: Int) {
        _activeTab.value = tabIndex
        if (tabIndex > 0) {
            markHistoryUsed()
        }
    }

    fun setActiveTab(tabIndex: Int) {
        onTabSelected(tabIndex)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterStatusChanged(status: String) {
        _filterStatus.value = status
    }

    fun selectUnit(unit: FleetUnitEntity?) {
        _selectedUnit.value = unit
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    fun previousMonth() {
        val current = _selectedMonth.value
        try {
            val parts = current.split("-")
            var yr = parts[0].toInt()
            var mo = parts[1].toInt() - 1
            if (mo < 1) {
                mo = 12
                yr -= 1
            }
            _selectedMonth.value = "%04d-%02d".format(yr, mo)
        } catch (e: Exception) {
            _selectedMonth.value = "2026-08"
        }
    }

    fun nextMonth() {
        val current = _selectedMonth.value
        try {
            val parts = current.split("-")
            var yr = parts[0].toInt()
            var mo = parts[1].toInt() + 1
            if (mo > 12) {
                mo = 1
                yr += 1
            }
            _selectedMonth.value = "%04d-%02d".format(yr, mo)
        } catch (e: Exception) {
            _selectedMonth.value = "2026-10"
        }
    }

    fun openDialog(dialogType: DialogType, unit: FleetUnitEntity? = null, inventoryItem: InventoryItemEntity? = null) {
        if (unit != null) _selectedUnit.value = unit
        _selectedInventoryItem.value = inventoryItem
        _activeDialog.value = dialogType
    }

    fun closeDialog() {
        _activeDialog.value = null
        _selectedInventoryItem.value = null
    }

    fun showSnackbar(message: String) {
        _messageSnackbar.value = message
    }

    fun dismissSnackbar() {
        _messageSnackbar.value = null
    }

    fun setLoggedIn(loggedIn: Boolean) {
        _isLoggedIn.value = loggedIn
    }

    // --- CHECKLIST PAGOS DIARIOS (CUOTA EDITABLE S/ 35, S/ 40, etc.) ---
    fun updateDefaultCuotaDiaria(newCuota: Double) {
        viewModelScope.launch {
            prefs.edit().putFloat("default_cuota_diaria", newCuota.toFloat()).apply()
            _defaultCuotaDiaria.value = newCuota
            repository.updateDefaultCuotaDiaria(newCuota)
            showSnackbar("Cuota diaria por defecto actualizada a S/ ${"%.2f".format(newCuota)}")
            closeDialog()
        }
    }

    fun updateUnitCuota(unit: FleetUnitEntity, newCuota: Double) {
        viewModelScope.launch {
            repository.updateUnitCuota(unit.id, newCuota)
            val updated = unit.copy(cuotaDiaria = newCuota)
            if (_selectedUnit.value?.id == unit.id) {
                _selectedUnit.value = updated
            }
            showSnackbar("Cuota diaria de ${unit.nombreMoto} actualizada a S/ ${"%.2f".format(newCuota)}")
        }
    }

    fun toggleDailyPayment(unit: FleetUnitEntity, isPaid: Boolean, customMonto: Double? = null) {
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.toggleDailyPayment(unit, date, isPaid, customMonto)
            val montoFinal = customMonto ?: unit.cuotaDiaria
            val action = if (isPaid) "marcado como PAGADO (S/ ${"%.2f".format(montoFinal)})" else "desmarcado"
            showSnackbar("${unit.nombreMoto} (${unit.placa}) $action para el $date.")
        }
    }

    fun markAllDailyPayments(paid: Boolean) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val currentUnits = uiState.value.units
            currentUnits.forEach { unit ->
                repository.toggleDailyPayment(unit, date, paid)
            }
            val text = if (paid) "Todas las unidades marcadas como Pagadas" else "Lista de cobros reiniciada"
            showSnackbar(text)
        }
    }

    // --- REVISIÓN TÉCNICA DEL MES ---
    fun toggleRevisionTecnicaMes(unit: FleetUnitEntity, realizada: Boolean) {
        viewModelScope.launch {
            repository.updateRevisionTecnicaMes(unit.placa, realizada)
            val updated = unit.copy(revisionTecnicaMesRealizada = realizada)
            if (_selectedUnit.value?.id == unit.id) {
                _selectedUnit.value = updated
            }
            val statusText = if (realizada) "marcada como AL DÍA (Realizada ✅)" else "marcada como PENDIENTE ⏳"
            showSnackbar("Revisión técnica de ${unit.nombreMoto} $statusText")
        }
    }

    // --- CONTROL DE DEUDAS Y GARANTÍAS ---
    fun addDebtCargo(unit: FleetUnitEntity, monto: Double, motivo: String) {
        viewModelScope.launch {
            repository.addDebtCargo(unit, monto, motivo)
            showSnackbar("Deuda de S/ ${"%.2f".format(monto)} registrada para ${unit.nombreMoto}.")
            closeDialog()
        }
    }

    fun addDebtAbono(unit: FleetUnitEntity, monto: Double, motivo: String) {
        viewModelScope.launch {
            repository.addDebtAbono(unit, monto, motivo)
            showSnackbar("Abono de S/ ${"%.2f".format(monto)} registrado. Deuda reducida.")
            closeDialog()
        }
    }

    // --- MANTENIMIENTOS DETALLADOS Y MODIFICACIONES MULTI-SERVICIO ---
    fun registerDetailedMaintenance(
        unit: FleetUnitEntity,
        tipoTrabajo: String,
        detallesDescripcion: String,
        costoTrabajo: Double,
        fecha: String,
        dejarEnTaller: Boolean = false,
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
    ) {
        viewModelScope.launch {
            val record = repository.registerDetailedMaintenance(
                unit = unit,
                tipoTrabajo = tipoTrabajo,
                detallesDescripcion = detallesDescripcion,
                costoTrabajo = costoTrabajo,
                fecha = fecha,
                botellasAceite = botellasAceite,
                precioBotellaAceite = precioBotellaAceite,
                llantasUsadas = llantasUsadas,
                precioLlantaCustom = precioLlantaCustom,
                costoGeneral = costoGeneral,
                costoFrenos = costoFrenos,
                costoLlantas = costoLlantas,
                costoCana = costoCana,
                costoPlanchado = costoPlanchado,
                costoBajadaMotorCompleto = costoBajadaMotorCompleto,
                costoBajadaMedioMotor = costoBajadaMedioMotor,
                costoOtros = costoOtros
            )
            if (dejarEnTaller && unit.estado != "En Taller") {
                val unitEnTaller = unit.copy(estado = "En Taller")
                repository.updateUnit(unitEnTaller)
                _selectedUnit.value = unitEnTaller
            }
            val statusMsg = if (dejarEnTaller) " (Unidad ingresada al Taller)" else ""
            val aceiteMsg = if (record.botellasAceite > 0) " (${record.botellasAceite} bot. aceite a S/ ${"%.2f".format(record.precioBotellaAceite)})" else ""
            val llantasMsg = if (record.llantasUsadas > 0) " (${record.llantasUsadas} llantas a S/ ${"%.2f".format(record.precioLlanta)})" else ""
            showSnackbar("Mantenimiento registrado para ${unit.nombreMoto}: Total S/ ${"%.2f".format(record.costoTotal)}$aceiteMsg$llantasMsg$statusMsg")
            closeDialog()
        }
    }

    // --- INVENTARIO DE ACEITE, LLANTAS Y REPUESTOS ---
    fun buyOil(botellas: Int, precioUnitario: Double) {
        viewModelScope.launch {
            repository.registerOilPurchase(botellas, precioUnitario)
            showSnackbar("Ingreso de $botellas botellas de aceite a S/ ${"%.2f".format(precioUnitario)} registrado.")
            closeDialog()
        }
    }

    fun buyTires(llantas: Int, precioUnitario: Double) {
        viewModelScope.launch {
            repository.registerTirePurchase(llantas, precioUnitario)
            showSnackbar("Ingreso de $llantas llantas a S/ ${"%.2f".format(precioUnitario)} registrado en taller.")
            closeDialog()
        }
    }

    fun updateStock(item: InventoryItemEntity, delta: Int) {
        viewModelScope.launch {
            val newStock = (item.stock + delta).coerceAtLeast(0)
            repository.updateStock(item.id, newStock)
        }
    }

    fun setStock(item: InventoryItemEntity, exactStock: Int) {
        viewModelScope.launch {
            repository.updateStock(item.id, exactStock.coerceAtLeast(0))
            showSnackbar("Stock de ${item.nombre} actualizado a $exactStock.")
            closeDialog()
        }
    }

    // --- CONTRATOS Y SOAT MODIFICABLES ---
    fun updateUnitContractAndSoat(
        unit: FleetUnitEntity,
        inicioContrato: String,
        finContrato: String,
        tipoDuracion: String,
        soatInicio: String,
        soatFin: String,
        chofer: String,
        telefono: String,
        cuotaDiaria: Double,
        nuevoEstado: String? = null,
        revisionTecnicaRealizada: Boolean = false
    ) {
        viewModelScope.launch {
            // Calcular días restantes de SOAT
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Calendar.getInstance().time
            val diasSoat = try {
                val endDate = sdf.parse(soatFin)
                val diff = (endDate?.time ?: today.time) - today.time
                TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt().coerceAtLeast(0)
            } catch (e: Exception) {
                15
            }

            val soatVenceStr = when {
                diasSoat <= 0 -> "Vencido"
                diasSoat == 1 -> "1 Día"
                diasSoat < 30 -> "$diasSoat Días"
                else -> "${diasSoat / 30} Meses"
            }

            val estadoActualizado = nuevoEstado ?: when {
                diasSoat <= 7 -> "Por Vencer"
                unit.estado == "En Taller" -> "En Taller"
                else -> "Vigente"
            }

            val updatedUnit = unit.copy(
                cliente = chofer.trim(),
                telefono = telefono.trim(),
                cuotaDiaria = cuotaDiaria,
                inicioContrato = inicioContrato,
                finContrato = finContrato,
                tipoDuracion = tipoDuracion,
                soatInicio = soatInicio,
                soatFin = soatFin,
                soatDiasRestantes = diasSoat,
                soatVence = soatVenceStr,
                estado = estadoActualizado,
                revisionTecnicaMesRealizada = revisionTecnicaRealizada
            )

            repository.updateUnit(updatedUnit)
            _selectedUnit.value = updatedUnit
            showSnackbar("Datos de ${unit.nombreMoto} (${unit.placa}) actualizados.")
            closeDialog()
        }
    }

    fun addUnit(
        nombreMoto: String,
        placa: String,
        cliente: String,
        telefono: String,
        cuotaDiaria: Double,
        garantiaTotal: Double = 200.0,
        garantiaInicial: Double,
        deudaGarantia: Double,
        inicioContrato: String,
        finContrato: String,
        tipoDuracion: String,
        soatInicio: String,
        soatFin: String,
        observaciones: String,
        revisionTecnicaRealizada: Boolean = true
    ) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Date()

            fun parseDate(dStr: String): Date? {
                val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd")
                for (fmt in formats) {
                    try {
                        val d = SimpleDateFormat(fmt, Locale.getDefault()).parse(dStr.trim())
                        if (d != null) return d
                    } catch (_: Exception) {}
                }
                return null
            }

            // Restricción: la fecha de fin de contrato no puede ser menor a 1 mes (mínimo 30 días)
            var finValido = finContrato.trim()
            try {
                val inicioDate = parseDate(inicioContrato.trim()) ?: today
                val finDate = parseDate(finValido) ?: Date(inicioDate.time + 30L * 24 * 3600 * 1000)
                val diffContrato = TimeUnit.MILLISECONDS.toDays(finDate.time - inicioDate.time)
                if (diffContrato < 30) {
                    // Forzar estricto mínimo de 1 mes (30 días)
                    val c = Calendar.getInstance().apply {
                        time = inicioDate
                        add(Calendar.DAY_OF_YEAR, 30)
                    }
                    finValido = sdf.format(c.time)
                }
            } catch (_: Exception) {
                val c = Calendar.getInstance().apply {
                    time = today
                    add(Calendar.DAY_OF_YEAR, 30)
                }
                finValido = sdf.format(c.time)
            }

            // Calcular días reales restantes de SOAT de forma segura (sin alertas espurias al registrar)
            val diasSoat = try {
                val finDate = parseDate(soatFin.trim())
                if (finDate != null) {
                    val diff = finDate.time - today.time
                    val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
                    if (days > 0) days else 365
                } else 365
            } catch (e: Exception) {
                365
            }

            val soatVenceStr = if (diasSoat > 30) "$diasSoat Días" else "$diasSoat Días (Por Vencer)"

            val newUnit = FleetUnitEntity(
                nombreMoto = nombreMoto.trim().ifEmpty { "Moto ${uiState.value.units.size + 1}" },
                placa = placa.uppercase().trim(),
                cliente = cliente.trim().ifEmpty { "Sin asignar" },
                telefono = telefono.trim(),
                estado = "Vigente",
                cuotaDiaria = cuotaDiaria,
                deudaGarantia = deudaGarantia,
                deudaTotal = 0.0, // Inicial en 0.0 para que addDebtCargo registre el cargo exacto sin duplicar
                progresoContrato = 0.05f,
                inicioContrato = inicioContrato.trim().ifEmpty { sdf.format(today) },
                finContrato = finValido,
                tipoDuracion = tipoDuracion.trim().ifEmpty { "1 Mes" },
                soatInicio = soatInicio.trim().ifEmpty { sdf.format(today) },
                soatFin = soatFin.trim().ifEmpty {
                    val c = Calendar.getInstance().apply { add(Calendar.YEAR, 1) }
                    sdf.format(c.time)
                },
                soatVence = soatVenceStr,
                soatDiasRestantes = diasSoat,
                revVence = if (revisionTecnicaRealizada) "Al día este mes" else "Pendiente",
                revDiasRestantes = if (revisionTecnicaRealizada) 180 else 0,
                revisionTecnicaMesRealizada = revisionTecnicaRealizada, // Al día al ingresar
                kilometraje = 0,
                observaciones = observaciones
            )
            repository.insertUnit(newUnit)

            // Si dejó garantía inicial en efectivo/transferencia, entra a Caja Disponible
            if (garantiaInicial > 0.0) {
                repository.insertMovement(
                    FinanceMovementEntity(
                        concepto = "Garantía inicial cobrada - ${newUnit.nombreMoto} (${newUnit.placa})",
                        monto = garantiaInicial,
                        esIngreso = true,
                        categoria = "Garantía",
                        fecha = sdf.format(today),
                        unitPlaca = newUnit.placa
                    )
                )
            }

            // Si debe saldo de garantía, se registra el cargo de deuda exacto
            if (deudaGarantia > 0.0) {
                repository.addDebtCargo(
                    newUnit,
                    deudaGarantia,
                    "Saldo garantía por pagar (S/ ${"%.2f".format(garantiaInicial)} pagado de S/ ${"%.2f".format(garantiaTotal)})"
                )
            }

            showSnackbar("${newUnit.nombreMoto} (${newUnit.placa}) registrada exitosamente. Garantía inicial en caja y saldo de deuda al día.")
            closeDialog()
        }
    }

    fun registerMovement(
        concepto: String,
        monto: Double,
        esIngreso: Boolean,
        categoria: String,
        unitPlaca: String?
    ) {
        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            AppDatabase.getInstance(getApplication()).fleetDao().insertMovement(
                FinanceMovementEntity(
                    concepto = concepto,
                    monto = monto,
                    esIngreso = esIngreso,
                    categoria = categoria,
                    fecha = now,
                    unitPlaca = unitPlaca
                )
            )
            showSnackbar("Movimiento registrado: S/ ${"%.2f".format(monto)}")
            closeDialog()
        }
    }

    fun scheduleMaintenance(
        unitPlaca: String,
        motivo: String,
        urgencia: String,
        fecha: String,
        costo: Double
    ) {
        viewModelScope.launch {
            repository.scheduleMaintenance(unitPlaca, motivo, urgencia, fecha, costo)
            showSnackbar("Mantenimiento programado para $unitPlaca.")
            closeDialog()
        }
    }

    fun toggleMaintenance(task: MaintenanceTaskEntity) {
        viewModelScope.launch {
            repository.toggleMaintenanceCompletion(task)
            val estado = if (!task.completado) "completado" else "pendiente"
            showSnackbar("Mantenimiento de ${task.unitPlaca} marcado como $estado.")
        }
    }

    fun recordRentalPayment(
        unit: FleetUnitEntity,
        monto: Double,
        fecha: String = "Hoy",
        metodo: String = "Efectivo"
    ) {
        viewModelScope.launch {
            toggleDailyPayment(unit, true)
            registerMovement(
                concepto = "Pago Alquiler ${unit.nombreMoto} ($metodo)",
                monto = monto,
                esIngreso = true,
                categoria = "Pago Diario S/35",
                unitPlaca = unit.placa
            )
            showSnackbar("Pago de S/ ${"%.2f".format(monto)} registrado para ${unit.nombreMoto}.")
            closeDialog()
        }
    }

    fun addUnitExpense(
        unit: FleetUnitEntity,
        motivo: String,
        monto: Double,
        categoria: String = "Mantenimiento Moto"
    ) {
        viewModelScope.launch {
            registerMovement(
                concepto = "Gasto en ${unit.nombreMoto}: $motivo",
                monto = monto,
                esIngreso = false,
                categoria = categoria,
                unitPlaca = unit.placa
            )
            showSnackbar("Gasto de S/ ${"%.2f".format(monto)} registrado para ${unit.nombreMoto}.")
            closeDialog()
        }
    }

    fun renewContract(unit: FleetUnitEntity, newDate: String? = null) {
        viewModelScope.launch {
            val updated = unit.copy(
                progresoContrato = 0.0f,
                estado = "Vigente",
                finContrato = newDate ?: unit.finContrato
            )
            repository.updateUnit(updated)
            _selectedUnit.value = updated
            showSnackbar("Contrato de ${unit.nombreMoto} renovado.")
        }
    }

    fun breakContract(unit: FleetUnitEntity) {
        viewModelScope.launch {
            val updated = unit.copy(cliente = "Sin asignar", telefono = "", estado = "Inactivo")
            repository.updateUnit(updated)
            _selectedUnit.value = updated
            showSnackbar("Contrato finalizado para ${unit.nombreMoto}.")
        }
    }

    fun markUnitAsOperational(unit: FleetUnitEntity) {
        viewModelScope.launch {
            val nuevoEstado = if (unit.soatDiasRestantes <= 7) "Por Vencer" else "Vigente"
            val updated = unit.copy(estado = nuevoEstado)
            repository.updateUnit(updated)
            _selectedUnit.value = updated
            showSnackbar("${unit.nombreMoto} (${unit.placa}) habilitada: ¡Operativa y lista para circular!")
        }
    }

    fun markUnitInWorkshop(unit: FleetUnitEntity) {
        viewModelScope.launch {
            val updated = unit.copy(estado = "En Taller")
            repository.updateUnit(updated)
            _selectedUnit.value = updated
            showSnackbar("${unit.nombreMoto} ingresada al Taller de Mantenimiento.")
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            repository.seedDefaultDataIfEmpty()
            showSnackbar("Datos de demostración listos.")
        }
    }
}

// Helpers para combinación de StateFlows
data class Tuple4<A, B, C, D>(val v1: A, val v2: B, val v3: C, val v4: D)
data class Tuple5<A, B, C, D, E>(val v1: A, val v2: B, val v3: C, val v4: D, val v5: E)
data class Tuple6<A, B, C, D, E, F>(val v1: A, val v2: B, val v3: C, val v4: D, val v5: E, val v6: F)
data class Tuple7<A, B, C, D, E, F, G>(val v1: A, val v2: B, val v3: C, val v4: D, val v5: E, val v6: F, val v7: G)

