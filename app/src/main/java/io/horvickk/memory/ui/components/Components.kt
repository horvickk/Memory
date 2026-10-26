package io.horvickk.memory.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import io.horvickk.memory.ui.theme.Dimens
import io.horvickk.memory.ui.theme.LocalAppColors
import io.horvickk.memory.ui.theme.Type

/** Текст с цветом по умолчанию из темы. */
@Composable
fun Txt(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalAppColors.current.ink,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    autoSize: TextAutoSize? = null,
) {
    BasicText(
        text,
        modifier,
        style.copy(color = color, textAlign = align),
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        autoSize = autoSize,
    )
}

/** Клик без ряби — как в оригинальном минималистичном интерфейсе. */
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clickable(enabled = enabled, interactionSource = null, indication = null, onClick = onClick)

/** Срабатывает в момент касания, а не отпускания — для быстрых игровых нажатий. */
@Composable
fun Modifier.press(enabled: Boolean = true, onPress: () -> Unit): Modifier {
    val current by rememberUpdatedState(onPress)
    if (!enabled) return this
    return this
        .semantics {
            role = Role.Button
            this.onClick(label = null, action = { current(); true })
        }
        .pointerInput(Unit) { detectTapGestures(onPress = { current() }) }
}

@Composable
fun Modifier.inkBorder(width: Dp = Dimens.border, color: Color = LocalAppColors.current.ink): Modifier =
    border(width, color)

/** Кнопка в рамке; при нажатии заливается цветом чернил. */
@Composable
fun InkButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fireOnPress: Boolean = false,
    fill: Color? = null,
    content: (@Composable BoxScope.(contentColor: Color) -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    var pressed by remember { mutableStateOf(false) }
    val currentClick by rememberUpdatedState(onClick)
    val inverted = pressed && enabled
    val background = when {
        inverted -> colors.ink
        fill != null -> fill
        else -> Color.Transparent
    }
    val contentColor = if (inverted) colors.paper else colors.ink
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.35f)
            .inkBorder()
            .background(background)
            .semantics {
                role = Role.Button
                this.onClick(label = null, action = { if (enabled) currentClick(); enabled })
            }
            .pointerInput(enabled, fireOnPress) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            if (fireOnPress) currentClick()
                            tryAwaitRelease()
                        } finally {
                            // Кнопка могла стать неактивной прямо во время нажатия — не оставляем её «залитой».
                            pressed = false
                        }
                    },
                    onTap = { if (!fireOnPress) currentClick() },
                )
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) content(contentColor)
        else Txt(text, Type.button, color = contentColor, align = TextAlign.Center)
    }
}

/** Крупное сообщение по центру игровой области. */
@Composable
fun GameMessage(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    footer: String? = null,
    titleColor: Color = LocalAppColors.current.ink,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Column(
        modifier
            .fillMaxSize()
            .then(if (onClick != null) Modifier.tap(onClick = onClick) else Modifier)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Txt(title, Type.huge, color = titleColor, align = TextAlign.Center)
        if (subtitle != null) {
            Spacer(Modifier.height(12.dp))
            Txt(subtitle, Type.body, color = colors.inkSoft, align = TextAlign.Center)
        }
        if (footer != null) {
            Spacer(Modifier.height(32.dp))
            Txt(footer, Type.label, color = colors.inkSoft, align = TextAlign.Center)
        }
    }
}

/**
 * Квадратная сетка, вписанная в доступное место.
 * [cell] получает индекс клетки (по строкам) и её размер.
 */
@Composable
fun SquareGrid(
    rows: Int,
    cols: Int,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    maxCell: Dp = 120.dp,
    cell: @Composable BoxScope.(index: Int, size: Dp) -> Unit,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val byWidth = (maxWidth - spacing * (cols - 1)) / cols
        val byHeight = (maxHeight - spacing * (rows - 1)) / rows
        val size = min(min(byWidth, byHeight), maxCell)
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            repeat(rows) { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    repeat(cols) { c ->
                        Box(Modifier.size(size), contentAlignment = Alignment.Center) { cell(r * cols + c, size) }
                    }
                }
            }
        }
    }
}

/** Клетка в рамке с заливкой. */
@Composable
fun Cell(
    modifier: Modifier = Modifier,
    fill: Color = Color.Transparent,
    borderColor: Color = LocalAppColors.current.ink,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier
            .fillMaxSize()
            .background(fill)
            .border(Dimens.thinBorder, borderColor),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Полоска оставшегося времени показа. */
@Composable
fun TimerBar(durationMs: Long, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val progress = remember { Animatable(1f) }
    LaunchedEffect(durationMs) {
        progress.snapTo(1f)
        progress.animateTo(0f, tween(durationMs.toInt(), easing = LinearEasing))
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(colors.inkFaint)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(colors.ink)
        )
    }
}

/** Цифровая клавиатура: 1–9, стереть, 0, ОК. */
@Composable
fun NumPad(
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    okEnabled: Boolean = true,
    enabled: Boolean = true,
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "ОК")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in keys.chunked(3)) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (key in row) {
                    val keyModifier = Modifier.weight(1f).fillMaxHeight()
                    when (key) {
                        "⌫" -> InkButton(
                            "",
                            keyModifier,
                            enabled = enabled,
                            content = { c -> LineIcon(IconKind.ERASE, size = 28.dp, color = c) },
                            onClick = onErase,
                        )
                        "ОК" -> InkButton(key, keyModifier, enabled = enabled && okEnabled, onClick = onOk)
                        else -> InkButton(key, keyModifier, enabled = enabled, onClick = { onDigit(key.toInt()) })
                    }
                }
            }
        }
    }
}

enum class HintTone { NORMAL, CORRECT, WRONG }

/**
 * Типовая раскладка игровой области: строка-подсказка сверху и поле под ней.
 * Подсказка же сообщает «ВЕРНО» / «ОШИБКА».
 */
@Composable
fun GameColumn(
    hint: String,
    modifier: Modifier = Modifier,
    tone: HintTone = HintTone.NORMAL,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val hintColor = when (tone) {
        HintTone.NORMAL -> colors.inkSoft
        HintTone.CORRECT -> colors.correct
        HintTone.WRONG -> colors.wrong
    }
    Column(modifier.fillMaxSize().padding(12.dp)) {
        Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
            Txt(
                hint,
                Type.label,
                color = hintColor,
                align = TextAlign.Center,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = Type.label.fontSize, stepSize = 0.5.sp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center, content = content)
    }
}

/** Прозрачный слой, ловящий нажатие поверх игры («нажми, чтобы продолжить»). */
@Composable
fun BoxScope.TapCatcher(onTap: () -> Unit) {
    Box(Modifier.matchParentSize().tap(onClick = onTap))
}
