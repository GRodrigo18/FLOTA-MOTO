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
// 1. DIÁLOGO: AGREGAR UNIDAD (MOTO)
// ----------------------------------------------------
@Composable
fun AddUnitDialog(
    unitCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (
        nombreMoto: String,
        placa: String,
        cliente: String,
        telefono: String,
        cuotaDiaria: Double,
        garantiaTotal: Double,
        garantiaInicial: Double,
        deudaGarantia: Double,
        inicio: String,
        fin: String,
        tipoDuracion: String,
        soatInicio: String,
        soatFin: String,
        obs: String,
        revisionAlDia: Boolean
    ) -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayCal = remember { Calendar.getInstance() }
    val todayStr = remember { sdf.format(todayCal.time) }
    
    val defaultFinStr = remember {
        val c = Calendar.getInstance()
        c.add(Calendar.MONTH, 3)
        sdf.format(c.time)
    }

    val defaultSoatFinStr = remember {
        val c = Calendar.getInstance()
        c.add(Calendar.YEAR, 1)
        sdf.format(c.time)
    }

    var nombreMoto by remember { mutableStateOf("Moto ${unitCount + 1}") }
    var placa by remember { mutableStateOf("") }
    var cliente by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var cuotaText by remember { mutableStateOf("35.0") }
    var garantiaTotalText by remember { mutableStateOf("200.0") }
    var garantiaInicialText by remember { mutableStateOf("100.0") }
    var tipoDuracion by remember { mutableStateOf("3 Meses") }
    var inicioContrato by remember { mutableStateOf(todayStr) }
    var finContrato by remember { mutableStateOf(defaultFinStr) }
    var soatInicio by remember { mutableStateOf(todayStr) }
    var soatFin by remember { mutableStateOf(defaultSoatFinStr) }
    var obs by remember { mutableStateOf("Torito Bajaj / TVS") }
    var revisionTecnicaAlDia by remember { mutableStateOf(true) }

    // Validación de duración de contrato: Mínimo 1 mes (30 días)
    val contractDays = remember(inicioContrato, finContrato) {
        try {
            val dIni = sdf.parse(inicioContrato.trim())
            val dFin = sdf.parse(finContrato.trim())
            if (dIni != null && dFin != null) {
                java.util.concurrent.TimeUnit.MILLISECONDS.toDays(dFin.time - dIni.time)
            } else -1L
        } catch (_: Exception) {
            -1L
        }
    }
    val isContractValid = contractDays > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ElectricRickshaw, contentDescription = null, tint = NavyPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrar Nueva Moto", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nombreMoto,
                    onValueChange = { nombreMoto = it },
                    label = { Text("Nombre de la Moto (Ej. Moto 1, Moto 2)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_unit_name_input")
                )
                OutlinedTextField(
                    value = placa,
                    onValueChange = { placa = it.uppercase() },
                    label = { Text("Placa (Ej. ABC-123)") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_unit_placa_input")
                )
                OutlinedTextField(
                    value = cliente,
                    onValueChange = { if (it.length <= 60) cliente = it },
                    label = { Text("Chofer (vacío si aún no tiene)") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_unit_chofer_input")
                )
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { v -> telefono = v.filter { it.isDigit() || it == '+' || it == ' ' }.take(15) },
                    label = { Text("Teléfono del chofer") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_unit_phone_input")
                )
                OutlinedTextField(
                    value = cuotaText,
                    onValueChange = { cuotaText = it },
                    label = { Text("Cuota Diaria (S/)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = garantiaTotalText,
                        onValueChange = { garantiaTotalText = it },
                        label = { Text("Garantía Total (S/)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = garantiaInicialText,
                        onValueChange = { garantiaInicialText = it },
                        label = { Text("Garantía Pagada (S/)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                val garTotal = garantiaTotalText.replace(',', '.').toDoubleOrNull() ?: 0.0
                val garIni = garantiaInicialText.replace(',', '.').toDoubleOrNull() ?: 0.0
                // Sin chofer no se cobra garantía (la app no registra esa deuda)
                val sinChofer = cliente.isBlank() || cliente.trim().equals("Sin asignar", ignoreCase = true)
                val deudaGar = if (sinChofer) 0.0 else (garTotal - garIni).coerceAtLeast(0.0)
                if (sinChofer) {
                    Text(
                        text = "Sin chofer: no se cobra garantía ni se registra deuda.",
                        fontSize = AppText.Small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (deudaGar > 0) WarningAmberBg else SuccessGreenBg,
                    border = BorderStroke(1.dp, if (deudaGar > 0) WarningAmberText.copy(alpha = 0.3f) else SuccessGreenText.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Garantía pactada: S/ ${"%.2f".format(garTotal)}",
                                fontSize = AppText.Small,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pagado hoy: S/ ${"%.2f".format(garIni)}",
                                fontSize = AppText.Small,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreenText
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (deudaGar > 0) {
                                "Saldo restante: S/ ${"%.2f".format(deudaGar)} de deuda a cobrar en partes."
                            } else {
                                "Garantía cancelada completa (S/ 0.00 de deuda inicial)."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (deudaGar > 0) WarningAmberText else SuccessGreenText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Contrato: modalidad + día de inicio; el fin se calcula solo (ej. 3 meses)
                ContractDatesSection(tipoDuracion, inicioContrato, finContrato) { t, ini, fin ->
                    tipoDuracion = t
                    inicioContrato = ini
                    finContrato = fin
                }

                Text(
                    text = "SOAT y estado técnico inicial:",
                    fontSize = AppText.Body,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SoatDatesSection(soatInicio, soatFin) { ini, fin ->
                    soatInicio = ini
                    soatFin = fin
                }

                // Switch de Revisión Técnica al Día para no disparar alertas espurias
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¿Revisión Técnica al Día al Ingresar?",
                                fontSize = AppText.Body,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (revisionTecnicaAlDia) "Al día ✅ (Ingresa sin alertas pendientes)" else "Pendiente ⚠️ (Requiere revisión)",
                                fontSize = AppText.Small,
                                color = if (revisionTecnicaAlDia) SuccessGreenText else WarningAmberText
                            )
                        }
                        Switch(
                            checked = revisionTecnicaAlDia,
                            onCheckedChange = { revisionTecnicaAlDia = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanAccent,
                                checkedTrackColor = NavyPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = obs,
                    onValueChange = { obs = it },
                    label = { Text("Observaciones / Modelo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (placa.isNotBlank() && isContractValid) {
                        val cuota = cuotaText.replace(',', '.').toDoubleOrNull() ?: 35.0
                        val garTotal = garantiaTotalText.replace(',', '.').toDoubleOrNull() ?: 0.0
                        val garIni = garantiaInicialText.replace(',', '.').toDoubleOrNull() ?: 0.0
                        val deudaGar = (garTotal - garIni).coerceAtLeast(0.0)
                        onConfirm(
                            nombreMoto,
                            placa,
                            cliente,
                            telefono,
                            cuota,
                            garTotal,
                            garIni,
                            deudaGar,
                            inicioContrato,
                            finContrato,
                            tipoDuracion,
                            soatInicio,
                            soatFin,
                            obs,
                            revisionTecnicaAlDia
                        )
                    }
                },
                enabled = placa.isNotBlank() && isContractValid,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Guardar Moto")
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
// 5. DIÁLOGO: EDITAR CONTRATO Y SOAT MODIFICABLE
// ----------------------------------------------------
@Composable
fun EditContractSoatDialog(
    unit: FleetUnitEntity,
    onDismiss: () -> Unit,
    onConfirm: (
        inicioContrato: String,
        finContrato: String,
        tipoDuracion: String,
        soatInicio: String,
        soatFin: String,
        chofer: String,
        telefono: String,
        cuotaDiaria: Double,
        nuevoEstado: String,
        revisionTecnicaRealizada: Boolean,
        nuevoNombre: String,
        nuevaPlaca: String
    ) -> Unit
) {
    var nombreMoto by remember { mutableStateOf(unit.nombreMoto) }
    var placa by remember { mutableStateOf(unit.placa) }
    var tipoDuracion by remember { mutableStateOf(unit.tipoDuracion) }
    var inicioContrato by remember { mutableStateOf(unit.inicioContrato) }
    var finContrato by remember { mutableStateOf(unit.finContrato) }
    var soatInicio by remember { mutableStateOf(unit.soatInicio) }
    var soatFin by remember { mutableStateOf(unit.soatFin) }
    var chofer by remember { mutableStateOf(unit.cliente) }
    var telefono by remember { mutableStateOf(unit.telefono) }
    var cuotaText by remember { mutableStateOf(unit.cuotaDiaria.toString()) }
    var estadoSeleccionado by remember { mutableStateOf(unit.estado) }
    var revisionTecnicaRealizada by remember { mutableStateOf(unit.revisionTecnicaMesRealizada) }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val contractDays = remember(inicioContrato, finContrato) {
        try {
            val dIni = sdf.parse(inicioContrato.trim())
            val dFin = sdf.parse(finContrato.trim())
            if (dIni != null && dFin != null) {
                java.util.concurrent.TimeUnit.MILLISECONDS.toDays(dFin.time - dIni.time)
            } else -1L
        } catch (_: Exception) {
            -1L
        }
    }
    val isContractValid = contractDays > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar ${unit.nombreMoto}", fontWeight = FontWeight.Bold, fontSize = AppText.Heading)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Nombre y placa (si se cambian, se actualiza todo su historial)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nombreMoto,
                        onValueChange = { if (it.length <= 30) nombreMoto = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_unit_name")
                    )
                    OutlinedTextField(
                        value = placa,
                        onValueChange = { if (it.length <= 10) placa = it.uppercase() },
                        label = { Text("Placa") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f).testTag("edit_unit_plate")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chofer,
                        onValueChange = { if (it.length <= 60) chofer = it },
                        label = { Text("Chofer") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.weight(1f).testTag("edit_unit_chofer")
                    )
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { v -> telefono = v.filter { it.isDigit() || it == '+' || it == ' ' }.take(15) },
                        label = { Text("Teléfono") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f).testTag("edit_unit_phone")
                    )
                }

                // Selector de Estado Operativo de la Moto
                Text(
                    text = "Estado Operativo de la Moto:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CyanAccent)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val opciones = listOf("Vigente", "En Taller", "Inactivo")
                    opciones.forEachIndexed { index, opt ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = opciones.size),
                            onClick = { estadoSeleccionado = opt },
                            selected = estadoSeleccionado == opt
                        ) {
                            Text(opt, fontSize = AppText.Small, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (estadoSeleccionado == "Inactivo" && unit.estado != "Inactivo") {
                    Text(
                        text = if (unit.deudaTotal > 0.004) {
                            "Inactiva no paga cuota ni garantía. Se anula toda su deuda (S/ ${"%.2f".format(unit.deudaTotal)}); queda en el historial de Deudas."
                        } else {
                            "Inactiva no paga cuota ni garantía."
                        },
                        fontSize = AppText.Small,
                        color = WarningAmberText
                    )
                }

                // Control de Revisión Técnica del Mes (Sí / No)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (revisionTecnicaRealizada) SuccessGreen.copy(alpha = 0.6f) else SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { revisionTecnicaRealizada = !revisionTecnicaRealizada }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Revisión Técnica de Este Mes",
                                fontWeight = FontWeight.Bold,
                                fontSize = AppText.Body,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (revisionTecnicaRealizada) "Completada este mes (Al día ✅)" else "Pendiente de realizar este mes ⏳",
                                fontSize = AppText.Small,
                                color = if (revisionTecnicaRealizada) SuccessGreenText else WarningAmberText
                            )
                        }
                        Switch(
                            checked = revisionTecnicaRealizada,
                            onCheckedChange = { revisionTecnicaRealizada = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = SuccessGreen)
                        )
                    }
                }

                OutlinedTextField(
                    value = cuotaText,
                    onValueChange = { cuotaText = it },
                    label = { Text("Cuota Diaria (S/)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Contrato: al cambiar la modalidad o el inicio, el fin se recalcula solo
                ContractDatesSection(tipoDuracion, inicioContrato, finContrato) { t, ini, fin ->
                    tipoDuracion = t
                    inicioContrato = ini
                    finContrato = fin
                }

                Text(
                    text = "SOAT (vigencia legal):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CyanAccent)
                )
                SoatDatesSection(soatInicio, soatFin) { ini, fin ->
                    soatInicio = ini
                    soatFin = fin
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cuota = cuotaText.replace(',', '.').toDoubleOrNull() ?: unit.cuotaDiaria
                    onConfirm(
                        inicioContrato,
                        finContrato,
                        tipoDuracion,
                        soatInicio,
                        soatFin,
                        chofer,
                        telefono,
                        cuota,
                        estadoSeleccionado,
                        revisionTecnicaRealizada,
                        nombreMoto,
                        placa
                    )
                },
                enabled = isContractValid && nombreMoto.isNotBlank() && placa.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
            ) {
                Text("Guardar Cambios", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
