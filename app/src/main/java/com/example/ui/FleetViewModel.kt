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
import com.example.util.AlertMemory
import com.example.util.AppUser
import com.example.util.AutoBackup
import com.example.util.DriverMessages
import com.example.util.FleetDates
import com.example.util.MonthlyReport
import com.example.util.ShareActions
import com.example.util.FleetRules
import com.example.util.MonthlyRevision
import com.example.util.UserRole
import com.example.util.UserStore
import com.example.util.BackupHelper
import com.example.util.PinAuth
import com.example.util.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
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
    EDIT_PROFILE,
    EDIT_DAILY_QUOTA,
    EMERGENT_ALERTS,
    MANAGE_USERS,
    CHANGE_PIN,
    START_FROM_ZERO
}

private val ISO_DATE = Regex("^\\d{4}-\\d{2}-\\d{2}")

data class FleetUiState(
    // Todas las motos. La lista filtrada por la búsqueda de la pestaña Flota es filteredUnits.
    val units: List<FleetUnitEntity> = emptyList(),
    val filteredUnits: List<FleetUnitEntity> = emptyList(),
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
    val activeTab: Int = 0, // 0: Inicio, 1: Pagos, 2: Flota, 3: Caja (solo administrador), 4: Taller
    val isLoggedIn: Boolean = false,
    val activeDialog: DialogType? = null,
    val selectedInventoryItem: InventoryItemEntity? = null,
    val messageSnackbar: String? = null,
    val isDarkMode: Boolean = false,
    /** Claves ([UrgentAlert.hideKey]) de recordatorios quitados de Inicio (siguen en la campana). */
    val homeHiddenKeys: Set<String> = emptySet(),
    /** true cuando ya se leyó la base de datos (antes, la lista vacía no significa "todo resuelto"). */
    val dataLoaded: Boolean = false,
    val userEmail: String = "",
    val userName: String = "Administrador",
    val hasUsedHistory: Boolean = false,
    val lastCloudSync: String = "Aún no has enviado respaldos",
    val lastLocalBackup: String = "Aún no hay respaldo guardado en el teléfono",
    val notificationsEnabled: Boolean = true,
    val defaultCuotaDiaria: Double = 35.0,
    val isInteractiveTourActive: Boolean = false,
    val interactiveTourStep: Int = 0,
    val currentUserId: String? = null,
    val isAdmin: Boolean = true,
    val userRoleLabel: String = "Administrador",
    val users: List<AppUser> = emptyList()
) {
    val totalIngresos: Double = movements.filter { it.esIngreso }.sumOf { it.monto }
    val totalEgresos: Double = movements.filter { !it.esIngreso }.sumOf { it.monto }
    val totalBalance: Double = totalIngresos - totalEgresos

    // Egresos del mes en curso (antes la tarjeta decía "Egresos Mes" pero sumaba TODO el historial)
    // Se usa la FECHA del movimiento (un trabajo de taller registrado hoy con fecha del mes pasado
    // cuenta en el mes pasado); si la fecha no es año-mes-día se usa el momento en que se registró.
    val egresosMesActual: Double = run {
        val now = Calendar.getInstance()
        val mesActual = SimpleDateFormat("yyyy-MM", Locale.US).format(now.time)
        val c = Calendar.getInstance()
        movements.filter { mov ->
            if (mov.esIngreso) return@filter false
            if (ISO_DATE.containsMatchIn(mov.fecha)) mov.fecha.startsWith(mesActual)
            else {
                c.timeInMillis = mov.timestamp
                c.get(Calendar.YEAR) == now.get(Calendar.YEAR) && c.get(Calendar.MONTH) == now.get(Calendar.MONTH)
            }
        }.sumOf { it.monto }
    }

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

    // Recordatorios (las mismas reglas que usa el aviso diario en segundo plano). Son TODOS los
    // activos, de más urgente a menos: solo desaparecen cuando se resuelve el problema.
    // Los usan la campana, su contador y los avisos al celular.
    val urgentAlerts: List<UrgentAlert> get() {
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return AlertEngine.sortByUrgency(AlertEngine.build(
            units = units.map { u ->
                AlertUnit(
                    placa = u.placa,
                    nombre = u.nombreMoto,
                    estado = u.estado,
                    cliente = u.cliente,
                    soatDias = u.soatDiasRestantes,
                    soatFin = u.soatFin,
                    revisionPendiente = !u.revisionTecnicaMesRealizada,
                    finContrato = u.finContrato,
                    deuda = u.deudaTotal
                )
            },
            paidTodayPlacas = dailyPayments.filter { it.fecha == hoy && it.pagado }.map { it.unitPlaca }.toSet(),
            stockAceite = if (inventory.isNotEmpty()) stockAceiteBotellas else null,
            hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            stockLlantas = llantaItem?.stock,
            minimoLlantas = llantaItem?.stockMinimo?.coerceAtLeast(1) ?: AlertEngine.STOCK_LLANTAS_MINIMO
        ))
    }

    /** Recordatorios que se muestran en Inicio: todos menos los quitados de Inicio. */
    val homeAlerts: List<UrgentAlert> get() = urgentAlerts.filter { it.hideKey !in homeHiddenKeys }
}

class FleetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FleetRepository
    private val prefs = application.getSharedPreferences("moto_flota_prefs", Context.MODE_PRIVATE)

    init {
        val database = AppDatabase.getInstance(application)
        repository = FleetRepository(database.fleetDao(), database)
        // La guía inicial se activa al ingresar por primera vez (ver unlockSession).
    }

    private val _searchQuery = MutableStateFlow("")
    private val _filterStatus = MutableStateFlow("Todas")
    private val _activeTab = MutableStateFlow(0)
    private val _isLoggedIn = MutableStateFlow(false)
    private val _selectedUnit = MutableStateFlow<FleetUnitEntity?>(null)
    private val _activeDialog = MutableStateFlow<DialogType?>(null)
    private val _selectedInventoryItem = MutableStateFlow<InventoryItemEntity?>(null)
    private val _messageSnackbar = MutableStateFlow<String?>(null)
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    private val _homeHiddenKeys = MutableStateFlow(AlertMemory.homeHidden(prefs))

    /** true = alguien pidió abrir la campana (aviso tocado, línea de Inicio...). Ver [consumeOpenAlertsRequest]. */
    private val _openAlertsRequests = MutableStateFlow(false)
    val openAlertsRequests: StateFlow<Boolean> = _openAlertsRequests
    private val _currentUser = MutableStateFlow<AppUser?>(null)
    private val _users = MutableStateFlow(UserStore.users(prefs))

    // La copia automática (carpeta + correo) debe quedar lista antes de empezar a trabajar.
    private val _syncReady = MutableStateFlow(isSyncConfigured())
    val syncReady: StateFlow<Boolean> = _syncReady

    private fun isSyncConfigured(): Boolean {
        // Tras restaurar en otro teléfono la carpeta ya no es accesible: se vuelve a pedir.
        try {
            AutoBackup.dropFolderIfNoAccess(getApplication())
        } catch (_: Exception) {
        }
        return AutoBackup.hasFolder(getApplication()) && !(prefs.getString("user_email", "") ?: "").isBlank()
    }
    private val _selectedDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    private val _selectedMonth = MutableStateFlow(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    private val _userName = MutableStateFlow(prefs.getString("user_name", "Administrador") ?: "Administrador")
    private val _hasUsedHistory = MutableStateFlow(prefs.getBoolean("has_used_history", false))
    private val _lastCloudSync = MutableStateFlow(prefs.getString("last_cloud_sync", "Aún no has enviado respaldos") ?: "Aún no has enviado respaldos")
    private val _lastLocalBackup = MutableStateFlow(prefs.getString("last_local_backup", "Aún no hay respaldo guardado en el teléfono") ?: "Aún no hay respaldo guardado en el teléfono")
    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    private val _defaultCuotaDiaria = MutableStateFlow(prefs.getFloat("default_cuota_diaria", 35.0f).toDouble())
    private val _isInteractiveTourActive = MutableStateFlow(false)

    /** true = la guía muestra solo las novedades de esta versión (para quien ya hizo la guía). */
    private val _tourNewsOnly = MutableStateFlow(false)
    val tourNewsOnly: StateFlow<Boolean> = _tourNewsOnly

    /**
     * Pasos de la guía a mostrar: los que tienen `since` > este valor.
     * En la guía completa vale -1 (se ven todos); en modo novedades es la última versión vista.
     */
    private val _tourNewsSinceVersion = MutableStateFlow(-1)
    val tourNewsSinceVersion: StateFlow<Int> = _tourNewsSinceVersion
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
            _homeHiddenKeys
        ) { dialog, invItem, snackbar, isDark, hidden ->
            Tuple5(dialog, invItem, snackbar, isDark, hidden)
        },
        combine(
            combine(
                _userEmail,
                _userName,
                _currentUser,
                _users
            ) { email, ownerName, user, users ->
                Pair(email, Triple(user?.name ?: ownerName, user, users))
            },
            _hasUsedHistory,
            _lastCloudSync,
            _defaultCuotaDiaria,
            combine(_notificationsEnabled, _isInteractiveTourActive, _interactiveTourStep) { notif, isTour, tourStep ->
                Triple(notif, isTour, tourStep)
            }
        ) { (email, userInfo), usedHist, cloudSync, defaultCuota, (notifEnabled, isTour, tourStep) ->
            Tuple7(email, userInfo, usedHist, cloudSync, defaultCuota, notifEnabled, Pair(isTour, tourStep))
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
        val homeHidden = t4.v5

        val email = t5.v1
        val (name, currentUser, allUsers) = t5.v2
        val usedHist = t5.v3
        val cloudSync = t5.v4
        val defaultCuota = t5.v5
        val notifEnabled = t5.v6
        val tourInfo = t5.v7

        val filteredUnits = units.filter { unit ->
            // Se busca por moto, placa, chofer o teléfono
            val q = query.trim()
            val matchesQuery = q.isEmpty() ||
                    unit.nombreMoto.contains(q, ignoreCase = true) ||
                    unit.placa.contains(q.replace(" ", ""), ignoreCase = true) ||
                    unit.cliente.contains(q, ignoreCase = true) ||
                    (q.any { it.isDigit() } && unit.telefono.filter { it.isDigit() }.contains(q.filter { it.isDigit() }))
            val matchesFilter = when (filter) {
                "Todas" -> true
                "Vigente" -> unit.estado == "Vigente"
                "Por Vencer" -> unit.estado == "Por Vencer"
                "En Taller" -> unit.estado == "En Taller"
                "Inactivo" -> unit.estado == "Inactivo"
                else -> true
            }
            matchesQuery && matchesFilter
        }

        FleetUiState(
            units = units,
            filteredUnits = filteredUnits,
            movements = movements,
            inventory = inventory,
            inventoryBatches = batches,
            maintenanceTasks = maintenance,
            dailyPayments = dailyPayments,
            debtRecords = debts,
            detailedMaintenances = detailed,
            // La ficha abierta siempre muestra los datos actuales (deuda, estado, cuota...)
            selectedUnit = selectedUnit?.let { sel -> units.firstOrNull { it.id == sel.id } ?: sel },
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
            homeHiddenKeys = homeHidden,
            dataLoaded = true,
            userEmail = email,
            userName = name,
            hasUsedHistory = usedHist || units.isNotEmpty() || dailyPayments.isNotEmpty(),
            lastCloudSync = cloudSync,
            lastLocalBackup = _lastLocalBackup.value,
            notificationsEnabled = notifEnabled,
            defaultCuotaDiaria = defaultCuota,
            isInteractiveTourActive = tourInfo.first,
            interactiveTourStep = tourInfo.second,
            currentUserId = currentUser?.id,
            isAdmin = currentUser?.isAdmin ?: true,
            userRoleLabel = currentUser?.role?.label ?: "Administrador",
            users = allUsers
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FleetUiState()
    )

    // Trabajo automático mientras la app está abierta (sin botones):
    //  1) Olvidar los "quitados de Inicio" que ya se resolvieron.
    //  2) Avisar al celular los recordatorios del día, una sola vez cada uno (también los ocultos),
    //     y mantener al día el aviso fijo "N recordatorios pendientes".
    //  3) Guardar el respaldo en la carpeta de sincronización tras cada cambio de datos.
    @OptIn(FlowPreview::class)
    private fun startAutomaticWork() {
        viewModelScope.launch {
            uiState
                .filter { it.dataLoaded }
                .map { st -> st.urgentAlerts.map { it.hideKey } }
                .distinctUntilChanged()
                .collect { activeKeys -> pruneHomeHidden(activeKeys) }
        }
        viewModelScope.launch {
            uiState
                .filter { it.dataLoaded }
                .map { st -> if (st.isLoggedIn) st.urgentAlerts else emptyList() }
                .distinctUntilChanged()
                .collect { alerts -> notifyNewAlerts(alerts) }
        }
        viewModelScope.launch {
            uiState
                .filter { it.dataLoaded && it.isLoggedIn }
                .map { st -> st.notificationsEnabled to st.urgentAlerts.map { it.titulo } }
                .distinctUntilChanged()
                .collect { (enabled, titles) -> updateRemindersSummary(enabled, titles) }
        }
        viewModelScope.launch {
            uiState
                .map { st ->
                    listOf(st.units, st.dailyPayments, st.debtRecords, st.movements,
                        st.detailedMaintenances, st.inventory, st.maintenanceTasks).hashCode()
                }
                .distinctUntilChanged()
                .drop(1) // el estado inicial vacío, antes de leer la base de datos
                .debounce(15_000) // espera a que terminen de registrar varias cosas seguidas
                .collect { autoBackupToFolder() }
        }
    }

    private fun pruneHomeHidden(activeKeys: List<String>) {
        val current = _homeHiddenKeys.value
        val pruned = AlertMemory.prune(current, activeKeys)
        if (pruned != current) {
            _homeHiddenKeys.value = pruned
            AlertMemory.saveHomeHidden(prefs, pruned)
        }
    }

    /** Aviso fijo y silencioso con todos los recordatorios pendientes (se quita al llegar a 0). */
    private fun updateRemindersSummary(enabled: Boolean, titles: List<String>) {
        val app = getApplication<Application>()
        if (enabled) NotificationHelper.showRemindersSummary(app, titles)
        else NotificationHelper.cancelRemindersSummary(app)
    }

    /** Recibe TODOS los recordatorios activos (los ocultos en Inicio también avisan). */
    private fun notifyNewAlerts(alerts: List<UrgentAlert>) {
        if (alerts.isEmpty()) return
        val app = getApplication<Application>()
        val pendingIds = AlertMemory.pendingToNotify(
            today = AlertMemory.today(),
            storedDay = AlertMemory.today(),
            storedIds = AlertMemory.notifiedToday(prefs),
            currentIds = alerts.map { it.id }
        )
        if (pendingIds.isEmpty()) return
        val nuevas = alerts.filter { it.id in pendingIds }
        if (_notificationsEnabled.value) {
            val title = if (nuevas.size == 1) nuevas[0].titulo else "MotoFlota: ${nuevas.size} avisos nuevos"
            val message = if (nuevas.size == 1) nuevas[0].descripcion
            else nuevas.take(6).joinToString("\n") { "• ${it.titulo}" } + if (nuevas.size > 6) "\n… y ${nuevas.size - 6} más" else ""
            NotificationHelper.sendSystemAlertNotification(app, title, message, notificationId = 7100)
        }
        AlertMemory.markNotified(prefs, pendingIds)
    }

    private suspend fun autoBackupToFolder() {
        val app = getApplication<Application>()
        if (!AutoBackup.hasFolder(app)) return
        val payload = repository.getFullBackupPayload(_userEmail.value)
        // Nunca se guarda un respaldo vacío: en un teléfono nuevo pisaría el respaldo bueno de hoy.
        if (payload.units.isEmpty()) return
        // Si nada cambió desde la última copia, no se vuelve a escribir en Drive
        val huella = payload.copy(exportDate = "").hashCode()
        if (huella == lastBackupFingerprint) return
        val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { AutoBackup.writeBackup(app, BackupHelper.serializeToJson(payload)) }
        if (ok) lastBackupFingerprint = huella
    }

    /** Huella de los datos de la última copia guardada con éxito (solo en memoria). */
    @Volatile
    private var lastBackupFingerprint: Int? = null

    init {
        startAutomaticWork()
        // Al empezar un mes nuevo, la revisión técnica mensual vuelve a quedar pendiente.
        viewModelScope.launch {
            if (MonthlyRevision.checkAndMark(prefs)) repository.resetMonthlyRevision()
        }
    }

    fun markHistoryUsed() {
        if (!_hasUsedHistory.value) {
            _hasUsedHistory.value = true
            prefs.edit().putBoolean("has_used_history", true).apply()
        }
    }

    // --- USUARIOS Y ACCESO CON PIN (local, cada persona con su propio PIN) ---
    fun loginUsers(): List<AppUser> = UserStore.users(prefs)

    fun lastLoginUserId(): String? = UserStore.lastUserId(prefs)

    fun pinLockRemainingMs(userId: String): Long = UserStore.lockRemainingMillis(prefs, userId)

    fun verifyUserPin(userId: String, pin: String): PinAuth.Result {
        val result = UserStore.verify(prefs, userId, pin)
        if (result == PinAuth.Result.OK) startSession(userId)
        return result
    }

    /** Primer uso del teléfono: crea al dueño como Administrador y entra. */
    fun createFirstAdmin(name: String, email: String, pin: String): UserStore.AddResult {
        val (result, user) = UserStore.addUser(prefs, name, pin, UserRole.ADMIN)
        if (result != UserStore.AddResult.OK || user == null) return result
        _userName.value = user.name
        prefs.edit().putString("user_name", user.name).apply()
        if (email.isNotBlank()) {
            _userEmail.value = email
            prefs.edit().putString("user_email", email).apply()
        }
        startSession(user.id)
        return result
    }

    /**
     * Huella / bloqueo del teléfono. Solo se ofrece cuando hay UN usuario en el teléfono:
     * con varios usuarios, quien conoce el bloqueo del celular podría entrar como otro.
     */
    fun unlockWithDeviceCredential(userId: String) {
        if (UserStore.users(prefs).size != 1) return
        UserStore.markLastUser(prefs, userId)
        startSession(userId)
    }

    fun resetPinWithDeviceCredential(userId: String, newPin: String): Boolean {
        if (UserStore.users(prefs).size != 1) return false
        val ok = UserStore.changePin(prefs, userId, newPin)
        if (ok) {
            UserStore.markLastUser(prefs, userId)
            startSession(userId)
        }
        return ok
    }

    private fun startSession(userId: String) {
        _users.value = UserStore.users(prefs)
        _currentUser.value = _users.value.firstOrNull { it.id == userId }
        _activeTab.value = 0
        _isLoggedIn.value = true
        val hasDoneTour = prefs.getBoolean(TourNews.KEY_TOUR_DONE, false)
        val seenVersion = tourSeenVersion()
        if (!hasDoneTour) {
            // Primera vez: la guía completa
            _tourNewsOnly.value = false
            _tourNewsSinceVersion.value = -1
            _isInteractiveTourActive.value = true
            _interactiveTourStep.value = 0
        } else if (seenVersion < TourNews.CURRENT_VERSION) {
            // Ya conocía la app: tras actualizar se muestran solo las funciones nuevas, una vez
            _tourNewsOnly.value = true
            _tourNewsSinceVersion.value = seenVersion
            _isInteractiveTourActive.value = true
            _interactiveTourStep.value = 0
        }
    }

    fun logout() {
        _activeDialog.value = null
        _selectedUnit.value = null
        _isInteractiveTourActive.value = false
        _currentUser.value = null
        _activeTab.value = 0
        _isLoggedIn.value = false
    }

    private fun requireAdmin(): Boolean {
        if (_currentUser.value?.isAdmin == true) return true
        showSnackbar("Solo un administrador puede hacer esto.")
        return false
    }

    private fun refreshUsers() {
        _users.value = UserStore.users(prefs)
        _currentUser.value = _users.value.firstOrNull { it.id == _currentUser.value?.id }
    }

    private fun addResultMessage(result: UserStore.AddResult): String = when (result) {
        UserStore.AddResult.OK -> ""
        UserStore.AddResult.INVALID_NAME -> "Escribe un nombre (máximo 30 letras)."
        UserStore.AddResult.NAME_TAKEN -> "Ya existe un usuario con ese nombre."
        UserStore.AddResult.INVALID_PIN -> "El PIN debe tener entre ${PinAuth.MIN_LENGTH} y ${PinAuth.MAX_LENGTH} números."
    }

    /** Devuelve null si salió bien, o el mensaje de error para mostrar en el formulario. */
    fun addUser(name: String, pin: String, role: UserRole): String? {
        if (!requireAdmin()) return "Solo un administrador puede crear usuarios."
        val (result, user) = UserStore.addUser(prefs, name, pin, role)
        if (result != UserStore.AddResult.OK) return addResultMessage(result)
        refreshUsers()
        showSnackbar("Usuario \"${user?.name}\" creado como ${role.label}.")
        return null
    }

    fun resetUserPin(userId: String, newPin: String): String? {
        if (!requireAdmin()) return "Solo un administrador puede cambiar PIN de otros."
        if (!UserStore.changePin(prefs, userId, newPin)) return addResultMessage(UserStore.AddResult.INVALID_PIN)
        refreshUsers()
        showSnackbar("PIN actualizado.")
        return null
    }

    fun setUserRole(userId: String, role: UserRole) {
        if (!requireAdmin()) return
        when (UserStore.setRole(prefs, userId, role)) {
            UserStore.ChangeResult.OK -> showSnackbar("Rol cambiado a ${role.label}.")
            UserStore.ChangeResult.LAST_ADMIN -> showSnackbar("Debe quedar al menos un administrador.")
            UserStore.ChangeResult.NOT_FOUND -> Unit
        }
        refreshUsers()
    }

    fun removeUser(userId: String) {
        if (!requireAdmin()) return
        if (userId == _currentUser.value?.id) {
            showSnackbar("No puedes borrar tu propio usuario mientras lo usas.")
            return
        }
        when (UserStore.remove(prefs, userId)) {
            UserStore.ChangeResult.OK -> showSnackbar("Usuario eliminado.")
            UserStore.ChangeResult.LAST_ADMIN -> showSnackbar("Debe quedar al menos un administrador.")
            UserStore.ChangeResult.NOT_FOUND -> Unit
        }
        refreshUsers()
    }

    /** Cualquier usuario puede cambiar su propio PIN, confirmando el actual. */
    fun changeOwnPin(currentPin: String, newPin: String): String? {
        val me = _currentUser.value ?: return "Vuelve a ingresar."
        when (UserStore.verify(prefs, me.id, currentPin)) {
            PinAuth.Result.OK -> Unit
            PinAuth.Result.WRONG -> return "El PIN actual no es correcto."
            PinAuth.Result.LOCKED -> return "Demasiados intentos. Espera 30 segundos."
        }
        if (!UserStore.changePin(prefs, me.id, newPin)) return addResultMessage(UserStore.AddResult.INVALID_PIN)
        refreshUsers()
        showSnackbar("Tu PIN se cambió.")
        return null
    }

    /** Última versión de novedades vista, migrando la marca de la v1.4 la primera vez. */
    private fun tourSeenVersion(): Int {
        val stored = if (prefs.contains(TourNews.KEY_SEEN_VERSION)) prefs.getInt(TourNews.KEY_SEEN_VERSION, -1) else null
        val seen = TourNews.resolveSeenVersion(
            storedVersion = stored,
            v14Seen = prefs.getBoolean(TourNews.OLD_KEY_V14_SEEN, false),
            completedTour = prefs.getBoolean(TourNews.KEY_TOUR_DONE, false)
        )
        if (stored == null && seen >= 0) prefs.edit().putInt(TourNews.KEY_SEEN_VERSION, seen).apply()
        return seen
    }

    fun startInteractiveTour() {
        _tourNewsOnly.value = false
        _tourNewsSinceVersion.value = -1
        _isInteractiveTourActive.value = true
        _interactiveTourStep.value = 0
        _activeTab.value = 0
    }

    fun advanceTourStep(totalSteps: Int) {
        val next = _interactiveTourStep.value + 1
        if (next >= totalSteps) {
            completeInteractiveTour()
        } else {
            _interactiveTourStep.value = next
        }
    }

    /** Terminar o saltar la guía: no vuelve a aparecer sola (se puede repetir desde el menú). */
    fun completeInteractiveTour() {
        _isInteractiveTourActive.value = false
        _activeDialog.value = null
        prefs.edit()
            .putBoolean(TourNews.KEY_TOUR_DONE, true)
            .putBoolean(TourNews.OLD_KEY_V14_SEEN, true)
            .putInt(TourNews.KEY_SEEN_VERSION, TourNews.CURRENT_VERSION)
            .apply()
        _tourNewsOnly.value = false
        _tourNewsSinceVersion.value = -1
        _activeTab.value = 0
    }

    fun dismissTourGuide() {
        _isInteractiveTourActive.value = false
    }

    fun updateProfile(email: String, name: String) {
        val me = _currentUser.value
        if (me != null && name.isNotBlank() && name != me.name) {
            val result = UserStore.rename(prefs, me.id, name)
            if (result != UserStore.AddResult.OK) {
                showSnackbar(addResultMessage(result))
                return
            }
            refreshUsers()
        }
        if (me?.isAdmin != false) {
            _userEmail.value = email
            prefs.edit().putString("user_email", email).apply()
        }
        _userName.value = name
        prefs.edit().putString("user_name", name).apply()
        showSnackbar("Perfil actualizado.")
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        showSnackbar(if (enabled) "Notificaciones móviles activadas." else "Notificaciones móviles silenciadas.")
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
            prefs.edit()
                .putString("last_cloud_sync", _lastCloudSync.value)
                .putLong("last_email_backup_at", System.currentTimeMillis())
                .apply()
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

    // --- Carpeta de sincronización (p. ej. una carpeta dentro de Google Drive) ---
    fun syncFolderName(): String? = AutoBackup.folderName(getApplication())

    fun latestFolderBackupDate(): String? = AutoBackup.latestBackupDate(getApplication())

    /** Paso obligatorio del primer ingreso: correo + carpeta. Devuelve un error o null si todo salió bien. */
    fun completeSyncSetup(email: String): String? {
        val mail = email.trim()
        if (!(mail.contains("@") && mail.contains("."))) return "Escribe un correo válido."
        if (!AutoBackup.hasFolder(getApplication())) return "Toca «Guardar copia en Google Drive»."
        _userEmail.value = mail
        prefs.edit().putString("user_email", mail).apply()
        _syncReady.value = true
        viewModelScope.launch { autoBackupToFolder() }
        return null
    }

    /** Recupera datos desde un archivo .json elegido por el usuario (por ejemplo, el adjunto del correo). */
    fun restoreFromFile(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            val json = try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                }
            } catch (e: Exception) {
                null
            }
            if (json.isNullOrBlank()) {
                showSnackbar("No se pudo leer ese archivo.")
            } else {
                restoreFromJson(json)
            }
        }
    }

    /**
     * "Empezar de cero" para pruebas: borra motos, pagos, caja, taller, usuarios y ajustes de este
     * teléfono, y vuelve a la pantalla de bienvenida. NO borra las copias de la carpeta.
     * Pide el PIN del administrador para evitar borrados accidentales.
     */
    fun wipeEverything(pin: String): String? {
        val me = _currentUser.value ?: return "Vuelve a ingresar."
        if (!me.isAdmin) return "Solo un administrador puede hacer esto."
        when (UserStore.verify(prefs, me.id, pin)) {
            PinAuth.Result.OK -> Unit
            PinAuth.Result.WRONG -> return "PIN incorrecto."
            PinAuth.Result.LOCKED -> return "Demasiados intentos. Espera 30 segundos."
        }
        viewModelScope.launch {
            repository.clearAllData()
            val app = getApplication<Application>()
            androidx.work.WorkManager.getInstance(app).apply {
                cancelUniqueWork(com.example.util.BackupScheduler.WORK_MORNING)
                cancelUniqueWork(com.example.util.BackupScheduler.WORK_EVENING)
            }
            prefs.edit().clear().commit()
            _users.value = emptyList()
            _userEmail.value = ""
            _userName.value = "Administrador"
            _homeHiddenKeys.value = emptySet()
            NotificationHelper.cancelRemindersSummary(app)
            _isDarkMode.value = false
            _syncReady.value = false
            _hasUsedHistory.value = false
            logout()
        }
        return null
    }

    fun setSyncFolder(context: Context, treeUri: android.net.Uri) {
        AutoBackup.setFolder(context, treeUri)
        _syncReady.value = isSyncConfigured()
    }

    /** Archivo de copia en Google Drive (creado con "Guardar copia en Google Drive" o elegido si ya existía). */
    fun setSyncDocument(context: Context, docUri: android.net.Uri, isNewFile: Boolean) {
        AutoBackup.setDocument(context, docUri)
        _syncReady.value = isSyncConfigured()
        // Archivo nuevo: se llena de inmediato. Archivo que ya existía: NO se toca, porque puede
        // tener la copia que se quiere recuperar en un teléfono nuevo.
        if (isNewFile) viewModelScope.launch { autoBackupToFolder() }
    }

    fun clearSyncFolder(context: Context) {
        AutoBackup.clearFolder(context)
        _syncReady.value = isSyncConfigured()
    }

    fun backupToSyncFolderNow(context: Context) {
        viewModelScope.launch {
            val payload = repository.getFullBackupPayload(_userEmail.value)
            if (payload.units.isEmpty()) {
                showSnackbar("No hay motos registradas: no se guarda un respaldo vacío.")
                return@launch
            }
            val json = BackupHelper.serializeToJson(payload)
            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { AutoBackup.writeBackup(context, json) }
            showSnackbar(
                if (ok) "Respaldo guardado en \"${AutoBackup.folderName(context)}\"."
                else "No se pudo guardar en Drive. Revisa el internet o toca «Cambiar»."
            )
        }
    }

    fun restoreFromSyncFolder(context: Context) {
        viewModelScope.launch {
            val json = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { AutoBackup.readLatestBackup(context) }
            if (json != null) {
                restoreFromJson(json)
            } else {
                showSnackbar("No se encontró una copia en Drive.")
            }
        }
    }

    fun restoreFromJson(jsonString: String) {
        viewModelScope.launch {
            val payload = try {
                BackupHelper.parseFromJson(jsonString)
            } catch (e: Exception) {
                showSnackbar("Ese archivo no es una copia de MotoFlota.")
                return@launch
            }
            // Una copia sin motos borraría todo: se rechaza.
            if (payload.units.isEmpty()) {
                showSnackbar("La copia está vacía (sin motos). No se cambió nada.")
                return@launch
            }
            try {
                // Antes de reemplazar, se guarda lo que había (por si se eligió la copia equivocada).
                val actual = repository.getFullBackupPayload(_userEmail.value)
                if (actual.units.isNotEmpty()) {
                    BackupHelper.saveLocalBackupFile(getApplication(), BackupHelper.serializeToJson(actual))
                }
                repository.restoreFromBackup(payload)
                markHistoryUsed()
                showSnackbar("Datos recuperados: ${payload.units.size} motos.")
            } catch (e: Exception) {
                showSnackbar("No se pudo recuperar. Tus datos quedaron como estaban.")
            }
        }
    }

    // --- RECORDATORIOS: no se borran a mano, solo se quitan de Inicio ---

    /** Quita el recordatorio de Inicio. Sigue en la campana (y avisando) hasta que se resuelva. */
    fun hideAlertFromHome(alert: UrgentAlert) {
        if (alert.hideKey in _homeHiddenKeys.value) return
        _homeHiddenKeys.value = _homeHiddenKeys.value + alert.hideKey
        AlertMemory.saveHomeHidden(prefs, _homeHiddenKeys.value)
        showSnackbar("Sigue en la campana 🔔 hasta que lo resuelvas.")
    }

    /** Vuelve a mostrar en Inicio un recordatorio que se había quitado. */
    fun showAlertOnHome(alert: UrgentAlert) {
        if (alert.hideKey !in _homeHiddenKeys.value) return
        _homeHiddenKeys.value = _homeHiddenKeys.value - alert.hideKey
        AlertMemory.saveHomeHidden(prefs, _homeHiddenKeys.value)
    }

    /** Pide abrir la campana (p. ej. al tocar el aviso del celular). */
    fun requestOpenAlerts() {
        _openAlertsRequests.value = true
    }

    /** Quien abre la campana marca el pedido como atendido. */
    fun consumeOpenAlertsRequest() {
        _openAlertsRequests.value = false
    }

    fun clearAllDatabaseData() {
        viewModelScope.launch {
            repository.clearAllData()
            _homeHiddenKeys.value = emptySet()
            AlertMemory.saveHomeHidden(prefs, emptySet())
            _messageSnackbar.value = "Base de datos restaurada correctamente."
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
        prefs.edit().putBoolean("dark_mode", _isDarkMode.value).apply()
    }

    fun onTabSelected(tabIndex: Int) {
        // La pestaña Caja (3) es solo para administradores.
        if (tabIndex == 3 && _currentUser.value?.isAdmin == false) return
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
            _selectedMonth.value = FleetDates.today().substring(0, 7)
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
            _selectedMonth.value = FleetDates.today().substring(0, 7)
        }
    }

    fun openDialog(dialogType: DialogType, unit: FleetUnitEntity? = null, inventoryItem: InventoryItemEntity? = null) {
        if (unit != null) _selectedUnit.value = unit
        _selectedInventoryItem.value = inventoryItem
        _activeDialog.value = dialogType
    }

    // Ventanas abiertas desde una alerta: al cerrarlas se suelta la moto, para que la ficha
    // de Flota no se abra sola después.
    private var releaseUnitOnClose = false

    fun openDialogFromAlert(dialogType: DialogType, unit: FleetUnitEntity) {
        releaseUnitOnClose = _selectedUnit.value == null
        openDialog(dialogType, unit = unit)
    }

    fun closeDialog() {
        if (releaseUnitOnClose) {
            _selectedUnit.value = null
            releaseUnitOnClose = false
        }
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
        if (!loggedIn) logout() else _isLoggedIn.value = true
    }

    // --- CHECKLIST PAGOS DIARIOS (CUOTA EDITABLE S/ 35, S/ 40, etc.) ---
    fun updateDefaultCuotaDiaria(newCuota: Double) {
        if (newCuota <= 0.0 || newCuota > 1000.0) {
            showSnackbar("Escribe una cuota válida (por ejemplo 35).")
            return
        }
        viewModelScope.launch {
            prefs.edit().putFloat("default_cuota_diaria", newCuota.toFloat()).apply()
            _defaultCuotaDiaria.value = newCuota
            repository.updateDefaultCuotaDiaria(newCuota)
            showSnackbar("Cuota diaria por defecto actualizada a S/ ${"%.2f".format(newCuota)}")
            closeDialog()
        }
    }

    fun updateUnitCuota(unit: FleetUnitEntity, newCuota: Double) {
        if (newCuota <= 0.0 || newCuota > 1000.0) {
            showSnackbar("Escribe una cuota válida (por ejemplo 35).")
            return
        }
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
            // Solo las motos que trabajan pagan cuota: inactivas y en taller no se marcan como pagadas.
            // Las que ya pagaron ese día no se tocan (se respeta el monto que se cobró de verdad).
            val yaPagaron = uiState.value.dailyPayments
                .filter { it.fecha == date && it.pagado }
                .map { it.unitPlaca }
                .toSet()
            val currentUnits = uiState.value.units.filter {
                if (paid) FleetRules.debeCuota(it.estado) && it.placa !in yaPagaron else it.placa in yaPagaron
            }
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
        val actual = uiState.value.units.firstOrNull { it.id == unit.id } ?: unit
        if (actual.estado == "Inactivo") {
            showSnackbar("${actual.nombreMoto} está inactiva: no se le cargan deudas. Actívala primero.")
            return
        }
        viewModelScope.launch {
            if (monto <= 0.0) {
                showSnackbar("Escribe un monto mayor a 0.")
                return@launch
            }
            repository.addDebtCargo(unit, monto, motivo)
            showSnackbar("Deuda de S/ ${"%.2f".format(monto)} registrada para ${unit.nombreMoto}.")
            closeDialog()
        }
    }

    fun addDebtAbono(unit: FleetUnitEntity, monto: Double, motivo: String) {
        val actual = uiState.value.units.firstOrNull { it.id == unit.id } ?: unit
        if (monto <= 0.0) {
            showSnackbar("Escribe un monto mayor a 0.")
            return
        }
        if (monto > actual.deudaTotal + 0.009) {
            showSnackbar("El abono (S/ ${"%.2f".format(monto)}) es mayor que la deuda de ${actual.nombreMoto} (S/ ${"%.2f".format(actual.deudaTotal)}).")
            return
        }
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
        costoCablesCambio: Double = 0.0,
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
                costoCablesCambio = costoCablesCambio,
                costoBajadaMotorCompleto = costoBajadaMotorCompleto,
                costoBajadaMedioMotor = costoBajadaMedioMotor,
                costoOtros = costoOtros
            )
            if (dejarEnTaller && unit.estado != "En Taller") {
                // Solo cambia el estado: así no se pisa ningún otro dato de la moto
                repository.updateUnitEstado(unit.placa, "En Taller")
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
        revisionTecnicaRealizada: Boolean = false,
        nuevoNombre: String = unit.nombreMoto,
        nuevaPlaca: String = unit.placa
    ) {
        // Datos más recientes de la moto (la ventana pudo abrirse antes de un pago o abono)
        val fresh = uiState.value.units.firstOrNull { it.id == unit.id } ?: unit
        val otrasPlacas = uiState.value.units.filter { it.id != fresh.id }.map { it.placa }
        FleetRules.validateRename(nuevoNombre, nuevaPlaca, otrasPlacas)?.let { error ->
            showSnackbar(error)
            return
        }
        val nombreFinal = nuevoNombre.trim()
        val placaFinal = FleetRules.normalizePlaca(nuevaPlaca)
        // Fechas siempre guardadas como año-mes-día: si no, las alertas de SOAT y contrato no funcionan
        val fechas = listOf(inicioContrato, finContrato, soatInicio, soatFin).map { FleetDates.normalize(it) }
        if (fechas.any { it == null }) {
            showSnackbar("Revisa las fechas: escríbelas como 2026-12-31 o 31/12/2026.")
            return
        }
        val (inicioContrato, finContrato, soatInicio, soatFin) = fechas.map { it!! }
        if (finContrato <= inicioContrato) {
            showSnackbar("El fin del contrato debe ser después del inicio.")
            return
        }
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
                fresh.estado == "En Taller" || fresh.estado == "Inactivo" -> fresh.estado
                diasSoat <= 7 -> "Por Vencer"
                else -> "Vigente"
            }

            val updatedUnit = fresh.copy(
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
            // Nombre o placa nuevos: se arrastran a todo el historial de la moto
            if (nombreFinal != fresh.nombreMoto || placaFinal != fresh.placa) {
                repository.renameUnit(fresh.placa, nombreFinal, placaFinal)
            }
            // Pasó a Inactivo: la garantía pendiente deja de ser deuda
            val anulado = if (estadoActualizado == "Inactivo" && fresh.estado != "Inactivo") {
                repository.cancelPendingGuarantee(placaFinal)
            } else 0.0
            _selectedUnit.value = updatedUnit.copy(nombreMoto = nombreFinal, placa = placaFinal)
            showSnackbar(
                if (anulado > 0.0) "$nombreFinal inactiva. Garantía pendiente anulada (S/ ${"%.2f".format(anulado)})."
                else "Datos de $nombreFinal ($placaFinal) actualizados."
            )
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
        val placaNueva = FleetRules.normalizePlaca(placa)
        if (uiState.value.units.any { FleetRules.normalizePlaca(it.placa) == placaNueva }) {
            showSnackbar("Ya hay una moto con la placa $placaNueva.")
            return
        }
        // Fechas escritas a mano (31/12/2026) se guardan como 2026-12-31; vacías = valor por defecto
        val fechasNuevas = listOf(inicioContrato, finContrato, soatInicio, soatFin)
        if (fechasNuevas.any { it.isNotBlank() && FleetDates.normalize(it) == null }) {
            showSnackbar("Revisa las fechas: escríbelas como 2026-12-31 o 31/12/2026.")
            return
        }
        val inicioContrato = FleetDates.normalize(inicioContrato) ?: ""
        val finContrato = FleetDates.normalize(finContrato) ?: ""
        val soatInicio = FleetDates.normalize(soatInicio) ?: ""
        val soatFin = FleetDates.normalize(soatFin) ?: ""
        if (cuotaDiaria <= 0.0) {
            showSnackbar("La cuota diaria debe ser mayor a 0.")
            return
        }
        // Sin chofer asignado no hay garantía que cobrar
        val sinChofer = cliente.isBlank() || cliente.trim().equals("Sin asignar", ignoreCase = true)
        val deudaGarantia = if (sinChofer) 0.0 else deudaGarantia
        val garantiaInicial = if (sinChofer) 0.0 else garantiaInicial // tampoco entra dinero a Caja
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
                placa = placaNueva,
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

            showSnackbar(
                if (sinChofer) "${newUnit.nombreMoto} (${newUnit.placa}) registrada, sin chofer ni garantía."
                else "${newUnit.nombreMoto} (${newUnit.placa}) registrada. Garantía al día."
            )
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
        monto: Double
    ) {
        viewModelScope.launch {
            // Un solo registro: el pago del día (con el monto real) y su ingreso en caja.
            // Antes se sumaba dos veces el ingreso (cuota + "Pago Alquiler").
            val hoy = FleetDates.today()
            repository.toggleDailyPayment(unit, hoy, true, monto)
            showSnackbar("Pago de S/ ${"%.2f".format(monto)} registrado para ${unit.nombreMoto} (hoy).")
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

    /**
     * Renovar con una modalidad (1 Mes, 3 Meses...): empieza donde terminó el anterior (o hoy si ya venció)
     * y el fin se calcula solo. La modalidad elegida queda guardada en la moto.
     */
    fun renewContractWithModalidad(unit: FleetUnitEntity, tipoDuracion: String) {
        viewModelScope.launch {
            val fresh = repository.getUnitByPlaca(unit.placa) ?: unit
            val inicio = FleetDates.renewalStart(fresh.finContrato)
            val fin = FleetDates.contractEnd(inicio, tipoDuracion)
            if (fin == null || fin <= inicio) {
                showSnackbar("No se pudo calcular la fecha de fin.")
                return@launch
            }
            val updated = fresh.copy(
                progresoContrato = 0.0f,
                inicioContrato = inicio,
                finContrato = fin,
                tipoDuracion = tipoDuracion,
                estado = if (fresh.estado == "En Taller") "En Taller" else "Vigente"
            )
            repository.updateUnit(updated)
            _selectedUnit.value = updated
            showSnackbar("Contrato de ${fresh.nombreMoto} renovado: ${FleetDates.pretty(inicio)} → ${FleetDates.pretty(fin)}.")
        }
    }

    fun renewContract(unit: FleetUnitEntity, newDate: String? = null) {
        viewModelScope.launch {
            // Datos frescos de la base (la ficha abierta puede estar desactualizada)
            val fresh = repository.getUnitByPlaca(unit.placa) ?: unit
            val fin = if (newDate == null) {
                FleetDates.suggestedRenewalEnd(fresh.finContrato, fresh.tipoDuracion)
            } else {
                FleetDates.normalize(newDate)
            }
            if (fin == null) {
                showSnackbar("Fecha no válida. Escríbela así: 2026-12-31 o 31/12/2026.")
                return@launch
            }
            val inicio = FleetDates.renewalStart(fresh.finContrato)
            if (fin <= inicio) {
                showSnackbar("La nueva fecha de fin debe ser posterior al $inicio.")
                return@launch
            }
            val updated = fresh.copy(
                progresoContrato = 0.0f,
                inicioContrato = inicio,
                finContrato = fin,
                // Si está en el taller, sigue en el taller; si no, vuelve a trabajar.
                estado = if (fresh.estado == "En Taller") "En Taller" else "Vigente"
            )
            repository.updateUnit(updated)
            _selectedUnit.value = updated
            showSnackbar("Contrato de ${fresh.nombreMoto} renovado hasta el $fin.")
        }
    }

    // --- ELIMINAR UNA MOTO (registrada por error) ---
    fun loadUnitHistory(unit: FleetUnitEntity, onResult: (FleetRepository.UnitHistory) -> Unit) {
        viewModelScope.launch { onResult(repository.unitHistory(unit.placa)) }
    }

    /** Solo administrador. Antes de borrar se guarda una copia interna completa por seguridad. */
    fun deleteUnit(unit: FleetUnitEntity) {
        if (!requireAdmin()) return
        viewModelScope.launch {
            try {
                val antes = repository.getFullBackupPayload(_userEmail.value)
                if (antes.units.isNotEmpty()) {
                    BackupHelper.saveLocalBackupFile(getApplication(), BackupHelper.serializeToJson(antes))
                }
            } catch (_: Exception) {
            }
            repository.deleteUnitCompletely(unit.placa)
            _selectedUnit.value = null
            _activeDialog.value = null
            showSnackbar("${unit.nombreMoto} (${unit.placa}) eliminada.")
        }
    }

    // --- ORDEN DE LA LISTA DE FLOTA (se recuerda) ---
    private val _fleetSort = MutableStateFlow(
        runCatching { FleetRules.Sort.valueOf(prefs.getString("fleet_sort", "NOMBRE") ?: "NOMBRE") }
            .getOrDefault(FleetRules.Sort.NOMBRE)
    )
    val fleetSort: StateFlow<FleetRules.Sort> = _fleetSort

    fun setFleetSort(sort: FleetRules.Sort) {
        _fleetSort.value = sort
        prefs.edit().putString("fleet_sort", sort.name).apply()
    }

    fun breakContract(unit: FleetUnitEntity) {
        viewModelScope.launch {
            val fresh = uiState.value.units.firstOrNull { it.id == unit.id } ?: unit
            val updated = fresh.copy(cliente = "Sin asignar", telefono = "", estado = "Inactivo")
            repository.updateUnit(updated)
            val anulado = repository.cancelPendingGuarantee(updated.placa)
            _selectedUnit.value = updated.copy(
                deudaGarantia = 0.0,
                deudaTotal = (updated.deudaTotal - anulado).coerceAtLeast(0.0)
            )
            showSnackbar(
                if (anulado > 0.0) "Contrato finalizado. Garantía pendiente anulada (S/ ${"%.2f".format(anulado)})."
                else "Contrato finalizado para ${unit.nombreMoto}."
            )
        }
    }

    fun markUnitAsOperational(unit: FleetUnitEntity) {
        viewModelScope.launch {
            // "Vigente": si el SOAT vence en 7 días o menos se mostrará solo como "Por Vencer"
            repository.updateUnitEstado(unit.placa, "Vigente")
            showSnackbar("${unit.nombreMoto} lista y trabajando.")
        }
    }

    // ------------------------------------------------------------------
    // v1.4: corregir errores, hablar con el chofer y reportes
    // ------------------------------------------------------------------

    /** La app pasó a segundo plano: se guarda la copia de inmediato (no se espera los 15 segundos). */
    fun onAppBackground() {
        viewModelScope.launch {
            try {
                autoBackupToFolder()
            } catch (_: Exception) {
            }
        }
    }

    /** Anular un trabajo de taller registrado por error (solo administrador). */
    fun deleteMaintenanceRecord(record: DetailedMaintenanceEntity) {
        if (!requireAdmin()) return
        viewModelScope.launch {
            repository.deleteMaintenanceRecord(record)
            val devuelto = listOfNotNull(
                record.botellasAceite.takeIf { it > 0 }?.let { "$it bot. de aceite" },
                record.llantasUsadas.takeIf { it > 0 }?.let { "$it llanta(s)" }
            ).joinToString(" y ")
            showSnackbar(
                "Trabajo de ${record.nombreMoto} anulado (S/ ${"%.2f".format(record.costoTotal)} vuelve a Caja)" +
                    if (devuelto.isNotEmpty()) ". Regresan al almacén: $devuelto." else "."
            )
        }
    }

    /** Qué movimientos de Caja se pueden anular aquí y cuáles se corrigen en su propia pestaña. */
    fun movementFixHint(mov: FinanceMovementEntity): String? = when {
        mov.categoria.startsWith("Pago Diario") || mov.categoria.startsWith("Pago Adelantado") -> null
        mov.categoria == "Mantenimiento Moto" -> "Este gasto se anula desde Taller → Trabajos (así el aceite y las llantas vuelven al almacén)."
        mov.categoria == "Abono Garantía" -> "Este abono se anula desde Cobros → Deudas (así la deuda vuelve a subir)."
        mov.categoria == "Compra Aceites" || mov.categoria == "Compra Repuestos" ->
            "Es una compra del almacén: corrige el stock en Taller → Repuestos y luego anula este gasto aquí."
        else -> null
    }

    /** Anular un movimiento de Caja (solo administrador). Una cuota anulada también se desmarca del cobro diario. */
    fun deleteMovement(mov: FinanceMovementEntity) {
        if (!requireAdmin()) return
        if (mov.categoria == "Mantenimiento Moto" || mov.categoria == "Abono Garantía") {
            showSnackbar(movementFixHint(mov) ?: "Este movimiento se corrige en su pestaña.")
            return
        }
        viewModelScope.launch {
            repository.deleteMovement(mov)
            showSnackbar("Movimiento anulado: ${if (mov.esIngreso) "-" else "+"} S/ ${"%.2f".format(mov.monto)} en Caja.")
        }
    }

    /** Anular un cargo o abono de deuda registrado por error (solo administrador). */
    fun deleteDebtRecord(record: DebtRecordEntity) {
        if (!requireAdmin()) return
        viewModelScope.launch {
            repository.deleteDebtRecord(record)
            showSnackbar(
                when (record.tipo) {
                    "CARGO" -> "Cargo anulado: la deuda de ${record.nombreMoto} baja S/ ${"%.2f".format(record.monto)}."
                    "ABONO" -> "Abono anulado: la deuda de ${record.nombreMoto} sube S/ ${"%.2f".format(record.monto)} y sale de Caja."
                    else -> "Anulación deshecha: la garantía de ${record.nombreMoto} vuelve a ser deuda."
                }
            )
        }
    }

    /** Cobro directo con monto libre; lo que falte de la cuota puede pasar a deuda. */
    fun recordPaymentWithRest(unit: FleetUnitEntity, monto: Double, restoADeuda: Boolean) {
        if (monto <= 0.0) {
            showSnackbar("Escribe un monto mayor a 0.")
            return
        }
        val actual = uiState.value.units.firstOrNull { it.id == unit.id } ?: unit
        val pasarADeuda = restoADeuda && actual.estado != "Inactivo"
        viewModelScope.launch {
            val hoy = FleetDates.today()
            val falta = repository.recordPayment(actual, hoy, monto, pasarADeuda)
            showSnackbar(
                if (falta > 0.0) "Pago de S/ ${"%.2f".format(monto)} registrado. Faltan S/ ${"%.2f".format(falta)}: pasaron a la deuda de ${actual.nombreMoto}."
                else "Pago de S/ ${"%.2f".format(monto)} registrado para ${actual.nombreMoto} (hoy)."
            )
            closeDialog()
        }
    }

    /** Texto del estado de cuenta del mes en curso de una moto. */
    suspend fun buildStatement(unit: FleetUnitEntity): String {
        val fresh = uiState.value.units.firstOrNull { it.id == unit.id } ?: unit
        val hoy = FleetDates.today()
        val mes = hoy.substring(0, 7)
        val pagos = repository.paymentsOfMonth(fresh.placa, mes)
        val sinPago = if (FleetRules.debeCuota(fresh.estado)) {
            DriverMessages.unpaidDays(mes, hoy, fresh.inicioContrato, pagos.map { it.fecha }.toSet())
        } else 0
        return DriverMessages.statement(
            DriverMessages.Statement(
                moto = fresh.nombreMoto,
                placa = fresh.placa,
                chofer = fresh.cliente,
                mesLabel = monthLabel(mes),
                diasPagados = pagos.size,
                totalPagado = pagos.sumOf { it.cuotaMonto },
                diasSinPago = sinPago,
                deudaTotal = fresh.deudaTotal,
                garantiaPendiente = fresh.deudaGarantia,
                finContrato = fresh.finContrato
            )
        )
    }

    /** Estado de cuenta para mostrarlo en pantalla. */
    fun loadStatement(unit: FleetUnitEntity, onResult: (String) -> Unit) {
        viewModelScope.launch { onResult(buildStatement(unit)) }
    }

    /** Compartir el estado de cuenta (el teléfono deja elegir: correo, imprimir, guardar...). */
    fun shareStatement(context: Context, text: String) {
        if (!ShareActions.shareText(context, text, "Estado de cuenta")) showSnackbar("No se pudo compartir el estado de cuenta.")
    }

    private fun monthLabel(mes: String): String {
        val nombres = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
        val m = mes.substring(5, 7).toIntOrNull() ?: return mes
        return "${nombres.getOrElse(m - 1) { mes }} ${mes.substring(0, 4)}"
    }

    /** Reporte del mes ("2026-11") en CSV para abrir en Excel o mandarlo por correo. Solo administrador. */
    fun exportMonthlyReport(context: Context, mes: String) {
        if (!requireAdmin()) return
        viewModelScope.launch {
            val st = uiState.value
            val pagosMes = st.dailyPayments.filter { it.fecha.startsWith(mes) }
            val tallerMes = st.detailedMaintenances.filter { it.mesAno == mes }
            val movMes = st.movements.filter { it.fecha.startsWith(mes) }
            val unitRows = st.units.map { u ->
                val pagos = pagosMes.filter { it.unitPlaca == u.placa }
                MonthlyReport.UnitRow(
                    nombre = u.nombreMoto,
                    placa = u.placa,
                    chofer = u.cliente,
                    estado = u.estado,
                    diasPagados = pagos.size,
                    cobrado = pagos.sumOf { it.cuotaMonto },
                    gastoTaller = tallerMes.filter { it.unitPlaca == u.placa }.sumOf { it.costoTotal },
                    deuda = u.deudaTotal
                )
            }
            val movRows = movMes.map {
                MonthlyReport.MovementRow(it.fecha, it.esIngreso, it.categoria, it.concepto, it.unitPlaca, it.monto)
            }
            val csv = MonthlyReport.build(monthLabel(mes), unitRows, movRows)
            val ok = ShareActions.shareCsv(context, "MotoFlota_reporte_$mes.csv", csv)
            if (!ok) showSnackbar("No se pudo crear el reporte.")
        }
    }

    fun markUnitInWorkshop(unit: FleetUnitEntity) {
        viewModelScope.launch {
            repository.updateUnitEstado(unit.placa, "En Taller")
            showSnackbar("${unit.nombreMoto} entró al taller.")
        }
    }

}

// Helpers para combinación de StateFlows
data class Tuple4<A, B, C, D>(val v1: A, val v2: B, val v3: C, val v4: D)
data class Tuple5<A, B, C, D, E>(val v1: A, val v2: B, val v3: C, val v4: D, val v5: E)
data class Tuple6<A, B, C, D, E, F>(val v1: A, val v2: B, val v3: C, val v4: D, val v5: E, val v6: F)
data class Tuple7<A, B, C, D, E, F, G>(val v1: A, val v2: B, val v3: C, val v4: D, val v5: E, val v6: F, val v7: G)

/**
 * Novedades de la guía por versión. Cada paso de la guía tiene `since` (0 = original,
 * 14 = v1.4, 15 = v1.5); en modo novedades se muestran los pasos con since > versión vista.
 */
object TourNews {
    const val CURRENT_VERSION = 15
    const val KEY_SEEN_VERSION = "tour_news_seen_version"
    const val KEY_TOUR_DONE = "has_completed_interactive_tour_v2"
    const val OLD_KEY_V14_SEEN = "tour_news_v14_seen"

    /**
     * Versión de novedades ya vista. Función pura.
     * - Guardada → esa.
     * - Sin guardar: marca vieja de la v1.4 → 14; guía completa hecha → 0; nunca hizo la guía → -1.
     */
    fun resolveSeenVersion(storedVersion: Int?, v14Seen: Boolean, completedTour: Boolean): Int = when {
        storedVersion != null -> storedVersion
        v14Seen -> 14
        completedTour -> 0
        else -> -1
    }

    /** true si un paso con [since] se muestra; [sinceVersion] = -1 muestra todos. */
    fun isStepShown(since: Int, sinceVersion: Int): Boolean = since > sinceVersion
}
