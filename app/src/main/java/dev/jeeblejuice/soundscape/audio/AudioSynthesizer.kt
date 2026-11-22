package dev.jeeblejuice.soundscape.audio

import dev.jeeblejuice.soundscape.model.SoundKind
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Creates deterministic offline WAV sources. The generated files are application data, not
 * recordings, and can be replaced with licensed assets without changing the playback model.
 */
class AudioSynthesizer(private val directory: File) {
    private val sampleRate = 22_050
    private val dropletTimes = doubleArrayOf(.45, 1.15, 2.05, 2.82, 3.55)

    @Synchronized fun fileFor(kind: SoundKind): File {
        directory.mkdirs()
        val file = File(directory, "${kind.name.lowercase()}_v2.wav")
        if (!file.exists() || file.length() < 1_000) {
            val temporary = File.createTempFile("sound-", ".wav", directory)
            try {
                writeWav(temporary, kind)
                check(temporary.renameTo(file))
            } finally {
                temporary.delete()
            }
        }
        return file
    }

    private fun writeWav(file: File, kind: SoundKind) {
        val event = kind in setOf(
            SoundKind.DROPS, SoundKind.THUNDER, SoundKind.TUNNEL,
            SoundKind.PASSING_TRAIN, SoundKind.CREAKS, SoundKind.OWL,
        )
        val durationSeconds = if (event) 5 else 23 + kind.ordinal * 2
        val count = sampleRate * durationSeconds
        val overlap = if (event) 0 else sampleRate
        val random = Random(kind.ordinal * 7_919 + 41)
        var smooth = 0.0
        var slow = 0.0
        val samples = ShortArray(count + overlap) { index ->
            val t = index.toDouble() / sampleRate
            val white = random.nextDouble(-1.0, 1.0)
            smooth = smooth * .92 + white * .08
            slow = slow * .997 + white * .003
            val value = when (kind) {
                SoundKind.RAIN -> smooth * .72 + white * .16 + sin(2 * PI * .11 * t) * .04
                SoundKind.DROPS -> droplets(t, white)
                SoundKind.ROOM -> slow * .45 + sin(2 * PI * 58 * t) * .05
                SoundKind.TRAFFIC -> slow * .68 + sin(2 * PI * (44 + 4 * sin(t * .4)) * t) * .08
                SoundKind.THUNDER -> thunder(t, smooth)
                SoundKind.CARRIAGE -> smooth * .32 + sin(2 * PI * 47 * t) * .18 + sin(2 * PI * 94 * t) * .06
                SoundKind.RAILS -> railRhythm(t, smooth)
                SoundKind.WIND -> slow * (0.55 + .25 * sin(2 * PI * .08 * t))
                SoundKind.TUNNEL -> tunnel(t, smooth)
                SoundKind.PASSING_TRAIN -> passingTrain(t, smooth)
                SoundKind.FIRE -> fire(t, white, smooth)
                SoundKind.CABIN_RAIN -> smooth * .65 + white * .11 + sin(2 * PI * 2.2 * t) * .025
                SoundKind.CREAKS -> creak(t)
                SoundKind.OWL -> owl(t)
            }
            val loopEdge = if (event) eventEnvelope(t, durationSeconds.toDouble()) else 1.0
            (value * loopEdge * 18_000).toInt().coerceIn(-32_000, 32_000).toShort()
        }
        BufferedOutputStream(FileOutputStream(file)).use { output ->
            writeHeader(output, count * 2)
            repeat(count) { index ->
                val sample = if (index < overlap) {
                    val blend = index.toDouble() / overlap
                    (samples[count + index] * (1 - blend) + samples[index] * blend).toInt()
                } else samples[index].toInt()
                output.write(sample and 0xff)
                output.write((sample shr 8) and 0xff)
            }
        }
    }

    private fun droplets(t: Double, noise: Double): Double {
        return dropletTimes.sumOf { hit ->
            val d = t - hit
            if (d in 0.0..0.18) sin(2 * PI * (900 - 1_800 * d) * d) * exp(-24 * d) else 0.0
        } + noise * .015
    }

    private fun thunder(t: Double, noise: Double): Double {
        val d = t - .55
        if (d !in 0.0..3.8) return 0.0
        return (noise * .7 + sin(2 * PI * 38 * d) * .24) * exp(-.72 * d) * (1 - exp(-9 * d))
    }

    private fun railRhythm(t: Double, noise: Double): Double {
        val phase = (t * 2.15) % 1.0
        val clack = exp(-42 * phase) + .75 * exp(-42 * ((phase + .51) % 1.0))
        return noise * .24 + clack * .34 - .12
    }

    private fun tunnel(t: Double, noise: Double): Double {
        val d = t - .5
        val envelope = if (d in 0.0..4.0) sin(PI * d / 4.0).coerceAtLeast(0.0) else 0.0
        return envelope * (noise * .45 + sin(2 * PI * 63 * t) * .18)
    }

    private fun passingTrain(t: Double, noise: Double): Double {
        val d = t - .45
        val envelope = if (d in 0.0..4.0) sin(PI * d / 4.0).coerceAtLeast(0.0) else 0.0
        val pitch = 82 - d * 7
        return envelope * (noise * .42 + sin(2 * PI * pitch * d) * .2)
    }

    private fun fire(t: Double, white: Double, smooth: Double): Double {
        val crackle = if (abs(sin(t * 13.7) * sin(t * 7.1)) > .93) white * .9 else 0.0
        return smooth * .28 + crackle
    }

    private fun creak(t: Double): Double {
        val d = t - .6
        if (d !in 0.0..2.8) return 0.0
        return sin(2 * PI * (185 - 39 * d) * d) * sin(PI * d / 2.8) * .55
    }

    private fun owl(t: Double): Double {
        fun call(start: Double): Double {
            val d = t - start
            return if (d in 0.0..0.62) sin(2 * PI * (460 + 25 * sin(d * 8)) * d) * sin(PI * d / .62) else 0.0
        }
        return (call(.7) + call(1.55)) * .42
    }

    private fun eventEnvelope(t: Double, duration: Double): Double =
        (t / .08).coerceIn(0.0, 1.0) * ((duration - t) / .2).coerceIn(0.0, 1.0)

    private fun writeHeader(output: BufferedOutputStream, dataSize: Int) {
        fun ascii(value: String) = value.forEach { output.write(it.code) }
        fun le(value: Int, bytes: Int) = repeat(bytes) { shift -> output.write((value shr (8 * shift)) and 0xff) }
        ascii("RIFF"); le(36 + dataSize, 4); ascii("WAVEfmt "); le(16, 4)
        le(1, 2); le(1, 2); le(sampleRate, 4); le(sampleRate * 2, 4); le(2, 2); le(16, 2)
        ascii("data"); le(dataSize, 4)
    }
}
