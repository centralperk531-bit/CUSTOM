package com.ismaelcr.custodiaapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        val rvCalendarGrid: RecyclerView = view.findViewById(R.id.rvCalendarGrid)
        val tvLegend1: TextView = view.findViewById(R.id.tvLegend1)
        val tvLegend2: TextView = view.findViewById(R.id.tvLegend2)
        val tvLegend3: TextView = view.findViewById(R.id.tvLegend3)
        val tvLegend4: TextView = view.findViewById(R.id.tvLegend4)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_page, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val yearMonth = getYearMonthForPosition(position)

        // Título del mes
        val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        holder.monthTitle.text = "${monthName.replaceFirstChar { it.uppercase() }} ${yearMonth.year}"

        // Generar celdas
        val cells = buildCells(yearMonth)

        // Configurar RecyclerView
        holder.rvCalendarGrid.layoutManager = GridLayoutManager(activity, 7)
        holder.rvCalendarGrid.adapter = CalendarDayAdapter(cells) { date ->
            activity.onCalendarDateClicked(date)
        }

        // Leyenda
        setupLegend(holder)
    }

    private fun buildCells(yearMonth: YearMonth): List<CalendarCell> {
        val cells = mutableListOf<CalendarCell>()
        val custodyCalculator = MainActivity.CustodyCalculator(viewModel)

        val firstDay = yearMonth.atDay(1)
        val lastDay = yearMonth.atEndOfMonth()
        val firstDayOfWeek = firstDay.dayOfWeek.value // 1=Lunes, 7=Domingo

        // Celdas vacías al inicio
        repeat(firstDayOfWeek - 1) {
            cells.add(CalendarCell(null, Color.TRANSPARENT, Color.TRANSPARENT, ""))
        }

        // Días del mes
        for (day in 1..lastDay.dayOfMonth) {
            val date = yearMonth.atDay(day)
            val custody = custodyCalculator.getCustodyForDate(date)

            val baseBgColor = when (custody.parent) {
                ParentType.PARENT1 -> Color.parseColor("#FFE780")
                ParentType.PARENT2 -> Color.parseColor("#95A9FF")
                ParentType.NONE -> Color.parseColor("#D0D0D0")
            }

            val isSpecialPeriod = custody.parent == ParentType.NONE
                    || viewModel.summerEvents.any { date in it.startDate..it.endDate }
                    || viewModel.noCustodyPeriods.any { date in it.startDate..it.endDate }
            val visitParent = if (!isSpecialPeriod) viewModel.getVisitParent(date, custody.parent) else null
            val isVisitFromSpecial = viewModel.specialDates.any {
                it.date == date && it.description == "Visita"
            }
            val isVisitDay = isVisitFromSpecial || (visitParent != null && visitParent != custody.parent)

            val bgColor = when {
                calendarRenderer.rangeSelectionManager?.isDateInRange(date) == true ->
                    Color.parseColor("#7EDC82")
                isVisitDay -> Color.parseColor("#F8BBD0")
                else -> baseBgColor
            }

            val luminance = with(bgColor) {
                val r = Color.red(this) / 255.0
                val g = Color.green(this) / 255.0
                val b = Color.blue(this) / 255.0
                0.2126 * r + 0.7152 * g + 0.0722 * b
            }
            val textColor = if (luminance < 0.50) Color.WHITE else Color.BLACK

            cells.add(CalendarCell(date, bgColor, textColor, day.toString()))
        }

        // Celdas vacías al final para completar la última fila
        val remaining = (7 - cells.size % 7) % 7
        repeat(remaining) {
            cells.add(CalendarCell(null, Color.TRANSPARENT, Color.TRANSPARENT, ""))
        }

        return cells
    }

    private fun setupLegend(holder: CalendarViewHolder) {
        val p1Color = Color.parseColor("#FFE780")
        val p2Color = Color.parseColor("#95A9FF")
        val noneColor = Color.parseColor("#D0D0D0")
        val visitColor = Color.parseColor("#F8BBD0")

        fun setLegendItem(tv: TextView, color: Int, label: String) {
            val span = android.text.SpannableString("■ = $label")
            span.setSpan(
                android.text.style.ForegroundColorSpan(color),
                0, 1,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            tv.text = span
            tv.setTextColor(Color.BLACK)
        }

        setLegendItem(holder.tvLegend1, p1Color, viewModel.parent1Name)
        setLegendItem(holder.tvLegend2, p2Color, viewModel.parent2Name)
        setLegendItem(holder.tvLegend3, noneColor, "Sin custodia")
        setLegendItem(holder.tvLegend4, visitColor, "Visita")
    }


    override fun getItemCount(): Int {
        return (endMonth.year - startMonth.year) * 12 +
                (endMonth.monthValue - startMonth.monthValue) + 1

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
