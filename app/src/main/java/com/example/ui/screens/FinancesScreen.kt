package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.items
import java.util.Calendar
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PlateBadge
import com.example.ui.components.SubTabs
import com.example.ui.DialogType
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.ui.theme.AppText
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedBg
import com.example.ui.theme.DangerRedText
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import com.example.ui.theme.WarningAmberText
import kotlin.math.PI

@Composable
fun FinancesScreen(
    state: FleetUiState,
    viewModel: FleetViewModel,
    modifier: Modifier = Modifier
) {
    val todayCal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH) + 1) }
    var selectedDay by remember { mutableStateOf<Int?>(todayCal.get(Calendar.DAY_OF_MONTH)) } // null means "Todo el Mes"
    var cajaTab by remember { mutableIntStateOf(0) }
    var movDetail by remember { mutableStateOf<com.example.data.model.FinanceMovementEntity?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Detalle de un movimiento: texto completo y, para el administrador, anularlo si fue un error
    movDetail?.let { mov ->
        val hint = viewModel.movementFixHint(mov)
        val bloqueado = mov.categoria == "Mantenimiento Moto" || mov.categoria == "Abono Garantía"
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { movDetail = null; confirmDelete = false },
            title = { Text(if (mov.esIngreso) "Ingreso" else "Egreso") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        (if (mov.esIngreso) "+ S/ " else "- S/ ") + "%.2f".format(mov.monto),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = AppText.Heading,
                        color = if (mov.esIngreso) SuccessGreenText else DangerRedText
                    )
                    Text(mov.concepto, fontSize = AppText.Body)
                    Text(
                        listOfNotNull(mov.fecha, mov.categoria, mov.unitPlaca?.takeIf { it.isNotBlank() }).joinToString(" · "),
                        fontSize = AppText.Small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hint != null) Text(hint, fontSize = AppText.Small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (confirmDelete) {
                        Text(
                            "¿Seguro? Se borra este movimiento" +
                                (if (mov.categoria.startsWith("Pago")) " y la cuota queda como NO pagada." else "."),
                            fontSize = AppText.Body,
                            fontWeight = FontWeight.SemiBold,
                            color = DangerRedText
                        )
                    }
                }
            },
            confirmButton = {
                if (state.isAdmin && !bloqueado) {
                    androidx.compose.material3.Button(
                        onClick = {
                            if (confirmDelete) {
                                viewModel.deleteMovement(mov)
                                movDetail = null
                                confirmDelete = false
                            } else confirmDelete = true
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) { Text(if (confirmDelete) "Sí, anular" else "Anular") }
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { movDetail = null; confirmDelete = false }) { Text("Cerrar") }
            }
        )
    }

    val fullMonthNames = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    val daysInMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, selectedMonth - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeek = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, selectedMonth - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        // Calendar.SUNDAY = 1, MONDAY = 2... -> Lun = 0, Mar = 1... Dom = 6
        (dow + 5) % 7
    }

    val filteredByDate = remember(state.movements, selectedYear, selectedMonth, selectedDay) {
        state.movements.filter { mov ->
            try {
                val parts = mov.fecha.split("-")
                val yr = parts[0].toInt()
                val mo = parts[1].toInt()
                val dy = if (parts.size >= 3) parts[2].take(2).toIntOrNull() else null
                val yearMatch = yr == selectedYear
                val monthMatch = mo == selectedMonth
                val dayMatch = if (selectedDay != null) (dy == selectedDay) else true
                yearMatch && monthMatch && dayMatch
            } catch (e: Exception) {
                false
            }
        }
    }

    val periodIngresos = remember(filteredByDate) { filteredByDate.filter { it.esIngreso }.sumOf { it.monto } }
    val periodEgresos = remember(filteredByDate) { filteredByDate.filter { !it.esIngreso }.sumOf { it.monto } }
    val periodBalance = periodIngresos - periodEgresos

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pestañas internas: Resumen (saldo y gastos) | Historial (calendario y movimientos).
        // Los costos de taller ya no se repiten aquí: viven solo en la pestaña Taller.
        item {
            SubTabs(
                options = listOf("Resumen", "Historial"),
                selected = cajaTab,
                onSelect = { cajaTab = it },
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (cajaTab == 0) {
            // Balance Header Card con los montos sincronizados con Inicio y el flujo del período
            item {
                val periodName = if (selectedDay != null) {
                    "Día $selectedDay de ${fullMonthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear"
                } else {
                    "${fullMonthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear (Mes Completo)"
                }

                FinancesHeaderCard(
                    totalCaja = state.totalBalance,
                    totalIngresosGeneral = state.totalIngresos,
                    totalEgresosGeneral = state.totalEgresos,
                    periodBalance = periodBalance,
                    periodIngresos = periodIngresos,
                    periodEgresos = periodEgresos,
                    periodLabel = periodName,
                    isFilteredByDay = selectedDay != null,
                    onAddIncome = { viewModel.openDialog(DialogType.RECORD_PAYMENT) },
                    onAddExpense = { viewModel.openDialog(DialogType.ADD_MOVEMENT) }
                )
            }

            // Reporte del mes para Excel / correo
            item {
                androidx.compose.material3.OutlinedButton(
                    onClick = { viewModel.exportMonthlyReport(context, "%04d-%02d".format(selectedYear, selectedMonth)) },
                    modifier = Modifier.fillMaxWidth().height(48.dp).tourTarget("caja_report"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar reporte de ${fullMonthNames.getOrElse(selectedMonth - 1) { "" }} (Excel)", fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }

            // Expense Breakdown by Category (Donut Chart)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Distribución de Egresos del Mes",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        val categoryColors = listOf(
                            DangerRed,
                            WarningAmber,
                            NavyLight,
                            ElectricBlue,
                            Color(0xFF8B5CF6)
                        )

                        val categories: List<Pair<String, Double>> = state.gastosPorCategoria.toList()
                        val totalGastos = state.totalEgresos

                        if (totalGastos <= 0.0 || categories.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No hay egresos registrados en este período.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = AppText.Body,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Donut Canvas
                                Box(
                                    modifier = Modifier
                                        .size(130.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(120.dp)) {
                                        val strokeWidth = 22.dp.toPx()
                                        val diameter = size.minDimension - strokeWidth
                                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                                        val arcSize = Size(diameter, diameter)

                                        var startAngle = -90f
                                        for ((index, pair) in categories.withIndex()) {
                                            val sweep = ((pair.second / totalGastos) * 360f).toFloat()
                                            val color = categoryColors[index % categoryColors.size]
                                            drawArc(
                                                color = color,
                                                startAngle = startAngle,
                                                sweepAngle = sweep - 1.5f,
                                                useCenter = false,
                                                topLeft = topLeft,
                                                size = arcSize,
                                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                            )
                                            startAngle += sweep
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Total",
                                            fontSize = AppText.Small,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "S/ ${"%.0f".format(totalGastos)}",
                                            fontSize = AppText.Title,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(18.dp))

                                // Legends
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for ((index, pair) in categories.withIndex()) {
                                        val color = categoryColors[index % categoryColors.size]
                                        val percentage = if (totalGastos > 0) ((pair.second / totalGastos) * 100).toInt() else 0
                                        CategoryLegendItem(
                                            titulo = pair.first,
                                            monto = pair.second,
                                            porcentaje = percentage,
                                            color = color
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Calendario tipo Laptop / Dispositivo (Navegación dinámica sin límite de años)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Encabezado del calendario con flechas para mes y año
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                IconButton(
                                    onClick = {
                                        if (selectedMonth == 1) {
                                            selectedMonth = 12
                                            selectedYear--
                                        } else {
                                            selectedMonth--
                                        }
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Mes anterior",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${fullMonthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = AppText.Title,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (selectedDay != null) "Día $selectedDay seleccionado" else "Mostrando todo el mes",
                                        fontSize = AppText.Small,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selectedDay != null) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (selectedMonth == 12) {
                                            selectedMonth = 1
                                            selectedYear++
                                        } else {
                                            selectedMonth++
                                        }
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Mes siguiente",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Acciones rápidas de Año y Hoy
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    onClick = { selectedYear-- },
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "-1 Año",
                                        fontSize = AppText.Small,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    onClick = { selectedYear++ },
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "+1 Año",
                                        fontSize = AppText.Small,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    onClick = {
                                        val now = Calendar.getInstance()
                                        selectedYear = now.get(Calendar.YEAR)
                                        selectedMonth = now.get(Calendar.MONTH) + 1
                                        selectedDay = now.get(Calendar.DAY_OF_MONTH)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = CyanAccent.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "Hoy",
                                        fontSize = AppText.Small,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Botón para seleccionar todo el mes completo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedDay == null,
                                onClick = { selectedDay = null },
                                label = { Text("Todo el Mes (${fullMonthNames.getOrElse(selectedMonth - 1) { "" }})", fontSize = AppText.Small, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanAccent,
                                    selectedLabelColor = Color.Black
                                )
                            )
                            if (selectedDay != null) {
                                Text(
                                    text = "Toca el día activo para ver todo el mes",
                                    fontSize = AppText.Small,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Días de la semana estilo Calendario de Laptop (L, M, M, J, V, S, D)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("L", "M", "M", "J", "V", "S", "D").forEach { d ->
                                Text(
                                    text = d,
                                    fontSize = AppText.Small,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Matriz de días del mes
                        val totalCells = firstDayOfWeek + daysInMonth
                        val rows = (totalCells + 6) / 7
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (row in 0 until rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    for (col in 0..6) {
                                        val cellIndex = row * 7 + col
                                        val dayNum = cellIndex - firstDayOfWeek + 1
                                        if (dayNum in 1..daysInMonth) {
                                            val isSelected = selectedDay == dayNum
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1.15f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) CyanAccent else Color.Transparent
                                                    )
                                                    .clickable {
                                                        selectedDay = if (selectedDay == dayNum) null else dayNum
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$dayNum",
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
                    }
                }
            }

            // Lista de Transacciones Detalladas del Período
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Movimientos",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "${filteredByDate.size} registros",
                        fontSize = AppText.Body,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (filteredByDate.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No se encontraron movimientos registrados en este período.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(filteredByDate, key = { it.id }) { mov ->
                    Surface(
                        onClick = { movDetail = mov; confirmDelete = false },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (mov.esIngreso) SuccessGreenBg else DangerRedBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (mov.esIngreso) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = if (mov.esIngreso) "Ingreso" else "Egreso",
                                        tint = if (mov.esIngreso) SuccessGreen else DangerRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mov.concepto,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = AppText.Body,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = listOfNotNull(mov.fecha, mov.categoria, mov.unitPlaca?.takeIf { it.isNotBlank() })
                                            .joinToString(" · "),
                                        fontSize = AppText.Small,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Text(
                                text = (if (mov.esIngreso) "+ S/ " else "- S/ ") + "%.2f".format(mov.monto),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = AppText.Title,
                                color = if (mov.esIngreso) SuccessGreenText else DangerRedText
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FinancesHeaderCard(
    totalCaja: Double,
    totalIngresosGeneral: Double,
    totalEgresosGeneral: Double,
    periodBalance: Double,
    periodIngresos: Double,
    periodEgresos: Double,
    periodLabel: String,
    isFilteredByDay: Boolean,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(NavyDark, NavyPrimary, ElectricBlue)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Caja Disponible en Cuenta",
                            color = Color.White,
                            fontSize = AppText.Body,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SuccessGreen.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Enlace Inicio ✅",
                            color = SuccessGreen,
                            fontSize = AppText.Small,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Monto principal: Caja Disponible Total de la empresa (Idéntico a Inicio)
                Text(
                    text = "S/ ${"%,.2f".format(totalCaja)}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Saldo total disponible (+S/ ${"%.0f".format(totalIngresosGeneral)} ingresados / -S/ ${"%.0f".format(totalEgresosGeneral)} egresos)",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = AppText.Small,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Sub-tarjeta de Flujo del Período / Día Seleccionado
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Flujo: $periodLabel",
                                color = CyanAccent,
                                fontSize = AppText.Small,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Neto: S/ ${"%,.2f".format(periodBalance)}",
                                color = if (periodBalance >= 0) SuccessGreen else DangerRed,
                                fontSize = AppText.Body,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.25f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Ingresos", color = Color.White.copy(alpha = 0.7f), fontSize = AppText.Small)
                                        Text("+S/ ${"%,.0f".format(periodIngresos)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = AppText.Body)
                                    }
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.25f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = DangerRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Egresos", color = Color.White.copy(alpha = 0.7f), fontSize = AppText.Small)
                                        Text("-S/ ${"%,.0f".format(periodEgresos)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = AppText.Body)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons inside banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAddIncome,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Cobro", fontSize = AppText.Body, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onAddExpense,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("- Gasto", fontSize = AppText.Body, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryLegendItem(
    titulo: String,
    monto: Double,
    porcentaje: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                fontSize = AppText.Body,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "$porcentaje%",
            fontSize = AppText.Small,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = "S/ ${"%.0f".format(monto)}",
            fontSize = AppText.Body,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
