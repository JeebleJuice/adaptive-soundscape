package dev.jeeblejuice.soundscape.service

import dev.jeeblejuice.soundscape.model.Mix
import dev.jeeblejuice.soundscape.model.PlaybackSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlaybackStore {
    var serviceRunning = false
    private val mutable = MutableStateFlow(PlaybackSnapshot())
    val state: StateFlow<PlaybackSnapshot> = mutable.asStateFlow()

    fun update(block: (PlaybackSnapshot) -> PlaybackSnapshot) {
        mutable.value = block(mutable.value)
    }

    fun setMix(mix: Mix) = update { it.copy(mix = mix) }
}
