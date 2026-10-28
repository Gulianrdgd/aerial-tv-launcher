package nl.julian.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ArrangementTest {
    private fun app(pkg: String) = AppEntry(pkg, pkg, banner = null, icon = null, stamp = 0, isSystem = false)

    // Scan order is alphabetical by label.
    private val apps = listOf("a", "b", "c", "com.google.android.youtube.tv", "d", "e", "f", "g", "h").map(::app)

    @Test
    fun defaultDockPutsKnownAppsFirstThenFills() {
        val home = arrange(apps, LauncherConfig())
        assertEquals(listOf("com.google.android.youtube.tv", "a", "b", "c", "d", "e"), home.dock.map { it.pkg })
        assertEquals(listOf("f", "g", "h"), home.grid.map { it.pkg })
    }

    @Test
    fun customDockAndHiddenAppsApply() {
        val home = arrange(apps, LauncherConfig(dock = listOf("g", "b"), hidden = setOf("a", "b")))
        assertEquals(listOf("g"), home.dock.map { it.pkg })
        assertEquals(listOf("c", "com.google.android.youtube.tv", "d", "e", "f", "h"), home.grid.map { it.pkg })
    }

    @Test
    fun manualOrderComesFirstAndNewAppsFollowAlphabetically() {
        val home = arrange(apps, LauncherConfig(dock = emptyList(), order = listOf("f", "c")))
        assertEquals(listOf("f", "c", "a", "b", "com.google.android.youtube.tv", "d", "e", "g", "h"), home.grid.map { it.pkg })
    }

    @Test
    fun movingIsClampedAtTheEnds() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), moved(list, "a", 2))
        assertEquals(listOf("a", "b", "d", "c"), moved(list, "d", -1))
        assertEquals(listOf("b", "c", "d", "a"), moved(list, "a", 99))
        assertEquals(list, moved(list, "a", -1))
    }

    @Test
    fun pinningAFullDockDropsItsLastApp() {
        assertEquals(listOf("a", "b", "x"), pinned(listOf("a", "b"), "x"))
        assertEquals(listOf("a", "b", "c", "d", "e", "x"), pinned(listOf("a", "b", "c", "d", "e", "f"), "x"))
        assertEquals(listOf("a", "b"), pinned(listOf("a", "b"), "a"))
    }
}
