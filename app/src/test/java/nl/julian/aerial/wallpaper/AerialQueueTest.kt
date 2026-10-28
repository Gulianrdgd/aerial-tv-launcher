package nl.julian.aerial.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AerialQueueTest {
    private val clips = (1..5).map { Aerial("Clip $it", "https://example.com/$it.mov") }

    @Test
    fun everyClipPlaysOnceBeforeAnyRepeats() {
        val queue = AerialQueue(clips)
        repeat(20) {
            val round = List(clips.size) { queue.next() }
            assertEquals(clips.toSet(), round.toSet())
        }
    }

    @Test
    fun noClipPlaysTwiceInARowAcrossRefills() {
        val queue = AerialQueue(clips)
        var previous = queue.next()
        repeat(500) {
            val current = queue.next()
            assertNotEquals(previous, current)
            previous = current
        }
    }

    @Test
    fun peekReturnsWhatNextWillPlay() {
        val queue = AerialQueue(clips)
        repeat(12) { assertEquals(queue.peek(), queue.next()) }
    }

    @Test
    fun preferFirstMovesMatchToTheFront() {
        val queue = AerialQueue(clips)
        queue.preferFirst { it.title == "Clip 4" }
        assertEquals("Clip 4", queue.next().title)
    }
}
