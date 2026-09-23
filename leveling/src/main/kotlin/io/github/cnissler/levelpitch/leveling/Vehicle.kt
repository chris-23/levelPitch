package io.github.cnissler.levelpitch.leveling

/**
 * Wheel positions. Single-axle caravans use [LEFT] and [RIGHT]; motorhomes and
 * tandem caravans use the four corners (for a tandem: front/rear axle of the pair).
 */
enum class Wheel { FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT, LEFT, RIGHT }

/** Position in the vehicle frame (x forward, y left), in mm. */
data class Point(val xMm: Double, val yMm: Double)

/**
 * Vehicle geometry as far as levelling needs it. The frame origin is the centre of the
 * wheel footprint: mid-wheelbase for motorhomes, the (mid-)axle centre for caravans.
 */
sealed interface Vehicle {
    /** Contact point of every wheel. */
    val wheels: Map<Wheel, Point>

    /** Wheels that always get the same wedge step (one group per independently raised position). */
    val raiseGroups: List<List<Wheel>>

    /** [state] with [wheel] and the wheels raised together with it set to [step]. */
    fun withStep(state: Map<Wheel, Int>, wheel: Wheel, step: Int): Map<Wheel, Int> {
        val group = requireNotNull(raiseGroups.find { wheel in it }) { "$wheel is not a wheel of this vehicle" }
        return state + group.associateWith { step }
    }

    /** Ground heights at the wheel contact points implied by [tilt], in mm relative to the origin. */
    fun contactHeightsMm(tilt: Tilt): Map<Wheel, Double> =
        wheels.mapValues { (_, p) -> tilt.heightAtMm(p.xMm, p.yMm) }
}

/** Two-axle motorhome or van: every wheel can be raised on its own. */
data class Motorhome(val wheelbaseMm: Double, val trackMm: Double) : Vehicle {
    init {
        require(wheelbaseMm > 0 && trackMm > 0) { "dimensions must be positive" }
    }

    override val wheels = corners(wheelbaseMm, trackMm)
    override val raiseGroups = wheels.keys.map { listOf(it) }
}

/** Caravan: roll is levelled with wedges per side, pitch with the jockey wheel at the hitch. */
sealed interface Caravan : Vehicle {
    /** Horizontal distance from the (mid-)axle to the hitch, which sits at y = 0. */
    val hitchToAxleMm: Double

    val hitch: Point get() = Point(hitchToAxleMm, 0.0)
}

data class SingleAxleCaravan(val trackMm: Double, override val hitchToAxleMm: Double) : Caravan {
    init {
        require(trackMm > 0 && hitchToAxleMm > 0) { "dimensions must be positive" }
    }

    override val wheels = mapOf(
        Wheel.LEFT to Point(0.0, trackMm / 2),
        Wheel.RIGHT to Point(0.0, -trackMm / 2),
    )
    override val raiseGroups = listOf(listOf(Wheel.LEFT), listOf(Wheel.RIGHT))
}

/** Tandem-axle caravan: both wheels of a side stand on wedges of the same step. */
data class TandemCaravan(
    val trackMm: Double,
    val axleSpacingMm: Double,
    override val hitchToAxleMm: Double,
) : Caravan {
    init {
        require(trackMm > 0 && axleSpacingMm > 0 && hitchToAxleMm > 0) { "dimensions must be positive" }
    }

    override val wheels = corners(axleSpacingMm, trackMm)
    override val raiseGroups = listOf(
        listOf(Wheel.FRONT_LEFT, Wheel.REAR_LEFT),
        listOf(Wheel.FRONT_RIGHT, Wheel.REAR_RIGHT),
    )
}

private fun corners(lengthMm: Double, widthMm: Double): Map<Wheel, Point> = mapOf(
    Wheel.FRONT_LEFT to Point(lengthMm / 2, widthMm / 2),
    Wheel.FRONT_RIGHT to Point(lengthMm / 2, -widthMm / 2),
    Wheel.REAR_LEFT to Point(-lengthMm / 2, widthMm / 2),
    Wheel.REAR_RIGHT to Point(-lengthMm / 2, -widthMm / 2),
)
