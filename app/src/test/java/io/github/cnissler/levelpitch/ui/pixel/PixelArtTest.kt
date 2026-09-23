package io.github.cnissler.levelpitch.ui.pixel

import io.github.cnissler.levelpitch.leveling.Wheel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelArtTest {

    private val red = 0xFFFF0000.toInt()

    @Test
    fun fillIsClippedToTheImage() {
        val img = PixelImage(4, 3)
        img.fill(-2, -2, 10, 10, red)
        assertTrue(img.pixels.all { it == red })
        assertEquals(0, img[5, 5])
    }

    @Test
    fun discIsSymmetric() {
        val img = PixelImage(12, 12)
        img.disc(6.0, 6.0, 4.0, red)
        for (y in 0 until 12) for (x in 0 until 12) assertEquals(img[x, y], img[11 - x, y])
        assertEquals(red, img[6, 6])
        assertEquals(0, img[0, 0])
    }

    @Test
    fun lineIncludesBothEnds() {
        val img = PixelImage(10, 10)
        img.line(1, 1, 8, 5, red)
        assertEquals(red, img[1, 1])
        assertEquals(red, img[8, 5])
    }

    @Test
    fun roundedBoxHasOutlineFillAndCutCorners() {
        val img = PixelImage(10, 10)
        img.roundedBox(0, 0, 9, 9, 2, fill = 1, outline = 2)
        assertEquals(0, img[0, 0])
        assertEquals(2, img[5, 0])
        assertEquals(1, img[5, 5])
    }

    @Test
    fun spritesHaveWheelsAtTheirAxleRows() {
        for (kind in SpriteKind.entries) {
            val s = vehicleSprite(kind)
            assertEquals(SPRITE_WIDTH, s.image.width)
            s.wheelRows.forEach { (wheel, row) ->
                val left = wheel in setOf(Wheel.FRONT_LEFT, Wheel.REAR_LEFT, Wheel.LEFT)
                val x = if (left) 0 else SPRITE_WIDTH - 1
                assertEquals("$kind $wheel", Palette.TYRE_EDGE, s.image[x, row])
                assertEquals("$kind $wheel", Palette.TYRE, s.image[if (left) 1 else SPRITE_WIDTH - 2, row + 1])
            }
        }
    }

    @Test
    fun highlightedWheelsChangeColour() {
        val s = vehicleSprite(SpriteKind.MOTORHOME, highlight = setOf(Wheel.REAR_RIGHT))
        assertEquals(Palette.HIGHLIGHT, s.image[SPRITE_WIDTH - 1, s.wheelRows.getValue(Wheel.REAR_RIGHT)])
        assertEquals(Palette.TYRE_EDGE, s.image[0, s.wheelRows.getValue(Wheel.REAR_LEFT)])
    }

    @Test
    fun caravanWheelSetsMatchTheirAxles() {
        assertEquals(setOf(Wheel.LEFT, Wheel.RIGHT), vehicleSprite(SpriteKind.CARAVAN_SINGLE).wheelRows.keys)
        assertEquals(4, vehicleSprite(SpriteKind.CARAVAN_TANDEM).wheelRows.size)
    }

    @Test
    fun stepHeightsAreProportionalWithAMinimum() {
        assertEquals(listOf(3, 5, 8), badgeStepHeights(listOf(30.0, 60.0, 90.0)))
        assertEquals(listOf(2, 8), badgeStepHeights(listOf(5.0, 100.0)))
    }

    @Test
    fun tyreRestsOnTheCarryingStep() {
        val steps = listOf(30.0, 60.0, 90.0)
        val heights = badgeStepHeights(steps)
        for (current in 0..3) {
            val img = wedgeBadge(steps, current, target = null)
            val stand = if (current == 0) 0 else heights[current - 1]
            val bottom = BADGE_GROUND_ROW - stand - 1
            val x = BADGE_TYRE_X
            assertEquals("tyre bottom, step $current", Palette.TYRE_EDGE, img[x, bottom])
            val below = img[x, bottom + 1]
            if (current == 0) {
                assertEquals(Palette.GROUND, below)
            } else {
                assertTrue("wedge under tyre, step $current", below == Palette.WEDGE_TOP)
            }
        }
    }

    @Test
    fun wedgeInFrontOfTheTyreFitsTheBadge() {
        val wedgeColours = setOf(Palette.WEDGE, Palette.WEDGE_TOP, Palette.WEDGE_DARK)
        for (n in 1..6) {
            val img = wedgeBadge((1..n).map { it * 20.0 }, current = 0, target = null)
            val columns = (0 until BADGE_WIDTH).count { x -> (0 until BADGE_HEIGHT).any { y -> img[x, y] in wedgeColours } }
            assertEquals("all wedge columns drawn with $n steps", n * maxOf(3, 20 / n), columns)
        }
    }

    @Test
    fun tyreRestsOnTheLiftBlock() {
        for (mm in listOf(0.0, 50.0, 100.0)) {
            val img = liftBadge(maxLiftMm = 100.0, currentMm = mm, targetMm = null)
            val bottom = BADGE_GROUND_ROW - (mm / 100.0 * 8).toInt() - 1
            assertEquals("lift $mm", Palette.TYRE_EDGE, img[BADGE_TYRE_X, bottom])
            assertTrue("lift $mm", img[BADGE_TYRE_X, bottom + 1] in setOf(Palette.GROUND, Palette.WEDGE_TOP))
        }
    }

    @Test
    fun extraLiftGlowsAndLessLiftGreysOut() {
        assertTrue(liftBadge(100.0, 20.0, 60.0).pixels.any { it == Palette.HIGHLIGHT })
        assertTrue(liftBadge(100.0, 60.0, 20.0).pixels.any { it == Palette.WEDGE_OFF })
        assertTrue(liftBadge(100.0, 40.0, 40.0).pixels.none { it == Palette.HIGHLIGHT || it == Palette.WEDGE_OFF })
    }

    @Test
    fun targetStepGlowsAndRemovalGreysOut() {
        val steps = listOf(30.0, 60.0, 90.0)
        val glowing = wedgeBadge(steps, current = 1, target = 2)
        assertTrue(glowing.pixels.any { it == Palette.HIGHLIGHT })
        val same = wedgeBadge(steps, current = 2, target = 2)
        assertTrue(same.pixels.none { it == Palette.HIGHLIGHT })
        val removing = wedgeBadge(steps, current = 1, target = 0)
        assertTrue(removing.pixels.any { it == Palette.WEDGE_OFF })
        assertNotEquals(0, removing.pixels.count { it == Palette.TYRE })
    }
}
