package com.horvickk.memory.ui.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class AppColors(
    val paper: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val correct: Color,
    val wrong: Color,
    /** Цвета для игр. Порядок фиксирован: индекс цвета — часть игровой логики. */
    val palette: List<Color>,
)

val LightColors = AppColors(
    paper = Color(0xFFFAF8F2),
    ink = Color(0xFF333333),
    inkSoft = Color(0xFF555555),
    inkFaint = Color(0xFFD9D6CE),
    correct = Color(0xFF3E9B57),
    wrong = Color(0xFFD9473F),
    palette = listOf(
        Color(0xFFE0483E), // красный
        Color(0xFF2F6FD6), // синий
        Color(0xFF3E9B57), // зелёный
        Color(0xFFF2B705), // жёлтый
        Color(0xFF8B55C9), // фиолетовый
        Color(0xFFF07B22), // оранжевый
    ),
)

val DarkColors = AppColors(
    paper = Color(0xFF1C1B19),
    ink = Color(0xFFECE9E1),
    inkSoft = Color(0xFFB8B4AB),
    inkFaint = Color(0xFF3D3B37),
    correct = Color(0xFF5DBB76),
    wrong = Color(0xFFEF6158),
    palette = listOf(
        Color(0xFFF0574C),
        Color(0xFF5B8FEA),
        Color(0xFF55B26E),
        Color(0xFFF5C431),
        Color(0xFFA57BDB),
        Color(0xFFF58F43),
    ),
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

object Dimens {
    val border = 2.5.dp
    val thinBorder = 2.dp
    val screenPadding = 20.dp
}

object Type {
    private val family = FontFamily.SansSerif
    val screenTitle = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 1.sp)
    val cardTitle = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 21.sp, letterSpacing = 0.5.sp)
    val cardSubtitle = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.5.sp, lineHeight = 18.sp)
    val body = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 15.sp, letterSpacing = 0.4.sp, lineHeight = 20.sp)
    val label = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 1.sp)
    val button = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.sp)
    val huge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = 1.sp)
}

@Composable
fun MemoryTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalTextSelectionColors provides TextSelectionColors(colors.ink, colors.inkFaint),
        content = content,
    )
}
