package dev.jeeblejuice.soundscape.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.jeeblejuice.soundscape.SoundscapeApplication
import dev.jeeblejuice.soundscape.model.FavouriteMix
import dev.jeeblejuice.soundscape.model.Mix
import dev.jeeblejuice.soundscape.model.PlaybackSnapshot
import dev.jeeblejuice.soundscape.model.SceneId
import dev.jeeblejuice.soundscape.service.PlaybackStore
import dev.jeeblejuice.soundscape.service.SoundscapeService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SoundscapeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SoundscapeApplication
    val playback = PlaybackStore.state
    val favourites: StateFlow<List<FavouriteMix>> = app.settings.favourites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val saved = app.settings.lastMix.first()
            if (!PlaybackStore.serviceRunning && playback.value == PlaybackSnapshot()) {
                saved?.let(PlaybackStore::setMix)
            }
        }
    }

    fun selectScene(sceneId: SceneId) {
        val mix = playback.value.mix.let { if (it.sceneId == sceneId) it else Mix(sceneId, it.masterVolume) }
        PlaybackStore.setMix(mix)
        val intent = SoundscapeService.putMix(
            Intent(app, SoundscapeService::class.java).apply { action = SoundscapeService.ACTION_APPLY_MIX },
            mix,
        )
        dispatch(intent)
        persist(mix)
    }

    fun togglePlayback() {
        val snapshot = playback.value
        if (snapshot.isPlaying) {
            dispatch(Intent(app, SoundscapeService::class.java).apply { action = SoundscapeService.ACTION_PAUSE })
        } else {
            val intent = SoundscapeService.putMix(
                Intent(app, SoundscapeService::class.java).apply { action = SoundscapeService.ACTION_APPLY_MIX },
                snapshot.mix,
            )
            dispatch(intent)
        }
    }

    fun setMasterVolume(volume: Float) {
        val mix = playback.value.mix.copy(masterVolume = volume)
        PlaybackStore.setMix(mix)
        if (PlaybackStore.serviceRunning) dispatch(Intent(app, SoundscapeService::class.java).apply {
            action = SoundscapeService.ACTION_MASTER_VOLUME
            putExtra(SoundscapeService.EXTRA_VOLUME, volume)
        })
        persist(mix)
    }

    fun setLayerVolume(layerId: String, volume: Float) {
        val current = playback.value.mix
        val mix = current.copy(layerVolumes = current.layerVolumes + (layerId to volume))
        PlaybackStore.setMix(mix)
        if (PlaybackStore.serviceRunning) dispatch(Intent(app, SoundscapeService::class.java).apply {
            action = SoundscapeService.ACTION_LAYER_VOLUME
            putExtra(SoundscapeService.EXTRA_LAYER, layerId)
            putExtra(SoundscapeService.EXTRA_VOLUME, volume)
        })
        persist(mix)
    }

    fun setTimer(minutes: Int) {
        if (minutes == 0 && !PlaybackStore.serviceRunning) return
        if (!playback.value.isPlaying && minutes > 0) togglePlayback()
        dispatch(Intent(app, SoundscapeService::class.java).apply {
            action = SoundscapeService.ACTION_TIMER
            putExtra(SoundscapeService.EXTRA_TIMER_MINUTES, minutes)
        })
    }

    fun saveFavourite(name: String) {
        viewModelScope.launch { app.settings.addFavourite(name, playback.value.mix) }
    }

    fun loadFavourite(favourite: FavouriteMix) {
        PlaybackStore.setMix(favourite.mix)
        val intent = SoundscapeService.putMix(
            Intent(app, SoundscapeService::class.java).apply { action = SoundscapeService.ACTION_APPLY_MIX },
            favourite.mix,
        )
        dispatch(intent)
        persist(favourite.mix)
    }

    fun deleteFavourite(id: String) {
        viewModelScope.launch { app.settings.deleteFavourite(id) }
    }

    private fun persist(mix: Mix) {
        viewModelScope.launch { app.settings.saveLastMix(mix) }
    }

    private fun dispatch(intent: Intent) {
        app.startService(intent)
    }
}
