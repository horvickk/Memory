package io.horvickk.memory.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread

/**
 * Проигрывает синтезированные звуки через один AudioTrack и собственный микшер.
 * В отличие от SoundPool, звуки не обрывают друг друга. Пока тихо, дорожка стоит на паузе.
 */
class SoundEngine(context: Context) {
    private val lock = Object()
    private val mixer = Mixer()

    @Volatile
    private var bank: Map<Sound, FloatArray> = emptyMap()

    init {
        val audio = context.getSystemService(AudioManager::class.java)
        val rate = audio?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 48_000
        val burst = audio?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull() ?: 256
        thread(name = "sfx", priority = Thread.MAX_PRIORITY) { run(rate, burst.coerceIn(64, 1024)) }
    }

    fun play(sound: Sound) {
        val data = bank[sound] ?: return
        synchronized(lock) {
            mixer.add(data)
            lock.notifyAll()
        }
    }

    private fun run(rate: Int, chunk: Int) {
        bank = Sound.entries.associateWith { SoundSynth.render(it, rate) }
        var track = createTrack(rate, chunk) ?: return
        val mix = FloatArray(chunk)
        val out = ShortArray(chunk)
        val idleLimit = rate / chunk // ~1 с тишины после последнего звука, затем пауза
        var idleChunks = idleLimit
        var playing = false
        while (true) {
            synchronized(lock) {
                while (mixer.isIdle && idleChunks >= idleLimit) {
                    if (playing) {
                        track.pause()
                        track.flush()
                        playing = false
                    }
                    lock.wait()
                }
                mixer.render(mix)
                idleChunks = if (mixer.isIdle) idleChunks + 1 else 0
            }
            if (!playing) {
                track.play()
                playing = true
            }
            for (i in 0 until chunk) out[i] = (mix[i] * Short.MAX_VALUE).toInt().toShort()
            if (track.write(out, 0, chunk) < 0) {
                // Аудиовыход пропал (например, переключение устройства) — пересоздаём дорожку.
                track.release()
                track = createTrack(rate, chunk) ?: return
                playing = false
            }
        }
    }

    private fun createTrack(rate: Int, chunk: Int): AudioTrack? = runCatching {
        val minBytes = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(rate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minBytes, chunk * 2 * 2))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }.getOrNull()?.takeIf { track ->
        (track.state == AudioTrack.STATE_INITIALIZED).also { ok -> if (!ok) track.release() }
    }
}
