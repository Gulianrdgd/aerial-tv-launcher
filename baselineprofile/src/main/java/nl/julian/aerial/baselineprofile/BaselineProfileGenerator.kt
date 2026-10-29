package nl.julian.aerial.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Startup, then the D-pad walk most people do: down through the sections and along the dock. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = "nl.julian.aerial") {
        pressHome()
        startActivityAndWait()
        repeat(4) { device.pressDPadRight(); device.waitForIdle() }
        repeat(6) { device.pressDPadDown(); device.waitForIdle() }
        repeat(6) { device.pressDPadUp(); device.waitForIdle() }
    }
}
