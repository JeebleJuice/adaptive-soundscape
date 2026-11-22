package dev.jeeblejuice.soundscape.audio

import dev.jeeblejuice.soundscape.model.SceneCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventSchedulerTest {
    private val event = SceneCatalog.scenes.first().layers.first { !it.continuous }

    @Test fun `same seed produces same long schedule`() {
        val first = EventScheduler(93).let { scheduler -> List(100) { scheduler.nextDelayMillis(event) } }
        val second = EventScheduler(93).let { scheduler -> List(100) { scheduler.nextDelayMillis(event) } }
        assertEquals(first, second)
    }

    @Test fun `every event stays within configured bounds`() {
        val scheduler = EventScheduler(11)
        repeat(10_000) {
            val delay = scheduler.nextDelayMillis(event)
            assertTrue(delay >= event.eventMinSeconds * 1_000L)
            assertTrue(delay <= event.eventMaxSeconds * 1_000L)
        }
    }

    @Test fun `every event layer varies throughout an hour without violating its cooldown`() {
        val events = SceneCatalog.scenes.flatMap { scene -> scene.layers.filterNot { it.continuous } }
        events.forEachIndexed { index, layer ->
            val scheduler = EventScheduler(index.toLong())
            val intervals = mutableListOf<Long>()
            var elapsed = 0L
            while (elapsed < 3_600_000L) {
                val interval = scheduler.nextDelayMillis(layer)
                assertTrue(interval in layer.eventMinSeconds * 1_000L..layer.eventMaxSeconds * 1_000L)
                intervals += interval
                elapsed += interval
            }
            assertTrue(intervals.distinct().size > 10)
        }
    }
}
