package com.horvickk.memory.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class Sound {
    TONE_0, TONE_1, TONE_2, TONE_3, TONE_4, TONE_5, TONE_6, TONE_7, TONE_8,
    TAP, CORRECT, WRONG, FINISH;

    companion object {
        private val tones = listOf(TONE_0, TONE_1, TONE_2, TONE_3, TONE_4, TONE_5, TONE_6, TONE_7, TONE_8)
        fun tone(index: Int): Sound = tones[Math.floorMod(index, tones.size)]
    }
}

/**
 * Короткие звуки синтезируются при запуске (синус с затуханием) и проигрываются через SoundPool —
 * так не нужны аудиофайлы в ресурсах, а задержка минимальна.
 */
class SoundEngine(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val soundIds = ConcurrentHashMap<Sound, Int>()
    private val loaded = ConcurrentHashMap.newKeySet<Int>()

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded += id }
        val dir = File(context.cacheDir, "sfx")
        thread(name = "sfx-init") {
            dir.mkdirs()
            for (sound in Sound.entries) {
                val file = File(dir, "${sound.name.lowercase()}.wav")
                file.writeBytes(wav(synth(sound)))
                soundIds[sound] = pool.load(file.path, 1)
            }
        }
    }

    fun play(sound: Sound) {
        val id = soundIds[sound] ?: return
        if (id in loaded) pool.play(id, VOLUME, VOLUME, 1, 0, 1f)
    }

    private fun synth(sound: Sound): ShortArray = when (sound) {
        Sound.TAP -> notes(1760f to 30)
        Sound.CORRECT -> notes(1046.5f to 70, 1568f to 110)
        Sound.WRONG -> notes(220f to 90, 164.8f to 200, harmonics = 0.6f)
        Sound.FINISH -> notes(784f to 90, 1046.5f to 90, 1318.5f to 200)
        else -> notes(SCALE[sound.ordinal] to 220)
    }

    /** Склеивает ноты (частота в Гц, длительность в мс). */
    private fun notes(vararg notes: Pair<Float, Int>, harmonics: Float = 0.25f): ShortArray {
        val parts = notes.map { (freq, ms) -> note(freq, ms, harmonics) }
        val out = ShortArray(parts.sumOf { it.size })
        var offset = 0
        for (part in parts) {
            part.copyInto(out, offset)
            offset += part.size
        }
        return out
    }

    private fun note(freq: Float, ms: Int, harmonics: Float): ShortArray {
        val count = SAMPLE_RATE * ms / 1000
        val attack = SAMPLE_RATE * 4 / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = minOf(1.0, i.toDouble() / attack) * exp(-5.0 * i / count)
            val wave = sin(2 * PI * freq * t) + harmonics * sin(4 * PI * freq * t)
            (wave / (1 + harmonics) * envelope * Short.MAX_VALUE * 0.8).toInt().toShort()
        }
    }

    private fun wav(samples: ShortArray): ByteArray {
        val dataSize = samples.size * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(dataSize)
        for (s in samples) buffer.putShort(s)
        return buffer.array()
    }

    private companion object {
        const val SAMPLE_RATE = 44100
        const val VOLUME = 0.7f
        /** Пентатоника: любые две ноты звучат приятно вместе. */
        val SCALE = floatArrayOf(523.25f, 587.33f, 659.25f, 783.99f, 880f, 1046.5f, 1174.66f, 1318.51f, 1567.98f)
    }
}
