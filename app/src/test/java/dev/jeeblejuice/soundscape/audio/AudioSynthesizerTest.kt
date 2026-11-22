package dev.jeeblejuice.soundscape.audio

import dev.jeeblejuice.soundscape.model.SoundKind
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.math.abs

class AudioSynthesizerTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun `every sound kind creates a valid nonempty PCM wave`() {
        val synth = AudioSynthesizer(temporary.root)
        SoundKind.entries.forEach { kind ->
            val file = synth.fileFor(kind)
            assertTrue("$kind should have audio data", file.length() > 44)
            val bytes = file.readBytes()
            assertEquals("RIFF", bytes.copyOfRange(0, 4).decodeToString())
            assertEquals("WAVE", bytes.copyOfRange(8, 12).decodeToString())
            assertEquals("data", bytes.copyOfRange(36, 40).decodeToString())
        }
    }

    @Test fun `generation is deterministic across directories`() {
        val first = AudioSynthesizer(temporary.newFolder("a")).fileFor(SoundKind.RAIN).readBytes()
        val second = AudioSynthesizer(temporary.newFolder("b")).fileFor(SoundKind.RAIN).readBytes()
        assertArrayEquals(first, second)
    }

    @Test fun `existing generated file is reused`() {
        val synth = AudioSynthesizer(temporary.root)
        val file = synth.fileFor(SoundKind.FIRE)
        val modified = 123L
        file.setLastModified(modified)
        synth.fileFor(SoundKind.FIRE)
        assertEquals(modified, file.lastModified())
    }

    @Test fun `loop boundaries have no silence fade or oversized sample jump`() {
        val synth = AudioSynthesizer(temporary.root)
        val kinds = dev.jeeblejuice.soundscape.model.SceneCatalog.scenes
            .flatMap { scene -> scene.layers.filter { it.continuous }.map { it.kind } }.distinct()
        kinds.forEach { kind ->
            val bytes = synth.fileFor(kind).readBytes()
            fun sample(index: Int): Int {
                val offset = 44 + index * 2
                return ((bytes[offset].toInt() and 255) or (bytes[offset + 1].toInt() shl 8)).toShort().toInt()
            }
            val count = (bytes.size - 44) / 2
            val seam = abs(sample(count - 1) - sample(0))
            val maxLocalJump = (1..2_000).maxOf { abs(sample(it) - sample(it - 1)) }
            assertTrue("$kind seam jumps by $seam", seam <= maxLocalJump * 2 + 5)
            assertTrue("$kind starts with signal", (0..1_000).maxOf { abs(sample(it)) } > 50)
        }
    }
}
