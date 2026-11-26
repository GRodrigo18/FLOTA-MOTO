package com.example.ui.screens

import androidx.compose.runtime.collectAsState
import com.example.util.FleetRules
import com.example.data.repository.FleetRepository
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FleetUnitEntity
import com.example.data.model.UnitExpenseEntity
import com.example.data.model.DetailedMaintenanceEntity
import com.example.ui.DialogType
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.ui.components.PlateBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppText
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedBg
import com.example.ui.theme.DangerRedText
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import com.example.ui.theme.WarningAmberText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetScreen(
    state: FleetUiState,
    viewModel: FleetViewModel,
    modifier: Modifier = Modifier
) {
    val filterOptions = listOf("Todas", "Vigente", "Por Vencer", "En Taller", "Inactivo")
    val sort by viewModel.fleetSort.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val listaOrdenada = remember(state.filteredUnits, sort) {
        FleetRules.sortUnits(state.filteredUnits, sort) {
            FleetRules.SortKey(it.nombreMoto, it.placa, it.estado, it.deudaTotal)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top Action: Add Unit Button
        Button(
            onClick = { viewModel.openDialog(DialogType.ADD_UNIT) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .tourTarget("fleet_add")
                .testTag("add_unit_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "NUEVA UNIDAD / CONTRATO",
                fontWeight = FontWeight.Bold,
                fontSize = AppText.Title
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("Buscar por placa o conductor...", fontSize = AppText.Title, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_units_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterOptions) { filter ->
                val selected = state.filterStatus == filter
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.onFilterStatusChanged(filter) },
                    label = {
                        Text(
                            text = filter,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NavyPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = NavyPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Ordenar la lista (se recuerda la última opción)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.AutoMirrored.Filled.Sort,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Ordenar:", fontSize = AppText.Small, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                items(FleetRules.Sort.entries.toList()) { opcion ->
                    val elegido = sort == opcion
                    FilterChip(
                        selected = elegido,
                        onClick = { viewModel.setFleetSort(opcion) },
                        label = { Text(opcion.label, fontSize = AppText.Small) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("fleet_sort_${opcion.name}")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Units List
        if (state.units.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ElectricRickshaw,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No hay motos registradas en la flota",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = AppText.Title
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Registra tu primera unidad para comenzar a gestionar cobros y contratos.",
                        fontSize = AppText.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.openDialog(DialogType.ADD_UNIT) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Registrar Primera Moto", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.filteredUnits.isEmpty()) {
                    item {
                        Text(
                            text = "Ninguna moto coincide con la búsqueda o el filtro.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
                items(listaOrdenada, key = { it.id }) { unit ->
                    UnitCard(
                        modifier = if (unit.id == listaOrdenada.first().id) Modifier.tourTarget("fleet_unit_first") else Modifier,
                        unit = unit,
                        onClick = { viewModel.selectUnit(unit) },
                        onEditContractSoat = {
                            viewModel.openDialog(DialogType.EDIT_CONTRACT_SOAT, unit = unit)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Unit Detail Modal Bottom Sheet
    if (state.selectedUnit != null) {
        val unitMaintenances = state.detailedMaintenances.filter { it.unitPlaca == state.selectedUnit.placa }
        UnitDetailBottomSheet(
            unit = state.selectedUnit,
            detailedMaintenances = unitMaintenances,
            onDismiss = { viewModel.selectUnit(null) },
            onRenewContract = { tipo ->
                viewModel.renewContractWithModalidad(state.selectedUnit, tipo)
            },
            onBreakContract = {
                viewModel.breakContract(state.selectedUnit)
            },
            onRecordPayment = {
                viewModel.openDialog(DialogType.RECORD_PAYMENT)
            },
            onRegisterMaintenance = {
                viewModel.openDialog(DialogType.REGISTER_DETAILED_MAINTENANCE)
            },
            onMarkOperational = {
                viewModel.markUnitAsOperational(state.selectedUnit)
            },
            onMarkInWorkshop = {
                viewModel.markUnitInWorkshop(state.selectedUnit)
            },
            canDelete = state.isAdmin,
            onEdit = { viewModel.openDialog(DialogType.EDIT_CONTRACT_SOAT, unit = state.selectedUnit) },
            loadHistory = { unit, onResult -> viewModel.loadUnitHistory(unit, onResult) },
            onDelete = { viewModel.deleteUnit(state.selectedUnit) },
            loadStatement = { unit, onResult -> viewModel.loadStatement(unit, onResult) },
            onOpenPayments = { viewModel.openDialog(DialogType.MISSED_PAYMENTS) },
            onShareStatement = { text -> viewModel.shareStatement(context, text) }
        )
    }
}

@Composable
private fun UnitCard(
    modifier: Modifier = Modifier,
    unit: FleetUnitEntity,
    onClick: () -> Unit,
    onEditContractSoat: () -> Unit
) {
    val isCritical = unit.progresoContrato >= 0.85f || unit.estado == "Por Vencer" || unit.soatDiasRestantes <= 7
    val progressColor = when {
        unit.estado == "En Taller" -> DangerRed
        isCritical -> WarningAmber
        else -> SuccessGreen
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("unit_card_${unit.placa}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, if (unit.soatDiasRestantes <= 7) DangerRed.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Fila superior: Nombre Moto protagonista ("Moto 1") + Placa + Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricRickshaw,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = unit.nombreMoto.ifEmpty { "Moto ${unit.id}" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = AppText.Heading
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            PlateBadge(placa = unit.placa)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Cuota: S/ ${"%.2f".format(unit.cuotaDiaria)} / día",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(estado = unit.estado)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Información de Alquiler Modificable
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TIEMPO ALQUILER (${unit.tipoDuracion})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = AppText.Small
                            )
                        )
                        Text(
                            text = "${unit.inicioContrato} al ${unit.finContrato}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CUOTA DIARIA",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreenText,
                                fontSize = AppText.Small
                            )
                        )
                        Text(
                            text = "S/ ${"%.2f".format(unit.cuotaDiaria)}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = SuccessGreenText
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SOAT y Alertas por fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = if (unit.soatDiasRestantes <= 7) DangerRedText else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "SOAT: ${unit.soatFin} (${unit.soatDiasRestantes} días)",
                        fontSize = AppText.Body,
                        fontWeight = if (unit.soatDiasRestantes <= 7) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (unit.soatDiasRestantes <= 7) DangerRedText else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (unit.deudaTotal > 0.004) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WarningAmberBg,
                        border = BorderStroke(1.dp, WarningAmberText.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Deuda: S/ ${"%.2f".format(unit.deudaTotal)}",
                            color = WarningAmberText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = AppText.Small,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botón rápido para editar contrato y SOAT
            OutlinedButton(
                onClick = onEditContractSoat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Editar moto y contrato",
                    fontSize = AppText.Body,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDetailBottomSheet(
    unit: FleetUnitEntity,
    detailedMaintenances: List<DetailedMaintenanceEntity>,
    onDismiss: () -> Unit,
    onRenewContract: (String) -> Unit,
    onBreakContract: () -> Unit,
    onRecordPayment: () -> Unit,
    onRegisterMaintenance: () -> Unit,
    onMarkOperational: () -> Unit = {},
    onMarkInWorkshop: () -> Unit = {},
    canDelete: Boolean = false,
    onEdit: () -> Unit = {},
    loadHistory: (FleetUnitEntity, (FleetRepository.UnitHistory) -> Unit) -> Unit = { _, _ -> },
    onDelete: () -> Unit = {},
    loadStatement: (FleetUnitEntity, (String) -> Unit) -> Unit = { _, _ -> },
    onOpenPayments: () -> Unit = {},
    onShareStatement: (String) -> Unit = {}
) {
    var statementText by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRenewDialog by remember { mutableStateOf(false) }
    var showEndContractDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<FleetRepository.UnitHistory?>(null) }
    // Renovar: se elige la modalidad (por defecto la misma) y el fin se calcula solo
    var renewTipo by remember(unit.tipoDuracion) {
        mutableStateOf(
            com.example.util.FleetDates.MODALIDADES.firstOrNull { it.first.equals(unit.tipoDuracion, ignoreCase = true) }?.first ?: "3 Meses"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricRickshaw,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlateBadge(placa = unit.placa)
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(estado = unit.estado)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${unit.nombreMoto} • Cuota: S/ ${"%.2f".format(unit.cuotaDiaria)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Modalidad: ${unit.tipoDuracion}",
                            fontSize = AppText.Body,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chofer: nombre, teléfono, deuda y estado de cuenta del mes
            val tieneChofer = unit.cliente.isNotBlank() && !unit.cliente.equals("Sin asignar", ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (tieneChofer) unit.cliente else "Sin chofer asignado",
                        fontWeight = FontWeight.Bold,
                        fontSize = AppText.Title,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(unit.telefono.ifBlank { "Sin teléfono" })
                            if (unit.deudaTotal > 0.004) append(" • Debe S/ ${"%.2f".format(unit.deudaTotal)}")
                        },
                        fontSize = AppText.Body,
                        color = if (unit.deudaTotal > 0.004) WarningAmberText else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { loadStatement(unit) { statementText = it } },
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Estado de cuenta", fontSize = AppText.Body, maxLines = 1)
                        }
                        // Días sin pagar y "avisar pagos desde" de esta moto
                        OutlinedButton(
                            onClick = onOpenPayments,
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pagos", fontSize = AppText.Body, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contract Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Estado del Contrato",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Inicio", fontSize = AppText.Body, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(unit.inicioContrato, fontWeight = FontWeight.Bold, fontSize = AppText.Title, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Vencimiento", fontSize = AppText.Body, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(unit.finContrato, fontWeight = FontWeight.Bold, fontSize = AppText.Title, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { unit.progresoContrato },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (unit.progresoContrato > 0.8f) WarningAmber else SuccessGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons: Romper / Renovar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEndContractDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            border = BorderStroke(1.dp, DangerRed)
                        ) {
                            Text("FINALIZAR", fontSize = AppText.Body, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showRenewDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Icon(Icons.Outlined.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RENOVAR", fontSize = AppText.Body, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Direct payment button
                    Button(
                        onClick = onRecordPayment,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "REGISTRAR COBRO DE CUOTA (S/ ${"%.2f".format(unit.cuotaDiaria)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = AppText.Body
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // El ingreso y la salida del taller se manejan SOLO en la pestaña Taller.
                    if (unit.estado == "En Taller") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = WarningAmber.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Esta moto está en el taller",
                                fontSize = AppText.Body,
                                fontWeight = FontWeight.SemiBold,
                                color = WarningAmberText,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legal Expirations Cards
            Text(
                text = "Vencimientos Legales",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegalExpiryCard(
                    titulo = "SOAT",
                    vencimiento = unit.soatVence,
                    esUrgente = unit.soatDiasRestantes <= 7,
                    modifier = Modifier.weight(1f)
                )
                LegalExpiryCard(
                    titulo = "Rev. del mes",
                    vencimiento = if (unit.revisionTecnicaMesRealizada) "Al día" else "Pendiente",
                    esUrgente = !unit.revisionTecnicaMesRealizada,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sheet_edit_unit"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar nombre, placa y datos", fontWeight = FontWeight.SemiBold)
            }

            if (canDelete) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.TextButton(
                    onClick = {
                        history = null
                        loadHistory(unit) { history = it }
                        showDeleteDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().testTag("sheet_delete_unit")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Eliminar moto", color = DangerRed, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmar eliminación
    if (showDeleteDialog) {
        val h = history
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
            title = { Text("¿Eliminar ${unit.nombreMoto} (${unit.placa})?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        h == null -> Text("Revisando su historial…")
                        h.isEmpty -> Text("No tiene pagos ni trabajos de taller. Se borra por completo.")
                        else -> {
                            Text(
                                "Tiene ${h.pagos} pagos, ${h.trabajosTaller} trabajos de taller y " +
                                    "${h.movimientosCaja} movimientos de caja. Se borra TODO eso también."
                            )
                            Text(
                                "Si la moto solo dejó de trabajar, mejor márcala como Inactiva (en Editar).",
                                color = WarningAmberText,
                                fontSize = AppText.Small
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    enabled = h != null,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_delete_unit")
                ) {
                    Text("Sí, eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Estado de cuenta del mes: se ve en pantalla y se puede compartir (correo, imprimir, guardar)
    statementText?.let { text ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { statementText = null },
            title = { Text("Estado de cuenta") },
            text = { Text(text, fontSize = AppText.Body) },
            confirmButton = {
                Button(
                    onClick = { onShareStatement(text) },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) { Text("Compartir") }
            },
            dismissButton = { OutlinedButton(onClick = { statementText = null }) { Text("Cerrar") } }
        )
    }

    // Finalizar contrato: se pide confirmación (antes se hacía con un solo toque)
    if (showEndContractDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEndContractDialog = false },
            title = { Text("¿Finalizar contrato de ${unit.nombreMoto}?") },
            text = {
                Text(
                    "La moto quedará Inactiva y sin chofer. Ya no se le cobrará cuota" +
                        (if (unit.deudaTotal > 0.004) " y se anulará toda su deuda (S/ ${"%.2f".format(unit.deudaTotal)})." else ".") +
                        " El historial se conserva.",
                    fontSize = AppText.Body
                )
            },
            confirmButton = {
                Button(
                    onClick = { showEndContractDialog = false; onBreakContract() },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("Finalizar") }
            },
            dismissButton = { OutlinedButton(onClick = { showEndContractDialog = false }) { Text("Volver") } }
        )
    }

    // Renewal Dialog
    if (showRenewDialog) {
        val renewStart = com.example.util.FleetDates.renewalStart(unit.finContrato)
        val renewEnd = com.example.util.FleetDates.contractEnd(renewStart, renewTipo) ?: renewStart
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRenewDialog = false },
            title = { Text("Renovar contrato • ${unit.nombreMoto}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModalidadChips(selected = renewTipo) { renewTipo = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AutoDateField("Desde", renewStart, "Fin del anterior u hoy", Modifier.weight(1f))
                        AutoDateField("Hasta", renewEnd, "Automático", Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenewContract(renewTipo)
                        showRenewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Renovar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRenewDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun LegalExpiryCard(
    titulo: String,
    vencimiento: String,
    esUrgente: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (esUrgente) DangerRedBg else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (esUrgente) DangerRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = if (esUrgente) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = titulo,
                    fontSize = AppText.Body,
                    fontWeight = FontWeight.Bold,
                    color = if (esUrgente) DangerRedText else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = vencimiento,
                fontSize = AppText.Title,
                fontWeight = FontWeight.ExtraBold,
                color = if (esUrgente) DangerRedText else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
