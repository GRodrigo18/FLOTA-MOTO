package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TourNewsTest {

    @Test
    fun `quien vio las novedades de la v1_4 queda en 14`() {
        assertEquals(14, TourNews.resolveSeenVersion(storedVersion = null, v14Seen = true, completedTour = true))
    }

    @Test
    fun `guia completa sin marca de la v1_4 queda en 0`() {
        assertEquals(0, TourNews.resolveSeenVersion(storedVersion = null, v14Seen = false, completedTour = true))
    }

    @Test
    fun `nunca hizo la guia queda en -1`() {
        assertEquals(-1, TourNews.resolveSeenVersion(storedVersion = null, v14Seen = false, completedTour = false))
    }

    @Test
    fun `la version guardada manda sobre la marca vieja`() {
        assertEquals(15, TourNews.resolveSeenVersion(storedVersion = 15, v14Seen = true, completedTour = true))
    }

    @Test
    fun `en modo novedades solo se ven los pasos posteriores a lo visto`() {
        assertFalse(TourNews.isStepShown(since = 0, sinceVersion = 14))
        assertFalse(TourNews.isStepShown(since = 14, sinceVersion = 14))
        assertTrue(TourNews.isStepShown(since = 15, sinceVersion = 14))
        assertTrue(TourNews.isStepShown(since = 14, sinceVersion = 0))
        assertTrue(TourNews.isStepShown(since = 0, sinceVersion = -1))
    }
}
