package nl.julian.aerial.ui.menus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.julian.aerial.data.Weather
import nl.julian.aerial.data.WeatherDetails
import nl.julian.aerial.ui.widgets.skyIcon
import nl.julian.aerial.ui.widgets.skyTint
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** The forecast behind the weather tile: the next hours, the next days, or both. Display only; Back closes it. */
@Composable
fun WeatherPanel(weather: Weather, details: WeatherDetails, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    SidePanel(weather.place, onDismiss, initialFocus = null) {
        if (details != WeatherDetails.Daily && weather.hours.isNotEmpty()) {
            Label("Next hours")
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                weather.hours.take(6).forEach { h ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(h.time.substringAfter('T').take(5), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                        Icon(
                            painterResource(skyIcon(weather.copy(sky = h.sky, isDay = h.isDay))), null,
                            tint = skyTint(weather.copy(sky = h.sky, isDay = h.isDay)), modifier = Modifier.size(28.dp),
                        )
                        Text("${h.temperature}°", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    }
                }
            }
        }
        if (details != WeatherDetails.Hourly && weather.days.isNotEmpty()) {
            Label("Next days")
            weather.days.forEachIndexed { i, d ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val name = if (i == 0) "Today" else runCatching {
                        LocalDate.parse(d.date).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    }.getOrDefault(d.date)
                    Text(name, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, modifier = Modifier.weight(1f))
                    Icon(
                        painterResource(skyIcon(weather.copy(sky = d.sky, isDay = true))), null,
                        tint = skyTint(weather.copy(sky = d.sky, isDay = true)), modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("${d.high}°", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    Spacer(Modifier.width(10.dp))
                    Text("${d.low}°", style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
    )
}
