package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserStoreTest {

    @Test
    fun `el PIN unico de la version anterior se convierte en administrador`() {
        val prefs = FakeSharedPreferences()
        prefs.edit().putString("user_name", "Rodrigo").apply()
        PinAuth.setPin(prefs, "4821")
        val users = UserStore.users(prefs)
        assertEquals(1, users.size)
        assertTrue(users[0].isAdmin)
        assertEquals("Rodrigo", users[0].name)
        assertEquals(PinAuth.Result.OK, UserStore.verify(prefs, users[0].id, "4821"))
    }

    @Test
    fun `cada usuario entra solo con su propio PIN`() {
        val prefs = FakeSharedPreferences()
        val admin = UserStore.addUser(prefs, "Rodrigo", "1111", UserRole.ADMIN).second
        val cobrador = UserStore.addUser(prefs, "Luis", "2222", UserRole.COBRADOR).second
        assertNotNull(admin); assertNotNull(cobrador)
        assertEquals(PinAuth.Result.WRONG, UserStore.verify(prefs, cobrador!!.id, "1111"))
        assertEquals(PinAuth.Result.OK, UserStore.verify(prefs, cobrador.id, "2222"))
        assertEquals(cobrador.id, UserStore.lastUserId(prefs))
    }

    @Test
    fun `no se repiten nombres y el PIN debe ser valido`() {
        val prefs = FakeSharedPreferences()
        UserStore.addUser(prefs, "Rodrigo", "1111", UserRole.ADMIN)
        assertEquals(UserStore.AddResult.NAME_TAKEN, UserStore.addUser(prefs, "rodrigo", "3333", UserRole.COBRADOR).first)
        assertEquals(UserStore.AddResult.INVALID_PIN, UserStore.addUser(prefs, "Ana", "12", UserRole.COBRADOR).first)
        assertEquals(UserStore.AddResult.INVALID_NAME, UserStore.addUser(prefs, "  ", "1234", UserRole.COBRADOR).first)
    }

    @Test
    fun `el bloqueo por intentos fallidos es por usuario`() {
        val prefs = FakeSharedPreferences()
        val admin = UserStore.addUser(prefs, "Rodrigo", "1111", UserRole.ADMIN).second!!
        val cobrador = UserStore.addUser(prefs, "Luis", "2222", UserRole.COBRADOR).second!!
        val t = 10_000L
        repeat(4) { UserStore.verify(prefs, cobrador.id, "0000", t) }
        assertEquals(PinAuth.Result.LOCKED, UserStore.verify(prefs, cobrador.id, "0000", t))
        assertEquals(PinAuth.Result.OK, UserStore.verify(prefs, admin.id, "1111", t))
        assertEquals(PinAuth.Result.OK, UserStore.verify(prefs, cobrador.id, "2222", t + 31_000))
    }

    @Test
    fun `siempre queda al menos un administrador`() {
        val prefs = FakeSharedPreferences()
        val admin = UserStore.addUser(prefs, "Rodrigo", "1111", UserRole.ADMIN).second!!
        val luis = UserStore.addUser(prefs, "Luis", "2222", UserRole.COBRADOR).second!!
        assertEquals(UserStore.ChangeResult.LAST_ADMIN, UserStore.remove(prefs, admin.id))
        assertEquals(UserStore.ChangeResult.LAST_ADMIN, UserStore.setRole(prefs, admin.id, UserRole.COBRADOR))
        assertEquals(UserStore.ChangeResult.OK, UserStore.setRole(prefs, luis.id, UserRole.ADMIN))
        assertEquals(UserStore.ChangeResult.OK, UserStore.remove(prefs, admin.id))
        assertEquals(1, UserStore.users(prefs).size)
    }

    @Test
    fun `cambiar el PIN invalida el anterior`() {
        val prefs = FakeSharedPreferences()
        val admin = UserStore.addUser(prefs, "Rodrigo", "1111", UserRole.ADMIN).second!!
        assertTrue(UserStore.changePin(prefs, admin.id, "9999"))
        assertEquals(PinAuth.Result.WRONG, UserStore.verify(prefs, admin.id, "1111"))
        assertEquals(PinAuth.Result.OK, UserStore.verify(prefs, admin.id, "9999"))
    }
}
