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
import androidx.compose.runtime.Composable
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
    val unit = state.selectedUnit ?: run { viewModel.closeDialog(); return }
    val faltas = state.missedByPlaca[unit.placa].orEmpty().reversed() // la más reciente arriba

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
                                ) { Text("Pagó S/ ${"%.0f".format(unit.cuotaDiaria)}", maxLines = 1, fontSize = AppText.Body) }
                                OutlinedButton(
                                    onClick = { viewModel.markDayNotWorked(unit, fecha) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("No trabajó", maxLines = 1, fontSize = AppText.Body) }
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
