package com.example.custodiaapp

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

    // Companion object para colores constantes
    companion object {
        // Colores pastel (suaves, estables)
        private val PARENT1_COLOR = Color.parseColor("#FFE780") // Amarillo pastel
        private val PARENT2_COLOR = Color.parseColor("#95A9FF") // Azul pastel
        private val NO_CUSTODY_COLOR = Color.parseColor("#D0D0D0") // Gris claro
        private val SELECTION_COLOR = Color.parseColor("#7EDC82") // Verde pastel para selección

        private const val LUMINANCE_THRESHOLD = 0.50
        private const val CELL_WIDTH = 4 // Cada celda ocupa 4 caracteres
    }

    var rangeSelectionManager: RangeSelectionManager? = null

    // Detectar si un color es oscuro (para texto blanco/negro)
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

        // Renderizar cabecera
        renderHeader(builder)

        // Renderizar días del mes
        renderDays(builder, yearMonth, custodyCalculator)

        // Renderizar leyenda
        renderLegend(builder, parent1Name, parent2Name)

        return builder
    }

    private fun renderHeader(builder: SpannableStringBuilder) {
        val daysHeader = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do")
        val headerStart = builder.length

        for (day in daysHeader) {
            // cada columna ocupa 4 caracteres (" sp + XX + sp")
            val centered = day.padStart((day.length + CELL_WIDTH) / 2).padEnd(CELL_WIDTH)
            builder.append(centered)
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

        // Alinear inicio del mes (cada "columna" = 4 espacios)
        repeat(firstDayOfWeek - 1) {
            builder.append("    ")
        }

        var currentDayOfWeek = firstDayOfWeek

        for (day in 1..lastDay.dayOfMonth) {
            val date = yearMonth.atDay(day)
            val custody = custodyCalculator.getCustodyForDate(date)

            // Color base según custodio (pastel)
            val baseBgColor = when (custody.parent) {
                ParentType.PARENT1 -> PARENT1_COLOR
                ParentType.PARENT2 -> PARENT2_COLOR
                ParentType.NONE -> NO_CUSTODY_COLOR
            }

            // Si está en selección → usar color de selección (sobrescribe)
            val bgColor = if (rangeSelectionManager?.isDateInRange(date) == true) {
                SELECTION_COLOR
            } else {
                baseBgColor
            }

            // Texto del día con un espacio a cada lado (margen visual)
            val number = String.format("%2d", day)
            val chunk = " $number " // 4 chars: sp + 2 dígitos + sp

            val start = builder.length
            builder.append(chunk)
            val end = builder.length

            // Aplicar estilos
            builder.setSpan(
                BackgroundColorSpan(bgColor),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val textColor = if (isColorDark(bgColor)) Color.WHITE else Color.BLACK
            builder.setSpan(
                ForegroundColorSpan(textColor),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Manejo de saltos de línea
            if (currentDayOfWeek == 7) {
                builder.append("\n")
                currentDayOfWeek = 1
            } else {
                currentDayOfWeek++
            }
        }

        // Asegurar nueva línea al final si no terminó en domingo
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

        // Leyenda padre 1
        val l1 = builder.length
        builder.append("■ = $parent1Name\n")
        builder.setSpan(
            ForegroundColorSpan(PARENT1_COLOR),
            l1,
            l1 + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Leyenda padre 2
        val l2 = builder.length
        builder.append("■ = $parent2Name\n")
        builder.setSpan(
            ForegroundColorSpan(PARENT2_COLOR),
            l2,
            l2 + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Leyenda sin custodia
        val l3 = builder.length
        builder.append("■ = Sin custodia")
        builder.setSpan(
            ForegroundColorSpan(NO_CUSTODY_COLOR),
            l3,
            l3 + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}
