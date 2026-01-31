package com.ismaelcr.custodiaapp

import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance

/**
 * ForegroundColorSpan compatible con ReplacementSpan.
 * Actualiza el color del TextPaint al dibujar texto normal (útil para la leyenda).
 */
class ForegroundColorSpanCompat(private val color: Int) : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) {
        tp.color = color
    }
}
