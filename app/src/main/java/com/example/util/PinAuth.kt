package com.example.util

import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Acceso local con PIN (4 a 6 dígitos).
 *
 * - El PIN nunca se guarda: solo un hash SHA-256 con sal aleatoria y 60.000 iteraciones.
 * - Tras 5 intentos fallidos se bloquea el ingreso durante 30 segundos.
 * - La recuperación del PIN se hace verificando la identidad con la huella/bloqueo del teléfono
 *   (ver BiometricHelper), nunca con un valor fijo dentro de la app.
 */
object PinAuth {

    enum class Result { OK, WRONG, LOCKED }

    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 6
    const val MAX_ATTEMPTS = 5
    const val LOCK_MILLIS = 30_000L

    private const val ITERATIONS = 60_000
    private const val KEY_HASH = "pin_hash"
    private const val KEY_SALT = "pin_salt"
    private const val KEY_FAILS = "pin_fail_count"
    private const val KEY_LOCK_UNTIL = "pin_lock_until"

    fun isValidPin(pin: String): Boolean =
        pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }

    fun hasPin(prefs: SharedPreferences): Boolean =
        prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)

    /** Hash del PIN con la sal indicada (hex). Función pura, apta para pruebas unitarias. */
    fun hashPin(pin: String, saltHex: String): String {
        val salt = fromHex(saltHex)
        val md = MessageDigest.getInstance("SHA-256")
        var data = md.digest(salt + pin.toByteArray(Charsets.UTF_8))
        repeat(ITERATIONS) { data = md.digest(data + salt) }
        return toHex(data)
    }

    fun newSaltHex(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return toHex(salt)
    }

    fun setPin(prefs: SharedPreferences, pin: String) {
        val salt = newSaltHex()
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, hashPin(pin, salt))
            .remove(KEY_FAILS)
            .remove(KEY_LOCK_UNTIL)
            .apply()
    }

    fun lockRemainingMillis(prefs: SharedPreferences, now: Long = System.currentTimeMillis()): Long {
        val until = prefs.getLong(KEY_LOCK_UNTIL, 0L)
        return if (until > now) until - now else 0L
    }

    fun verify(prefs: SharedPreferences, pin: String, now: Long = System.currentTimeMillis()): Result {
        if (lockRemainingMillis(prefs, now) > 0L) return Result.LOCKED

        val salt = prefs.getString(KEY_SALT, null)
        val stored = prefs.getString(KEY_HASH, null)
        if (salt == null || stored == null) return Result.WRONG

        val candidate = hashPin(pin, salt)
        val matches = MessageDigest.isEqual(candidate.toByteArray(), stored.toByteArray())

        if (matches) {
            prefs.edit().remove(KEY_FAILS).remove(KEY_LOCK_UNTIL).apply()
            return Result.OK
        }

        val fails = prefs.getInt(KEY_FAILS, 0) + 1
        return if (fails >= MAX_ATTEMPTS) {
            prefs.edit().putInt(KEY_FAILS, 0).putLong(KEY_LOCK_UNTIL, now + LOCK_MILLIS).apply()
            Result.LOCKED
        } else {
            prefs.edit().putInt(KEY_FAILS, fails).apply()
            Result.WRONG
        }
    }

    private fun toHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun fromHex(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}
