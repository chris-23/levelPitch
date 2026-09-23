package io.github.cnissler.levelpitch.ui.pixel

import kotlin.math.hypot
import kotlin.math.roundToInt

/** Extra colours for the tutorial illustrations. */
object TutorialPalette {
    val WOOD = 0xFFC9975B.toInt()
    val WOOD_DARK = 0xFFA87843.toInt()
    val CUPBOARD = 0xFF6B4A2E.toInt()
    val CUPBOARD_EDGE = 0xFF4A3220.toInt()
    val PHONE = 0xFF22252E.toInt()
    val SCREEN = 0xFF2E4A7D.toInt()
    val CHOCK = 0xFFD94A3A.toInt()
    val CHOCK_DARK = 0xFF9E2F24.toInt()
    val VIAL_LIGHT = 0xFFA8F0CF.toInt()
    val BUBBLE = 0xFFF2FFF8.toInt()
}

const val LEVEL_SIZE = 33
private const val LEVEL_CENTRE = LEVEL_SIZE / 2
/** How far the bubble can travel from the centre, in pixels. */
const val BUBBLE_TRAVEL = 11
/** Tilt at which the bubble reaches the rim. */
const val BUBBLE_FULL_SCALE_DEG = 5.0

/**
 * Bubble offset in pixels for a phone tilted by [rightEdgeUpDeg] (right edge raised) and
 * [topEdgeUpDeg] (top edge raised), as on screen: the bubble floats to the high side, and screen y
 * grows downwards. Clamped to the rim.
 */
fun bubbleOffset(rightEdgeUpDeg: Double, topEdgeUpDeg: Double): Pair<Int, Int> {
    var dx = rightEdgeUpDeg / BUBBLE_FULL_SCALE_DEG * BUBBLE_TRAVEL
    var dy = -topEdgeUpDeg / BUBBLE_FULL_SCALE_DEG * BUBBLE_TRAVEL
    val r = hypot(dx, dy)
    if (r > BUBBLE_TRAVEL) {
        dx *= BUBBLE_TRAVEL / r
        dy *= BUBBLE_TRAVEL / r
    }
    return dx.roundToInt() to dy.roundToInt()
}

/** A round spirit level seen from above, bubble offset by ([dx], [dy]); the centre ring glows when level. */
fun bullseyeLevel(dx: Int, dy: Int): PixelImage {
    val img = PixelImage(LEVEL_SIZE, LEVEL_SIZE)
    val c = LEVEL_CENTRE + 0.5
    img.disc(c, c, 16.4, Palette.OUTLINE)
    img.disc(c, c, 15.2, Palette.HIGHLIGHT)
    img.disc(c, c, 13.0, TutorialPalette.VIAL_LIGHT)
    img.disc(c, c, 12.0, Palette.HIGHLIGHT)
    val level = hypot(dx.toDouble(), dy.toDouble()) <= 1.0
    img.disc(c, c, 5.6, if (level) TutorialPalette.BUBBLE else Palette.OUTLINE)
    img.disc(c, c, 4.6, Palette.HIGHLIGHT)
    img.disc(c + dx, c + dy, 3.6, TutorialPalette.BUBBLE)
    img[LEVEL_CENTRE + dx - 1, LEVEL_CENTRE + dy - 2] = Palette.HUB
    return img
}

/** A phone lying screen-up on a wooden floor, top edge pushed against a cupboard. */
fun phonePlacementArt(): PixelImage {
    val img = PixelImage(40, 30)
    for (y in 0 until 30) for (x in 0 until 40) {
        img[x, y] = if ((x + (y / 5) * 7) % 13 == 0) TutorialPalette.WOOD_DARK else TutorialPalette.WOOD
    }
    img.fill(0, 0, 40, 6, TutorialPalette.CUPBOARD)
    img.fill(0, 5, 40, 1, TutorialPalette.CUPBOARD_EDGE)
    img.roundedBox(13, 6, 26, 28, 2, TutorialPalette.PHONE, Palette.OUTLINE)
    img.fill(15, 9, 10, 17, TutorialPalette.SCREEN)
    img.fill(18, 7, 4, 1, Palette.RIM)
    for (i in 0..3) img[17 + i, 13 - i] = Palette.GLASS_SHINE
    return img
}

/** A tyre with a red wheel chock in front of it. */
fun chockArt(): PixelImage {
    val img = PixelImage(36, 20)
    img.fill(0, 19, 36, 1, Palette.GROUND)
    img.disc(14.0, 11.0, 8.0, Palette.TYRE_EDGE)
    img.disc(14.0, 11.0, 7.0, Palette.TYRE)
    img.disc(14.0, 11.0, 4.0, Palette.RIM)
    img.disc(14.0, 11.0, 1.8, Palette.HUB)
    // Chock: a right triangle leaning against the tyre's front.
    for (i in 0..8) {
        img.fill(21, 18 - i, 1 + (8 - i), 1, if (i == 0) TutorialPalette.CHOCK_DARK else TutorialPalette.CHOCK)
    }
    return img
}
