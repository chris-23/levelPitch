package io.github.cnissler.levelpitch.leveling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaneFitTest {

    private val rectangle = listOf(
        Point(1000.0, 500.0), Point(1000.0, -500.0), Point(-1000.0, 500.0), Point(-1000.0, -500.0),
    )

    private fun plane(a: Double, b: Double, c: Double, at: List<Point>) =
        at.map { it to a + b * it.xMm + c * it.yMm }

    @Test
    fun recoversExactPlaneOnRectangle() {
        val s = fitSlopes(plane(5.0, 0.01, -0.02, rectangle))
        assertEquals(0.01, s.x!!, 1e-12)
        assertEquals(-0.02, s.y!!, 1e-12)
    }

    @Test
    fun recoversExactPlaneOnIrregularPoints() {
        // Correlated x and y, as camera-mode contact points may be.
        val pts = listOf(Point(0.0, 0.0), Point(1000.0, 0.0), Point(300.0, 700.0), Point(900.0, 400.0))
        val s = fitSlopes(plane(1.0, 0.002, 0.003, pts))
        assertEquals(0.002, s.x!!, 1e-12)
        assertEquals(0.003, s.y!!, 1e-12)
    }

    @Test
    fun oneRaisedCornerSplitsIntoPitchAndRoll() {
        // Front-left raised 40 mm: front mean 20, rear 0, left mean 20, right 0.
        val heights = listOf(40.0, 0.0, 0.0, 0.0)
        val s = fitSlopes(rectangle.zip(heights))
        assertEquals(20.0 / 2000.0, s.x!!, 1e-12)
        assertEquals(20.0 / 1000.0, s.y!!, 1e-12)
    }

    @Test
    fun singleAxleOnlyDeterminesRoll() {
        val s = fitSlopes(listOf(Point(0.0, 800.0) to 10.0, Point(0.0, -800.0) to -10.0))
        assertNull(s.x)
        assertEquals(10.0 / 800.0, s.y!!, 1e-12)
    }

    @Test
    fun pointsAlongXOnlyDeterminePitch() {
        val s = fitSlopes(listOf(Point(500.0, 0.0) to 5.0, Point(-500.0, 0.0) to 0.0))
        assertEquals(0.005, s.x!!, 1e-12)
        assertNull(s.y)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDiagonallyCollinearPoints() {
        fitSlopes(listOf(Point(0.0, 0.0) to 0.0, Point(1.0, 1.0) to 1.0, Point(2.0, 2.0) to 2.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSinglePoint() {
        fitSlopes(listOf(Point(0.0, 0.0) to 0.0))
    }
}
