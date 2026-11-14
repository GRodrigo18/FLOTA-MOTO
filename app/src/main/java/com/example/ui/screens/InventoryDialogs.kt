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
// 6. DIÁLOGO: COMPRA DE BOTELLAS DE ACEITE
// ----------------------------------------------------
@Composable
fun BuyOilDialog(
    onDismiss: () -> Unit,
    onConfirm: (botellas: Int, precioUnitario: Double) -> Unit
) {
    var botellasText by remember { mutableStateOf("") }
    var precioUnitText by remember { mutableStateOf("25.0") }

    val botellas = botellasText.toIntOrNull() ?: 0
    val precio = precioUnitText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val totalEgreso = botellas * precio

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.OilBarrel, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Comprar Lote de Botellas de Aceite", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "El inventario se controla por botellas individuales para permitir el descuento de 2 botellas en cada servicio.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                OutlinedTextField(
                    value = botellasText,
                    onValueChange = { botellasText = it },
                    label = { Text("Cantidad de Botellas a Comprar (Ej. 100)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_oil_bottles")
                )

                OutlinedTextField(
                    value = precioUnitText,
                    onValueChange = { precioUnitText = it },
                    label = { Text("Precio por Botella (S/)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_oil_price")
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WarningAmberBg,
                    border = BorderStroke(1.dp, WarningAmberText.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "TOTAL EGRESO A REGISTRAR EN CAJA:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = WarningAmberText)
                        )
                        Text(
                            text = "S/ ${"%.2f".format(totalEgreso)}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = WarningAmberText)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (botellas > 0 && precio > 0.0) {
                        onConfirm(botellas, precio)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text("Confirmar Compra", color = Color.Black, fontWeight = FontWeight.Bold)
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
// 6B. DIÁLOGO: COMPRA DE LOTE DE LLANTAS (NEUMÁTICOS)
// ----------------------------------------------------
@Composable
fun BuyTiresDialog(
    onDismiss: () -> Unit,
    onConfirm: (llantas: Int, precioUnitario: Double) -> Unit
) {
    var llantasText by remember { mutableStateOf("") }
    var precioUnitText by remember { mutableStateOf("85.0") }

    val llantas = llantasText.toIntOrNull() ?: 0
    val precio = precioUnitText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val totalEgreso = llantas * precio

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Comprar Lote de Llantas para Taller", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "El inventario de llantas se almacena en el taller. Cuando una moto solicite cambio de llantas en mantenimiento, se descontará del inventario.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                OutlinedTextField(
                    value = llantasText,
                    onValueChange = { llantasText = it },
                    label = { Text("Cantidad de Llantas a Comprar (Ej. 10)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = precioUnitText,
                    onValueChange = { precioUnitText = it },
                    label = { Text("Precio Unitario por Llanta (S/ Ej. 85.00)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WarningAmberBg,
                    border = BorderStroke(1.dp, WarningAmberText.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "TOTAL EGRESO A REGISTRAR EN CAJA:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = WarningAmberText)
                        )
                        Text(
                            text = "S/ ${"%.2f".format(totalEgreso)}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = WarningAmberText)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (llantas > 0 && precio > 0.0) {
                        onConfirm(llantas, precio)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text("Confirmar Compra", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun RestockItemDialog(
    item: InventoryItemEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var stockText by remember { mutableStateOf(item.stock.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar Stock: ${item.nombre}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Ingresa la cantidad exacta en bodega (${item.unidadMedida}):")
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it },
                    label = { Text("Stock Actual") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stock = stockText.toIntOrNull() ?: item.stock
                    onConfirm(stock)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
