/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.tv

import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.Typography as M3Typography
import androidx.compose.material3.darkColorScheme as m3DarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

/**
 * Overscan-safe margins recommended for TV layouts (5% of a 1080p screen).
 */
object TvDimens {
    val ScreenHorizontal = 58.dp
    val ScreenVertical = 27.dp
    val CardSpacing = 24.dp
    val RowSpacing = 32.dp
    val AppCardWidth = 200.dp
    val CategoryCardWidth = 260.dp
}

private const val TV_TYPE_SCALE = 1.25f

private fun TextStyle.scaled(factor: Float) = copy(
    fontSize = fontSize * factor,
    lineHeight = lineHeight * factor
)

private fun M3Typography.scaled(factor: Float) = M3Typography(
    displayLarge = displayLarge.scaled(factor),
    displayMedium = displayMedium.scaled(factor),
    displaySmall = displaySmall.scaled(factor),
    headlineLarge = headlineLarge.scaled(factor),
    headlineMedium = headlineMedium.scaled(factor),
    headlineSmall = headlineSmall.scaled(factor),
    titleLarge = titleLarge.scaled(factor),
    titleMedium = titleMedium.scaled(factor),
    titleSmall = titleSmall.scaled(factor),
    bodyLarge = bodyLarge.scaled(factor),
    bodyMedium = bodyMedium.scaled(factor),
    bodySmall = bodySmall.scaled(factor),
    labelLarge = labelLarge.scaled(factor),
    labelMedium = labelMedium.scaled(factor),
    labelSmall = labelSmall.scaled(factor)
)

/**
 * 10-foot typography, sized for reading from a couch.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
private val TvTypography = Typography(
    displaySmall = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 22.sp),
    bodyMedium = TextStyle(fontSize = 20.sp),
    bodySmall = TextStyle(fontSize = 18.sp),
    labelLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 20.sp),
    labelSmall = TextStyle(fontSize = 18.sp)
)

/**
 * TV counterpart of [com.aurora.store.compose.theme.AuroraTheme]. TV is always dark.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTheme(content: @Composable () -> Unit) {
    // Screens that still use phone (Material 3) composables read this theme, so give them the
    // same dark palette and larger type instead of the phone defaults.
    M3MaterialTheme(
        colorScheme = m3DarkColorScheme(
            primary = Color(0xFF8AB4F8),
            onPrimary = Color(0xFF062E6F),
            primaryContainer = Color(0xFF1F3A6B),
            onPrimaryContainer = Color(0xFFD6E3FF),
            surface = Color(0xFF121317),
            onSurface = Color(0xFFE4E2E6),
            surfaceVariant = Color(0xFF24262C),
            onSurfaceVariant = Color(0xFFC4C6D0),
            background = Color(0xFF0C0D10),
            onBackground = Color(0xFFE4E2E6)
        ),
        typography = M3Typography().scaled(TV_TYPE_SCALE)
    ) {
        TvTypographyTheme(content)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvTypographyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF8AB4F8),
            onPrimary = Color(0xFF062E6F),
            primaryContainer = Color(0xFF1F3A6B),
            onPrimaryContainer = Color(0xFFD6E3FF),
            surface = Color(0xFF121317),
            onSurface = Color(0xFFE4E2E6),
            surfaceVariant = Color(0xFF24262C),
            onSurfaceVariant = Color(0xFFC4C6D0),
            background = Color(0xFF0C0D10),
            onBackground = Color(0xFFE4E2E6)
        ),
        typography = TvTypography,
        content = content
    )
}
