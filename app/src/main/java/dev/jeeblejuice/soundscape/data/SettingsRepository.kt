package dev.jeeblejuice.soundscape.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.jeeblejuice.soundscape.model.FavouriteMix
import dev.jeeblejuice.soundscape.model.Mix
import dev.jeeblejuice.soundscape.model.SceneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore("soundscape_settings")

class SettingsRepository(private val context: Context) {
    private val favouritesKey = stringPreferencesKey("favourites_v1")
    private val lastMixKey = stringPreferencesKey("last_mix_v1")

    val favourites: Flow<List<FavouriteMix>> = context.dataStore.data.map { preferences ->
        MixCodec.decode(preferences[favouritesKey].orEmpty())
    }

    val lastMix: Flow<Mix?> = context.dataStore.data.map { preferences ->
        MixCodec.decode(preferences[lastMixKey].orEmpty()).firstOrNull()?.mix
    }

    suspend fun saveLastMix(mix: Mix) {
        val wrapped = FavouriteMix("last", "Last mix", mix, 0)
        context.dataStore.edit { it[lastMixKey] = MixCodec.encode(listOf(wrapped)) }
    }

    suspend fun addFavourite(name: String, mix: Mix) {
        context.dataStore.edit { preferences ->
            val current = MixCodec.decode(preferences[favouritesKey].orEmpty())
            val favourite = FavouriteMix(
                UUID.randomUUID().toString(),
                name.trim().take(60).ifBlank { SceneIdLabel.forId(mix.sceneId) },
                mix,
                System.currentTimeMillis(),
            )
            preferences[favouritesKey] = MixCodec.encode((listOf(favourite) + current).take(20))
        }
    }

    suspend fun deleteFavourite(id: String) {
        context.dataStore.edit { preferences ->
            val current = MixCodec.decode(preferences[favouritesKey].orEmpty())
            preferences[favouritesKey] = MixCodec.encode(current.filterNot { it.id == id })
        }
    }
}

private object SceneIdLabel {
    fun forId(id: SceneId) = when (id) {
        SceneId.RAINY_ROOM -> "Rainy room mix"
        SceneId.NIGHT_TRAIN -> "Night train mix"
        SceneId.FOREST_CABIN -> "Forest cabin mix"
    }
}
