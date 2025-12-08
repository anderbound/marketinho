package com.example.marketinho.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ========== TEMA CLARO DO MARKETINHO ==========
private val LightColorScheme = lightColorScheme(
    primary = MarketGreen,              // Cor principal (botões, etc)
    onPrimary = Color.White,            // Texto sobre a cor principal
    primaryContainer = MarketGreenLight, // Container da cor principal
    onPrimaryContainer = MarketGreenDark,

    secondary = MarketBlue,              // Cor secundária
    onSecondary = Color.White,
    secondaryContainer = MarketBlueLight,
    onSecondaryContainer = MarketBlueDark,

    tertiary = MarketOrange,             // Cor terciária (destaques)
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = MarketOrangeDark,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFB71C1C),

    background = Gray50,                 // Background do app
    onBackground = Gray900,              // Texto sobre o background
    surface = Color.White,               // Superfície de cards
    onSurface = Gray900,                 // Texto sobre superfícies
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,

    outline = Gray300,
    outlineVariant = Gray200,
    scrim = Color.Black.copy(alpha = 0.32f),
    inverseSurface = Gray800,
    inverseOnSurface = Gray50,
    inversePrimary = MarketGreenLight,
    surfaceTint = MarketGreen
)

// ========== TEMA ESCURO DO MARKETINHO ==========
private val DarkColorScheme = darkColorScheme(
    primary = MarketGreenLight,          // Cor principal (mais clara no escuro)
    onPrimary = Gray900,
    primaryContainer = MarketGreenDark,
    onPrimaryContainer = MarketGreenLight,

    secondary = MarketBlueLight,
    onSecondary = Gray900,
    secondaryContainer = MarketBlueDark,
    onSecondaryContainer = MarketBlueLight,

    tertiary = MarketOrange,
    onTertiary = Gray900,
    tertiaryContainer = MarketOrangeDark,
    onTertiaryContainer = Color(0xFFFFCC80),

    error = Color(0xFFEF5350),
    onError = Gray900,
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Color(0xFFFFCDD2),

    background = Gray900,
    onBackground = Gray50,
    surface = Gray800,
    onSurface = Gray50,
    surfaceVariant = Gray700,
    onSurfaceVariant = Gray300,

    outline = Gray600,
    outlineVariant = Gray700,
    scrim = Color.Black.copy(alpha = 0.32f),
    inverseSurface = Gray50,
    inverseOnSurface = Gray900,
    inversePrimary = MarketGreen,
    surfaceTint = MarketGreenLight
)

@Composable
fun MarketinhoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color está disponível no Android 12+
    dynamicColor: Boolean = false, // DESABILITADO para usar nossas cores
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}