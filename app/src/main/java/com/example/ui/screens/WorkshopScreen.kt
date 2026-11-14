package com.example.ui.screens

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.SubTabs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircleOutline
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DetailedMaintenanceEntity
import com.example.data.model.InventoryBatchEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.MaintenanceTaskEntity
import com.example.ui.DialogType
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.ui.components.PlateBadge
import com.example.ui.theme.AppText
import com.example.ui.theme.BrightAccentBg
import com.example.ui.theme.BrightAccentText
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedBg
import com.example.ui.theme.DangerRedText
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import com.example.ui.theme.WarningAmberText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkshopScreen(
    state: FleetUiState,
    viewModel: FleetViewModel,
    modifier: Modifier = Modifier
) {
    val criticalItems = state.inventory.filter { it.stock <= it.stockMinimo }
    val oilItem = state.inventory.firstOrNull { it.nombre.contains("Aceite", ignoreCase = true) }
    val oilStock = oilItem?.stock ?: 0
    val oilPrice = oilItem?.precioUnitario ?: 25.0
    var tallerTab by remember { mutableIntStateOf(0) }
    var showSendDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<DetailedMaintenanceEntity?>(null) }

    // Anular un trabajo registrado por error: devuelve el dinero a Caja y los insumos al almacén
    recordToDelete?.let { rec ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("¿Anular este trabajo?") },
            text = {
                Text(
                    "${rec.nombreMoto} • ${rec.fecha}\nTotal S/ ${"%.2f".format(rec.costoTotal)}\n\n" +
                        "Se borra el trabajo y su gasto en Caja" +
                        (if (rec.botellasAceite > 0 || rec.llantasUsadas > 0) ", y el aceite/llantas usados vuelven al almacén." else "."),
                    fontSize = AppText.Body
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { viewModel.deleteMaintenanceRecord(rec); recordToDelete = null },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.DangerRed)
                ) { Text("Anular") }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { recordToDelete = null }) { Text("Volver") }
            }
        )
    }

    if (showSendDialog) {
        SendToWorkshopDialog(
            motos = state.units.filter { it.estado != "En Taller" && it.estado != "Inactivo" },
            onPick = { unit ->
                viewModel.markUnitInWorkshop(unit)
                showSendDialog = false
            },
            onDismiss = { showSendDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("workshop_screen_list")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Todo lo del taller vive SOLO aquí, en dos pestañas para no saturar la vista:
        // Trabajos (registrar, motos en taller, historial) | Repuestos (aceite, llantas, inventario).
        item {
            SubTabs(
                options = listOf("Trabajos", "Repuestos"),
                selected = tallerTab,
                onSelect = { tallerTab = it },
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (tallerTab == 0) {
            // Botón principal superior: Nuevo mantenimiento detallado
            item {
                Button(
                    onClick = { viewModel.openDialog(DialogType.REGISTER_DETAILED_MAINTENANCE) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .tourTarget("taller_add")
                        .testTag("btn_register_detailed_maintenance"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Registrar trabajo de taller",
                        fontWeight = FontWeight.Bold,
                        fontSize = AppText.Title
                    )
                }
            }

            // Motos que están ahora en el taller (no pagan cuota mientras estén aquí)
            item {
                MotosEnTallerSection(
                    enTaller = state.units.filter { it.estado == "En Taller" },
                    onReady = { viewModel.markUnitAsOperational(it) },
                    onSendClick = { showSendDialog = true }
                )
            }

            // ANÁLISIS MENSUAL DE MANTENIMIENTO CON NAVEGACIÓN DINÁMICA POR MESES
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Análisis Mensual de Mantenimientos",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Barra de Navegación con Flechas para todos los meses y años
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.previousMonth() },
                                    modifier = Modifier.size(36.dp).testTag("btn_prev_month")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Mes anterior",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = formatMonthDisplay(state.selectedMonth),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "Navegación continua mes a mes",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = AppText.Small,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.nextMonth() },
                                    modifier = Modifier.size(36.dp).testTag("btn_next_month")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Mes siguiente",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Resumen numérico del mes (Total Inversión - Optimizado para Modo Oscuro y Claro)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = CyanAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Inversión Total:",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                    Text(
                                        text = "Mes: ${formatMonthDisplay(state.selectedMonth)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                                Text(
                                    text = "S/ ${"%.2f".format(state.totalMantenimientoMes)}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 22.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tarjeta destacando la moto más cara (compatible con modo oscuro y modo claro)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏆", fontSize = AppText.Title)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Unidad con Mayor Inversión en ${formatMonthDisplay(state.selectedMonth)}:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = WarningAmber
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.motoMasCostosaMesTexto,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // HISTORIAL DE MANTENIMIENTOS DETALLADOS REGISTRADOS - FILTRADOS POR MES SELECCIONADO
            val monthMaintenances = state.detailedMaintenances.filter { it.mesAno == state.selectedMonth }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historial: ${formatMonthDisplay(state.selectedMonth)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "${monthMaintenances.size} servicios",
                        fontSize = AppText.Body,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (monthMaintenances.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No hay registros de mantenimientos en ${formatMonthDisplay(state.selectedMonth)}. Utiliza las flechas arriba para ver otros meses.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(monthMaintenances, key = { it.id }) { record ->
                    DetailedMaintenanceCard(
                        record = record,
                        onDelete = if (state.isAdmin) ({ recordToDelete = record }) else null
                    )
                }
            }
        } else {
            // SECCIÓN DESTACADA: CONTROL DE INVENTARIO (ACEITE 4T Y LLANTAS)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Control de Insumos e Inventario en Taller",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    // Tarjeta 1: Aceite 4T
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (state.stockAceiteBotellas < 10) WarningAmber.copy(alpha = 0.6f) else SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(CyanAccent.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OilBarrel,
                                            contentDescription = null,
                                            tint = CyanAccent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "ACEITE 4T (BOTELLAS)",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = CyanAccent,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                        Text(
                                            text = "Uso: 2 botellas en mant. general",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (state.stockAceiteBotellas < 10) WarningAmber.copy(alpha = 0.15f) else EmeraldGreen.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (state.stockAceiteBotellas < 10) WarningAmber.copy(alpha = 0.4f) else EmeraldGreen.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "${state.stockAceiteBotellas} Botellas",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (state.stockAceiteBotellas < 10) WarningAmber else EmeraldGreen
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Precio Ref.", fontSize = AppText.Small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("S/ ${"%.2f".format(state.precioAceiteBotella)} / bot.", fontWeight = FontWeight.Bold, fontSize = AppText.Body, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Valor en Bodega", fontSize = AppText.Small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("S/ ${"%.2f".format(state.valorTotalAceite)}", fontWeight = FontWeight.Bold, fontSize = AppText.Body, color = CyanAccent)
                                }
                            }

                            // Detalle por lotes de compra
                            val oilBatches = state.inventoryBatches.filter { it.categoria == "Fluidos" && it.cantidadDisponible > 0 }
                            if (oilBatches.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Lotes disponibles de Aceite:", fontSize = AppText.Small, fontWeight = FontWeight.Bold, color = CyanAccent)
                                        oilBatches.forEach { b ->
                                            Text("• ${b.cantidadDisponible} botellas a S/ ${"%.2f".format(b.precioUnitario)} c/u (${b.fecha})", fontSize = AppText.Small, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { viewModel.openDialog(DialogType.BUY_OIL) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("btn_buy_oil_lot"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp), tint = CyanAccent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Comprar Lote de Aceite (ej. 100 botellas)", fontWeight = FontWeight.SemiBold, fontSize = AppText.Body)
                            }
                        }
                    }

                    // Tarjeta 2: Llantas (Neumáticos) para Torito
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (state.stockLlantas < 4) WarningAmber.copy(alpha = 0.6f) else SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(CyanAccent.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TireRepair,
                                            contentDescription = null,
                                            tint = CyanAccent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "STOCK DE LLANTAS (BODEGA)",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = CyanAccent,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                        Text(
                                            text = "Uso: a requerimiento en mantenimiento",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (state.stockLlantas < 4) WarningAmber.copy(alpha = 0.15f) else EmeraldGreen.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (state.stockLlantas < 4) WarningAmber.copy(alpha = 0.4f) else EmeraldGreen.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "${state.stockLlantas} Llantas",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (state.stockLlantas < 4) WarningAmber else EmeraldGreen
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Precio Ref.", fontSize = AppText.Small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("S/ ${"%.2f".format(state.precioLlanta)} / llanta", fontWeight = FontWeight.Bold, fontSize = AppText.Body, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Valor en Bodega", fontSize = AppText.Small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("S/ ${"%.2f".format(state.valorTotalLlantas)}", fontWeight = FontWeight.Bold, fontSize = AppText.Body, color = CyanAccent)
                                }
                            }

                            // Detalle por lotes de compra de llantas
                            val tireBatches = state.inventoryBatches.filter { it.categoria == "Neumáticos" && it.cantidadDisponible > 0 }
                            if (tireBatches.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Lotes disponibles de Llantas:", fontSize = AppText.Small, fontWeight = FontWeight.Bold, color = CyanAccent)
                                        tireBatches.forEach { b ->
                                            Text("• ${b.cantidadDisponible} llantas a S/ ${"%.2f".format(b.precioUnitario)} c/u (${b.fecha})", fontSize = AppText.Small, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { viewModel.openDialog(DialogType.BUY_TIRES) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("btn_buy_tires_lot"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp), tint = CyanAccent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Comprar Lote de Llantas (ej. 10 llantas)", fontWeight = FontWeight.SemiBold, fontSize = AppText.Body)
                            }
                        }
                    }
                }
            }

            // SECCIÓN INVENTARIO GENERAL
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inventario Físico de Taller",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "${state.inventory.size} Ítems",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = AppText.Body,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(state.inventory) { item ->
                InventoryItemCard(
                    item = item,
                    onIncrease = { viewModel.updateStock(item, 1) },
                    onDecrease = { viewModel.updateStock(item, -1) },
                    onRestock = { viewModel.openDialog(DialogType.RESTOCK_ITEM, inventoryItem = item) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DetailedMaintenanceCard(record: DetailedMaintenanceEntity, onDelete: (() -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.nombreMoto,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = AppText.Heading
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${record.unitPlaca})",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "S/ ${"%.2f".format(record.costoTotal)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrightAccentBg
                    ) {
                        Text(
                            text = record.tipoTrabajo.ifEmpty { "Mantenimiento" },
                            color = BrightAccentText,
                            fontWeight = FontWeight.Bold,
                            fontSize = AppText.Small,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = record.fecha,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                if (record.motivoDetallado.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = record.motivoDetallado,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    )
                }
            }

            if (record.detallesDescripcion.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Detalles técnicos / Descripción:",
                            fontSize = AppText.Small,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = record.detallesDescripcion,
                            fontSize = AppText.Body,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Desglose de conceptos con FlowRow adaptable para evitar textos cortados
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (record.costoGeneral > 0.0) {
                    CostBadge(label = "General", amount = record.costoGeneral)
                }
                if (record.costoCana > 0.0) {
                    CostBadge(label = "La Caña", amount = record.costoCana, isHighlight = true)
                }
                if (record.costoFrenos > 0.0) {
                    CostBadge(label = "Frenos", amount = record.costoFrenos)
                }
                if (record.costoPlanchado > 0.0) {
                    CostBadge(label = "Planchado", amount = record.costoPlanchado, isHighlight = true)
                }
                if (record.costoCablesCambio > 0.0) {
                    CostBadge(label = "Cables de cambios", amount = record.costoCablesCambio)
                }
                if (record.costoLlantas > 0.0) {
                    CostBadge(label = "Llantas", amount = record.costoLlantas)
                }
                if (record.costoBajadaMotorCompleto > 0.0) {
                    CostBadge(label = "Motor Completo", amount = record.costoBajadaMotorCompleto, isHighlight = true)
                }
                if (record.costoBajadaMedioMotor > 0.0) {
                    CostBadge(label = "Medio Motor", amount = record.costoBajadaMedioMotor)
                }
                if (record.costoOtros > 0.0) {
                    CostBadge(label = "Otros", amount = record.costoOtros)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.OilBarrel,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Aceite: ${record.botellasAceite} botellas descontadas (S/ ${"%.2f".format(record.costoAceiteTotal)})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyanAccent,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (onDelete != null) {
                    androidx.compose.material3.TextButton(
                        onClick = onDelete,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Anular", fontSize = AppText.Small, color = com.example.ui.theme.DangerRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CostBadge(label: String, amount: Double, isHighlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isHighlight) WarningAmberBg else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isHighlight) BorderStroke(1.dp, WarningAmberText.copy(alpha = 0.3f)) else null
    ) {
        Text(
            text = "$label: S/ ${"%.0f".format(amount)}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) WarningAmberText else MaterialTheme.colorScheme.onSurface,
                fontSize = AppText.Small
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryItemEntity,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRestock: () -> Unit
) {
    val isCritical = item.stock <= item.stockMinimo
    val statusColor = if (isCritical) DangerRed else NavyPrimary
    val itemIcon: ImageVector = when {
        item.nombre.contains("Aceite", ignoreCase = true) -> Icons.Default.OilBarrel
        item.nombre.contains("Llanta", ignoreCase = true) -> Icons.Default.TireRepair
        else -> Icons.Default.Build
    }

    // Dos filas: arriba el nombre (puede ocupar 2 líneas), abajo estado + precio y el contador.
    // Antes todo iba en una fila y en celulares angostos los números se partían en varias líneas.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isCritical) DangerRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = itemIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = item.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCritical) "Stock bajo (mín. ${item.stockMinimo})" else "Stock normal",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCritical) DangerRedText else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "S/ ${"%.2f".format(item.precioUnitario)} c/u",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Contador: − cantidad +   (tocar la cantidad abre "reponer stock")
                IconButton(onClick = onDecrease, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Reducir", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCritical) DangerRedBg else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isCritical) DangerRedText.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.clickable(onClick = onRestock)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${item.stock}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCritical) DangerRedText else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = item.unidadMedida.lowercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                IconButton(onClick = onIncrease, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun formatMonthDisplay(mes: String): String {
    return try {
        val sdfIn = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = sdfIn.parse(mes)
        val sdfOut = SimpleDateFormat("MMMM yyyy", Locale("es", "PE"))
        val formatted = sdfOut.format(date ?: Date())
        formatted.replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        mes
    }
}



@Composable
private fun MotosEnTallerSection(
    enTaller: List<com.example.data.model.FleetUnitEntity>,
    onReady: (com.example.data.model.FleetUnitEntity) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (enTaller.isEmpty()) "Ninguna moto en el taller" else "En el taller (${enTaller.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.material3.TextButton(onClick = onSendClick, modifier = Modifier.tourTarget("taller_send")) { Text("+ Enviar moto") }
            }
            enTaller.forEach { unit ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(unit.nombreMoto, style = MaterialTheme.typography.titleSmall)
                        Text(
                            unit.placa,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { onReady(unit) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White)
                    ) {
                        Text("Lista ✓", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SendToWorkshopDialog(
    motos: List<com.example.data.model.FleetUnitEntity>,
    onPick: (com.example.data.model.FleetUnitEntity) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Qué moto entra al taller?", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (motos.isEmpty()) {
                    Text("No hay motos disponibles.", style = MaterialTheme.typography.bodyMedium)
                }
                motos.forEach { unit ->
                    Surface(
                        onClick = { onPick(unit) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SurfaceBorder),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(unit.nombreMoto, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(unit.placa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
