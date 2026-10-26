package io.horvickk.memory.audio

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.tanh

/**
 * Смешивает одновременно звучащие звуки. Каждый звук доигрывает до конца:
 * новый звук добавляется поверх и не обрывает предыдущий.
 */
internal class Mixer(private val maxVoices: Int = 24) {
    private class Voice(val data: FloatArray) {
        var position = 0
    }

    private val voices = ArrayList<Voice>()

    val isIdle: Boolean get() = voices.isEmpty()

    fun add(data: FloatArray) {
        // Предел недостижим в обычной игре (нужно больше 20 звуков в секунду) — защита от переполнения.
        if (voices.size >= maxVoices) voices.removeAt(0)
        voices += Voice(data)
    }

    /** Заполняет [out] следующим отрезком смеси. */
    fun render(out: FloatArray) {
        out.fill(0f)
        val iterator = voices.iterator()
        while (iterator.hasNext()) {
            val voice = iterator.next()
            val count = minOf(out.size, voice.data.size - voice.position)
            for (i in 0 until count) out[i] += voice.data[voice.position + i]
            voice.position += count
            if (voice.position >= voice.data.size) iterator.remove()
        }
        for (i in out.indices) out[i] = limit(out[i])
    }

    /** Тихие звуки проходят без изменений, громкие наложения мягко сжимаются, а не клиппируют. */
    private fun limit(x: Float): Float {
        val a = abs(x)
        if (a <= KNEE) return x
        return sign(x) * (KNEE + (1 - KNEE) * tanh((a - KNEE) / (1 - KNEE)))
    }

    private companion object {
        const val KNEE = 0.6f
    }
}
