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

@Composable
fun FleetDialogHost(
    state: FleetUiState,
    viewModel: FleetViewModel
) {
    when (state.activeDialog) {
        DialogType.ADD_UNIT -> AddUnitDialog(
            unitCount = state.units.size,
            onDismiss = { viewModel.closeDialog() },
            onConfirm = { nombreMoto, placa, cliente, tel, cuota, garantiaTotal, garantiaIni, deudaGarantia, inicio, fin, tipoDur, soatIni, soatFin, obs, revAlDia ->
                viewModel.addUnit(
                    nombreMoto = nombreMoto,
                    placa = placa,
                    cliente = cliente,
                    telefono = tel,
                    cuotaDiaria = cuota,
                    garantiaTotal = garantiaTotal,
                    garantiaInicial = garantiaIni,
                    deudaGarantia = deudaGarantia,
                    inicioContrato = inicio,
                    finContrato = fin,
                    tipoDuracion = tipoDur,
                    soatInicio = soatIni,
                    soatFin = soatFin,
                    observaciones = obs,
                    revisionTecnicaRealizada = revAlDia
                )
            }
        )

        DialogType.RECORD_PAYMENT -> RecordPaymentDialog(
            units = state.units,
            preselectedUnit = state.selectedUnit,
            onDismiss = { viewModel.closeDialog() },
            onConfirm = { unit, amount ->
                viewModel.recordRentalPayment(unit, amount)
            }
        )

        DialogType.ADD_MOVEMENT -> AddMovementDialog(
            units = state.units,
            onDismiss = { viewModel.closeDialog() },
            onConfirm = { concepto, monto, esIngreso, cat, placa ->
                viewModel.registerMovement(concepto, monto, esIngreso, cat, placa)
            }
        )

        DialogType.SCHEDULE_MAINTENANCE -> ScheduleMaintenanceDialog(
            units = state.units,
            onDismiss = { viewModel.closeDialog() },
            onConfirm = { placa, motivo, urgencia, fecha, costo ->
                viewModel.scheduleMaintenance(placa, motivo, urgencia, fecha, costo)
            }
        )

        DialogType.RESTOCK_ITEM -> {
            val item = state.selectedInventoryItem
            if (item != null) {
                RestockItemDialog(
                    item = item,
                    onDismiss = { viewModel.closeDialog() },
                    onConfirm = { newStock ->
                        viewModel.setStock(item, newStock)
                    }
                )
            }
        }

        DialogType.ADD_UNIT_EXPENSE -> {
            val unit = state.selectedUnit
            if (unit != null) {
                AddUnitExpenseDialog(
                    unit = unit,
                    onDismiss = { viewModel.closeDialog() },
                    onConfirm = { motivo, monto, cat ->
                        viewModel.addUnitExpense(unit, motivo, monto, cat)
                    }
                )
            }
        }

        DialogType.ADD_DEBT_CARGO -> {
            AddDebtCargoDialog(
                units = state.units,
                preselectedUnit = state.selectedUnit,
                onDismiss = { viewModel.closeDialog() },
                onConfirm = { unit, monto, motivo ->
                    viewModel.addDebtCargo(unit, monto, motivo)
                }
            )
        }

        DialogType.ADD_DEBT_ABONO -> {
            AddDebtAbonoDialog(
                units = state.units,
                preselectedUnit = state.selectedUnit,
                onDismiss = { viewModel.closeDialog() },
                onConfirm = { unit, monto, motivo ->
                    viewModel.addDebtAbono(unit, monto, motivo)
                }
            )
        }

        DialogType.REGISTER_DETAILED_MAINTENANCE -> {
            RegisterDetailedMaintenanceDialog(
                units = state.units,
                preselectedUnit = state.selectedUnit,
                stockLlantasDisponibles = state.stockLlantas,
                precioLlantaRef = state.precioLlanta,
                precioAceiteRef = state.precioAceiteBotella,
                onDismiss = { viewModel.closeDialog() },
                onConfirm = { unit, tipoTrabajo, detallesDescripcion, costoTrabajo, fecha, dejarEnTaller, botellasAceite, precioBotellaAceite, llantasUsadas, precioLlantaCustom, costoGeneral, costoFrenos, costoLlantas, costoCana, costoPlanchado, costoBajadaMotorCompleto, costoBajadaMedioMotor, costoOtros ->
                    viewModel.registerDetailedMaintenance(
                        unit = unit,
                        tipoTrabajo = tipoTrabajo,
                        detallesDescripcion = detallesDescripcion,
                        costoTrabajo = costoTrabajo,
                        fecha = fecha,
                        dejarEnTaller = dejarEnTaller,
                        botellasAceite = botellasAceite,
                        precioBotellaAceite = precioBotellaAceite,
                        llantasUsadas = llantasUsadas,
                        precioLlantaCustom = precioLlantaCustom,
                        costoGeneral = costoGeneral,
                        costoFrenos = costoFrenos,
                        costoLlantas = costoLlantas,
                        costoCana = costoCana,
                        costoPlanchado = costoPlanchado,
                        costoBajadaMotorCompleto = costoBajadaMotorCompleto,
                        costoBajadaMedioMotor = costoBajadaMedioMotor,
                        costoOtros = costoOtros
                    )
                }
            )
        }

        DialogType.EDIT_CONTRACT_SOAT -> {
            val unit = state.selectedUnit
            if (unit != null) {
                EditContractSoatDialog(
                    unit = unit,
                    onDismiss = { viewModel.closeDialog() },
                    onConfirm = { inicioContrato, finContrato, tipoDuracion, soatInicio, soatFin, chofer, telefono, cuotaDiaria, nuevoEstado, revisionTecnicaRealizada ->
                        viewModel.updateUnitContractAndSoat(
                            unit = unit,
                            inicioContrato = inicioContrato,
                            finContrato = finContrato,
                            tipoDuracion = tipoDuracion,
                            soatInicio = soatInicio,
                            soatFin = soatFin,
                            chofer = chofer,
                            telefono = telefono,
                            cuotaDiaria = cuotaDiaria,
                            nuevoEstado = nuevoEstado,
                            revisionTecnicaRealizada = revisionTecnicaRealizada
                        )
                    }
                )
            }
        }

        DialogType.BUY_OIL -> {
            BuyOilDialog(
                onDismiss = { viewModel.closeDialog() },
                onConfirm = { botellas, precioUnitario ->
                    viewModel.buyOil(botellas, precioUnitario)
                }
            )
        }

        DialogType.BUY_TIRES -> {
            BuyTiresDialog(
                onDismiss = { viewModel.closeDialog() },
                onConfirm = { llantas, precioUnitario ->
                    viewModel.buyTires(llantas, precioUnitario)
                }
            )
        }

        DialogType.TUTORIAL, DialogType.TOUR_GUIDE -> {
            viewModel.closeDialog()
            viewModel.startInteractiveTour()
        }

        DialogType.BACKUP_CLOUD -> {
            BackupCloudDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { viewModel.closeDialog() }
            )
        }

        DialogType.GOOGLE_ACCOUNT_SWITCHER -> {
            GoogleAccountDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { viewModel.closeDialog() }
            )
        }

        DialogType.EDIT_DAILY_QUOTA -> {
            viewModel.closeDialog()
        }

        DialogType.EMERGENT_ALERTS -> {
            EmergentAlertsDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { viewModel.closeDialog() }
            )
        }

        null -> {}
    }
}

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
    val isContractValid = contractDays >= 30L

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

                val garTotal = garantiaTotalText.toDoubleOrNull() ?: 200.0
                val garIni = garantiaInicialText.toDoubleOrNull() ?: 100.0
                val deudaGar = (garTotal - garIni).coerceAtLeast(0.0)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (deudaGar > 0) WarningAmberBg else SuccessGreenBg,
                    border = BorderStroke(1.dp, if (deudaGar > 0) WarningAmberText.copy(alpha = 0.3f) else SuccessGreenText.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Garantía pactada: S/ ${"%.2f".format(garTotal)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pagado hoy: S/ ${"%.2f".format(garIni)}",
                                fontSize = 11.sp,
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

                // Selector rápido de duración obligatoria >= 1 Mes
                Text(
                    text = "Duración del Contrato (Mínimo 1 Mes):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("1 Mes" to 1, "3 Meses" to 3, "6 Meses" to 6, "1 Año" to 12).forEach { (label, months) ->
                        FilterChip(
                            selected = tipoDuracion == label,
                            onClick = {
                                tipoDuracion = label
                                try {
                                    val base = sdf.parse(inicioContrato) ?: Date()
                                    val c = Calendar.getInstance().apply {
                                        time = base
                                        add(Calendar.MONTH, months)
                                    }
                                    finContrato = sdf.format(c.time)
                                } catch (_: Exception) {}
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inicioContrato,
                        onValueChange = { inicioContrato = it },
                        label = { Text("Inicio Contrato") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = finContrato,
                        onValueChange = { finContrato = it },
                        label = { Text("Fin Contrato") },
                        modifier = Modifier.weight(1f),
                        isError = !isContractValid,
                        singleLine = true
                    )
                }

                // Banner de restricción estricta de contrato menor a 1 mes
                if (!isContractValid) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DangerRedBg,
                        border = BorderStroke(1.dp, DangerRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Restricción: La moto no puede tener un contrato menor a 1 mes como mínimo (actual: ${if (contractDays >= 0) "$contractDays días" else "fecha inválida"}).",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = DangerRedText,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    try {
                                        val base = sdf.parse(inicioContrato) ?: Date()
                                        val c = Calendar.getInstance().apply {
                                            time = base
                                            add(Calendar.DAY_OF_YEAR, 30)
                                        }
                                        finContrato = sdf.format(c.time)
                                        tipoDuracion = "1 Mes"
                                    } catch (_: Exception) {}
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Ajustar a 1 Mes Mínimo (+30 días)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Text(
                    text = "Documentación y Estado Técnico Inicial:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = soatInicio,
                        onValueChange = { soatInicio = it },
                        label = { Text("Inicio SOAT") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = soatFin,
                        onValueChange = { soatFin = it },
                        label = { Text("Vencimiento SOAT (1 Año)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (revisionTecnicaAlDia) "Al día ✅ (Ingresa sin alertas pendientes)" else "Pendiente ⚠️ (Requiere revisión)",
                                fontSize = 11.sp,
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
                        val cuota = cuotaText.toDoubleOrNull() ?: 35.0
                        val garTotal = garantiaTotalText.toDoubleOrNull() ?: 200.0
                        val garIni = garantiaInicialText.toDoubleOrNull() ?: 100.0
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
                        Text("Garantía", fontSize = 11.sp, modifier = Modifier.padding(6.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { motivo = "Repuesto / Taller adelantado" }
                    ) {
                        Text("Repuestos", fontSize = 11.sp, modifier = Modifier.padding(6.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { motivo = "Choque / Planchado" }
                    ) {
                        Text("Choque", fontSize = 11.sp, modifier = Modifier.padding(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unit = selectedUnit
                    val monto = montoText.toDoubleOrNull() ?: 0.0
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
                    val monto = montoText.toDoubleOrNull() ?: 0.0
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
    var botellasAceiteCount by remember { mutableIntStateOf(2) }
    var precioBotellaAceiteText by remember { mutableStateOf("%.2f".format(precioAceiteRef)) }

    var checkCana by remember { mutableStateOf(false) }
    var costoCanaText by remember { mutableStateOf("40.0") }

    var checkFrenos by remember { mutableStateOf(false) }
    var costoFrenosText by remember { mutableStateOf("30.0") }

    var checkPlanchado by remember { mutableStateOf(false) }
    var costoPlanchadoText by remember { mutableStateOf("200.0") }

    var checkLlantas by remember { mutableStateOf(false) }
    var cantidadLlantasCount by remember { mutableIntStateOf(1) }
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
    val precioBotellaEfectivo = precioBotellaAceiteText.toDoubleOrNull() ?: precioAceiteRef
    val subtotalAceite = if (checkGeneral) (botellasAceiteCount * precioBotellaEfectivo) else 0.0
    val costoGeneralVal = if (checkGeneral) (costoGeneralText.toDoubleOrNull() ?: 0.0) else 0.0

    val costoCanaVal = if (checkCana) (costoCanaText.toDoubleOrNull() ?: 0.0) else 0.0
    val costoFrenosVal = if (checkFrenos) (costoFrenosText.toDoubleOrNull() ?: 0.0) else 0.0
    val costoPlanchadoVal = if (checkPlanchado) (costoPlanchadoText.toDoubleOrNull() ?: 0.0) else 0.0

    val precioLlantaEfectivo = precioLlantaText.toDoubleOrNull() ?: precioLlantaRef
    val subtotalLlantas = if (checkLlantas) (cantidadLlantasCount * precioLlantaEfectivo) else 0.0

    val costoMotorCompletoVal = if (checkMotorCompleto) (costoMotorCompletoText.toDoubleOrNull() ?: 0.0) else 0.0
    val costoMedioMotorVal = if (checkMedioMotor) (costoMedioMotorText.toDoubleOrNull() ?: 0.0) else 0.0
    val costoOtrosVal = if (checkOtros) (costoOtrosText.toDoubleOrNull() ?: 0.0) else 0.0

    val totalCosto = costoGeneralVal + subtotalAceite + costoCanaVal + costoFrenosVal + costoPlanchadoVal +
            subtotalLlantas + costoMotorCompletoVal + costoMedioMotorVal + costoOtrosVal

    // Título descriptivo combinado
    val listaServicios = mutableListOf<String>()
    if (checkGeneral) listaServicios.add("General")
    if (checkCana) listaServicios.add("Caña")
    if (checkFrenos) listaServicios.add("Frenos")
    if (checkPlanchado) listaServicios.add("Planchado")
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
                Text("Registrar Mantenimiento", fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
                            Text("Mantenimiento General", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                    value = botellasAceiteCount.toString(),
                                    onValueChange = { botellasAceiteCount = it.toIntOrNull() ?: 2 },
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
                                fontSize = 11.sp,
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
                            Text("Caña / Horquilla Delantera", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            Text("Frenos (Delanteros / Traseros)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            Text("Planchado / Carrocería", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                Text("Llantas del Inventario de Taller", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Disponibles en taller: $stockLlantasDisponibles llantas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (checkLlantas) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = cantidadLlantasCount.toString(),
                                    onValueChange = { cantidadLlantasCount = it.toIntOrNull() ?: 1 },
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
                                fontSize = 11.sp,
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
                            Text("Bajada Motor Completo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            Text("Bajada Medio Motor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            Text("Otros Trabajos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    Text("Dejar moto con estado \"En Taller\"", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                            Text("TOTAL MANTENIMIENTO:", fontWeight = FontWeight.ExtraBold, color = SuccessGreenText, fontSize = 13.sp)
                            Text("S/ ${"%.2f".format(totalCosto)}", fontWeight = FontWeight.ExtraBold, color = SuccessGreenText, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Servicios: $tipoTrabajoCombinado",
                            fontSize = 11.sp,
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
                            subtotalLlantas,
                            costoCanaVal,
                            costoPlanchadoVal,
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
        revisionTecnicaRealizada: Boolean
    ) -> Unit
) {
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
    val isContractValid = contractDays >= 30L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Fechas: ${unit.nombreMoto}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ajusta las fechas de contrato, estado operativo, SOAT y revisión técnica mensual.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

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
                            Text(opt, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (revisionTecnicaRealizada) "Completada este mes (Al día ✅)" else "Pendiente de realizar este mes ⏳",
                                fontSize = 11.sp,
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

                OutlinedTextField(
                    value = tipoDuracion,
                    onValueChange = { tipoDuracion = it },
                    label = { Text("Tipo de Duración (Ej. 3 Meses, 1 Semana)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Fechas de Alquiler / Contrato:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CyanAccent)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inicioContrato,
                        onValueChange = { inicioContrato = it },
                        label = { Text("Inicio (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = finContrato,
                        onValueChange = { finContrato = it },
                        label = { Text("Fin (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        isError = !isContractValid,
                        singleLine = true
                    )
                }

                if (!isContractValid) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DangerRedBg,
                        border = BorderStroke(1.dp, DangerRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Restricción: El contrato no puede ser menor a 1 mes como mínimo (mínimo 30 días).",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = DangerRedText,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Text(
                    text = "Fechas de SOAT (Vigencia Legal):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CyanAccent)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = soatInicio,
                        onValueChange = { soatInicio = it },
                        label = { Text("Inicio SOAT") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = soatFin,
                        onValueChange = { soatFin = it },
                        label = { Text("Fin SOAT") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cuota = cuotaText.toDoubleOrNull() ?: 35.0
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
                        revisionTecnicaRealizada
                    )
                },
                enabled = isContractValid,
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

// ----------------------------------------------------
// 6. DIÁLOGO: COMPRA DE BOTELLAS DE ACEITE
// ----------------------------------------------------
@Composable
fun BuyOilDialog(
    onDismiss: () -> Unit,
    onConfirm: (botellas: Int, precioUnitario: Double) -> Unit
) {
    var botellasText by remember { mutableStateOf("100") }
    var precioUnitText by remember { mutableStateOf("25.0") }

    val botellas = botellasText.toIntOrNull() ?: 0
    val precio = precioUnitText.toDoubleOrNull() ?: 0.0
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
    var llantasText by remember { mutableStateOf("10") }
    var precioUnitText by remember { mutableStateOf("85.0") }

    val llantas = llantasText.toIntOrNull() ?: 0
    val precio = precioUnitText.toDoubleOrNull() ?: 0.0
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

// ----------------------------------------------------
// 7. DIÁLOGOS EXISTENTES: MOVIMIENTOS, COBROS, GASTOS
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    units: List<FleetUnitEntity>,
    preselectedUnit: FleetUnitEntity?,
    onDismiss: () -> Unit,
    onConfirm: (FleetUnitEntity, Double) -> Unit
) {
    var selectedUnit by remember { mutableStateOf(preselectedUnit ?: units.firstOrNull()) }
    var amountText by remember { mutableStateOf(selectedUnit?.cuotaDiaria?.toString() ?: "35.0") }
    var expanded by remember { mutableStateOf(false) }

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
                                    amountText = unit.cuotaDiaria.toString()
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
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unit = selectedUnit
                    val amount = amountText.toDoubleOrNull() ?: 35.0
                    if (unit != null) {
                        onConfirm(unit, amount)
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

@Composable
fun AddMovementDialog(
    units: List<FleetUnitEntity>,
    onDismiss: () -> Unit,
    onConfirm: (concepto: String, monto: Double, esIngreso: Boolean, cat: String, placa: String?) -> Unit
) {
    var concepto by remember { mutableStateOf("") }
    var montoText by remember { mutableStateOf("") }
    var esIngreso by remember { mutableStateOf(true) }
    var categoria by remember { mutableStateOf("Alquiler") }
    var selectedPlaca by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Movimiento de Caja", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = esIngreso,
                        onClick = {
                            esIngreso = true
                            categoria = "Alquiler"
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Ingreso (+)")
                    }
                    SegmentedButton(
                        selected = !esIngreso,
                        onClick = {
                            esIngreso = false
                            categoria = "Repuestos"
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Egreso (-)")
                    }
                }

                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("movement_concept_input")
                )

                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it },
                    label = { Text("Monto (S/)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("movement_amount_input")
                )

                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoría (Ej. Alquiler, Taller, Combustible)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monto = montoText.toDoubleOrNull() ?: 0.0
                    if (concepto.isNotBlank() && monto > 0.0) {
                        onConfirm(concepto, monto, esIngreso, categoria, selectedPlaca)
                    }
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

@Composable
fun ScheduleMaintenanceDialog(
    units: List<FleetUnitEntity>,
    onDismiss: () -> Unit,
    onConfirm: (placa: String, motivo: String, urgencia: String, fecha: String, costo: Double) -> Unit
) {
    var placa by remember { mutableStateOf(units.firstOrNull()?.placa ?: "") }
    var motivo by remember { mutableStateOf("Revisión de frenos y caña") }
    var urgencia by remember { mutableStateOf("Media") }
    var fecha by remember { mutableStateOf("2026-09-20") }
    var costoText by remember { mutableStateOf("45.0") }

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
                OutlinedTextField(
                    value = fecha,
                    onValueChange = { fecha = it },
                    label = { Text("Fecha (YYYY-MM-DD)") },
                    singleLine = true,
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
                onClick = {
                    val costo = costoText.toDoubleOrNull() ?: 0.0
                    onConfirm(placa, motivo, urgencia, fecha, costo)
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

@Composable
fun AddUnitExpenseDialog(
    unit: FleetUnitEntity,
    onDismiss: () -> Unit,
    onConfirm: (motivo: String, monto: Double, cat: String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    var montoText by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Reparación") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gasto Directo: ${unit.nombreMoto} (${unit.placa})", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo del gasto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it },
                    label = { Text("Monto (S/)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoría (Ej. Repuestos, Taller, Trámites)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monto = montoText.toDoubleOrNull() ?: 0.0
                    if (motivo.isNotBlank() && monto > 0.0) {
                        onConfirm(motivo, monto, categoria)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Registrar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
