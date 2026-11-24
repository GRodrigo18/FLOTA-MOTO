package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberText
import com.example.util.AutoBackup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** "hoy 16:45", "ayer 19:30" o "14/10 19:30". */
private fun friendlyTime(millis: Long): String {
    if (millis <= 0L) return "nunca"
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val hour = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dayDiff == 0 -> "hoy $hour"
        sameYear && dayDiff == 1 -> "ayer $hour"
        else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(millis))
    }
}

/**
 * Respaldos en una sola vista y con pocas palabras:
 * estado de la copia + 3 botones grandes (Copiar ahora, Enviar a mi correo, Recuperar datos).
 */
@Composable
fun BackupCloudDialog(
    state: FleetUiState,
    viewModel: FleetViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var folderName by remember { mutableStateOf(AutoBackup.folderName(context)) }
    var lastAt by remember { mutableStateOf(AutoBackup.lastBackupAt(context)) }
    var preview by remember { mutableStateOf<com.example.ui.FleetViewModel.BackupPreview?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var linkPicked by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            viewModel.setSyncDocument(context, uri, isNewFile = true)
            folderName = AutoBackup.folderName(context)
        }
    }
    // Elegir una copia en Google Drive (o en Descargas, correo...) con el selector del teléfono
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            loading = true
            viewModel.previewFileBackup(context, uri) { p ->
                loading = false
                previewUri = uri
                // Solo se propone enlazar el archivo si este celular aún no guarda copias en Drive
                linkPicked = folderName == null
                preview = p
            }
        }
    }

    // Antes de reemplazar los datos se muestra QUÉ tiene la copia (fecha y cantidades)
    preview?.let { p ->
        AlertDialog(
            onDismissRequest = { preview = null },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = NavyPrimary) },
            title = { Text("¿Recuperar esta copia?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(p.origen, style = MaterialTheme.typography.titleSmall)
                    Text("Guardada: ${p.fecha}", style = MaterialTheme.typography.bodyLarge)
                    Text("${p.motos} moto(s) • ${p.pagos} cobro(s) • ${p.movimientos} movimiento(s) de Caja", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Los datos de este celular se reemplazan por los de la copia. Antes se guarda una copia de lo que hay ahora, por si acaso.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (previewUri != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(checked = linkPicked, onCheckedChange = { linkPicked = it })
                            Text("Seguir guardando las copias en este archivo", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.restorePreview(context, p, if (previewUri != null && linkPicked) previewUri else null)
                    preview = null
                    onDismiss()
                }) { Text("Sí, recuperar") }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("No") } }
        )
        return
    }

    val recent = lastAt > 0L && System.currentTimeMillis() - lastAt < 26 * 60 * 60 * 1000L

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("backup_cloud_dialog"),
        title = { Text("Respaldos", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Estado
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (recent) SuccessGreen.copy(alpha = 0.12f) else WarningAmber.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (recent) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (recent) SuccessGreenText else WarningAmberText,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                if (recent) "Copia al día" else "Falta copiar",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (recent) SuccessGreenText else WarningAmberText
                            )
                            Text(
                                "Última: ${friendlyTime(lastAt)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Carpeta y correo, una línea cada uno
                InfoLine(Icons.Default.Folder, folderName ?: "Sin copia en Drive", action = "Cambiar") { folderPicker.launch(AutoBackup.DOC_NAME) }
                InfoLine(Icons.Default.Mail, state.userEmail.ifBlank { "Sin correo" }, action = "Cambiar") {
                    viewModel.openDialog(com.example.ui.DialogType.EDIT_PROFILE)
                }

                BigButton("Copiar ahora", Icons.Default.CloudSync, NavyPrimary, enabled = folderName != null) {
                    viewModel.backupToSyncFolderNow(context)
                    lastAt = System.currentTimeMillis()
                }
                BigButton("Enviar a mi correo", Icons.Default.Mail, SuccessGreen, enabled = state.userEmail.isNotBlank()) {
                    viewModel.exportAndEmailBackup(context)
                }

                // Recuperar: la copia de Drive de este teléfono, u otra copia que esté en Drive
                Text("Recuperar datos", style = MaterialTheme.typography.titleSmall)
                OutlinedButton(
                    onClick = {
                        loading = true
                        viewModel.previewDriveBackup(context) { p ->
                            loading = false
                            previewUri = null
                            preview = p
                        }
                    },
                    enabled = folderName != null && !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (loading) "Leyendo la copia…" else "Recuperar mi copia de Drive", style = MaterialTheme.typography.titleSmall)
                }
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscar otra copia en Drive", style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    "En el selector toca ☰ › Drive y elige «MotoFlota_respaldo.json» (o un archivo enviado a tu correo). Antes de recuperar verás la fecha y cuántas motos tiene.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun InfoLine(icon: ImageVector, text: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun BigButton(label: String, icon: ImageVector, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}
