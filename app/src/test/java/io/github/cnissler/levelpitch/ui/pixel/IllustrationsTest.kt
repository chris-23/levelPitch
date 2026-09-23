package io.github.cnissler.levelpitch.ui.pixel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class IllustrationsTest {

    @Test
    fun bubbleFloatsToTheHighSide() {
        val (right, _) = bubbleOffset(rightEdgeUpDeg = 2.0, topEdgeUpDeg = 0.0)
        assertTrue(right > 0)
        val (_, down) = bubbleOffset(rightEdgeUpDeg = 0.0, topEdgeUpDeg = 2.0)
        assertTrue("top edge up: bubble moves up the screen", down < 0)
        assertEquals(0 to 0, bubbleOffset(0.0, 0.0))
    }

    @Test
    fun bubbleStopsAtTheRim() {
        val (dx, dy) = bubbleOffset(30.0, 30.0)
        assertTrue(hypot(dx.toDouble(), dy.toDouble()) <= BUBBLE_TRAVEL + 0.8)
        assertEquals(BUBBLE_TRAVEL, bubbleOffset(90.0, 0.0).first)
    }

    @Test
    fun bubbleIsDrawnWhereItShouldBe() {
        val centre = LEVEL_SIZE / 2
        assertEquals(TutorialPalette.BUBBLE, bullseyeLevel(0, 0)[centre, centre])
        val moved = bullseyeLevel(6, -4)
        assertEquals(TutorialPalette.BUBBLE, moved[centre + 6, centre - 4])
        assertNotEquals(TutorialPalette.BUBBLE, moved[centre - 6, centre + 4])
    }

    @Test
    fun centreRingGlowsOnlyWhenLevel() {
        val ringPixel = { img: PixelImage -> img[LEVEL_SIZE / 2 + 5, LEVEL_SIZE / 2] }
        assertEquals(TutorialPalette.BUBBLE, ringPixel(bullseyeLevel(0, 0)))
        assertEquals(Palette.OUTLINE, ringPixel(bullseyeLevel(8, 8)))
    }

    @Test
    fun illustrationsHaveTheirSizes() {
        assertEquals(40, phonePlacementArt().width)
        assertEquals(36, chockArt().width)
    }
}
