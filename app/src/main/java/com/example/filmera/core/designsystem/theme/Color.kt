package com.example.filmera.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Raw Filmera palette. Feature composables should prefer semantic colors from
 * [androidx.compose.material3.MaterialTheme.colorScheme].
 */
object FilmeraColors {
  val Neon = Color(0xFFB7FF4A)
  val NeonStrong = Color(0xFF9CFF00)
  val NeonSoft = Color(0xFFD8FFA2)
  val NeonContainer = Color(0xFF22350F)

  val Background = Color(0xFF050705)
  val SurfaceLowest = Color(0xFF030403)
  val Surface = Color(0xFF0A0C0A)
  val SurfaceLow = Color(0xFF0E120E)
  val SurfaceContainer = Color(0xFF131813)
  val SurfaceHigh = Color(0xFF192019)
  val SurfaceHighest = Color(0xFF222A22)

  val OnBackground = Color(0xFFEFF7EB)
  val OnSurfaceMuted = Color(0xFFBCC8B7)
  val Outline = Color(0xFF879581)
  val OutlineVariant = Color(0xFF354034)

  val Secondary = Color(0xFFBED6B4)
  val SecondaryContainer = Color(0xFF263326)
  val Tertiary = Color(0xFF8EDBB0)
  val TertiaryContainer = Color(0xFF143D29)

  val Error = Color(0xFFFFB4AB)
  val ErrorContainer = Color(0xFF690005)
  val Success = Color(0xFF76E59B)
  val Warning = Color(0xFFFFCE6A)
  val Rating = Color(0xFFFFC857)

  val ImageScrim = Color(0xFF000000)
  val OnImage = Color(0xFFFFFFFF)
}

@Immutable
data class FilmeraExtendedColors(
  val neonGlow: Color,
  val onImage: Color,
  val imageScrim: Color,
  val rating: Color,
  val success: Color,
  val warning: Color,
)
