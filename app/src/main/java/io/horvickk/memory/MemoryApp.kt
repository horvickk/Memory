package io.horvickk.memory

import android.app.Application
import io.horvickk.memory.audio.SoundEngine
import io.horvickk.memory.data.ResultsStore
import io.horvickk.memory.data.SettingsStore

class MemoryApp : Application() {
    lateinit var settings: SettingsStore
        private set
    lateinit var results: ResultsStore
        private set
    lateinit var sounds: SoundEngine
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        results = ResultsStore(this)
        sounds = SoundEngine(this)
    }
}
