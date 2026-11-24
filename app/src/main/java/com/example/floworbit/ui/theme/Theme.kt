package com.example.floworbit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// FlowOrbit is a single branded look (navy/teal/mint) rather than a system-adaptive theme,
// so the palette reads identically on every device regardless of OS light/dark mode or wallpaper.
// Values are matched to the provided screen mockups.
private val FlowOrbitColorScheme = darkColorScheme(
    primary = Teal,
    onPrimary = OnAccent,
    primaryContainer = NavyWell,
    onPrimaryContainer = Teal,

    secondary = Mint,
    onSecondary = OnAccent,
    secondaryContainer = TealDeep,
    onSecondaryContainer = Mint,

    tertiary = Mauve,
    onTertiary = OnAccent,
    tertiaryContainer = PeachSurface,
    onTertiaryContainer = Mauve,

    background = NavyDeep,
    onBackground = TextPrimary,

    surface = NavyDeep,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Teal,
    surfaceContainerLowest = NavyWell,
    surfaceContainerLow = NavyWell,
    surfaceContainer = CardSurface,
    surfaceContainerHigh = CardSurface,
    surfaceContainerHighest = NavyPill,

    error = Peach,
    onError = OnAccent,
    errorContainer = PeachSurface,
    onErrorContainer = PeachText,

    outline = CardBorder,
    outlineVariant = DividerColor,
)

@Composable
fun FlowOrbitTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FlowOrbitColorScheme,
        typography = Typography,
        content = content
    )
}
