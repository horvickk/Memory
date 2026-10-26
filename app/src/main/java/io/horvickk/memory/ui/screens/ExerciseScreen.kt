package io.horvickk.memory.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horvickk.memory.audio.Feedback
import io.horvickk.memory.data.ResultsStore
import io.horvickk.memory.data.SettingsStore
import io.horvickk.memory.game.Exercise
import io.horvickk.memory.game.GameContext
import io.horvickk.memory.game.GameStatus
import io.horvickk.memory.ui.components.GameMessage
import io.horvickk.memory.ui.components.IconKind
import io.horvickk.memory.ui.components.IconTap
import io.horvickk.memory.ui.components.StartTopBar
import io.horvickk.memory.ui.components.StatusView
import io.horvickk.memory.ui.components.Txt
import io.horvickk.memory.ui.components.inkBorder
import io.horvickk.memory.ui.components.tap
import io.horvickk.memory.ui.theme.Dimens
import io.horvickk.memory.ui.theme.LocalAppColors
import io.horvickk.memory.ui.theme.Type

private sealed interface Phase {
    data object Intro : Phase
    data object Playing : Phase
    data class Done(val score: Int, val record: Boolean, val previousBest: Int?) : Phase
}

@Composable
fun ExerciseScreen(
    exercise: Exercise,
    results: ResultsStore,
    settings: SettingsStore,
    feedback: Feedback,
    onClose: () -> Unit,
) {
    var phase by remember { mutableStateOf<Phase>(Phase.Intro) }
    var session by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf(GameStatus()) }

    // Экран не гаснет, пока открыто упражнение.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    fun start() {
        status = GameStatus()
        session++
        phase = Phase.Playing
    }

    Column(Modifier.fillMaxSize()) {
        StartTopBar(exercise.title, IconKind.CLOSE, onClose) {
            Box(Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                if (phase == Phase.Playing) StatusView(status.text, status.lives, status.maxLives)
            }
            IconTap(IconKind.RESTART, "Начать заново") {
                status = GameStatus()
                phase = Phase.Intro
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = Dimens.screenPadding, end = Dimens.screenPadding, bottom = Dimens.screenPadding),
        ) {
            Txt(
                exercise.rules,
                Type.body,
                Modifier
                    .fillMaxWidth()
                    .inkBorder()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .inkBorder(),
            ) {
                when (val current = phase) {
                    Phase.Intro -> {
                        val best = exercise.best(results.of(exercise.id))
                        GameMessage(
                            title = "НАЧАТЬ",
                            subtitle = best?.let { "ЛУЧШИЙ РЕЗУЛЬТАТ: ${exercise.formatScore(it)}" },
                            onClick = ::start,
                        )
                    }

                    Phase.Playing -> key(session) {
                        val ctx = remember {
                            val thisSession = session
                            GameContext(
                                feedback = feedback,
                                speedFactor = settings.speed.factor,
                                onStatus = { status = it },
                                onFinish = { score ->
                                    if (phase == Phase.Playing && session == thisSession) {
                                        val previousBest = exercise.best(results.of(exercise.id))
                                        val record = previousBest == null || exercise.isBetter(score, previousBest)
                                        results.add(exercise.id, score)
                                        feedback.finish()
                                        phase = Phase.Done(score, record, previousBest)
                                    }
                                },
                            )
                        }
                        exercise.play(ctx)
                    }

                    is Phase.Done -> ResultView(exercise, current, onAgain = ::start)
                }
            }
        }
    }
}

@Composable
private fun ResultView(exercise: Exercise, done: Phase.Done, onAgain: () -> Unit) {
    val colors = LocalAppColors.current
    Column(
        Modifier
            .fillMaxSize()
            .tap(onClick = onAgain)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Txt("РЕЗУЛЬТАТ", Type.label, color = colors.inkSoft)
        Spacer(Modifier.height(12.dp))
        Txt(exercise.formatScore(done.score), Type.huge.copy(fontSize = 44.sp), align = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        when {
            done.record && done.previousBest != null -> Txt("НОВЫЙ РЕКОРД!", Type.button, color = colors.correct)
            done.previousBest != null -> Txt(
                "ЛУЧШИЙ: ${exercise.formatScore(done.previousBest)}",
                Type.body,
                color = colors.inkSoft,
            )
        }
        Spacer(Modifier.height(40.dp))
        Txt("НАЖМИ, ЧТОБЫ СЫГРАТЬ ЕЩЁ", Type.label, color = colors.inkSoft)
    }
}
