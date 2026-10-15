package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

/**
 * Representa cada paso de la capacitación interactiva obligatoria.
 */
data class WalkthroughStep(
    val stepIndex: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val targetTab: Int,
    val targetButtonName: String,
    val interconnectionExplanation: String,
    val mandatoryActionDescription: String,
    val buttonActionLabel: String
)

@Composable
fun InteractiveTourWalkthrough(
    state: FleetUiState,
    viewModel: FleetViewModel,
    modifier: Modifier = Modifier
) {
    var showExitDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_animation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val steps = listOf(
        WalkthroughStep(
            stepIndex = 0,
            title = "Centro de Operaciones",
            subtitle = "Control general e interconexión de datos",
            icon = Icons.Default.AccountBalance,
            accentColor = CyanAccent,
            targetTab = 0,
            targetButtonName = "Checklist S/35",
            interconnectionExplanation = "Todo el sistema está 100% interconectado en vivo: cada cuota diaria cobrada y abono entra directo a tu Caja en Finanzas. Los gastos y repuestos de taller salen de esa misma caja, y las garantías no pagadas se convierten en deuda de choferes.",
            mandatoryActionDescription = "Para entender cómo cobrar cuotas, debes ingresar al módulo de pagos diarios.",
            buttonActionLabel = "👉 TOCAR 'CHECKLIST S/35' PARA IR A COBROS"
        ),
        WalkthroughStep(
            stepIndex = 1,
            title = "Cobranzas Diarias (S/ 35.00)",
            subtitle = "Checklist con un solo toque sin papeleo",
            icon = Icons.Default.PriceCheck,
            accentColor = SuccessGreen,
            targetTab = 1,
            targetButtonName = "Casilla de Pago S/ 35",
            interconnectionExplanation = "Al marcar la casilla verde de cualquier moto, el sistema registra el cobro de S/ 35 al instante con fecha y hora. Ese monto ingresa automáticamente a tu balance de caja y el contador diario sube sin que tengas que hacer cálculos manuales.",
            mandatoryActionDescription = "Presiona el botón interactivo para registrar un cobro de cuota diaria.",
            buttonActionLabel = "👉 TOCAR Y MARCAR CUOTA DE S/ 35"
        ),
        WalkthroughStep(
            stepIndex = 2,
            title = "Deudas y Fondo de Garantía",
            subtitle = "Control de saldos por cobrar a choferes",
            icon = Icons.Default.MonetizationOn,
            accentColor = WarningAmber,
            targetTab = 1,
            targetButtonName = "Pestaña 'Deudas y Garantías'",
            interconnectionExplanation = "Si una moto se registra con garantía pactada de S/ 200 y el chofer solo dejó S/ 100 de inicial, los S/ 100 restantes se quedan aquí como deuda. Cada vez que el chofer abona, su deuda disminuye y el dinero entra a tu Caja Disponible.",
            mandatoryActionDescription = "Abre la pestaña de Deudas para ver el estado financiero de cada chofer.",
            buttonActionLabel = "👉 ABRIR PESTAÑA 'DEUDAS Y GARANTÍAS'"
        ),
        WalkthroughStep(
            stepIndex = 3,
            title = "Flota, Motos y Contratos",
            subtitle = "Placas únicas, SOAT y duración de contrato",
            icon = Icons.Default.ElectricRickshaw,
            accentColor = CyanAccent,
            targetTab = 2,
            targetButtonName = "Pestaña 'Flota'",
            interconnectionExplanation = "En Flota registras tus mototaxis con su placa única. Puedes definir contratos de 3 meses o de 1 semana provisional. El sistema monitorea el vencimiento del SOAT y genera alertas tempranas antes de que caduque.",
            mandatoryActionDescription = "Navega al módulo Flota para revisar cómo se administran las motos.",
            buttonActionLabel = "👉 IR A LA PESTAÑA 'FLOTA'"
        ),
        WalkthroughStep(
            stepIndex = 4,
            title = "Taller, Aceite 4T e Inventario",
            subtitle = "6 Trabajos técnicos predefinidos",
            icon = Icons.Default.Build,
            accentColor = ElectricBlue,
            targetTab = 4,
            targetButtonName = "Pestaña 'Taller'",
            interconnectionExplanation = "En Taller registras servicios mecánicos. Al seleccionar 'Mantenimiento General', el sistema descuenta automáticamente 2 botellas de aceite 4T de tu inventario y genera un egreso en tu Caja de Finanzas sin desorden.",
            mandatoryActionDescription = "Accede al Taller para ver el inventario de botellas de aceite y llantas.",
            buttonActionLabel = "👉 IR AL MÓDULO 'TALLER'"
        ),
        WalkthroughStep(
            stepIndex = 5,
            title = "Caja Disponible en Cuenta",
            subtitle = "El corazón financiero con calendario continuo",
            icon = Icons.Default.AccountBalance,
            accentColor = EmeraldGreen,
            targetTab = 3,
            targetButtonName = "Pestaña 'Finanzas'",
            interconnectionExplanation = "Tu Caja Disponible en Inicio y en Finanzas coinciden exactamente: refleja todas las cuotas de S/ 35 cobradas más garantías iniciales y abonos, menos los gastos de taller. Puedes filtrar cualquier día o mes en el calendario.",
            mandatoryActionDescription = "Ingresa a Finanzas para verificar el balance consolidado de tu negocio.",
            buttonActionLabel = "👉 IR A LA PESTAÑA 'FINANZAS'"
        ),
        WalkthroughStep(
            stepIndex = 6,
            title = "Capacitación Completada",
            subtitle = "Ya dominas el sistema interconectado",
            icon = Icons.Default.CheckCircle,
            accentColor = SuccessGreen,
            targetTab = 0,
            targetButtonName = "Comenzar",
            interconnectionExplanation = "¡Felicitaciones! Has completado el recorrido guiado obligatorio. Todos los botones de la app están vinculados entre sí para que nunca pierdas el control de tus ingresos, cuotas diarias, repuestos y motos.",
            mandatoryActionDescription = "Presiona el botón para finalizar el guiado y comenzar a usar la aplicación.",
            buttonActionLabel = "🚀 FINALIZAR Y EMPEZAR A OPERAR"
        )
    )

    val currentStepIndex = state.interactiveTourStep.coerceIn(0, steps.size - 1)
    val currentStep = steps[currentStepIndex]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.76f))
            .testTag("interactive_tour_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Cabecera superior del Coach Mark
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = currentStep.accentColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, currentStep.accentColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(currentStep.accentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PASO ${currentStepIndex + 1} DE ${steps.size} • GUIADO OBLIGATORIO",
                            color = currentStep.accentColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                IconButton(
                    onClick = { showExitDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Salir del tutorial",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tarjeta principal explicativa
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, currentStep.accentColor.copy(alpha = glowAlpha)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Ícono y título
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(currentStep.accentColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = currentStep.icon,
                                contentDescription = null,
                                tint = currentStep.accentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = currentStep.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = currentStep.subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Caja explicativa de interconexión
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Lightbulb,
                                    contentDescription = null,
                                    tint = currentStep.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "¿CÓMO SE INTERCONECTA EL SISTEMA?",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = currentStep.accentColor,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentStep.interconnectionExplanation,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Caja de acción obligatoria (Misión)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = currentStep.accentColor.copy(alpha = 0.1f),
                        border = BorderStroke(1.5.dp, currentStep.accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = currentStep.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ACCIÓN OBLIGATORIA DEL PASO:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = currentStep.accentColor,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = currentStep.mandatoryActionDescription,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra inferior con progreso e indicador de acción
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Indicador de Puntos (Dots de progreso)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, s ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(6.dp)
                                .width(if (index == currentStepIndex) 26.dp else 8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (index == currentStepIndex) currentStep.accentColor else Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // BOTÓN OBLIGATORIO DE ACCIÓN PULSANTE
                Button(
                    onClick = {
                        executeTourStepAction(
                            stepIndex = currentStepIndex,
                            state = state,
                            viewModel = viewModel
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scale(pulseScale)
                        .testTag("tour_action_button_${currentStepIndex}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentStep.accentColor,
                        contentColor = if (currentStep.accentColor == CyanAccent || currentStep.accentColor == SuccessGreen) Color.Black else Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentStep.buttonActionLabel,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "¿Deseas salir del guiado obligatorio?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Este recorrido te enseña cómo se interconectan los cobros de S/ 35, el fondo de garantías y los repuestos de taller para evitar descuadres en tu caja.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.completeInteractiveTour()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                ) {
                    Text("Sí, salir por ahora")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showExitDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                ) {
                    Text("Continuar aprendiendo")
                }
            }
        )
    }
}

/**
 * Ejecuta la acción real en la app y avanza al siguiente paso del guiado.
 */
private fun executeTourStepAction(
    stepIndex: Int,
    state: FleetUiState,
    viewModel: FleetViewModel
) {
    when (stepIndex) {
        0 -> {
            // Paso 0: Inicio -> Ir a Pagos (Checklist S/35)
            viewModel.onTabSelected(1)
            viewModel.advanceTourStep()
        }
        1 -> {
            // Paso 1: Pagos -> Demostración en vivo de cobro de S/ 35
            val firstUnit = state.units.firstOrNull()
            if (firstUnit != null) {
                // Marcar como pagada para registrar el cobro en vivo
                viewModel.toggleDailyPayment(firstUnit, true)
            }
            viewModel.advanceTourStep()
        }
        2 -> {
            // Paso 2: Deudas y Garantías -> Ver pestaña de deudas y luego aprender de Flota
            viewModel.advanceTourStep()
        }
        3 -> {
            // Paso 3: Flota -> Ir a Flota
            viewModel.onTabSelected(2)
            viewModel.advanceTourStep()
        }
        4 -> {
            // Paso 4: Taller -> Ir a Taller
            viewModel.onTabSelected(4)
            viewModel.advanceTourStep()
        }
        5 -> {
            // Paso 5: Finanzas -> Ir a Finanzas
            viewModel.onTabSelected(3)
            viewModel.advanceTourStep()
        }
        6 -> {
            // Paso 6: Fin -> Completar tour y regresar a Inicio
            viewModel.completeInteractiveTour()
        }
    }
}
