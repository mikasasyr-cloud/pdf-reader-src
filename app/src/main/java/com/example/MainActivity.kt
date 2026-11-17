package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material.icons.outlined.ViewSidebar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.core.common.AppLanguage
import com.example.core.common.AppLocaleManager
import com.example.core.common.AppLogger
import com.example.core.designsystem.AppLayoutType
import com.example.core.designsystem.QalamTheme
import com.example.core.designsystem.ThemeMode
import com.example.core.designsystem.layoutTypeForWidth
import com.example.core.designsystem.rememberAppLayoutType
import com.example.core.designsystem.sidebarDockSideFor

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)

        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
            val currentLanguage = remember { AppLocaleManager.getCurrentLanguage() }
            val layoutType = rememberAppLayoutType(this)

            QalamTheme(themeMode = themeMode) {
                Phase1FoundationScreen(
                    layoutType = layoutType,
                    themeMode = themeMode,
                    currentLanguage = currentLanguage,
                    onThemeModeChange = { themeMode = it },
                    onLanguageChange = { target ->
                        if (target != currentLanguage) {
                            AppLocaleManager.setLanguage(target)
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.host == "oauth2redirect") {
            AppLogger.d("MainActivity", "OAuth redirect received: ${data.scheme}://${data.host}")
            val container = (application as QalamApplication).container
            lifecycleScope.launch {
                container.cloudRepository.handleOAuthRedirect(data)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Phase1FoundationScreen(
    layoutType: AppLayoutType,
    themeMode: ThemeMode,
    currentLanguage: AppLanguage,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val dockSide = sidebarDockSideFor(layoutDirection)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.phase1_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Text(
                    text = stringResource(R.string.phase1_welcome_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = stringResource(R.string.phase1_welcome_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val layoutLabel = when (layoutType) {
                        AppLayoutType.COMPACT -> stringResource(R.string.phase1_layout_compact)
                        AppLayoutType.MEDIUM -> stringResource(R.string.phase1_layout_medium)
                        AppLayoutType.EXPANDED -> stringResource(R.string.phase1_layout_expanded)
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text(layoutLabel) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Splitscreen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("Sidebar: ${dockSide.name}") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.ViewSidebar,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.sidebar_section_cloud)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.CloudQueue,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_language),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = currentLanguage == AppLanguage.ENGLISH,
                                onClick = { onLanguageChange(AppLanguage.ENGLISH) },
                                label = { Text(stringResource(R.string.language_english)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                modifier = Modifier.testTag("language_en_chip"),
                            )
                            FilterChip(
                                selected = currentLanguage == AppLanguage.ARABIC,
                                onClick = { onLanguageChange(AppLanguage.ARABIC) },
                                label = { Text(stringResource(R.string.language_arabic)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                modifier = Modifier.testTag("language_ar_chip"),
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(R.string.settings_appearance),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = themeMode == ThemeMode.SYSTEM,
                                onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                                label = { Text(stringResource(R.string.theme_system)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.SettingsBrightness,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                modifier = Modifier.testTag("theme_system_chip"),
                            )
                            FilterChip(
                                selected = themeMode == ThemeMode.LIGHT,
                                onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                                label = { Text(stringResource(R.string.theme_light)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LightMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                modifier = Modifier.testTag("theme_light_chip"),
                            )
                            FilterChip(
                                selected = themeMode == ThemeMode.DARK,
                                onClick = { onThemeModeChange(ThemeMode.DARK) },
                                label = { Text(stringResource(R.string.theme_dark)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                modifier = Modifier.testTag("theme_dark_chip"),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Phone LTR", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Phase1PhonePreview() {
    QalamTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
        BoxWithConstraints {
            Phase1FoundationScreen(
                layoutType = layoutTypeForWidth(maxWidth),
                themeMode = ThemeMode.LIGHT,
                currentLanguage = AppLanguage.ENGLISH,
                onThemeModeChange = {},
                onLanguageChange = {},
            )
        }
    }
}

@Preview(name = "Tablet Expanded", showBackground = true, widthDp = 1024, heightDp = 768)
@Composable
private fun Phase1TabletPreview() {
    QalamTheme(themeMode = ThemeMode.DARK, dynamicColor = false) {
        BoxWithConstraints {
            Phase1FoundationScreen(
                layoutType = layoutTypeForWidth(maxWidth),
                themeMode = ThemeMode.DARK,
                currentLanguage = AppLanguage.ENGLISH,
                onThemeModeChange = {},
                onLanguageChange = {},
            )
        }
    }
}

@Preview(
    name = "Arabic RTL",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    locale = "ar",
)
@Composable
private fun Phase1ArabicRtlPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        QalamTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
            BoxWithConstraints {
                Phase1FoundationScreen(
                    layoutType = layoutTypeForWidth(maxWidth),
                    themeMode = ThemeMode.LIGHT,
                    currentLanguage = AppLanguage.ARABIC,
                    onThemeModeChange = {},
                    onLanguageChange = {},
                )
            }
        }
    }
}
