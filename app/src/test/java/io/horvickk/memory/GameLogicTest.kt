package io.horvickk.memory

import io.horvickk.memory.game.exercises.ALL_GLYPHS
import io.horvickk.memory.game.exercises.EXERCISES
import io.horvickk.memory.game.exercises.NBACK
import io.horvickk.memory.game.exercises.NBACK_TRIALS
import io.horvickk.memory.game.exercises.Words
import io.horvickk.memory.game.exercises.alteredGlyph
import io.horvickk.memory.game.exercises.distinctGlyphs
import io.horvickk.memory.game.exercises.matrixSide
import io.horvickk.memory.game.exercises.missingOptions
import io.horvickk.memory.game.exercises.nBackSequence
import io.horvickk.memory.game.exercises.nextCell
import io.horvickk.memory.game.exercises.nextVerbalWord
import io.horvickk.memory.game.exercises.randomDigits
import io.horvickk.memory.game.plural
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameLogicTest {

    @Test
    fun pluralFollowsRussianRules() {
        val forms = arrayOf("цифра", "цифры", "цифр")
        assertEquals("1 цифра", plural(1, forms[0], forms[1], forms[2]))
        assertEquals("3 цифры", plural(3, forms[0], forms[1], forms[2]))
        assertEquals("5 цифр", plural(5, forms[0], forms[1], forms[2]))
        assertEquals("11 цифр", plural(11, forms[0], forms[1], forms[2]))
        assertEquals("12 цифр", plural(12, forms[0], forms[1], forms[2]))
        assertEquals("21 цифра", plural(21, forms[0], forms[1], forms[2]))
        assertEquals("24 цифры", plural(24, forms[0], forms[1], forms[2]))
        assertEquals("0 цифр", plural(0, forms[0], forms[1], forms[2]))
    }

    @Test
    fun randomDigitsHaveNoLeadingZeroAndNoRepeats() {
        val random = Random(1)
        repeat(500) {
            val length = 1 + random.nextInt(15)
            val digits = randomDigits(length, random)
            assertEquals(length, digits.length)
            assertNotEquals('0', digits.first())
            digits.zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }
        }
    }

    @Test
    fun nextCellNeverRepeatsPrevious() {
        val random = Random(2)
        var previous: Int? = null
        repeat(1000) {
            val cell = nextCell(previous, 9, random)
            assertTrue(cell in 0 until 9)
            assertNotEquals(previous, cell)
            previous = cell
        }
    }

    @Test
    fun matrixGridAlwaysHasRoomForTargets() {
        for (cells in 1..60) {
            val side = matrixSide(cells)
            assertTrue("$cells cells in ${side}x$side", side * side >= cells * 2)
        }
    }

    @Test
    fun distinctGlyphsAreUnique() {
        val random = Random(3)
        for (count in 1..20) {
            val glyphs = distinctGlyphs(count, random)
            assertEquals(count, glyphs.size)
            assertEquals(count, glyphs.toSet().size)
        }
        // До восьми фигур формы не повторяются.
        assertEquals(8, distinctGlyphs(8, random).map { it.shape }.toSet().size)
    }

    @Test
    fun alteredGlyphDiffersInExactlyOneAttribute() {
        val random = Random(4)
        for (glyph in ALL_GLYPHS) {
            val altered = alteredGlyph(glyph, random)
            val shapeChanged = altered.shape != glyph.shape
            val colorChanged = altered.colorIndex != glyph.colorIndex
            assertTrue(shapeChanged xor colorChanged)
        }
    }

    @Test
    fun missingOptionsContainAnswerAndNoOtherShownGlyph() {
        val random = Random(5)
        repeat(200) {
            val shown = distinctGlyphs(3 + random.nextInt(10), random)
            val missing = shown.random(random)
            val options = missingOptions(missing, shown, random)
            assertEquals(4, options.size)
            assertEquals(4, options.toSet().size)
            assertTrue(missing in options)
            assertEquals(listOf(missing), options.filter { it in shown })
        }
    }

    @Test
    fun nBackSequenceHasExpectedTargets() {
        val random = Random(6)
        repeat(200) {
            val seq = nBackSequence(NBACK, NBACK_TRIALS, random)
            assertEquals(NBACK + NBACK_TRIALS, seq.size)
            val targets = (NBACK until seq.size).count { seq[it] == seq[it - NBACK] }
            assertTrue(targets in 6..8)
        }
    }

    @Test
    fun verbalWordIsNeverTheCurrentOne() {
        val random = Random(7)
        val fresh = ArrayDeque(Words.nouns)
        val seen = mutableListOf<String>()
        var current = fresh.removeFirst()
        repeat(200) {
            if (current !in seen) seen += current
            val next = nextVerbalWord(seen, fresh, current, random)
            assertNotEquals(current, next)
            current = next
        }
    }

    @Test
    fun wordBankIsCleanAndLargeEnough() {
        val words = Words.nouns
        assertEquals("duplicates: ${words.groupBy { it }.filter { it.value.size > 1 }.keys}", words.size, words.toSet().size)
        assertTrue(words.size >= 300)
        assertTrue(words.all { it.length in 2..10 && it == it.uppercase() })
        // Списки слов берут слова до 9 букв: их должно хватать на все раунды без повторов.
        assertTrue(words.count { it.length <= 9 } >= 48)
    }

    @Test
    fun exerciseIdsAreUnique() {
        assertEquals(EXERCISES.size, EXERCISES.map { it.id }.toSet().size)
    }
}
