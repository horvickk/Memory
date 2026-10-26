package com.horvickk.memory.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.horvickk.memory.ui.components.GameColumn
import com.horvickk.memory.ui.components.HintTone
import com.horvickk.memory.ui.components.TapCatcher
import kotlinx.coroutines.delay

/** Один раунд уровневой игры. Раунд вызывает [finish] ровно один раз. */
class RoundScope(
    val level: Int,
    val ctx: GameContext,
    private val setHint: (String) -> Unit,
    private val onFinish: (Boolean) -> Unit,
) {
    var finished = false
        private set

    fun hint(text: String) {
        if (!finished) setHint(text)
    }

    fun finish(success: Boolean) {
        if (finished) return
        finished = true
        onFinish(success)
    }
}

/**
 * Общая схема для упражнений «на объём памяти»: уровень растёт после каждого верного ответа,
 * ошибка отнимает жизнь и повторяет уровень с новым заданием. Результат — лучший пройденный уровень.
 */
@Composable
fun LevelGame(
    ctx: GameContext,
    startLevel: Int,
    maxLives: Int = 3,
    round: @Composable (RoundScope) -> Unit,
) {
    var level by remember { mutableIntStateOf(startLevel) }
    var lives by remember { mutableIntStateOf(maxLives) }
    var attempt by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(HintTone.NORMAL) }
    var outcome by remember { mutableStateOf<Boolean?>(null) }
    var waitingTap by remember { mutableStateOf(false) }

    LaunchedEffect(level, lives) { ctx.status("УР. $level", lives, maxLives) }

    val scope = remember(attempt) {
        RoundScope(
            level = level,
            ctx = ctx,
            setHint = { hint = it; tone = HintTone.NORMAL },
            onFinish = { outcome = it },
        )
    }

    fun nextAttempt() {
        attempt++
        hint = ""
        tone = HintTone.NORMAL
        waitingTap = false
        outcome = null
    }

    LaunchedEffect(outcome) {
        when (outcome) {
            true -> {
                best = maxOf(best, level)
                hint = "ВЕРНО!"
                tone = HintTone.CORRECT
                // Небольшая пауза, чтобы сигнал уровня не накладывался на звук последнего нажатия.
                delay(120)
                ctx.feedback.correct()
                delay(780)
                level++
                nextAttempt()
            }
            false -> {
                ctx.feedback.wrong()
                lives--
                hint = if (lives > 0) "ОШИБКА · НАЖМИ, ЧТОБЫ ПРОДОЛЖИТЬ" else "ИГРА ОКОНЧЕНА · НАЖМИ"
                tone = HintTone.WRONG
                delay(400)
                waitingTap = true
            }
            null -> Unit
        }
    }

    GameColumn(hint, tone = tone) {
        key(attempt) { round(scope) }
        if (waitingTap) {
            TapCatcher { if (lives > 0) nextAttempt() else ctx.finish(best) }
        }
    }
}
