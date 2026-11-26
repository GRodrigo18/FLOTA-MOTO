package com.example.e2e

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.repository.FleetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Prueba de humo del modo de prueba: Room real en memoria dentro de Robolectric. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SmokeRobolectricTest {
    @Test
    fun `la base de datos real arranca vacia`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        val repo = FleetRepository(db.fleetDao(), db)
        assertEquals(0, repo.allUnits.first().size)
        db.close()
    }
}
