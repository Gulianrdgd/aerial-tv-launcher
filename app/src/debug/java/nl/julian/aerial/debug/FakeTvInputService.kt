package nl.julian.aerial.debug

import android.media.tv.TvInputService

/**
 * Debug builds only, and disabled by default: a do-nothing TV input so the
 * input switcher has something to list on an emulator. Enable with
 * adb shell am broadcast -n nl.julian.aerial/.debug.FakeMediaReceiver -a input-on
 */
class FakeTvInputService : TvInputService() {
    override fun onCreateSession(inputId: String): Session? = null
}
