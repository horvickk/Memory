package io.horvickk.memory.game.exercises

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horvickk.memory.game.GameContext
import io.horvickk.memory.game.plural
import io.horvickk.memory.ui.components.GameColumn
import io.horvickk.memory.ui.components.HintTone
import io.horvickk.memory.ui.components.InkButton
import io.horvickk.memory.ui.components.TapCatcher
import io.horvickk.memory.ui.components.TimerBar
import io.horvickk.memory.ui.components.Txt
import io.horvickk.memory.ui.components.press
import io.horvickk.memory.ui.theme.Dimens
import io.horvickk.memory.ui.theme.LocalAppColors
import io.horvickk.memory.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// 8. Было или нет: слово встречалось раньше? Три ошибки — конец.
// ---------------------------------------------------------------------------------------------

private const val VERBAL_LIVES = 3

/** Следующее слово: с вероятностью ~45% — уже встречавшееся (но не только что показанное). */
internal fun nextVerbalWord(
    seen: List<String>,
    fresh: ArrayDeque<String>,
    current: String?,
    random: Random = Random,
): String {
    val candidates = seen.filter { it != current }
    return if (candidates.size >= 2 && (random.nextFloat() < 0.45f || fresh.isEmpty())) {
        candidates.random(random)
    } else {
        fresh.removeFirst()
    }
}

@Composable
fun VerbalGame(ctx: GameContext) {
    val fresh = remember { ArrayDeque(Words.nouns.shuffled()) }
    val seen = remember { mutableListOf<String>() }
    var word by remember { mutableStateOf(fresh.removeFirst()) }
    var wordIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(VERBAL_LIVES) }
    var hint by remember { mutableStateOf("СЛОВО УЖЕ БЫЛО?") }
    var tone by remember { mutableStateOf(HintTone.NORMAL) }
    var locked by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(score, lives) { ctx.status("СЧЁТ $score", lives, VERBAL_LIVES) }

    fun showNext() {
        val previous = word
        if (previous !in seen) seen += previous
        word = nextVerbalWord(seen, fresh, previous)
        wordIndex++
    }

    fun answer(saidSeen: Boolean) {
        if (locked) return
        val wasSeen = word in seen
        if (saidSeen == wasSeen) {
            score++
            ctx.feedback.blip()
            hint = "ВЕРНО"
            tone = HintTone.CORRECT
            showNext()
            return
        }
        ctx.feedback.wrong()
        lives--
        tone = HintTone.WRONG
        locked = true
        if (lives == 0) {
            hint = "ИГРА ОКОНЧЕНА · НАЖМИ"
            scope.launch {
                delay(400)
                gameOver = true
            }
        } else {
            hint = if (wasSeen) "ЭТО СЛОВО УЖЕ БЫЛО" else "ЭТО СЛОВО НОВОЕ"
            scope.launch {
                delay(1100)
                tone = HintTone.NORMAL
                hint = "СЛОВО УЖЕ БЫЛО?"
                showNext()
                locked = false
            }
        }
    }

    GameColumn(hint, tone = tone) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                key(wordIndex) {
                    Txt(word, Type.huge.copy(fontSize = 36.sp), align = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InkButton("БЫЛО", Modifier.weight(1f).fillMaxHeight(), enabled = !locked) { answer(true) }
                InkButton("НОВОЕ", Modifier.weight(1f).fillMaxHeight(), enabled = !locked) { answer(false) }
            }
        }
        if (gameOver) TapCatcher { ctx.finish(score) }
    }
}

// ---------------------------------------------------------------------------------------------
// 11. Список слов: запомнить слова и найти их среди других. Три раунда.
// ---------------------------------------------------------------------------------------------

internal val WORD_LIST_SIZES = listOf(6, 8, 10)

@Composable
fun WordListGame(ctx: GameContext) {
    var roundIndex by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    val pool = remember { Words.nouns.filter { it.length <= 9 }.shuffled() }

    LaunchedEffect(roundIndex) { ctx.status("${roundIndex + 1}/${WORD_LIST_SIZES.size}") }

    key(roundIndex) {
        val size = WORD_LIST_SIZES[roundIndex]
        val offset = WORD_LIST_SIZES.take(roundIndex).sumOf { it * 2 }
        val words = remember { pool.subList(offset, offset + size * 2) }
        WordListRound(ctx, words.take(size), words.shuffled(), isLast = roundIndex == WORD_LIST_SIZES.lastIndex) { got ->
            total += got
            if (roundIndex < WORD_LIST_SIZES.lastIndex) roundIndex++ else ctx.finish(total)
        }
    }
}

private enum class ListPhase { STUDY, RECALL, REVIEW }

@Composable
private fun WordListRound(
    ctx: GameContext,
    targets: List<String>,
    options: List<String>,
    isLast: Boolean,
    onDone: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    var phase by remember { mutableStateOf(ListPhase.STUDY) }
    val selected = remember { mutableStateListOf<String>() }
    val studyDuration = ctx.showMs(1500L * targets.size)
    val hits = selected.count { it in targets }

    LaunchedEffect(Unit) {
        delay(studyDuration)
        if (phase == ListPhase.STUDY) phase = ListPhase.RECALL
    }

    val hint = when (phase) {
        ListPhase.STUDY -> "ЗАПОМНИ ${targets.size} СЛОВ"
        ListPhase.RECALL -> "ВЫБЕРИ ${targets.size} СЛОВ · ВЫБРАНО ${selected.size}"
        ListPhase.REVIEW -> "УГАДАНО: $hits ИЗ ${targets.size}"
    }
    GameColumn(hint, tone = if (phase == ListPhase.REVIEW) HintTone.CORRECT else HintTone.NORMAL) {
        Column(Modifier.fillMaxSize()) {
            if (phase == ListPhase.STUDY) {
                TimerBar(studyDuration)
                Spacer(Modifier.height(12.dp))
                WordColumns(targets, Modifier.weight(1f)) { word ->
                    Txt(word, Type.button, align = TextAlign.Center, maxLines = 1)
                }
            } else {
                WordColumns(options, Modifier.weight(1f)) { word ->
                    val isSelected = word in selected
                    val isTarget = word in targets
                    val (fill, textColor, border) = when {
                        phase == ListPhase.RECALL && isSelected -> Triple(colors.ink, colors.paper, colors.ink)
                        phase == ListPhase.RECALL -> Triple(Color.Transparent, colors.ink, colors.ink)
                        isSelected && isTarget -> Triple(colors.correct, colors.paper, colors.correct)
                        isSelected -> Triple(colors.wrong, colors.paper, colors.wrong)
                        isTarget -> Triple(Color.Transparent, colors.correct, colors.correct)
                        else -> Triple(Color.Transparent, colors.inkSoft, colors.inkFaint)
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(fill)
                            .border(Dimens.thinBorder, border)
                            .press(enabled = phase == ListPhase.RECALL) {
                                ctx.feedback.tap()
                                if (isSelected) selected.remove(word)
                                else if (selected.size < targets.size) selected += word
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Txt(word, Type.label.copy(fontSize = 15.sp), color = textColor, align = TextAlign.Center, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val buttonText = when (phase) {
                ListPhase.STUDY -> "ЗАПОМНИЛ"
                ListPhase.RECALL -> "ПРОВЕРИТЬ"
                ListPhase.REVIEW -> if (isLast) "РЕЗУЛЬТАТ" else "ДАЛЕЕ"
            }
            InkButton(
                buttonText,
                Modifier.fillMaxWidth().height(56.dp),
                enabled = phase != ListPhase.RECALL || selected.size == targets.size,
            ) {
                when (phase) {
                    ListPhase.STUDY -> phase = ListPhase.RECALL
                    ListPhase.RECALL -> {
                        phase = ListPhase.REVIEW
                        if (hits == targets.size) ctx.feedback.correct() else ctx.feedback.tap()
                    }
                    ListPhase.REVIEW -> onDone(hits)
                }
            }
        }
    }
}

/** Слова в два столбца; строки делят высоту поровну. */
@Composable
private fun WordColumns(words: List<String>, modifier: Modifier, item: @Composable (String) -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
        for (row in words.chunked(2)) {
            Row(
                Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 30.dp, max = 52.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (word in row) {
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { item(word) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 12. Пары слов: запомнить пары, затем к каждому слову выбрать его пару.
// ---------------------------------------------------------------------------------------------

internal val PAIRED_SIZES = listOf(4, 5, 6)

@Composable
fun PairedWordsGame(ctx: GameContext) {
    var roundIndex by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    val pool = remember { Words.nouns.filter { it.length <= 9 }.shuffled() }

    LaunchedEffect(roundIndex) { ctx.status("${roundIndex + 1}/${PAIRED_SIZES.size}") }

    key(roundIndex) {
        val size = PAIRED_SIZES[roundIndex]
        val offset = PAIRED_SIZES.take(roundIndex).sumOf { it * 2 }
        val pairs = remember { pool.subList(offset, offset + size * 2).chunked(2).map { it[0] to it[1] } }
        PairedRound(ctx, pairs) { got ->
            total += got
            if (roundIndex < PAIRED_SIZES.lastIndex) roundIndex++ else ctx.finish(total)
        }
    }
}

@Composable
private fun PairedRound(ctx: GameContext, pairs: List<Pair<String, String>>, onDone: (Int) -> Unit) {
    val colors = LocalAppColors.current
    var studying by remember { mutableStateOf(true) }
    val questions = remember { pairs.shuffled() }
    val optionsList = remember {
        questions.map { (_, right) -> (listOf(right) + (pairs.map { it.second } - right).shuffled().take(3)).shuffled() }
    }
    var step by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val studyDuration = ctx.showMs(2500L * pairs.size)

    LaunchedEffect(Unit) {
        delay(studyDuration)
        studying = false
    }

    if (studying) {
        GameColumn("ЗАПОМНИ ${plural(pairs.size, "ПАРУ", "ПАРЫ", "ПАР")}") {
            Column(Modifier.fillMaxSize()) {
                TimerBar(studyDuration)
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                ) {
                    for ((left, right) in pairs) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Txt(left, Type.button, Modifier.weight(1f), align = TextAlign.End, maxLines = 1)
                            Txt("—", Type.button, Modifier.padding(horizontal = 12.dp))
                            Txt(right, Type.button, Modifier.weight(1f), maxLines = 1)
                        }
                    }
                }
                InkButton("ЗАПОМНИЛ", Modifier.fillMaxWidth().height(56.dp)) { studying = false }
            }
        }
        return
    }

    val (question, answer) = questions[step]
    val hint = "ВЫБЕРИ ПАРУ · ${step + 1} ИЗ ${questions.size}"
    GameColumn(hint) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Txt(question, Type.huge.copy(fontSize = 34.sp), align = TextAlign.Center)
            }
            val options = optionsList[step]
            Column(Modifier.fillMaxWidth().height(150.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in options.chunked(2)) {
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (option in row) {
                            val fill = when {
                                picked == null -> null
                                option == answer -> colors.correct
                                option == picked -> colors.wrong
                                else -> null
                            }
                            InkButton(
                                option,
                                Modifier.weight(1f).fillMaxHeight(),
                                fill = fill,
                                content = { c ->
                                    val textColor = if (fill != null) colors.paper else c
                                    Txt(option, Type.label.copy(fontSize = 15.sp), color = textColor, align = TextAlign.Center, maxLines = 1)
                                },
                            ) {
                                if (picked != null) return@InkButton
                                picked = option
                                if (option == answer) {
                                    score++
                                    ctx.feedback.correct()
                                } else {
                                    ctx.feedback.wrong()
                                }
                                scope.launch {
                                    delay(if (option == answer) 600 else 1200)
                                    picked = null
                                    if (step < questions.lastIndex) step++ else onDone(score)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
