package com.example.custodiaapp

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import java.time.LocalDate
import java.time.YearMonth

class CalendarRenderer(
    private val viewModel: CustodyViewModel
) {

    // Colores para cada custodio (puedes cambiarlos aquí)
    private val parent1Color = Color.parseColor("#FFC107") // Amarillo
    private val parent2Color = Color.parseColor("#3F51B5") // Índigo
    private val noCustodyColor = Color.parseColor("#9E9E9E") // Gris

    /**
     * Renderiza el mes con los días coloreados según el custodio.
     * Cada día se pinta con el color del custodio asignado (normal, especial, o sin custodia).
     * La leyenda al final muestra los colores y nombres de cada custodio.
     */
    fun renderMonthWithCustody(
        yearMonth: YearMonth,
        custodyCalculator: MainActivity.CustodyCalculator,
        parent1Name: String,
        parent2Name: String
    ): CharSequence {
        val builder = SpannableStringBuilder()

        // Encabezado: días de la semana en español en negrita
        val headerStart = builder.length
        builder.append("Lu  Ma  Mi  Ju  Vi  Sá  Do\n\n")
        builder.setSpan(
            StyleSpan(Typeface.BOLD),
            headerStart,
            builder.length - 2,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Primer y último día del mes
        val firstDay = yearMonth.atDay(1)
        val lastDay = yearMonth.atEndOfMonth()

        // Espacios antes del primer día (1=Lunes, 7=Domingo)
        val firstDayOfWeek = firstDay.dayOfWeek.value
        repeat(firstDayOfWeek - 1) {
            builder.append("    ")
        }

        var currentDayOfWeek = firstDayOfWeek
        for (day in 1..lastDay.dayOfMonth) {
            val date = yearMonth.atDay(day)
            val custody = custodyCalculator.getCustodyForDate(date)
            val color = when (custody.parent) {
                ParentType.PARENT1 -> parent1Color
                ParentType.PARENT2 -> parent2Color
                ParentType.NONE -> noCustodyColor
            }

            val dayStr = String.format("%2d", day)
            val dayStart = builder.length
            builder.append(dayStr)
            builder.setSpan(
                ForegroundColorSpan(color),
                dayStart,
                dayStart + dayStr.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            if (currentDayOfWeek == 7) {
                builder.append("\n")
                currentDayOfWeek = 1
            } else {
                builder.append("  ")
                currentDayOfWeek++
            }
        }

        // Asegura nueva línea al final si no terminó en domingo
        if (currentDayOfWeek != 1) {
            builder.append("\n")
        }

        // Leyenda de colores y custodios
        builder.append("\n")
        val legendStart1 = builder.length
        builder.append("■ = $parent1Name\n")
        builder.setSpan(
            ForegroundColorSpan(parent1Color),
            legendStart1,
            legendStart1 + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val legendStart2 = builder.length
        builder.append("■ = $parent2Name\n")
        builder.setSpan(
            ForegroundColorSpan(parent2Color),
            legendStart2,
            legendStart2 + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val legendStart3 = builder.length
        builder.append("■ = Sin custodia")
        builder.setSpan(
            ForegroundColorSpan(noCustodyColor),
            legendStart3,
            legendStart3 + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return builder
    }
}
