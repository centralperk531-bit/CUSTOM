package com.ismaelcr.custodiaapp

import java.time.LocalDate

// ===== Enums =====

enum class ParentType {
    PARENT1, PARENT2, NONE;

    fun toggle(): ParentType {
        return when (this) {
            PARENT1 -> PARENT2
            PARENT2 -> PARENT1
            NONE -> NONE
        }
    }

    companion object {
        fun fromInt(value: Int): ParentType {
            return when (value) {
                1 -> PARENT1
                2 -> PARENT2
                else -> NONE
            }
        }
    }
}

enum class VacationDivision {
    HALF,           // Mitad y mitad
    FULL,           // Todo el periodo
    ALTERNATE_DAYS, // Alternar cada 7 días (semanas completas)
    ALTERNATE_WEEKS,// Alternar por semanas naturales (lunes a domingo)
    BIWEEKLY;       // Quincenas (1-15 y 16-31)

    fun getDescription(): String {
        return when (this) {
            HALF -> "Dividir por mitad"
            FULL -> "Período completo"
            ALTERNATE_DAYS -> "Alternar cada 7 días"
            ALTERNATE_WEEKS -> "Alternar por semanas"
            BIWEEKLY -> "Por quincenas"
        }
    }
}

enum class YearRule {
    EVEN,   // Años pares
    ODD,    // Años impares
    ALWAYS; // Siempre aplica

    fun applies(year: Int): Boolean {
        return when (this) {
            EVEN -> year % 2 == 0
            ODD -> year % 2 != 0
            ALWAYS -> true
        }
    }
}

// ===== Data classes =====

/**
 * Representa una fecha especial con custodia asignada a un padre específico
 */
data class SpecialDate(
    val date: LocalDate,
    val parent: ParentType,
    val description: String = ""
) {
    init {
        require(parent != ParentType.NONE) {
            "Una fecha especial debe tener un padre asignado (no puede ser NONE)"
        }
    }
}

/**
 * Representa un evento de verano con un rango de fechas
 */
data class SummerEvent(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val parent: ParentType,
    val description: String = ""
) {
    init {
        require(!endDate.isBefore(startDate)) {
            "La fecha de fin ($endDate) no puede ser anterior a la fecha de inicio ($startDate)"
        }
        require(parent != ParentType.NONE) {
            "Un evento de verano debe tener un padre asignado (no puede ser NONE)"
        }
    }

    /**
     * Verifica si una fecha está dentro del rango del evento
     */
    fun contains(date: LocalDate): Boolean {
        return !date.isBefore(startDate) && !date.isAfter(endDate)
    }

    /**
     * Calcula la duración del evento en días
     */
    fun getDurationInDays(): Long {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
    }
}

/**
 * Representa un período sin custodia (vacaciones escolares, etc.)
 */
data class NoCustodyPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val description: String = ""
) {
    init {
        require(!endDate.isBefore(startDate)) {
            "La fecha de fin ($endDate) no puede ser anterior a la fecha de inicio ($startDate)"
        }
    }

    /**
     * Verifica si una fecha está dentro del período sin custodia
     */
    fun contains(date: LocalDate): Boolean {
        return !date.isBefore(startDate) && !date.isAfter(endDate)
    }

    /**
     * Verifica si este período se solapa con otro
     */
    fun overlaps(other: NoCustodyPeriod): Boolean {
        return !endDate.isBefore(other.startDate) && !startDate.isAfter(other.endDate)
    }
}

/**
 * Representa un día específico con información de custodia
 */
data class CustodyDay(
    val date: LocalDate,
    val parent: ParentType,
    val notes: String? = null
) {
    /**
     * Verifica si este día tiene custodia asignada
     */
    fun hasCustody(): Boolean = parent != ParentType.NONE
}

/**
 * Representa un calendario completo de custodia
 */
data class CustodySchedule(
    val startDate: LocalDate,
    val pattern: CustodyPattern,
    val parent1Name: String,
    val parent2Name: String,
    val days: MutableMap<LocalDate, CustodyDay> = mutableMapOf()
) {
    init {
        require(parent1Name.isNotBlank()) { "El nombre del padre 1 no puede estar vacío" }
        require(parent2Name.isNotBlank()) { "El nombre del padre 2 no puede estar vacío" }
    }

    /**
     * Obtiene el día de custodia para una fecha específica
     */
    fun getDayForDate(date: LocalDate): CustodyDay? {
        return days[date]
    }

    /**
     * Añade un día de custodia al calendario
     */
    fun addDay(custodyDay: CustodyDay) {
        days[custodyDay.date] = custodyDay
    }

    /**
     * Obtiene el nombre del padre según el tipo
     */
    fun getParentName(parentType: ParentType): String {
        return when (parentType) {
            ParentType.PARENT1 -> parent1Name
            ParentType.PARENT2 -> parent2Name
            ParentType.NONE -> "Sin custodia"
        }
    }
}
