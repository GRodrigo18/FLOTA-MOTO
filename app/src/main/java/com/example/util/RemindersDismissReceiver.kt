package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Se llama cuando alguien desliza el aviso fijo de recordatorios pendientes. Como los recordatorios
 * solo se quitan al resolverlos, el aviso vuelve a aparecer (sin sonido).
 */
class RemindersDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        // Se vuelve a poner al instante con lo último conocido...
        NotificationHelper.repostSummaryAfterDismiss(app)
        // ...y se recalcula enseguida con los datos de ahora (por si algo ya se resolvió)
        try {
            val request = androidx.work.OneTimeWorkRequestBuilder<DailyJobWorker>()
                .setInputData(androidx.work.workDataOf(DailyJobWorker.KEY_MODE to DailyJobWorker.MODE_SUMMARY))
                .build()
            androidx.work.WorkManager.getInstance(app)
                .enqueueUniqueWork("moto_flota_resumen", androidx.work.ExistingWorkPolicy.REPLACE, request)
        } catch (_: Exception) {
        }
    }
}
