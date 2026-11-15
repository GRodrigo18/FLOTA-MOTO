package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_ID = "moto_flota_critical_alerts"
    private const val CHANNEL_NAME = "Alertas de Flota y Cobros"
    private const val CHANNEL_DESC = "Avisos urgentes con sonido y mensajes emergentes de SOAT, taller y cobranzas."

    // Canal silencioso del resumen fijo "N recordatorios pendientes"
    const val REMINDERS_CHANNEL_ID = "moto_flota_pending_reminders"
    private const val REMINDERS_CHANNEL_NAME = "Recordatorios pendientes"
    private const val REMINDERS_CHANNEL_DESC = "Resumen fijo y sin sonido de lo que falta resolver."
    const val REMINDERS_NOTIFICATION_ID = 7300
    private const val REMINDERS_MAX_LINES = 6

    /** Extra del Intent a MainActivity: true = abrir la campana de recordatorios. */
    const val EXTRA_OPEN_ALERTS = "open_alerts"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
                setShowBadge(true)
            }

            val reminders = NotificationChannel(
                REMINDERS_CHANNEL_ID,
                REMINDERS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = REMINDERS_CHANNEL_DESC
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
                setShowBadge(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(reminders)
        }
    }

    /** Intent que abre la app con la campana de recordatorios abierta. */
    private fun openAlertsPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ALERTS, true)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Publica o actualiza el aviso FIJO "MotoFlota: N recordatorios pendientes" (sin sonido, no se
     * puede deslizar). Con lista vacía lo quita. Devuelve false si no hay permiso o falla.
     */
    fun showRemindersSummary(context: Context, titles: List<String>): Boolean {
        if (titles.isEmpty()) {
            cancelRemindersSummary(context)
            return true
        }
        return try {
            initNotificationChannel(context)
            if (!hasNotificationPermission(context)) return false

            val n = titles.size
            val title = "MotoFlota: $n recordatorio${if (n == 1) "" else "s"} pendiente${if (n == 1) "" else "s"}"
            val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
            titles.take(REMINDERS_MAX_LINES).forEach { style.addLine(it) }
            if (n > REMINDERS_MAX_LINES) style.addLine("y ${n - REMINDERS_MAX_LINES} más")
            style.setSummaryText("Se quitan solos cuando lo resuelves")

            val builder = NotificationCompat.Builder(context, REMINDERS_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(if (n == 1) titles[0] else "${titles[0]} y ${n - 1} más")
                .setStyle(style)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setShowWhen(false)
                .setNumber(n)
                // Con el teléfono bloqueado no se muestran nombres ni montos.
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(
                    NotificationCompat.Builder(context, REMINDERS_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("MotoFlota Pro")
                        .setContentText("Tienes $n recordatorio${if (n == 1) "" else "s"} pendiente${if (n == 1) "" else "s"}.")
                        .setSilent(true)
                        .build()
                )
                .setContentIntent(openAlertsPendingIntent(context, REMINDERS_NOTIFICATION_ID))

            NotificationManagerCompat.from(context).notify(REMINDERS_NOTIFICATION_ID, builder.build())
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun cancelRemindersSummary(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(REMINDERS_NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun sendSystemAlertNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt() + 1000
    ): Boolean {
        initNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        // Al tocar el aviso se abre la app con la campana de recordatorios
        val pendingIntent = openAlertsPendingIntent(context, notificationId)

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            // Con el teléfono bloqueado no se muestran nombres ni montos, solo el aviso genérico.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("MotoFlota Pro")
                    .setContentText("Tienes avisos nuevos. Desbloquea para verlos.")
                    .build()
            )
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 350, 150, 350))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
            return true
        } catch (_: SecurityException) {
            return false
        }
    }
}
