package dev.jeeblejuice.soundscape.audio

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dev.jeeblejuice.soundscape.model.LayerDefinition
import dev.jeeblejuice.soundscape.model.Mix
import dev.jeeblejuice.soundscape.model.SceneCatalog
import dev.jeeblejuice.soundscape.model.SceneId
import dev.jeeblejuice.soundscape.service.PlaybackStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

@OptIn(UnstableApi::class)
class SoundscapeEngine(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val synthesizer = AudioSynthesizer(context.filesDir.resolve("generated_audio"))
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()
    private val layerPlayers = mutableMapOf<String, ExoPlayer>()
    private val eventJobs = mutableListOf<Job>()
    private val eventPlayers = mutableMapOf<ExoPlayer, String>()
    private var variationJob: Job? = null
    private var mix = Mix(SceneId.RAINY_ROOM)
    private var fadeFraction = 1f
    private var startedAt = 0L

    val sessionPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(audioAttributes, true)
        setHandleAudioBecomingNoisy(true)
        setWakeMode(C.WAKE_MODE_LOCAL)
        repeatMode = Player.REPEAT_MODE_ONE
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) startedAt = SystemClock.elapsedRealtime()
                else releaseEvents()
                layerPlayers.values.forEach { if (isPlaying) it.play() else it.pause() }
                PlaybackStore.update { it.copy(isPlaying = isPlaying) }
            }
        })
    }

    init {
        selectMix(mix)
    }

    fun selectMix(newMix: Mix) {
        if (sessionPlayer.mediaItemCount > 0 && newMix.sceneId == mix.sceneId) {
            mix = sanitize(newMix)
            applyVolumes()
            PlaybackStore.setMix(mix)
            return
        }
        mix = sanitize(newMix)
        val scene = SceneCatalog.get(mix.sceneId)
        val wasPlaying = sessionPlayer.playWhenReady
        eventJobs.forEach { it.cancel() }
        eventJobs.clear()
        releaseEvents()
        variationJob?.cancel()
        layerPlayers.values.forEach { it.release() }
        layerPlayers.clear()

        val continuous = scene.layers.filter { it.continuous }
        val first = continuous.first()
        sessionPlayer.setMediaItem(itemFor(first))
        sessionPlayer.prepare()
        sessionPlayer.repeatMode = Player.REPEAT_MODE_ONE
        continuous.drop(1).forEach { layer ->
            layerPlayers[layer.id] = ExoPlayer.Builder(context).build().apply {
                setAudioAttributes(audioAttributes, false)
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(itemFor(layer))
                prepare()
            }
        }
        scene.layers.filterNot { it.continuous }.forEach { scheduleEvents(it) }
        variationJob = scope.launch {
            while (isActive) {
                if (sessionPlayer.isPlaying) applyVolumes()
                delay(if (sessionPlayer.isPlaying) 100 else 1_000)
            }
        }
        applyVolumes()
        sessionPlayer.playWhenReady = wasPlaying
        PlaybackStore.setMix(mix)
    }

    fun setLayerVolume(layerId: String, volume: Float) {
        if (SceneCatalog.get(mix.sceneId).layers.none { it.id == layerId }) return
        mix = mix.copy(layerVolumes = mix.layerVolumes + (layerId to volume.coerceIn(0f, 1f)))
        applyVolumes()
        PlaybackStore.setMix(mix)
    }

    fun setMasterVolume(volume: Float) {
        mix = mix.copy(masterVolume = volume.coerceIn(0f, 1f))
        applyVolumes()
        PlaybackStore.setMix(mix)
    }

    fun setFadeFraction(fraction: Float) {
        fadeFraction = fraction.coerceIn(0f, 1f)
        applyVolumes()
        PlaybackStore.update { it.copy(fadeFraction = fadeFraction) }
    }

    fun play() {
        sessionPlayer.play()
    }

    fun pause() {
        sessionPlayer.pause()
    }

    fun release() {
        eventJobs.forEach { it.cancel() }
        releaseEvents()
        variationJob?.cancel()
        layerPlayers.values.forEach { it.release() }
        sessionPlayer.release()
    }

    private fun scheduleEvents(layer: LayerDefinition) {
        val scheduler = EventScheduler(Random.nextLong())
        eventJobs += scope.launch {
            while (isActive) {
                delay(scheduler.nextDelayMillis(layer))
                if (!sessionPlayer.isPlaying || mix.layerVolumes[layer.id] == 0f) continue
                val player = ExoPlayer.Builder(context).build().apply {
                    setAudioAttributes(audioAttributes, false)
                    volume = effectiveVolume(layer.id)
                    setMediaItem(itemFor(layer))
                    prepare()
                    play()
                }
                eventPlayers[player] = layer.id
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) releaseEvent(player)
                    }
                    override fun onPlayerError(error: PlaybackException) = releaseEvent(player)
                })
            }
        }
    }

    private fun releaseEvent(player: ExoPlayer) {
        if (eventPlayers.remove(player) != null) player.release()
    }

    private fun releaseEvents() = eventPlayers.keys.toList().forEach(::releaseEvent)

    private fun applyVolumes() {
        val continuous = SceneCatalog.get(mix.sceneId).layers.filter { it.continuous }
        sessionPlayer.volume = effectiveVolume(continuous.first().id)
        continuous.drop(1).forEach { layer ->
            layerPlayers[layer.id]?.volume = effectiveVolume(layer.id)
        }
        eventPlayers.forEach { (player, layerId) -> player.volume = effectiveVolume(layerId) }
    }

    private fun effectiveVolume(layerId: String): Float {
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        val variation = 1f + sin(elapsed / 11_000.0 + layerId.hashCode() % 11).toFloat() * .06f
        val fadeIn = if (startedAt == 0L) 0f else (elapsed / 1_500f).coerceIn(0f, 1f)
        return ((mix.layerVolumes[layerId] ?: 0f) * mix.masterVolume * fadeFraction * fadeIn * variation * .65f)
            .coerceIn(0f, 1f)
    }

    private fun itemFor(layer: LayerDefinition) = MediaItem.Builder()
        .setUri(Uri.fromFile(synthesizer.fileFor(layer.kind)))
        .setMediaId("${mix.sceneId.name}:${layer.id}")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(SceneCatalog.get(mix.sceneId).title)
                .setArtist("Adaptive Soundscape")
                .build(),
        )
        .build()

    private fun sanitize(candidate: Mix): Mix {
        val scene = SceneCatalog.get(candidate.sceneId)
        val volumes = scene.layers.associate { layer ->
            layer.id to (candidate.layerVolumes[layer.id] ?: layer.defaultVolume).coerceIn(0f, 1f)
        }
        return candidate.copy(masterVolume = candidate.masterVolume.coerceIn(0f, 1f), layerVolumes = volumes)
    }
}
