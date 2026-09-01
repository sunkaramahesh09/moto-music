package com.motomusic.app.presentation.app

import java.util.Calendar

/** Time-of-day greeting shown at the top of the home screen. */
fun greetingFor(hourOfDay: Int): String = when (hourOfDay) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

fun currentGreeting(calendar: Calendar = Calendar.getInstance()): String =
    greetingFor(calendar.get(Calendar.HOUR_OF_DAY))
