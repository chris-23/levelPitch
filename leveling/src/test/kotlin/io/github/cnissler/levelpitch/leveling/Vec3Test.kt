package io.github.cnissler.levelpitch.leveling

import org.junit.Assert.assertEquals
import org.junit.Test

class Vec3Test {

    private val x = Vec3(1.0, 0.0, 0.0)
    private val y = Vec3(0.0, 1.0, 0.0)

    @Test
    fun norm() {
        assertEquals(5.0, Vec3(3.0, 0.0, 4.0).norm, 0.0)
    }

    @Test
    fun arithmetic() {
        assertEquals(Vec3(4.0, 2.0, 6.0), Vec3(1.0, 2.0, 3.0) + Vec3(3.0, 0.0, 3.0))
        assertEquals(Vec3(2.0, 4.0, 6.0), Vec3(1.0, 2.0, 3.0) * 2.0)
        assertEquals(Vec3(0.5, 1.0, 1.5), Vec3(1.0, 2.0, 3.0) / 2.0)
    }

    @Test
    fun dotAndCross() {
        assertEquals(0.0, x dot y, 0.0)
        assertEquals(14.0, Vec3(1.0, 2.0, 3.0) dot Vec3(1.0, 2.0, 3.0), 0.0)
        assertEquals(Vec3.UP, x cross y)
        assertEquals(Vec3(0.0, 0.0, -1.0), y cross x)
    }

    @Test
    fun normalized() {
        assertEquals(Vec3(0.6, 0.0, 0.8), Vec3(3.0, 0.0, 4.0).normalized())
    }

    @Test
    fun mean() {
        assertEquals(Vec3(2.0, 1.0, 0.0), Vec3.mean(listOf(Vec3(1.0, 0.0, 0.0), Vec3(3.0, 2.0, 0.0))))
    }
}
