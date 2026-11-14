package com.example.util

import com.example.data.model.DetailedMaintenanceEntity
import com.example.data.model.FleetUnitEntity
import com.example.data.model.InventoryBatchEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/** Lo que se guarda en la copia debe volver igual al recuperarla (usa org.json real). */
class BackupRoundTripTest {

    private fun payload() = FleetBackupPayload(
        exportDate = "2026-10-15 18:00:00",
        userEmail = "dueno@gmail.com",
        units = listOf(FleetUnitEntity(placa = "ADASJD", nombreMoto = "Moto 1", revisionTecnicaMesRealizada = false)),
        dailyPayments = emptyList(),
        debtRecords = emptyList(),
        detailedMaintenances = listOf(
            DetailedMaintenanceEntity(
                unitPlaca = "ADASJD", nombreMoto = "Moto 1", fecha = "2026-10-15", mesAno = "2026-10",
                costoPlanchado = 200.0, costoCablesCambio = 35.0, costoTotal = 235.0
            )
        ),
        unitExpenses = emptyList(),
        financeMovements = emptyList(),
        inventoryItems = emptyList(),
        maintenanceTasks = emptyList(),
        inventoryBatches = listOf(
            InventoryBatchEntity(categoria = "Fluidos", nombre = "Aceite 4T", cantidadInicial = 50,
                cantidadDisponible = 42, precioUnitario = 30.0, fecha = "2026-10-15")
        )
    )

    @Test
    fun `la revision tecnica pendiente sobrevive a la copia`() {
        val back = BackupHelper.parseFromJson(BackupHelper.serializeToJson(payload()))
        assertEquals(false, back.units.single().revisionTecnicaMesRealizada)
    }

    @Test
    fun `cables de cambios sobrevive a la copia`() {
        val back = BackupHelper.parseFromJson(BackupHelper.serializeToJson(payload()))
        assertEquals(35.0, back.detailedMaintenances.single().costoCablesCambio, 0.001)
        assertEquals(200.0, back.detailedMaintenances.single().costoPlanchado, 0.001)
    }

    @Test
    fun `los lotes de aceite y llantas sobreviven a la copia`() {
        val batch = BackupHelper.parseFromJson(BackupHelper.serializeToJson(payload())).inventoryBatches.single()
        assertEquals(42, batch.cantidadDisponible)
        assertEquals(30.0, batch.precioUnitario, 0.001)
    }

    @Test
    fun `una copia vieja sin campos nuevos se lee con valores seguros`() {
        val vieja = """{"units":[{"placa":"ADASJD","nombreMoto":"Moto 1"}],
            "detailedMaintenances":[{"unitPlaca":"ADASJD","nombreMoto":"Moto 1","fecha":"2026-10-01","mesAno":"2026-10"}]}"""
        val back = BackupHelper.parseFromJson(vieja)
        assertEquals(1, back.units.size)
        assertEquals(0.0, back.detailedMaintenances.single().costoCablesCambio, 0.001)
        assertEquals(0, back.inventoryBatches.size)
    }
}
