package com.ismaelcr.custodiaapp

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import java.time.YearMonth

class CalendarRenderer(
    private val viewModel: CustodyViewModel
) {

    companion object {
        private val PARENT1_COLOR = Color.parseColor("#FFE780") // Amarillo pastel
        private val PARENT2_COLOR = Color.parseColor("#95A9FF") // Azul pastel
        private val NO_CUSTODY_COLOR = Color.parseColor("#D0D0D0") // Gris claro
        private val SELECTION_COLOR = Color.parseColor("#7EDC82") // Verde para selección
        private val VISIT_COLOR = Color.parseColor("#F8BBD0") // Rosa Palo visita ← NUEVO

        private const val LUMINANCE_THRESHOLD = 0.50
        private const val CELL_WIDTH = 4
    }

    var rangeSelectionManager: RangeSelectionManager? = null

    private fun isColorDark(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return luminance < LUMINANCE_THRESHOLD
    }

    fun renderMonthWithCustody(
        yearMonth: YearMonth,
        custodyCalculator: MainActivity.CustodyCalculator,
        parent1Name: String,
        parent2Name: String
    ): CharSequence {
        val builder = SpannableStringBuilder()
        renderHeader(builder)
        renderDays(builder, yearMonth, custodyCalculator)
        renderLegend(builder, parent1Name, parent2Name)
        return builder
    }

    private fun renderHeader(builder: SpannableStringBuilder) {
        val daysHeader = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do")
        val headerStart = builder.length
        for (day in daysHeader) {
            builder.append(" $day ")
        }
        builder.append("\n\n")
        builder.setSpan(
            StyleSpan(Typeface.BOLD),
            headerStart,
            builder.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun renderDays(
        builder: SpannableStringBuilder,
        yearMonth: YearMonth,
        custodyCalculator: MainActivity.CustodyCalculator
    ) {
        val firstDay = yearMonth.atDay(1)
        val lastDay = yearMonth.atEndOfMonth()
        val firstDayOfWeek = firstDay.dayOfWeek.value

        repeat(firstDayOfWeek - 1) {
            builder.append("    ")
        }

        var currentDayOfWeek = firstDayOfWeek

        for (day in 1..lastDay.dayOfMonth) {
            val date = yearMonth.atDay(day)
            val custody = custodyCalculator.getCustodyForDate(date)

            // ─── Color base ───────────────────────────────────────
            val baseBgColor = when (custody.parent) {
                ParentType.PARENT1 -> PARENT1_COLOR
                ParentType.PARENT2 -> PARENT2_COLOR
                ParentType.NONE -> NO_CUSTODY_COLOR
            }

            // ─── Visita (solo si NO es período especial) ──────────
            // La visita solo aplica si el día NO está en verano/Navidad/SS
            // (esa lógica se añadirá en el CalcuCalculator; aquí confiamos en getVisitParent)
            val isSpecialPeriod = custody.parent == ParentType.NONE
                    || viewModel.summerEvents.any { date in it.startDate..it.endDate }
                    || viewModel.noCustodyPeriods.any { date in it.startDate..it.endDate }
            val visitParent = if (!isSpecialPeriod) viewModel.getVisitParent(date) else null

            // La visita solo aplica si el padre visitante es DISTINTO al custodio del día
            val isVisitFromSpecial = viewModel.specialDates.any {
                it.date == date && it.description == "Visita"
            }
            val isVisitDay = isVisitFromSpecial || (visitParent != null && visitParent != custody.parent)

            // ─── Color final ──────────────────────────────────────
            val bgColor = when {
                rangeSelectionManager?.isDateInRange(date) == true -> SELECTION_COLOR
                isVisitDay -> VISIT_COLOR  // ← Rosa Palo si es visita
                else -> baseBgColor
            }
            // ─────────────────────────────────────────────────────

            val number = String.format("%2d", day)
            val chunk = " $number "

            val start = builder.length
            builder.append(chunk)
            val end = builder.length

            builder.setSpan(
                BackgroundColorSpan(bgColor),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val textColor = if (isColorDark(bgColor)) Color.WHITE else Color.BLACK
            builder.setSpan(
                ForegroundColorSpan(textColor),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            if (currentDayOfWeek == 7) {
                builder.append("\n")
                currentDayOfWeek = 1
            } else {
                currentDayOfWeek++
            }
        }

        if (currentDayOfWeek != 1) {
            builder.append("\n")
        }
    }

    private fun renderLegend(
        builder: SpannableStringBuilder,
        parent1Name: String,
        parent2Name: String
    ) {
        builder.append("\n")

        // Padre 1
        val l1 = builder.length
        builder.append("■ = $parent1Name\n")
        builder.setSpan(ForegroundColorSpan(PARENT1_COLOR), l1, l1 + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Padre 2
        val l2 = builder.length
        builder.append("■ = $parent2Name\n")
        builder.setSpan(ForegroundColorSpan(PARENT2_COLOR), l2, l2 + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Sin custodia
        val l3 = builder.length
        builder.append("■ = Sin custodia\n")
        builder.setSpan(ForegroundColorSpan(NO_CUSTODY_COLOR), l3, l3 + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Visita ← NUEVO (solo "Visita", sin nombre de padre)
        val l4 = builder.length
        builder.append("■ = Visita")
        builder.setSpan(ForegroundColorSpan(VISIT_COLOR), l4, l4 + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
