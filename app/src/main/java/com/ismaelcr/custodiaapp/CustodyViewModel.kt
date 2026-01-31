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

    // NUEVA LÓGICA: Configuración de fecha de inicio del patrón
    var startDate: LocalDate = LocalDate.now()
    var patternStartsWithParent: Int = 1  // 1 = Parent1, 2 = Parent2
    var patternApplicationMode: String = "FORWARD"  // "FORWARD" o "FROM_DATE"
    var changeDayOfWeek: Int = 1 // Lunes = 1, Domingo = 7

    // Mes actual para el calendario
    var currentYearMonth: YearMonth = YearMonth.now()

    // Alternancia por años (SOLO afecta al verano y leyendas)
    var evenYearStartsWith: Int = 1  // 1 = Parent1, 2 = Parent2
    var oddYearStartsWith: Int = 2   // 1 = Parent1, 2 = Parent2

    // Configuración de verano (solo división, el resto se determina por años pares/impares)
    var summerDivision: VacationDivision = VacationDivision.HALF

    // Configuración de Navidad - 2 PERÍODOS (deprecated, mantenido solo por compatibilidad)
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

    // Configuración antigua (mantener por compatibilidad)
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

    // Configuración de Semana Santa (deprecated, debe configurarse manualmente)
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
    var easterDisabled: Boolean = true  // Deshabilitada por defecto

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

    // Función para obtener información de un día específico
    fun getDayInfo(date: LocalDate): CustodyDay {
        // Buscar si hay cambio de patrón para esta fecha
        val cambioAplicable = patternChanges
            .filter { it.startDate <= date }
            .maxByOrNull { it.startDate }

        // Usar el patrón del cambio si existe, si no usar el base
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
}