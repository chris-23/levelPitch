package io.github.cnissler.levelpitch.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TiltTextTest {

    @Test
    fun roundsToTwoDecimals() {
        assertEquals(1.23, displayDeg(1.2345), 0.0)
        assertEquals(-0.46, displayDeg(-0.456), 0.0)
    }

    @Test
    fun neverShowsNegativeZero() {
        assertEquals("0.0", displayDeg(-0.001).toString())
    }

    @Test
    fun directionFollowsTheDisplayedValue() {
        assertEquals(Direction.POSITIVE, direction(0.3))
        assertEquals(Direction.NEGATIVE, direction(-0.3))
        assertEquals(Direction.NONE, direction(0.004))
        assertEquals(Direction.NONE, direction(-0.004))
        assertEquals(Direction.POSITIVE, direction(0.006))
    }
}
