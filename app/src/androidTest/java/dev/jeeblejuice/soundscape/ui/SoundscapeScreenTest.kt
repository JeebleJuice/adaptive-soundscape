package dev.jeeblejuice.soundscape.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import dev.jeeblejuice.soundscape.model.PlaybackSnapshot
import dev.jeeblejuice.soundscape.model.SceneId
import dev.jeeblejuice.soundscape.ui.theme.SoundscapeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SoundscapeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun firstSceneIsImmediatelyUsable() {
        var toggles = 0
        compose.setContent {
            SoundscapeTheme {
                SoundscapeScreen(
                    playback = PlaybackSnapshot(), favourites = emptyList(),
                    onSelectScene = {}, onTogglePlayback = { toggles++ },
                    onMasterVolume = {}, onLayerVolume = { _, _ -> }, onTimer = {},
                    onSaveFavourite = {}, onLoadFavourite = {}, onDeleteFavourite = {},
                )
            }
        }
        compose.onAllNodesWithText("Rainy room")[0].assertIsDisplayed()
        compose.onNodeWithTag("soundscape").performScrollToIndex(3)
        compose.onNodeWithContentDescription("Play soundscape").performClick()
        assertEquals(1, toggles)
    }

    @Test fun mixerExposesEveryRainyRoomLayer() {
        compose.setContent {
            SoundscapeTheme {
                SoundscapeScreen(
                    playback = PlaybackSnapshot(), favourites = emptyList(),
                    onSelectScene = {}, onTogglePlayback = {}, onMasterVolume = {},
                    onLayerVolume = { _, _ -> }, onTimer = {}, onSaveFavourite = {},
                    onLoadFavourite = {}, onDeleteFavourite = {},
                )
            }
        }
        compose.onNodeWithTag("soundscape").performScrollToIndex(3)
        compose.onNodeWithTag("Mixer").performClick()
        compose.onNodeWithTag("soundscape").performScrollToIndex(4)
        compose.onNodeWithTag("mixer").assertIsDisplayed()
        compose.onNodeWithContentDescription("Master volume").assertIsDisplayed()
        compose.onNodeWithContentDescription("Window rain volume").assertIsDisplayed()
        compose.onNodeWithContentDescription("Far thunder volume").assertExists()
    }

    @Test fun choosingASceneAndFocusPresetDispatchesTheirValues() {
        var chosenScene: SceneId? = null
        var minutes = 0
        compose.setContent {
            SoundscapeTheme {
                SoundscapeScreen(
                    playback = PlaybackSnapshot(), favourites = emptyList(),
                    onSelectScene = { chosenScene = it }, onTogglePlayback = {},
                    onMasterVolume = {}, onLayerVolume = { _, _ -> }, onTimer = { minutes = it },
                    onSaveFavourite = {}, onLoadFavourite = {}, onDeleteFavourite = {},
                )
            }
        }
        compose.onNodeWithText("Night train").performClick()
        assertEquals(SceneId.NIGHT_TRAIN, chosenScene)
        compose.onNodeWithTag("soundscape").performScrollToIndex(3)
        compose.onNodeWithTag("Timer").performClick()
        compose.onNodeWithTag("soundscape").performScrollToIndex(4)
        compose.onNodeWithText("Focus · 25 min").performClick()
        assertEquals(25, minutes)
    }
}
