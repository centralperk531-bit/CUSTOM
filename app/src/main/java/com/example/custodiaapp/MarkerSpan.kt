package com.example.custodiaapp

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import android.graphics.Color

class MarkerSpan(private val bgColor: Int) : ReplacementSpan() {

    // Decide color de texto en función del bgColor
    private fun contrastTextColor(bgColor: Int): Int {
        val r = Color.red(bgColor) / 255.0
        val g = Color.green(bgColor) / 255.0
        val b = Color.blue(bgColor) / 255.0
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return if (luminance < 0.5) Color.WHITE else Color.BLACK
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        // ancho del texto (no contamos padding aquí, ya que draw añade margen visual)
        return paint.measureText(text, start, end).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        // guardar estado
        val originalColor = paint.color
        val originalStyle = paint.style
        val originalTextSize = paint.textSize

        val markerPaint = Paint(paint).apply {
            color = bgColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textWidth = paint.measureText(text, start, end)
        val textHeight = paint.textSize

        // Ajustes: modifica estos valores si quieres trazo más ancho/alto
        val horizontalPad = 8f // padding lateral del marcador
        val verticalPadTop = 10f // cuánto sube el marcador respecto a baseline
        val verticalPadBottom = 10f

        val left = x - horizontalPad
        val right = x + textWidth + horizontalPad
        val topRect = y - textHeight + verticalPadTop
        val bottomRect = y + verticalPadBottom

        // Dibuja fondo tipo marcador (rectángulo redondeado)
        canvas.drawRoundRect(
            left,
            topRect,
            right,
            bottomRect,
            20f,
            12f,
            markerPaint
        )

        // Decide color de texto y dibuja el texto encima
        paint.color = contrastTextColor(bgColor)
        paint.style = Paint.Style.FILL

        // dibujar texto en la posición correcta
        canvas.drawText(text, start, end, x, y.toFloat(), paint)

        // restaurar paint
        paint.color = originalColor
        paint.style = originalStyle
        paint.textSize = originalTextSize
    }
}
