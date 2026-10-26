package com.horvickk.memory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.horvickk.memory.ui.theme.LocalAppColors
import kotlin.math.cos
import kotlin.math.sin

enum class IconKind { SOUND_ON, SOUND_OFF, SETTINGS, HISTORY, CHECK, CLOSE, RESTART, BACK, ERASE, HEART, HEART_EMPTY }

/** Линейные иконки, нарисованные вручную в сетке 24×24 — в духе минималистичного интерфейса. */
@Composable
fun LineIcon(kind: IconKind, modifier: Modifier = Modifier, size: Dp = 32.dp, color: Color = LocalAppColors.current.ink) {
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 24f
        scale(unit, unit, pivot = Offset.Zero) {
            drawIcon(kind, color)
        }
    }
}

private fun DrawScope.drawIcon(kind: IconKind, color: Color) {
    val stroke = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun line(vararg points: Pair<Float, Float>, width: Float = 1.9f) {
        val path = Path()
        points.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
        drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    fun polar(cx: Float, cy: Float, r: Float, deg: Float): Pair<Float, Float> {
        val a = Math.toRadians(deg.toDouble())
        return (cx + r * cos(a)).toFloat() to (cy + r * sin(a)).toFloat()
    }

    /** Наконечник стрелки в точке (x, y), направленный по (dx, dy). */
    fun arrowHead(x: Float, y: Float, dx: Float, dy: Float) {
        val px = -dy
        val py = dx
        line(x - dx * 3f + px * 2.6f to y - dy * 3f + py * 2.6f, x to y, x - dx * 3f - px * 2.6f to y - dy * 3f - py * 2.6f)
    }

    when (kind) {
        IconKind.SOUND_ON, IconKind.SOUND_OFF -> {
            line(3f to 9.5f, 7f to 9.5f, 11.5f to 5.5f, 11.5f to 18.5f, 7f to 14.5f, 3f to 14.5f, 3f to 9.5f)
            if (kind == IconKind.SOUND_ON) {
                drawArc(color, -45f, 90f, false, Offset(10f, 8.5f), Size(7f, 7f), style = stroke)
                drawArc(color, -50f, 100f, false, Offset(8f, 5f), Size(14f, 14f), style = stroke)
            } else {
                line(15.5f to 9.5f, 20.5f to 14.5f)
                line(20.5f to 9.5f, 15.5f to 14.5f)
            }
        }

        IconKind.SETTINGS -> {
            val path = Path()
            val teeth = 8
            for (i in 0 until teeth) {
                val c = i * 360f / teeth
                val points = listOf(
                    polar(12f, 12f, 7.3f, c - 13f),
                    polar(12f, 12f, 10f, c - 8f),
                    polar(12f, 12f, 10f, c + 8f),
                    polar(12f, 12f, 7.3f, c + 13f),
                )
                points.forEachIndexed { j, (x, y) -> if (i == 0 && j == 0) path.moveTo(x, y) else path.lineTo(x, y) }
            }
            path.close()
            drawPath(path, color, style = stroke)
            drawCircle(color, radius = 3.2f, center = Offset(12f, 12f), style = stroke)
        }

        IconKind.HISTORY -> {
            drawArc(color, 210f, 300f, false, Offset(5f, 4f), Size(16f, 16f), style = stroke)
            val (sx, sy) = polar(13f, 12f, 8f, 210f)
            arrowHead(sx - 0.5f, sy + 0.9f, -0.5f, 0.866f)
            line(13f to 8f, 13f to 12.5f, 16f to 14.3f)
        }

        IconKind.CHECK -> line(4f to 12.5f, 9.5f to 18f, 20f to 6.5f, width = 2.8f)

        IconKind.CLOSE -> {
            line(5f to 5f, 19f to 19f)
            line(19f to 5f, 5f to 19f)
        }

        IconKind.RESTART -> {
            drawArc(color, 200f, 130f, false, Offset(4f, 4f), Size(16f, 16f), style = stroke)
            drawArc(color, 20f, 130f, false, Offset(4f, 4f), Size(16f, 16f), style = stroke)
            val a = Math.toRadians(330.0)
            val (ax, ay) = polar(12f, 12f, 8f, 330f)
            arrowHead(ax, ay, -sin(a).toFloat(), cos(a).toFloat())
            val b = Math.toRadians(150.0)
            val (bx, by) = polar(12f, 12f, 8f, 150f)
            arrowHead(bx, by, -sin(b).toFloat(), cos(b).toFloat())
        }

        IconKind.BACK -> {
            line(20f to 12f, 4.5f to 12f)
            line(10.5f to 6f, 4.5f to 12f, 10.5f to 18f)
        }

        IconKind.ERASE -> {
            line(21f to 6f, 9f to 6f, 3f to 12f, 9f to 18f, 21f to 18f, 21f to 6f)
            line(12.5f to 9.5f, 17.5f to 14.5f)
            line(17.5f to 9.5f, 12.5f to 14.5f)
        }

        IconKind.HEART, IconKind.HEART_EMPTY -> {
            val path = Path().apply {
                moveTo(12f, 20.5f)
                cubicTo(12f, 20.5f, 3f, 14.5f, 3f, 8.7f)
                cubicTo(3f, 5.8f, 5.3f, 3.8f, 7.8f, 3.8f)
                cubicTo(9.6f, 3.8f, 11.1f, 4.8f, 12f, 6.3f)
                cubicTo(12.9f, 4.8f, 14.4f, 3.8f, 16.2f, 3.8f)
                cubicTo(18.7f, 3.8f, 21f, 5.8f, 21f, 8.7f)
                cubicTo(21f, 14.5f, 12f, 20.5f, 12f, 20.5f)
                close()
            }
            if (kind == IconKind.HEART) drawPath(path, color) else drawPath(path, color, style = stroke)
        }
    }
}
