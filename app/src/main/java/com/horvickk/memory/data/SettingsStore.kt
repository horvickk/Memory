package com.horvickk.memory.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

enum class ThemeMode(val title: String) { SYSTEM("КАК В СИСТЕМЕ"), LIGHT("СВЕТЛАЯ"), DARK("ТЁМНАЯ") }

/** Множитель времени показа заданий: чем больше, тем дольше показ. */
enum class ShowSpeed(val title: String, val factor: Float) {
    SLOW("МЕДЛЕННО", 1.5f),
    NORMAL("ОБЫЧНО", 1f),
    FAST("БЫСТРО", 0.7f),
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var sound by mutableStateOf(prefs.getBoolean(KEY_SOUND, true))
        private set
    var vibration by mutableStateOf(prefs.getBoolean(KEY_VIBRATION, true))
        private set
    var theme by mutableStateOf(enumOrDefault(prefs.getString(KEY_THEME, null), ThemeMode.SYSTEM))
        private set
    var speed by mutableStateOf(enumOrDefault(prefs.getString(KEY_SPEED, null), ShowSpeed.NORMAL))
        private set

    fun updateSound(value: Boolean) {
        sound = value
        prefs.edit { putBoolean(KEY_SOUND, value) }
    }

    fun updateVibration(value: Boolean) {
        vibration = value
        prefs.edit { putBoolean(KEY_VIBRATION, value) }
    }

    fun updateTheme(value: ThemeMode) {
        theme = value
        prefs.edit { putString(KEY_THEME, value.name) }
    }

    fun updateSpeed(value: ShowSpeed) {
        speed = value
        prefs.edit { putString(KEY_SPEED, value.name) }
    }

    private companion object {
        const val KEY_SOUND = "sound"
        const val KEY_VIBRATION = "vibration"
        const val KEY_THEME = "theme"
        const val KEY_SPEED = "speed"

        inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
