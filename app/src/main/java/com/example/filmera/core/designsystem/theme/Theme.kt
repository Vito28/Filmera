package com.example.filmera.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val FilmeraDarkColorScheme = darkColorScheme(
  primary = FilmeraColors.Neon,
  onPrimary = Color(0xFF172600),
  primaryContainer = FilmeraColors.NeonContainer,
  onPrimaryContainer = FilmeraColors.NeonSoft,
  inversePrimary = Color(0xFF426900),
  secondary = FilmeraColors.Secondary,
  onSecondary = Color(0xFF1B2618),
  secondaryContainer = FilmeraColors.SecondaryContainer,
  onSecondaryContainer = Color(0xFFD4E8CD),
  tertiary = FilmeraColors.Tertiary,
  onTertiary = Color(0xFF003921),
  tertiaryContainer = FilmeraColors.TertiaryContainer,
  onTertiaryContainer = Color(0xFFB7F1CD),
  background = FilmeraColors.Background,
  onBackground = FilmeraColors.OnBackground,
  surface = FilmeraColors.Surface,
  onSurface = FilmeraColors.OnBackground,
  surfaceVariant = FilmeraColors.SurfaceHigh,
  onSurfaceVariant = FilmeraColors.OnSurfaceMuted,
  surfaceTint = FilmeraColors.Neon,
  surfaceDim = FilmeraColors.SurfaceLowest,
  surfaceBright = FilmeraColors.SurfaceHighest,
  surfaceContainerLowest = FilmeraColors.SurfaceLowest,
  surfaceContainerLow = FilmeraColors.SurfaceLow,
  surfaceContainer = FilmeraColors.SurfaceContainer,
  surfaceContainerHigh = FilmeraColors.SurfaceHigh,
  surfaceContainerHighest = FilmeraColors.SurfaceHighest,
  inverseSurface = FilmeraColors.OnBackground,
  inverseOnSurface = FilmeraColors.Background,
  outline = FilmeraColors.Outline,
  outlineVariant = FilmeraColors.OutlineVariant,
  error = FilmeraColors.Error,
  onError = Color(0xFF690005),
  errorContainer = FilmeraColors.ErrorContainer,
  onErrorContainer = Color(0xFFFFDAD6),
  scrim = FilmeraColors.ImageScrim,
)

private val FilmeraExtendedDarkColors = FilmeraExtendedColors(
  neonGlow = FilmeraColors.NeonStrong,
  onImage = FilmeraColors.OnImage,
  imageScrim = FilmeraColors.ImageScrim,
  rating = FilmeraColors.Rating,
  success = FilmeraColors.Success,
  warning = FilmeraColors.Warning,
)

private val LocalFilmeraExtendedColors = staticCompositionLocalOf {
  FilmeraExtendedDarkColors
}

val MaterialTheme.filmeraColors: FilmeraExtendedColors
  @Composable
  @ReadOnlyComposable
  get() = LocalFilmeraExtendedColors.current

@Composable
fun FilmeraTheme(
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(
    LocalFilmeraExtendedColors provides FilmeraExtendedDarkColors,
  ) {
    MaterialTheme(
      colorScheme = FilmeraDarkColorScheme,
      typography = FilmeraTypography,
      content = content,
    )
  }
}
