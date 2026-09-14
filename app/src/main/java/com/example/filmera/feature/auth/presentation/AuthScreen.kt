package com.example.filmera.feature.auth.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.FilmeraColors
import com.example.filmera.core.designsystem.theme.FilmeraTheme
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.feature.auth.presentation.component.CinematicAuthBackground

@Composable
fun AuthScreen(
  state: AuthUiState,
  onEvent: (AuthEvent) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier.fillMaxSize()) {
    CinematicAuthBackground()
    val expanded = maxWidth >= 760.dp
    Image(
      painter = painterResource(R.drawable.filmera_welcome_cinema),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      alignment = if (expanded) Alignment.CenterStart else Alignment.TopCenter,
      modifier = Modifier
        .fillMaxSize()
        .alpha(if (expanded) 0.34f else 0.22f),
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          if (expanded) {
            Brush.horizontalGradient(
              colorStops = arrayOf(
                0f to MaterialTheme.colorScheme.background.copy(alpha = 0.08f),
                0.48f to MaterialTheme.colorScheme.background.copy(alpha = 0.58f),
                0.72f to MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                1f to MaterialTheme.colorScheme.background,
              ),
            )
          } else {
            Brush.verticalGradient(
              colorStops = arrayOf(
                0f to MaterialTheme.colorScheme.background.copy(alpha = 0.18f),
                0.28f to MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                0.48f to MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                1f to MaterialTheme.colorScheme.background,
              ),
            )
          },
        ),
    )
    Box(
      modifier = Modifier
        .align(Alignment.TopStart)
        .safeDrawingPadding()
        .padding(start = 8.dp, top = 8.dp),
    ) {
      IconButton(onClick = onBack) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.navigate_back),
          tint = MaterialTheme.colorScheme.onBackground,
        )
      }
    }
    if (expanded) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .safeDrawingPadding()
          .imePadding()
          .padding(horizontal = 48.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AuthBrandStory(
          modifier = Modifier.weight(0.82f),
          centered = false,
        )
        AuthFormPanel(
          state = state,
          onEvent = onEvent,
          elevated = true,
          centeredHeading = false,
          modifier = Modifier
            .weight(1f)
            .widthIn(max = 560.dp),
        )
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .safeDrawingPadding()
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(Modifier.height(48.dp))
        AuthBrandStory(
          modifier = Modifier.fillMaxWidth(),
          centered = true,
        )
        Spacer(Modifier.height(24.dp))
        AuthFormPanel(
          state = state,
          onEvent = onEvent,
          elevated = false,
          centeredHeading = true,
          modifier = Modifier.widthIn(max = 540.dp),
        )
        Spacer(Modifier.height(20.dp))
      }
    }
  }
}

@Composable
private fun AuthBrandStory(
  centered: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .shadow(18.dp, CircleShape, ambientColor = FilmeraColors.Neon)
          .background(FilmeraColors.NeonContainer, CircleShape)
          .border(1.dp, FilmeraColors.Neon.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.MovieFilter,
          contentDescription = null,
          tint = FilmeraColors.Neon,
          modifier = Modifier.size(23.dp),
        )
      }
      Spacer(Modifier.width(12.dp))
      Text(
        text = stringResource(R.string.auth_brand),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 22.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 3.sp,
      )
    }
    if (!centered) {
      Spacer(Modifier.height(28.dp))
      Text(
        text = stringResource(R.string.auth_brand_headline),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(14.dp))
      Text(
        text = stringResource(R.string.auth_brand_supporting),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.widthIn(max = 460.dp),
      )
      Spacer(Modifier.height(30.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AuthValueChip(stringResource(R.string.auth_value_discover))
        AuthValueChip(stringResource(R.string.auth_value_remember))
        AuthValueChip(stringResource(R.string.auth_value_share))
      }
    }
  }
}

@Composable
private fun AuthValueChip(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier
      .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f), CircleShape)
      .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
      .padding(horizontal = 13.dp, vertical = 8.dp),
  )
}

@Composable
private fun AuthFormPanel(
  state: AuthUiState,
  onEvent: (AuthEvent) -> Unit,
  elevated: Boolean,
  centeredHeading: Boolean,
  modifier: Modifier = Modifier,
) {
  val cardShape = RoundedCornerShape(30.dp)
  val panelModifier = if (elevated) {
    modifier
      .fillMaxWidth()
      .shadow(
        elevation = 30.dp,
        shape = cardShape,
        ambientColor = Color.Black,
        spotColor = FilmeraColors.Neon.copy(alpha = 0.12f),
      )
      .clip(cardShape)
      .background(
        Brush.verticalGradient(
          listOf(
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.985f),
          ),
        ),
      )
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.17f),
        shape = cardShape,
      )
      .padding(horizontal = 24.dp, vertical = 28.dp)
  } else {
    modifier.fillMaxWidth()
  }
  Column(
    modifier = panelModifier,
  ) {
    AnimatedContent(
      targetState = state.mode,
      transitionSpec = {
        val direction = if (targetState == AuthMode.SIGN_UP) 1 else -1
        (fadeIn() + slideInHorizontally { it / 12 * direction }) togetherWith
          (fadeOut() + slideOutHorizontally { -it / 12 * direction }) using
          SizeTransform(clip = false)
      },
      label = "auth_form_mode",
    ) { mode ->
      when (mode) {
        AuthMode.SIGN_IN -> SignInForm(
          state = state,
          onEvent = onEvent,
          centeredHeading = centeredHeading,
        )
        AuthMode.SIGN_UP -> SignUpForm(
          state = state,
          onEvent = onEvent,
          centeredHeading = centeredHeading,
        )
      }
    }
    AnimatedVisibility(
      visible = state.message != null,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      state.message?.let { message ->
        Column {
          Spacer(Modifier.height(16.dp))
          AuthMessageBanner(
            message = message,
            onDismiss = { onEvent(AuthEvent.DismissMessage) },
          )
        }
      }
    }
    Spacer(Modifier.height(20.dp))
    AuthPrimaryButton(state = state, onEvent = onEvent)
    Spacer(Modifier.height(16.dp))
    AuthModeFooter(
      mode = state.mode,
      enabled = !state.isSubmitting && !state.isSuccess,
      onModeChange = { onEvent(AuthEvent.ModeChanged(it)) },
    )
  }
}

@Composable
private fun SignInForm(
  state: AuthUiState,
  onEvent: (AuthEvent) -> Unit,
  centeredHeading: Boolean,
) {
  val focusManager = LocalFocusManager.current
  Column {
    AuthHeading(
      title = stringResource(R.string.auth_sign_in_title),
      subtitle = stringResource(R.string.auth_sign_in_subtitle),
      centered = centeredHeading,
    )
    Spacer(Modifier.height(20.dp))
    AuthTextField(
      value = state.signIn.email,
      onValueChange = { onEvent(AuthEvent.SignInEmailChanged(it)) },
      label = stringResource(R.string.auth_email_label),
      placeholder = stringResource(R.string.auth_email_placeholder),
      leadingIcon = Icons.Default.AlternateEmail,
      error = state.errors[AuthField.SIGN_IN_EMAIL],
      onBlur = { onEvent(AuthEvent.FieldBlurred(AuthField.SIGN_IN_EMAIL)) },
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next,
      ),
    )
    Spacer(Modifier.height(14.dp))
    AuthPasswordField(
      value = state.signIn.password,
      onValueChange = { onEvent(AuthEvent.SignInPasswordChanged(it)) },
      label = stringResource(R.string.auth_password_label),
      error = state.errors[AuthField.SIGN_IN_PASSWORD],
      onBlur = { onEvent(AuthEvent.FieldBlurred(AuthField.SIGN_IN_PASSWORD)) },
      keyboardActions = KeyboardActions(onDone = {
        focusManager.clearFocus()
        onEvent(AuthEvent.Submit)
      }),
    )
    Spacer(Modifier.height(4.dp))
    Box(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = if (state.isResettingPassword) {
          stringResource(R.string.auth_sending_reset)
        } else {
          stringResource(R.string.auth_forgot_password)
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .clip(RoundedCornerShape(10.dp))
          .clickable(
            enabled = !state.isResettingPassword && !state.isSubmitting,
            onClick = { onEvent(AuthEvent.ForgotPassword) },
          )
          .padding(horizontal = 4.dp, vertical = 10.dp),
      )
    }
  }
}

@Composable
private fun SignUpForm(
  state: AuthUiState,
  onEvent: (AuthEvent) -> Unit,
  centeredHeading: Boolean,
) {
  val focusManager = LocalFocusManager.current
  Column {
    AuthHeading(
      title = stringResource(R.string.auth_sign_up_title),
      subtitle = stringResource(R.string.auth_sign_up_subtitle),
      centered = centeredHeading,
    )
    Spacer(Modifier.height(20.dp))
    AuthTextField(
      value = state.signUp.displayName,
      onValueChange = { onEvent(AuthEvent.DisplayNameChanged(it)) },
      label = stringResource(R.string.auth_display_name_label),
      placeholder = stringResource(R.string.auth_display_name_placeholder),
      leadingIcon = Icons.Default.Person,
      error = state.errors[AuthField.DISPLAY_NAME],
      onBlur = { onEvent(AuthEvent.FieldBlurred(AuthField.DISPLAY_NAME)) },
    )
    Spacer(Modifier.height(14.dp))
    AuthTextField(
      value = state.signUp.username,
      onValueChange = { onEvent(AuthEvent.UsernameChanged(it)) },
      label = stringResource(R.string.auth_username_label),
      placeholder = stringResource(R.string.auth_username_placeholder),
      leadingIcon = Icons.Default.AlternateEmail,
      error = state.errors[AuthField.USERNAME],
      onBlur = { onEvent(AuthEvent.FieldBlurred(AuthField.USERNAME)) },
      trailingContent = {
        UsernameAvailabilityIcon(
          availability = state.usernameAvailability,
          onRetry = { onEvent(AuthEvent.RetryUsernameAvailability) },
        )
      },
      supportingContent = if (state.errors[AuthField.USERNAME] == null) {
        { UsernameAvailabilityText(state.usernameAvailability) }
      } else {
        null
      },
    )
    Spacer(Modifier.height(14.dp))
    AuthTextField(
      value = state.signUp.email,
      onValueChange = { onEvent(AuthEvent.SignUpEmailChanged(it)) },
      label = stringResource(R.string.auth_email_label),
      placeholder = stringResource(R.string.auth_email_placeholder),
      leadingIcon = Icons.Default.AlternateEmail,
      error = state.errors[AuthField.SIGN_UP_EMAIL],
      onBlur = { onEvent(AuthEvent.FieldBlurred(AuthField.SIGN_UP_EMAIL)) },
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next,
      ),
    )
    Spacer(Modifier.height(14.dp))
    AuthPasswordField(
      value = state.signUp.password,
      onValueChange = { onEvent(AuthEvent.SignUpPasswordChanged(it)) },
      label = stringResource(R.string.auth_password_label),
      error = state.errors[AuthField.SIGN_UP_PASSWORD],
      onBlur = { onEvent(AuthEvent.FieldBlurred(AuthField.SIGN_UP_PASSWORD)) },
    )
    PasswordStrengthIndicator(state.passwordStrength)
    Spacer(Modifier.height(14.dp))
    AuthPasswordField(
      value = state.signUp.confirmPassword,
      onValueChange = { onEvent(AuthEvent.ConfirmPasswordChanged(it)) },
      label = stringResource(R.string.auth_confirm_password_label),
      error = state.errors[AuthField.CONFIRM_PASSWORD],
      onBlur = { onEvent(AuthEvent.FieldBlurred(AuthField.CONFIRM_PASSWORD)) },
      keyboardActions = KeyboardActions(onDone = {
        focusManager.clearFocus()
        onEvent(AuthEvent.Submit)
      }),
    )
    Spacer(Modifier.height(10.dp))
    AuthCheckRow(
      checked = state.signUp.acceptedTerms,
      text = stringResource(R.string.auth_terms_agreement),
      onCheckedChange = { onEvent(AuthEvent.TermsChanged(it)) },
      error = state.errors[AuthField.TERMS] != null,
    )
    if (state.errors[AuthField.TERMS] != null) {
      Text(
        text = validationErrorText(AuthValidationError.TERMS_REQUIRED),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
      )
    }
  }
}

@Composable
private fun AuthHeading(
  title: String,
  subtitle: String,
  centered: Boolean,
) {
  Text(
    text = title,
    style = MaterialTheme.typography.headlineSmall,
    color = MaterialTheme.colorScheme.onSurface,
    fontWeight = FontWeight.Bold,
    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
    modifier = Modifier.fillMaxWidth(),
  )
  Spacer(Modifier.height(7.dp))
  Text(
    text = subtitle,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
    modifier = Modifier.fillMaxWidth(),
  )
}

@Composable
private fun AuthTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String,
  leadingIcon: ImageVector,
  error: AuthValidationError?,
  onBlur: () -> Unit,
  modifier: Modifier = Modifier,
  keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  trailingContent: (@Composable () -> Unit)? = null,
  supportingContent: (@Composable () -> Unit)? = null,
) {
  var wasFocused by remember { mutableStateOf(false) }
  Column(modifier = modifier.fillMaxWidth()) {
    AuthFieldLabel(label)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier
        .fillMaxWidth()
        .onFocusChanged { focusState ->
          if (wasFocused && !focusState.isFocused) onBlur()
          wasFocused = focusState.isFocused
        },
      placeholder = { Text(placeholder) },
      leadingIcon = {
        Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          tint = if (error == null) {
            MaterialTheme.colorScheme.onSurfaceVariant
          } else {
            MaterialTheme.colorScheme.error
          },
        )
      },
      trailingIcon = trailingContent,
      supportingText = when {
        error != null -> ({ Text(validationErrorText(error)) })
        supportingContent != null -> supportingContent
        else -> null
      },
      isError = error != null,
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      colors = authTextFieldColors(),
    )
  }
}

@Composable
private fun AuthPasswordField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  error: AuthValidationError?,
  onBlur: () -> Unit,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
  var passwordVisible by remember { mutableStateOf(false) }
  var wasFocused by remember { mutableStateOf(false) }
  val visibilityDescription = stringResource(
    if (passwordVisible) R.string.auth_hide_password else R.string.auth_show_password,
  )
  Column(modifier = Modifier.fillMaxWidth()) {
    AuthFieldLabel(label)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier
        .fillMaxWidth()
        .onFocusChanged { focusState ->
          if (wasFocused && !focusState.isFocused) onBlur()
          wasFocused = focusState.isFocused
        },
      placeholder = { Text(stringResource(R.string.auth_password_placeholder)) },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      },
      trailingIcon = {
        IconButton(onClick = { passwordVisible = !passwordVisible }) {
          Icon(
            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = visibilityDescription,
          )
        }
      },
      supportingText = error?.let { current ->
        { Text(validationErrorText(current)) }
      },
      isError = error != null,
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      visualTransformation = if (passwordVisible) {
        VisualTransformation.None
      } else {
        PasswordVisualTransformation()
      },
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
      ),
      keyboardActions = keyboardActions,
      colors = authTextFieldColors(),
    )
  }
}

@Composable
private fun AuthFieldLabel(label: String) {
  Text(
    text = label,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurface,
    fontWeight = FontWeight.SemiBold,
    modifier = Modifier.padding(start = 2.dp),
  )
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = MaterialTheme.colorScheme.primary,
  unfocusedBorderColor = MaterialTheme.colorScheme.outline,
  focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
  unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.72f),
  errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f),
  cursorColor = MaterialTheme.colorScheme.primary,
)

@Composable
private fun UsernameAvailabilityIcon(
  availability: UsernameAvailability,
  onRetry: () -> Unit,
) {
  when (availability) {
    UsernameAvailability.CHECKING -> CircularProgressIndicator(
      modifier = Modifier.size(19.dp),
      strokeWidth = 2.dp,
    )
    UsernameAvailability.AVAILABLE -> Icon(
      Icons.Default.Check,
      contentDescription = stringResource(R.string.auth_username_available),
      tint = MaterialTheme.filmeraColors.success,
    )
    UsernameAvailability.TAKEN -> Icon(
      Icons.Default.ErrorOutline,
      contentDescription = stringResource(R.string.auth_username_taken),
      tint = MaterialTheme.colorScheme.error,
    )
    UsernameAvailability.CHECK_FAILED -> IconButton(onClick = onRetry) {
      Icon(
        Icons.Default.Refresh,
        contentDescription = stringResource(R.string.auth_retry_username_check),
        tint = MaterialTheme.colorScheme.primary,
      )
    }
    UsernameAvailability.IDLE -> Unit
  }
}

@Composable
private fun UsernameAvailabilityText(availability: UsernameAvailability) {
  val text = when (availability) {
    UsernameAvailability.IDLE -> stringResource(R.string.auth_username_hint)
    UsernameAvailability.CHECKING -> stringResource(R.string.auth_username_checking)
    UsernameAvailability.AVAILABLE -> stringResource(R.string.auth_username_available)
    UsernameAvailability.TAKEN -> stringResource(R.string.auth_username_taken)
    UsernameAvailability.CHECK_FAILED -> stringResource(R.string.auth_username_check_failed)
  }
  val color = when (availability) {
    UsernameAvailability.AVAILABLE -> MaterialTheme.filmeraColors.success
    UsernameAvailability.TAKEN -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }
  Text(text = text, color = color)
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength) {
  if (strength == PasswordStrength.NONE) return
  val activeSegments = when (strength) {
    PasswordStrength.NONE -> 0
    PasswordStrength.WEAK -> 1
    PasswordStrength.FAIR -> 2
    PasswordStrength.STRONG -> 3
  }
  val color = when (strength) {
    PasswordStrength.NONE -> MaterialTheme.colorScheme.outlineVariant
    PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
    PasswordStrength.FAIR -> MaterialTheme.filmeraColors.warning
    PasswordStrength.STRONG -> MaterialTheme.filmeraColors.success
  }
  Row(
    modifier = Modifier.padding(top = 7.dp, start = 4.dp, end = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    repeat(3) { index ->
      Box(
        Modifier
          .weight(1f)
          .height(3.dp)
          .background(
            if (index < activeSegments) color else MaterialTheme.colorScheme.outlineVariant,
            CircleShape,
          ),
      )
    }
    Text(
      text = stringResource(
        when (strength) {
          PasswordStrength.WEAK -> R.string.auth_password_weak
          PasswordStrength.FAIR -> R.string.auth_password_fair
          PasswordStrength.STRONG -> R.string.auth_password_strong
          PasswordStrength.NONE -> R.string.auth_password_weak
        },
      ),
      style = MaterialTheme.typography.labelSmall,
      color = color,
    )
  }
}

@Composable
private fun AuthCheckRow(
  checked: Boolean,
  text: String,
  onCheckedChange: (Boolean) -> Unit,
  error: Boolean = false,
) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .toggleable(
        value = checked,
        role = Role.Checkbox,
        onValueChange = onCheckedChange,
      )
      .padding(vertical = 8.dp, horizontal = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(22.dp)
        .background(
          if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
          RoundedCornerShape(7.dp),
        )
        .border(
          1.dp,
          when {
            error -> MaterialTheme.colorScheme.error
            checked -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
          },
          RoundedCornerShape(7.dp),
        ),
      contentAlignment = Alignment.Center,
    ) {
      if (checked) {
        Icon(
          Icons.Default.Check,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(16.dp),
        )
      }
    }
    Spacer(Modifier.width(9.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun AuthPrimaryButton(
  state: AuthUiState,
  onEvent: (AuthEvent) -> Unit,
) {
  val buttonText = when {
    state.isSuccess -> stringResource(R.string.auth_success)
    state.isSubmitting -> stringResource(R.string.auth_please_wait)
    state.mode == AuthMode.SIGN_IN -> stringResource(R.string.auth_sign_in_action)
    else -> stringResource(R.string.auth_create_account_action)
  }
  Button(
    onClick = { onEvent(AuthEvent.Submit) },
    enabled = state.canSubmit && !state.isSubmitting && !state.isSuccess,
    modifier = Modifier
      .fillMaxWidth()
      .height(54.dp),
    shape = RoundedCornerShape(17.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
      disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
  ) {
    if (state.isSubmitting) {
      CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.onPrimary,
      )
      Spacer(Modifier.width(10.dp))
    } else if (state.isSuccess) {
      Icon(Icons.Default.Check, contentDescription = null)
      Spacer(Modifier.width(8.dp))
    }
    Text(text = buttonText, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun AuthModeFooter(
  mode: AuthMode,
  enabled: Boolean,
  onModeChange: (AuthMode) -> Unit,
) {
  val targetMode = if (mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(
        if (mode == AuthMode.SIGN_IN) {
          R.string.welcome_new_to_filmera
        } else {
          R.string.auth_already_have_account
        },
      ),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TextButton(
      onClick = { onModeChange(targetMode) },
      enabled = enabled,
    ) {
      Text(
        text = stringResource(
          if (mode == AuthMode.SIGN_IN) R.string.auth_tab_sign_up else R.string.auth_tab_sign_in,
        ),
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun AuthMessageBanner(
  message: AuthMessage,
  onDismiss: () -> Unit,
) {
  val positive = message == AuthMessage.RESET_EMAIL_SENT ||
    message == AuthMessage.CONFIRMATION_EMAIL_SENT
  val accent = if (positive) MaterialTheme.filmeraColors.success else MaterialTheme.colorScheme.error
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(15.dp))
      .background(accent.copy(alpha = 0.09f))
      .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(15.dp))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onDismiss,
      )
      .padding(13.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(
      imageVector = if (positive) Icons.Default.Check else Icons.Default.ErrorOutline,
      contentDescription = null,
      tint = accent,
      modifier = Modifier.size(20.dp),
    )
    Spacer(Modifier.width(10.dp))
    Text(
      text = authMessageText(message),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun validationErrorText(error: AuthValidationError): String = stringResource(
  when (error) {
    AuthValidationError.REQUIRED -> R.string.auth_error_required
    AuthValidationError.INVALID_EMAIL -> R.string.auth_error_invalid_email
    AuthValidationError.DISPLAY_NAME_LENGTH -> R.string.auth_error_display_name
    AuthValidationError.USERNAME_FORMAT -> R.string.auth_error_username_format
    AuthValidationError.PASSWORD_TOO_SHORT -> R.string.auth_error_password_length
    AuthValidationError.PASSWORD_MISMATCH -> R.string.auth_error_password_mismatch
    AuthValidationError.TERMS_REQUIRED -> R.string.auth_error_terms
  },
)

@Composable
internal fun authMessageText(message: AuthMessage): String = stringResource(
  when (message) {
    AuthMessage.INVALID_CREDENTIALS -> R.string.auth_message_invalid_credentials
    AuthMessage.EMAIL_NOT_CONFIRMED -> R.string.auth_message_email_not_confirmed
    AuthMessage.EMAIL_ALREADY_REGISTERED -> R.string.auth_message_email_registered
    AuthMessage.INVALID_EMAIL -> R.string.auth_message_invalid_email
    AuthMessage.WEAK_PASSWORD -> R.string.auth_message_weak_password
    AuthMessage.RATE_LIMITED -> R.string.auth_message_rate_limited
    AuthMessage.SIGN_UP_DISABLED -> R.string.auth_message_signup_disabled
    AuthMessage.PROVIDER_UNAVAILABLE -> R.string.auth_message_provider_unavailable
    AuthMessage.NETWORK -> R.string.auth_message_network
    AuthMessage.UNKNOWN -> R.string.auth_message_unknown
    AuthMessage.USERNAME_TAKEN -> R.string.auth_message_username_taken
    AuthMessage.RESET_EMAIL_SENT -> R.string.auth_message_reset_sent
    AuthMessage.RESET_REQUIRES_VALID_EMAIL -> R.string.auth_message_reset_email_required
    AuthMessage.CONFIRMATION_EMAIL_SENT -> R.string.auth_message_confirmation_sent
  },
)

@Preview(showBackground = true, widthDp = 412, heightDp = 940)
@Composable
private fun SignInAuthPreview() {
  FilmeraTheme {
    AuthScreen(
      state = AuthUiState(),
      onEvent = {},
      onBack = {},
    )
  }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 900)
@Composable
private fun SignUpExpandedAuthPreview() {
  FilmeraTheme {
    AuthScreen(
      state = AuthUiState(
        mode = AuthMode.SIGN_UP,
        signUp = SignUpFormState(
          displayName = "Raka Pratama",
          username = "raka_film",
          email = "raka@example.com",
          password = "Cinema!2026",
          confirmPassword = "Cinema!2026",
          acceptedTerms = true,
        ),
        usernameAvailability = UsernameAvailability.AVAILABLE,
        passwordStrength = PasswordStrength.STRONG,
      ),
      onEvent = {},
      onBack = {},
    )
  }
}
