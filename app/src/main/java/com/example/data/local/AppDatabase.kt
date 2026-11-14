package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DailyPaymentEntity
import com.example.data.model.DebtRecordEntity
import com.example.data.model.DetailedMaintenanceEntity
import com.example.data.model.FinanceMovementEntity
import com.example.data.model.FleetUnitEntity
import com.example.data.model.InventoryBatchEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.MaintenanceTaskEntity
import com.example.data.model.UnitExpenseEntity

@Database(
    entities = [
        FleetUnitEntity::class,
        DailyPaymentEntity::class,
        DebtRecordEntity::class,
        DetailedMaintenanceEntity::class,
        UnitExpenseEntity::class,
        FinanceMovementEntity::class,
        InventoryItemEntity::class,
        MaintenanceTaskEntity::class,
        InventoryBatchEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fleetDao(): FleetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return it }
                // Copia del archivo original ANTES de convertirlo a la versión nueva (ver DbSafety)
                try {
                    DbSafety.snapshotIfUpgrading(context.applicationContext, DbMigrations.CURRENT_VERSION)
                } catch (_: Exception) {
                    // Si no se pudo copiar (poco espacio, etc.), se sigue: la migración igual conserva los datos.
                }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DbSafety.DB_NAME
                )
                    // Solo las versiones 1-5 (pruebas de desarrollo) pueden reiniciarse.
                    // Desde la versión 6 TODO cambio de estructura exige una Migration que conserve los datos.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5)
                    .addMigrations(*DbMigrations.ALL)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
