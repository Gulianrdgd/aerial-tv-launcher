package nl.julian.aerial.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import nl.julian.aerial.data.Sky
import nl.julian.aerial.data.Weather
import nl.julian.aerial.theme.AerialType
import nl.julian.aerial.theme.LineIcons
import nl.julian.aerial.theme.LocalLauncherColors
import nl.julian.aerial.theme.glassCore

/** Square tile: place, sky glyph, temperature. Display only, so not focusable. */
@Composable
fun WeatherTile(weather: Weather, radius: Dp, modifier: Modifier = Modifier) {
    val colors = LocalLauncherColors.current
    Column(
        modifier.glassCore(RoundedCornerShape(radius), colors.tile).padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            weather.place.uppercase(), style = AerialType.eyebrow, color = colors.contentMuted,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Icon(skyIcon(weather), contentDescription = weather.sky.name, tint = colors.content, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(6.dp))
        Text("${weather.temperature}°", style = AerialType.clock.copy(fontSize = 30.sp, lineHeight = 32.sp), color = colors.content)
    }
}

private fun skyIcon(w: Weather) = when (w.sky) {
    Sky.Clear -> if (w.isDay) LineIcons.Sun else LineIcons.Moon
    Sky.PartlyCloudy -> if (w.isDay) LineIcons.PartlyCloudy else LineIcons.Cloud
    Sky.Cloudy -> LineIcons.Cloud
    Sky.Fog -> LineIcons.Fog
    Sky.Rain -> LineIcons.Rain
    Sky.Snow -> LineIcons.Snow
    Sky.Storm -> LineIcons.Storm
}
