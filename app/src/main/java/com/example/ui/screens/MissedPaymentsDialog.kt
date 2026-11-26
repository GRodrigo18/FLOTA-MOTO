package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.ui.theme.AppText
import com.example.ui.theme.DangerRedText
import com.example.ui.theme.EmeraldGreen
import com.example.util.MissedPayments

/**
 * Días sin pagar de una moto. Cada día se resuelve con «Pagó» (se registra la cuota con esa fecha
 * y entra a Caja) o «No trabajó» (no se cobra). Al quedar todos resueltos, el aviso se quita solo.
 */
@Composable
fun MissedPaymentsDialog(state: FleetUiState, viewModel: FleetViewModel) {
    val unit = state.selectedUnit
    if (unit == null) {
        // Cerrar fuera de la composición (no se cambia el estado mientras se dibuja)
        LaunchedEffect(Unit) { viewModel.closeDialog() }
        return
    }
    val faltas = state.missedByPlaca[unit.placa].orEmpty().reversed() // la más reciente arriba
    val avisarDesde = state.avisarDesde[unit.placa]
    var confirmarTodos by remember { mutableStateOf(false) }

    if (confirmarTodos) {
        AlertDialog(
            onDismissRequest = { confirmarTodos = false },
            title = { Text("¿Marcar los ${faltas.size} días como «No trabajó»?") },
            text = { Text("No se cobran y dejan de avisarse. Úsalo para días en que la moto no salió.", fontSize = AppText.Body) },
            confirmButton = {
                Button(onClick = { confirmarTodos = false; viewModel.markAllMissedNotWorked(unit) }) { Text("Sí, marcar") }
            },
            dismissButton = { OutlinedButton(onClick = { confirmarTodos = false }) { Text("Volver") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = { viewModel.closeDialog() },
        title = {
            Column {
                Text("Días sin pagar • ${unit.nombreMoto}", fontWeight = FontWeight.Bold)
                Text(
                    if (faltas.isEmpty()) "Al día: no le falta ningún pago ✅"
                    else "${faltas.size} día${if (faltas.size == 1) "" else "s"} • S/ ${"%.2f".format(unit.cuotaDiaria * faltas.size)} • ${unit.cliente}",
                    fontSize = AppText.Body,
                    color = if (faltas.isEmpty()) EmeraldGreen else DangerRedText
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 420.dp)
            ) {
                // Desde qué fecha se avisan los pagos de ESTA moto (lo elige el dueño)
                if (state.isAdmin) item(key = "avisar_desde") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DatePickField(
                            label = "Avisar pagos desde",
                            value = avisarDesde ?: "",
                            onPick = { viewModel.setPaymentAlertStart(unit, it) },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = if (avisarDesde == null) "Automático (desde el inicio del contrato o su primer cobro). Toca para elegir otra fecha."
                            else "Elegida por ti (máximo 6 meses atrás)."
                        )
                        if (avisarDesde != null) {
                            TextButton(onClick = { viewModel.setPaymentAlertStart(unit, null) }) {
                                Text("Volver a automático", fontSize = AppText.Body)
                            }
                        }
                        if (faltas.size >= 2) {
                            OutlinedButton(onClick = { confirmarTodos = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Marcar todos «No trabajó»", fontSize = AppText.Body, maxLines = 1)
                            }
                        }
                    }
                }
                items(faltas, key = { it }) { fecha ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                MissedPayments.label(fecha).replaceFirstChar { it.uppercase() },
                                fontWeight = FontWeight.Bold,
                                fontSize = AppText.Title
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { viewModel.payMissedDay(unit, fecha) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Pagó S/ ${if (unit.cuotaDiaria % 1.0 == 0.0) "%.0f".format(unit.cuotaDiaria) else "%.2f".format(unit.cuotaDiaria)}", maxLines = 1, fontSize = AppText.Body) }
                                OutlinedButton(
                                    onClick = { viewModel.markDayNotWorked(unit, fecha) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("No trabajó", maxLines = 1, fontSize = AppText.Body) }
                            }
                            // Abrir el cobro diario en ese día (para registrar ahí el pago u otro monto)
                            TextButton(onClick = { viewModel.goToPaymentsOn(fecha) }) {
                                Text("Ver ese día en Pagos ›", fontSize = AppText.Body)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.closeDialog() }) { Text(if (faltas.isEmpty()) "Listo" else "Cerrar") }
        }
    )
}
