package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {

    private val isoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }

    private val spanishDisplayFormat: SimpleDateFormat
        get() = SimpleDateFormat("EEE d MMM", Locale.forLanguageTag("es-ES"))

    private val spanishMonthFormat: SimpleDateFormat
        get() = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-ES"))

    fun todayIso(): String = isoFormat.format(Calendar.getInstance().time)

    fun millisToIso(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return isoFormat.format(cal.time)
    }

    fun isoToMillis(isoDate: String): Long {
        return try {
            isoFormat.parse(isoDate)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun addDays(isoDate: String, deltaDays: Int): String {
        val cal = Calendar.getInstance()
        cal.time = isoFormat.parse(isoDate) ?: Calendar.getInstance().time
        cal.add(Calendar.DAY_OF_MONTH, deltaDays)
        return isoFormat.format(cal.time)
    }

    fun mondayOfWeek(isoDate: String): String {
        val cal = Calendar.getInstance()
        cal.time = isoFormat.parse(isoDate) ?: Calendar.getInstance().time
        val dow = cal.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2, ..., Sat=7
        val diffFromMonday = (dow + 5) % 7
        cal.add(Calendar.DAY_OF_MONTH, -diffFromMonday)
        return isoFormat.format(cal.time)
    }

    fun firstDayOfMonth(isoDate: String): String {
        return if (isoDate.length >= 7) "${isoDate.substring(0, 7)}-01" else todayIso().substring(0, 7) + "-01"
    }

    fun daysInMonth(isoDate: String): Int {
        val cal = Calendar.getInstance()
        cal.time = isoFormat.parse(isoDate) ?: Calendar.getInstance().time
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun lastDayOfMonth(isoDate: String): String {
        val days = daysInMonth(isoDate)
        val prefix = if (isoDate.length >= 7) isoDate.substring(0, 7) else todayIso().substring(0, 7)
        return "$prefix-${days.toString().padStart(2, '0')}"
    }

    fun formatSpanishDayLabel(isoDate: String): String {
        val today = todayIso()
        val yesterday = addDays(today, -1)
        return when (isoDate) {
            today -> "Hoy"
            yesterday -> "Ayer"
            else -> try {
                val date = isoFormat.parse(isoDate) ?: return isoDate
                spanishDisplayFormat.format(date).replaceFirstChar { it.uppercase() }
            } catch (_: Exception) {
                isoDate
            }
        }
    }

    fun formatSpanishMonthTitle(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: return isoDate.take(7)
            spanishMonthFormat.format(date).replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            isoDate.take(7)
        }
    }

    fun weekDayInitial(indexFromMonday: Int): String {
        val initials = listOf("L", "M", "X", "J", "V", "S", "D")
        return initials.getOrElse(indexFromMonday) { "" }
    }
}
