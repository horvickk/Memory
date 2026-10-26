package com.horvickk.memory

import com.horvickk.memory.audio.Mixer
import com.horvickk.memory.audio.Sound
import com.horvickk.memory.audio.SoundSynth
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AudioTest {
    private val rate = 48_000

    @Test
    fun soundsStartAndEndSilentlyWithoutClicks() {
        for (sound in Sound.entries) {
            val data = SoundSynth.render(sound, rate)
            assertTrue("$sound is empty", data.isNotEmpty())
            assertTrue("$sound starts with a click", abs(data.first()) < 1e-3f)
            assertTrue("$sound ends with a click", data.takeLast(10).all { abs(it) < 1e-3f })
            assertTrue("$sound is too loud", data.all { abs(it) <= 0.5f })
            // Соседние отсчёты не прыгают — нет резких перепадов, которые слышны как щелчок.
            val maxStep = data.toList().zipWithNext { a, b -> abs(b - a) }.max()
            assertTrue("$sound has a jump of $maxStep", maxStep < 0.1f)
        }
    }

    @Test
    fun newSoundDoesNotCutPreviousOne() {
        val first = FloatArray(1000) { 0.1f }
        val second = FloatArray(300) { 0.05f }
        val mixer = Mixer()
        val chunk = FloatArray(200)
        val output = mutableListOf<Float>()

        mixer.add(first)
        mixer.render(chunk); output += chunk.toList()
        mixer.add(second) // второй звук начинается, пока первый ещё играет
        repeat(5) { mixer.render(chunk); output += chunk.toList() }

        val expected = FloatArray(1200) { i ->
            val a = if (i < 1000) 0.1f else 0f
            val b = if (i in 200 until 500) 0.05f else 0f
            a + b
        }
        assertArrayEquals(expected, output.toFloatArray(), 1e-6f)
        assertTrue(mixer.isIdle)
    }

    @Test
    fun loudOverlapIsSoftLimited() {
        val mixer = Mixer()
        repeat(5) { mixer.add(FloatArray(10) { 0.5f }) }
        val out = FloatArray(10)
        mixer.render(out)
        assertTrue(out.all { it in 0.6f..1f })
        assertEquals(out[0], out[9], 0f)
    }
}
