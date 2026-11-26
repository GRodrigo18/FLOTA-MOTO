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
import androidx.compose.runtime.LaunchedEffect
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
            // Las motos inactivas no pagan cuota
            units = state.units.filter { it.estado != "Inactivo" },
            preselectedUnit = state.selectedUnit?.takeIf { it.estado != "Inactivo" },
            onDismiss = { viewModel.closeDialog() },
            onConfirm = { unit, amount, restoADeuda ->
                viewModel.recordPaymentWithRest(unit, amount, restoADeuda)
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
                onConfirm = { unit, tipoTrabajo, detallesDescripcion, costoTrabajo, fecha, dejarEnTaller, botellasAceite, precioBotellaAceite, llantasUsadas, precioLlantaCustom, costoGeneral, costoFrenos, costoLlantas, costoCana, costoPlanchado, costoCablesCambio, costoBajadaMotorCompleto, costoBajadaMedioMotor, costoOtros ->
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
                        costoCablesCambio = costoCablesCambio,
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
                    onConfirm = { inicioContrato, finContrato, tipoDuracion, soatInicio, soatFin, chofer, telefono, cuotaDiaria, nuevoEstado, revisionTecnicaRealizada, nuevoNombre, nuevaPlaca ->
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
                            revisionTecnicaRealizada = revisionTecnicaRealizada,
                            nuevoNombre = nuevoNombre,
                            nuevaPlaca = nuevaPlaca
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

        DialogType.BACKUP_CLOUD -> if (!state.isAdmin) {
            LaunchedEffect(Unit) { viewModel.closeDialog() }
        } else {
            BackupCloudDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { viewModel.closeDialog() }
            )
        }

        DialogType.EDIT_PROFILE -> {
            ProfileDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { viewModel.closeDialog() }
            )
        }

        DialogType.MANAGE_USERS -> {
            ManageUsersDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { viewModel.closeDialog() }
            )
        }

        DialogType.CHANGE_PIN -> {
            ChangeOwnPinDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeDialog() }
            )
        }

        DialogType.START_FROM_ZERO -> {
            StartFromZeroDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeDialog() }
            )
        }

        DialogType.EDIT_DAILY_QUOTA -> {
            viewModel.closeDialog()
        }

        DialogType.MISSED_PAYMENTS -> MissedPaymentsDialog(state = state, viewModel = viewModel)
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
