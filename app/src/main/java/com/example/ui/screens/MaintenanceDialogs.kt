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
// 4. DIÁLOGO: MANTENIMIENTO O MODIFICACIÓN POR MOTO (MULTI-SERVICIO)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterDetailedMaintenanceDialog(
    units: List<FleetUnitEntity>,
    preselectedUnit: FleetUnitEntity?,
    stockLlantasDisponibles: Int = 0,
    precioLlantaRef: Double = 80.0,
    precioAceiteRef: Double = 19.0,
    onDismiss: () -> Unit,
    onConfirm: (
        unit: FleetUnitEntity,
        tipoTrabajo: String,
        detallesDescripcion: String,
        costoTrabajo: Double,
        fecha: String,
        dejarEnTaller: Boolean,
        botellasAceite: Int,
        precioBotellaAceite: Double,
        llantasUsadas: Int,
        precioLlantaCustom: Double,
        costoGeneral: Double,
        costoFrenos: Double,
        costoLlantas: Double,
        costoCana: Double,
        costoPlanchado: Double,
        costoCablesCambio: Double,
        costoBajadaMotorCompleto: Double,
        costoBajadaMedioMotor: Double,
        costoOtros: Double
    ) -> Unit
) {
    var selectedUnit by remember { mutableStateOf(preselectedUnit ?: units.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    // Servicios múltiples combinables
    var checkGeneral by remember { mutableStateOf(true) }
    var costoGeneralText by remember { mutableStateOf("40.0") }
    // Se guarda el texto: así se puede borrar el número y escribir otro (antes volvía a "2" solo)
    var botellasAceiteText by remember { mutableStateOf("2") }
    val botellasAceiteCount = botellasAceiteText.toIntOrNull()?.coerceIn(0, 99) ?: 0
    var precioBotellaAceiteText by remember { mutableStateOf("%.2f".format(precioAceiteRef)) }

    var checkCana by remember { mutableStateOf(false) }
    var costoCanaText by remember { mutableStateOf("40.0") }

    var checkFrenos by remember { mutableStateOf(false) }
    var costoFrenosText by remember { mutableStateOf("30.0") }

    var checkPlanchado by remember { mutableStateOf(false) }
    var costoPlanchadoText by remember { mutableStateOf("200.0") }

    var checkCables by remember { mutableStateOf(false) }
    var costoCablesText by remember { mutableStateOf("") }

    var checkLlantas by remember { mutableStateOf(false) }
    var cantidadLlantasText by remember { mutableStateOf("1") }
    val cantidadLlantasCount = cantidadLlantasText.toIntOrNull()?.coerceIn(0, 99) ?: 0
    var precioLlantaText by remember { mutableStateOf("%.2f".format(precioLlantaRef)) }

    var checkMotorCompleto by remember { mutableStateOf(false) }
    var costoMotorCompletoText by remember { mutableStateOf("650.0") }

    var checkMedioMotor by remember { mutableStateOf(false) }
    var costoMedioMotorText by remember { mutableStateOf("380.0") }

    var checkOtros by remember { mutableStateOf(false) }
    var costoOtrosText by remember { mutableStateOf("50.0") }
    var otrosDescripcion by remember { mutableStateOf("") }

    var dejarEnTaller by remember { mutableStateOf(selectedUnit?.estado == "En Taller") }
    var detallesExtra by remember { mutableStateOf("") }

    val sdfFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val hoy = remember { sdfFecha.format(Date()) }

    // Cálculos dinámicos
    val precioBotellaEfectivo = precioBotellaAceiteText.replace(',', '.').toDoubleOrNull() ?: precioAceiteRef
    val subtotalAceite = if (checkGeneral) (botellasAceiteCount * precioBotellaEfectivo) else 0.0
    val costoGeneralVal = if (checkGeneral) (costoGeneralText.replace(',', '.').toDoubleOrNull() ?: 0.0) else 0.0

    val costoCanaVal = if (checkCana) (costoCanaText.replace(',', '.').toDoubleOrNull() ?: 0.0) else 0.0
    val costoFrenosVal = if (checkFrenos) (costoFrenosText.replace(',', '.').toDoubleOrNull() ?: 0.0) else 0.0
    val costoPlanchadoVal = if (checkPlanchado) (costoPlanchadoText.replace(',', '.').toDoubleOrNull() ?: 0.0) else 0.0
    val costoCablesVal = if (checkCables) (costoCablesText.replace(',', '.').toDoubleOrNull() ?: 0.0) else 0.0

    val precioLlantaEfectivo = precioLlantaText.replace(',', '.').toDoubleOrNull() ?: precioLlantaRef
    val subtotalLlantas = if (checkLlantas) (cantidadLlantasCount * precioLlantaEfectivo) else 0.0

    val costoMotorCompletoVal = if (checkMotorCompleto) (costoMotorCompletoText.replace(',', '.').toDoubleOrNull() ?: 0.0) else 0.0
    val costoMedioMotorVal = if (checkMedioMotor) (costoMedioMotorText.replace(',', '.').toDoubleOrNull() ?: 0.0) else 0.0
    val costoOtrosVal = if (checkOtros) (costoOtrosText.replace(',', '.').toDoubleOrNull() ?: 0.0) else 0.0

    val totalCosto = costoGeneralVal + subtotalAceite + costoCanaVal + costoFrenosVal + costoPlanchadoVal + costoCablesVal +
            subtotalLlantas + costoMotorCompletoVal + costoMedioMotorVal + costoOtrosVal

    // Título descriptivo combinado
    val listaServicios = mutableListOf<String>()
    if (checkGeneral) listaServicios.add("General")
    if (checkCana) listaServicios.add("Caña")
    if (checkFrenos) listaServicios.add("Frenos")
    if (checkPlanchado) listaServicios.add("Planchado")
    if (checkCables) listaServicios.add("Cables de cambios")
    if (checkLlantas) listaServicios.add("$cantidadLlantasCount Llanta(s)")
    if (checkMotorCompleto) listaServicios.add("Motor Completo")
    if (checkMedioMotor) listaServicios.add("Medio Motor")
    if (checkOtros) listaServicios.add(otrosDescripcion.ifBlank { "Otros" })
    val tipoTrabajoCombinado = if (listaServicios.isNotEmpty()) listaServicios.joinToString(" + ") else "Mantenimiento"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrar Mantenimiento", fontWeight = FontWeight.Bold, fontSize = AppText.Heading)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selector de Moto
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedUnit?.nombreMoto ?: "Selecciona Moto"} (${selectedUnit?.placa ?: ""})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Moto a Realizar Trabajos") },
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
                                text = { Text("${unit.nombreMoto} (${unit.placa}) - Estado: ${unit.estado}") },
                                onClick = {
                                    selectedUnit = unit
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Seleccione los trabajos realizados (puede marcar varios):",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                )

                // 1. Mantenimiento General
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (checkGeneral) CyanAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, if (checkGeneral) CyanAccent.copy(alpha = 0.5f) else SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checkGeneral = !checkGeneral },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checkGeneral,
                                onCheckedChange = { checkGeneral = it },
                                colors = CheckboxDefaults.colors(checkedColor = CyanAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mantenimiento General", fontWeight = FontWeight.Bold, fontSize = AppText.Body)
                        }
                        if (checkGeneral) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = costoGeneralText,
                                onValueChange = { costoGeneralText = it },
                                label = { Text("Mano de obra General (S/)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Botellas de Aceite y Precio Unitario Editable
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = botellasAceiteText,
                                    onValueChange = { botellasAceiteText = it.filter { c -> c.isDigit() }.take(2) },
                                    label = { Text("Botellas Aceite") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = precioBotellaAceiteText,
                                    onValueChange = { precioBotellaAceiteText = it },
                                    label = { Text("Precio x Botella (S/)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Text(
                                text = "Aceite a descontar: $botellasAceiteCount bot. x S/ ${"%.2f".format(precioBotellaEfectivo)} = S/ ${"%.2f".format(subtotalAceite)}",
                                fontSize = AppText.Small,
                                color = CyanAccent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // 2. Caña / Horquilla
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (checkCana) CyanAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, if (checkCana) CyanAccent.copy(alpha = 0.5f) else SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checkCana = !checkCana },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checkCana, onCheckedChange = { checkCana = it }, colors = CheckboxDefaults.colors(checkedColor = CyanAccent))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Caña / Horquilla Delantera", fontWeight = FontWeight.Bold, fontSize = AppText.Body)
                        }
                        if (checkCana) {
                            OutlinedTextField(
                                value = costoCanaText,
                                onValueChange = { costoCanaText = it },
                                label = { Text("Costo Caña (S/)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // 3. Frenos
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (checkFrenos) CyanAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, if (checkFrenos) CyanAccent.copy(alpha = 0.5f) else SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checkFrenos = !checkFrenos },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checkFrenos, onCheckedChange = { checkFrenos = it }, colors = CheckboxDefaults.colors(checkedColor = CyanAccent))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Frenos (Delanteros / Traseros)", fontWeight = FontWeight.Bold, fontSize = AppText.Body)
                        }
                        if (checkFrenos) {
                            OutlinedTextField(
                                value = costoFrenosText,
                                onValueChange = { costoFrenosText = it },
                                label = { Text("Costo Frenos (S/)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // 4. Planchado
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (checkPlanchado) CyanAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, if (checkPlanchado) CyanAccent.copy(alpha = 0.5f) else SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checkPlanchado = !checkPlanchado },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checkPlanchado, onCheckedChange = { checkPlanchado = it }, colors = CheckboxDefaults.colors(checkedColor = CyanAccent))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Planchado / Carrocería", fontWeight = FontWeight.Bold, fontSize = AppText.Body)
                        }
                        if (checkPlanchado) {
                            OutlinedTextField(
                                value = costoPlanchadoText,
                                onValueChange = { costoPlanchadoText = it },
                                label = { Text("Costo Planchado (S/)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // 4b. Cables de cambios (pedido del cliente: debajo de Planchado)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (checkCables) CyanAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, if (checkCables) CyanAccent.copy(alpha = 0.5f) else SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checkCables = !checkCables },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checkCables, onCheckedChange = { checkCables = it }, colors = CheckboxDefaults.colors(checkedColor = CyanAccent))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cables de cambios", fontWeight = FontWeight.Bold, fontSize = AppText.Body)
                        }
                        if (checkCables) {
                            OutlinedTextField(
                                value = costoCablesText,
                                onValueChange = { costoCablesText = it },
                                label = { Text("Costo cables de cambios (S/)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // 5. Neumáticos / Llantas
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (checkLlantas) CyanAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, if (checkLlantas) CyanAccent.copy(alpha = 0.5f) else SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checkLlantas = !checkLlantas },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checkLlantas, onCheckedChange = { checkLlantas = it }, colors = CheckboxDefaults.colors(checkedColor = CyanAccent))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Llantas del Inventario de Taller", fontWeight = FontWeight.Bold, fontSize = AppText.Body)
                                Text("Disponibles en taller: $stockLlantasDisponibles llantas", fontSize = AppText.Small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (checkLlantas) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = cantidadLlantasText,
                                    onValueChange = { cantidadLlantasText = it.filter { c -> c.isDigit() }.take(2) },
                                    label = { Text("Cant. Llantas") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = precioLlantaText,
                                    onValueChange = { precioLlantaText = it },
                                    label = { Text("Precio Llanta (S/)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Text(
                                text = "Llantas: $cantidadLlantasCount x S/ ${"%.2f".format(precioLlantaEfectivo)} = S/ ${"%.2f".format(subtotalLlantas)}",
                                fontSize = AppText.Small,
                                color = CyanAccent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // 6. Bajada de motor / Otros
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (checkMotorCompleto || checkMedioMotor || checkOtros) CyanAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checkMotorCompleto, onCheckedChange = { checkMotorCompleto = it })
                            Text("Bajada Motor Completo", fontSize = AppText.Body, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            if (checkMotorCompleto) {
                                OutlinedTextField(
                                    value = costoMotorCompletoText,
                                    onValueChange = { costoMotorCompletoText = it },
                                    modifier = Modifier.width(100.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checkMedioMotor, onCheckedChange = { checkMedioMotor = it })
                            Text("Bajada Medio Motor", fontSize = AppText.Body, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            if (checkMedioMotor) {
                                OutlinedTextField(
                                    value = costoMedioMotorText,
                                    onValueChange = { costoMedioMotorText = it },
                                    modifier = Modifier.width(100.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checkOtros, onCheckedChange = { checkOtros = it })
                            Text("Otros Trabajos", fontSize = AppText.Body, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            if (checkOtros) {
                                OutlinedTextField(
                                    value = costoOtrosText,
                                    onValueChange = { costoOtrosText = it },
                                    modifier = Modifier.width(100.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                        if (checkOtros) {
                            OutlinedTextField(
                                value = otrosDescripcion,
                                onValueChange = { otrosDescripcion = it },
                                label = { Text("Descripción de otros trabajos") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // Observaciones adicionales
                OutlinedTextField(
                    value = detallesExtra,
                    onValueChange = { detallesExtra = it },
                    label = { Text("Detalles técnicos u observaciones adicionales") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Dejar en taller
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dejarEnTaller = !dejarEnTaller },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = dejarEnTaller, onCheckedChange = { dejarEnTaller = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dejar moto con estado \"En Taller\"", fontSize = AppText.Body, fontWeight = FontWeight.SemiBold)
                }

                // Tarjeta de Resumen Total Combinado
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SuccessGreenBg,
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL MANTENIMIENTO:", fontWeight = FontWeight.ExtraBold, color = SuccessGreenText, fontSize = AppText.Body)
                            Text("S/ ${"%.2f".format(totalCosto)}", fontWeight = FontWeight.ExtraBold, color = SuccessGreenText, fontSize = AppText.Heading)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Servicios: $tipoTrabajoCombinado",
                            fontSize = AppText.Small,
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessGreenText.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unit = selectedUnit
                    if (unit != null) {
                        onConfirm(
                            unit,
                            tipoTrabajoCombinado,
                            detallesExtra,
                            totalCosto,
                            hoy,
                            dejarEnTaller,
                            if (checkGeneral) botellasAceiteCount else 0,
                            precioBotellaEfectivo,
                            if (checkLlantas) cantidadLlantasCount else 0,
                            precioLlantaEfectivo,
                            costoGeneralVal,
                            costoFrenosVal,
                            0.0, // las llantas van solo como "llantas usadas" (antes se cobraban dos veces)
                            costoCanaVal,
                            costoPlanchadoVal,
                            costoCablesVal,
                            costoMotorCompletoVal,
                            costoMedioMotorVal,
                            costoOtrosVal
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
            ) {
                Text("Guardar Mantenimiento (S/ ${"%.2f".format(totalCosto)})", fontWeight = FontWeight.Bold)
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
fun ScheduleMaintenanceDialog(
    units: List<FleetUnitEntity>,
    onDismiss: () -> Unit,
    onConfirm: (placa: String, motivo: String, urgencia: String, fecha: String, costo: Double) -> Unit
) {
    var placa by remember { mutableStateOf(units.firstOrNull()?.placa ?: "") }
    var motivo by remember { mutableStateOf("") }
    var urgencia by remember { mutableStateOf("Media") }
    var fecha by remember { mutableStateOf(com.example.util.FleetDates.today()) }
    var costoText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Programar Mantenimiento", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = placa,
                    onValueChange = { placa = it.uppercase() },
                    label = { Text("Placa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DatePickField(
                    label = "Fecha",
                    value = fecha,
                    onPick = { fecha = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costoText,
                    onValueChange = { costoText = it },
                    label = { Text("Costo estimado (S/)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = placa.isNotBlank() && motivo.isNotBlank() && com.example.util.FleetDates.normalize(fecha) != null,
                onClick = {
                    val costo = costoText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    onConfirm(placa, motivo.trim(), urgencia, com.example.util.FleetDates.normalize(fecha) ?: fecha, costo)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Programar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
