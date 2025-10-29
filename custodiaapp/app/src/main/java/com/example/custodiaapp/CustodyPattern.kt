package com.example.custodiaapp

import java.time.LocalDate
import java.time.DayOfWeek

// Clase base para patrones de custodia
sealed class CustodyPattern {
    abstract val name: String
    abstract val description: String
    abstract fun getParentForDate(date: LocalDate, startDate: LocalDate): Int
}

// Patrón de semanas alternas
data class AlternateWeeks(
    override val name: String = "Semanas Alternas",
    override val description: String = "Cada padre tiene al niño semanas completas alternadas",
    val startWithParent: Int = 1  // ¿Quién comienza? 1 o 2
) : CustodyPattern() {
    override fun getParentForDate(date: LocalDate, startDate: LocalDate): Int {
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
        val weekNumber = (daysSinceStart / 7).toInt()
        return if (weekNumber % 2 == 0) startWithParent else if (startWithParent == 1) 2 else 1
    }
}

// Patrón de días alternos
data class AlternateDays(
    override val name: String = "Días Alternos",
    override val description: String = "Los padres se alternan cada día",
    val startWithParent: Int = 1
) : CustodyPattern() {
    override fun getParentForDate(date: LocalDate, startDate: LocalDate): Int {
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
        return if (daysSinceStart.toInt() % 2 == 0) startWithParent else if (startWithParent == 1) 2 else 1
    }
}

// Patrón de entre semana/fines de semana
data class WeekdaysWeekends(
    override val name: String = "Entre Semana / Fines de Semana",
    override val description: String = "Un padre tiene entre semana, otro los fines de semana",
    val weekdaysParent: Int = 1,  // Quién tiene lunes-viernes
    val weekendsParent: Int = 2   // Quién tiene sábado-domingo
) : CustodyPattern() {
    override fun getParentForDate(date: LocalDate, startDate: LocalDate): Int {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> weekendsParent
            else -> weekdaysParent
        }
    }
}

// Patrón personalizado
data class CustomPattern(
    override val name: String = "Patrón Personalizado",
    override val description: String = "Patrón definido manualmente",
    val customDays: Map<LocalDate, Int> = emptyMap()
) : CustodyPattern() {
    override fun getParentForDate(date: LocalDate, startDate: LocalDate): Int {
        return customDays[date] ?: 1  // Por defecto padre 1 si no está definido
    }
}

// Datos de un día específico
/*data class CustodyDay(
    val date: LocalDate,
    val parent: Int,      // 1 o 2
    val notes: String? = null
)

// Horario completo de custodia
data class CustodySchedule(
    val startDate: LocalDate,
    val pattern: CustodyPattern,
    val parent1Name: String,
    val parent2Name: String,
    val days: MutableMap<LocalDate, CustodyDay> = mutableMapOf()
)
*/