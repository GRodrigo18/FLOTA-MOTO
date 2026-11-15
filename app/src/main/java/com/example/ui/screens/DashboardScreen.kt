package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.DialogType
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedBg
import com.example.ui.theme.DangerRedText
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.WarningAmber

/**
 * Inicio simplificado:
 *  1. Cobros de hoy (lo que más se usa) en una sola tarjeta.
 *  2. Pendientes: una sola lista de alertas (antes salían repetidas en 3 tarjetas distintas).
 *  3. Cifras clave. La Caja y los movimientos solo los ve un Administrador.
 *  4. Accesos rápidos.
 */
@Composable
fun DashboardScreen(
    state: FleetUiState,
    viewModel: FleetViewModel,
    modifier: Modifier = Modifier
) {
    // Inicio muestra solo lo no ocultado; la campana siempre tiene todo.
    val alerts = state.homeAlerts
    val hiddenCount = (state.urgentAlerts.size - alerts.size).coerceAtLeast(0)
    // Siempre HOY (antes dependía de la fecha elegida en la pestaña Pagos)
    val hoy = com.example.util.FleetDates.today()
    val pagosHoy = state.dailyPayments.filter { it.fecha == hoy && it.pagado }
    // Las motos marcadas "No trabajó" hoy no cuentan como cobrables
    val noTrabajaronHoy = state.dailyPayments.filter { it.fecha == hoy && !it.pagado }.map { it.unitPlaca }.toSet()
    // Mismo criterio que el cobro diario y las alertas: trabajando y con chofer asignado
    val cobrables = state.units.count {
        com.example.util.FleetRules.debeCuota(it.estado) && it.cliente.isNotBlank() &&
            !it.cliente.equals("Sin asignar", ignoreCase = true) && it.placa !in noTrabajaronHoy
    }
    val maxVisibleAlerts = 3

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(2.dp)) }

        // 1. Cobros de hoy
        item {
            TodayCollectionCard(
                userName = state.userName,
                pagados = pagosHoy.map { it.unitPlaca }.distinct().size,
                total = cobrables,
                cobrado = pagosHoy.sumOf { it.cuotaMonto },
                onClick = { viewModel.onTabSelected(1) }
            )
        }

        // 2. Pendientes: tocar = resolver; el ojo lo quita de Inicio (sigue en la campana).
        item {
            SectionTitle(
                text = if (alerts.isEmpty()) "Pendientes" else "Pendientes (${alerts.size})",
                action = if (alerts.size > maxVisibleAlerts) "Ver todas" else null,
                onAction = { viewModel.requestOpenAlerts() }
            )
        }
        if (state.urgentAlerts.isEmpty()) {
            item { AllClearRow() }
        } else {
            items(alerts.take(maxVisibleAlerts), key = { it.hideKey }) { alert ->
                AlertRow(
                    alert = alert,
                    onOpen = { openAlertTarget(alert, state, viewModel) },
                    onHideFromHome = { viewModel.hideAlertFromHome(alert) }
                )
            }
            if (hiddenCount > 0) {
                item(key = "hidden_in_bell") {
                    HiddenInBellLine(count = hiddenCount, onClick = { viewModel.requestOpenAlerts() })
                }
            }
        }

        // 3. Cifras
        item { SectionTitle(text = "Resumen") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Deuda de choferes",
                    value = "S/ ${"%,.2f".format(state.totalDeudaPendiente)}",
                    detail = "${state.units.count { it.deudaTotal > 0 }} motos con saldo",
                    icon = Icons.Default.MonetizationOn,
                    color = WarningAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onTabSelected(1) }
                )
                StatTile(
                    label = "Flota",
                    value = "${state.units.size} motos",
                    detail = "${state.units.count { it.estado == "Vigente" || it.estado == "Por Vencer" }} trabajando",
                    icon = Icons.Default.ElectricRickshaw,
                    color = NavyLight,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onTabSelected(2) }
                )
            }
        }
        if (state.isAdmin) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "Caja disponible",
                        value = "S/ ${"%,.0f".format(state.totalBalance)}",
                        detail = "Ingresos S/ ${"%,.0f".format(state.totalIngresos)}",
                        icon = Icons.Default.AccountBalance,
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onTabSelected(3) }
                    )
                    StatTile(
                        label = "Gastos del mes",
                        value = "S/ ${"%,.0f".format(state.egresosMesActual)}",
                        detail = "Taller e insumos",
                        icon = Icons.Default.TrendingDown,
                        color = DangerRed,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onTabSelected(3) }
                    )
                }
            }
        }

        // 4. Accesos rápidos
        item { SectionTitle(text = "Accesos rápidos") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Cobrar", Icons.Default.PriceCheck, SuccessGreen, Modifier.weight(1f)) { viewModel.onTabSelected(1) }
                QuickAction("Deudas", Icons.Default.MonetizationOn, WarningAmber, Modifier.weight(1f)) { viewModel.onTabSelected(1) }
                QuickAction("+ Moto", Icons.Default.Add, NavyPrimary, Modifier.weight(1f)) { viewModel.openDialog(DialogType.ADD_UNIT) }
                QuickAction("Taller", Icons.Default.Build, CyanAccent, Modifier.weight(1f)) { viewModel.onTabSelected(4) }
            }
        }

        // 5. Últimos movimientos (solo administrador)
        if (state.isAdmin && state.movements.isNotEmpty()) {
            item {
                SectionTitle(text = "Últimos movimientos", action = "Ver caja", onAction = { viewModel.onTabSelected(3) })
            }
            items(state.movements.take(4), key = { it.id }) { mov ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (mov.esIngreso) SuccessGreenBg else DangerRedBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (mov.esIngreso) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = if (mov.esIngreso) "Ingreso" else "Gasto",
                                tint = if (mov.esIngreso) SuccessGreenText else DangerRedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                mov.concepto,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                listOfNotNull(mov.fecha, mov.unitPlaca).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${if (mov.esIngreso) "+" else "-"} S/ ${"%.2f".format(mov.monto)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (mov.esIngreso) SuccessGreenText else DangerRedText
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun TodayCollectionCard(
    userName: String,
    pagados: Int,
    total: Int,
    cobrado: Double,
    onClick: () -> Unit
) {
    val progress = if (total > 0) (pagados.toFloat() / total).coerceIn(0f, 1f) else 0f
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .tourTarget("today_card")
            .testTag("today_collection_card")
    ) {
        Column(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(NavyDark, NavyPrimary)), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Text("Hola, $userName", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(6.dp))
            Text("Cobros de hoy", style = MaterialTheme.typography.titleSmall, color = CyanAccent)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$pagados de $total", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "motos pagaron",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = CyanAccent,
                trackColor = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Cobrado: S/ ${"%,.2f".format(cobrado)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text("Ver lista ›", style = MaterialTheme.typography.labelLarge, color = CyanAccent)
            }
        }
    }
}

/** Línea corta cuando hay recordatorios quitados de Inicio: abre la campana. */
@Composable
private fun HiddenInBellLine(count: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("home_hidden_in_bell")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (count == 1) "1 recordatorio en la campana 🔔" else "$count recordatorios en la campana 🔔",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text("Ver", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SectionTitle(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (action != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    detail: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier.testTag("quick_action_$label")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}
