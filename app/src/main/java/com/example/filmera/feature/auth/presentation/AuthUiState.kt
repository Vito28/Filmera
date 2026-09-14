package com.example.filmera.feature.auth.presentation

import androidx.compose.runtime.Immutable

enum class AuthMode {
  SIGN_IN,
  SIGN_UP,
}

enum class AuthField {
  SIGN_IN_EMAIL,
  SIGN_IN_PASSWORD,
  DISPLAY_NAME,
  USERNAME,
  SIGN_UP_EMAIL,
  SIGN_UP_PASSWORD,
  CONFIRM_PASSWORD,
  TERMS,
}

enum class AuthValidationError {
  REQUIRED,
  INVALID_EMAIL,
  DISPLAY_NAME_LENGTH,
  USERNAME_FORMAT,
  PASSWORD_TOO_SHORT,
  PASSWORD_MISMATCH,
  TERMS_REQUIRED,
}

enum class UsernameAvailability {
  IDLE,
  CHECKING,
  AVAILABLE,
  TAKEN,
  CHECK_FAILED,
}

enum class PasswordStrength {
  NONE,
  WEAK,
  FAIR,
  STRONG,
}

enum class AuthMessage {
  INVALID_CREDENTIALS,
  EMAIL_NOT_CONFIRMED,
  EMAIL_ALREADY_REGISTERED,
  INVALID_EMAIL,
  WEAK_PASSWORD,
  RATE_LIMITED,
  SIGN_UP_DISABLED,
  PROVIDER_UNAVAILABLE,
  NETWORK,
  UNKNOWN,
  USERNAME_TAKEN,
  RESET_EMAIL_SENT,
  RESET_REQUIRES_VALID_EMAIL,
  CONFIRMATION_EMAIL_SENT,
}

@Immutable
data class SignInFormState(
  val email: String = "",
  val password: String = "",
  val rememberMe: Boolean = true,
)

@Immutable
data class SignUpFormState(
  val displayName: String = "",
  val username: String = "",
  val email: String = "",
  val password: String = "",
  val confirmPassword: String = "",
  val acceptedTerms: Boolean = false,
)

@Immutable
data class AuthUiState(
  val mode: AuthMode = AuthMode.SIGN_IN,
  val signIn: SignInFormState = SignInFormState(),
  val signUp: SignUpFormState = SignUpFormState(),
  val touchedFields: Set<AuthField> = emptySet(),
  val errors: Map<AuthField, AuthValidationError> = emptyMap(),
  val usernameAvailability: UsernameAvailability = UsernameAvailability.IDLE,
  val passwordStrength: PasswordStrength = PasswordStrength.NONE,
  val isSubmitting: Boolean = false,
  val isGoogleLoading: Boolean = false,
  val isResettingPassword: Boolean = false,
  val isSuccess: Boolean = false,
  val message: AuthMessage? = null,
)

sealed interface AuthEvent {
  data class ModeChanged(val mode: AuthMode) : AuthEvent
  data class SignInEmailChanged(val value: String) : AuthEvent
  data class SignInPasswordChanged(val value: String) : AuthEvent
  data class RememberMeChanged(val checked: Boolean) : AuthEvent
  data class DisplayNameChanged(val value: String) : AuthEvent
  data class UsernameChanged(val value: String) : AuthEvent
  data class SignUpEmailChanged(val value: String) : AuthEvent
  data class SignUpPasswordChanged(val value: String) : AuthEvent
  data class ConfirmPasswordChanged(val value: String) : AuthEvent
  data class TermsChanged(val checked: Boolean) : AuthEvent
  data class FieldBlurred(val field: AuthField) : AuthEvent
  data object Submit : AuthEvent
  data object ContinueWithGoogle : AuthEvent
  data object ForgotPassword : AuthEvent
  data object RetryUsernameAvailability : AuthEvent
  data object DismissMessage : AuthEvent
}

enum class AuthNextDestination {
  PREFERENCES,
  HOME,
}

sealed interface AuthEffect {
  data class Navigate(val destination: AuthNextDestination) : AuthEffect
}

internal val AuthUiState.canSubmit: Boolean
  get() = when (mode) {
    AuthMode.SIGN_IN ->
      validateEmail(signIn.email) == null && validatePassword(signIn.password) == null
    AuthMode.SIGN_UP ->
      validateDisplayName(signUp.displayName) == null &&
        validateUsername(signUp.username) == null &&
        validateEmail(signUp.email) == null &&
        validatePassword(signUp.password) == null &&
        validateConfirmPassword(signUp.password, signUp.confirmPassword) == null &&
        signUp.acceptedTerms &&
        usernameAvailability != UsernameAvailability.CHECKING &&
        usernameAvailability != UsernameAvailability.TAKEN
  }
