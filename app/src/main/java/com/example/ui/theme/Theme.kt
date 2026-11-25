package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NuncatDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = MoonlightText,
    primaryContainer = CrimsonDeep,
    onPrimaryContainer = MoonlightText,
    secondary = CrimsonGlow,
    onSecondary = ObsidianBg,
    secondaryContainer = CatacombElevated,
    onSecondaryContainer = MoonlightText,
    tertiary = AshMuted,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = MoonlightText,
    surface = CatacombSurface,
    onSurface = MoonlightText,
    surfaceVariant = CatacombSurfaceVariant,
    onSurfaceVariant = AshMuted,
    outline = BloodOutline,
    outlineVariant = CatacombElevated,
    error = CrimsonGlow,
    onError = ObsidianBg,
)

@Composable
fun NuncatTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NuncatDarkColorScheme,
        typography = NuncatTypography,
        content = content,
    )
}
