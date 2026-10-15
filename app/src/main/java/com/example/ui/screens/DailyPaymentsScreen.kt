package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyPaymentEntity
import com.example.data.model.DebtRecordEntity
import com.example.data.model.FleetUnitEntity
import com.example.ui.DialogType
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.ui.theme.*
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import com.example.ui.theme.WarningAmberText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatDayWithCalendarNumber(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = sdf.parse(dateStr) ?: return dateStr
        val cal = Calendar.getInstance().apply { time = parsed }
        val dayNumber = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = SimpleDateFormat("EEEE", Locale("es", "PE")).format(parsed)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val monthName = SimpleDateFormat("MMMM", Locale("es", "PE")).format(parsed)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        "$dayOfWeek $dayNumber de $monthName (Día $dayNumber de Calendario)"
    } catch (_: Exception) {
        dateStr
    }
}

fun formatShortDayLabel(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = sdf.parse(dateStr) ?: return dateStr
        val cal = Calendar.getInstance().apply { time = parsed }
        val dayNumber = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = SimpleDateFormat("EEE", Locale("es", "PE")).format(parsed)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        "$dayOfWeek $dayNumber"
    } catch (_: Exception) {
        dateStr
    }
}

@Composable
fun DailyPaymentsScreen(
    state: FleetUiState,
    viewModel: FleetViewModel
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Checklist Diario S/35, 1: Control de Deudas

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab selector superior
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                        color = CyanAccent,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PriceCheck,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (selectedSubTab == 0) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Cobros Diarios S/ 35",
                                fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedSubTab == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.testTag("subtab_daily_payments")
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (selectedSubTab == 1) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Deudas y Garantías",
                                fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedSubTab == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.testTag("subtab_debt_management")
                )
            }
        }

        if (selectedSubTab == 0) {
            DailyChecklistTabContent(state = state, viewModel = viewModel)
        } else {
            DebtManagementTabContent(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun DailyChecklistTabContent(
    state: FleetUiState,
    viewModel: FleetViewModel
) {
    val totalMotos = state.units.size
    val pagadasCount = state.conteoPagadosHoy
    val porcentajePagado = if (totalMotos > 0) pagadasCount.toFloat() / totalMotos else 0f
    val totalEsperado = state.units.sumOf { it.cuotaDiaria }

    var showEditCuotaDialog by remember { mutableStateOf(false) }
    var cuotaToEditInput by remember { mutableStateOf(state.defaultCuotaDiaria.toString()) }
    var unitForCuotaEdit by remember { mutableStateOf<FleetUnitEntity?>(null) }
    var unitCuotaInput by remember { mutableStateOf("35.0") }

    val todayFormatted = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val isFutureDate = state.selectedDate > todayFormatted

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("daily_payments_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Selector de Fecha
        item {
            DateSelectorBar(
                selectedDate = state.selectedDate,
                onDateSelected = { viewModel.setSelectedDate(it) }
            )
        }

        if (isFutureDate) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyanAccent.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CALENDARIO: MODO PAGO ADELANTADO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanAccent
                            )
                            Text(
                                text = "Marcando cuotas para el ${state.selectedDate}. Se registrarán con fecha adelantada en caja y en el historial.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Resumen del día
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = NavySurface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CONTROL DIARIO DE CUOTAS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatDayWithCalendarNumber(state.selectedDate),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Cuota base: S/ ${"%.2f".format(state.defaultCuotaDiaria)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    onClick = {
                                        cuotaToEditInput = state.defaultCuotaDiaria.toString()
                                        showEditCuotaDialog = true
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = CyanAccent.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar cuota", tint = CyanAccent, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Modificar", color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldGreen.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "COBRADO HOY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                                Text(
                                    text = "S/ ${"%.2f".format(state.totalCobradoHoy)}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldGreen
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { porcentajePagado },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldGreen,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$pagadasCount de $totalMotos motos cobradas (${(porcentajePagado * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Meta: S/ ${"%.2f".format(totalEsperado)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.markAllDailyPayments(true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_mark_all_paid"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Marcar Todas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.markAllDailyPayments(false) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_reset_checklist"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Text("Desmarcar", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Lista de Motos en el Checklist
        if (state.units.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricRickshaw,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No hay motos registradas en la flota",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Agrega tus unidades en el módulo Flota para iniciar el registro diario de cobros.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.openDialog(DialogType.ADD_UNIT) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Registrar Primera Moto", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(state.units, key = { "unit_chk_${it.id}" }) { unit ->
                val payment = state.pagosDelDiaSeleccionado.firstOrNull { it.unitPlaca == unit.placa }
                val isPaid = payment != null

                DailyChecklistUnitCard(
                    unit = unit,
                    selectedDate = state.selectedDate,
                    isPaid = isPaid,
                    paidTime = payment?.hora,
                    onToggle = { newPaidState ->
                        viewModel.toggleDailyPayment(unit, newPaidState)
                    },
                    onCardClick = {
                        viewModel.selectUnit(unit)
                    },
                    onEditCuota = {
                        unitForCuotaEdit = unit
                        unitCuotaInput = unit.cuotaDiaria.toString()
                    },
                    onAbonarDeuda = {
                        viewModel.openDialog(DialogType.ADD_DEBT_ABONO, unit)
                    }
                )
            }
        }
    }

    // Diálogo para editar Cuota Diaria Base
    if (showEditCuotaDialog) {
        AlertDialog(
            onDismissRequest = { showEditCuotaDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PriceCheck, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Modificar Cuota Diaria Base", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Configura el cobro diario por defecto para la flota (ej. S/ 35.00, S/ 40.00).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = cuotaToEditInput,
                        onValueChange = { cuotaToEditInput = it },
                        label = { Text("Cuota Base Diaria (S/)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { cuotaToEditInput = "35.0" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("S/ 35.00", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { cuotaToEditInput = "40.0" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("S/ 40.00", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newCuota = cuotaToEditInput.toDoubleOrNull() ?: 35.0
                        viewModel.updateDefaultCuotaDiaria(newCuota)
                        showEditCuotaDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                ) {
                    Text("Aplicar a Flota", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditCuotaDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para editar cuota de una moto específica
    unitForCuotaEdit?.let { targetUnit ->
        AlertDialog(
            onDismissRequest = { unitForCuotaEdit = null },
            title = {
                Text("Cuota de ${targetUnit.nombreMoto} (${targetUnit.placa})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Personaliza la cuota diaria para esta unidad específica:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = unitCuotaInput,
                        onValueChange = { unitCuotaInput = it },
                        label = { Text("Cuota Diaria (S/)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cuotaVal = unitCuotaInput.toDoubleOrNull() ?: targetUnit.cuotaDiaria
                        viewModel.updateUnitCuota(targetUnit, cuotaVal)
                        unitForCuotaEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { unitForCuotaEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DailyChecklistUnitCard(
    unit: FleetUnitEntity,
    selectedDate: String,
    isPaid: Boolean,
    paidTime: String?,
    onToggle: (Boolean) -> Unit,
    onCardClick: () -> Unit,
    onEditCuota: () -> Unit = {},
    onAbonarDeuda: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) EmeraldGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isPaid) 1.5.dp else 1.dp,
            color = if (isPaid) EmeraldGreen.copy(alpha = 0.6f) else SurfaceBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isPaid) }
            .testTag("unit_checklist_card_${unit.placa}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox interactivo grande
            Checkbox(
                checked = isPaid,
                onCheckedChange = { onToggle(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = EmeraldGreen,
                    checkmarkColor = Color.Black,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .size(36.dp)
                    .testTag("checkbox_paid_${unit.placa}")
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Datos de la moto identificada por nombre
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = unit.nombreMoto.ifEmpty { "Moto ${unit.id}" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NavyPrimary.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = unit.placa,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Chofer: ${unit.cliente.ifBlank { "Sin asignar" }}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Cobro: ${formatShortDayLabel(selectedDate)} (Calendario)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )

                if (unit.deudaTotal > 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = WarningAmberBg,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, WarningAmberText.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Debe: S/ ${"%.2f".format(unit.deudaTotal)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WarningAmberText,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            onClick = onAbonarDeuda,
                            shape = RoundedCornerShape(4.dp),
                            color = WarningAmber.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Abonar",
                                color = WarningAmberText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Lado Derecho: Cuota de S/ 35.00 en caja destacada y estado alineados sin descuadres
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Caja destacada de la cuota diaria (S/ 35.00)
                Surface(
                    onClick = onEditCuota,
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPaid) SuccessGreenBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isPaid) SuccessGreenText.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "S/ ${"%.2f".format(unit.cuotaDiaria)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPaid) SuccessGreenText else MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar cuota",
                            tint = CyanAccent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Insignia de estado: PAGADO / PENDIENTE
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isPaid) SuccessGreenBg else DangerRedBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isPaid) SuccessGreenText.copy(alpha = 0.3f) else DangerRedText.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (isPaid) (paidTime ?: "PAGADO") else "PENDIENTE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPaid) SuccessGreenText else DangerRedText
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtManagementTabContent(
    state: FleetUiState,
    viewModel: FleetViewModel
) {
    var filterByPlaca by remember { mutableStateOf<String?>(null) }

    val filteredUnits = if (filterByPlaca == null) {
        state.units
    } else {
        state.units.filter { it.placa == filterByPlaca }
    }

    val filteredRecords = if (filterByPlaca == null) {
        state.debtRecords
    } else {
        state.debtRecords.filter { it.unitPlaca == filterByPlaca }
    }

    val totalDeudaGeneral = state.units.sumOf { it.deudaTotal }
    val totalGarantiaGeneral = state.units.sumOf { it.deudaGarantia }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("debt_management_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Resumen total de deudas
        item(key = "debt_header_summary") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CONTROL DE CUENTAS CORRIENTES Y DEUDAS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Deuda Total Flota",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            )
                            Text(
                                text = "S/ ${"%.2f".format(totalDeudaGeneral)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (totalDeudaGeneral > 0) WarningAmberText else SuccessGreenText
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Garantías Pendientes",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            )
                            Text(
                                text = "S/ ${"%.2f".format(totalGarantiaGeneral)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            )
                        }
                    }
                }
            }
        }

        // Título de Unidades
        item(key = "debt_header_units") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saldos por Moto",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                if (filterByPlaca != null) {
                    OutlinedButton(
                        onClick = { filterByPlaca = null },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Ver todas", fontSize = 12.sp)
                    }
                }
            }
        }

        // Unidades con sus botones de Cargo y Abono
        items(filteredUnits, key = { "debt_unit_${it.id}" }) { unit ->
            DebtUnitCard(
                unit = unit,
                isSelected = filterByPlaca == unit.placa,
                onCardClick = {
                    filterByPlaca = if (filterByPlaca == unit.placa) null else unit.placa
                },
                onAddCargo = {
                    viewModel.openDialog(DialogType.ADD_DEBT_CARGO, unit)
                },
                onAddAbono = {
                    viewModel.openDialog(DialogType.ADD_DEBT_ABONO, unit)
                }
            )
        }

        // Historial de Movimientos
        item(key = "debt_header_history") {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (filterByPlaca == null) "Últimos Movimientos de Deudas" else "Movimientos de la Moto Seleccionada",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        if (filteredRecords.isEmpty()) {
            item(key = "debt_empty_records") {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No hay registros de deudas o abonos aún.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            items(filteredRecords, key = { "debt_rec_${it.id}" }) { record ->
                DebtRecordRow(record = record)
            }
        }
    }
}

@Composable
private fun DebtUnitCard(
    unit: FleetUnitEntity,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onAddCargo: () -> Unit,
    onAddAbono: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NavyPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) CyanAccent else SurfaceBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("debt_unit_card_${unit.placa}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = unit.nombreMoto.ifEmpty { "Moto ${unit.id}" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${unit.placa})",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (unit.deudaTotal > 0.0) WarningAmberBg else SuccessGreenBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (unit.deudaTotal > 0.0) WarningAmberText.copy(alpha = 0.3f) else SuccessGreenText.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (unit.deudaTotal > 0.0) "DEBE S/ ${"%.2f".format(unit.deudaTotal)}" else "AL DÍA (S/ 0.00)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (unit.deudaTotal > 0.0) WarningAmberText else SuccessGreenText
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Placa: ${unit.placa} • Estado: ${unit.estado}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            if (unit.deudaGarantia > 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Saldo de Garantía pendiente: S/ ${"%.2f".format(unit.deudaGarantia)} de S/ 200.00",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddCargo,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmberText.copy(alpha = 0.8f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmberText)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Cargar Deuda", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddAbono,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreenText,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Abonar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DebtRecordRow(record: DebtRecordEntity) {
    val isCargo = record.tipo == "CARGO"

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isCargo) WarningAmberBg else SuccessGreenBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCargo) Icons.Default.Add else Icons.Default.Remove,
                    contentDescription = null,
                    tint = if (isCargo) WarningAmberText else SuccessGreenText,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${record.nombreMoto} (${record.unitPlaca}) - ${if (isCargo) "Cargo" else "Abono"}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = record.motivo,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = record.fecha,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Text(
                text = "${if (isCargo) "+" else "-"} S/ ${"%.2f".format(record.monto)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCargo) WarningAmberText else SuccessGreenText
                )
            )
        }
    }
}

@Composable
private fun DateSelectorBar(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = sdf.format(Date())

    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = sdf.format(cal.time)

    cal.add(Calendar.DAY_OF_YEAR, 2)
    val tomorrowStr = sdf.format(cal.time)

    var showDatePickerDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        try {
                            val parsed = sdf.parse(selectedDate) ?: Date()
                            val c = Calendar.getInstance().apply { time = parsed; add(Calendar.DAY_OF_YEAR, -1) }
                            onDateSelected(sdf.format(c.time))
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Día anterior", modifier = Modifier.size(16.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedDate == yesterdayStr,
                        onClick = { onDateSelected(yesterdayStr) },
                        label = { Text("Ayer (${formatShortDayLabel(yesterdayStr)})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedDate == todayStr,
                        onClick = { onDateSelected(todayStr) },
                        label = { Text("HOY (${formatShortDayLabel(todayStr)})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedDate == tomorrowStr,
                        onClick = { onDateSelected(tomorrowStr) },
                        label = { Text("Mañana (${formatShortDayLabel(tomorrowStr)})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                IconButton(
                    onClick = {
                        try {
                            val parsed = sdf.parse(selectedDate) ?: Date()
                            val c = Calendar.getInstance().apply { time = parsed; add(Calendar.DAY_OF_YEAR, 1) }
                            onDateSelected(sdf.format(c.time))
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Día siguiente", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Botón selector de fecha del calendario con detalle completo del día
            Surface(
                onClick = { showDatePickerDialog = true },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = CyanAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatDayWithCalendarNumber(selectedDate),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Toca para abrir calendario y elegir cualquier día",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDatePickerDialog) {
        val parsedDate = remember(selectedDate) {
            try {
                sdf.parse(selectedDate) ?: Date()
            } catch (e: Exception) {
                Date()
            }
        }
        val calParsed = remember(parsedDate) {
            Calendar.getInstance().apply { time = parsedDate }
        }

        var currentYear by remember { mutableIntStateOf(calParsed.get(Calendar.YEAR)) }
        var currentMonth by remember { mutableIntStateOf(calParsed.get(Calendar.MONTH) + 1) }
        var currentDay by remember { mutableIntStateOf(calParsed.get(Calendar.DAY_OF_MONTH)) }

        val monthFullNames = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        val dialogDaysInMonth = remember(currentYear, currentMonth) {
            val c = Calendar.getInstance()
            c.set(Calendar.YEAR, currentYear)
            c.set(Calendar.MONTH, currentMonth - 1)
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        val dialogFirstDay = remember(currentYear, currentMonth) {
            val c = Calendar.getInstance()
            c.set(Calendar.YEAR, currentYear)
            c.set(Calendar.MONTH, currentMonth - 1)
            c.set(Calendar.DAY_OF_MONTH, 1)
            (c.get(Calendar.DAY_OF_WEEK) + 5) % 7
        }

        AlertDialog(
            onDismissRequest = { showDatePickerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calendario de Cobros", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Controles de Mes y Año Continuos (Estilo laptop/dispositivo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (currentMonth == 1) {
                                        currentMonth = 12
                                        currentYear--
                                    } else {
                                        currentMonth--
                                    }
                                    if (currentDay > dialogDaysInMonth) currentDay = 1
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Mes anterior", modifier = Modifier.size(14.dp), tint = CyanAccent)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${monthFullNames.getOrElse(currentMonth - 1) { "" }} $currentYear",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Día $currentDay seleccionado",
                                    fontSize = 10.sp,
                                    color = CyanAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (currentMonth == 12) {
                                        currentMonth = 1
                                        currentYear++
                                    } else {
                                        currentMonth++
                                    }
                                    if (currentDay > dialogDaysInMonth) currentDay = 1
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Mes siguiente", modifier = Modifier.size(14.dp), tint = CyanAccent)
                            }
                        }

                        // Accesos rápidos
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                onClick = { currentYear-- },
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text("-1a", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                            }
                            Surface(
                                onClick = { currentYear++ },
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text("+1a", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                            }
                            Surface(
                                onClick = {
                                    val now = Calendar.getInstance()
                                    currentYear = now.get(Calendar.YEAR)
                                    currentMonth = now.get(Calendar.MONTH) + 1
                                    currentDay = now.get(Calendar.DAY_OF_MONTH)
                                },
                                shape = RoundedCornerShape(4.dp),
                                color = CyanAccent.copy(alpha = 0.18f)
                            ) {
                                Text("Hoy", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanAccent, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }

                    // Encabezado de Días de semana
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("L", "M", "M", "J", "V", "S", "D").forEach { d ->
                            Text(
                                text = d,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Grilla de días
                    val totalCells = dialogFirstDay + dialogDaysInMonth
                    val rows = (totalCells + 6) / 7
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (r in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                for (c in 0..6) {
                                    val idx = r * 7 + c
                                    val dNum = idx - dialogFirstDay + 1
                                    if (dNum in 1..dialogDaysInMonth) {
                                        val isSelected = currentDay == dNum
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1.1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) CyanAccent else Color.Transparent)
                                                .clickable { currentDay = dNum },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$dNum",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Fecha seleccionada: %04d-%02d-%02d".format(currentYear, currentMonth, currentDay),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            modifier = Modifier.padding(8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formatted = "%04d-%02d-%02d".format(currentYear, currentMonth, currentDay)
                        onDateSelected(formatted)
                        showDatePickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Aplicar Fecha", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
