package nl.julian.aerial.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import nl.julian.aerial.ui.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.julian.aerial.data.HaConfig
import nl.julian.aerial.data.HaEntity
import nl.julian.aerial.data.HomeAssistant
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.SectionPanel
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.ui.menus.Overlay

/** The chosen Home Assistant buttons, four to a row, with their live state. */
@Composable
fun HomeAssistantRow(ha: HaConfig, modifier: Modifier = Modifier) {
    if (ha.buttons.isEmpty()) return
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableStateOf(0) }
    val states by produceState(emptyMap<String, HaEntity>(), ha, refresh) {
        while (true) {
            HomeAssistant.states(ha).onSuccess { all -> value = all.filter { it.id in ha.buttons }.associateBy { it.id } }
            delay(30_000)
        }
    }
    SectionPanel(modifier, title = "Home") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ha.buttons.chunked(4).forEach { ids ->
                Row(Modifier.fillMaxWidth().focusGroup(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ids.forEach { id ->
                        val e = states[id]
                        val domain = id.substringBefore('.')
                        QuickTile(
                            when (domain) { "light" -> Symbols.Sun; "scene", "script" -> Symbols.Play; else -> Symbols.Tune },
                            e?.name ?: id,
                            when { e == null -> "…"; domain == "scene" || domain == "script" -> "Run"; e.on -> "On"; else -> "Off" },
                            active = e?.on == true,
                            onClick = {
                                scope.launch {
                                    HomeAssistant.press(ha, id)
                                    delay(600)
                                    refresh++
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - ids.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * Watches the doorbell while Aerial is on screen (every 3 seconds) and,
 * when it turns on, shows the camera for 30 seconds. Back closes it.
 */
@Composable
fun DoorbellWatcher(ha: HaConfig) {
    val bell = ha.doorbell ?: return
    val cam = ha.camera ?: return
    var ringing by remember { mutableStateOf(false) }
    LaunchedEffect(ha) {
        var last: String? = null
        while (true) {
            val now = HomeAssistant.state(ha, bell)
            if (now == "on" && last != null && last != "on") ringing = true
            last = now
            delay(3_000)
        }
    }
    if (!ringing) return
    val frame by produceState<ImageBitmap?>(null, ha) {
        val until = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < until) {
            HomeAssistant.camera(ha, cam)?.let { value = it.asImageBitmap() }
            delay(1_000)
        }
        ringing = false
    }
    Overlay(onDismiss = { ringing = false }) {
        Column(
            Modifier
                .width(960.dp)
                .background(LocalSurfaces.current.default, MaterialTheme.shapes.extraLarge)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Someone's at the door", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            frame?.let {
                Image(
                    it, contentDescription = "Doorbell camera", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)),
                )
            }
        }
    }
}
