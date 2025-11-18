package com.example.custodiaapp

import java.time.LocalDate

class RangeSelectionManager {
    var startDate: LocalDate? = null
        private set
    var endDate: LocalDate? = null
        private set

    var isSelecting: Boolean = false
        private set

    fun startSelection(date: LocalDate) {
        startDate = date
        endDate = null
        isSelecting = true
    }

    fun updateEndDate(date: LocalDate) {
        if (startDate != null) {
            endDate = date
        }
    }

    fun completeSelection(): Pair<LocalDate, LocalDate>? {
        return if (startDate != null && endDate != null) {
            val start = startDate!!
            val end = endDate!!
            // Asegurar que start <= end
            if (start.isAfter(end)) {
                end to start
            } else {
                start to end
            }
        } else {
            null
        }
    }

    fun clearSelection() {
        startDate = null
        endDate = null
        isSelecting = false
    }

    fun isDateInRange(date: LocalDate): Boolean {
        val start = startDate ?: return false
        val end = endDate ?: return date == start

        val actualStart = if (start.isAfter(end)) end else start
        val actualEnd = if (start.isAfter(end)) start else end

        return !date.isBefore(actualStart) && !date.isAfter(actualEnd)
    }
}