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
        NotificationHelper.repostSummaryAfterDismiss(context.applicationContext)
    }
}
