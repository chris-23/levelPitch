package io.github.cnissler.levelpitch.leveling

import kotlin.math.abs

/**
 * Slopes of a fitted plane z = a + x·[x] + y·[y]. A slope is null when the points have no
 * spread along that axis (e.g. the two wheels of a single axle), so it cannot be determined.
 */
data class Slopes(val x: Double?, val y: Double?)

/**
 * Least-squares plane through [points] (position to height in mm).
 * If the points only spread along one axis, fits a line along that axis instead.
 */
fun fitSlopes(points: List<Pair<Point, Double>>): Slopes {
    require(points.size >= 2) { "need at least two points" }
    val mx = points.sumOf { it.first.xMm } / points.size
    val my = points.sumOf { it.first.yMm } / points.size
    val mz = points.sumOf { it.second } / points.size
    var sxx = 0.0; var syy = 0.0; var sxy = 0.0; var sxz = 0.0; var syz = 0.0
    for ((p, z) in points) {
        val dx = p.xMm - mx
        val dy = p.yMm - my
        val dz = z - mz
        sxx += dx * dx; syy += dy * dy; sxy += dx * dy
        sxz += dx * dz; syz += dy * dz
    }
    val spreadX = sxx > SPREAD_EPS_MM2
    val spreadY = syy > SPREAD_EPS_MM2
    return when {
        spreadX && spreadY -> {
            val det = sxx * syy - sxy * sxy
            require(abs(det) > SPREAD_EPS_MM2 * (sxx + syy)) { "points are collinear" }
            Slopes(x = (sxz * syy - syz * sxy) / det, y = (syz * sxx - sxz * sxy) / det)
        }
        spreadX -> Slopes(x = sxz / sxx, y = null)
        spreadY -> Slopes(x = null, y = syz / syy)
        else -> throw IllegalArgumentException("points coincide")
    }
}

/** Below this summed squared spread (mm²), an axis counts as having no spread. */
private const val SPREAD_EPS_MM2 = 1e-6
