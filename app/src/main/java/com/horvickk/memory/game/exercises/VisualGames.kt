package com.horvickk.memory.game.exercises

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horvickk.memory.game.GameContext
import com.horvickk.memory.game.LevelGame
import com.horvickk.memory.game.RoundScope
import com.horvickk.memory.ui.components.Cell
import com.horvickk.memory.ui.components.GameColumn
import com.horvickk.memory.ui.components.Glyph
import com.horvickk.memory.ui.components.GlyphShape
import com.horvickk.memory.ui.components.GlyphView
import com.horvickk.memory.ui.components.SquareGrid
import com.horvickk.memory.ui.components.TimerBar
import com.horvickk.memory.ui.components.Txt
import com.horvickk.memory.ui.components.press
import com.horvickk.memory.ui.theme.Dimens
import com.horvickk.memory.ui.theme.LocalAppColors
import com.horvickk.memory.ui.theme.Type
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.random.Random

private const val COLOR_COUNT = 6

internal val ALL_GLYPHS: List<Glyph> = GlyphShape.entries.flatMap { shape -> (0 until COLOR_COUNT).map { Glyph(shape, it) } }

/** Набор различных фигур: сначала разные формы, при нехватке — повторы формы другим цветом. */
internal fun distinctGlyphs(count: Int, random: Random = Random): List<Glyph> {
    val shapes = GlyphShape.entries.shuffled(random)
    val result = mutableListOf<Glyph>()
    val colors = (0 until COLOR_COUNT).shuffled(random)
    for (i in 0 until minOf(count, shapes.size)) result += Glyph(shapes[i], colors[i % COLOR_COUNT])
    if (count > result.size) {
        result += (ALL_GLYPHS - result.toSet()).shuffled(random).take(count - result.size)
    }
    return result.shuffled(random)
}

/** Фигура, отличающаяся от исходной либо цветом, либо формой. */
internal fun alteredGlyph(original: Glyph, random: Random = Random): Glyph =
    if (random.nextBoolean()) {
        original.copy(colorIndex = ((0 until COLOR_COUNT) - original.colorIndex).random(random))
    } else {
        original.copy(shape = (GlyphShape.entries - original.shape).random(random))
    }

@Composable
private fun GlyphFill(glyph: Glyph, modifier: Modifier = Modifier) {
    GlyphView(glyph.shape, LocalAppColors.current.palette[glyph.colorIndex], modifier.fillMaxSize())
}

/** Поле для показа n фигур: число столбцов и строк. */
private fun itemsGrid(count: Int): Pair<Int, Int> {
    val cols = when {
        count <= 4 -> 2
        count <= 9 -> 3
        else -> 4
    }
    return ceil(count / cols.toDouble()).toInt() to cols
}

// ---------------------------------------------------------------------------------------------
// 5. Пары: классический «мемори», 16 карточек. Результат — число ходов (меньше — лучше).
// ---------------------------------------------------------------------------------------------

@Composable
fun PairsGame(ctx: GameContext) {
    val colors = LocalAppColors.current
    val cards = remember { distinctGlyphs(8).let { (it + it).shuffled() } }
    val matched = remember { mutableStateListOf<Int>() }
    var open by remember { mutableStateOf(listOf<Int>()) }
    var moves by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var closeJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(moves) { ctx.status("ХОДЫ $moves") }

    fun flip(index: Int) {
        if (index in matched || index in open || matched.size == cards.size) return
        if (open.size == 2) {
            // Не ждём, пока закроется неудачная пара, — сразу открываем новую карточку.
            closeJob?.cancel()
            open = emptyList()
        }
        ctx.feedback.tap()
        if (open.isEmpty()) {
            open = listOf(index)
            return
        }
        val first = open[0]
        open = listOf(first, index)
        moves++
        if (cards[first] == cards[index]) {
            matched += first
            matched += index
            open = emptyList()
            ctx.feedback.correct()
            if (matched.size == cards.size) {
                scope.launch {
                    delay(700)
                    ctx.finish(moves)
                }
            }
        } else {
            closeJob = scope.launch {
                delay(900)
                open = emptyList()
            }
        }
    }

    val hint = if (matched.size == cards.size) "ВСЕ ПАРЫ НАЙДЕНЫ" else "НАЙДЕНО ПАР: ${matched.size / 2} ИЗ 8"
    GameColumn(hint) {
        SquareGrid(4, 4, Modifier.fillMaxSize(), spacing = 10.dp) { index, _ ->
            val faceUp = index in open || index in matched
            Cell(
                Modifier.press(enabled = !faceUp) { flip(index) },
                fill = if (faceUp) Color.Transparent else colors.ink,
            ) {
                if (faceUp) {
                    GlyphFill(
                        cards[index],
                        Modifier
                            .padding(6.dp)
                            .alpha(if (index in matched) 0.35f else 1f),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 9. Что изменилось: одна фигура меняет цвет или форму.
// ---------------------------------------------------------------------------------------------

@Composable
fun ChangeGame(ctx: GameContext) {
    LevelGame(ctx, startLevel = 3) { round -> ChangeRound(round) }
}

private enum class ChangePhase { SHOW, BLANK, ASK }

@Composable
private fun ChangeRound(round: RoundScope) {
    val colors = LocalAppColors.current
    val ctx = round.ctx
    val count = round.level
    val side = when {
        count <= 6 -> 3
        count <= 11 -> 4
        else -> 5
    }
    val placement = remember {
        val cells = (0 until side * side).shuffled()
        cells.take(count).zip(distinctGlyphs(count)).toMap()
    }
    val changedCell = remember { placement.keys.random() }
    val changed = remember { placement + (changedCell to alteredGlyph(placement.getValue(changedCell))) }
    var phase by remember { mutableStateOf(ChangePhase.SHOW) }
    var tapped by remember { mutableIntStateOf(-1) }
    val showDuration = ctx.showMs(1500L + 500L * count)

    LaunchedEffect(Unit) {
        round.hint("ЗАПОМНИ ФИГУРЫ")
        delay(showDuration)
        phase = ChangePhase.BLANK
        round.hint("")
        delay(ctx.showMs(700))
        phase = ChangePhase.ASK
        round.hint("ЧТО ИЗМЕНИЛОСЬ?")
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.Center) {
            if (phase == ChangePhase.SHOW) TimerBar(showDuration)
        }
        SquareGrid(side, side, Modifier.fillMaxWidth().weight(1f), spacing = 10.dp) { index, _ ->
            val items = if (phase == ChangePhase.ASK) changed else placement
            val glyph = items[index]
            val border = when {
                round.finished && index == changedCell -> colors.correct
                index == tapped -> colors.wrong
                else -> colors.ink
            }
            Cell(
                Modifier.press(enabled = phase == ChangePhase.ASK && glyph != null) {
                    if (round.finished) return@press
                    tapped = index
                    round.finish(index == changedCell)
                },
                borderColor = border,
            ) {
                if (glyph != null && phase != ChangePhase.BLANK) GlyphFill(glyph, Modifier.padding(8.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 10. Где это было: запомнить расположение фигур, затем указать место каждой.
// ---------------------------------------------------------------------------------------------

@Composable
fun LocationGame(ctx: GameContext) {
    LevelGame(ctx, startLevel = 3) { round -> LocationRound(round) }
}

@Composable
private fun LocationRound(round: RoundScope) {
    val colors = LocalAppColors.current
    val ctx = round.ctx
    val count = round.level
    val side = if (count <= 8) 4 else 5
    val placement = remember {
        (0 until side * side).shuffled().take(count).zip(distinctGlyphs(count)).toMap()
    }
    val order = remember { placement.keys.shuffled() }
    var asking by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) }
    val placed = remember { mutableStateListOf<Int>() }
    var wrongCell by remember { mutableIntStateOf(-1) }
    val showDuration = ctx.showMs(1000L + 700L * count)

    LaunchedEffect(Unit) {
        round.hint("ЗАПОМНИ, ГДЕ ФИГУРЫ")
        delay(showDuration)
        asking = true
        round.hint("ГДЕ БЫЛА ЭТА ФИГУРА?")
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(76.dp), contentAlignment = Alignment.Center) {
            if (!asking) {
                TimerBar(showDuration)
            } else if (step < order.size) {
                Box(Modifier.size(64.dp).border(Dimens.border, colors.ink)) {
                    GlyphFill(placement.getValue(order[step]), Modifier.padding(8.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SquareGrid(side, side, Modifier.fillMaxWidth().weight(1f), spacing = 8.dp) { index, _ ->
            val target = order.getOrNull(step)
            val visible = !asking || index in placed || (round.finished && placement.containsKey(index))
            val border = when {
                index == wrongCell -> colors.wrong
                round.finished && index == target -> colors.correct
                else -> colors.ink
            }
            Cell(
                Modifier.press(enabled = asking && index !in placed) {
                    if (round.finished) return@press
                    if (index == target) {
                        placed += index
                        ctx.feedback.tone(step)
                        step++
                        if (step == order.size) round.finish(true)
                    } else {
                        wrongCell = index
                        round.finish(false)
                    }
                },
                borderColor = border,
            ) {
                val glyph = placement[index]
                if (glyph != null && visible) GlyphFill(glyph, Modifier.padding(7.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 15. Пропажа: одна фигура исчезает, нужно выбрать её среди вариантов.
// ---------------------------------------------------------------------------------------------

@Composable
fun MissingGame(ctx: GameContext) {
    LevelGame(ctx, startLevel = 3) { round -> MissingRound(round) }
}

/** Варианты ответа: пропавшая фигура и похожие на неё (та же форма или тот же цвет), которых не было. */
internal fun missingOptions(missing: Glyph, shown: List<Glyph>, random: Random = Random): List<Glyph> {
    val unused = ALL_GLYPHS - shown.toSet()
    val similar = unused.filter { it.shape == missing.shape || it.colorIndex == missing.colorIndex }.shuffled(random)
    val other = (unused - similar.toSet()).shuffled(random)
    return ((similar + other).take(3) + missing).shuffled(random)
}

@Composable
private fun MissingRound(round: RoundScope) {
    val colors = LocalAppColors.current
    val ctx = round.ctx
    val count = round.level
    val items = remember { distinctGlyphs(count) }
    val missing = remember { items.random() }
    val remaining = remember { (items - missing).shuffled() }
    val options = remember { missingOptions(missing, items) }
    var asking by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Glyph?>(null) }
    val (rows, cols) = itemsGrid(count)
    val showDuration = ctx.showMs(1200L + 600L * count)

    LaunchedEffect(Unit) {
        round.hint("ЗАПОМНИ ФИГУРЫ")
        delay(showDuration)
        round.hint("")
        delay(ctx.showMs(400))
        asking = true
        round.hint("КАКОЙ ФИГУРЫ НЕ ХВАТАЕТ?")
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.Center) {
            if (!asking) TimerBar(showDuration)
        }
        val shown = if (asking) remaining else items
        SquareGrid(rows, cols, Modifier.fillMaxWidth().weight(1f), spacing = 10.dp, maxCell = 100.dp) { index, _ ->
            val glyph = shown.getOrNull(index)
            if (glyph != null) GlyphFill(glyph, Modifier.padding(6.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().height(84.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (asking) {
                for (option in options) {
                    val border = when {
                        round.finished && option == missing -> colors.correct
                        option == picked -> colors.wrong
                        else -> colors.ink
                    }
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        Cell(
                            Modifier.press {
                                if (round.finished) return@press
                                picked = option
                                round.finish(option == missing)
                            },
                            borderColor = border,
                        ) {
                            GlyphFill(option, Modifier.padding(10.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 16. Очерёдность: фигуры появляются по одной, затем их нужно нажать в том же порядке.
// ---------------------------------------------------------------------------------------------

@Composable
fun OrderGame(ctx: GameContext) {
    LevelGame(ctx, startLevel = 3) { round -> OrderRound(round) }
}

@Composable
private fun OrderRound(round: RoundScope) {
    val colors = LocalAppColors.current
    val ctx = round.ctx
    val count = round.level
    val sequence = remember { distinctGlyphs(count) }
    val layout = remember { sequence.indices.shuffled() } // клетка -> индекс в последовательности
    var current by remember { mutableIntStateOf(-1) }
    var asking by remember { mutableStateOf(false) }
    var next by remember { mutableIntStateOf(0) }
    var wrongCell by remember { mutableIntStateOf(-1) }
    val (rows, cols) = itemsGrid(count)

    LaunchedEffect(Unit) {
        round.hint("ЗАПОМИНАЙ ПОРЯДОК")
        delay(ctx.showMs(600))
        for (i in sequence.indices) {
            current = i
            ctx.feedback.silentTone(i)
            delay(ctx.showMs(900))
            current = -1
            delay(ctx.showMs(250))
        }
        asking = true
        round.hint("НАЖИМАЙ В ТОМ ЖЕ ПОРЯДКЕ")
    }

    if (!asking) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(180.dp).border(Dimens.border, colors.ink)) {
                if (current >= 0) GlyphFill(sequence[current], Modifier.padding(24.dp))
            }
        }
        return
    }

    SquareGrid(rows, cols, Modifier.fillMaxSize(), spacing = 10.dp, maxCell = 110.dp) { index, size ->
        val seqIndex = layout.getOrNull(index) ?: return@SquareGrid
        val done = seqIndex < next
        Cell(
            Modifier.press(enabled = !done) {
                if (round.finished) return@press
                if (seqIndex == next) {
                    ctx.feedback.tone(next)
                    next++
                    if (next == count) round.finish(true)
                } else {
                    wrongCell = index
                    round.finish(false)
                }
            },
            borderColor = if (index == wrongCell) colors.wrong else colors.ink,
        ) {
            GlyphFill(sequence[seqIndex], Modifier.padding(10.dp).alpha(if (done) 0.3f else 1f))
            if (done || round.finished) {
                Txt(
                    (seqIndex + 1).toString(),
                    Type.button.copy(fontSize = (size.value * 0.3f).sp),
                    Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 2.dp),
                )
            }
        }
    }
}
