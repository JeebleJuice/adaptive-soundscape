package dev.jeeblejuice.soundscape.service

import android.content.Intent
import android.app.PendingIntent
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dev.jeeblejuice.soundscape.MainActivity
import dev.jeeblejuice.soundscape.audio.FadeController
import dev.jeeblejuice.soundscape.audio.SoundscapeEngine
import dev.jeeblejuice.soundscape.model.Mix
import dev.jeeblejuice.soundscape.model.SceneCatalog
import dev.jeeblejuice.soundscape.model.SceneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class SoundscapeService : MediaSessionService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var engine: SoundscapeEngine
    private lateinit var mediaSession: MediaSession
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        engine = SoundscapeEngine(this, scope)
        mediaSession = MediaSession.Builder(this, engine.sessionPlayer)
            .setSessionActivity(PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ))
            .build()
        addSession(mediaSession)
        PlaybackStore.serviceRunning = true
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let(::handleIntent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        timerJob?.cancel()
        PlaybackStore.serviceRunning = false
        removeSession(mediaSession)
        mediaSession.release()
        engine.release()
        PlaybackStore.update { it.copy(isPlaying = false, timerEndMillis = null, fadeFraction = 1f) }
        scope.cancel()
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_PAUSE -> engine.pause()
            ACTION_APPLY_MIX -> decodeMix(intent.extras)?.let {
                engine.selectMix(it)
                engine.play()
            }
            ACTION_LAYER_VOLUME -> engine.setLayerVolume(
                intent.getStringExtra(EXTRA_LAYER).orEmpty(),
                intent.getFloatExtra(EXTRA_VOLUME, 0f),
            )
            ACTION_MASTER_VOLUME -> engine.setMasterVolume(intent.getFloatExtra(EXTRA_VOLUME, .8f))
            ACTION_TIMER -> startTimer(intent.getIntExtra(EXTRA_TIMER_MINUTES, 0))
        }
    }

    private fun startTimer(minutes: Int) {
        timerJob?.cancel()
        if (minutes <= 0) {
            engine.setFadeFraction(1f)
            PlaybackStore.update { it.copy(timerEndMillis = null) }
            return
        }
        val end = SystemClock.elapsedRealtime() + minutes.coerceAtMost(180) * 60_000L
        PlaybackStore.update { it.copy(timerEndMillis = end) }
        timerJob = scope.launch {
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val fraction = FadeController.fraction(now, end)
                engine.setFadeFraction(fraction)
                if (now >= end) {
                    engine.pause()
                    PlaybackStore.update { it.copy(timerEndMillis = null) }
                    engine.setFadeFraction(1f)
                    break
                }
                delay(if (fraction < 1f) 100 else 500)
            }
        }
    }

    private fun decodeMix(bundle: Bundle?): Mix? {
        bundle ?: return null
        val scene = runCatching { SceneId.valueOf(bundle.getString(EXTRA_SCENE).orEmpty()) }.getOrNull() ?: return null
        val definition = SceneCatalog.get(scene)
        val volumes = definition.layers.associate { it.id to bundle.getFloat("$EXTRA_LAYER.${it.id}", it.defaultVolume) }
        return Mix(scene, bundle.getFloat(EXTRA_VOLUME, .8f), volumes)
    }

    companion object {
        const val ACTION_PAUSE = "dev.jeeblejuice.soundscape.PAUSE"
        const val ACTION_APPLY_MIX = "dev.jeeblejuice.soundscape.APPLY_MIX"
        const val ACTION_LAYER_VOLUME = "dev.jeeblejuice.soundscape.LAYER_VOLUME"
        const val ACTION_MASTER_VOLUME = "dev.jeeblejuice.soundscape.MASTER_VOLUME"
        const val ACTION_TIMER = "dev.jeeblejuice.soundscape.TIMER"
        const val EXTRA_SCENE = "scene"
        const val EXTRA_LAYER = "layer"
        const val EXTRA_VOLUME = "volume"
        const val EXTRA_TIMER_MINUTES = "timer_minutes"

        fun putMix(intent: Intent, mix: Mix): Intent = intent.apply {
            putExtra(EXTRA_SCENE, mix.sceneId.name)
            putExtra(EXTRA_VOLUME, mix.masterVolume)
            mix.layerVolumes.forEach { (id, volume) -> putExtra("$EXTRA_LAYER.$id", volume) }
        }
    }
}
