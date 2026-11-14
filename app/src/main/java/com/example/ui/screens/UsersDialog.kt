package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.FleetUiState
import com.example.ui.FleetViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NavyPrimary
import com.example.util.AppUser
import com.example.util.PinAuth
import com.example.util.UserRole

private enum class UsersMode { LIST, ADD, RESET_PIN }

/**
 * Administración de usuarios (solo Administrador):
 * crear usuarios, ponerles un PIN nuevo, cambiar su rol o eliminarlos.
 */
@Composable
fun ManageUsersDialog(
    state: FleetUiState,
    viewModel: FleetViewModel,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf(UsersMode.LIST) }
    var target by remember { mutableStateOf<AppUser?>(null) }
    var confirmDelete by remember { mutableStateOf<AppUser?>(null) }

    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.COBRADOR) }
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun resetForm() {
        name = ""; role = UserRole.COBRADOR; pin = ""; pin2 = ""; error = null
    }

    confirmDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("¿Eliminar a ${user.name}?") },
            text = { Text("Ya no podrá ingresar a la app. Los registros que hizo se conservan.") },
            confirmButton = {
                Button(onClick = { viewModel.removeUser(user.id); confirmDelete = null }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancelar") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (mode) {
                    UsersMode.LIST -> "Usuarios"
                    UsersMode.ADD -> "Nuevo usuario"
                    UsersMode.RESET_PIN -> "PIN nuevo para ${target?.name ?: ""}"
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (mode) {
                    UsersMode.LIST -> {
                        Text(
                            text = "Administrador: ve todo, incluida la Caja, respaldos y usuarios.\n" +
                                "Cobrador: registra pagos, deudas, flota y taller; no ve la Caja.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.users.forEach { u ->
                            UserRow(
                                user = u,
                                isMe = u.id == state.currentUserId,
                                onRole = { newRole -> viewModel.setUserRole(u.id, newRole) },
                                onResetPin = { target = u; resetForm(); mode = UsersMode.RESET_PIN },
                                onDelete = { confirmDelete = u }
                            )
                        }
                        OutlinedButton(
                            onClick = { resetForm(); mode = UsersMode.ADD },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agregar usuario")
                        }
                    }

                    UsersMode.ADD -> {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 30) name = it },
                            label = { Text("Nombre") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Rol", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UserRole.entries.forEach { r ->
                                FilterChip(
                                    selected = role == r,
                                    onClick = { role = r },
                                    label = { Text(r.label) }
                                )
                            }
                        }
                        PinFields(pin, pin2, { pin = it }, { pin2 = it })
                    }

                    UsersMode.RESET_PIN -> {
                        Text(
                            "Dale este PIN a la persona. Podrá cambiarlo luego desde Menú › Cambiar mi PIN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PinFields(pin, pin2, { pin = it }, { pin2 = it })
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            when (mode) {
                UsersMode.LIST -> Button(onClick = onDismiss) { Text("Listo") }
                UsersMode.ADD, UsersMode.RESET_PIN -> Button(onClick = {
                    error = when {
                        !PinAuth.isValidPin(pin) -> "El PIN debe tener entre ${PinAuth.MIN_LENGTH} y ${PinAuth.MAX_LENGTH} números."
                        pin != pin2 -> "Los dos PIN no coinciden."
                        mode == UsersMode.ADD -> viewModel.addUser(name, pin, role)
                        else -> target?.let { viewModel.resetUserPin(it.id, pin) }
                    }
                    if (error == null) {
                        resetForm()
                        mode = UsersMode.LIST
                    }
                }) { Text("Guardar") }
            }
        },
        dismissButton = {
            if (mode != UsersMode.LIST) {
                TextButton(onClick = { resetForm(); mode = UsersMode.LIST }) { Text("Volver") }
            }
        }
    )
}

@Composable
private fun UserRow(
    user: AppUser,
    isMe: Boolean,
    onRole: (UserRole) -> Unit,
    onResetPin: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(NavyPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.name.take(1).uppercase(), color = CyanAccent, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isMe) "${user.name} (tú)" else user.name,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(user.role.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onResetPin) {
                    Icon(Icons.Default.Key, contentDescription = "Poner PIN nuevo a ${user.name}")
                }
                if (!isMe) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar a ${user.name}", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserRole.entries.forEach { r ->
                    FilterChip(
                        selected = user.role == r,
                        onClick = { if (user.role != r) onRole(r) },
                        label = { Text(r.label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinFields(pin: String, pin2: String, onPin: (String) -> Unit, onPin2: (String) -> Unit) {
    fun accept(v: String) = v.length <= PinAuth.MAX_LENGTH && v.all { it in '0'..'9' }
    OutlinedTextField(
        value = pin,
        onValueChange = { if (accept(it)) onPin(it) },
        label = { Text("PIN (${PinAuth.MIN_LENGTH} a ${PinAuth.MAX_LENGTH} números)") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = pin2,
        onValueChange = { if (accept(it)) onPin2(it) },
        label = { Text("Repite el PIN") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Cualquier usuario cambia su propio PIN confirmando el actual. */
@Composable
fun ChangeOwnPinDialog(
    viewModel: FleetViewModel,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar mi PIN", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = current,
                    onValueChange = { v -> if (v.length <= PinAuth.MAX_LENGTH && v.all { it in '0'..'9' }) current = v },
                    label = { Text("PIN actual") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                PinFields(pin, pin2, { pin = it }, { pin2 = it })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            Button(onClick = {
                error = when {
                    !PinAuth.isValidPin(pin) -> "El PIN nuevo debe tener entre ${PinAuth.MIN_LENGTH} y ${PinAuth.MAX_LENGTH} números."
                    pin != pin2 -> "Los dos PIN nuevos no coinciden."
                    else -> viewModel.changeOwnPin(current, pin)
                }
                if (error == null) onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/**
 * Borra todo lo de este teléfono para hacer pruebas desde cero. Pide el PIN del administrador.
 * Las copias guardadas en la carpeta NO se borran: se pueden recuperar al volver a configurar.
 */
@Composable
fun StartFromZeroDialog(
    viewModel: FleetViewModel,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Empezar de cero", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Se borran motos, pagos, caja, taller y usuarios de este teléfono. " +
                        "La copia de tu Google Drive se conserva.",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { v -> if (v.length <= PinAuth.MAX_LENGTH && v.all { it in '0'..'9' }) pin = v },
                    label = { Text("Tu PIN para confirmar") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            Button(
                onClick = { error = viewModel.wipeEverything(pin) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Borrar todo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
