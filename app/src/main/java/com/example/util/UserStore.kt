package com.example.util

import android.content.SharedPreferences
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID

/** Rol de cada persona que usa la app en este teléfono. */
enum class UserRole(val label: String) {
    ADMIN("Administrador"),
    COBRADOR("Cobrador")
}

data class AppUser(
    val id: String,
    val name: String,
    val role: UserRole,
    val salt: String,
    val hash: String
) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN
}

/**
 * Usuarios locales del teléfono, cada uno con su propio PIN.
 *
 * - El PIN nunca se guarda: solo hash + sal (ver [PinAuth.hashPin]).
 * - Cada usuario tiene su propio bloqueo de 30 s tras 5 intentos fallidos.
 * - Siempre debe quedar al menos un Administrador.
 * - Si la app tenía el PIN único de la versión anterior, se convierte automáticamente en el
 *   primer Administrador (no hay que volver a crear nada).
 */
object UserStore {

    enum class AddResult { OK, INVALID_NAME, NAME_TAKEN, INVALID_PIN }
    enum class ChangeResult { OK, NOT_FOUND, LAST_ADMIN }

    private const val KEY_USERS = "users_v1"
    private const val KEY_LAST_USER = "last_user_id"
    private const val KEY_LEGACY_HASH = "pin_hash"
    private const val KEY_LEGACY_SALT = "pin_salt"

    // ---------- lectura ----------

    fun users(prefs: SharedPreferences): List<AppUser> {
        migrateLegacyIfNeeded(prefs)
        return decode(prefs.getString(KEY_USERS, "") ?: "")
    }

    fun find(prefs: SharedPreferences, id: String): AppUser? = users(prefs).firstOrNull { it.id == id }

    fun lastUserId(prefs: SharedPreferences): String? = prefs.getString(KEY_LAST_USER, null)

    // ---------- alta / cambios ----------

    fun addUser(prefs: SharedPreferences, name: String, pin: String, role: UserRole): Pair<AddResult, AppUser?> {
        val clean = name.trim()
        if (clean.isEmpty() || clean.length > 30) return AddResult.INVALID_NAME to null
        if (!PinAuth.isValidPin(pin)) return AddResult.INVALID_PIN to null
        val current = users(prefs)
        if (current.any { it.name.equals(clean, ignoreCase = true) }) return AddResult.NAME_TAKEN to null

        val salt = PinAuth.newSaltHex()
        val user = AppUser(
            id = UUID.randomUUID().toString().take(8),
            name = clean,
            role = role,
            salt = salt,
            hash = PinAuth.hashPin(pin, salt)
        )
        save(prefs, current + user)
        return AddResult.OK to user
    }

    fun rename(prefs: SharedPreferences, id: String, newName: String): AddResult {
        val clean = newName.trim()
        if (clean.isEmpty() || clean.length > 30) return AddResult.INVALID_NAME
        val current = users(prefs)
        if (current.any { it.id != id && it.name.equals(clean, ignoreCase = true) }) return AddResult.NAME_TAKEN
        save(prefs, current.map { if (it.id == id) it.copy(name = clean) else it })
        return AddResult.OK
    }

    fun changePin(prefs: SharedPreferences, id: String, newPin: String): Boolean {
        if (!PinAuth.isValidPin(newPin)) return false
        val current = users(prefs)
        if (current.none { it.id == id }) return false
        val salt = PinAuth.newSaltHex()
        save(prefs, current.map { if (it.id == id) it.copy(salt = salt, hash = PinAuth.hashPin(newPin, salt)) else it })
        prefs.edit().remove(failsKey(id)).remove(lockKey(id)).apply()
        return true
    }

    fun setRole(prefs: SharedPreferences, id: String, role: UserRole): ChangeResult {
        val current = users(prefs)
        val target = current.firstOrNull { it.id == id } ?: return ChangeResult.NOT_FOUND
        if (target.isAdmin && role != UserRole.ADMIN && current.count { it.isAdmin } <= 1) {
            return ChangeResult.LAST_ADMIN
        }
        save(prefs, current.map { if (it.id == id) it.copy(role = role) else it })
        return ChangeResult.OK
    }

    fun remove(prefs: SharedPreferences, id: String): ChangeResult {
        val current = users(prefs)
        val target = current.firstOrNull { it.id == id } ?: return ChangeResult.NOT_FOUND
        if (target.isAdmin && current.count { it.isAdmin } <= 1) return ChangeResult.LAST_ADMIN
        save(prefs, current.filterNot { it.id == id })
        val edit = prefs.edit().remove(failsKey(id)).remove(lockKey(id))
        if (lastUserId(prefs) == id) edit.remove(KEY_LAST_USER)
        edit.apply()
        return ChangeResult.OK
    }

    // ---------- ingreso ----------

    fun lockRemainingMillis(prefs: SharedPreferences, id: String, now: Long = System.currentTimeMillis()): Long {
        val until = prefs.getLong(lockKey(id), 0L)
        return if (until > now) until - now else 0L
    }

    fun verify(prefs: SharedPreferences, id: String, pin: String, now: Long = System.currentTimeMillis()): PinAuth.Result {
        if (lockRemainingMillis(prefs, id, now) > 0L) return PinAuth.Result.LOCKED
        val user = find(prefs, id) ?: return PinAuth.Result.WRONG

        val candidate = PinAuth.hashPin(pin, user.salt)
        val matches = java.security.MessageDigest.isEqual(candidate.toByteArray(), user.hash.toByteArray())
        if (matches) {
            prefs.edit()
                .remove(failsKey(id))
                .remove(lockKey(id))
                .putString(KEY_LAST_USER, id)
                .apply()
            return PinAuth.Result.OK
        }

        val fails = prefs.getInt(failsKey(id), 0) + 1
        return if (fails >= PinAuth.MAX_ATTEMPTS) {
            prefs.edit().putInt(failsKey(id), 0).putLong(lockKey(id), now + PinAuth.LOCK_MILLIS).apply()
            PinAuth.Result.LOCKED
        } else {
            prefs.edit().putInt(failsKey(id), fails).apply()
            PinAuth.Result.WRONG
        }
    }

    fun markLastUser(prefs: SharedPreferences, id: String) {
        prefs.edit().putString(KEY_LAST_USER, id).apply()
    }

    // ---------- internos ----------

    private fun failsKey(id: String) = "pin_fail_count_$id"
    private fun lockKey(id: String) = "pin_lock_until_$id"

    /** La versión anterior tenía un solo PIN: se convierte en el primer Administrador. */
    private fun migrateLegacyIfNeeded(prefs: SharedPreferences) {
        val stored = prefs.getString(KEY_USERS, "") ?: ""
        if (stored.isNotEmpty()) return
        val hash = prefs.getString(KEY_LEGACY_HASH, null) ?: return
        val salt = prefs.getString(KEY_LEGACY_SALT, null) ?: return
        val name = (prefs.getString("user_name", null) ?: "").trim().ifEmpty { "Administrador" }
        val admin = AppUser(id = "admin001", name = name, role = UserRole.ADMIN, salt = salt, hash = hash)
        prefs.edit()
            .putString(KEY_USERS, encode(listOf(admin)))
            .putString(KEY_LAST_USER, admin.id)
            .apply()
    }

    private fun save(prefs: SharedPreferences, list: List<AppUser>) {
        prefs.edit().putString(KEY_USERS, encode(list)).apply()
    }

    internal fun encode(list: List<AppUser>): String = list.joinToString("\n") { u ->
        listOf(u.id, u.role.name, u.salt, u.hash, URLEncoder.encode(u.name, "UTF-8")).joinToString("|")
    }

    internal fun decode(raw: String): List<AppUser> = raw.lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val p = line.split("|")
            if (p.size != 5) return@mapNotNull null
            val role = runCatching { UserRole.valueOf(p[1]) }.getOrNull() ?: return@mapNotNull null
            AppUser(id = p[0], name = URLDecoder.decode(p[4], "UTF-8"), role = role, salt = p[2], hash = p[3])
        }
        .toList()
}
