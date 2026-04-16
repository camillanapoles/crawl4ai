package com.crawl4ai.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Crawl4AI brand colours
private val SeedBlue = Color(0xFF4FC3F7)
private val SeedBlueDark = Color(0xFF0288D1)

private val DarkColorScheme = darkColorScheme(
    primary = SeedBlue,
    onPrimary = Color(0xFF003353),
    primaryContainer = Color(0xFF004A72),
    secondary = Color(0xFFB2EBF2),
    background = Color(0xFF1B1B1F),
    surface = Color(0xFF1B1B1F),
)

private val LightColorScheme = lightColorScheme(
    primary = SeedBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCE5FF),
    secondary = Color(0xFF006874),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
)

/**
 * Crawl4AI Material 3 theme.
 *
 * Uses dynamic colour on Android 12+ (API 31); falls back to the brand
 * colour scheme on older versions.
 */
@Composable
fun Crawl4AITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
