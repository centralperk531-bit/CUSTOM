package com.example.custodiaapp

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Gestor para la selección de rangos de fechas en el calendario
 * Permite seleccionar un rango con fecha de inicio y fin
 */
class RangeSelectionManager {

    var startDate: LocalDate? = null
        private set

    var endDate: LocalDate? = null
        private set

    var isSelecting: Boolean = false
        private set

    companion object {
        private const val MAX_SELECTION_DAYS = 365 // Limitar selección a 1 año máximo
    }

    /**
     * Inicia una nueva selección con la fecha proporcionada
     */
    fun startSelection(date: LocalDate) {
        startDate = date
        endDate = null
        isSelecting = true
    }

    /**
     * Actualiza la fecha de fin de la selección
     * Valida que el rango no exceda el máximo permitido
     */
    fun updateEndDate(date: LocalDate): Boolean {
        if (startDate == null) {
            return false
        }

        // Validar que el rango no sea excesivo
        val daysBetween = Math.abs(ChronoUnit.DAYS.between(startDate, date))
        if (daysBetween > MAX_SELECTION_DAYS) {
            return false
        }

        endDate = date
        return true
    }

    /**
     * Completa la selección y retorna el rango ordenado (start <= end)
     * Retorna null si la selección no es válida
     */
    fun completeSelection(): Pair<LocalDate, LocalDate>? {
        val start = startDate
        val end = endDate

        return if (start != null && end != null) {
            isSelecting = false
            // Asegurar que start <= end
            if (start.isAfter(end)) {
                end to start
            } else {
                start to end
            }
        } else if (start != null) {
            // Si solo hay fecha de inicio, considerar un rango de un día
            isSelecting = false
            start to start
        } else {
            null
        }
    }

    /**
     * Limpia la selección actual
     */
    fun clearSelection() {
        startDate = null
        endDate = null
        isSelecting = false
    }

    /**
     * Verifica si una fecha está dentro del rango seleccionado
     */
    fun isDateInRange(date: LocalDate): Boolean {
        val start = startDate ?: return false
        val end = endDate ?: return date == start

        val actualStart = if (start.isAfter(end)) end else start
        val actualEnd = if (start.isAfter(end)) start else end

        return !date.isBefore(actualStart) && !date.isAfter(actualEnd)
    }

    /**
     * Obtiene el rango actual ordenado (start, end) o null si no hay selección
     */
    fun getCurrentRange(): Pair<LocalDate, LocalDate>? {
        val start = startDate ?: return null
        val end = endDate ?: return null

        return if (start.isAfter(end)) {
            end to start
        } else {
            start to end
        }
    }

    /**
     * Calcula el número de días en el rango actual
     */
    fun getSelectionDays(): Int {
        val range = getCurrentRange() ?: return if (startDate != null) 1 else 0
        return ChronoUnit.DAYS.between(range.first, range.second).toInt() + 1
    }

    /**
     * Verifica si la selección actual es válida
     */
    fun isValidSelection(): Boolean {
        return startDate != null && (endDate != null || !isSelecting)
    }

    /**
     * Obtiene todas las fechas incluidas en el rango actual
     */
    fun getAllDatesInRange(): List<LocalDate> {
        val range = getCurrentRange() ?: return emptyList()
        val dates = mutableListOf<LocalDate>()
        var current = range.first

        while (!current.isAfter(range.second)) {
            dates.add(current)
            current = current.plusDays(1)
        }

        return dates
    }
}
