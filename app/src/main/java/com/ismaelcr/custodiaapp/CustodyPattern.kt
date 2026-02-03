package com.ismaelcr.custodiaapp

import android.util.Log
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class WeekendMode {
    FIXED,           // Fines de semana fijos (siempre el mismo padre)
    ALTERNATE,       // Fines de semana alternos (se alternan cada semana)
    ONE_OF_THREE,    // 1 de cada 3 fines de semana
    ONE_OF_FOUR      // 1 de cada 4 fines de semana
}

// Clase base para patrones de custodia
sealed class CustodyPattern {
    abstract val name: String
    abstract val description: String
    abstract fun getParentForDate(date: LocalDate, startDate: LocalDate, changeDayOfWeek: Int = 1): Int

    companion object {
        const val TAG = "CustodyPattern"
        const val DEBUG = false // Cambiar a false en producción

        fun getDayOfWeekFromInt(dayOfWeek: Int): DayOfWeek {
            return when (dayOfWeek) {
                1 -> DayOfWeek.MONDAY
                2 -> DayOfWeek.TUESDAY
                3 -> DayOfWeek.WEDNESDAY
                4 -> DayOfWeek.THURSDAY
                5 -> DayOfWeek.FRIDAY
                6 -> DayOfWeek.SATURDAY
                7 -> DayOfWeek.SUNDAY
                else -> DayOfWeek.MONDAY
            }
        }
    }
}

// Patrón de semanas alternas
data class AlternateWeeks(
    override val name: String = "Semanas Alternas",
    override val description: String = "Cada padre tiene al niño semanas completas alternadas",
    val startWithParent: Int = 1
) : CustodyPattern() {

    override fun getParentForDate(date: LocalDate, startDate: LocalDate, changeDayOfWeek: Int): Int {
        val targetDay = getDayOfWeekFromInt(changeDayOfWeek)

        // Ajustar startDate al día de cambio de su semana (hacia atrás)
        val adjustedStartDate = startDate.with(TemporalAdjusters.previousOrSame(targetDay))

        // Ajustar la fecha consultada al día de cambio de SU semana (hacia atrás)
        val adjustedDate = date.with(TemporalAdjusters.previousOrSame(targetDay))

        // Calcular semanas completas entre las fechas ajustadas
        val weeksSinceStart = ChronoUnit.WEEKS.between(adjustedStartDate, adjustedDate)

        if (DEBUG) {
            Log.d(
                TAG, "AlternateWeeks - Fecha: $date | AdjDate: $adjustedDate | " +
                    "Start: $startDate | AdjStart: $adjustedStartDate | " +
                    "Semanas: $weeksSinceStart | StartWith: $startWithParent")
        }

        // Usar Math.floorMod para manejar correctamente semanas negativas
        val weekOffset = Math.floorMod(weeksSinceStart, 2L).toInt()

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

        if (DEBUG) {
            Log.d(
                TAG, "AlternateDays - Fecha: $date | Start: $startDate | " +
                    "Días: $daysSinceStart | StartWith: $startWithParent")
        }

        // Usar Math.floorMod para manejar correctamente días negativos
        val dayOffset = Math.floorMod(daysSinceStart, 2L).toInt()

        return if (dayOffset == 0) startWithParent else if (startWithParent == 1) 2 else 1
    }
}

// Patrón de entre semana/fines de semana
data class WeekdaysWeekends(
    override val name: String = "Entre Semana / Fines de Semana",
    override val description: String = "Un padre tiene entre semana, fines de semana configurables",
    val weekdaysParent: Int = 1,
    val weekendMode: WeekendMode = WeekendMode.FIXED,
    val fixedWeekendsParent: Int = 2,
    val startWeekendWithParent: Int = 1
) : CustodyPattern() {

    override fun getParentForDate(date: LocalDate, startDate: LocalDate, changeDayOfWeek: Int): Int {
        // Si es entre semana (lunes a viernes)
        if (date.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
            return weekdaysParent
        }

        // Si es fin de semana, depende del modo
        return when (weekendMode) {
            WeekendMode.FIXED -> {
                // Siempre el mismo padre
                fixedWeekendsParent
            }

            WeekendMode.ALTERNATE -> {
                // Alternar cada semana
                val adjustedStartDate = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
                val adjustedDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
                val weeksSinceStart = ChronoUnit.WEEKS.between(adjustedStartDate, adjustedDate)

                if (DEBUG) {
                    Log.d(TAG, "WeekdaysWeekends ALTERNATE - Fecha: $date | Semanas: $weeksSinceStart")
                }

                val weekOffset = Math.floorMod(weeksSinceStart, 2L).toInt()
                if (weekOffset == 0) startWeekendWithParent else if (startWeekendWithParent == 1) 2 else 1
            }

            WeekendMode.ONE_OF_THREE -> {
                // Ciclo de 3 semanas: 2 para uno, 1 para el otro
                val adjustedStartDate = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
                val adjustedDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
                val weeksSinceStart = ChronoUnit.WEEKS.between(adjustedStartDate, adjustedDate)

                val positionInCycle = Math.floorMod(weeksSinceStart, 3L).toInt()

                if (DEBUG) {
                    Log.d(TAG, "WeekdaysWeekends ONE_OF_THREE - Fecha: $date | Semanas: $weeksSinceStart | Pos: $positionInCycle")
                }

                // Ciclo: startWith, startWith, otro
                if (positionInCycle < 2) startWeekendWithParent else if (startWeekendWithParent == 1) 2 else 1
            }

            WeekendMode.ONE_OF_FOUR -> {
                // Ciclo de 4 semanas: 3 para uno, 1 para el otro
                val adjustedStartDate = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
                val adjustedDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
                val weeksSinceStart = ChronoUnit.WEEKS.between(adjustedStartDate, adjustedDate)

                val positionInCycle = Math.floorMod(weeksSinceStart, 4L).toInt()

                if (DEBUG) {
                    Log.d(TAG, "WeekdaysWeekends ONE_OF_FOUR - Fecha: $date | Semanas: $weeksSinceStart | Pos: $positionInCycle")
                }

                // Ciclo: startWith, startWith, startWith, otro
                if (positionInCycle < 3) startWeekendWithParent else if (startWeekendWithParent == 1) 2 else 1
            }
        }
    }
}

// Patrón de días personalizados
data class CustomDaysPattern(
    override val name: String = "Patrón Personalizado",
    override val description: String = "Alterna cada X días según lo configurado",
    val daysForParent1: Int = 7, // Cuántos días tiene Padre 1
    val daysForParent2: Int = 7, // Cuántos días tiene Padre 2
    val startWithParent: Int = 1
) : CustodyPattern() {

    init {
        require(daysForParent1 > 0) { "daysForParent1 debe ser mayor que 0" }
        require(daysForParent2 > 0) { "daysForParent2 debe ser mayor que 0" }
        require(startWithParent in 1..2) { "startWithParent debe ser 1 o 2" }
    }

    override fun getParentForDate(date: LocalDate, startDate: LocalDate, changeDayOfWeek: Int): Int {
        // Calcular el ciclo total (días de padre 1 + días de padre 2)
        val totalCycleDays = daysForParent1 + daysForParent2

        // Calcular días desde el inicio (puede ser negativo si la fecha es anterior)
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)

        // Usar Math.floorMod para manejar correctamente días negativos
        val positionInCycle = Math.floorMod(daysSinceStart, totalCycleDays.toLong()).toInt()

        if (DEBUG) {
            Log.d(
                TAG, "CustomDaysPattern - Fecha: $date | Start: $startDate | " +
                    "Días: $daysSinceStart | Ciclo: $totalCycleDays | " +
                    "Pos: $positionInCycle | StartWith: $startWithParent")
        }

        // Determinar qué padre le toca
        return if (startWithParent == 1) {
            if (positionInCycle < daysForParent1) 1 else 2
        } else {
            if (positionInCycle < daysForParent2) 2 else 1
        }
    }
}
