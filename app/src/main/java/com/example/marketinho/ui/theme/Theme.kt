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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    // Adicione as cores de contraste para o Dark Theme
    background = Color(0xFF1C1B1F), // Ou use a cor que você tinha: Color(0xFF121212)
    onBackground = Color.White, // Texto sobre o background escuro
    surface = Color(0xFF1C1B1F), // Superfície de componentes, pode ser igual ao background ou ligeiramente diferente
    onSurface = Color.White, // Texto sobre a superfície escura (para TextField)
    onPrimary = Color.Black, // Texto sobre a cor primary (Purple80)
    onSecondary = Color.Black, // Texto sobre a cor secondary (PurpleGrey80)
    onTertiary = Color.Black // Texto sobre a cor tertiary (Pink80)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    // Adicione as cores de contraste para o Light Theme
    background = Color(0xFFFFFBFE),
    onBackground = Color.Black, // Texto sobre o background claro
    surface = Color(0xFFFFFBFE),
    onSurface = Color.Black, // Texto sobre a superfície clara (para TextField)
    onPrimary = Color.White, // Texto sobre a cor primary (Purple40)
    onSecondary = Color.White, // Texto sobre a cor secondary (PurpleGrey40)
    onTertiary = Color.White // Texto sobre a cor tertiary (Pink40)
)

@Composable
fun MarketinhoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true, // Manter o dynamicColor se quiser
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Certifique-se que Typography está importado (geralmente em Type.kt)
        content = content
    )
}