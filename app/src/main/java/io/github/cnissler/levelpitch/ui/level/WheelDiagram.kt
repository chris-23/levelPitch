package io.github.cnissler.levelpitch.ui.level

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import io.github.cnissler.levelpitch.R
import io.github.cnissler.levelpitch.leveling.Equipment
import io.github.cnissler.levelpitch.leveling.Motorhome
import io.github.cnissler.levelpitch.leveling.SingleAxleCaravan
import io.github.cnissler.levelpitch.leveling.TandemCaravan
import io.github.cnissler.levelpitch.leveling.Vehicle
import io.github.cnissler.levelpitch.leveling.WedgeState
import io.github.cnissler.levelpitch.leveling.Wheel
import io.github.cnissler.levelpitch.ui.pixel.PixelImage
import io.github.cnissler.levelpitch.ui.pixel.SPRITE_WIDTH
import io.github.cnissler.levelpitch.ui.pixel.SpriteKind
import io.github.cnissler.levelpitch.ui.pixel.vehicleSprite
import io.github.cnissler.levelpitch.ui.pixel.wedgeBadge
import io.github.cnissler.levelpitch.ui.profiles.formatDecimal

private val CARD_WIDTH = 112.dp
private val GAP = 8.dp

/**
 * Pixel-art vehicle from above (front at the top) with one card per raise group beside its axle.
 * Each card shows the tyre on its wedge step and the recommended step; tapping it sets the step
 * the wheels actually stand on.
 */
@Composable
fun VehicleScene(
    vehicle: Vehicle,
    equipment: Equipment,
    current: WedgeState,
    targets: WedgeState?,
    onSetStep: (Wheel, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val changing = vehicle.wheels.keys.filterTo(mutableSetOf()) { w -> targets?.get(w)?.let { it != (current[w] ?: 0) } == true }
    val sprite = remember(vehicle.kind(), changing) { vehicleSprite(vehicle.kind(), changing) }

    ElevatedCard(modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.diagram_front), style = MaterialTheme.typography.labelMedium)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val pixel = min(5.dp, (maxWidth - CARD_WIDTH * 2 - GAP * 2) / SPRITE_WIDTH)
                val height = pixel * sprite.image.height
                Row(horizontalArrangement = Arrangement.spacedBy(GAP), modifier = Modifier.fillMaxWidth()) {
                    SideColumn(vehicle, left = true, sprite.wheelRows, pixel, height, equipment, current, targets, onSetStep)
                    PixelArt(sprite.image, pixel)
                    SideColumn(vehicle, left = false, sprite.wheelRows, pixel, height, equipment, current, targets, onSetStep)
                }
            }
        }
    }
}

@Composable
private fun SideColumn(
    vehicle: Vehicle,
    left: Boolean,
    wheelRows: Map<Wheel, Int>,
    pixel: Dp,
    height: Dp,
    equipment: Equipment,
    current: WedgeState,
    targets: WedgeState?,
    onSetStep: (Wheel, Int) -> Unit,
) {
    val leftWheels = setOf(Wheel.FRONT_LEFT, Wheel.REAR_LEFT, Wheel.LEFT)
    Box(Modifier.width(CARD_WIDTH).height(height)) {
        vehicle.raiseGroups.filter { g -> (g.first() in leftWheels) == left }.forEach { group ->
            val row = group.map { wheelRows.getValue(it) }.average()
            val wheel = group.first()
            WedgeCard(
                label = groupLabel(group),
                equipment = equipment,
                step = current[wheel] ?: 0,
                target = targets?.get(wheel),
                onSetStep = { onSetStep(wheel, it) },
                modifier = Modifier.centredAt(pixel * (row.toFloat() + 0.5f), height),
            )
        }
    }
}

/** Places the element so its vertical centre is at [centreY], kept inside [parentHeight]. */
private fun Modifier.centredAt(centreY: Dp, parentHeight: Dp) = layout { measurable, constraints ->
    val p = measurable.measure(constraints)
    val top = (centreY.roundToPx() - p.height / 2).coerceIn(0, maxOf(0, parentHeight.roundToPx() - p.height))
    layout(p.width, p.height) { p.place(0, top) }
}

@Composable
private fun WedgeCard(
    label: String,
    equipment: Equipment,
    step: Int,
    target: Int?,
    onSetStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    val next = target?.takeIf { it != step }
    val now = stepShort(step)
    val nextText = next?.let { if (it == 0) stringResource(R.string.to_none) else stringResource(R.string.to_step, it) }
    val description = stringResource(R.string.wheel_description, label, listOfNotNull(now, nextText).joinToString(" "))
    val badge = remember(equipment.stepHeightsMm, step, next) { wedgeBadge(equipment.stepHeightsMm, step, next) }
    Box(modifier) {
        OutlinedCard(onClick = { menu = true }, modifier = Modifier.width(CARD_WIDTH).semantics { contentDescription = description }) {
            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                PixelArt(badge, 2.75.dp, Modifier.padding(vertical = 4.dp))
                Text(now, style = MaterialTheme.typography.bodySmall)
                if (nextText != null) {
                    Text(nextText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            equipment.steps.forEach { s ->
                DropdownMenuItem(text = { Text(stepOption(s, equipment)) }, onClick = {
                    menu = false
                    onSetStep(s)
                })
            }
        }
    }
}

/** Draws [image] scaled up with nearest-neighbour filtering, so pixels stay crisp. */
@Composable
fun PixelArt(image: PixelImage, pixel: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(image) {
        Bitmap.createBitmap(image.pixels, image.width, image.height, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    Canvas(modifier.size(pixel * image.width, pixel * image.height)) {
        drawImage(
            bitmap,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.None,
        )
    }
}

private fun Vehicle.kind(): SpriteKind = when (this) {
    is Motorhome -> SpriteKind.MOTORHOME
    is SingleAxleCaravan -> SpriteKind.CARAVAN_SINGLE
    is TandemCaravan -> SpriteKind.CARAVAN_TANDEM
}

@Composable
private fun stepShort(step: Int): String =
    if (step == 0) stringResource(R.string.no_wedge) else stringResource(R.string.step_short, step)

@Composable
private fun stepOption(step: Int, equipment: Equipment): String =
    if (step == 0) stringResource(R.string.no_wedge) else stringResource(R.string.step_option, step, cm(equipment.heightMm(step)))

fun cm(mm: Double): String = formatDecimal(Math.round(mm) / 10.0)

fun Wheel.label(): Int = when (this) {
    Wheel.FRONT_LEFT -> R.string.wheel_front_left
    Wheel.FRONT_RIGHT -> R.string.wheel_front_right
    Wheel.REAR_LEFT -> R.string.wheel_rear_left
    Wheel.REAR_RIGHT -> R.string.wheel_rear_right
    Wheel.LEFT -> R.string.wheel_left
    Wheel.RIGHT -> R.string.wheel_right
}

/** Label of a raise group: the wheel's name, or "Left/Right wheels" for a tandem side. */
@Composable
fun groupLabel(wheels: List<Wheel>): String = when {
    wheels.size == 1 -> stringResource(wheels.single().label())
    wheels.all { it == Wheel.FRONT_LEFT || it == Wheel.REAR_LEFT } -> stringResource(R.string.wheels_left)
    else -> stringResource(R.string.wheels_right)
}
