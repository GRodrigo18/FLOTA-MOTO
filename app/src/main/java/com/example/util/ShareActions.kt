package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Compartir textos o archivos con otras apps del teléfono (correo, Drive, imprimir...).
 * No usan internet desde MotoFlota ni piden permisos: el teléfono abre la app que corresponda.
 */
object ShareActions {

    fun shareText(context: Context, text: String, title: String): Boolean = try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) {
        false
    }

    /** Guarda el CSV en la caché privada y abre "Compartir" (Drive, Gmail, Excel...). */
    fun shareCsv(context: Context, fileName: String, csv: String): Boolean = try {
        val dir = File(context.cacheDir, "reportes").apply { mkdirs() }
        // Solo se deja el último reporte en caché
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, fileName)
        file.writeText(csv, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName.removeSuffix(".csv").replace('_', ' '))
            clipData = android.content.ClipData.newRawUri("reporte", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "Enviar reporte").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
        true
    } catch (_: Exception) {
        false
    }
}
