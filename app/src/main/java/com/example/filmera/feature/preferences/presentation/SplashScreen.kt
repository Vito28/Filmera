package com.example.filmera.feature.preferences.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filmera.core.designsystem.theme.FilmeraColors
import com.example.filmera.core.designsystem.theme.FilmeraTheme
import kotlinx.coroutines.delay

@Composable
fun SplashRoute(
  onDestinationReady: (StartupDestination) -> Unit,
  viewModel: StartupViewModel = hiltViewModel(),
) {
  val destination by viewModel.destination.collectAsStateWithLifecycle()
  var logoVisible by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    logoVisible = true
  }
  LaunchedEffect(destination) {
    val resolvedDestination = destination ?: return@LaunchedEffect
    delay(SPLASH_HOLD_MILLIS)
    logoVisible = false
    delay(SPLASH_EXIT_MILLIS)
    onDestinationReady(resolvedDestination)
  }

  SplashScreen(logoVisible = logoVisible)
}

@Composable
fun SplashScreen(
  logoVisible: Boolean,
  modifier: Modifier = Modifier,
) {
  val view = LocalView.current
  SideEffect {
    val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
    WindowCompat.getInsetsController(window, view).apply {
      isAppearanceLightStatusBars = false
      isAppearanceLightNavigationBars = false
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            FilmeraColors.SurfaceContainer,
            FilmeraColors.Surface,
            FilmeraColors.Background,
          ),
        ),
      ),
    contentAlignment = Alignment.Center,
  ) {
    AnimatedVisibility(
      visible = logoVisible,
      enter = fadeIn(tween(SPLASH_ENTER_MILLIS.toInt())) +
        scaleIn(
          initialScale = 0.94f,
          animationSpec = tween(SPLASH_ENTER_MILLIS.toInt()),
        ),
      exit = fadeOut(tween(SPLASH_EXIT_MILLIS.toInt())) +
        scaleOut(
          targetScale = 1.03f,
          animationSpec = tween(SPLASH_EXIT_MILLIS.toInt()),
        ),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Box(
          modifier = Modifier
            .size(164.dp)
            .blur(34.dp)
            .background(
              Brush.radialGradient(
                listOf(
                  lerp(FilmeraColors.Neon, FilmeraColors.OnImage, 0.1f)
                    .copy(alpha = 0.34f),
                  Color.Transparent,
                ),
              ),
              CircleShape,
            ),
        )
        Text(
          text = "FILMERA",
          modifier = Modifier.semantics {
            contentDescription = "Filmera"
          },
          color = FilmeraColors.Neon,
          fontSize = 34.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 5.sp,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

private const val SPLASH_ENTER_MILLIS = 420L
private const val SPLASH_HOLD_MILLIS = 1_050L
private const val SPLASH_EXIT_MILLIS = 240L

@Preview(showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun SplashPreview() {
  FilmeraTheme {
    SplashScreen(logoVisible = true)
  }
}
