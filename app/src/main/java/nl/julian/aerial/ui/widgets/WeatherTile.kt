package nl.julian.aerial.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.julian.aerial.data.Sky
import nl.julian.aerial.data.Weather
import nl.julian.aerial.theme.EyebrowStyle
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.theme.topHighlight

/** Sky glyph; beside it the place, then the temperature with the next two hours of rain. Display only. */
@Composable
fun WeatherTile(weather: Weather, radius: Dp, modifier: Modifier = Modifier, compact: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .background(tileColor(), RoundedCornerShape(radius))
            .topHighlight(RoundedCornerShape(radius))
            .padding(horizontal = TILE_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start,
    ) {
        Icon(painterResource(skyIcon(weather)), contentDescription = weather.sky.name, tint = skyTint(weather), modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(10.dp))
        if (compact) {
            Text("${weather.temperature}°", style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
            return@Row
        }
        Column {
            Text(
                weather.place.uppercase(), style = EyebrowStyle, color = cs.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${weather.temperature}°", style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
                Spacer(Modifier.width(10.dp))
                if (weather.rainExpected) {
                    RainBars(weather.rain)
                } else {
                    Text("Dry for 2 h", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

/** Eight quarter-hour bars for the next two hours; empty quarters are dots. */
@Composable
private fun RainBars(rain: List<Float>) {
    val cs = MaterialTheme.colorScheme
    val dry = cs.onSurfaceVariant.copy(alpha = 0.35f)
    Canvas(Modifier.width(72.dp).height(14.dp).semantics { contentDescription = "Rain expected in the next 2 hours" }) {
        val n = rain.size.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        rain.forEachIndexed { i, mm ->
            // 2 mm per quarter (heavy rain) fills the bar.
            val h = if (mm < 0.1f) w else (mm / 2f).coerceIn(0.2f, 1f) * size.height
            drawRoundRect(
                color = if (mm < 0.1f) dry else cs.tertiary,
                topLeft = Offset(i * (w + gap), size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(w / 2),
            )
        }
    }
}

internal fun skyIcon(w: Weather) = when (w.sky) {
    Sky.Clear -> if (w.isDay) Symbols.Sun else Symbols.Moon
    Sky.PartlyCloudy -> if (w.isDay) Symbols.PartlyCloudy else Symbols.Cloud
    Sky.Cloudy -> Symbols.Cloud
    Sky.Fog -> Symbols.Fog
    Sky.Rain -> Symbols.Rain
    Sky.Snow -> Symbols.Snow
    Sky.Storm -> Symbols.Storm
}

/** A colour per sky, like a weather app: amber sun, pale moon, blue rain, white clouds. */
internal fun skyTint(w: Weather): Color = when (w.sky) {
    Sky.Clear -> if (w.isDay) Color(0xFFFFC94D) else Color(0xFFE6E3FF)
    Sky.PartlyCloudy -> if (w.isDay) Color(0xFFFFD873) else Color(0xFFE8ECF5)
    Sky.Rain, Sky.Storm -> Color(0xFF9CCBFF)
    Sky.Snow -> Color(0xFFE3F1FF)
    Sky.Cloudy, Sky.Fog -> Color(0xFFE8ECF5)
}
