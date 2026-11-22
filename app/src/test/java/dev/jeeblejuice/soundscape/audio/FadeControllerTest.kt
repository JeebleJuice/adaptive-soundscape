package dev.jeeblejuice.soundscape.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class FadeControllerTest {
    @Test fun `timer stays full before fade window`() {
        assertEquals(1f, FadeController.fraction(10_000, 90_000, 30_000), 0f)
    }

    @Test fun `timer fades linearly in final window`() {
        assertEquals(.5f, FadeController.fraction(75_000, 90_000, 30_000), .0001f)
    }

    @Test fun `expired timer is silent`() {
        assertEquals(0f, FadeController.fraction(90_001, 90_000), 0f)
    }

    @Test fun `missing timer is full volume`() {
        assertEquals(1f, FadeController.fraction(90_001, null), 0f)
    }
}
