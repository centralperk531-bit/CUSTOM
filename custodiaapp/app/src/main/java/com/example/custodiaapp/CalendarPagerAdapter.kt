package com.example.custodiaapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class CalendarPagerAdapter(
    private val activity: MainActivity,
    private val calendarRenderer: CalendarRenderer,
    private val viewModel: CustodyViewModel
) : RecyclerView.Adapter<CalendarPagerAdapter.CalendarViewHolder>() {

    private val startMonth = YearMonth.now().minusMonths(120) // 10 años atrás
    private val endMonth = YearMonth.now().plusMonths(120) // 10 años adelante

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

        // Título del mes en español con primera letra mayúscula
        val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        val monthCapitalized = monthName.replaceFirstChar { it.uppercase() }
        holder.monthTitle.text = "$monthCapitalized ${yearMonth.year}"

        // Renderizar calendario con custodia
        val custodyCalculator = MainActivity.CustodyCalculator(viewModel)
        val calendarText = calendarRenderer.renderMonthWithCustody(
            yearMonth,
            custodyCalculator,
            viewModel.parent1Name,
            viewModel.parent2Name
        )
        holder.calendarText.text = calendarText
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