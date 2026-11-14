package com.example.ui.screens

import com.example.util.FleetRules
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
import com.example.ui.theme.AppText
import com.example.ui.theme.*
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import com.example.ui.theme.WarningAmberText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun DailyChecklistTabContent(
    state: FleetUiState,
    viewModel: FleetViewModel
) {
    // Las motos inactivas no pagan cuota: no aparecen en el cobro diario ni en lo esperado.
    val motosCobro = state.units.filter { it.estado != "Inactivo" }
    val totalMotos = motosCobro.count { FleetRules.debeCuota(it.estado) }
    val pagadasCount = state.conteoPagadosHoy
    val porcentajePagado = if (totalMotos > 0) (pagadasCount.toFloat() / totalMotos).coerceAtMost(1f) else 0f
    val totalEsperado = motosCobro.filter { FleetRules.debeCuota(it.estado) }.sumOf { it.cuotaDiaria }

    var showEditCuotaDialog by remember { mutableStateOf(false) }
    var cuotaToEditInput by remember { mutableStateOf(state.defaultCuotaDiaria.toString()) }
    var unitForCuotaEdit by remember { mutableStateOf<FleetUnitEntity?>(null) }
    var confirmMarkAll by remember { mutableStateOf(false) }
    var confirmUnmarkAll by remember { mutableStateOf(false) }

    if (confirmMarkAll) {
        val pendientes = motosCobro.count { FleetRules.debeCuota(it.estado) && it.placa !in state.pagosDelDiaSeleccionado.filter { p -> p.pagado }.map { p -> p.unitPlaca } }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmMarkAll = false },
            title = { Text("¿Marcar todas como pagadas?") },
            text = { Text("Se registrará la cuota de $pendientes moto(s) que faltan para el ${state.selectedDate}. Las que ya pagaron no cambian.", fontSize = AppText.Body) },
            confirmButton = {
                Button(onClick = { confirmMarkAll = false; viewModel.markAllDailyPayments(true) }) { Text("Sí, marcar") }
            },
            dismissButton = { OutlinedButton(onClick = { confirmMarkAll = false }) { Text("Cancelar") } }
        )
    }
    if (confirmUnmarkAll) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmUnmarkAll = false },
            title = { Text("¿Desmarcar todos los cobros?") },
            text = { Text("Se borrarán los pagos del ${state.selectedDate} y sus ingresos en Caja.", fontSize = AppText.Body) },
            confirmButton = {
                Button(onClick = { confirmUnmarkAll = false; viewModel.markAllDailyPayments(false) }) { Text("Sí, desmarcar") }
            },
            dismissButton = { OutlinedButton(onClick = { confirmUnmarkAll = false }) { Text("Cancelar") } }
        )
    }
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
                                fontSize = AppText.Small,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanAccent
                            )
                            Text(
                                text = "Marcando cuotas para el ${state.selectedDate}. Se registrarán con fecha adelantada en caja y en el historial.",
                                fontSize = AppText.Body,
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
                                fontSize = AppText.Body,
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
                                        Text("Modificar", color = CyanAccent, fontSize = AppText.Small, fontWeight = FontWeight.Bold)
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
                                    fontSize = AppText.Small,
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
                            onClick = { confirmMarkAll = true },
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
                            Text("Marcar Todas", fontWeight = FontWeight.Bold, fontSize = AppText.Body)
                        }

                        OutlinedButton(
                            onClick = { confirmUnmarkAll = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_reset_checklist"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Text("Desmarcar", color = Color.White.copy(alpha = 0.85f), fontSize = AppText.Body)
                        }
                    }
                }
            }
        }

        // Lista de Motos en el Checklist
        if (motosCobro.isEmpty()) {
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
                            fontSize = AppText.Title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Agrega tus unidades en el módulo Flota para iniciar el registro diario de cobros.",
                            fontSize = AppText.Body,
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
            items(motosCobro, key = { "unit_chk_${it.id}" }) { unit ->
                val payment = state.pagosDelDiaSeleccionado.firstOrNull { it.unitPlaca == unit.placa }
                val isPaid = payment != null

                DailyChecklistUnitCard(
                    unit = unit,
                    isFirst = unit.id == motosCobro.first().id,
                    selectedDate = state.selectedDate,
                    isPaid = isPaid,
                    paidTime = payment?.hora,
                    paidAmount = payment?.cuotaMonto,
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
                    Text("Modificar Cuota Diaria Base", fontWeight = FontWeight.Bold, fontSize = AppText.Title)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Configura el cobro diario por defecto para la flota (ej. S/ 35.00, S/ 40.00).",
                        fontSize = AppText.Body,
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
                            Text("S/ 35.00", fontSize = AppText.Body)
                        }
                        OutlinedButton(
                            onClick = { cuotaToEditInput = "40.0" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("S/ 40.00", fontSize = AppText.Body)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newCuota = cuotaToEditInput.replace(',', '.').toDoubleOrNull() ?: 35.0
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
                Text("Cuota de ${targetUnit.nombreMoto} (${targetUnit.placa})", fontWeight = FontWeight.Bold, fontSize = AppText.Title)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Personaliza la cuota diaria para esta unidad específica:", fontSize = AppText.Body)
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
                        val cuotaVal = unitCuotaInput.replace(',', '.').toDoubleOrNull() ?: targetUnit.cuotaDiaria
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
internal fun DailyChecklistUnitCard(
    unit: FleetUnitEntity,
    isFirst: Boolean = false,
    selectedDate: String,
    isPaid: Boolean,
    paidTime: String?,
    onToggle: (Boolean) -> Unit,
    onCardClick: () -> Unit,
    onEditCuota: () -> Unit = {},
    onAbonarDeuda: () -> Unit = {},
    paidAmount: Double? = null
) {
    val noPaga = !FleetRules.debeCuota(unit.estado)
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
                    .then(if (isFirst) Modifier.tourTarget("pay_check_first") else Modifier)
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
                            fontSize = AppText.Title
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
                        fontSize = AppText.Body
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
                                fontSize = AppText.Small,
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
                            // Si ya pagó, se muestra lo que realmente pagó (puede ser distinto a la cuota)
                            text = "S/ ${"%.2f".format(if (isPaid && paidAmount != null) paidAmount else unit.cuotaDiaria)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPaid) SuccessGreenText else MaterialTheme.colorScheme.onSurface,
                                fontSize = AppText.Title
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

                // Insignia de estado: PAGADO / PENDIENTE / EN TALLER
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when { isPaid -> SuccessGreenBg; noPaga -> MaterialTheme.colorScheme.surfaceVariant; else -> DangerRedBg },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when { isPaid -> SuccessGreenText.copy(alpha = 0.3f); noPaga -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f); else -> DangerRedText.copy(alpha = 0.3f) }
                    )
                ) {
                    Text(
                        text = when {
                            isPaid -> paidTime ?: "PAGADO"
                            noPaga -> "EN TALLER"
                            else -> "PENDIENTE"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = when { isPaid -> SuccessGreenText; noPaga -> MaterialTheme.colorScheme.onSurfaceVariant; else -> DangerRedText }
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
