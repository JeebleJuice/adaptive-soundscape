package dev.jeeblejuice.soundscape.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneCatalogTest {
    @Test fun `catalog contains three scenes with loops and events`() {
        assertEquals(3, SceneCatalog.scenes.size)
        SceneCatalog.scenes.forEach { scene ->
            assertTrue(scene.layers.count { it.continuous } >= 3)
            assertTrue(scene.layers.count { !it.continuous } >= 2)
            assertEquals(scene.layers.size, scene.layers.map { it.id }.distinct().size)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `catalog rejects an incomplete scene set`() {
        SceneCatalog.load("[]")
    }

    @Test fun `default mixes contain every declared layer`() {
        SceneCatalog.scenes.forEach { scene ->
            val mix = Mix(scene.id)
            assertEquals(scene.layers.map { it.id }.toSet(), mix.layerVolumes.keys)
            assertTrue(mix.layerVolumes.values.all { it in 0f..1f })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid event interval is rejected`() {
        LayerDefinition("bad_event", "Bad", SoundKind.OWL, .5f, false, 10, 5)
    }
}
