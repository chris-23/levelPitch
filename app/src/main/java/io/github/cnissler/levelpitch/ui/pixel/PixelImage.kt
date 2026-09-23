package io.github.cnissler.levelpitch.ui.pixel

import kotlin.math.abs
import kotlin.math.max

/** A small ARGB image for pixel art; drawing outside the bounds is clipped. 0 = transparent. */
class PixelImage(val width: Int, val height: Int) {
    val pixels = IntArray(width * height)

    operator fun get(x: Int, y: Int): Int = if (inside(x, y)) pixels[y * width + x] else 0

    operator fun set(x: Int, y: Int, color: Int) {
        if (inside(x, y)) pixels[y * width + x] = color
    }

    fun inside(x: Int, y: Int) = x in 0 until width && y in 0 until height

    fun fill(x: Int, y: Int, w: Int, h: Int, color: Int) {
        for (yy in y until y + h) for (xx in x until x + w) this[xx, yy] = color
    }

    /** Filled disc centred at ([cx], [cy]) in continuous coordinates (pixel (x, y) spans x..x+1). */
    fun disc(cx: Double, cy: Double, r: Double, color: Int) {
        for (y in (cy - r).toInt() - 1..(cy + r).toInt() + 1) {
            for (x in (cx - r).toInt() - 1..(cx + r).toInt() + 1) {
                val dx = x + 0.5 - cx
                val dy = y + 0.5 - cy
                if (dx * dx + dy * dy <= r * r) this[x, y] = color
            }
        }
    }

    /** Bresenham line, both ends included. */
    fun line(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        var x = x0
        var y = y0
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            this[x, y] = color
            if (x == x1 && y == y1) return
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
    }

    /** Rectangle [x0..x1] × [y0..y1] with corners rounded by [r], filled and outlined. */
    fun roundedBox(x0: Int, y0: Int, x1: Int, y1: Int, r: Int, fill: Int, outline: Int) {
        fun inBox(x: Int, y: Int): Boolean {
            if (x !in x0..x1 || y !in y0..y1) return false
            val dx = max(max(x0 + r - x, x - (x1 - r)), 0)
            val dy = max(max(y0 + r - y, y - (y1 - r)), 0)
            return dx * dx + dy * dy <= r * r
        }
        for (y in y0..y1) for (x in x0..x1) {
            if (!inBox(x, y)) continue
            val edge = !inBox(x - 1, y) || !inBox(x + 1, y) || !inBox(x, y - 1) || !inBox(x, y + 1)
            this[x, y] = if (edge) outline else fill
        }
    }
}
