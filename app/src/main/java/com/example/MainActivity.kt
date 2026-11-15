package com.example

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RestartAlt
import com.example.ui.screens.SyncSetupScreen
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BuildCircle
import androidx.compose.material.icons.outlined.ElectricRickshaw
import androidx.compose.material.icons.outlined.PriceCheck
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.CloudDone
import com.example.ui.DialogType
import com.example.ui.theme.SuccessGreen
import com.example.ui.screens.DailyPaymentsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FinancesScreen
import com.example.ui.screens.FleetDialogHost
import com.example.ui.screens.FleetScreen
import com.example.ui.screens.InteractiveTourWalkthrough
import com.example.ui.screens.tourTarget
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.WorkshopScreen
import androidx.compose.material.icons.outlined.HelpOutline
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.MyApplicationTheme
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SurfaceBorder
import com.example.util.BackupScheduler

class MainActivity : FragmentActivity() {
    private val viewModel: FleetViewModel by viewModels()

    // Si el teléfono queda fuera de la app más de 5 minutos, se cierra la sesión y pide el PIN.
    private var backgroundSince = 0L

    override fun onStop() {
        super.onStop()
        backgroundSince = android.os.SystemClock.elapsedRealtime()
        // Al salir de la app se guarda la copia en Drive de inmediato
        viewModel.onAppBackground()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundSince > 0L && android.os.SystemClock.elapsedRealtime() - backgroundSince > AUTO_LOCK_MS) {
            viewModel.logout()
        }
        backgroundSince = 0L
    }

    private companion object {
        const val AUTO_LOCK_MS = 5 * 60 * 1000L
        const val EXTRA_OPEN_ALERTS = "open_alerts"
    }

    /**
     * La notificación fija de recordatorios abre la app con la campana abierta.
     * Solo se deja pedido: la campana se abre cuando ya se ingresó con el PIN (MainAppScaffold).
     */
    private fun handleOpenAlertsIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_ALERTS, false) == true) {
            intent.removeExtra(EXTRA_OPEN_ALERTS)
            viewModel.requestOpenAlerts()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenAlertsIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleOpenAlertsIntent(intent)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val syncReady by viewModel.syncReady.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = state.isDarkMode) {
                val context = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* Si no la concede, simplemente no recibirá avisos; la app sigue funcionando. */ }

                // Se activa solo una vez por sesión, al ingresar: pide el permiso de notificaciones
                // (Android 13+) y programa los avisos diarios de las 8:00 y 19:30 junto con el
                // respaldo automático de la tarde. enqueueUniquePeriodicWork evita duplicarlos.
                LaunchedEffect(state.isLoggedIn) {
                    if (state.isLoggedIn) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        BackupScheduler.schedule(context)
                    }
                }

                LaunchedEffect(state.messageSnackbar) {
                    state.messageSnackbar?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.dismissSnackbar()
                    }
                }

                if (!state.isLoggedIn) {
                    LoginScreen(
                        users = remember { viewModel.loginUsers() },
                        lastUserId = remember { viewModel.lastLoginUserId() },
                        onCreateFirstAdmin = { name, email, pin -> viewModel.createFirstAdmin(name, email, pin) },
                        onVerifyPin = { id, pin -> viewModel.verifyUserPin(id, pin) },
                        lockRemainingMs = { id -> viewModel.pinLockRemainingMs(id) },
                        onDeviceUnlock = { id -> viewModel.unlockWithDeviceCredential(id) },
                        onResetPinWithDevice = { id, pin -> viewModel.resetPinWithDeviceCredential(id, pin) }
                    )
                } else if (state.isAdmin && !syncReady) {
                    // Paso obligatorio: correo + carpeta de copias antes de empezar
                    SyncSetupScreen(state = state, viewModel = viewModel)
                } else {
                    MainAppScaffold(
                        state = state,
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}

private data class NavTab(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    state: FleetUiState,
    viewModel: FleetViewModel,
    snackbarHostState: SnackbarHostState
) {
    // Títulos cortos: el nombre largo de cada sección no cabía en la barra superior.
    val tabs = buildList {
        add(NavTab(0, "Inicio", Icons.Default.SpaceDashboard, Icons.Outlined.SpaceDashboard))
        add(NavTab(1, "Pagos", Icons.Default.PriceCheck, Icons.Outlined.PriceCheck))
        add(NavTab(2, "Flota", Icons.Default.ElectricRickshaw, Icons.Outlined.ElectricRickshaw))
        if (state.isAdmin) add(NavTab(3, "Caja", Icons.Default.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet))
        add(NavTab(4, "Taller", Icons.Default.BuildCircle, Icons.Outlined.BuildCircle))
    }
    // Si un cobrador quedara en la pestaña Caja, se muestra Inicio.
    val visibleTab = if (!state.isAdmin && state.activeTab == 3) 0 else state.activeTab
    var menuOpen by remember { mutableStateOf(false) }

    // Pedidos de abrir la campana (línea de Inicio, "Ver todas" o la notificación fija).
    // Este Scaffold solo existe tras ingresar con el PIN, así que nunca se salta el bloqueo.
    val openAlertsRequested by viewModel.openAlertsRequests.collectAsStateWithLifecycle()
    LaunchedEffect(openAlertsRequested) {
        if (openAlertsRequested) {
            viewModel.consumeOpenAlertsRequest()
            viewModel.openDialog(DialogType.EMERGENT_ALERTS)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tabs.firstOrNull { it.index == visibleTab }?.label ?: "MotoFlota",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    // Todos los recordatorios pendientes, también los quitados de Inicio
                    val urgentCount = state.urgentAlerts.size
                    IconButton(
                        onClick = { viewModel.openDialog(DialogType.EMERGENT_ALERTS) },
                        modifier = Modifier.tourTarget("bell").testTag("urgent_alerts_bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (urgentCount > 0) {
                                    Badge(containerColor = DangerRed, contentColor = Color.White) {
                                        Text("$urgentCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (urgentCount > 0) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                                contentDescription = "Recordatorios pendientes",
                                tint = if (urgentCount > 0) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Un solo botón con la inicial del usuario abre el menú (antes eran 4 íconos sueltos)
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.tourTarget("menu").testTag("user_menu_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(NavyPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.userName.take(1).uppercase(),
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(state.userName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    state.userRoleLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider()
                            if (state.isAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Usuarios") },
                                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                                    onClick = { menuOpen = false; viewModel.openDialog(DialogType.MANAGE_USERS) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Respaldos y sincronización") },
                                    leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null) },
                                    onClick = { menuOpen = false; viewModel.openDialog(DialogType.BACKUP_CLOUD) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Mi perfil") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    onClick = { menuOpen = false; viewModel.openDialog(DialogType.EDIT_PROFILE) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Cambiar mi PIN") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                onClick = { menuOpen = false; viewModel.openDialog(DialogType.CHANGE_PIN) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (state.notificationsEnabled) "Silenciar avisos" else "Activar avisos") },
                                leadingIcon = {
                                    Icon(
                                        if (state.notificationsEnabled) Icons.Outlined.Notifications else Icons.Default.NotificationsActive,
                                        contentDescription = null
                                    )
                                },
                                onClick = { menuOpen = false; viewModel.setNotificationsEnabled(!state.notificationsEnabled) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (state.isDarkMode) "Modo claro" else "Modo oscuro") },
                                leadingIcon = {
                                    Icon(if (state.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = null)
                                },
                                onClick = { menuOpen = false; viewModel.toggleDarkMode() }
                            )
                            if (state.isAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Empezar de cero") },
                                    leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
                                    onClick = { menuOpen = false; viewModel.openDialog(DialogType.START_FROM_ZERO) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Guía de uso") },
                                leadingIcon = { Icon(Icons.Outlined.HelpOutline, contentDescription = null) },
                                onClick = { menuOpen = false; viewModel.startInteractiveTour() }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Cerrar sesión") },
                                leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                                onClick = { menuOpen = false; viewModel.logout() },
                                modifier = Modifier.testTag("logout_button")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                tabs.forEach { tab ->
                    val selected = visibleTab == tab.index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.onTabSelected(tab.index) },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.tourTarget("nav_${tab.index}"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = visibleTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_switch"
            ) { tab ->
                when (tab) {
                    0 -> DashboardScreen(state = state, viewModel = viewModel)
                    1 -> DailyPaymentsScreen(state = state, viewModel = viewModel)
                    2 -> FleetScreen(state = state, viewModel = viewModel)
                    3 -> FinancesScreen(state = state, viewModel = viewModel)
                    4 -> WorkshopScreen(state = state, viewModel = viewModel)
                }
            }

            FleetDialogHost(state = state, viewModel = viewModel)
        }
    }

    // La guía se dibuja encima de TODA la pantalla (barras incluidas) para señalar cualquier botón.
    if (state.isInteractiveTourActive) {
        InteractiveTourWalkthrough(
            state = state,
            viewModel = viewModel
        )
    }
    }
}
