package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = GeoPrimary,
    secondary = GeoSecondary,
    tertiary = GeoAccent,
    background = Color(0xFF0F1A16),
    surface = Color(0xFF13241F),
    onPrimary = Color(0xFF0F1A16),
    onSecondary = Color.White,
    onBackground = Color(0xFFECF3F0),
    onSurface = Color(0xFFECF3F0)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GeoPrimary,
    secondary = GeoSecondary,
    tertiary = GeoAccent,
    background = GeoBackground,
    surface = GeoSurface,
    onPrimary = Color.White,
    onSecondary = GeoTextDark,
    onBackground = GeoTextDark,
    onSurface = GeoTextDark,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Enforce the hand-crafted look by default
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
