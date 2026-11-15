package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.AlertType
import com.example.ui.DialogType
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.ui.UrgentAlert
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedText
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberText

private fun alertIcon(tipo: AlertType): ImageVector = when (tipo) {
    AlertType.SOAT -> Icons.Outlined.Shield
    AlertType.REVISION -> Icons.Default.Build
    AlertType.CONTRATO -> Icons.Default.ElectricRickshaw
    AlertType.STOCK -> Icons.Default.ShoppingCart
    AlertType.DEUDA -> Icons.Default.MonetizationOn
}

/** Lo urgente (SOAT, stock) va en rojo; lo demás en ámbar. */
private fun isUrgent(alert: UrgentAlert) = alert.tipo == AlertType.SOAT || alert.tipo == AlertType.STOCK

/**
 * Al tocar un recordatorio se abre directamente lo que hay que hacer, sin botones intermedios.
 * Contrato → ficha de la moto en Flota (Renovar/Editar); SOAT y revisión → editar la moto;
 * deuda de una moto → abono; cobros del día → Pagos; stock → Taller.
 */
fun openAlertTarget(alert: UrgentAlert, state: FleetUiState, viewModel: FleetViewModel) {
    val unit = state.units.firstOrNull { it.placa == alert.unitPlaca }
    when (alert.tipo) {
        AlertType.CONTRATO -> {
            viewModel.onTabSelected(2)
            if (unit != null) viewModel.selectUnit(unit)
        }
        AlertType.SOAT, AlertType.REVISION ->
            if (unit != null) viewModel.openDialogFromAlert(DialogType.EDIT_CONTRACT_SOAT, unit) else viewModel.onTabSelected(2)
        AlertType.DEUDA -> when {
            // Días sin pagar de una moto: lista de días con «Pagó» / «No trabajó»
            unit != null && alert.id.startsWith(com.example.ui.AlertEngine.FALTAS_PREFIX) ->
                viewModel.openDialogFromAlert(DialogType.MISSED_PAYMENTS, unit)
            unit != null -> viewModel.openDialogFromAlert(DialogType.ADD_DEBT_ABONO, unit)
            else -> viewModel.onTabSelected(1)
        }
        AlertType.STOCK -> viewModel.onTabSelected(4)
    }
}

/**
 * Fila de recordatorio: tocarla lleva a resolverlo. Se quita sola cuando se resuelve.
 * [onHideFromHome] != null (Inicio) → ojo tachado "Quitar de Inicio" (sigue en la campana).
 * En la campana no hay botón para quitar; si está oculto en Inicio se ve la etiqueta
 * "Oculto en Inicio" y la acción [onShowOnHome].
 * Los avisos al celular salen solos (ver FleetViewModel.startAutomaticWork y DailyJobWorker).
 */
@Composable
fun AlertRow(
    alert: UrgentAlert,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onHideFromHome: (() -> Unit)? = null,
    hiddenFromHome: Boolean = false,
    onShowOnHome: (() -> Unit)? = null
) {
    val accent = if (isUrgent(alert)) DangerRed else WarningAmber
    val textAccent = if (isUrgent(alert)) DangerRedText else WarningAmberText

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("alert_row_${alert.id}")
    ) {
        Row(
            modifier = Modifier.padding(
                start = 12.dp,
                top = 10.dp,
                bottom = 10.dp,
                end = if (onHideFromHome != null) 4.dp else 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(alertIcon(alert.tipo), contentDescription = null, tint = textAccent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = alert.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (hiddenFromHome) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "Oculto en Inicio",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (onShowOnHome != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(
                                onClick = onShowOnHome,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("alert_show_home_${alert.id}")
                            ) {
                                Text("Mostrar en Inicio", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            if (onHideFromHome != null) {
                // IconButton mantiene el área táctil de 48dp
                IconButton(onClick = onHideFromHome, modifier = Modifier.testTag("alert_hide_home_${alert.id}")) {
                    Icon(
                        Icons.Default.VisibilityOff,
                        contentDescription = "Quitar de Inicio",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AllClearRow(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SuccessGreenBg,
        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreenText)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Todo al día", style = MaterialTheme.typography.titleSmall, color = SuccessGreenText)
                Text(
                    "SOAT, contratos, cobros y stock sin pendientes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreenText.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * Ventana de la campana: TODOS los recordatorios pendientes, siempre. No se pueden borrar:
 * se quitan solos cuando se resuelve el problema (renovar contrato, pagar deuda, etc.).
 */
@Composable
fun EmergentAlertsDialog(
    state: FleetUiState,
    viewModel: FleetViewModel,
    onDismiss: () -> Unit
) {
    val alerts = state.urgentAlerts
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("emergent_alerts_dialog"),
        title = {
            Column {
                Text("Recordatorios pendientes", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (alerts.isEmpty()) "Nada pendiente" else "Se quitan solos cuando lo resuelves",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            if (alerts.isEmpty()) {
                AllClearRow()
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(alerts, key = { it.hideKey }) { alert ->
                        AlertRow(
                            alert = alert,
                            onOpen = {
                                onDismiss()
                                openAlertTarget(alert, state, viewModel)
                            },
                            hiddenFromHome = alert.hideKey in state.homeHiddenKeys,
                            onShowOnHome = { viewModel.showAlertOnHome(alert) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
