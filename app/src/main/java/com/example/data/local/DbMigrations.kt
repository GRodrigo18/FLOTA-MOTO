package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migraciones de la base de datos. REGLA: cada cambio de estructura sube la versión en
 * AppDatabase y agrega aquí una Migration que CONSERVE los datos (nunca borrar tablas).
 */
object DbMigrations {

    /** Debe ser igual a @Database(version = ...) en AppDatabase. */
    const val CURRENT_VERSION = 7

    /** SQL de la versión 6 → 7: nuevo costo "Cables de cambios" en mantenimientos (0 en los registros viejos). */
    const val SQL_6_7 = "ALTER TABLE detailed_maintenances ADD COLUMN costoCablesCambio REAL NOT NULL DEFAULT 0.0"

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(SQL_6_7)
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_6_7)
}
