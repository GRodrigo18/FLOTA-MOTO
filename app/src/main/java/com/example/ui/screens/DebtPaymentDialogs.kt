package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DetailedMaintenanceEntity
import com.example.data.model.FleetUnitEntity
import com.example.data.model.InventoryItemEntity
import com.example.ui.DialogType
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.ui.theme.AppText
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedBg
import com.example.ui.theme.DangerRedText
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import com.example.ui.theme.WarningAmberText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ----------------------------------------------------
// 2. DIÁLOGO: AGREGAR CARGO / DEUDA A UNA MOTO
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtCargoDialog(
    units: List<FleetUnitEntity>,
    preselectedUnit: FleetUnitEntity?,
    onDismiss: () -> Unit,
    onConfirm: (unit: FleetUnitEntity, monto: Double, motivo: String) -> Unit
) {
    var selectedUnit by remember { mutableStateOf(preselectedUnit ?: units.firstOrNull()) }
    var montoText by remember { mutableStateOf("100.0") }
    var motivo by remember { mutableStateOf("Saldo de Garantía restante") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = WarningAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text("+ Agregar Deuda / Cargo", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector de Moto
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedUnit?.nombreMoto ?: "Selecciona"} (${selectedUnit?.placa ?: ""})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Moto a Cargar") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.nombreMoto} (${unit.placa})") },
                                onClick = {
                                    selectedUnit = unit
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it },
                    label = { Text("Monto de la Deuda a Cargar (S/)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_debt_cargo_amount")
                )

                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo / Concepto del Cargo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Sugerencias rápidas de motivos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { motivo = "Garantía pendiente de pago" }
                    ) {
                        Text("Garantía", fontSize = AppText.Small, modifier = Modifier.padding(6.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { motivo = "Repuesto / Taller adelantado" }
                    ) {
                        Text("Repuestos", fontSize = AppText.Small, modifier = Modifier.padding(6.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { motivo = "Choque / Planchado" }
                    ) {
                        Text("Choque", fontSize = AppText.Small, modifier = Modifier.padding(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unit = selectedUnit
                    val monto = montoText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (unit != null && monto > 0.0 && motivo.isNotBlank()) {
                        onConfirm(unit, monto, motivo)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
            ) {
                Text("Registrar Cargo", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ----------------------------------------------------
// 3. DIÁLOGO: REGISTRAR ABONO A DEUDA
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtAbonoDialog(
    units: List<FleetUnitEntity>,
    preselectedUnit: FleetUnitEntity?,
    onDismiss: () -> Unit,
    onConfirm: (unit: FleetUnitEntity, monto: Double, motivo: String) -> Unit
) {
    var selectedUnit by remember { mutableStateOf(preselectedUnit ?: units.firstOrNull()) }
    var montoText by remember { mutableStateOf("50.0") }
    var motivo by remember { mutableStateOf("Abono a garantía / deuda") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("- Registrar Abono de Chofer", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedUnit?.nombreMoto ?: "Selecciona"} (${selectedUnit?.placa ?: ""}) - Saldo: S/ ${"%.2f".format(selectedUnit?.deudaTotal ?: 0.0)}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Moto / Chofer que Abona") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.nombreMoto} (${unit.placa}) - Saldo Deuda: S/ ${"%.2f".format(unit.deudaTotal)}") },
                                onClick = {
                                    selectedUnit = unit
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it },
                    label = { Text("Monto Abonado (S/)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_debt_abono_amount")
                )

                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Detalle del Abono") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SuccessGreenBg,
                    border = BorderStroke(1.dp, SuccessGreenText.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Este abono reduce la deuda de la moto y se registra automáticamente como INGRESO en la Caja de Finanzas.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SuccessGreenText,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unit = selectedUnit
                    val monto = montoText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (unit != null && monto > 0.0) {
                        onConfirm(unit, monto, motivo)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Text("Registrar Abono", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ----------------------------------------------------
// 7. DIÁLOGOS EXISTENTES: MOVIMIENTOS, COBROS, GASTOS
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    units: List<FleetUnitEntity>,
    preselectedUnit: FleetUnitEntity?,
    onDismiss: () -> Unit,
    onConfirm: (FleetUnitEntity, Double, Boolean) -> Unit
) {
    var selectedUnit by remember { mutableStateOf(preselectedUnit ?: units.firstOrNull()) }
    var amountText by remember { mutableStateOf(selectedUnit?.cuotaDiaria?.let { "%.2f".format(java.util.Locale.US, it) } ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var restoADeuda by remember { mutableStateOf(true) }
    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val falta = selectedUnit?.let { u -> amount?.let { com.example.util.DebtMath.faltante(u.cuotaDiaria, it) } } ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cobro Directo de Cuota", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedUnit?.nombreMoto ?: "Selecciona"} (${selectedUnit?.placa ?: ""})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Moto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.nombreMoto} (${unit.placa}) - S/ ${unit.cuotaDiaria}") },
                                onClick = {
                                    selectedUnit = unit
                                    amountText = "%.2f".format(java.util.Locale.US, unit.cuotaDiaria)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto Recaudado (S/)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = amountText.isNotBlank() && (amount == null || amount <= 0.0),
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )
                // Pagó menos que la cuota: lo que falta puede quedar como deuda (opcional)
                if (falta > 0.0 && amount != null && amount > 0.0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restoADeuda = !restoADeuda }
                    ) {
                        androidx.compose.material3.Checkbox(checked = restoADeuda, onCheckedChange = { restoADeuda = it })
                        Text(
                            "Pasar los S/ ${"%.2f".format(falta)} que faltan a su deuda",
                            fontSize = com.example.ui.theme.AppText.Body
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedUnit != null && amount != null && amount > 0.0,
                onClick = {
                    val unit = selectedUnit
                    if (unit != null && amount != null && amount > 0.0) {
                        onConfirm(unit, amount, restoADeuda && falta > 0.0)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Text("Registrar Cobro", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
