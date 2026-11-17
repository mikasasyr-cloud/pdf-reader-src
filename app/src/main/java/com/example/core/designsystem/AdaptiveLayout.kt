package com.example.core.designsystem

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class AppLayoutType {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

enum class SidebarDockSide {
    LEFT,
    RIGHT,
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberAppLayoutType(activity: Activity): AppLayoutType {
    val sizeClass = calculateWindowSizeClass(activity)
    return when (sizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> AppLayoutType.COMPACT
        WindowWidthSizeClass.Medium -> AppLayoutType.MEDIUM
        WindowWidthSizeClass.Expanded -> AppLayoutType.EXPANDED
        else -> AppLayoutType.COMPACT
    }
}

fun layoutTypeForWidth(availableWidth: Dp): AppLayoutType = when {
    availableWidth < 600.dp -> AppLayoutType.COMPACT
    availableWidth < 840.dp -> AppLayoutType.MEDIUM
    else -> AppLayoutType.EXPANDED
}

@Composable
fun isRtlLayout(): Boolean = LocalLayoutDirection.current == LayoutDirection.Rtl

fun sidebarDockSideFor(layoutDirection: LayoutDirection): SidebarDockSide =
    if (layoutDirection == LayoutDirection.Rtl) SidebarDockSide.RIGHT else SidebarDockSide.LEFT

/**
 * Orders a two-page facing spread according to book binding direction:
 * - LTR (English): first page on the left, second page on the right.
 * - RTL (Arabic): first page on the right, second page on the left.
 */
fun <T> bookSpreadPair(
    firstPage: T,
    secondPage: T?,
    layoutDirection: LayoutDirection,
): Pair<T?, T?> = if (layoutDirection == LayoutDirection.Rtl) {
    secondPage to firstPage
} else {
    firstPage to secondPage
}
