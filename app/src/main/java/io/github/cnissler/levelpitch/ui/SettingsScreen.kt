package io.github.cnissler.levelpitch.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import io.github.cnissler.levelpitch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var language by remember { mutableStateOf(AppLanguage.fromTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())) }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings)) }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
            AppLanguage.entries.forEach { l ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = language == l, role = Role.RadioButton) {
                            language = l
                            // Stored by AppCompat; the activity is recreated in the new language.
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(l.tag))
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = language == l, onClick = null)
                    Text(stringResource(l.label()), modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp))
                }
            }
        }
    }
}

private fun AppLanguage.label(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.ENGLISH -> R.string.language_english
    AppLanguage.GERMAN -> R.string.language_german
}
