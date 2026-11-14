package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NavyPrimary
import com.example.util.AppUser
import com.example.util.BiometricHelper
import com.example.util.PinAuth
import com.example.util.UserStore
import kotlinx.coroutines.delay

/**
 * Pantalla de ingreso.
 * - Primera vez: se crea el dueño como Administrador (nombre + PIN).
 * - Con un usuario: PIN, o huella / bloqueo del teléfono.
 * - Con varios usuarios: se elige el nombre y se escribe SU PIN. La huella no se ofrece
 *   porque cualquiera que conozca el bloqueo del celular podría entrar como otra persona.
 */
@Composable
fun LoginScreen(
    users: List<AppUser>,
    lastUserId: String?,
    onCreateFirstAdmin: (name: String, email: String, pin: String) -> UserStore.AddResult,
    onVerifyPin: (userId: String, pin: String) -> PinAuth.Result,
    lockRemainingMs: (userId: String) -> Long,
    onDeviceUnlock: (userId: String) -> Unit,
    onResetPinWithDevice: (userId: String, newPin: String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val singleUser = users.size == 1
    val deviceAuthAvailable = remember {
        BiometricHelper.checkBiometricStatus(context) == BiometricHelper.BiometricStatus.AVAILABLE
    }

    val setupMode = users.isEmpty()
    var selectedId by remember {
        mutableStateOf(users.firstOrNull { it.id == lastUserId }?.id ?: users.firstOrNull()?.id)
    }
    var resettingPin by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lockLeft by remember { mutableStateOf(0) }

    fun secondsLeft(): Int = selectedId?.let { ((lockRemainingMs(it) + 999) / 1000).toInt() } ?: 0

    LaunchedEffect(selectedId) {
        lockLeft = secondsLeft()
        pin = ""
        error = null
    }
    LaunchedEffect(lockLeft) {
        if (lockLeft > 0) {
            delay(1000)
            lockLeft = secondsLeft()
        }
    }

    fun launchDeviceAuth(onOk: () -> Unit) {
        val activity = context as? FragmentActivity
        if (activity == null) {
            error = "No se pudo abrir la verificación del teléfono."
            return
        }
        BiometricHelper.authenticate(
            activity = activity,
            title = "Verifica que eres tú",
            subtitle = "Usa tu huella, rostro o el bloqueo de pantalla del teléfono",
            onSuccess = onOk,
            onError = { msg -> error = msg }
        )
    }

    fun submit() {
        error = null
        val creatingPin = setupMode || resettingPin
        if (creatingPin) {
            if (!PinAuth.isValidPin(pin)) {
                error = "El PIN debe tener entre ${PinAuth.MIN_LENGTH} y ${PinAuth.MAX_LENGTH} números."
                return
            }
            if (pin != confirmPin) {
                error = "Los dos PIN no coinciden."
                return
            }
            if (resettingPin) {
                val id = selectedId ?: return
                if (!onResetPinWithDevice(id, pin)) error = "No se pudo cambiar el PIN."
                return
            }
            val mail = email.trim()
            if (mail.isNotEmpty() && !(mail.contains("@") && mail.contains("."))) {
                error = "El correo no es válido."
                return
            }
            error = when (onCreateFirstAdmin(name.trim(), mail, pin)) {
                UserStore.AddResult.OK -> null
                UserStore.AddResult.INVALID_NAME -> "Escribe tu nombre."
                UserStore.AddResult.NAME_TAKEN -> "Ese nombre ya existe."
                UserStore.AddResult.INVALID_PIN -> "El PIN no es válido."
            }
            return
        }

        val id = selectedId ?: return
        if (lockLeft > 0) return
        when (onVerifyPin(id, pin)) {
            PinAuth.Result.OK -> Unit
            PinAuth.Result.WRONG -> {
                error = "PIN incorrecto."
                pin = ""
            }
            PinAuth.Result.LOCKED -> {
                lockLeft = secondsLeft()
                pin = ""
            }
        }
    }

    val creatingPin = setupMode || resettingPin
    val inputLocked = !creatingPin && lockLeft > 0
    val pinVisual: VisualTransformation =
        if (showPin) VisualTransformation.None else PasswordVisualTransformation()
    val selectedUser = users.firstOrNull { it.id == selectedId }

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(NavyPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "MotoFlota Pro",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = when {
                    setupMode -> "Crea el usuario administrador"
                    resettingPin -> "Crea un PIN nuevo para ${selectedUser?.name ?: ""}"
                    singleUser -> "Hola, ${selectedUser?.name ?: ""}. Ingresa tu PIN"
                    else -> "¿Quién va a ingresar?"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Selección de usuario (solo cuando hay varios)
            if (!setupMode && !singleUser && !resettingPin) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    users.forEach { u ->
                        val selected = u.id == selectedId
                        Surface(
                            onClick = { selectedId = u.id },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) NavyPrimary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) NavyPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_user_${u.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NavyPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(u.name.take(1).uppercase(), color = CyanAccent, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(u.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text(u.role.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (selected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Seleccionado", tint = NavyPrimary)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (setupMode) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 30) name = it },
                    label = { Text("Tu nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setup_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo para respaldos (opcional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("setup_email_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = pin,
                onValueChange = { v -> if (v.length <= PinAuth.MAX_LENGTH && v.all { it in '0'..'9' }) pin = v },
                label = { Text(if (creatingPin) "PIN nuevo (${PinAuth.MIN_LENGTH} a ${PinAuth.MAX_LENGTH} números)" else "PIN") },
                singleLine = true,
                enabled = !inputLocked,
                visualTransformation = pinVisual,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = if (creatingPin) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailingIcon = {
                    IconButton(onClick = { showPin = !showPin }) {
                        Icon(
                            imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPin) "Ocultar PIN" else "Mostrar PIN"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("login_pin_input")
            )

            if (creatingPin) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { v -> if (v.length <= PinAuth.MAX_LENGTH && v.all { it in '0'..'9' }) confirmPin = v },
                    label = { Text("Repite el PIN") },
                    singleLine = true,
                    visualTransformation = pinVisual,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth().testTag("setup_confirm_pin_input")
                )
            }

            if (inputLocked) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Demasiados intentos. Espera $lockLeft s.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            error?.let { msg ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = { submit() },
                enabled = !inputLocked && (setupMode || selectedId != null),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("login_submit_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text(
                    text = when {
                        setupMode -> "Crear y entrar"
                        resettingPin -> "Guardar PIN y entrar"
                        else -> "Ingresar"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (!setupMode && !resettingPin) {
                Spacer(modifier = Modifier.height(6.dp))
                if (singleUser && deviceAuthAvailable) {
                    TextButton(onClick = { launchDeviceAuth { selectedId?.let(onDeviceUnlock) } }) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ingresar con huella o bloqueo del teléfono")
                    }
                }
                TextButton(
                    onClick = {
                        error = when {
                            !singleUser ->
                                "Pide a un administrador que te ponga un PIN nuevo desde Menú › Usuarios."
                            deviceAuthAvailable -> {
                                launchDeviceAuth {
                                    resettingPin = true
                                    pin = ""
                                    confirmPin = ""
                                    error = null
                                }
                                null
                            }
                            else ->
                                "El teléfono no tiene huella ni bloqueo de pantalla. Para recuperar el acceso " +
                                    "hay que borrar los datos de la app y restaurar un respaldo."
                        }
                    }
                ) {
                    Text("¿Olvidaste tu PIN?")
                }
            }
            if (resettingPin) {
                TextButton(onClick = { resettingPin = false; pin = ""; confirmPin = ""; error = null }) {
                    Text("Cancelar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Los datos se guardan en este teléfono y se copian en tu Google Drive.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
