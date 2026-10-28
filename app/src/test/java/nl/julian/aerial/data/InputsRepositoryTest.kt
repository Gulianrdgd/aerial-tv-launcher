package nl.julian.aerial.data

import android.media.tv.TvInputInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class InputsRepositoryTest {
    private fun input(label: String, id: String, type: Int = TvInputInfo.TYPE_HDMI) =
        TvInput(id = id, label = label, type = type, passthrough = type != TvInputInfo.TYPE_TUNER, connected = true)

    @Test
    fun hdmiFirstInPortOrderThenOtherThenTuners() {
        val ordered = InputsRepository.ordered(
            listOf(
                input("Antenna", "com.tv/.Tuner/HW1", TvInputInfo.TYPE_TUNER),
                input("Wii", "com.tv/.hdmi.HDMIInputService/HW7"),
                input("Streaming", "com.tv/.hdmi.HDMIInputService/HW5"),
                input("Xbox", "com.tv/.hdmi.HDMIInputService/HW6"),
                input("YPbPr", "com.tv/.component.ComponentInputService/HW2", TvInputInfo.TYPE_COMPONENT),
            )
        )
        assertEquals(listOf("Streaming", "Xbox", "Wii", "YPbPr", "Antenna"), ordered.map { it.label })
    }

    @Test
    fun philipsDeviceTagsAreSplitOff() {
        assertEquals("Xbox" to InputDevice.Console, InputsRepository.parseLabel("Xbox&GAME_CONSOLE"))
        assertEquals("Wii" to InputDevice.Console, InputsRepository.parseLabel("Wii&GAME_CONSOLE"))
        assertEquals("HDMI 1" to InputDevice.Unknown, InputsRepository.parseLabel("HDMI 1"))
    }

    @Test
    fun vendorInputNamesBecomeReadable() {
        assertEquals("Antenna (digital)", InputsRepository.friendlyName("Tuner(DTV)"))
        assertEquals("Antenna (analog)", InputsRepository.friendlyName("Tuner(ATV)"))
        assertEquals("Component", InputsRepository.friendlyName("YPbPr"))
        assertEquals("HDMI 3", InputsRepository.friendlyName("HDMI 3"))
    }
}
