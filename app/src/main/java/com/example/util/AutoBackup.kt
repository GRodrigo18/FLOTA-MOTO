package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Respaldo automático. Dos formas de guardarlo:
 *  - ARCHIVO (la recomendada): un archivo "MotoFlota_respaldo.json" creado en Google Drive con el
 *    selector de Android. La app lo actualiza después de cada cambio y Drive lo sube a la cuenta.
 *    (Google Drive no deja elegir CARPETAS desde otras apps, pero sí crear y actualizar archivos.)
 *  - CARPETA: una carpeta del teléfono u otra nube que sí lo permita (versiones anteriores).
 *
 * Detalle del modo carpeta: respaldo automático en una carpeta que elige el dueño (por ejemplo "Sincronizar").
 * Si esa carpeta está dentro de Google Drive (u otra nube instalada en el teléfono), la propia
 * nube se encarga de subir los archivos a la cuenta del usuario. No se necesita ninguna clave ni servidor.
 *
 * Se guarda un archivo por día (MotoFlota_respaldo_AAAA-MM-DD.json) y se conservan los últimos [KEEP_FILES].
 */
object AutoBackup {
    private const val PREFS = "moto_flota_prefs"
    private const val KEY_TREE = "sync_folder_uri"
    private const val KEY_DOC = "sync_doc_uri"
    const val DOC_NAME = "MotoFlota_respaldo.json"
    private const val KEY_NAME = "sync_folder_name"
    private const val KEY_LAST = "last_auto_backup_at"
    const val FILE_PREFIX = "MotoFlota_respaldo_"
    const val KEEP_FILES = 14

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** true si hay dónde copiar (archivo en Drive o carpeta). */
    fun hasFolder(context: Context): Boolean =
        prefs(context).getString(KEY_DOC, null) != null || prefs(context).getString(KEY_TREE, null) != null

    private fun docUri(context: Context): Uri? = prefs(context).getString(KEY_DOC, null)?.let { Uri.parse(it) }

    /** true si el archivo de copia elegido se puede escribir (si es solo de lectura, las copias fallarían). */
    fun canWrite(context: Context): Boolean {
        val doc = docUri(context) ?: return prefs(context).getString(KEY_TREE, null) != null
        return try {
            context.contentResolver.persistedUriPermissions.any { it.uri == doc && it.isWritePermission }
        } catch (_: Exception) {
            false
        }
    }

    /** Usar un archivo (creado o elegido en Google Drive) como destino de la copia automática. */
    fun setDocument(context: Context, doc: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                doc,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            try {
                context.contentResolver.takePersistableUriPermission(doc, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
            }
        }
        prefs(context).edit()
            .putString(KEY_DOC, doc.toString())
            .putString(KEY_NAME, docDisplayName(context, doc) ?: DOC_NAME)
            .remove(KEY_TREE)
            .remove(KEY_LAST)
            .apply()
    }

    private fun docDisplayName(context: Context, doc: Uri): String? = try {
        context.contentResolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (_: Exception) {
        null
    }

    /**
     * El permiso de una carpeta es de ESTE teléfono: no viaja con la copia de seguridad de Android.
     * Si se restauró la app en otro teléfono (o el usuario quitó el acceso), se olvida la carpeta
     * para que la app vuelva a pedirla en vez de fallar en silencio. Devuelve true si se quitó.
     */
    fun dropFolderIfNoAccess(context: Context): Boolean {
        docUri(context)?.let { doc ->
            val persisted = context.contentResolver.persistedUriPermissions.any { it.uri == doc }
            if (persisted) return false
            val readable = try {
                context.contentResolver.openInputStream(doc)?.close()
                true
            } catch (_: Exception) {
                false
            }
            if (readable) return false
            clearFolder(context)
            return true
        }
        val raw = prefs(context).getString(KEY_TREE, null) ?: return false
        val tree = Uri.parse(raw)
        val persisted = context.contentResolver.persistedUriPermissions.any {
            it.uri == tree && it.isWritePermission
        }
        if (persisted) return false
        val readable = try {
            listChildren(context, tree)
            true
        } catch (_: Exception) {
            false
        }
        if (readable) return false
        clearFolder(context)
        return true
    }

    fun folderName(context: Context): String? = prefs(context).getString(KEY_NAME, null)

    fun lastBackupAt(context: Context): Long = prefs(context).getLong(KEY_LAST, 0L)

    fun setFolder(context: Context, tree: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                tree,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Algunos proveedores no permiten permisos persistentes; se intentará igualmente.
        }
        val name = displayName(context, tree)
        prefs(context).edit()
            .putString(KEY_TREE, tree.toString())
            .putString(KEY_NAME, name)
            .remove(KEY_LAST)
            .apply()
    }

    fun clearFolder(context: Context) {
        prefs(context).edit().remove(KEY_TREE).remove(KEY_DOC).remove(KEY_NAME).remove(KEY_LAST).apply()
    }

    /** Guarda el respaldo de hoy en la carpeta elegida. Devuelve true si se pudo escribir. */
    fun writeBackup(context: Context, json: String): Boolean {
        docUri(context)?.let { doc -> return writeDocument(context, doc, json) }
        val tree = prefs(context).getString(KEY_TREE, null)?.let { Uri.parse(it) } ?: return false
        return try {
            val resolver = context.contentResolver
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val fileName = "$FILE_PREFIX$today.json"

            val children = listChildren(context, tree)
            // Si ya hay uno de hoy, se reemplaza.
            children.filter { it.second == fileName }.forEach { deleteDoc(context, tree, it.first) }

            val doc = DocumentsContract.createDocument(resolver, parent, "application/json", fileName)
                ?: return false
            val stream = resolver.openOutputStream(doc, "wt") ?: return false
            stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }

            // Conservar solo los más recientes (el nombre incluye la fecha, así que ordena por nombre)
            children
                .filter { it.second.startsWith(FILE_PREFIX) && it.second != fileName }
                .sortedByDescending { it.second }
                .drop(KEEP_FILES - 1)
                .forEach { deleteDoc(context, tree, it.first) }

            prefs(context).edit().putLong(KEY_LAST, System.currentTimeMillis()).apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Reemplaza el contenido del archivo y luego lo vuelve a leer para confirmar que quedó bien
     * (si la nube no lo aceptó completo, se informa "Falta copiar" en vez de dar una falsa tranquilidad).
     */
    private fun writeDocument(context: Context, doc: Uri, json: String): Boolean {
        val resolver = context.contentResolver
        val bytes = json.toByteArray(Charsets.UTF_8)
        val written = listOf("wt", "w").any { mode ->
            try {
                resolver.openOutputStream(doc, mode)?.use { it.write(bytes) } != null
            } catch (_: Exception) {
                false
            }
        }
        if (!written) return false
        val ok = try {
            resolver.openInputStream(doc)?.use { it.readBytes().contentEquals(bytes) } ?: false
        } catch (_: Exception) {
            false
        }
        if (ok) prefs(context).edit().putLong(KEY_LAST, System.currentTimeMillis()).apply()
        return ok
    }

    /** Lee el respaldo más reciente de la carpeta elegida (para restaurar en un teléfono nuevo). */
    fun readLatestBackup(context: Context): String? {
        docUri(context)?.let { doc ->
            return try {
                context.contentResolver.openInputStream(doc)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?.takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }
        val tree = prefs(context).getString(KEY_TREE, null)?.let { Uri.parse(it) } ?: return null
        return try {
            val latest = listChildren(context, tree)
                .filter { it.second.startsWith(FILE_PREFIX) }
                .maxByOrNull { it.second } ?: return null
            val doc = DocumentsContract.buildDocumentUriUsingTree(tree, latest.first)
            context.contentResolver.openInputStream(doc)?.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (_: Exception) {
            null
        }
    }

    /** Fecha (AAAA-MM-DD) del respaldo más reciente que hay en la carpeta, o null si no hay ninguno. */
    fun latestBackupDate(context: Context): String? {
        docUri(context)?.let { doc ->
            return try {
                context.contentResolver.query(
                    doc,
                    arrayOf(DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                    null, null, null
                )?.use { c ->
                    if (!c.moveToFirst() || c.getLong(0) <= 0L) null
                    else SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(c.getLong(1)))
                }
            } catch (_: Exception) {
                null
            }
        }
        val tree = prefs(context).getString(KEY_TREE, null)?.let { Uri.parse(it) } ?: return null
        return try {
            listChildren(context, tree)
                .map { it.second }
                .filter { it.startsWith(FILE_PREFIX) && it.endsWith(".json") }
                .maxOrNull()
                ?.removePrefix(FILE_PREFIX)
                ?.removeSuffix(".json")
        } catch (_: Exception) {
            null
        }
    }

    private fun listChildren(context: Context, tree: Uri): List<Pair<String, String>> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree, DocumentsContract.getTreeDocumentId(tree)
        )
        val result = mutableListOf<Pair<String, String>>()
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val name = c.getString(1)
                if (id != null && name != null) result.add(id to name)
            }
        }
        return result
    }

    private fun deleteDoc(context: Context, tree: Uri, docId: String) {
        try {
            DocumentsContract.deleteDocument(
                context.contentResolver,
                DocumentsContract.buildDocumentUriUsingTree(tree, docId)
            )
        } catch (_: Exception) {
            // Si no se puede borrar un archivo viejo, no es grave.
        }
    }

    private fun displayName(context: Context, tree: Uri): String {
        return try {
            val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            context.contentResolver.query(
                doc,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "Carpeta elegida"
        } catch (_: Exception) {
            "Carpeta elegida"
        }
    }
}
