package com.horvickk.memory.game.exercises

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horvickk.memory.game.GameContext
import com.horvickk.memory.game.LevelGame
import com.horvickk.memory.game.RoundScope
import com.horvickk.memory.ui.components.NumPad
import com.horvickk.memory.ui.components.TimerBar
import com.horvickk.memory.ui.components.Txt
import com.horvickk.memory.ui.theme.LocalAppColors
import com.horvickk.memory.ui.theme.Type
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Случайная строка цифр без одинаковых соседних; первая цифра не 0. */
internal fun randomDigits(length: Int, random: Random = Random): String {
    val sb = StringBuilder()
    while (sb.length < length) {
        val d = random.nextInt(if (sb.isEmpty()) 1 else 0, 10)
        if (sb.isNotEmpty() && sb.last().digitToInt() == d) continue
        sb.append(d)
    }
    return sb.toString()
}

private fun digitsFontSize(length: Int): TextUnit = when {
    length <= 6 -> 56.sp
    length <= 9 -> 44.sp
    length <= 12 -> 34.sp
    else -> 26.sp
}

// ---------------------------------------------------------------------------------------------
// 3. Число: показать число целиком, затем ввести его.
// ---------------------------------------------------------------------------------------------

@Composable
fun NumberGame(ctx: GameContext) {
    LevelGame(ctx, startLevel = 3) { round ->
        val number = remember { randomDigits(round.level) }
        DigitsRound(
            round = round,
            expected = number,
            inputHint = "ВВЕДИ ЧИСЛО",
            reveal = "ПРАВИЛЬНО: $number",
        ) { onShown ->
            val duration = round.ctx.showMs(800L + 450L * number.length)
            LaunchedEffect(Unit) {
                round.hint("ЗАПОМНИ ЧИСЛО")
                delay(duration)
                onShown()
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Txt(number, Type.huge.copy(fontSize = digitsFontSize(number.length)), align = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                TimerBar(duration, Modifier.padding(horizontal = 24.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 4. Задом наперёд: цифры по одной, ввести в обратном порядке.
// ---------------------------------------------------------------------------------------------

@Composable
fun ReverseGame(ctx: GameContext) {
    LevelGame(ctx, startLevel = 3) { round ->
        val digits = remember { randomDigits(round.level) }
        DigitsRound(
            round = round,
            expected = digits.reversed(),
            inputHint = "ВВЕДИ В ОБРАТНОМ ПОРЯДКЕ",
            reveal = "БЫЛО: $digits\nПРАВИЛЬНО: ${digits.reversed()}",
        ) { onShown ->
            var current by remember { mutableStateOf<Char?>(null) }
            LaunchedEffect(Unit) {
                val c = round.ctx
                round.hint("ЗАПОМИНАЙ ЦИФРЫ")
                delay(c.showMs(600))
                for (d in digits) {
                    current = d
                    c.feedback.silentTone(d.digitToInt())
                    delay(c.showMs(800))
                    current = null
                    delay(c.showMs(250))
                }
                onShown()
            }
            Txt(current?.toString() ?: "", Type.huge.copy(fontSize = 96.sp), align = TextAlign.Center)
        }
    }
}

/**
 * Общая часть цифровых упражнений: показ (своя для каждой игры), затем ввод с клавиатуры
 * и сравнение. При ошибке неверные цифры подсвечиваются.
 */
@Composable
private fun DigitsRound(
    round: RoundScope,
    expected: String,
    inputHint: String,
    reveal: String,
    show: @Composable (onShown: () -> Unit) -> Unit,
) {
    val colors = LocalAppColors.current
    var inputting by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (!inputting && !round.finished) {
                show {
                    inputting = true
                    round.hint(inputHint)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val text = buildAnnotatedString {
                        if (input.isEmpty()) {
                            withStyle(SpanStyle(color = colors.inkFaint)) { append("_".repeat(expected.length)) }
                        }
                        input.forEachIndexed { i, ch ->
                            val color = if (failed && expected.getOrNull(i) != ch) colors.wrong else colors.ink
                            withStyle(SpanStyle(color = color)) { append(ch) }
                        }
                    }
                    BasicText(
                        text,
                        style = Type.huge.copy(
                            fontSize = digitsFontSize(maxOf(input.length, expected.length)),
                            textAlign = TextAlign.Center,
                        ),
                    )
                    if (failed) {
                        Spacer(Modifier.height(16.dp))
                        Txt(reveal, Type.body, color = colors.inkSoft, align = TextAlign.Center)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        NumPad(
            onDigit = { d ->
                if (input.length < expected.length + 2) {
                    round.ctx.feedback.tap()
                    input += d
                }
            },
            onErase = {
                round.ctx.feedback.tap()
                input = input.dropLast(1)
            },
            onOk = {
                if (round.finished) return@NumPad
                inputting = false
                if (input == expected) {
                    round.finish(true)
                } else {
                    failed = true
                    round.finish(false)
                }
            },
            modifier = Modifier.fillMaxWidth().height(248.dp),
            okEnabled = input.isNotEmpty(),
            enabled = inputting && !round.finished,
        )
    }
}
