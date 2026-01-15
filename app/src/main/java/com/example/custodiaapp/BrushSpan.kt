package com.example.custodiaapp

import android.graphics.*
import android.text.style.ReplacementSpan
import kotlin.random.Random

class BrushSpan(private val color: Int) : ReplacementSpan() {

    private val alphaLevel = 130   // intensidad del color (más bajo = más suave)
    private val verticalNoise = 6  // variación vertical de la textura
    private val horizontalPad = 10f
    private val verticalPad = 6f

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return (paint.measureText(text, start, end) + horizontalPad * 2).toInt()
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
        val originalColor = paint.color

        // Color suave del pincel
        val brushPaint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
            isAntiAlias = true
            alpha = alphaLevel
        }

        val textWidth = paint.measureText(text, start, end)
        val left = x
        val right = x + textWidth + horizontalPad * 2
        val baseline = y.toFloat()

        // Altura del trazo
        val topY = baseline + paint.ascent() - verticalPad
        val bottomY = baseline + verticalPad

        // Dibujamos varias pinceladas irregulares
        for (i in 0..7) {
            val noiseTop = topY + Random.nextInt(-verticalNoise, verticalNoise)
            val noiseBottom = bottomY + Random.nextInt(-verticalNoise, verticalNoise)
            val offsetX = Random.nextInt(-2, 3).toFloat()

            val rect = RectF(
                left + offsetX,
                noiseTop,
                right + offsetX,
                noiseBottom
            )

            canvas.drawRoundRect(rect, 14f, 14f, brushPaint)
        }

        // Dibujar texto encima
        paint.color = originalColor
        canvas.drawText(text, start, end, x + horizontalPad, baseline, paint)
    }
}
