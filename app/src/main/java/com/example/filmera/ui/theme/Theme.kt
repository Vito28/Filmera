package com.example.filmera.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LightColorScheme: ColorScheme = lightColorScheme(
  primary = PinkPrimary,
  onPrimary = Color.White,
  secondary = PinkSecondary,
  onSecondary = Color.White,
  background = PinkBackground,
  onBackground = Color.Black,
  surface = WhiteSurface,
  onSurface = Color.Black
)

val DarkColorScheme: ColorScheme = darkColorScheme(
  primary = PinkDarkPrimary,
  onPrimary = Color.Black,
  secondary = PinkDarkSecondary,
  onSecondary = Color.White,
  background = DarkBackground,
  onBackground = Color.White,
  surface = DarkSurface,
  onSurface = Color.White
)

@Composable
fun FilmeraTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
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
    typography = Typography,
    content = content
  )
}
