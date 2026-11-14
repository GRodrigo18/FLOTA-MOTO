package com.example.util

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Programa los avisos diarios y el respaldo automático.
 *
 * No se usa un "trabajo periódico" de Android, porque ese tipo de tarea no garantiza una hora
 * exacta: cada repetición se cuenta desde que corrió la anterior, así que con los días la hora
 * se va atrasando poco a poco. En su lugar, cada ejecución de [DailyJobWorker] programa la
 * siguiente ella misma, siempre calculando la hora exacta del día siguiente. Así nunca se acumula
 * atraso, aunque el teléfono haya estado apagado o sin batería un rato.
 */
object BackupScheduler {

    const val WORK_MORNING = "avisos_manana"
    const val WORK_EVENING = "avisos_tarde_y_respaldo"
    const val HORA_AVISO_MANANA = 8
    const val MINUTO_AVISO_MANANA = 0
    const val HORA_AVISO_TARDE = 19
    const val MINUTO_AVISO_TARDE = 30

    fun schedule(context: Context) {
        scheduleNext(context, WORK_MORNING, DailyJobWorker.MODE_MORNING, HORA_AVISO_MANANA, MINUTO_AVISO_MANANA, replace = false)
        scheduleNext(context, WORK_EVENING, DailyJobWorker.MODE_EVENING, HORA_AVISO_TARDE, MINUTO_AVISO_TARDE, replace = false)
    }

    /** Usado por el propio worker al terminar, para encadenar su siguiente ejecución (24 h después, misma hora exacta). */
    fun scheduleNext(context: Context, workName: String, mode: String, hour: Int, minute: Int, replace: Boolean) {
        val request = OneTimeWorkRequestBuilder<DailyJobWorker>()
            .setInitialDelay(delayUntil(hour, minute), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(DailyJobWorker.KEY_MODE to mode))
            .build()
        val policy = if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        WorkManager.getInstance(context).enqueueUniqueWork(workName, policy, request)
    }

    /** Milisegundos hasta la próxima vez que sean las [hour]:[minute]. */
    fun delayUntil(hour: Int, minute: Int, now: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= now) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis - now
    }
}
