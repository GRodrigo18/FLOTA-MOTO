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

fun formatDayWithCalendarNumber(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = sdf.parse(dateStr) ?: return dateStr
        val cal = Calendar.getInstance().apply { time = parsed }
        val dayNumber = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = SimpleDateFormat("EEEE", Locale("es", "PE")).format(parsed)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val monthName = SimpleDateFormat("MMMM", Locale("es", "PE")).format(parsed)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        "$dayOfWeek $dayNumber de $monthName (Día $dayNumber de Calendario)"
    } catch (_: Exception) {
        dateStr
    }
}

fun formatShortDayLabel(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = sdf.parse(dateStr) ?: return dateStr
        val cal = Calendar.getInstance().apply { time = parsed }
        val dayNumber = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = SimpleDateFormat("EEE", Locale("es", "PE")).format(parsed)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        "$dayOfWeek $dayNumber"
    } catch (_: Exception) {
        dateStr
    }
}

@Composable
fun DailyPaymentsScreen(
    state: FleetUiState,
    viewModel: FleetViewModel
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Checklist Diario S/35, 1: Control de Deudas

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab selector superior
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                        color = CyanAccent,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PriceCheck,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (selectedSubTab == 0) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Cobros Diarios S/ 35",
                                fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedSubTab == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.testTag("subtab_daily_payments")
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (selectedSubTab == 1) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Deudas y Garantías",
                                fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedSubTab == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.tourTarget("debts_tab").testTag("subtab_debt_management")
                )
            }
        }

        if (selectedSubTab == 0) {
            DailyChecklistTabContent(state = state, viewModel = viewModel)
        } else {
            DebtManagementTabContent(state = state, viewModel = viewModel)
        }
    }
}

@Composable
internal fun DateSelectorBar(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = sdf.format(Date())

    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = sdf.format(cal.time)

    cal.add(Calendar.DAY_OF_YEAR, 2)
    val tomorrowStr = sdf.format(cal.time)

    var showDatePickerDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        try {
                            val parsed = sdf.parse(selectedDate) ?: Date()
                            val c = Calendar.getInstance().apply { time = parsed; add(Calendar.DAY_OF_YEAR, -1) }
                            onDateSelected(sdf.format(c.time))
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Día anterior", modifier = Modifier.size(16.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedDate == yesterdayStr,
                        onClick = { onDateSelected(yesterdayStr) },
                        label = { Text("Ayer (${formatShortDayLabel(yesterdayStr)})", fontSize = AppText.Small) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedDate == todayStr,
                        onClick = { onDateSelected(todayStr) },
                        label = { Text("HOY (${formatShortDayLabel(todayStr)})", fontSize = AppText.Small, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedDate == tomorrowStr,
                        onClick = { onDateSelected(tomorrowStr) },
                        label = { Text("Mañana (${formatShortDayLabel(tomorrowStr)})", fontSize = AppText.Small) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                IconButton(
                    onClick = {
                        try {
                            val parsed = sdf.parse(selectedDate) ?: Date()
                            val c = Calendar.getInstance().apply { time = parsed; add(Calendar.DAY_OF_YEAR, 1) }
                            onDateSelected(sdf.format(c.time))
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Día siguiente", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Botón selector de fecha del calendario con detalle completo del día
            Surface(
                onClick = { showDatePickerDialog = true },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = CyanAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatDayWithCalendarNumber(selectedDate),
                            fontSize = AppText.Body,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Toca para abrir calendario y elegir cualquier día",
                        fontSize = AppText.Small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDatePickerDialog) {
        val parsedDate = remember(selectedDate) {
            try {
                sdf.parse(selectedDate) ?: Date()
            } catch (e: Exception) {
                Date()
            }
        }
        val calParsed = remember(parsedDate) {
            Calendar.getInstance().apply { time = parsedDate }
        }

        var currentYear by remember { mutableIntStateOf(calParsed.get(Calendar.YEAR)) }
        var currentMonth by remember { mutableIntStateOf(calParsed.get(Calendar.MONTH) + 1) }
        var currentDay by remember { mutableIntStateOf(calParsed.get(Calendar.DAY_OF_MONTH)) }

        val monthFullNames = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        val dialogDaysInMonth = remember(currentYear, currentMonth) {
            val c = Calendar.getInstance()
            c.set(Calendar.YEAR, currentYear)
            c.set(Calendar.MONTH, currentMonth - 1)
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        val dialogFirstDay = remember(currentYear, currentMonth) {
            val c = Calendar.getInstance()
            c.set(Calendar.YEAR, currentYear)
            c.set(Calendar.MONTH, currentMonth - 1)
            c.set(Calendar.DAY_OF_MONTH, 1)
            (c.get(Calendar.DAY_OF_WEEK) + 5) % 7
        }

        AlertDialog(
            onDismissRequest = { showDatePickerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calendario de Cobros", fontWeight = FontWeight.Bold, fontSize = AppText.Title)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Controles de Mes y Año Continuos (Estilo laptop/dispositivo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(
                                onClick = {
                                    if (currentMonth == 1) {
                                        currentMonth = 12
                                        currentYear--
                                    } else {
                                        currentMonth--
                                    }
                                    if (currentDay > dialogDaysInMonth) currentDay = 1
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Mes anterior", modifier = Modifier.size(14.dp), tint = CyanAccent)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${monthFullNames.getOrElse(currentMonth - 1) { "" }} $currentYear",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = AppText.Title,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Día $currentDay seleccionado",
                                    fontSize = AppText.Small,
                                    color = CyanAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (currentMonth == 12) {
                                        currentMonth = 1
                                        currentYear++
                                    } else {
                                        currentMonth++
                                    }
                                    if (currentDay > dialogDaysInMonth) currentDay = 1
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Mes siguiente", modifier = Modifier.size(14.dp), tint = CyanAccent)
                            }
                        }

                        // Accesos rápidos
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                onClick = { currentYear-- },
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text("-1a", fontSize = AppText.Small, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                            }
                            Surface(
                                onClick = { currentYear++ },
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text("+1a", fontSize = AppText.Small, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                            }
                            Surface(
                                onClick = {
                                    val now = Calendar.getInstance()
                                    currentYear = now.get(Calendar.YEAR)
                                    currentMonth = now.get(Calendar.MONTH) + 1
                                    currentDay = now.get(Calendar.DAY_OF_MONTH)
                                },
                                shape = RoundedCornerShape(4.dp),
                                color = CyanAccent.copy(alpha = 0.18f)
                            ) {
                                Text("Hoy", fontSize = AppText.Small, fontWeight = FontWeight.Bold, color = CyanAccent, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }

                    // Encabezado de Días de semana
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("L", "M", "M", "J", "V", "S", "D").forEach { d ->
                            Text(
                                text = d,
                                fontSize = AppText.Small,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Grilla de días
                    val totalCells = dialogFirstDay + dialogDaysInMonth
                    val rows = (totalCells + 6) / 7
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (r in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                for (c in 0..6) {
                                    val idx = r * 7 + c
                                    val dNum = idx - dialogFirstDay + 1
                                    if (dNum in 1..dialogDaysInMonth) {
                                        val isSelected = currentDay == dNum
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1.1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) CyanAccent else Color.Transparent)
                                                .clickable { currentDay = dNum },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$dNum",
                                                fontSize = AppText.Small,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Fecha seleccionada: %04d-%02d-%02d".format(currentYear, currentMonth, currentDay),
                            fontSize = AppText.Body,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            modifier = Modifier.padding(8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formatted = "%04d-%02d-%02d".format(currentYear, currentMonth, currentDay)
                        onDateSelected(formatted)
                        showDatePickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Aplicar Fecha", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
