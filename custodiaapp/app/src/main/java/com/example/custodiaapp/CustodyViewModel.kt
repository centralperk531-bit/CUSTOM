package com.example.custodiaapp

import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.YearMonth

class CustodyViewModel : ViewModel() {
    // Nombres de los custodios
    var parent1Name: String = "Custodio 1"
    var parent2Name: String = "Custodio 2"

    // Patrón de custodia
    var custodyPattern: CustodyPattern = AlternateWeeks(startWithParent = 1)

    // MODIFICADO: Fecha de inicio del patrón (por defecto hoy)
    var startDate: LocalDate = LocalDate.now()
    var changeDayOfWeek: Int = 1 // Lunes = 1, Domingo = 7

    // Mes actual para el calendario
    var currentYearMonth: YearMonth = YearMonth.now()

    // Alternancia por años
    var evenYearStartsWith: Int = 1
    var oddYearStartsWith: Int = 2

    // Configuración de verano
    var summerFirstParent: ParentType = ParentType.PARENT1
    var summerDivision: VacationDivision = VacationDivision.HALF
    var summerYearRule: YearRule = YearRule.EVEN

    // Configuración de Navidad - 2 PERÍODOS
    var christmasPeriod1Start: LocalDate = LocalDate.of(2024, 12, 23)
    var christmasPeriod1End: LocalDate = LocalDate.of(2024, 12, 30)
    var christmasPeriod1FirstParent: ParentType = ParentType.PARENT1
    var christmasPeriod1YearRule: YearRule = YearRule.EVEN

    var christmasPeriod2Start: LocalDate = LocalDate.of(2024, 12, 31)
    var christmasPeriod2End: LocalDate = LocalDate.of(2025, 1, 8)
    var christmasPeriod2FirstParent: ParentType = ParentType.PARENT2
    var christmasPeriod2YearRule: YearRule = YearRule.EVEN

    // Configuración antigua (mantener por compatibilidad)
    @Deprecated("Usar christmasPeriod1 y christmasPeriod2")
    var christmasFirstParent: ParentType = ParentType.PARENT1
    @Deprecated("Usar christmasPeriod1 y christmasPeriod2")
    var christmasDivision: VacationDivision = VacationDivision.HALF
    @Deprecated("Usar christmasPeriod1 y christmasPeriod2")
    var christmasYearRule: YearRule = YearRule.EVEN
    @Deprecated("Usar christmasPeriod1Start")
    var christmasStart: LocalDate = LocalDate.of(2024, 12, 23)
    @Deprecated("Usar christmasPeriod2End")
    var christmasEnd: LocalDate = LocalDate.of(2025, 1, 8)

    // Configuración de Semana Santa
    var easterFirstParent: ParentType = ParentType.PARENT2
    var easterDivision: VacationDivision = VacationDivision.HALF
    var easterYearRule: YearRule = YearRule.ODD
    var easterStart: LocalDate = LocalDate.of(2024, 3, 28)
    var easterEnd: LocalDate = LocalDate.of(2024, 4, 1)
    var easterDisabled: Boolean = false

    // Períodos sin custodia
    val noCustodyPeriods: MutableList<NoCustodyPeriod> = mutableListOf()

    // Listas de eventos especiales
    val specialDates: MutableList<SpecialDate> = mutableListOf()
    val summerEvents: MutableList<SummerEvent> = mutableListOf()

    // Funciones de navegación
    fun nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1)
    }

    fun previousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1)
    }

    // Función para obtener información de un día específico
    fun getDayInfo(date: LocalDate): CustodyDay {
        val parentInt = custodyPattern.getParentForDate(date, startDate)
        val parent = when (parentInt) {
            0 -> ParentType.PARENT1
            1 -> ParentType.PARENT2
            else -> ParentType.PARENT1
        }
        return CustodyDay(date = date, parent = parent)
    }
}