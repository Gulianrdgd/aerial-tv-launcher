package nl.julian.aerial.data

import android.media.tv.TvInputInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class InputsRepositoryTest {
    private fun input(label: String, type: Int, connected: Boolean = true) =
        TvInput(id = label, label = label, type = type, passthrough = type != TvInputInfo.TYPE_TUNER, connected = connected)

    @Test
    fun connectedFirstThenHdmiThenOtherThenTuner() {
        val ordered = InputsRepository.ordered(
            listOf(
                input("Antenna", TvInputInfo.TYPE_TUNER),
                input("HDMI 2", TvInputInfo.TYPE_HDMI, connected = false),
                input("Component", TvInputInfo.TYPE_COMPONENT),
                input("HDMI 1", TvInputInfo.TYPE_HDMI),
            )
        )
        assertEquals(listOf("HDMI 1", "Component", "Antenna", "HDMI 2"), ordered.map { it.label })
    }
}
