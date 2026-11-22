package dev.jeeblejuice.soundscape.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jeeblejuice.soundscape.model.FavouriteMix
import dev.jeeblejuice.soundscape.model.Mix
import dev.jeeblejuice.soundscape.model.PlaybackSnapshot
import dev.jeeblejuice.soundscape.model.SceneCatalog
import dev.jeeblejuice.soundscape.model.SceneDefinition
import dev.jeeblejuice.soundscape.model.SceneId
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun SoundscapeApp(viewModel: SoundscapeViewModel) {
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()
    SoundscapeScreen(
        playback = playback,
        favourites = favourites,
        onSelectScene = viewModel::selectScene,
        onTogglePlayback = viewModel::togglePlayback,
        onMasterVolume = viewModel::setMasterVolume,
        onLayerVolume = viewModel::setLayerVolume,
        onTimer = viewModel::setTimer,
        onSaveFavourite = viewModel::saveFavourite,
        onLoadFavourite = viewModel::loadFavourite,
        onDeleteFavourite = viewModel::deleteFavourite,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundscapeScreen(
    playback: PlaybackSnapshot,
    favourites: List<FavouriteMix>,
    onSelectScene: (SceneId) -> Unit,
    onTogglePlayback: () -> Unit,
    onMasterVolume: (Float) -> Unit,
    onLayerVolume: (String, Float) -> Unit,
    onTimer: (Int) -> Unit,
    onSaveFavourite: (String) -> Unit,
    onLoadFavourite: (FavouriteMix) -> Unit,
    onDeleteFavourite: (String) -> Unit,
) {
    var showMixer by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    val scene = SceneCatalog.get(playback.mix.sceneId)

    Scaffold(containerColor = Color.Transparent) { scaffoldPadding ->
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(sceneBackground(scene.id), MaterialTheme.colorScheme.background)),
            ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding).testTag("soundscape"),
            ) {
                item { Header() }
                item {
                    SceneVisual(
                        scene = scene,
                        intensity = playback.mix.layerVolumes.getValue(scene.layers.first().id) *
                            playback.mix.masterVolume * playback.fadeFraction,
                        isPlaying = playback.isPlaying,
                        modifier = Modifier.fillMaxWidth().height(330.dp).padding(horizontal = 20.dp),
                    )
                }
                item { ScenePicker(playback.mix.sceneId, onSelectScene) }
                item {
                    NowPlaying(
                        playback,
                        onTogglePlayback,
                        onMixer = { showMixer = !showMixer },
                        onTimer = { showTimer = !showTimer },
                        onSave = { showSave = true },
                    )
                }
                if (showMixer) item { Mixer(playback.mix, onMasterVolume, onLayerVolume) }
                if (showTimer) item { TimerPanel(playback, onTimer) }
                if (favourites.isNotEmpty()) item {
                    Favourites(favourites, onLoadFavourite, onDeleteFavourite)
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    if (showSave) SaveMixDialog(
        defaultName = "${scene.title} mix",
        onDismiss = { showSave = false },
        onSave = { onSaveFavourite(it); showSave = false },
    )
}

@Composable
private fun Header() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text("Adaptive Soundscape", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScenePicker(selected: SceneId, onSelect: (SceneId) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SceneCatalog.scenes.forEach { scene ->
            if (scene.id == selected) Button(onClick = { onSelect(scene.id) }) { Text(scene.title) }
            else OutlinedButton(onClick = { onSelect(scene.id) }) { Text(scene.title) }
        }
    }
}

@Composable
private fun NowPlaying(
    playback: PlaybackSnapshot,
    onTogglePlayback: () -> Unit,
    onMixer: () -> Unit,
    onTimer: () -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
        AnimatedContent(playback.mix.sceneId, label = "scene title") {
            val displayed = SceneCatalog.get(it)
            Column {
                Text(displayed.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text(displayed.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = onTogglePlayback,
                modifier = Modifier.size(64.dp).testTag("play_pause"),
            ) {
                Icon(
                    if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (playback.isPlaying) "Pause soundscape" else "Play soundscape",
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            ActionButton(Icons.Default.Tune, "Mixer", onMixer)
            ActionButton(Icons.Default.Schedule, "Timer", onTimer)
            ActionButton(Icons.Default.BookmarkAdd, "Save mix", onSave)
        }
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.width(72.dp).testTag(label).clickable(onClick = onClick).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Mixer(mix: Mix, onMaster: (Float) -> Unit, onLayer: (String, Float) -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).testTag("mixer"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .92f)),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Mixer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            VolumeRow("Master", mix.masterVolume, onMaster)
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = .25f))
            SceneCatalog.get(mix.sceneId).layers.forEach { layer ->
                VolumeRow(layer.name, mix.layerVolumes[layer.id] ?: layer.defaultVolume) { onLayer(layer.id, it) }
            }
        }
    }
}

@Composable
private fun VolumeRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp)
            Text("${(value * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.semantics { contentDescription = "$label volume" },
        )
    }
}

@Composable
private fun TimerPanel(playback: PlaybackSnapshot, onTimer: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("Fade-out timer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            val end = playback.timerEndMillis
            var now by remember(end) { mutableStateOf(SystemClock.elapsedRealtime()) }
            LaunchedEffect(end) {
                if (end != null) while (now < end) {
                    delay(1_000)
                    now = SystemClock.elapsedRealtime()
                }
            }
            val remaining = end?.let { ((it - now + 999) / 1_000).coerceAtLeast(0) }
            Text(
                if (remaining != null) "${remaining / 60} min ${remaining % 60} sec remaining" else "Playback fades out during the final 30 seconds.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Focus" to 25, "Relax" to 45, "Sleep" to 60).forEach { (label, minutes) ->
                    OutlinedButton(onClick = { onTimer(minutes) }) { Text("$label · $minutes min") }
                }
                if (playback.timerEndMillis != null) TextButton(onClick = { onTimer(0) }) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun Favourites(items: List<FavouriteMix>, onLoad: (FavouriteMix) -> Unit, onDelete: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text("Saved mixes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        items.forEach { favourite ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onLoad(favourite) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f)),
            ) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(favourite.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        Text(SceneCatalog.get(favourite.mix.sceneId).title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                    IconButton(onClick = { onDelete(favourite.id) }) { Icon(Icons.Default.DeleteOutline, "Delete ${favourite.name}") }
                }
            }
        }
    }
}

@Composable
private fun SaveMixDialog(defaultName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save this mix") },
        text = { TextField(value = name, onValueChange = { name = it.take(60) }, singleLine = true, label = { Text("Mix name") }) },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SceneVisual(scene: SceneDefinition, intensity: Float, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val phase = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "scene motion")
        transition.animateFloat(
            0f, 1f, infiniteRepeatable(tween(6_000), RepeatMode.Restart), label = "phase",
        ).value
    } else 0f
    Canvas(modifier.clip(RoundedCornerShape(32.dp)).background(sceneBackground(scene.id).copy(alpha = .72f)).semantics {
        contentDescription = "Animated ${scene.title} atmosphere"
    }) {
        when (scene.id) {
            SceneId.RAINY_ROOM -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF21413F), Color(0xFF0B1718))))
                repeat(46) { i ->
                    val x = ((i * 79f + phase * 260f) % (size.width + 40f)) - 20f
                    val y = ((i * 137f + phase * size.height * 1.7f) % (size.height + 80f)) - 40f
                    drawLine(Color(0xFFBCD9D2).copy(alpha = .18f + intensity * .35f), Offset(x, y), Offset(x - 8, y + 34), 2f)
                }
                drawCircle(Color(0xFFF4C47B).copy(alpha = .15f), size.minDimension * .2f, Offset(size.width * .78f, size.height * .28f))
            }
            SceneId.NIGHT_TRAIN -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF101A34), Color(0xFF080E18))))
                val horizon = size.height * .66f
                repeat(8) { i ->
                    val x = ((i * size.width / 5f - phase * size.width * 1.7f) % (size.width * 1.4f) + size.width * 1.4f) % (size.width * 1.4f) - size.width * .2f
                    drawCircle(if (i % 3 == 0) Color(0xFFF1B774) else Color(0xFF9FC5C6), 4f + intensity * 5f, Offset(x, horizon - (i % 4) * 19f))
                }
                val path = Path().apply { moveTo(0f, horizon); lineTo(size.width, size.height * .8f) }
                drawPath(path, Color(0xFF6C807D).copy(alpha = .6f), style = Stroke(5f))
                drawLine(Color(0xFF6C807D).copy(alpha = .45f), Offset(size.width, horizon), Offset(0f, size.height * .8f), 4f)
            }
            SceneId.FOREST_CABIN -> {
                drawRect(Brush.radialGradient(listOf(Color(0xFF6D3E27), Color(0xFF12241C), Color(0xFF07100F)), center = Offset(size.width * .5f, size.height * .65f)))
                repeat(28) { i ->
                    val angle = i * .73f
                    val x = size.width * .5f + sin(angle + phase * 6.28f) * size.width * .16f
                    val y = size.height * .72f - ((i * 31f + phase * 180f) % (size.height * .45f))
                    drawCircle(Color(0xFFFFB45E).copy(alpha = .15f + intensity * .55f), 2f + i % 3, Offset(x, y))
                }
                drawCircle(Color(0xFFFFA95E).copy(alpha = .16f + intensity * .12f), size.minDimension * .22f, Offset(size.width * .5f, size.height * .72f))
            }
        }
    }
}

private fun sceneBackground(id: SceneId) = when (id) {
    SceneId.RAINY_ROOM -> Color(0xFF173230)
    SceneId.NIGHT_TRAIN -> Color(0xFF111B34)
    SceneId.FOREST_CABIN -> Color(0xFF203024)
}
