package com.example.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.FleetRepository
import com.example.ui.AlertEngine
import com.example.ui.AlertUnit
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Tarea diaria en segundo plano (funciona aunque la app esté cerrada):
 *  - MAÑANA (8:00): avisa de SOAT por vencer, contratos, deudas, stock y recuerda el respaldo por correo.
 *  - TARDE (19:30): avisa de las cuotas del día sin cobrar y guarda el respaldo en la carpeta elegida.
 *
 * Al terminar, siempre programa su propia siguiente ejecución a la misma hora exacta del día
 * siguiente (ver [BackupScheduler]), así nunca se acumula atraso.
 */
class DailyJobWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val mode = inputData.getString(KEY_MODE) ?: MODE_MORNING
        try {
            runOnce(ctx, mode)
        } catch (_: Exception) {
            // No se reintenta de inmediato: la tarea volverá a correr en su próximo horario.
        } finally {
            // Pase lo que pase arriba, siempre se programa el aviso de mañana a la misma hora.
            rescheduleNext(ctx, mode)
        }
        return Result.success()
    }

    private fun rescheduleNext(ctx: Context, mode: String) {
        val (workName, hour, minute) = if (mode == MODE_MORNING) {
            Triple(BackupScheduler.WORK_MORNING, BackupScheduler.HORA_AVISO_MANANA, BackupScheduler.MINUTO_AVISO_MANANA)
        } else {
            Triple(BackupScheduler.WORK_EVENING, BackupScheduler.HORA_AVISO_TARDE, BackupScheduler.MINUTO_AVISO_TARDE)
        }
        BackupScheduler.scheduleNext(ctx, workName, mode, hour, minute, replace = true)
    }

    private suspend fun runOnce(ctx: Context, mode: String) {
        val repo = FleetRepository(AppDatabase.getInstance(ctx).fleetDao())
        val prefs = ctx.getSharedPreferences("moto_flota_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

        // Si la app no se abrió el día 1, el aviso de la mañana igual reinicia la revisión del mes.
        if (MonthlyRevision.checkAndMark(prefs)) repo.resetMonthlyRevision()

        val units = repo.allUnits.first()
        // Aunque no haya motos se evalúa todo: el stock bajo de aceite o llantas también es un recordatorio
        // (igual que en la app). Si no queda nada pendiente, el aviso fijo se quita solo.

        val payments = repo.allDailyPayments.first()
        val inventory = repo.allInventory.first()
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val aceite = inventory.firstOrNull {
            it.categoria == "Fluidos" || it.nombre.contains("Aceite", ignoreCase = true)
        }
        val llanta = inventory.firstOrNull {
            it.categoria == "Neumáticos" || it.nombre.contains("Llanta", ignoreCase = true) || it.nombre.contains("Neumático", ignoreCase = true)
        }

        val alerts = AlertEngine.sortByUrgency(AlertEngine.build(
            units = units.map { u ->
                AlertUnit(
                    placa = u.placa,
                    nombre = u.nombreMoto,
                    estado = u.estado,
                    cliente = u.cliente,
                    soatDias = u.soatDiasRestantes,
                    soatFin = u.soatFin,
                    revisionPendiente = !u.revisionTecnicaMesRealizada,
                    finContrato = u.finContrato,
                    deuda = u.deudaTotal
                )
            },
            paidTodayPlacas = payments.filter { it.fecha == hoy && it.pagado }.map { it.unitPlaca }.toSet(),
            stockAceite = if (inventory.isNotEmpty()) (aceite?.stock ?: 0) else null,
            hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            stockLlantas = llanta?.stock,
            minimoLlantas = llanta?.stockMinimo?.coerceAtLeast(1) ?: AlertEngine.STOCK_LLANTAS_MINIMO
        ))

        // Resumen fijo de recordatorios pendientes (todos, también los ocultos en Inicio)
        if (notificationsEnabled) {
            NotificationHelper.showRemindersSummary(ctx, alerts.map { it.titulo })
        } else {
            NotificationHelper.cancelRemindersSummary(ctx)
        }

        // En la mañana se omite "revisión del mes" (es un aviso mensual, no diario)
        val lines = alerts
            .filter { mode == MODE_EVENING || it.id.startsWith("rev_").not() }
            .map { it.titulo }
            .toMutableList()

        if (mode == MODE_MORNING) {
            val lastEmail = prefs.getLong("last_email_backup_at", 0L)
            if (System.currentTimeMillis() - lastEmail > SEVEN_DAYS_MS) {
                lines.add("Hace más de 7 días que no envías el respaldo a tu correo.")
            }
        }

        // El respaldo automático de la tarde se guarda aunque las notificaciones estén
        // silenciadas; solo se omite el AVISO en la barra de notificaciones.
        if (lines.isNotEmpty() && notificationsEnabled) {
            val title = "MotoFlota: ${lines.size} aviso${if (lines.size == 1) "" else "s"}"
            val message = lines.take(6).joinToString("\n") { "• $it" } +
                if (lines.size > 6) "\n… y ${lines.size - 6} más" else ""
            NotificationHelper.sendSystemAlertNotification(
                context = ctx,
                title = title,
                message = message,
                notificationId = if (mode == MODE_MORNING) 7001 else 7002
            )
        }

        AlertMemory.markNotified(prefs, alerts.map { it.id })

        // Nunca un respaldo vacío (sin motos): en un teléfono nuevo pisaría el respaldo bueno en Drive
        if (mode == MODE_EVENING && units.isNotEmpty() && AutoBackup.hasFolder(ctx)) {
            val email = prefs.getString("user_email", "") ?: ""
            val json = BackupHelper.serializeToJson(repo.getFullBackupPayload(email))
            AutoBackup.writeBackup(ctx, json)
        }
    }

    companion object {
        const val KEY_MODE = "mode"
        const val MODE_MORNING = "morning"
        const val MODE_EVENING = "evening"
        private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    }
}
