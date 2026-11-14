package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppText
import com.example.ui.theme.DangerRedBg
import com.example.ui.theme.DangerRedText
import com.example.ui.theme.DarkDangerRed
import com.example.ui.theme.DarkDangerRedBg
import com.example.ui.theme.DarkSuccessGreen
import com.example.ui.theme.DarkSuccessGreenBg
import com.example.ui.theme.DarkWarningAmber
import com.example.ui.theme.DarkWarningAmberBg
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.WarningAmberBg
import com.example.ui.theme.WarningAmberText

@Composable
fun PlateBadge(
    placa: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.2f
    val badgeBg = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val borderColor = if (isDark) Color(0xFF60A5FA) else NavyPrimary

    Surface(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(6.dp)),
        shape = RoundedCornerShape(6.dp),
        color = badgeBg
    ) {
        // Placa en una sola línea y compacta: antes, en filas angostas, las letras se apilaban
        // una debajo de otra ("A D A S J D") y la fila se volvía muy alta.
        Text(
            text = placa,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                color = textColor
            ),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatusBadge(
    estado: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.2f
    val (bgColor, textColor) = when (estado) {
        "Vigente" -> if (isDark) DarkSuccessGreenBg to DarkSuccessGreen else SuccessGreenBg to SuccessGreenText
        "Por Vencer" -> if (isDark) DarkWarningAmberBg to DarkWarningAmber else WarningAmberBg to WarningAmberText
        "En Taller" -> if (isDark) DarkDangerRedBg to DarkDangerRed else DangerRedBg to DangerRedText
        else -> if (isDark) Color(0xFF334155) to Color(0xFFE2E8F0) else Color(0xFFE2E8F0) to Color(0xFF334155)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = estado,
                color = textColor,
                fontSize = AppText.Body,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun MetricCard(
    titulo: String,
    valor: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitulo: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = titulo,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.padding(top = 12.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.padding(top = 4.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            if (subtitulo != null) {
                Spacer(modifier = Modifier.padding(top = 4.dp))
                Text(
                    text = subtitulo,
                    fontSize = AppText.Body,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



/**
 * Pestañas internas de una pantalla (ej. Taller: Trabajos | Repuestos). Sirven para no amontonar
 * todo en una sola lista larga.
 */
@Composable
fun SubTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selected
                Surface(
                    onClick = { onSelect(index) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}
