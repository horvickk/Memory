package io.horvickk.memory.game.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import io.horvickk.memory.game.GameContext
import io.horvickk.memory.game.LevelGame
import io.horvickk.memory.game.RoundScope
import io.horvickk.memory.ui.components.Cell
import io.horvickk.memory.ui.components.InkButton
import io.horvickk.memory.ui.components.SquareGrid
import io.horvickk.memory.ui.components.Txt
import io.horvickk.memory.ui.components.press
import io.horvickk.memory.ui.theme.Dimens
import io.horvickk.memory.ui.theme.LocalAppColors
import io.horvickk.memory.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// 1. Вспышки (тест Корси): вспышки клеток 3×3, последовательность растёт на одну.
// ---------------------------------------------------------------------------------------------

private class SequenceHolder {
    val cells = mutableListOf<Int>()
}

@Composable
fun SequenceGame(ctx: GameContext) {
    val holder = remember { SequenceHolder() }
    LevelGame(ctx, startLevel = 2) { round ->
        val sequence = remember {
            val cells = holder.cells
            // После успеха продолжаем ту же последовательность, после ошибки — начинаем новую.
            if (cells.size == round.level - 1) {
                cells += nextCell(cells.lastOrNull(), 9)
            } else {
                cells.clear()
                repeat(round.level) { cells += nextCell(cells.lastOrNull(), 9) }
            }
            cells.toList()
        }
        SequenceRound(round, sequence)
    }
}

/** Случайная клетка, не совпадающая с предыдущей (иначе две вспышки подряд сливаются). */
internal fun nextCell(previous: Int?, count: Int, random: Random = Random): Int {
    var cell: Int
    do {
        cell = random.nextInt(count)
    } while (cell == previous)
    return cell
}

@Composable
private fun SequenceRound(round: RoundScope, sequence: List<Int>) {
    val colors = LocalAppColors.current
    val ctx = round.ctx
    val scope = rememberCoroutineScope()
    var lit by remember { mutableIntStateOf(-1) }
    var flash by remember { mutableIntStateOf(-1) }
    var inputting by remember { mutableStateOf(false) }
    var position by remember { mutableIntStateOf(0) }
    var wrongCell by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        round.hint("ЗАПОМИНАЙ")
        delay(ctx.showMs(700))
        for (cell in sequence) {
            lit = cell
            ctx.feedback.silentTone(cell)
            delay(ctx.showMs(520))
            lit = -1
            delay(ctx.showMs(220))
        }
        round.hint("ПОВТОРИ · 0 ИЗ ${sequence.size}")
        inputting = true
    }

    SquareGrid(3, 3, Modifier.fillMaxSize(), spacing = 12.dp) { index, _ ->
        val fill = when (index) {
            wrongCell -> colors.wrong
            lit, flash -> colors.ink
            else -> Color.Transparent
        }
        Cell(
            Modifier.press(enabled = inputting) {
                if (round.finished) return@press
                if (sequence[position] == index) {
                    ctx.feedback.tone(index)
                    flash = index
                    scope.launch {
                        delay(180)
                        if (flash == index) flash = -1
                    }
                    position++
                    round.hint("ПОВТОРИ · $position ИЗ ${sequence.size}")
                    if (position == sequence.size) {
                        inputting = false
                        round.finish(true)
                    }
                } else {
                    wrongCell = index
                    inputting = false
                    round.finish(false)
                }
            },
            fill = fill,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// 2. Матрица: запомнить закрашенные клетки. Поле растёт вместе с уровнем.
// ---------------------------------------------------------------------------------------------

@Composable
fun MatrixGame(ctx: GameContext) {
    LevelGame(ctx, startLevel = 3) { round -> MatrixRound(round) }
}

internal fun matrixSide(cells: Int): Int = when {
    cells <= 4 -> 3
    cells <= 6 -> 4
    cells <= 9 -> 5
    cells <= 13 -> 6
    cells <= 24 -> 7
    else -> ceil(sqrt(cells * 2.0)).toInt()
}

private const val MATRIX_MISSES = 3

@Composable
private fun MatrixRound(round: RoundScope) {
    val colors = LocalAppColors.current
    val ctx = round.ctx
    val count = round.level
    val side = matrixSide(count)
    val targets = remember { (0 until side * side).shuffled().take(count).toSet() }
    var showing by remember { mutableStateOf(false) }
    var inputting by remember { mutableStateOf(false) }
    val found = remember { mutableStateListOf<Int>() }
    val misses = remember { mutableStateListOf<Int>() }

    LaunchedEffect(Unit) {
        round.hint("ЗАПОМИНАЙ")
        delay(ctx.showMs(500))
        showing = true
        delay(ctx.showMs(1000L + 150L * count))
        showing = false
        round.hint("ОТМЕТЬ КЛЕТКИ")
        inputting = true
    }

    SquareGrid(side, side, Modifier.fillMaxSize(), spacing = if (side > 5) 6.dp else 10.dp) { index, _ ->
        val fill = when {
            showing && index in targets -> colors.ink
            index in found -> colors.ink
            index in misses -> colors.wrong
            round.finished && index in targets -> colors.inkFaint
            else -> Color.Transparent
        }
        Cell(
            Modifier.press(enabled = inputting) {
                if (round.finished || index in found || index in misses) return@press
                if (index in targets) {
                    found += index
                    ctx.feedback.tone(found.size)
                    if (found.size == count) {
                        inputting = false
                        round.finish(true)
                    }
                } else {
                    misses += index
                    if (misses.size >= MATRIX_MISSES) {
                        inputting = false
                        round.finish(false)
                    } else {
                        ctx.feedback.wrong()
                        round.hint("ПРОМАХОВ: ${misses.size} ИЗ $MATRIX_MISSES")
                    }
                }
            },
            fill = fill,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// 6. Скрытые числа (тест «шимпанзе»): после нажатия на 1 остальные числа прячутся.
// ---------------------------------------------------------------------------------------------

private const val CHIMP_COLS = 5
private const val CHIMP_ROWS = 6

@Composable
fun ChimpGame(ctx: GameContext) {
    LevelGame(ctx, startLevel = 4) { round -> ChimpRound(round) }
}

@Composable
private fun ChimpRound(round: RoundScope) {
    val colors = LocalAppColors.current
    val ctx = round.ctx
    val count = round.level
    // positions[k] — клетка, где стоит число k + 1.
    val positions = remember { (0 until CHIMP_COLS * CHIMP_ROWS).shuffled().take(count) }
    var next by remember { mutableIntStateOf(1) }
    var wrongCell by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) { round.hint("ЗАПОМНИ И НАЖМИ 1") }

    SquareGrid(CHIMP_ROWS, CHIMP_COLS, Modifier.fillMaxSize(), spacing = 8.dp) { index, size ->
        val number = positions.indexOf(index) + 1
        if (number == 0) return@SquareGrid
        val hidden = next > 1 && !round.finished
        when {
            number < next && wrongCell == -1 -> Unit // уже нажато
            hidden -> Cell(
                Modifier.press {
                    if (round.finished) return@press
                    if (number == next) {
                        ctx.feedback.tone(number)
                        next++
                        round.hint("ДАЛЬШЕ ПО ПОРЯДКУ")
                        if (next > count) round.finish(true)
                    } else {
                        wrongCell = index
                        round.finish(false)
                    }
                },
                fill = colors.ink,
            )
            else -> Cell(
                Modifier.press(enabled = number == 1 && next == 1) {
                    if (round.finished) return@press
                    ctx.feedback.tone(1)
                    next = 2
                    round.hint("ДАЛЬШЕ ПО ПОРЯДКУ")
                    if (count == 1) round.finish(true)
                },
                fill = if (index == wrongCell) colors.wrong else Color.Transparent,
            ) {
                val textColor = if (index == wrongCell) colors.paper else colors.ink
                Txt(number.toString(), Type.button.copy(fontSize = (size.value * 0.42f).sp), color = textColor)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 7. Цвета: последовательность цветов, каждый уровень — новая и на один цвет длиннее.
// ---------------------------------------------------------------------------------------------

@Composable
fun ColorsGame(ctx: GameContext) {
    LevelGame(ctx, startLevel = 3) { round -> ColorsRound(round) }
}

@Composable
private fun ColorsRound(round: RoundScope) {
    val colors = LocalAppColors.current
    val ctx = round.ctx
    val palette = colors.palette
    val sequence = remember {
        val list = mutableListOf<Int>()
        repeat(round.level) { list += nextCell(list.lastOrNull(), palette.size) }
        list.toList()
    }
    var shown by remember { mutableIntStateOf(-1) }
    var inputting by remember { mutableStateOf(false) }
    val entered = remember { mutableStateListOf<Int>() }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        round.hint("ЗАПОМИНАЙ")
        delay(ctx.showMs(600))
        for (c in sequence) {
            shown = c
            ctx.feedback.silentTone(c)
            delay(ctx.showMs(650))
            shown = -1
            delay(ctx.showMs(250))
        }
        round.hint("ПОВТОРИ ПОРЯДОК")
        inputting = true
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (!inputting && !round.finished) {
                val side = min(min(maxWidth, maxHeight) * 0.8f, 220.dp)
                Box(
                    Modifier
                        .size(side)
                        .background(if (shown >= 0) palette[shown] else Color.Transparent)
                        .border(Dimens.border, colors.ink)
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ColorStrip(sequence.size, entered)
                    if (failed) {
                        Spacer(Modifier.height(20.dp))
                        Txt("ПРАВИЛЬНО:", Type.label, color = colors.inkSoft)
                        Spacer(Modifier.height(8.dp))
                        ColorStrip(sequence.size, sequence)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().height(150.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in palette.indices.chunked(3)) {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (c in row) {
                        InkButton(
                            "",
                            Modifier.weight(1f).fillMaxHeight(),
                            enabled = inputting,
                            fireOnPress = true,
                            fill = palette[c],
                        ) {
                            if (round.finished) return@InkButton
                            entered += c
                            ctx.feedback.tone(c)
                            if (sequence[entered.size - 1] != c) {
                                failed = true
                                inputting = false
                                round.finish(false)
                            } else if (entered.size == sequence.size) {
                                inputting = false
                                round.finish(true)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Ряд квадратиков: заполненные цвета и пустые места под оставшиеся. */
@Composable
private fun ColorStrip(length: Int, filled: List<Int>) {
    val colors = LocalAppColors.current
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val side = min((maxWidth - 6.dp * (length - 1)) / length, 40.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(length) { i ->
                Box(
                    Modifier
                        .size(side)
                        .background(filled.getOrNull(i)?.let { colors.palette[it] } ?: Color.Transparent)
                        .border(Dimens.thinBorder, colors.ink)
                )
            }
        }
    }
}
