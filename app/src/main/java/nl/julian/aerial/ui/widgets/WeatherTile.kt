package nl.julian.aerial.ui.widgets

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.julian.aerial.data.Sky
import nl.julian.aerial.data.Weather
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.Symbols

/** Place, sky glyph (in the tertiary accent) and temperature. Display only, so not focusable. */
@Composable
fun WeatherTile(weather: Weather, radius: Dp, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier.background(LocalSurfaces.current.high, RoundedCornerShape(radius)).padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            weather.place, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Icon(painterResource(skyIcon(weather)), contentDescription = weather.sky.name, tint = cs.tertiary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(4.dp))
        Text("${weather.temperature}°", style = MaterialTheme.typography.headlineMedium, color = cs.onSurface)
    }
}

private fun skyIcon(w: Weather) = when (w.sky) {
    Sky.Clear -> if (w.isDay) Symbols.Sun else Symbols.Moon
    Sky.PartlyCloudy -> if (w.isDay) Symbols.PartlyCloudy else Symbols.Cloud
    Sky.Cloudy -> Symbols.Cloud
    Sky.Fog -> Symbols.Fog
    Sky.Rain -> Symbols.Rain
    Sky.Snow -> Symbols.Snow
    Sky.Storm -> Symbols.Storm
}
