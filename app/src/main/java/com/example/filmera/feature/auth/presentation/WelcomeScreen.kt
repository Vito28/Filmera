package com.example.filmera.feature.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.FilmeraColors
import com.example.filmera.core.designsystem.theme.FilmeraTheme

@Composable
fun WelcomeRoute(
  onContinueWithEmail: () -> Unit,
  onCreateAccount: () -> Unit,
  onAuthenticated: (AuthNextDestination) -> Unit,
  viewModel: AuthViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(viewModel) {
    viewModel.effects.collect { effect ->
      when (effect) {
        is AuthEffect.Navigate -> onAuthenticated(effect.destination)
      }
    }
  }

  WelcomeScreen(
    isGoogleLoading = state.isGoogleLoading,
    message = state.message,
    onContinueWithEmail = onContinueWithEmail,
    onContinueWithGoogle = { viewModel.onEvent(AuthEvent.ContinueWithGoogle) },
    onCreateAccount = onCreateAccount,
    onDismissMessage = { viewModel.onEvent(AuthEvent.DismissMessage) },
  )
}

@Composable
fun WelcomeScreen(
  isGoogleLoading: Boolean,
  message: AuthMessage?,
  onContinueWithEmail: () -> Unit,
  onContinueWithGoogle: () -> Unit,
  onCreateAccount: () -> Unit,
  onDismissMessage: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    if (maxWidth >= 760.dp) {
      WelcomeExpandedLayout(
        isGoogleLoading = isGoogleLoading,
        message = message,
        onContinueWithEmail = onContinueWithEmail,
        onContinueWithGoogle = onContinueWithGoogle,
        onCreateAccount = onCreateAccount,
        onDismissMessage = onDismissMessage,
      )
    } else {
      val heroHeight = (maxHeight * 0.53f).coerceIn(280.dp, 500.dp)
      Image(
        painter = painterResource(R.drawable.filmera_welcome_cinema),
        contentDescription = stringResource(R.string.welcome_artwork_description),
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
        modifier = Modifier
          .fillMaxWidth()
          .height(heroHeight),
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colorStops = arrayOf(
                0f to Color.Transparent,
                0.36f to Color.Transparent,
                0.57f to MaterialTheme.colorScheme.background.copy(alpha = 0.93f),
                0.69f to MaterialTheme.colorScheme.background,
                1f to MaterialTheme.colorScheme.background,
              ),
            ),
          ),
      )
      Column(
        modifier = Modifier
          .fillMaxSize()
          .safeDrawingPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(Modifier.height((heroHeight * 0.69f).coerceAtLeast(190.dp)))
        WelcomeContent(
          isGoogleLoading = isGoogleLoading,
          message = message,
          onContinueWithEmail = onContinueWithEmail,
          onContinueWithGoogle = onContinueWithGoogle,
          onCreateAccount = onCreateAccount,
          onDismissMessage = onDismissMessage,
          centered = false,
          modifier = Modifier.widthIn(max = 520.dp),
        )
        Spacer(Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun WelcomeExpandedLayout(
  isGoogleLoading: Boolean,
  message: AuthMessage?,
  onContinueWithEmail: () -> Unit,
  onContinueWithGoogle: () -> Unit,
  onCreateAccount: () -> Unit,
  onDismissMessage: () -> Unit,
) {
  Row(Modifier.fillMaxSize()) {
    Box(
      modifier = Modifier
        .weight(1.15f)
        .fillMaxHeight(),
    ) {
      Image(
        painter = painterResource(R.drawable.filmera_welcome_cinema),
        contentDescription = stringResource(R.string.welcome_artwork_description),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.horizontalGradient(
              colorStops = arrayOf(
                0f to Color.Transparent,
                0.72f to Color.Transparent,
                1f to MaterialTheme.colorScheme.background,
              ),
            ),
          ),
      )
    }
    Box(
      modifier = Modifier
        .weight(0.85f)
        .fillMaxHeight()
        .safeDrawingPadding()
        .padding(horizontal = 40.dp),
      contentAlignment = Alignment.Center,
    ) {
      WelcomeContent(
        isGoogleLoading = isGoogleLoading,
        message = message,
        onContinueWithEmail = onContinueWithEmail,
        onContinueWithGoogle = onContinueWithGoogle,
        onCreateAccount = onCreateAccount,
        onDismissMessage = onDismissMessage,
        centered = false,
        modifier = Modifier
          .widthIn(max = 520.dp)
          .verticalScroll(rememberScrollState())
          .padding(vertical = 32.dp),
      )
    }
  }
}

@Composable
private fun WelcomeContent(
  isGoogleLoading: Boolean,
  message: AuthMessage?,
  onContinueWithEmail: () -> Unit,
  onContinueWithGoogle: () -> Unit,
  onCreateAccount: () -> Unit,
  onDismissMessage: () -> Unit,
  centered: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
  ) {
    FilmeraWordmark()
    Spacer(Modifier.height(20.dp))
    Text(
      text = stringResource(R.string.welcome_eyebrow),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
      letterSpacing = 2.sp,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.welcome_title),
      style = MaterialTheme.typography.displaySmall,
      color = MaterialTheme.colorScheme.onBackground,
      fontWeight = FontWeight.Black,
      lineHeight = 43.sp,
      textAlign = if (centered) TextAlign.Center else TextAlign.Start,
    )
    Spacer(Modifier.height(12.dp))
    Text(
      text = stringResource(R.string.welcome_subtitle),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      lineHeight = 24.sp,
      textAlign = if (centered) TextAlign.Center else TextAlign.Start,
    )

    if (message != null) {
      Spacer(Modifier.height(18.dp))
      WelcomeMessage(
        message = message,
        onDismiss = onDismissMessage,
      )
    }

    Spacer(Modifier.height(28.dp))
    Button(
      onClick = onContinueWithEmail,
      enabled = !isGoogleLoading,
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .shadow(
          elevation = 22.dp,
          shape = RoundedCornerShape(18.dp),
          ambientColor = FilmeraColors.Neon.copy(alpha = 0.28f),
          spotColor = FilmeraColors.Neon.copy(alpha = 0.24f),
        ),
      shape = RoundedCornerShape(18.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ),
    ) {
      Icon(
        imageVector = Icons.Default.Email,
        contentDescription = null,
        modifier = Modifier.size(21.dp),
      )
      Spacer(Modifier.width(11.dp))
      Text(
        text = stringResource(R.string.welcome_continue_email),
        fontWeight = FontWeight.Bold,
      )
    }

    Spacer(Modifier.height(13.dp))
    OutlinedButton(
      onClick = onContinueWithGoogle,
      enabled = !isGoogleLoading,
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
      shape = RoundedCornerShape(18.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      colors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurface,
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f),
      ),
    ) {
      if (isGoogleLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          strokeWidth = 2.dp,
          color = MaterialTheme.colorScheme.primary,
        )
      } else {
        Icon(
          painter = painterResource(R.drawable.ic_google_logo),
          contentDescription = null,
          tint = Color.Unspecified,
          modifier = Modifier.size(21.dp),
        )
      }
      Spacer(Modifier.width(11.dp))
      Text(
        text = if (isGoogleLoading) {
          stringResource(R.string.auth_opening_google)
        } else {
          stringResource(R.string.auth_continue_google)
        },
        fontWeight = FontWeight.SemiBold,
      )
    }

    Spacer(Modifier.height(22.dp))
    Row(
      modifier = Modifier.align(Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Text(
        text = stringResource(R.string.welcome_new_to_filmera),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = stringResource(R.string.welcome_create_account),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .clickable(onClick = onCreateAccount)
          .padding(horizontal = 8.dp, vertical = 12.dp),
      )
    }
    Text(
      text = stringResource(R.string.welcome_security_note),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
      textAlign = TextAlign.Center,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .widthIn(max = 340.dp),
    )
  }
}

@Composable
private fun FilmeraWordmark() {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .shadow(
          elevation = 18.dp,
          shape = CircleShape,
          ambientColor = FilmeraColors.Neon.copy(alpha = 0.5f),
          spotColor = FilmeraColors.Neon.copy(alpha = 0.4f),
        )
        .background(MaterialTheme.colorScheme.primary, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Default.MovieFilter,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.size(24.dp),
      )
    }
    Spacer(Modifier.width(12.dp))
    Text(
      text = stringResource(R.string.auth_brand),
      color = MaterialTheme.colorScheme.onBackground,
      fontSize = 24.sp,
      fontWeight = FontWeight.Black,
      letterSpacing = 3.sp,
    )
  }
}

@Composable
private fun WelcomeMessage(
  message: AuthMessage,
  onDismiss: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(15.dp))
      .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.54f))
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
        shape = RoundedCornerShape(15.dp),
      )
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onDismiss,
      )
      .padding(13.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(
      imageVector = Icons.Default.ErrorOutline,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.error,
      modifier = Modifier.size(20.dp),
    )
    Spacer(Modifier.width(10.dp))
    Text(
      text = authMessageText(message),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onErrorContainer,
      modifier = Modifier.weight(1f),
    )
  }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun WelcomeScreenPreview() {
  FilmeraTheme {
    WelcomeScreen(
      isGoogleLoading = false,
      message = null,
      onContinueWithEmail = {},
      onContinueWithGoogle = {},
      onCreateAccount = {},
      onDismissMessage = {},
    )
  }
}

@Preview(showBackground = true, widthDp = 920, heightDp = 720)
@Composable
private fun WelcomeScreenExpandedPreview() {
  FilmeraTheme {
    WelcomeScreen(
      isGoogleLoading = false,
      message = null,
      onContinueWithEmail = {},
      onContinueWithGoogle = {},
      onCreateAccount = {},
      onDismissMessage = {},
    )
  }
}
