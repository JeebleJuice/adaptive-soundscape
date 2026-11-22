package dev.jeeblejuice.soundscape.ui

import android.content.ComponentName
import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.jeeblejuice.soundscape.MainActivity
import dev.jeeblejuice.soundscape.service.PlaybackStore
import dev.jeeblejuice.soundscape.service.SoundscapeService
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class PlaybackIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @After fun stopService() {
        val activity = compose.activity
        activity.stopService(Intent(activity, SoundscapeService::class.java))
    }

    @Test fun appStartsPlaybackAndMediaControllerPausesAndResumesIt() {
        compose.onNodeWithTag("soundscape").performScrollToIndex(3)
        compose.onNodeWithTag("play_pause").performClick()
        compose.waitUntil(20_000) { PlaybackStore.state.value.isPlaying }

        val activity = compose.activity
        val token = SessionToken(activity, ComponentName(activity, SoundscapeService::class.java))
        val future = compose.runOnIdle { MediaController.Builder(activity, token).buildAsync() }
        try {
            val controller = future.get(10, TimeUnit.SECONDS)
            compose.runOnIdle { controller.pause() }
            compose.waitUntil(10_000) { !PlaybackStore.state.value.isPlaying }
            compose.runOnIdle { controller.play() }
            compose.waitUntil(10_000) { PlaybackStore.state.value.isPlaying }
        } finally {
            compose.runOnIdle { MediaController.releaseFuture(future) }
        }
    }
}
