package com.example.data

import com.example.data.local.DbSafety
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DbSafetyTest {

    /** Cabecera mínima de un archivo SQLite con la versión indicada (PRAGMA user_version). */
    private fun sqliteHeader(version: Int): ByteArray {
        val bytes = ByteArray(100)
        "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII).copyInto(bytes)
        bytes[60] = (version shr 24).toByte(); bytes[61] = (version shr 16).toByte()
        bytes[62] = (version shr 8).toByte(); bytes[63] = version.toByte()
        return bytes
    }

    @Test
    fun `lee la version de la base sin abrirla`() {
        assertEquals(6, DbSafety.readUserVersion(sqliteHeader(6)))
        assertNull(DbSafety.readUserVersion(ByteArray(100)))
    }

    @Test
    fun `copia la base antes de actualizar y solo cuando hace falta`() {
        val dir = Files.createTempDirectory("dbsafety").toFile()
        val db = File(dir, DbSafety.DB_NAME).apply { writeBytes(sqliteHeader(6) + ByteArray(4000)) }
        File(dir, DbSafety.DB_NAME + "-wal").writeText("wal")
        val root = File(dir, "snapshots")

        val snap = DbSafety.snapshotIfUpgrading(db, root, targetVersion = 7)
        assertNotNull(snap)
        assertEquals(db.length(), File(snap, DbSafety.DB_NAME).length())
        assertTrue(File(snap, DbSafety.DB_NAME + "-wal").exists())

        assertNull(DbSafety.snapshotIfUpgrading(db, root, targetVersion = 6))
        assertNull(DbSafety.snapshotIfUpgrading(File(dir, "no_existe.db"), root, targetVersion = 7))
        dir.deleteRecursively()
    }
}
