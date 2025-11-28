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

    // Colores pastel (suaves, estables)
    private val parent1Color = Color.parseColor("#FFE780") // Amarillo pastel
    private val parent2Color = Color.parseColor("#95A9FF") // Azul pastel
    private val noCustodyColor = Color.parseColor("#D0D0D0") // Gris claro
    private val selectionColor = Color.parseColor("#7EDC82") // Verde pastel para selección

    var rangeSelectionManager: RangeSelectionManager? = null

    // Detectar si un color es oscuro (para texto blanco/negro)
    private fun isColorDark(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return luminance < 0.50
    }

    fun renderMonthWithCustody(
        yearMonth: YearMonth,
        custodyCalculator: MainActivity.CustodyCalculator,
        parent1Name: String,
        parent2Name: String
    ): CharSequence {
        val builder = SpannableStringBuilder()

        // -----------------------------
        // CABECERA CENTRADA Y EN NEGRITA
        // -----------------------------
        val daysHeader = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do")
        val headerStart = builder.length
        for (day in daysHeader) {
            // cada columna ocupa 4 caracteres (" sp + XX + sp")
            val centered = day.padStart((day.length + 4) / 2).padEnd(4)
            builder.append(centered)
        }
        builder.append("\n\n")
        builder.setSpan(StyleSpan(Typeface.BOLD), headerStart, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // -----------------------------
        // DÍAS DEL MES (cada "celda" es " <n> " = 4 chars)
        // -----------------------------
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
                ParentType.PARENT1 -> parent1Color
                ParentType.PARENT2 -> parent2Color
                ParentType.NONE -> noCustodyColor
            }

            // Si está en selección → usar color de selección (sobrescribe)
            val bgColor = if (rangeSelectionManager?.isDateInRange(date) == true) selectionColor else baseBgColor

            // Texto del día con un espacio a cada lado (margen visual)
            val number = String.format("%2d", day)
            val chunk = " $number " // 4 chars: sp + 2 dígitos + sp

            val start = builder.length
            builder.append(chunk)
            val end = builder.length

            // Aplicamos BackgroundColorSpan (pastel, estable)
            builder.setSpan(BackgroundColorSpan(bgColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Elegir color del texto según contraste
            val textColor = if (isColorDark(bgColor)) Color.WHITE else Color.BLACK
            builder.setSpan(ForegroundColorSpan(textColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Manejo de saltos: si es domingo -> nueva línea; si no, seguimos (sin añadir espacios extra)
            if (currentDayOfWeek == 7) {
                builder.append("\n")
                currentDayOfWeek = 1
            } else {
                currentDayOfWeek++
            }
        }

        // Asegura nueva línea al final si no terminó en domingo
        if (currentDayOfWeek != 1) builder.append("\n")

        // -----------------------------
        // LEYENDA FINAL
        // -----------------------------
        builder.append("\n")
        val l1 = builder.length
        builder.append("■ = $parent1Name\n")
        builder.setSpan(ForegroundColorSpan(parent1Color), l1, l1 + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        val l2 = builder.length
        builder.append("■ = $parent2Name\n")
        builder.setSpan(ForegroundColorSpan(parent2Color), l2, l2 + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        val l3 = builder.length
        builder.append("■ = Sin custodia")
        builder.setSpan(ForegroundColorSpan(noCustodyColor), l3, l3 + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return builder
    }
}
