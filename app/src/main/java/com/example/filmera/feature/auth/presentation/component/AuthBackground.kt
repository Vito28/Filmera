package com.example.filmera.feature.auth.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.filmera.core.designsystem.theme.FilmeraColors

@Composable
fun CinematicAuthBackground(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(
            FilmeraColors.SurfaceContainer,
            FilmeraColors.Background,
            FilmeraColors.SurfaceLowest,
          ),
        ),
      ),
  ) {
    Canvas(Modifier.fillMaxSize()) {
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            FilmeraColors.Neon.copy(alpha = 0.15f),
            Color.Transparent,
          ),
          center = Offset(size.width * 0.12f, size.height * 0.17f),
          radius = size.minDimension * 0.56f,
        ),
        radius = size.minDimension * 0.56f,
        center = Offset(size.width * 0.12f, size.height * 0.17f),
      )
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            FilmeraColors.Tertiary.copy(alpha = 0.08f),
            Color.Transparent,
          ),
          center = Offset(size.width * 0.88f, size.height * 0.76f),
          radius = size.minDimension * 0.48f,
        ),
        radius = size.minDimension * 0.48f,
        center = Offset(size.width * 0.88f, size.height * 0.76f),
      )

      val projectorBeam = Path().apply {
        moveTo(size.width * 0.08f, 0f)
        lineTo(size.width * 0.72f, size.height)
        lineTo(size.width * 0.98f, size.height)
        lineTo(size.width * 0.17f, 0f)
        close()
      }
      drawPath(
        path = projectorBeam,
        brush = Brush.verticalGradient(
          listOf(
            Color.White.copy(alpha = 0.025f),
            FilmeraColors.Neon.copy(alpha = 0.018f),
            Color.Transparent,
          ),
        ),
      )

      repeat(4) { index ->
        val frameWidth = size.width * (0.44f - index * 0.045f)
        val frameHeight = frameWidth * 0.58f
        drawRoundRect(
          color = Color.White.copy(alpha = 0.025f + index * 0.004f),
          topLeft = Offset(
            x = size.width * (0.64f + index * 0.028f),
            y = size.height * (0.05f + index * 0.105f),
          ),
          size = Size(frameWidth, frameHeight),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
          style = Stroke(width = 1.dp.toPx()),
        )
      }

      val particles = listOf(
        0.09f to 0.31f,
        0.24f to 0.12f,
        0.38f to 0.47f,
        0.58f to 0.19f,
        0.72f to 0.63f,
        0.86f to 0.37f,
        0.17f to 0.81f,
        0.47f to 0.88f,
        0.93f to 0.83f,
      )
      particles.forEachIndexed { index, (x, y) ->
        drawCircle(
          color = if (index % 3 == 0) {
            FilmeraColors.Neon.copy(alpha = 0.28f)
          } else {
            Color.White.copy(alpha = 0.12f)
          },
          radius = if (index % 2 == 0) 1.5.dp.toPx() else 1.dp.toPx(),
          center = Offset(size.width * x, size.height * y),
        )
      }
    }
  }
}
