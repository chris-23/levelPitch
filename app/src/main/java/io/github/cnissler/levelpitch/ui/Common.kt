package io.github.cnissler.levelpitch.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.cnissler.levelpitch.LevelPitchApp
import io.github.cnissler.levelpitch.R
import io.github.cnissler.levelpitch.leveling.PhoneOrientation

/** A ViewModel built from the app container; [key] separates instances of the same class. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (LevelPitchApp) -> VM): VM {
    val app = LocalContext.current.applicationContext as LevelPitchApp
    return viewModel(key = key) { create(app) }
}

@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
    }
}

@Composable
fun OrientationPicker(selected: PhoneOrientation, enabled: Boolean, onSelect: (PhoneOrientation) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier) {
        PhoneOrientation.entries.forEachIndexed { i, o ->
            SegmentedButton(
                selected = o == selected,
                onClick = { onSelect(o) },
                shape = SegmentedButtonDefaults.itemShape(i, PhoneOrientation.entries.size),
                enabled = enabled,
            ) { Text(stringResource(o.label())) }
        }
    }
}

fun PhoneOrientation.label(): Int = when (this) {
    PhoneOrientation.TOP_TO_FRONT -> R.string.orientation_front
    PhoneOrientation.TOP_TO_LEFT -> R.string.orientation_left
    PhoneOrientation.TOP_TO_REAR -> R.string.orientation_rear
    PhoneOrientation.TOP_TO_RIGHT -> R.string.orientation_right
}

@Composable
fun ConfirmDeleteDialog(title: Int, text: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(text)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
