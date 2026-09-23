package io.github.cnissler.levelpitch.ui.pixel

import io.github.cnissler.levelpitch.leveling.Wheel
import kotlin.math.max
import kotlin.math.roundToInt

/** Fixed palette; readable on light and dark backgrounds. */
object Palette {
    val OUTLINE = 0xFF2B2D42.toInt()
    val BODY = 0xFFF4F1E8.toInt()
    val BODY_SHADE = 0xFFD9D4C5.toInt()
    val GLASS = 0xFF2E4A7D.toInt()
    val GLASS_SHINE = 0xFF8EC5FF.toInt()
    val BUMPER = 0xFF4A4E5A.toInt()
    val STRIPE = 0xFFE07A2E.toInt()
    val STRIPE_DARK = 0xFFB35A1C.toInt()
    val SOLAR = 0xFF1F3B73.toInt()
    val SOLAR_LINE = 0xFF4F74B8.toInt()
    val VENT = 0xFFB8BDC7.toInt()
    val VENT_DARK = 0xFF8A909C.toInt()
    val SKY = 0xFFA9D6F5.toInt()
    val SKY_FRAME = 0xFFE8ECF2.toInt()
    val TYRE = 0xFF1E1E1E.toInt()
    /** Lighter tyre edge, so tyres stay visible on a dark background. */
    val TYRE_EDGE = 0xFF6A6F78.toInt()
    val TREAD = 0xFF3C3C3C.toInt()
    val RIM = 0xFF6B7078.toInt()
    val HUB = 0xFFC9CED6.toInt()
    val HIGHLIGHT = 0xFF35D399.toInt()
    val HIGHLIGHT_DARK = 0xFF1E9E6E.toInt()
    val WEDGE = 0xFFF2B233.toInt()
    val WEDGE_TOP = 0xFFFFD86B.toInt()
    val WEDGE_DARK = 0xFFA66E14.toInt()
    val WEDGE_OFF = 0xFF9EA3AB.toInt()
    val WEDGE_OFF_TOP = 0xFFC3C7CE.toInt()
    val GROUND = 0xFF7A7F87.toInt()
}

enum class SpriteKind { MOTORHOME, CARAVAN_SINGLE, CARAVAN_TANDEM }

/** A top-down vehicle, front at the top, with the pixel row of each wheel's axle. */
class VehicleSprite(val image: PixelImage, val wheelRows: Map<Wheel, Int>)

const val SPRITE_WIDTH = 34

/** Top-down vehicle; wheels in [highlight] are drawn in the highlight colour. */
fun vehicleSprite(kind: SpriteKind, highlight: Set<Wheel> = emptySet()): VehicleSprite = when (kind) {
    SpriteKind.MOTORHOME -> motorhome(highlight)
    SpriteKind.CARAVAN_SINGLE -> caravan(highlight, tandem = false)
    SpriteKind.CARAVAN_TANDEM -> caravan(highlight, tandem = true)
}

private fun motorhome(highlight: Set<Wheel>): VehicleSprite {
    val img = PixelImage(SPRITE_WIDTH, 74)
    val front = 11
    val rear = 53
    val rows = mapOf(Wheel.FRONT_LEFT to front, Wheel.FRONT_RIGHT to front, Wheel.REAR_LEFT to rear, Wheel.REAR_RIGHT to rear)
    rows.forEach { (w, y) -> wheel(img, w, y, w in highlight) }

    img.roundedBox(3, 1, 30, 73, 3, Palette.BODY, Palette.OUTLINE)
    // Cab: bumper, bonnet, windscreen with a glint, mirrors.
    img.fill(6, 0, 22, 2, Palette.BUMPER)
    img.fill(5, 3, 24, 3, Palette.BODY_SHADE)
    img.fill(5, 7, 24, 5, Palette.GLASS)
    for (i in 0..3) img[8 + i, 10 - i] = Palette.GLASS_SHINE
    for (i in 0..2) img[13 + i, 10 - i] = Palette.GLASS_SHINE
    img.fill(1, 8, 2, 2, Palette.BUMPER)
    img.fill(31, 8, 2, 2, Palette.BUMPER)
    // Living area: cab/body joint, stripes, skylight, solar panel, AC unit, small skylight.
    img.fill(4, 16, 26, 1, Palette.BODY_SHADE)
    stripes(img, 18, 70)
    skylight(img, 11, 20, 12, 8)
    solar(img, 8, 30, 18, 13)
    vent(img, 12, 47, 10, 9)
    skylight(img, 14, 62, 6, 5)
    img.fill(6, 72, 22, 1, Palette.BUMPER)
    return VehicleSprite(img, rows)
}

private fun caravan(highlight: Set<Wheel>, tandem: Boolean): VehicleSprite {
    val img = PixelImage(SPRITE_WIDTH, 70)
    val rows = if (tandem) {
        mapOf(Wheel.FRONT_LEFT to 44, Wheel.FRONT_RIGHT to 44, Wheel.REAR_LEFT to 53, Wheel.REAR_RIGHT to 53)
    } else {
        mapOf(Wheel.LEFT to 47, Wheel.RIGHT to 47)
    }
    rows.forEach { (w, y) -> wheel(img, w, y, w in highlight) }

    // Drawbar: coupling head, A-frame, jockey wheel, gas locker.
    img.line(16, 3, 7, 15, Palette.BUMPER)
    img.line(17, 3, 26, 15, Palette.BUMPER)
    img.disc(17.0, 2.5, 2.2, Palette.BUMPER)
    img.fill(16, 6, 2, 4, Palette.TYRE)
    img.roundedBox(11, 10, 22, 15, 1, Palette.VENT, Palette.OUTLINE)

    img.roundedBox(3, 15, 30, 69, 4, Palette.BODY, Palette.OUTLINE)
    img.fill(7, 17, 20, 3, Palette.GLASS)
    for (i in 0..1) img[9 + i, 19 - i] = Palette.GLASS_SHINE
    stripes(img, 22, 66)
    skylight(img, 12, 24, 10, 7)
    vent(img, 14, 35, 6, 5)
    skylight(img, 14, 60, 6, 5)
    return VehicleSprite(img, rows)
}

/** A 3 × 8 tyre sticking out beside the body, centred on [axleRow]. */
private fun wheel(img: PixelImage, wheel: Wheel, axleRow: Int, highlighted: Boolean) {
    val left = wheel == Wheel.FRONT_LEFT || wheel == Wheel.REAR_LEFT || wheel == Wheel.LEFT
    val x = if (left) 0 else SPRITE_WIDTH - 3
    val tyre = if (highlighted) Palette.HIGHLIGHT else Palette.TYRE
    val tread = if (highlighted) Palette.HIGHLIGHT_DARK else Palette.TREAD
    val edge = if (highlighted) Palette.HIGHLIGHT else Palette.TYRE_EDGE
    img.fill(x, axleRow - 4, 3, 8, edge)
    img.fill(if (left) x + 1 else x, axleRow - 3, 2, 6, tyre)
    for (y in axleRow - 2..axleRow + 2 step 2) img[x + 1, y] = tread
}

private fun stripes(img: PixelImage, y0: Int, y1: Int) {
    img.fill(4, y0, 1, y1 - y0, Palette.STRIPE)
    img.fill(29, y0, 1, y1 - y0, Palette.STRIPE)
    img.fill(5, y0, 1, y1 - y0, Palette.STRIPE_DARK)
    img.fill(28, y0, 1, y1 - y0, Palette.STRIPE_DARK)
}

private fun skylight(img: PixelImage, x: Int, y: Int, w: Int, h: Int) {
    img.fill(x, y, w, h, Palette.SKY_FRAME)
    img.fill(x + 1, y + 1, w - 2, h - 2, Palette.SKY)
    img[x + 2, y + 2] = Palette.BODY
}

private fun solar(img: PixelImage, x: Int, y: Int, w: Int, h: Int) {
    img.fill(x, y, w, h, Palette.SOLAR)
    for (xx in x + 3 until x + w step 4) img.fill(xx, y, 1, h, Palette.SOLAR_LINE)
    for (yy in y + 3 until y + h step 4) img.fill(x, yy, w, 1, Palette.SOLAR_LINE)
}

private fun vent(img: PixelImage, x: Int, y: Int, w: Int, h: Int) {
    img.fill(x, y, w, h, Palette.VENT)
    for (yy in y + 1 until y + h - 1 step 2) img.fill(x + 1, yy, w - 2, 1, Palette.VENT_DARK)
}

const val BADGE_WIDTH = 40
const val BADGE_HEIGHT = 20
const val BADGE_GROUND_ROW = BADGE_HEIGHT - 1
const val BADGE_TYRE_X = 12
const val BADGE_TYRE_RADIUS = 6
private const val BADGE_MAX_STEP_PX = 8
private const val BADGE_WEDGE_PX = 20

/** Pixel height of each step, proportional to its height in mm (at least 2 px so it stays visible). */
fun badgeStepHeights(stepHeightsMm: List<Double>): List<Int> =
    stepHeightsMm.map { max(2, (it / stepHeightsMm.last() * BADGE_MAX_STEP_PX).roundToInt()) }

/**
 * Side view of a tyre on a stepped wedge (low step on the left). The wedge slides under the tyre so
 * that step [current] carries it; with no wedge the tyre stands on the ground in front of it.
 * Step [target] glows when it differs; a wedge about to be removed is greyed out.
 */
fun wedgeBadge(stepHeightsMm: List<Double>, current: Int, target: Int?): PixelImage {
    require(stepHeightsMm.isNotEmpty()) { "need at least one step" }
    val img = PixelImage(BADGE_WIDTH, BADGE_HEIGHT)
    img.fill(0, BADGE_GROUND_ROW, BADGE_WIDTH, 1, Palette.GROUND)

    val heights = badgeStepHeights(stepHeightsMm)
    val stepWidth = max(3, BADGE_WEDGE_PX / heights.size)
    val left = if (current == 0) {
        BADGE_TYRE_X + BADGE_TYRE_RADIUS + 2
    } else {
        BADGE_TYRE_X - (current - 1) * stepWidth - stepWidth / 2
    }
    val removing = target == 0 && current > 0
    heights.forEachIndexed { i, h ->
        val step = i + 1
        val x = left + i * stepWidth
        val glow = target == step && target != current
        val (body, top) = when {
            glow -> Palette.HIGHLIGHT to Palette.HIGHLIGHT
            removing -> Palette.WEDGE_OFF to Palette.WEDGE_OFF_TOP
            else -> Palette.WEDGE to Palette.WEDGE_TOP
        }
        img.fill(x, BADGE_GROUND_ROW - h, stepWidth, h, body)
        img.fill(x, BADGE_GROUND_ROW - h, stepWidth, 1, top)
        img.fill(x, BADGE_GROUND_ROW - h, 1, h, if (glow) Palette.HIGHLIGHT_DARK else Palette.WEDGE_DARK)
    }

    // Tyre: bottom row rests on the top face of the carrying step (or on the ground).
    val stand = if (current == 0) 0 else heights[current - 1]
    val bottomEdge = (BADGE_GROUND_ROW - stand).toDouble()
    val cx = BADGE_TYRE_X.toDouble()
    val cy = bottomEdge - BADGE_TYRE_RADIUS
    img.disc(cx, cy, BADGE_TYRE_RADIUS.toDouble(), Palette.TYRE_EDGE)
    img.disc(cx, cy, BADGE_TYRE_RADIUS - 1.0, Palette.TYRE)
    img.disc(cx, cy, 3.5, Palette.RIM)
    img.disc(cx, cy, 1.5, Palette.HUB)
    return img
}
