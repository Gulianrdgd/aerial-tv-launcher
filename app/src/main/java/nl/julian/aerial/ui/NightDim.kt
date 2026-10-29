package nl.julian.aerial.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import nl.julian.aerial.data.LauncherConfig
import nl.julian.aerial.data.isNight
import nl.julian.aerial.theme.FluidEasing

/** Night mode: a black layer over the video and the panels, faded in when night begins. */
@Composable
fun NightDim(config: LauncherConfig, isDay: Boolean?) {
    val night by produceState(config.isNight(isDay), config.nightMode, isDay) {
        while (true) {
            value = config.isNight(isDay)
            delay(60_000)
        }
    }
    val alpha by animateFloatAsState(
        if (night) config.nightDim / 100f else 0f, tween(3_000, easing = FluidEasing), label = "night",
    )
    if (alpha > 0f) Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.background(Color.Black))
}
