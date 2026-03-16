package com.skipmoney.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors =
    lightColorScheme(
        primary = PineGreen,
        onPrimary = WarmCream,
        primaryContainer = Mist,
        onPrimaryContainer = PineGreen,
        secondary = MossGreen,
        background = WarmCream,
        surface = ColorTokens.Surface,
        surfaceVariant = Mist,
        onSurface = ColorTokens.OnSurface,
        onSurfaceVariant = ColorTokens.OnSurfaceVariant,
        tertiary = Clay,
    )

private val DarkColors =
    darkColorScheme(
        primary = Mist,
        onPrimary = PineGreen,
        primaryContainer = PineGreen,
        onPrimaryContainer = WarmCream,
        secondary = MossGreen,
        background = ColorTokens.DarkBackground,
        surface = ColorTokens.DarkSurface,
        surfaceVariant = ColorTokens.DarkSurfaceVariant,
        onSurface = WarmCream,
        onSurfaceVariant = Mist,
        tertiary = Clay,
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
    val Surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    val OnSurface = androidx.compose.ui.graphics.Color(0xFF183028)
    val OnSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF597267)
    val DarkBackground = androidx.compose.ui.graphics.Color(0xFF101614)
    val DarkSurface = androidx.compose.ui.graphics.Color(0xFF17211E)
    val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF23302B)
}
