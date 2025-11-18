package com.example.custodiaapp

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class CalendarPagerAdapter(
    private val activity: MainActivity,
    private val calendarRenderer: CalendarRenderer,
    private val viewModel: CustodyViewModel
) : RecyclerView.Adapter<CalendarPagerAdapter.CalendarViewHolder>() {

    private val startMonth = YearMonth.now().minusMonths(120)
    private val endMonth = YearMonth.now().plusMonths(120)

    class CalendarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val monthTitle: TextView = view.findViewById(R.id.tvMonthTitle)
        val calendarText: TextView = view.findViewById(R.id.tvCalendar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_page, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val yearMonth = getYearMonthForPosition(position)

        val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        val monthCapitalized = monthName.replaceFirstChar { it.uppercase() }
        holder.monthTitle.text = "$monthCapitalized ${yearMonth.year}"

        val custodyCalculator = MainActivity.CustodyCalculator(viewModel)
        val calendarText = calendarRenderer.renderMonthWithCustody(
            yearMonth,
            custodyCalculator,
            viewModel.parent1Name,
            viewModel.parent2Name
        )

        // Hacer el calendario clickeable día por día
        val clickableCalendar = makeCalendarClickable(calendarText, yearMonth)
        holder.calendarText.text = clickableCalendar
        holder.calendarText.movementMethod = LinkMovementMethod.getInstance()
        holder.calendarText.highlightColor = Color.TRANSPARENT
    }

    private fun makeCalendarClickable(calendarText: CharSequence, yearMonth: YearMonth): SpannableString {
        val spannable = SpannableString(calendarText)
        val text = calendarText.toString()

        // Buscar el inicio del calendario (después del header)
        val headerEnd = text.indexOf("\n\n") + 2
        if (headerEnd < 2) return spannable

        val firstDay = yearMonth.atDay(1)
        val lastDay = yearMonth.atEndOfMonth()
        val firstDayOfWeek = firstDay.dayOfWeek.value

        var currentIndex = headerEnd
        var currentDayOfWeek = firstDayOfWeek

        // Saltar espacios iniciales
        repeat(firstDayOfWeek - 1) {
            currentIndex += 4 // "    "
        }

        for (day in 1..lastDay.dayOfMonth) {
            val date = yearMonth.atDay(day)

            // Encontrar los dos dígitos del día
            val dayStr = String.format("%2d", day)
            val dayStart = currentIndex
            val dayEnd = dayStart + 2

            if (dayEnd <= text.length) {
                spannable.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            activity.onCalendarDateClicked(date)
                        }
                        override fun updateDrawState(ds: android.text.TextPaint) {
                            // No subrayar ni cambiar color
                            ds.isUnderlineText = false
                        }
                    },
                    dayStart,
                    dayEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (currentDayOfWeek == 7) {
                currentIndex = text.indexOf('\n', currentIndex) + 1
                currentDayOfWeek = 1
            } else {
                currentIndex += 4 // "##  "
                currentDayOfWeek++
            }
        }

        return spannable
    }

    override fun getItemCount(): Int {
        return (endMonth.year - startMonth.year) * 12 + (endMonth.monthValue - startMonth.monthValue) + 1
    }

    fun getInitialPosition(): Int {
        val now = YearMonth.now()
        return (now.year - startMonth.year) * 12 + (now.monthValue - startMonth.monthValue)
    }

    private fun getYearMonthForPosition(position: Int): YearMonth {
        val totalMonths = startMonth.year * 12 + startMonth.monthValue - 1 + position
        val year = totalMonths / 12
        val month = totalMonths % 12 + 1
        return YearMonth.of(year, month)
    }
}