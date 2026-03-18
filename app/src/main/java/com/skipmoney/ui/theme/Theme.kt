package com.skipmoney.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors =
    lightColorScheme(
        primary = RoyalNavy,
        onPrimary = WarmIvory,
        primaryContainer = DeepNavy,
        onPrimaryContainer = WarmIvory,
        secondary = SoftGold,
        onSecondary = RoyalNavy,
        background = WarmIvory,
        surface = ColorTokens.Surface,
        surfaceVariant = PaleGold,
        onSurface = ColorTokens.OnSurface,
        onSurfaceVariant = ColorTokens.OnSurfaceVariant,
        tertiary = SoftGold,
        onTertiary = RoyalNavy,
    )

private val DarkColors =
    darkColorScheme(
        primary = PaleGold,
        onPrimary = RoyalNavy,
        primaryContainer = RoyalNavy,
        onPrimaryContainer = WarmIvory,
        secondary = SoftGold,
        onSecondary = RoyalNavy,
        background = ColorTokens.DarkBackground,
        surface = ColorTokens.DarkSurface,
        surfaceVariant = ColorTokens.DarkSurfaceVariant,
        onSurface = WarmIvory,
        onSurfaceVariant = PaleGold,
        tertiary = SoftGold,
        onTertiary = RoyalNavy,
    )

@Composable
fun SkipMoneyTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SkipMoneyTypography,
        content = content,
    )
}

private object ColorTokens {
    val Surface = androidx.compose.ui.graphics.Color(0xFFFFFCF6)
    val OnSurface = androidx.compose.ui.graphics.Color(0xFF1B2330)
    val OnSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF5F6671)
    val DarkBackground = androidx.compose.ui.graphics.Color(0xFF0E1627)
    val DarkSurface = androidx.compose.ui.graphics.Color(0xFF152033)
    val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF1C2940)
}
