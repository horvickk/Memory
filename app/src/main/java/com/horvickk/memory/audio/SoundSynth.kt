package com.horvickk.memory.audio

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
 * Синтез звуков в духе мягкой маримбы: синус с быстро гаснущим обертоном,
 * плавной атакой и плавным хвостом — без щелчков в начале и в конце.
 */
internal object SoundSynth {
    /** Пентатоника до мажор (A4–E6): любые ноты созвучны друг другу и сигналу «верно». */
    private val SCALE = floatArrayOf(440f, 523.25f, 587.33f, 659.25f, 783.99f, 880f, 1046.5f, 1174.66f, 1318.51f)

    private const val ATTACK_MS = 6f
    private const val RELEASE_MS = 25f

    private class Note(val freq: Float, val offsetMs: Int, val endFreq: Float = freq)

    fun render(sound: Sound, sampleRate: Int): FloatArray = when (sound) {
        Sound.TAP -> chord(sampleRate, listOf(Note(660f, 0)), lengthMs = 60, decayMs = 14f, gain = 0.2f, overtone = 0f)
        Sound.BLIP -> chord(sampleRate, listOf(Note(880f, 0)), lengthMs = 220, decayMs = 60f, gain = 0.26f)
        Sound.CORRECT -> chord(
            sampleRate,
            listOf(Note(659.25f, 0), Note(880f, 90)), // ми — ля: мягкое «динь-динь»
            lengthMs = 560,
            decayMs = 150f,
            gain = 0.26f,
        )
        Sound.WRONG -> chord(
            sampleRate,
            listOf(Note(330f, 0, endFreq = 262f)), // низкий звук, плавно уходящий вниз
            lengthMs = 380,
            decayMs = 120f,
            gain = 0.4f,
            overtone = 0.3f,
        )
        Sound.FINISH -> chord(
            sampleRate,
            listOf(Note(523.25f, 0), Note(659.25f, 100), Note(783.99f, 200), Note(1046.5f, 300)),
            lengthMs = 900,
            decayMs = 260f,
            gain = 0.22f,
        )
        else -> chord(sampleRate, listOf(Note(SCALE[sound.ordinal], 0)), lengthMs = 450, decayMs = 150f, gain = 0.36f)
    }

    /** Ноты, звучащие внахлёст: каждая со своим затуханием, общий хвост плавно уходит в ноль. */
    private fun chord(
        sampleRate: Int,
        notes: List<Note>,
        lengthMs: Int,
        decayMs: Float,
        gain: Float,
        overtone: Float = 0.12f,
    ): FloatArray {
        val noteLength = sampleRate * lengthMs / 1000
        val total = notes.maxOf { sampleRate * it.offsetMs / 1000 } + noteLength
        val out = FloatArray(total)
        for (note in notes) {
            val start = sampleRate * note.offsetMs / 1000
            val samples = note(sampleRate, note, noteLength, decayMs, overtone)
            for (i in samples.indices) out[start + i] += samples[i] * gain
        }
        return out
    }

    private fun note(sampleRate: Int, note: Note, length: Int, decayMs: Float, overtone: Float): FloatArray {
        val attack = sampleRate * ATTACK_MS / 1000f
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
