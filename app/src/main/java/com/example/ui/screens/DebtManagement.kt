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
internal fun DebtManagementTabContent(
    state: FleetUiState,
    viewModel: FleetViewModel
) {
    var filterByPlaca by remember { mutableStateOf<String?>(null) }
    var recordToUndo by remember { mutableStateOf<DebtRecordEntity?>(null) }

    // Anular un cargo o abono registrado por error (solo administrador)
    recordToUndo?.let { rec ->
        val efecto = when (rec.tipo) {
            "CARGO" -> "La deuda de ${rec.nombreMoto} bajará S/ ${"%.2f".format(rec.monto)}."
            "ABONO" -> "La deuda de ${rec.nombreMoto} volverá a subir S/ ${"%.2f".format(rec.monto)} y ese dinero sale de Caja."
            else -> "La garantía anulada de ${rec.nombreMoto} (S/ ${"%.2f".format(rec.monto)}) volverá a ser deuda."
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { recordToUndo = null },
            title = { Text(if (rec.tipo == "ANULADO") "¿Deshacer la anulación?" else "¿Anular este ${if (rec.tipo == "CARGO") "cargo" else "abono"}?") },
            text = { Text("${rec.motivo}\n${rec.fecha}\n\n$efecto", fontSize = com.example.ui.theme.AppText.Body) },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { viewModel.deleteDebtRecord(rec); recordToUndo = null },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.DangerRed)
                ) { Text(if (rec.tipo == "ANULADO") "Deshacer" else "Anular") }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { recordToUndo = null }) { Text("Volver") }
            }
        )
    }

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
                        Column(modifier = Modifier.weight(1f)) {
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
                        Text("Ver todas", fontSize = AppText.Body)
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
                DebtRecordRow(
                    record = record,
                    onClick = if (state.isAdmin) ({ recordToUndo = record }) else null
                )
            }
        }
    }
}

@Composable
internal fun DebtUnitCard(
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
                            fontSize = AppText.Heading
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
                    Text("+ Cargar Deuda", fontSize = AppText.Body, fontWeight = FontWeight.Bold)
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
                    Text("Abonar", fontSize = AppText.Body, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun DebtRecordRow(record: DebtRecordEntity, onClick: (() -> Unit)? = null) {
    val isCargo = record.tipo == "CARGO"

    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
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
                // Dos líneas cortas (antes eran tres): qué y de qué moto / motivo y fecha
                Text(
                    text = "${when { isCargo -> "Cargo"; record.tipo == "ANULADO" -> "Anulado"; else -> "Abono" }} · ${record.nombreMoto}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(record.motivo, record.fecha).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Text(
                text = "${if (isCargo) "+" else "-"} S/ ${"%.2f".format(record.monto)}",
                style = MaterialTheme.typography.titleSmall,
                color = if (isCargo) WarningAmberText else SuccessGreenText
            )
        }
    }
}
