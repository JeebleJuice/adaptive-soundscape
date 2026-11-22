package dev.jeeblejuice.soundscape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.jeeblejuice.soundscape.ui.SoundscapeApp
import dev.jeeblejuice.soundscape.ui.SoundscapeViewModel
import dev.jeeblejuice.soundscape.ui.theme.SoundscapeTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<SoundscapeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoundscapeTheme { SoundscapeApp(viewModel) }
        }
    }
}
