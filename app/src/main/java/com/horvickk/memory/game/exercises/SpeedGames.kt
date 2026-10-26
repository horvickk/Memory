package com.horvickk.memory.game.exercises

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.horvickk.memory.game.GameContext
import com.horvickk.memory.ui.components.GameColumn
import com.horvickk.memory.ui.components.GlyphShape
import com.horvickk.memory.ui.components.GlyphView
import com.horvickk.memory.ui.components.HintTone
import com.horvickk.memory.ui.components.InkButton
import com.horvickk.memory.ui.components.Txt
import com.horvickk.memory.ui.theme.Dimens
import com.horvickk.memory.ui.theme.LocalAppColors
import com.horvickk.memory.ui.theme.Type
import kotlinx.coroutines.delay
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// 13. Совпадение: совпадает ли фигура с предыдущей? 45 секунд на скорость.
// ---------------------------------------------------------------------------------------------

internal const val SPEED_MATCH_MS = 45_000L
private val SPEED_SHAPES = listOf(
    GlyphShape.CIRCLE, GlyphShape.SQUARE, GlyphShape.TRIANGLE,
    GlyphShape.DIAMOND, GlyphShape.STAR, GlyphShape.PLUS,
)

private data class Card(val index: Int, val shape: GlyphShape)

private fun formatSeconds(ms: Long): String {
    val seconds = (ms + 999) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
fun SpeedMatchGame(ctx: GameContext) {
    val colors = LocalAppColors.current
    var card by remember { mutableStateOf(Card(0, SPEED_SHAPES.random())) }
    var previous by remember { mutableStateOf<GlyphShape?>(null) }
    var started by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }
    var correct by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf("ЗАПОМНИ ФИГУРУ И НАЖМИ «ДАЛЕЕ»") }
    var tone by remember { mutableStateOf(HintTone.NORMAL) }

    fun score() = (correct - wrong).coerceAtLeast(0)

    fun nextCard() {
        previous = card.shape
        val shape = if (Random.nextBoolean()) card.shape else (SPEED_SHAPES - card.shape).random()
        card = Card(card.index + 1, shape)
    }

    fun answer(same: Boolean) {
        if (!started || over) return
        if (same == (card.shape == previous)) {
            correct++
            ctx.feedback.tap()
            tone = HintTone.CORRECT
        } else {
            wrong++
            ctx.feedback.wrong()
            tone = HintTone.WRONG
        }
        hint = "ОЧКИ: ${score()}"
        nextCard()
    }

    LaunchedEffect(started) {
        if (!started) {
            ctx.status(formatSeconds(SPEED_MATCH_MS))
            return@LaunchedEffect
        }
        val end = SystemClock.elapsedRealtime() + SPEED_MATCH_MS
        while (true) {
            val left = end - SystemClock.elapsedRealtime()
            ctx.status(formatSeconds(left.coerceAtLeast(0)))
            if (left <= 0) break
            delay(100)
        }
        over = true
        tone = HintTone.NORMAL
        hint = "ВРЕМЯ ВЫШЛО"
        delay(1200)
        ctx.finish(score())
    }

    GameColumn(hint, tone = tone) {
        Column(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val side = min(min(maxWidth, maxHeight) * 0.75f, 200.dp)
                AnimatedContent(
                    targetState = card,
                    transitionSpec = {
                        (slideInHorizontally(tween(160)) { it / 2 } + fadeIn(tween(160))) togetherWith
                            (slideOutHorizontally(tween(160)) { -it / 2 } + fadeOut(tween(120)))
                    },
                    label = "card",
                ) { shown ->
                    Box(Modifier.size(side).border(Dimens.border, colors.ink)) {
                        GlyphView(shown.shape, colors.ink, Modifier.fillMaxSize().padding(side * 0.14f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!started) {
                    InkButton("ДАЛЕЕ", Modifier.fillMaxWidth().fillMaxHeight()) {
                        started = true
                        hint = "СОВПАДАЕТ С ПРЕДЫДУЩЕЙ?"
                        nextCard()
                    }
                } else {
                    InkButton("НЕТ", Modifier.weight(1f).fillMaxHeight(), enabled = !over, fireOnPress = true) { answer(false) }
                    InkButton("ДА", Modifier.weight(1f).fillMaxHeight(), enabled = !over, fireOnPress = true) { answer(true) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 14. Два назад (2-back): совпадает ли буква с той, что была два шага назад.
// ---------------------------------------------------------------------------------------------

internal const val NBACK = 2
internal const val NBACK_TRIALS = 20
private const val NBACK_LETTERS = "БВГДКЛМНПРСТ"

/** Последовательность из n + trials букв, в которой 6–8 совпадений «n назад». */
internal fun nBackSequence(n: Int, trials: Int, random: Random = Random): List<Char> {
    while (true) {
        val seq = MutableList(n) { NBACK_LETTERS.random(random) }
        var targets = 0
        repeat(trials) {
            val back = seq[seq.size - n]
            if (random.nextFloat() < 0.33f) {
                seq += back
                targets++
            } else {
                seq += NBACK_LETTERS.filter { it != back }.random(random)
            }
        }
        if (targets in 6..8) return seq
    }
}

@Composable
fun TwoBackGame(ctx: GameContext) {
    val colors = LocalAppColors.current
    val letters = remember { nBackSequence(NBACK, NBACK_TRIALS) }
    var index by remember { mutableIntStateOf(-1) }
    var visible by remember { mutableStateOf(false) }
    var responded by remember { mutableStateOf(false) }
    var hits by remember { mutableIntStateOf(0) }
    var correctRejections by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf("ЗАПОМИНАЙ БУКВЫ") }
    var tone by remember { mutableStateOf(HintTone.NORMAL) }
    // Номер пробы, до конца которой держится текущая подсказка.
    var hintUntil by remember { mutableIntStateOf(0) }

    fun setHint(text: String, newTone: HintTone, until: Int) {
        hint = text
        tone = newTone
        hintUntil = until
    }

    LaunchedEffect(Unit) {
        ctx.status("0/$NBACK_TRIALS")
        delay(800)
        for (i in letters.indices) {
            index = i
            responded = false
            visible = true
            if (i >= NBACK) {
                ctx.status("${i - NBACK + 1}/$NBACK_TRIALS")
                if (i > hintUntil) setHint("СОВПАДАЕТ С БУКВОЙ $NBACK НАЗАД?", HintTone.NORMAL, i)
            }
            delay(ctx.showMs(1300))
            visible = false
            delay(ctx.showMs(900))
            if (i >= NBACK && !responded) {
                if (letters[i] == letters[i - NBACK]) {
                    ctx.feedback.wrong()
                    setHint("ПРОПУЩЕНО СОВПАДЕНИЕ", HintTone.WRONG, i + 1)
                } else {
                    correctRejections++
                }
            }
        }
        visible = false
        delay(500)
        ctx.finish((hits + correctRejections) * 100 / NBACK_TRIALS)
    }

    fun press() {
        if (index < NBACK || responded) return
        responded = true
        if (letters[index] == letters[index - NBACK]) {
            hits++
            ctx.feedback.correct()
            setHint("ВЕРНО", HintTone.CORRECT, index)
        } else {
            ctx.feedback.wrong()
            setHint("НЕ СОВПАДАЕТ", HintTone.WRONG, index)
        }
    }

    GameColumn(hint, tone = tone) {
        Column(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val side = min(min(maxWidth, maxHeight) * 0.75f, 200.dp)
                Box(Modifier.size(side).border(Dimens.border, colors.ink), contentAlignment = Alignment.Center) {
                    if (visible && index >= 0) {
                        Txt(letters[index].toString(), Type.huge.copy(fontSize = (side.value * 0.5f).sp), align = TextAlign.Center)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            InkButton(
                "СОВПАДАЕТ",
                Modifier.fillMaxWidth().height(72.dp),
                enabled = index >= NBACK && !responded,
                fireOnPress = true,
            ) { press() }
        }
    }
}
