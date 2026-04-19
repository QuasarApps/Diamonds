package com.example.diamonds.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.ui.R

/**
 * Supported languages with their BCP-47 codes and display names.
 */
data class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String
)

private val supportedLanguages = listOf(
    SupportedLanguage("en", "English", "English", "🇬🇧"),
    SupportedLanguage("fr", "French", "Français", "🇫🇷"),
    SupportedLanguage("es", "Spanish", "Español", "🇪🇸"),
    SupportedLanguage("pt", "Portuguese", "Português", "🇧🇷"),
    SupportedLanguage("ar", "Arabic", "العربية", "🇸🇦"),
)

@Composable
fun LanguageSelectorScreen(
    viewModel: LanguageViewModel = hiltViewModel()
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.choose_language),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            stringResource(R.string.change_app_language),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                supportedLanguages.forEachIndexed { index, lang ->
                    LanguageRow(
                        language = lang,
                        isSelected = currentLanguage == lang.code,
                        onClick = {
                            viewModel.setLanguage(lang.code)
                            // Apply locale immediately using AndroidX per-app language API
                            val localeList = LocaleListCompat.forLanguageTags(lang.code)
                            AppCompatDelegate.setApplicationLocales(localeList)
                        }
                    )
                    if (index < supportedLanguages.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "ℹ️ ${stringResource(R.string.language)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "The app language will change immediately. " +
                            "Date, time, and currency formats will adapt to your chosen locale.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(
    language: SupportedLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(language.flag, fontSize = 28.sp)
        Column(Modifier.weight(1f)) {
            Text(
                language.nativeName,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                language.displayName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
    }
}
