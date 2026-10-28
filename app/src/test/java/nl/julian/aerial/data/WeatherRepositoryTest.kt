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
}
