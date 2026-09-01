package com.motomusic.app.presentation.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {

    @Test
    fun `each part of the day has its own greeting`() {
        assertEquals("Good morning", greetingFor(5))
        assertEquals("Good morning", greetingFor(11))
        assertEquals("Good afternoon", greetingFor(12))
        assertEquals("Good afternoon", greetingFor(16))
        assertEquals("Good evening", greetingFor(17))
        assertEquals("Good evening", greetingFor(21))
        assertEquals("Good night", greetingFor(22))
        assertEquals("Good night", greetingFor(4))
    }
}
