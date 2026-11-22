package dev.jeeblejuice.soundscape.data

import dev.jeeblejuice.soundscape.model.FavouriteMix
import dev.jeeblejuice.soundscape.model.Mix
import dev.jeeblejuice.soundscape.model.SceneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixCodecTest {
    @Test fun `favourite mixes round trip without losing levels`() {
        val mix = Mix(SceneId.NIGHT_TRAIN, .64f).let {
            it.copy(layerVolumes = it.layerVolumes + ("rails" to .18f))
        }
        val source = listOf(FavouriteMix("id-1", "Soft rails", mix, 123456L))
        val decoded = MixCodec.decode(MixCodec.encode(source))
        assertEquals(source, decoded)
    }

    @Test fun `damaged storage is safely ignored`() {
        assertTrue(MixCodec.decode("not JSON").isEmpty())
    }

    @Test fun `unknown layer data cannot enter the mix`() {
        val encoded = """[{"id":"x","name":"X","scene":"RAINY_ROOM","master":0.8,"created":1,"layers":{"window_rain":0.2,"malicious":1.0}}]"""
        val decoded = MixCodec.decode(encoded).single()
        assertTrue("malicious" !in decoded.mix.layerVolumes)
        assertEquals(5, decoded.mix.layerVolumes.size)
    }

    @Test fun `stored levels are clamped`() {
        val encoded = """[{"id":"x","name":"X","scene":"RAINY_ROOM","master":5,"created":1,"layers":{"window_rain":-3}}]"""
        val decoded = MixCodec.decode(encoded).single()
        assertEquals(1f, decoded.mix.masterVolume, 0f)
        assertEquals(0f, decoded.mix.layerVolumes.getValue("window_rain"), 0f)
    }
}
