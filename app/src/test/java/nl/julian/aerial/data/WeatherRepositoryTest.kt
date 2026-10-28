package nl.julian.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherRepositoryTest {
    @Test
    fun cityComesFromTheLastTimeZoneSegment() {
        assertEquals("Amsterdam", WeatherRepository.cityFromTimeZone("Europe/Amsterdam"))
        assertEquals("Los Angeles", WeatherRepository.cityFromTimeZone("America/Los_Angeles"))
        assertEquals("Buenos Aires", WeatherRepository.cityFromTimeZone("America/Argentina/Buenos_Aires"))
    }

    @Test
    fun zonesWithoutACityAreSkipped() {
        assertNull(WeatherRepository.cityFromTimeZone("UTC"))
        assertNull(WeatherRepository.cityFromTimeZone("Etc/GMT+2"))
    }

    @Test
    fun wmoCodesMapToSkies() {
        assertEquals(Sky.Clear, WeatherRepository.skyFor(0))
        assertEquals(Sky.PartlyCloudy, WeatherRepository.skyFor(2))
        assertEquals(Sky.Cloudy, WeatherRepository.skyFor(3))
        assertEquals(Sky.Fog, WeatherRepository.skyFor(45))
        assertEquals(Sky.Rain, WeatherRepository.skyFor(61))
        assertEquals(Sky.Rain, WeatherRepository.skyFor(81))
        assertEquals(Sky.Snow, WeatherRepository.skyFor(73))
        assertEquals(Sky.Storm, WeatherRepository.skyFor(95))
    }

    @Test
    fun rainCountsFromATenthOfAMillimetre() {
        val dry = Weather("A", 10, Sky.Cloudy, true, rain = listOf(0f, 0.05f, 0f, 0f, 0f, 0f, 0f, 0f))
        val wet = Weather("A", 10, Sky.Rain, true, rain = listOf(0f, 0f, 0.3f, 1.2f, 0f, 0f, 0f, 0f))
        assertEquals(false, dry.rainExpected)
        assertEquals(true, wet.rainExpected)
        assertEquals(false, Weather("A", 10, Sky.Clear, true).rainExpected)
    }

    @Test
    fun rainOutlookSummarisesTheNextTwoHours() {
        fun w(vararg mm: Float) = Weather("A", 10, Sky.Rain, true, rain = mm.toList())
        assertEquals("Dry for 2 h", w(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f).rainOutlook)
        assertEquals("Rain in 30 min", w(0f, 0.05f, 0.4f, 1f, 0f, 0f, 0f, 0f).rainOutlook)
        assertEquals("Dry in 45 min", w(0.5f, 0.3f, 0.2f, 0f, 0f, 0f, 0f, 0f).rainOutlook)
        assertEquals("Rain for 2 h", w(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f).rainOutlook)
    }
}
