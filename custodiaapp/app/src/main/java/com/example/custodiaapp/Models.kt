package com.example.custodiaapp

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
}

enum class VacationDivision {
    HALF,            // Mitad y mitad
    FULL,            // Todo el periodo
    ALTERNATE_DAYS,  // Alternar cada 7 días (semanas completas)
    ALTERNATE_WEEKS, // Alternar por semanas naturales (lunes a domingo)
    BIWEEKLY         // Quincenas (1-15 y 16-31)
}

enum class YearRule {
    EVEN,   // Años pares
    ODD,    // Años impares
    ALWAYS  // Siempre aplica
}

// ===== Data classes =====
data class SpecialDate(
    val date: LocalDate,
    val parent: ParentType,
    val description: String
)

data class SummerEvent(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val parent: ParentType,
    val description: String
)

data class NoCustodyPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val description: String
)

data class CustodyDay(
    val date: LocalDate,
    val parent: ParentType,
    val notes: String? = null
)

data class CustodySchedule(
    val startDate: LocalDate,
    val pattern: CustodyPattern,
    val parent1Name: String,
    val parent2Name: String,
    val days: MutableMap<LocalDate, CustodyDay> = mutableMapOf()
)