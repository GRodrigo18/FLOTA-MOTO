package com.example

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
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.WorkshopScreen
import androidx.compose.material.icons.outlined.HelpOutline
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SurfaceBorder

class MainActivity : FragmentActivity() {
    private val viewModel: FleetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = state.isDarkMode) {
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(state.messageSnackbar) {
                    state.messageSnackbar?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.dismissSnackbar()
                    }
                }

                if (!state.isLoggedIn) {
                    LoginScreen(
                        onLoginWithGoogle = { email, name ->
                            viewModel.loginWithGoogle(email, name)
                        },
                        onLoginSuccess = { viewModel.setLoggedIn(true) }
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    state: FleetUiState,
    viewModel: FleetViewModel,
    snackbarHostState: SnackbarHostState
) {
    val navTitles = listOf(
        "Centro de Operaciones",
        "Control de Pagos y Deudas",
        "Control de Flota y Contratos",
        "Caja y Finanzas",
        "Taller e Inventario"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(NavyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricRickshaw,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = navTitles.getOrElse(state.activeTab) { "MotoFlota Pro" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        )
                    }
                },
                actions = {
                    val urgentCount = state.urgentAlerts.size
                    IconButton(
                        onClick = { viewModel.openDialog(DialogType.EMERGENT_ALERTS) },
                        modifier = Modifier.testTag("urgent_alerts_bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (urgentCount > 0) {
                                    Badge(
                                        containerColor = DangerRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("$urgentCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (urgentCount > 0) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                                contentDescription = "Alertas de Vencimiento",
                                tint = if (urgentCount > 0) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.openDialog(DialogType.BACKUP_CLOUD) },
                        modifier = Modifier.testTag("backup_cloud_button")
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NavyPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.userName.take(1).uppercase(),
                                    color = CyanAccent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.startInteractiveTour() },
                        modifier = Modifier.testTag("tour_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = "Guía Interactiva",
                            tint = CyanAccent
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (state.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (state.isDarkMode) "Modo Claro" else "Modo Oscuro",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setLoggedIn(false) },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                // Tab 0: Inicio
                NavigationBarItem(
                    selected = state.activeTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    icon = {
                        Icon(
                            imageVector = if (state.activeTab == 0) Icons.Default.SpaceDashboard else Icons.Outlined.SpaceDashboard,
                            contentDescription = "Inicio"
                        )
                    },
                    label = { Text("Inicio", fontWeight = if (state.activeTab == 0) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Tab 1: Pagos y Deudas
                NavigationBarItem(
                    selected = state.activeTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    icon = {
                        Icon(
                            imageVector = if (state.activeTab == 1) Icons.Default.PriceCheck else Icons.Outlined.PriceCheck,
                            contentDescription = "Pagos"
                        )
                    },
                    label = { Text("Pagos", fontWeight = if (state.activeTab == 1) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Tab 2: Flota
                NavigationBarItem(
                    selected = state.activeTab == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    icon = {
                        Icon(
                            imageVector = if (state.activeTab == 2) Icons.Default.ElectricRickshaw else Icons.Outlined.ElectricRickshaw,
                            contentDescription = "Flota"
                        )
                    },
                    label = { Text("Flota", fontWeight = if (state.activeTab == 2) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Tab 3: Finanzas
                NavigationBarItem(
                    selected = state.activeTab == 3,
                    onClick = { viewModel.onTabSelected(3) },
                    icon = {
                        Icon(
                            imageVector = if (state.activeTab == 3) Icons.Default.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Finanzas"
                        )
                    },
                    label = { Text("Finanzas", fontWeight = if (state.activeTab == 3) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Tab 4: Taller
                NavigationBarItem(
                    selected = state.activeTab == 4,
                    onClick = { viewModel.onTabSelected(4) },
                    icon = {
                        Icon(
                            imageVector = if (state.activeTab == 4) Icons.Default.BuildCircle else Icons.Outlined.BuildCircle,
                            contentDescription = "Taller"
                        )
                    },
                    label = { Text("Taller", fontWeight = if (state.activeTab == 4) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
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
                targetState = state.activeTab,
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

            if (state.isInteractiveTourActive) {
                InteractiveTourWalkthrough(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }
}
