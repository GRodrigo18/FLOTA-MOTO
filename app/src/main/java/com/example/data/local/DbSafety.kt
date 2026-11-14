package com.example.data.local

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Red de seguridad para actualizaciones.
 *
 * Antes de que Room convierta la base de datos a una versión nueva, se copia el archivo tal cual
 * (más sus archivos -wal y -shm) en files/db_antes_de_actualizar/. Si una actualización saliera
 * mal, los datos originales siguen ahí y se pueden recuperar desde Android Studio
 * (View › Tool Windows › Device Explorer › data/data/<paquete>/files/db_antes_de_actualizar).
 * Se conservan las 3 copias más recientes.
 */
object DbSafety {
    const val DB_NAME = "motoflota_database.db"
    const val SNAPSHOT_DIR = "db_antes_de_actualizar"
    private const val KEEP = 3
    private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    /**
     * Lee la versión de la base de datos (PRAGMA user_version) desde la cabecera del archivo,
     * sin abrirla. Devuelve null si no es un archivo SQLite válido.
     */
    fun readUserVersion(header: ByteArray): Int? {
        if (header.size < 64) return null
        for (i in SQLITE_MAGIC.indices) if (header[i] != SQLITE_MAGIC[i]) return null
        return ((header[60].toInt() and 0xFF) shl 24) or
            ((header[61].toInt() and 0xFF) shl 16) or
            ((header[62].toInt() and 0xFF) shl 8) or
            (header[63].toInt() and 0xFF)
    }

    fun snapshotIfUpgrading(context: Context, targetVersion: Int): File? =
        snapshotIfUpgrading(context.getDatabasePath(DB_NAME), File(context.filesDir, SNAPSHOT_DIR), targetVersion)

    /** Versión sin Android (rutas explícitas) para poder probarla. */
    fun snapshotIfUpgrading(dbFile: File, snapshotRoot: File, targetVersion: Int, now: Long = System.currentTimeMillis()): File? {
        if (!dbFile.exists()) return null
        val header = ByteArray(100)
        val read = dbFile.inputStream().use { it.read(header) }
        if (read < 64) return null
        val current = readUserVersion(header) ?: return null
        if (current <= 0 || current >= targetVersion) return null

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))
        val dir = File(snapshotRoot, "v${current}_a_v${targetVersion}_$stamp").apply { mkdirs() }
        for (suffix in listOf("", "-wal", "-shm")) {
            val src = File(dbFile.path + suffix)
            if (src.exists()) src.copyTo(File(dir, src.name), overwrite = true)
        }
        snapshotRoot.listFiles { f -> f.isDirectory }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(KEEP)
            ?.forEach { it.deleteRecursively() }
        return dir
    }
}
