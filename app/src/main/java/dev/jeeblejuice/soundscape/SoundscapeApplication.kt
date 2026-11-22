package dev.jeeblejuice.soundscape

import android.app.Application
import dev.jeeblejuice.soundscape.data.SettingsRepository

class SoundscapeApplication : Application() {
    val settings by lazy { SettingsRepository(this) }
}
