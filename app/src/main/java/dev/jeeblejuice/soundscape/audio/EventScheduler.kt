package dev.jeeblejuice.soundscape.audio

import dev.jeeblejuice.soundscape.model.LayerDefinition
import kotlin.random.Random

/** Deterministic when seeded so long-running event patterns can be verified. */
class EventScheduler(seed: Long) {
    private val random = Random(seed)

    fun nextDelayMillis(layer: LayerDefinition): Long {
        require(!layer.continuous)
        return random.nextLong(
            layer.eventMinSeconds * 1_000L,
            layer.eventMaxSeconds * 1_000L + 1,
        )
    }
}
