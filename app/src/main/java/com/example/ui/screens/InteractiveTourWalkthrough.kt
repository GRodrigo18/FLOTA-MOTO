package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.ui.TourNews
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NavyPrimary
import kotlin.math.roundToInt

/**
 * Registro de posiciones en pantalla de los elementos que enseña la guía.
 * Cada elemento se marca con [tourTarget] y la guía dibuja la mano y el foco encima.
 */
object TourTargets {
    val bounds = mutableStateMapOf<String, Rect>()
}

fun Modifier.tourTarget(id: String): Modifier = composed {
    DisposableEffect(id) { onDispose { TourTargets.bounds.remove(id) } }
    onGloballyPositioned { TourTargets.bounds[id] = it.boundsInRoot() }
}

/**
 * Un paso de la guía. [target] vacío = sin mano, solo la frase en el centro.
 * [adminOnly] = no se muestra a los cobradores (no ven Caja ni pueden anular).
 * [since] = versión en que apareció el paso (0 = guía original, 14 = v1.4, 15 = v1.5). Tras actualizar,
 * la guía muestra una vez solo los pasos más nuevos que la última versión vista
 * ([FleetViewModel.tourNewsSinceVersion]).
 */
internal data class TourStep(
    val tab: Int?,
    val target: String,
    val text: String,
    val adminOnly: Boolean = false,
    val since: Int = 0
)

/** Pasos cortos: la mano señala y un texto de pocas palabras. Tocar en cualquier parte = siguiente. */
internal val ALL_TOUR_STEPS = listOf(
    TourStep(0, "today_card", "Aquí ves quién pagó hoy"),
    TourStep(0, "nav_1", "Toca «Pagos» para cobrar"),
    TourStep(1, "pay_check_first", "Toca el cuadro cuando el chofer pague"),
    TourStep(1, "debts_tab", "Deudas y garantías, aquí"),
    TourStep(1, "debts_tab", "¿Un cargo o abono mal puesto? Tócalo en Deudas y anúlalo", adminOnly = true, since = 14),
    TourStep(2, "fleet_add", "Toca aquí para agregar una moto"),
    TourStep(2, "fleet_unit_first", "Toca una moto: datos del chofer y su estado de cuenta", since = 14),
    TourStep(2, "fleet_unit_first", "¿Pagó incompleto? En la moto, «Registrar cobro»: lo que falta pasa a su deuda", since = 14),
    TourStep(4, "taller_add", "Anota cada arreglo aquí"),
    TourStep(4, "taller_send", "Manda una moto al taller"),
    TourStep(4, "", "¿Te equivocaste en un trabajo? Toca «Anular» en la lista", adminOnly = true, since = 14),
    TourStep(3, "caja_report", "Envía el reporte del mes para Excel", adminOnly = true, since = 14),
    TourStep(3, "", "En «Historial», toca un movimiento para ver el detalle o anularlo", adminOnly = true, since = 14),
    TourStep(0, "", "¿Mucho en Inicio? Quítalo con el ojo: sigue en la campana", since = 15),
    TourStep(null, "bell", "Los recordatorios quedan aquí hasta que los resuelvas", since = 15),
    TourStep(1, "pay_check_first", "Si un chofer no paga, la moto dice «Sin pagar: N días». Tócalo para marcar «Pagó» o «No trabajó»", since = 16),
    TourStep(2, "fleet_unit_first", "En la moto, «Pagos»: elige desde qué fecha quieres que te avise los pagos", since = 17),
    TourStep(null, "bell", "Todo va a la campana, también las copias de seguridad pendientes", since = 17),
    TourStep(1, "debts_tab", "En Deudas: «Cuotas sin pagar» con las fechas que debe cada moto", since = 18),
    TourStep(null, "menu", "Usuarios, copias y más")
)

/**
 * Pasos para esta persona: todos, o solo las novedades (since > versión ya vista) si ya hizo la guía.
 * Nunca devuelve una lista vacía.
 */
internal fun stepsFor(
    isAdmin: Boolean,
    newsOnly: Boolean,
    seenVersion: Int,
    steps: List<TourStep> = ALL_TOUR_STEPS
): List<TourStep> {
    val forUser = steps.filter { isAdmin || !it.adminOnly }
    if (!newsOnly) return forUser
    return forUser.filter { TourNews.isStepShown(it.since, seenVersion) }.ifEmpty { forUser }
}

/**
 * Guía con gestos: oscurece la pantalla, ilumina el botón que hay que usar y muestra una mano
 * que lo toca, con una frase corta. Cambia sola de pestaña. "Saltar" la cierra en cualquier momento.
 */
@Composable
fun InteractiveTourWalkthrough(
    state: FleetUiState,
    viewModel: FleetViewModel
) {
    val newsOnly by viewModel.tourNewsOnly.collectAsState()
    val seenVersion by viewModel.tourNewsSinceVersion.collectAsState()
    val tourSteps = remember(state.isAdmin, newsOnly, seenVersion) { stepsFor(state.isAdmin, newsOnly, seenVersion) }
    val stepIndex = state.interactiveTourStep.coerceIn(0, tourSteps.lastIndex)
    val step = tourSteps[stepIndex]
    val density = LocalDensity.current

    LaunchedEffect(stepIndex) {
        step.tab?.let { if (state.activeTab != it) viewModel.onTabSelected(it) }
    }

    val rect = if (step.target.isEmpty()) null else TourTargets.bounds[step.target]
    val pad = with(density) { 8.dp.toPx() }
    val focus = rect?.let { Rect(it.left - pad, it.top - pad, it.right + pad, it.bottom + pad) }

    val transition = rememberInfiniteTransition(label = "tour")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulse"
    )
    val press by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "press"
    )

    var screenHeight by remember { mutableIntStateOf(0) }
    var captionHeight by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { screenHeight = it.height }
            .testTag("interactive_tour_overlay")
            .pointerInput(stepIndex) {
                detectTapGestures { viewModel.advanceTourStep(tourSteps.size) }
            }
    ) {
        // Fondo oscuro con un "hueco" iluminado sobre el elemento
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color.Black.copy(alpha = 0.62f))
            if (focus != null) {
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(focus.left, focus.top),
                    size = Size(focus.width, focus.height),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
                // Onda que sale del punto donde se toca
                drawCircle(
                    color = CyanAccent.copy(alpha = (1f - pulse) * 0.7f),
                    radius = 14.dp.toPx() + pulse * 34.dp.toPx(),
                    center = focus.center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Mano que "toca" el elemento
        if (focus != null) {
            val handSize = 52.dp
            val handPx = with(density) { handSize.toPx() }
            val lift = with(density) { 10.dp.toPx() } * (1f - press)
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (focus.center.x - handPx * 0.35f).roundToInt(),
                            (focus.center.y - handPx * 0.15f + lift).roundToInt()
                        )
                    }
                    .size(handSize)
            )
        }

        // Frase corta: arriba del foco si está abajo en la pantalla, si no, debajo
        val gap = with(density) { 70.dp.toPx() }
        val captionY = when {
            focus == null -> (screenHeight - captionHeight) / 2f
            focus.center.y > screenHeight / 2f -> focus.top - captionHeight - gap * 0.4f
            else -> focus.bottom + gap
        }.coerceIn(0f, (screenHeight - captionHeight).toFloat().coerceAtLeast(0f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, captionY.roundToInt()) }
                .onSizeChanged { captionHeight = it.height }
                .padding(horizontal = 28.dp)
        ) {
            if (newsOnly && stepIndex == 0) {
                Surface(shape = RoundedCornerShape(10.dp), color = CyanAccent) {
                    Text(
                        "NOVEDADES",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Surface(shape = RoundedCornerShape(18.dp), color = NavyPrimary, shadowElevation = 6.dp) {
                Text(
                    text = step.text,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tourSteps.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == stepIndex) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (i == stepIndex) CyanAccent else Color.White.copy(alpha = 0.5f))
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Toca para seguir",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }

        // Saltar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(8.dp),
            contentAlignment = Alignment.TopCenter // abajo está la barra de pestañas que la guía señala
        ) {
            TextButton(onClick = { viewModel.completeInteractiveTour() }, modifier = Modifier.testTag("tour_skip")) {
                Text("Saltar guía", color = Color.White, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
