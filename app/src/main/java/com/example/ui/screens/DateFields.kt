package com.example.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppText
import com.example.ui.theme.CyanAccent
import com.example.util.FleetDates
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/*
 * Fechas sin escribir a mano: se tocan y se elige el día en un calendario.
 * Así siempre se guardan como año-mes-día y las alertas funcionan.
 * Los fines de contrato y de SOAT se calculan solos.
 */

private fun utcFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

/** "2026-11-05" → milisegundos UTC de ese día (lo que usa el calendario de Material). */
internal fun isoToUtcMillis(date: String): Long? = FleetDates.normalize(date)?.let {
    try { utcFormat().parse(it)?.time } catch (_: Exception) { null }
}

internal fun utcMillisToIso(millis: Long): String = utcFormat().format(java.util.Date(millis))

/**
 * Campo de fecha: muestra "05 nov 2026" y al tocarlo abre el calendario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickField(
    label: String,
    value: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    var open by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) { if (pressed) open = true }

    OutlinedTextField(
        value = if (value.isBlank()) "" else FleetDates.pretty(value),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Elegir fecha") },
        supportingText = supportingText?.let { { Text(it, fontSize = AppText.Small) } },
        singleLine = true,
        interactionSource = interaction,
        modifier = modifier
    )

    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = isoToUtcMillis(value) ?: isoToUtcMillis(FleetDates.today()))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                Button(onClick = {
                    state.selectedDateMillis?.let { onPick(utcMillisToIso(it)) }
                    open = false
                }) { Text("Listo") }
            },
            dismissButton = { OutlinedButton(onClick = { open = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}

/** Fecha que la app calcula sola (no se puede editar): se muestra con un candado. */
@Composable
fun AutoDateField(label: String, value: String, note: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = if (value.isBlank()) "—" else FleetDates.pretty(value),
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        supportingText = { Text(note, fontSize = AppText.Small) },
        singleLine = true,
        modifier = modifier
    )
}

/** Botones de modalidad: 1 Mes, 3 Meses, 6 Meses, 1 Año. */
@Composable
fun ModalidadChips(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FleetDates.MODALIDADES.forEach { (label, _) ->
            FilterChip(
                selected = selected.equals(label, ignoreCase = true),
                onClick = { onSelect(label) },
                label = { Text(label, fontSize = AppText.Small, maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanAccent,
                    selectedLabelColor = Color.Black
                )
            )
        }
    }
}

/**
 * Contrato: se elige la modalidad y el día de inicio; el fin se calcula solo.
 * [fin] lo mantiene quien llama (para no cambiar un contrato guardado hasta que se toque algo).
 */
@Composable
fun ContractDatesSection(
    tipoDuracion: String,
    inicio: String,
    fin: String,
    onChange: (tipo: String, inicio: String, fin: String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Duración del contrato", fontSize = AppText.Body, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        ModalidadChips(selected = tipoDuracion) { nuevo ->
            onChange(nuevo, inicio, FleetDates.contractEnd(inicio, nuevo) ?: fin)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            DatePickField(
                label = "Inicio",
                value = inicio,
                onPick = { nuevoInicio -> onChange(tipoDuracion, nuevoInicio, FleetDates.contractEnd(nuevoInicio, tipoDuracion) ?: fin) },
                modifier = Modifier.weight(1f)
            )
            AutoDateField(label = "Fin", value = fin, note = "Automático", modifier = Modifier.weight(1f))
        }
    }
}

/** SOAT: se elige el día de inicio; el vencimiento (1 año) se calcula solo. */
@Composable
fun SoatDatesSection(inicio: String, fin: String, onChange: (inicio: String, fin: String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        DatePickField(
            label = "Inicio SOAT",
            value = inicio,
            onPick = { nuevo -> onChange(nuevo, FleetDates.soatEnd(nuevo) ?: fin) },
            modifier = Modifier.weight(1f)
        )
        AutoDateField(label = "Vence SOAT", value = fin, note = "1 año, automático", modifier = Modifier.weight(1f))
    }
}

/** Tarjeta pequeña con un texto de ayuda. */
@Composable
fun HintCard(text: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
        Text(text, fontSize = AppText.Small, modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
