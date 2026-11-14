package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenText

/**
 * Paso obligatorio antes de empezar (solo administradores):
 *  1. Correo del dueño.
 *  2. Carpeta donde se guardan las copias (idealmente dentro de Google Drive, así quedan en su cuenta).
 * Pocas palabras y botones grandes: pensado para que lo haga cualquier persona sin ayuda.
 */
@Composable
fun SyncSetupScreen(
    state: FleetUiState,
    viewModel: FleetViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf(state.userEmail) }
    var folderName by remember { mutableStateOf(viewModel.syncFolderName()) }
    var backupDate by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Toma el correo de la cuenta Google ya iniciada en el teléfono (un toque, sin escribir)
    val accountPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)?.let { email = it }
    }

    // Crear el archivo de copia en Google Drive (Drive permite crear archivos desde otras apps)
    val createInDrive = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            viewModel.setSyncDocument(context, uri, isNewFile = true)
            folderName = viewModel.syncFolderName()
        }
    }
    // Teléfono nuevo: elegir la copia que ya está en Drive
    val pickExisting = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewModel.setSyncDocument(context, uri, isNewFile = false)
            folderName = viewModel.syncFolderName()
        }
    }

    // Si la carpeta ya tiene copias (teléfono nuevo o prueba desde cero), se ofrece recuperarlas.
    LaunchedEffect(folderName) {
        backupDate = if (folderName != null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { viewModel.latestFolderBackupDate() }
        } else null
    }

    val emailOk = email.contains("@") && email.contains(".")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Icon(
                Icons.Default.CloudSync,
                contentDescription = null,
                tint = NavyPrimary,
                modifier = Modifier.size(52.dp)
            )
            Text("Copia de seguridad", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Dos pasos y listo. Tus datos se copiarán solos.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Paso 1: correo
            SetupStep(number = 1, title = "Tu correo", done = emailOk, icon = Icons.Default.Mail) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    placeholder = { Text("nombre@gmail.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_setup_email")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            accountPicker.launch(
                                android.accounts.AccountManager.newChooseAccountIntent(
                                    null, null, arrayOf("com.google"), null, null, null, null
                                )
                            )
                        } catch (_: Exception) {
                            error = "Escribe tu correo (no se pudo abrir la lista de cuentas)."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sync_setup_google_account"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Usar mi cuenta de Google", style = MaterialTheme.typography.titleSmall)
                }
            }

            // Paso 2: archivo de copia en Google Drive
            SetupStep(number = 2, title = "Copia en Google Drive", done = folderName != null, icon = Icons.Default.Folder) {
                if (folderName == null) {
                    Text(
                        "Toca el botón, elige «Drive» y luego «Guardar».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { createInDrive.launch(com.example.util.AutoBackup.DOC_NAME) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("sync_setup_pick_folder"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Text("Guardar copia en Google Drive", style = MaterialTheme.typography.titleMedium)
                    }
                    androidx.compose.material3.TextButton(
                        onClick = { pickExisting.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth().testTag("sync_setup_existing")
                    ) {
                        Text("Ya tengo una copia en Drive")
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            folderName ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.TextButton(onClick = { createInDrive.launch(com.example.util.AutoBackup.DOC_NAME) }) {
                            Text("Cambiar")
                        }
                    }
                }
            }

            // Copia encontrada en la carpeta: recuperar con un toque
            if (backupDate != null && state.units.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SuccessGreen.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Hay una copia del $backupDate",
                            style = MaterialTheme.typography.titleMedium,
                            color = SuccessGreenText
                        )
                        OutlinedButton(
                            onClick = { viewModel.restoreFromSyncFolder(context) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, SuccessGreen)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = SuccessGreenText)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recuperar mis datos", color = SuccessGreenText, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            }

            Button(
                onClick = { error = viewModel.completeSyncSetup(email) },
                enabled = emailOk && folderName != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("sync_setup_done"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White)
            ) {
                Text("Empezar", style = MaterialTheme.typography.titleLarge)
            }

            Text(
                "Luego puedes cambiarlo en el menú › Respaldos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SetupStep(
    number: Int,
    title: String,
    done: Boolean,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (done) 2.dp else 1.dp, if (done) SuccessGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (done) SuccessGreen else NavyPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Listo", tint = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text("$number", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
