package com.scanpilot.app.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/** Annotation tools for marking up a PDF page. */
enum class AnnotationTool(val label: String) {
    HIGHLIGHT("Highlight"),
    UNDERLINE("Underline"),
    STRIKE("Strikethrough"),
    DRAW("Draw"),
    SIGN("Sign")
}

/**
 * One annotation, with points normalized (0 to 1) relative to the page
 * size, so it draws correctly on any page size.
 */
data class AnnotationStroke(
    val tool: AnnotationTool,
    val points: List<Offset>,
    val colorArgb: Int = android.graphics.Color.YELLOW,
    val strokeWidth: Float = 10f
) {
    val color: Color get() = Color(colorArgb)
}
