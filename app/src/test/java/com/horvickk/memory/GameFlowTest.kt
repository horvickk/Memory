package com.horvickk.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.horvickk.memory.audio.Feedback
import com.horvickk.memory.game.exercises.EXERCISES
import com.horvickk.memory.game.exercises.Words
import com.horvickk.memory.ui.screens.ExerciseScreen
import com.horvickk.memory.ui.theme.LocalAppColors
import com.horvickk.memory.ui.theme.MemoryTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xhdpi")
class GameFlowTest {
    @get:Rule
    val rule = createComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<MemoryApp>()

    private fun open(id: String) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MemoryTheme(false) {
                Box(Modifier.fillMaxSize().background(LocalAppColors.current.paper)) {
                    val view = LocalView.current
                    val feedback = remember { Feedback(app.sounds, app.settings, view) }
                    ExerciseScreen(EXERCISES.first { it.id == id }, app.results, app.settings, feedback, onClose = {})
                }
            }
        }
        click("НАЧАТЬ")
    }

    private fun click(text: String) {
        rule.onNodeWithText(text).performClick()
        advance(64)
    }

    private fun advance(ms: Long) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(ms)
        // advanceTimeBy не всегда успевает выполнить отложенную рекомпозицию — добиваем кадрами.
        repeat(3) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
    }

    private fun exists(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun allTexts(): List<String> {
        val all = mutableListOf<String>()
        fun walk(node: SemanticsNode) {
            node.config.getOrNull(SemanticsProperties.Text)?.forEach { all += it.text }
            node.children.forEach(::walk)
        }
        walk(rule.onRoot().fetchSemanticsNode())
        return all
    }

    /** Показанное число — единственный текст только из цифр. */
    private fun shownNumber(): String = allTexts().single { it.length >= 3 && it.all(Char::isDigit) }

    private fun currentWord(): String = allTexts().single { it in Words.nouns }

    private fun type(digits: String) {
        for (d in digits) click(d.toString())
        click("ОК")
    }

    @Test
    fun numberGameLevelsUpAndFinishesAfterThreeMistakes() {
        open("number")
        advance(500)
        val first = shownNumber()
        assertEquals(3, first.length)
        advance(3000)
        type(first)
        assertTrue(exists("ВЕРНО!"))
        advance(1000)
        assertTrue(exists("УР. 4"))

        // Три ошибки подряд.
        repeat(3) { attempt ->
            advance(5000)
            type("0")
            assertTrue(exists(if (attempt < 2) "ОШИБКА · НАЖМИ, ЧТОБЫ ПРОДОЛЖИТЬ" else "ИГРА ОКОНЧЕНА · НАЖМИ"))
            advance(600)
            rule.onRoot().performClick()
            advance(300)
        }
        assertTrue(exists("РЕЗУЛЬТАТ"))
        assertTrue(exists("3 ЦИФРЫ"))
        assertEquals(listOf(3), app.results.of("number").map { it.score })
    }

    @Test
    fun verbalGameCountsAnswersAndEndsOnThirdMistake() {
        open("verbal")
        advance(100)
        val seen = mutableSetOf<String>()
        seen += currentWord()
        click("НОВОЕ") // первое слово всегда новое
        assertTrue(exists("СЧЁТ 1"))
        // Три раза отвечаем неверно.
        repeat(3) {
            val word = currentWord()
            click(if (word in seen) "НОВОЕ" else "БЫЛО")
            seen += word
            advance(1500)
        }
        assertTrue(exists("ИГРА ОКОНЧЕНА · НАЖМИ"))
        rule.onRoot().performClick()
        advance(300)
        assertTrue(exists("РЕЗУЛЬТАТ"))
        assertTrue(exists("1 СЛОВО"))
        assertEquals(listOf(1), app.results.of("verbal").map { it.score })
    }

    @Test
    fun everyExerciseRunsWithoutCrashing() {
        rule.mainClock.autoAdvance = false
        var index by mutableIntStateOf(0)
        rule.setContent {
            MemoryTheme(false) {
                val view = LocalView.current
                val feedback = remember { Feedback(app.sounds, app.settings, view) }
                key(index) { ExerciseScreen(EXERCISES[index], app.results, app.settings, feedback, onClose = {}) }
            }
        }
        for (i in EXERCISES.indices) {
            rule.runOnUiThread { index = i }
            advance(100)
            click("НАЧАТЬ")
            // Прогоняем показ и ждём фазу ответа; нажимаем по центру поля, чтобы задеть игровые элементы.
            repeat(4) {
                advance(5000)
                rule.onRoot().performClick()
            }
            advance(1000)
        }
    }
}
