package dev.jeeblejuice.soundscape.audio

import kotlin.math.max

object FadeController {
    fun fraction(nowMillis: Long, endMillis: Long?, fadeDurationMillis: Long = 30_000): Float {
        if (endMillis == null) return 1f
        val remaining = endMillis - nowMillis
        if (remaining <= 0) return 0f
        return (remaining.toFloat() / max(1, fadeDurationMillis)).coerceIn(0f, 1f)
    }
}
