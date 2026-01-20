package com.example.custodiaapp

import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

// Clase base para patrones de custodia
sealed class CustodyPattern {
    abstract val name: String
    abstract val description: String
    abstract fun getParentForDate(date: LocalDate, startDate: LocalDate, changeDayOfWeek: Int = 1): Int
}

// Patrón de semanas alternas
data class AlternateWeeks(
    override val name: String = "Semanas Alternas",
    override val description: String = "Cada padre tiene al niño semanas completas alternadas",
    val startWithParent: Int = 1
) : CustodyPattern() {
    override fun getParentForDate(date: LocalDate, startDate: LocalDate, changeDayOfWeek: Int): Int {
        val targetDay = when (changeDayOfWeek) {
            1 -> DayOfWeek.MONDAY
            2 -> DayOfWeek.TUESDAY
            3 -> DayOfWeek.WEDNESDAY
            4 -> DayOfWeek.THURSDAY
            5 -> DayOfWeek.FRIDAY
            6 -> DayOfWeek.SATURDAY
            7 -> DayOfWeek.SUNDAY
            else -> DayOfWeek.MONDAY
        }

        // Ajustar startDate al día de cambio de su semana (hacia atrás)
        val adjustedStartDate = startDate.with(TemporalAdjusters.previousOrSame(targetDay))

        // Ajustar la fecha consultada al día de cambio de SU semana (hacia atrás)
        val adjustedDate = date.with(TemporalAdjusters.previousOrSame(targetDay))

        // Calcular semanas completas entre las fechas ajustadas
        val weeksSinceStart = ChronoUnit.WEEKS.between(adjustedStartDate, adjustedDate)

        // DEBUG
        android.util.Log.d("CUSTODY_DEBUG",
            "Fecha: $date | AdjDate: $adjustedDate | Start: $startDate | AdjStart: $adjustedStartDate | " +
                    "Semanas: $weeksSinceStart | StartWith: $startWithParent"
        )

        // Manejar semanas negativas (fechas anteriores al inicio)
        val weekOffset = if (weeksSinceStart < 0) {
            // Para fechas anteriores, necesitamos calcular correctamente la alternancia
            Math.floorMod(weeksSinceStart, 2)
        } else {
            (weeksSinceStart % 2).toInt()
        }

        return if (weekOffset == 0) {
            startWithParent
        } else {
            if (startWithParent == 1) 2 else 1
        }
    }
}

// Patrón de días alternos
data class AlternateDays(
    override val name: String = "Días Alternos",
    override val description: String = "Los padres se alternan cada día",
    val startWithParent: Int = 1
) : CustodyPattern() {
    override fun getParentForDate(date: LocalDate, startDate: LocalDate, changeDayOfWeek: Int): Int {
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)

        // Usar Math.floorMod para manejar correctamente días negativos
        val dayOffset = Math.floorMod(daysSinceStart, 2)

        return if (dayOffset == 0) startWithParent else if (startWithParent == 1) 2 else 1
    }
}

// Patrón de entre semana/fines de semana
data class WeekdaysWeekends(
    override val name: String = "Entre Semana / Fines de Semana",
    override val description: String = "Un padre tiene entre semana, otro los fines de semana",
    val weekdaysParent: Int = 1,
    val weekendsParent: Int = 2
) : CustodyPattern() {
    override fun getParentForDate(date: LocalDate, startDate: LocalDate, changeDayOfWeek: Int): Int {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> weekendsParent
            else -> weekdaysParent
        }
    }
}

// Patrón de días personalizados
// Patrón de días personalizados
data class CustomDaysPattern(
    override val name: String = "Patrón Personalizado",
    override val description: String = "Alterna cada X días según lo configurado",
    val daysForParent1: Int = 7,  // Cuántos días tiene Padre 1
    val daysForParent2: Int = 7,  // Cuántos días tiene Padre 2
    val startWithParent: Int = 1
) : CustodyPattern() {
    override fun getParentForDate(date: LocalDate, startDate: LocalDate, changeDayOfWeek: Int): Int {
        // Calcular el ciclo total (días de padre 1 + días de padre 2)
        val totalCycleDays = daysForParent1 + daysForParent2

        // Calcular días desde el inicio (puede ser negativo si la fecha es anterior)
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)

        // Ajustar para fechas anteriores: usar módulo matemático correcto
        val positionInCycle = if (daysSinceStart >= 0) {
            (daysSinceStart % totalCycleDays).toInt()
        } else {
            // Para fechas anteriores, calcular correctamente la posición en el ciclo
            val remainder = (daysSinceStart % totalCycleDays).toInt()
            if (remainder < 0) remainder + totalCycleDays else remainder
        }

        // Debug log
        android.util.Log.d("CUSTODY_DEBUG_CUSTOM",
            "Fecha: $date | Start: $startDate | Días: $daysSinceStart | " +
                    "Ciclo: $totalCycleDays | Pos: $positionInCycle | StartWith: $startWithParent"
        )

        // Determinar qué padre le toca
        return if (startWithParent == 1) {
            if (positionInCycle < daysForParent1) 1 else 2
        } else {
            if (positionInCycle < daysForParent2) 2 else 1
        }
    }
}