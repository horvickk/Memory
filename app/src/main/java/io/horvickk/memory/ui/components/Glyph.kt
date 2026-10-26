package io.horvickk.memory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class GlyphShape { CIRCLE, SQUARE, TRIANGLE, DIAMOND, STAR, PLUS, HEXAGON, RING }

/** Фигура определённой формы и цвета — «карточка» для зрительных упражнений. */
data class Glyph(val shape: GlyphShape, val colorIndex: Int)

@Composable
fun GlyphView(shape: GlyphShape, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawGlyph(shape, color) }
}

fun DrawScope.drawGlyph(shape: GlyphShape, color: Color) {
    val s = size.minDimension
    val r = s * 0.36f
    val c = Offset(size.width / 2, size.height / 2)

    fun polygon(points: Int, radius: Float, startDeg: Double, inner: Float? = null): Path {
        val path = Path()
        val total = if (inner != null) points * 2 else points
        for (i in 0 until total) {
            val rad = if (inner != null && i % 2 == 1) inner else radius
            val a = Math.toRadians(startDeg) + i * 2 * PI / total
            val x = c.x + rad * cos(a).toFloat()
            val y = c.y + rad * sin(a).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    when (shape) {
        GlyphShape.CIRCLE -> drawCircle(color, r, c)
        GlyphShape.RING -> drawCircle(color, r * 0.82f, c, style = Stroke(width = r * 0.36f))
        GlyphShape.SQUARE -> {
            val half = r * 0.84f
            drawRect(color, Offset(c.x - half, c.y - half), androidx.compose.ui.geometry.Size(half * 2, half * 2))
        }
        GlyphShape.TRIANGLE -> drawPath(polygon(3, r * 1.12f, -90.0).apply { translate(Offset(0f, r * 0.18f)) }, color)
        GlyphShape.DIAMOND -> drawPath(polygon(4, r * 1.08f, -90.0), color)
        GlyphShape.STAR -> drawPath(polygon(5, r * 1.12f, -90.0, inner = r * 0.48f).apply { translate(Offset(0f, r * 0.06f)) }, color)
        GlyphShape.HEXAGON -> drawPath(polygon(6, r * 1.02f, 0.0), color)
        GlyphShape.PLUS -> {
            val arm = r * 0.36f
            val len = r * 0.98f
            drawRect(color, Offset(c.x - len, c.y - arm), androidx.compose.ui.geometry.Size(len * 2, arm * 2))
            drawRect(color, Offset(c.x - arm, c.y - len), androidx.compose.ui.geometry.Size(arm * 2, len * 2))
        }
    }
}
