package com.ismaelcr.custodiaapp

import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.YearMonth

class CustodyViewModel : ViewModel() {
    // Nombres de los custodios
    var parent1Name: String = "Custodio 1"
    var parent2Name: String = "Custodio 2"

    // Patrón de custodia
    var custodyPattern: CustodyPattern = AlternateWeeks(startWithParent = 1)

    // Configuración de fecha de inicio del patrón
    var startDate: LocalDate = LocalDate.now()
    var patternStartsWithParent: Int = 1
    var patternApplicationMode: String = "FORWARD"
    var changeDayOfWeek: Int = 1

    // Mes actual para el calendario
    var currentYearMonth: YearMonth = YearMonth.now()

    // Alternancia por años
    var evenYearStartsWith: Int = 1
    var oddYearStartsWith: Int = 2

    // Configuración de verano
    var summerDivision: VacationDivision = VacationDivision.HALF

    // ─── VISITAS ───────────────────────────────────────────────
    var visitDaysParent1: List<Int> = emptyList()
    var visitDaysParent2: List<Int> = emptyList()
    // ──────────────────────────────────────────────────────────

    // Configuración de Navidad (deprecated)
    @Deprecated("Navidad debe configurarse manualmente vía summerEvents")
    var christmasPeriod1Start: LocalDate = LocalDate.of(2024, 12, 23)
    @Deprecated("Navidad debe configurarse manualmente vía summerEvents")
    var christmasPeriod1End: LocalDate = LocalDate.of(2024, 12, 30)
    @Deprecated("Navidad debe configurarse manualmente vía summerEvents")
    var christmasPeriod1FirstParent: ParentType = ParentType.PARENT1
    @Deprecated("Navidad debe configurarse manualmente vía summerEvents")
    var christmasPeriod1YearRule: YearRule = YearRule.EVEN

    @Deprecated("Navidad debe configurarse manualmente vía summerEvents")
    var christmasPeriod2Start: LocalDate = LocalDate.of(2024, 12, 31)
    @Deprecated("Navidad debe configurarse manualmente vía summerEvents")
    var christmasPeriod2End: LocalDate = LocalDate.of(2025, 1, 8)
    @Deprecated("Navidad debe configurarse manualmente vía summerEvents")
    var christmasPeriod2FirstParent: ParentType = ParentType.PARENT2
    @Deprecated("Navidad debe configurarse manualmente vía summerEvents")
    var christmasPeriod2YearRule: YearRule = YearRule.EVEN

    @Deprecated("Usar summerEvents para configurar Navidad")
    var christmasFirstParent: ParentType = ParentType.PARENT1
    @Deprecated("Usar summerEvents para configurar Navidad")
    var christmasDivision: VacationDivision = VacationDivision.HALF
    @Deprecated("Usar summerEvents para configurar Navidad")
    var christmasYearRule: YearRule = YearRule.EVEN
    @Deprecated("Usar summerEvents para configurar Navidad")
    var christmasStart: LocalDate = LocalDate.of(2024, 12, 23)
    @Deprecated("Usar summerEvents para configurar Navidad")
    var christmasEnd: LocalDate = LocalDate.of(2025, 1, 8)

    // Configuración de Semana Santa (deprecated)
    @Deprecated("Semana Santa debe configurarse manualmente vía summerEvents")
    var easterFirstParent: ParentType = ParentType.PARENT2
    @Deprecated("Semana Santa debe configurarse manualmente vía summerEvents")
    var easterDivision: VacationDivision = VacationDivision.HALF
    @Deprecated("Semana Santa debe configurarse manualmente vía summerEvents")
    var easterYearRule: YearRule = YearRule.ODD
    @Deprecated("Semana Santa debe configurarse manualmente vía summerEvents")
    var easterStart: LocalDate = LocalDate.of(2024, 3, 28)
    @Deprecated("Semana Santa debe configurarse manualmente vía summerEvents")
    var easterEnd: LocalDate = LocalDate.of(2024, 4, 1)
    @Deprecated("Semana Santa debe configurarse manualmente vía summerEvents")
    var easterDisabled: Boolean = true

    // Períodos sin custodia
    val noCustodyPeriods: MutableList<NoCustodyPeriod> = mutableListOf()

    // Listas de eventos especiales
    val specialDates: MutableList<SpecialDate> = mutableListOf()
    val summerEvents: MutableList<SummerEvent> = mutableListOf()
    val patternChanges = mutableListOf<MainActivity.PatternChange>()

    // Funciones de navegación
    fun nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1)
    }

    fun previousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1)
    }

    // ─── VISITAS: helper ──────────────────────────────────────
    fun getVisitParent(date: LocalDate, custodyParent: ParentType): ParentType? {
        val summerRange = LocalDate.of(date.year, 7, 1)..LocalDate.of(date.year, 8, 31)

        val enPeriodoEspecial = date in summerRange
                || summerEvents.any { date in it.startDate..it.endDate }
                || noCustodyPeriods.any { date in it.startDate..it.endDate }
                || specialDates.any { it.date == date }

        if (enPeriodoEspecial) return null

        val dow = date.dayOfWeek.value
        return when {
            custodyParent == ParentType.PARENT2 && dow in visitDaysParent1 -> ParentType.PARENT1
            custodyParent == ParentType.PARENT1 && dow in visitDaysParent2 -> ParentType.PARENT2
            else -> null
        }
    }

    // ──────────────────────────────────────────────────────────

    fun getDayInfo(date: LocalDate): CustodyDay {
        val cambioAplicable = patternChanges
            .filter { it.startDate <= date }
            .maxByOrNull { it.startDate }

        val patronAUsar = cambioAplicable?.pattern ?: custodyPattern
        val fechaInicio = cambioAplicable?.startDate ?: startDate

        val parentInt = patronAUsar.getParentForDate(date, fechaInicio)
        val parent = when (parentInt) {
            0 -> ParentType.PARENT1
            1 -> ParentType.PARENT2
            else -> ParentType.PARENT1
        }
        return CustodyDay(date = date, parent = parent)
    }

    // ─── MENÚ CONTEXTUAL DEL CALENDARIO ──────────────────────
    fun getEventForDate(date: LocalDate): Any? {
        return specialDates.find { it.date == date }
            ?: summerEvents.find { date in it.startDate..it.endDate }
            ?: noCustodyPeriods.find { date in it.startDate..it.endDate }
    }

    fun deleteEventForDate(date: LocalDate) {
        specialDates.removeAll { it.date == date }
        summerEvents.removeAll { date in it.startDate..it.endDate }
        noCustodyPeriods.removeAll { date in it.startDate..it.endDate }
    }
    // ──────────────────────────────────────────────────────────
}
