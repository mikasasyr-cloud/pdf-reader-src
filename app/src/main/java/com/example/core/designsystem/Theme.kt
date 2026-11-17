package com.example.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class ReadingFilterMode {
    NORMAL,
    SEPIA,
    NIGHT,
}

private val QalamLightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = TealOnPrimaryLight,
    primaryContainer = TealPrimaryContainerLight,
    onPrimaryContainer = TealOnPrimaryContainerLight,
    secondary = GoldSecondaryLight,
    onSecondary = GoldOnSecondaryLight,
    secondaryContainer = GoldSecondaryContainerLight,
    onSecondaryContainer = GoldOnSecondaryContainerLight,
    tertiary = SageTertiaryLight,
    onTertiary = SageOnTertiaryLight,
    tertiaryContainer = SageTertiaryContainerLight,
    onTertiaryContainer = SageOnTertiaryContainerLight,
    background = WarmPaperBackgroundLight,
    onBackground = WarmPaperOnBackgroundLight,
    surface = WarmPaperSurfaceLight,
    onSurface = WarmPaperOnSurfaceLight,
    surfaceVariant = WarmPaperSurfaceVariantLight,
    onSurfaceVariant = WarmPaperOnSurfaceVariantLight,
    surfaceContainerLow = WarmPaperSurfaceContainerLowLight,
    surfaceContainer = WarmPaperSurfaceContainerLight,
    surfaceContainerHigh = WarmPaperSurfaceContainerHighLight,
    outline = WarmOutlineLight,
    outlineVariant = WarmOutlineVariantLight,
)

private val QalamDarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealPrimaryContainerDark,
    onPrimaryContainer = TealOnPrimaryContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = GoldOnSecondaryDark,
    secondaryContainer = GoldSecondaryContainerDark,
    onSecondaryContainer = GoldOnSecondaryContainerDark,
    tertiary = SageTertiaryDark,
    onTertiary = SageOnTertiaryDark,
    tertiaryContainer = SageTertiaryContainerDark,
    onTertiaryContainer = SageOnTertiaryContainerDark,
    background = InkBackgroundDark,
    onBackground = InkOnBackgroundDark,
    surface = InkSurfaceDark,
    onSurface = InkOnSurfaceDark,
    surfaceVariant = InkSurfaceVariantDark,
    onSurfaceVariant = InkOnSurfaceVariantDark,
    surfaceContainerLow = InkSurfaceContainerLowDark,
    surfaceContainer = InkSurfaceContainerDark,
    surfaceContainerHigh = InkSurfaceContainerHighDark,
    outline = InkOutlineDark,
    outlineVariant = InkOutlineVariantDark,
)

val LocalQalamExtendedColors = staticCompositionLocalOf { LightQalamExtendedColors }

object QalamThemeExtras {
    val colors: QalamExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalQalamExtendedColors.current
}

@Composable
fun QalamTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> QalamDarkColorScheme
        else -> QalamLightColorScheme
    }

    val extendedColors = if (isDark) DarkQalamExtendedColors else LightQalamExtendedColors

    CompositionLocalProvider(LocalQalamExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = QalamTypography,
            shapes = QalamShapes,
            content = content,
        )
    }
}
