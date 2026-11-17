package com.example.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Editorial Deep Slate Teal + Warm Ivory + Terracotta Gold
val TealPrimaryLight = Color(0xFF164E5D)
val TealOnPrimaryLight = Color(0xFFFFFFFF)
val TealPrimaryContainerLight = Color(0xFFC2E8F4)
val TealOnPrimaryContainerLight = Color(0xFF06252E)

val GoldSecondaryLight = Color(0xFF9A5B13)
val GoldOnSecondaryLight = Color(0xFFFFFFFF)
val GoldSecondaryContainerLight = Color(0xFFFFDDB8)
val GoldOnSecondaryContainerLight = Color(0xFF311900)

val SageTertiaryLight = Color(0xFF3F6652)
val SageOnTertiaryLight = Color(0xFFFFFFFF)
val SageTertiaryContainerLight = Color(0xFFC1ECD3)
val SageOnTertiaryContainerLight = Color(0xFF0A2116)

val WarmPaperBackgroundLight = Color(0xFFFAF8F3)
val WarmPaperOnBackgroundLight = Color(0xFF191C1D)
val WarmPaperSurfaceLight = Color(0xFFFAF8F3)
val WarmPaperOnSurfaceLight = Color(0xFF191C1D)
val WarmPaperSurfaceVariantLight = Color(0xFFDFE3E6)
val WarmPaperOnSurfaceVariantLight = Color(0xFF42484A)
val WarmPaperSurfaceContainerLowLight = Color(0xFFF4F1EA)
val WarmPaperSurfaceContainerLight = Color(0xFFEEEAE1)
val WarmPaperSurfaceContainerHighLight = Color(0xFFE7E2D7)
val WarmOutlineLight = Color(0xFF72787B)
val WarmOutlineVariantLight = Color(0xFFC2C7CA)

val TealPrimaryDark = Color(0xFF8AD0E3)
val TealOnPrimaryDark = Color(0xFF003542)
val TealPrimaryContainerDark = Color(0xFF0C3B48)
val TealOnPrimaryContainerDark = Color(0xFFC2E8F4)

val GoldSecondaryDark = Color(0xFFF6BE78)
val GoldOnSecondaryDark = Color(0xFF4E2A00)
val GoldSecondaryContainerDark = Color(0xFF6F3F00)
val GoldOnSecondaryContainerDark = Color(0xFFFFDDB8)

val SageTertiaryDark = Color(0xFFA5D1B9)
val SageOnTertiaryDark = Color(0xFF0E3725)
val SageTertiaryContainerDark = Color(0xFF264E3B)
val SageOnTertiaryContainerDark = Color(0xFFC1ECD3)

val InkBackgroundDark = Color(0xFF111415)
val InkOnBackgroundDark = Color(0xFFE2E5E7)
val InkSurfaceDark = Color(0xFF111415)
val InkOnSurfaceDark = Color(0xFFE2E5E7)
val InkSurfaceVariantDark = Color(0xFF42484A)
val InkOnSurfaceVariantDark = Color(0xFFC2C7CA)
val InkSurfaceContainerLowDark = Color(0xFF191D1E)
val InkSurfaceContainerDark = Color(0xFF1E2224)
val InkSurfaceContainerHighDark = Color(0xFF282C2E)
val InkOutlineDark = Color(0xFF8C9295)
val InkOutlineVariantDark = Color(0xFF42484A)

@Immutable
data class QalamExtendedColors(
    val sepiaPaper: Color,
    val sepiaInk: Color,
    val nightPaper: Color,
    val nightInk: Color,
    val highlightYellow: Color,
    val highlightMint: Color,
    val highlightSky: Color,
    val highlightPink: Color,
    val tabStripSurface: Color,
    val tabActiveSurface: Color,
    val paneFocusedStroke: Color,
)

val LightQalamExtendedColors = QalamExtendedColors(
    sepiaPaper = Color(0xFFF5ECD8),
    sepiaInk = Color(0xFF3B2E1E),
    nightPaper = Color(0xFF14171A),
    nightInk = Color(0xFFDCE1E5),
    highlightYellow = Color(0xB3FFF176),
    highlightMint = Color(0xB3A5D6A7),
    highlightSky = Color(0xB381D4FA),
    highlightPink = Color(0xB3F48FB1),
    tabStripSurface = Color(0xFFEAE5DA),
    tabActiveSurface = Color(0xFFFAF8F3),
    paneFocusedStroke = Color(0xFF164E5D),
)

val DarkQalamExtendedColors = QalamExtendedColors(
    sepiaPaper = Color(0xFF342C21),
    sepiaInk = Color(0xFFEADBC3),
    nightPaper = Color(0xFF0E1113),
    nightInk = Color(0xFFD8DEE3),
    highlightYellow = Color(0x99FBC02D),
    highlightMint = Color(0x9966BB6A),
    highlightSky = Color(0x994FC3F7),
    highlightPink = Color(0x99F06292),
    tabStripSurface = Color(0xFF171A1C),
    tabActiveSurface = Color(0xFF252A2C),
    paneFocusedStroke = Color(0xFF8AD0E3),
)
