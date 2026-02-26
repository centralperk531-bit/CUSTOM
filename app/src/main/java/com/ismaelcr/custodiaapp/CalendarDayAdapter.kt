package com.ismaelcr.custodiaapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

data class CalendarCell(
    val date: LocalDate?,       // null = celda vacía (relleno inicio/fin de mes)
    val bgColor: Int,
    val textColor: Int,
    val dayNumber: String
)

class CalendarDayAdapter(
    private val cells: List<CalendarCell>,
    private val onDayClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder>() {

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val cell = cells[position]

        if (cell.date == null) {
            // Celda vacía
            holder.tvDayNumber.text = ""
            holder.tvDayNumber.setBackgroundColor(Color.TRANSPARENT)
            holder.itemView.isClickable = false
        } else {
            holder.tvDayNumber.text = cell.dayNumber
            holder.tvDayNumber.setTextColor(cell.textColor)
            holder.tvDayNumber.setBackgroundResource(R.drawable.day_cell_background)
            holder.tvDayNumber.backgroundTintList =
                android.content.res.ColorStateList.valueOf(cell.bgColor)
            holder.itemView.setOnClickListener { onDayClick(cell.date) }
        }
    }

    override fun getItemCount(): Int = cells.size
}
