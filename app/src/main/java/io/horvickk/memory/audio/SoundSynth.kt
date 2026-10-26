package io.horvickk.memory.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

enum class Sound {
    TONE_0, TONE_1, TONE_2, TONE_3, TONE_4, TONE_5, TONE_6, TONE_7, TONE_8,
    TAP, BLIP, CORRECT, WRONG, FINISH;

    companion object {
        private val tones = listOf(TONE_0, TONE_1, TONE_2, TONE_3, TONE_4, TONE_5, TONE_6, TONE_7, TONE_8)
        fun tone(index: Int): Sound = tones[Math.floorMod(index, tones.size)]
    }
}

/**
 * Синтез звуков в духе мягкой маримбы: почти чистый синус с едва заметным обертоном,
 * плавной атакой и плавным хвостом — без щелчков в начале и в конце.
 */
internal object SoundSynth {
    /** Пентатоника до мажор (G4–D6): любые ноты созвучны друг другу и сигналу «верно». */
    private val SCALE = floatArrayOf(392f, 440f, 523.25f, 587.33f, 659.25f, 783.99f, 880f, 1046.5f, 1174.66f)

    private const val RELEASE_MS = 25f

    private class Note(val freq: Float, val offsetMs: Int, val endFreq: Float = freq)

    fun render(sound: Sound, sampleRate: Int): FloatArray = when (sound) {
        Sound.TAP -> chord(sampleRate, listOf(Note(520f, 0)), lengthMs = 70, decayMs = 16f, gain = 0.16f, attackMs = 4f)
        Sound.BLIP -> chord(sampleRate, listOf(Note(659.25f, 0)), lengthMs = 240, decayMs = 70f, gain = 0.22f, attackMs = 8f)
        Sound.CORRECT -> chord(
            sampleRate,
            listOf(Note(587.33f, 0), Note(783.99f, 90)), // ре — соль: мягкое «динь-динь»
            lengthMs = 600,
            decayMs = 160f,
            gain = 0.22f,
        )
        Sound.WRONG -> chord(
            sampleRate,
            listOf(Note(330f, 0, endFreq = 262f)), // низкий звук, плавно уходящий вниз
            lengthMs = 400,
            decayMs = 120f,
            gain = 0.32f,
            overtone = 0.15f,
        )
        Sound.FINISH -> chord(
            sampleRate,
            listOf(Note(523.25f, 0), Note(659.25f, 100), Note(783.99f, 200), Note(1046.5f, 300)),
            lengthMs = 900,
            decayMs = 260f,
            gain = 0.19f,
        )
        else -> chord(sampleRate, listOf(Note(SCALE[sound.ordinal], 0)), lengthMs = 480, decayMs = 160f, gain = 0.3f)
    }

    /** Ноты, звучащие внахлёст: каждая со своим затуханием, общий хвост плавно уходит в ноль. */
    private fun chord(
        sampleRate: Int,
        notes: List<Note>,
        lengthMs: Int,
        decayMs: Float,
        gain: Float,
        overtone: Float = 0.04f,
        // Плавная атака убирает «щелчок» в начале — звук мягче.
        attackMs: Float = 14f,
    ): FloatArray {
        val noteLength = sampleRate * lengthMs / 1000
        val total = notes.maxOf { sampleRate * it.offsetMs / 1000 } + noteLength
        val out = FloatArray(total)
        for (note in notes) {
            val start = sampleRate * note.offsetMs / 1000
            val samples = note(sampleRate, note, noteLength, decayMs, overtone, attackMs)
            for (i in samples.indices) out[start + i] += samples[i] * gain
        }
        return out
    }

    private fun note(sampleRate: Int, note: Note, length: Int, decayMs: Float, overtone: Float, attackMs: Float): FloatArray {
        val attack = sampleRate * attackMs / 1000f
        val release = sampleRate * RELEASE_MS / 1000f
        val tau = sampleRate * decayMs / 1000f
        var phase = 0.0
        return FloatArray(length) { i ->
            val progress = i.toFloat() / length
            val freq = note.freq + (note.endFreq - note.freq) * progress
            phase += 2 * PI * freq / sampleRate
            // Обертон гаснет втрое быстрее основного тона: короткая «деревянная» атака, мягкий хвост.
            val wave = sin(phase) + overtone * exp(-3.0 * i / tau) * sin(2 * phase)
            val attackEnv = if (i < attack) 0.5 - 0.5 * cos(PI * i / attack) else 1.0
            val releaseEnv = if (i > length - release) (length - i) / release.toDouble() else 1.0
            (wave / (1 + overtone) * exp(-i / tau.toDouble()) * attackEnv * releaseEnv).toFloat()
        }
    }
}
