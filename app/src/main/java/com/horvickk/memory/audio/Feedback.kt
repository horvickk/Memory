package com.horvickk.memory.audio

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import com.horvickk.memory.data.SettingsStore

/** Звук + вибрация с учётом настроек. */
class Feedback(
    private val sounds: SoundEngine,
    private val settings: SettingsStore,
    private val view: View,
) {
    fun tap() = emit(Sound.TAP, HapticFeedbackConstants.KEYBOARD_TAP)

    fun tone(index: Int) = emit(Sound.tone(index), HapticFeedbackConstants.KEYBOARD_TAP)

    /** Короткий мягкий сигнал верного ответа в быстрых играх, где полный «динь-динь» слишком длинный. */
    fun blip() = emit(Sound.BLIP, HapticFeedbackConstants.KEYBOARD_TAP)

    /** Тон без вибрации — для показа последовательностей. */
    fun silentTone(index: Int) {
        if (settings.sound) sounds.play(Sound.tone(index))
    }

    fun correct() = emit(
        Sound.CORRECT,
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY,
    )

    fun wrong() = emit(
        Sound.WRONG,
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
    )

    fun finish() = emit(Sound.FINISH, HapticFeedbackConstants.LONG_PRESS)

    private fun emit(sound: Sound, haptic: Int) {
        if (settings.sound) sounds.play(sound)
        if (settings.vibration) view.performHapticFeedback(haptic)
    }
}
