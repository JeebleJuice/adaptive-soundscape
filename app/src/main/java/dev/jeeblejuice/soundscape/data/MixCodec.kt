package dev.jeeblejuice.soundscape.data

import dev.jeeblejuice.soundscape.model.FavouriteMix
import dev.jeeblejuice.soundscape.model.Mix
import dev.jeeblejuice.soundscape.model.SceneCatalog
import dev.jeeblejuice.soundscape.model.SceneId
import org.json.JSONArray
import org.json.JSONObject

object MixCodec {
    fun encode(favourites: List<FavouriteMix>): String = JSONArray().apply {
        favourites.forEach { favourite ->
            put(JSONObject().apply {
                put("id", favourite.id)
                put("name", favourite.name)
                put("scene", favourite.mix.sceneId.name)
                put("master", favourite.mix.masterVolume.toDouble())
                put("created", favourite.createdAtMillis)
                put("layers", JSONObject(favourite.mix.layerVolumes.mapValues { it.value.toDouble() }))
            })
        }
    }.toString()

    fun decode(value: String): List<FavouriteMix> = runCatching {
        val result = mutableListOf<FavouriteMix>()
        val array = JSONArray(value)
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val sceneId = SceneId.valueOf(item.getString("scene"))
            val validLayers = SceneCatalog.get(sceneId).layers.map { it.id }.toSet()
            val layerObject = item.getJSONObject("layers")
            val layers = validLayers.associateWith { id ->
                if (layerObject.has(id)) layerObject.getDouble(id).toFloat().coerceIn(0f, 1f)
                else SceneCatalog.get(sceneId).layers.first { it.id == id }.defaultVolume
            }
            result += FavouriteMix(
                id = item.getString("id"),
                name = item.getString("name").take(60),
                mix = Mix(sceneId, item.getDouble("master").toFloat().coerceIn(0f, 1f), layers),
                createdAtMillis = item.getLong("created"),
            )
        }
        result
    }.getOrDefault(emptyList())
}
