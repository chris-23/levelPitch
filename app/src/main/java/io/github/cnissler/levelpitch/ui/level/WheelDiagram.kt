package io.github.cnissler.levelpitch.ui.level

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.cnissler.levelpitch.R
import io.github.cnissler.levelpitch.leveling.Caravan
import io.github.cnissler.levelpitch.leveling.Equipment
import io.github.cnissler.levelpitch.leveling.Motorhome
import io.github.cnissler.levelpitch.leveling.SingleAxleCaravan
import io.github.cnissler.levelpitch.leveling.TandemCaravan
import io.github.cnissler.levelpitch.leveling.Vehicle
import io.github.cnissler.levelpitch.leveling.WedgeState
import io.github.cnissler.levelpitch.leveling.Wheel
import io.github.cnissler.levelpitch.ui.profiles.formatDecimal

/**
 * Top-down vehicle, front at the top. Each wheel shows its wedge step and, if [targets] differ,
 * the recommended one; tapping a wheel sets the step it stands on.
 */
@Composable
fun WheelDiagram(
    vehicle: Vehicle,
    equipment: Equipment,
    current: WedgeState,
    targets: WedgeState?,
    onSetStep: (Wheel, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    @Composable
    fun axle(left: Wheel, right: Wheel) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        WheelChip(left, equipment, current[left] ?: 0, targets?.get(left), onSetStep)
        WheelChip(right, equipment, current[right] ?: 0, targets?.get(right), onSetStep)
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.diagram_front), style = MaterialTheme.typography.labelMedium)
        if (vehicle is Caravan) {
            Text(stringResource(R.string.diagram_hitch), style = MaterialTheme.typography.labelMedium)
            Box(Modifier.width(2.dp).height(24.dp).border(1.dp, MaterialTheme.colorScheme.outline))
        }
        Column(
            Modifier
                .width(280.dp)
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .padding(8.dp),
        ) {
            when (vehicle) {
                is Motorhome -> {
                    axle(Wheel.FRONT_LEFT, Wheel.FRONT_RIGHT)
                    Spacer(Modifier.height(72.dp))
                    axle(Wheel.REAR_LEFT, Wheel.REAR_RIGHT)
                }
                is SingleAxleCaravan -> {
                    Spacer(Modifier.height(96.dp))
                    axle(Wheel.LEFT, Wheel.RIGHT)
                    Spacer(Modifier.height(24.dp))
                }
                is TandemCaravan -> {
                    Spacer(Modifier.height(72.dp))
                    axle(Wheel.FRONT_LEFT, Wheel.FRONT_RIGHT)
                    Spacer(Modifier.height(4.dp))
                    axle(Wheel.REAR_LEFT, Wheel.REAR_RIGHT)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun WheelChip(wheel: Wheel, equipment: Equipment, step: Int, target: Int?, onSetStep: (Wheel, Int) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val now = stepShort(step)
    val next = target?.takeIf { it != step }?.let { if (it == 0) stringResource(R.string.to_none) else stringResource(R.string.to_step, it) }
    val description = stringResource(R.string.wheel_description, stringResource(wheel.label()), listOfNotNull(now, next).joinToString(" "))
    Box {
        OutlinedCard(
            onClick = { menu = true },
            modifier = Modifier
                .width(96.dp)
                .semantics { contentDescription = description },
        ) {
            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(now, style = MaterialTheme.typography.bodyMedium)
                if (next != null) {
                    Text(next, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            equipment.steps.forEach { s ->
                DropdownMenuItem(
                    text = { Text(stepOption(s, equipment)) },
                    onClick = {
                        menu = false
                        onSetStep(wheel, s)
                    },
                )
            }
        }
    }
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
