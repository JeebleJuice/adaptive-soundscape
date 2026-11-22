package dev.jeeblejuice.soundscape.model

import org.json.JSONArray

enum class SceneId { RAINY_ROOM, NIGHT_TRAIN, FOREST_CABIN }

enum class SoundKind {
    RAIN, DROPS, ROOM, TRAFFIC, THUNDER,
    CARRIAGE, RAILS, WIND, TUNNEL, PASSING_TRAIN,
    FIRE, CABIN_RAIN, CREAKS, OWL
}

data class LayerDefinition(
    val id: String,
    val name: String,
    val kind: SoundKind,
    val defaultVolume: Float,
    val continuous: Boolean = true,
    val eventMinSeconds: Int = 0,
    val eventMaxSeconds: Int = 0,
) {
    init {
        require(name.isNotBlank())
        require(id.matches(Regex("[a-z][a-z0-9_]*")))
        require(defaultVolume in 0f..1f)
        require(continuous || (eventMinSeconds > 0 && eventMaxSeconds >= eventMinSeconds))
    }
}

data class SceneDefinition(
    val id: SceneId,
    val title: String,
    val subtitle: String,
    val layers: List<LayerDefinition>,
) {
    init {
        require(title.isNotBlank() && subtitle.isNotBlank())
        require(layers.map { it.id }.distinct().size == layers.size)
        require(layers.count { it.continuous } >= 3)
        require(layers.count { !it.continuous } >= 2)
    }
}

object SceneCatalog {
    val scenes: List<SceneDefinition> = load(
        checkNotNull(SceneCatalog::class.java.getResourceAsStream("/soundscape-scenes.json"))
            .bufferedReader().use { it.readText() },
    )

    internal fun load(json: String): List<SceneDefinition> {
        val definitions = JSONArray(json)
        val scenes = List(definitions.length()) { index ->
            val definition = definitions.getJSONObject(index)
            val layers = definition.getJSONArray("layers")
            SceneDefinition(
                id = SceneId.valueOf(definition.getString("id")),
                title = definition.getString("title"),
                subtitle = definition.getString("subtitle"),
                layers = List(layers.length()) { layerIndex ->
                    val layer = layers.getJSONObject(layerIndex)
                    val interval = if (layer.has("intervalSeconds")) layer.getJSONArray("intervalSeconds") else null
                    require(interval == null || interval.length() == 2)
                    LayerDefinition(
                        id = layer.getString("id"),
                        name = layer.getString("name"),
                        kind = SoundKind.valueOf(layer.getString("kind")),
                        defaultVolume = layer.getDouble("volume").toFloat(),
                        continuous = interval == null,
                        eventMinSeconds = interval?.getInt(0) ?: 0,
                        eventMaxSeconds = interval?.getInt(1) ?: 0,
                    )
                },
            )
        }
        require(scenes.size == SceneId.entries.size && scenes.map { it.id }.toSet() == SceneId.entries.toSet())
        return scenes
    }

    fun get(id: SceneId): SceneDefinition = scenes.first { it.id == id }
}

data class Mix(
    val sceneId: SceneId,
    val masterVolume: Float = .8f,
    val layerVolumes: Map<String, Float> = SceneCatalog.get(sceneId).layers.associate { it.id to it.defaultVolume },
) {
    init {
        require(masterVolume in 0f..1f)
        require(layerVolumes.values.all { it in 0f..1f })
    }
}

data class FavouriteMix(
    val id: String,
    val name: String,
    val mix: Mix,
    val createdAtMillis: Long,
)

data class PlaybackSnapshot(
    val mix: Mix = Mix(SceneId.RAINY_ROOM),
    val isPlaying: Boolean = false,
    val timerEndMillis: Long? = null,
    val fadeFraction: Float = 1f,
)
