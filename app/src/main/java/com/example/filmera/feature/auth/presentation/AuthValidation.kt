package com.example.filmera.feature.auth.presentation

private val emailPattern = Regex(
  pattern = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
  option = RegexOption.IGNORE_CASE,
)
private val usernamePattern = Regex("^[a-z0-9_]{3,24}$")

internal fun validateEmail(value: String): AuthValidationError? = when {
  value.isBlank() -> AuthValidationError.REQUIRED
  !emailPattern.matches(value.trim()) -> AuthValidationError.INVALID_EMAIL
  else -> null
}

internal fun validatePassword(value: String): AuthValidationError? = when {
  value.isBlank() -> AuthValidationError.REQUIRED
  value.length < 8 -> AuthValidationError.PASSWORD_TOO_SHORT
  else -> null
}

internal fun validateDisplayName(value: String): AuthValidationError? = when {
  value.isBlank() -> AuthValidationError.REQUIRED
  value.trim().length !in 1..60 -> AuthValidationError.DISPLAY_NAME_LENGTH
  else -> null
}

internal fun validateUsername(value: String): AuthValidationError? = when {
  value.isBlank() -> AuthValidationError.REQUIRED
  !usernamePattern.matches(value.trim()) -> AuthValidationError.USERNAME_FORMAT
  else -> null
}

internal fun validateConfirmPassword(
  password: String,
  confirmation: String,
): AuthValidationError? = when {
  confirmation.isBlank() -> AuthValidationError.REQUIRED
  password != confirmation -> AuthValidationError.PASSWORD_MISMATCH
  else -> null
}

internal fun passwordStrength(password: String): PasswordStrength {
  if (password.isEmpty()) return PasswordStrength.NONE
  var score = 0
  if (password.length >= 8) score++
  if (password.length >= 12) score++
  if (password.any(Char::isLowerCase) && password.any(Char::isUpperCase)) score++
  if (password.any(Char::isDigit)) score++
  if (password.any { !it.isLetterOrDigit() }) score++
  return when {
    score >= 4 -> PasswordStrength.STRONG
    score >= 2 -> PasswordStrength.FAIR
    else -> PasswordStrength.WEAK
  }
}

internal fun validationErrors(
  state: AuthUiState,
  touched: Set<AuthField> = state.touchedFields,
): Map<AuthField, AuthValidationError> = buildMap {
  fun add(field: AuthField, error: AuthValidationError?) {
    if (field in touched && error != null) put(field, error)
  }

  add(AuthField.SIGN_IN_EMAIL, validateEmail(state.signIn.email))
  add(AuthField.SIGN_IN_PASSWORD, validatePassword(state.signIn.password))
  add(AuthField.DISPLAY_NAME, validateDisplayName(state.signUp.displayName))
  add(AuthField.USERNAME, validateUsername(state.signUp.username))
  add(AuthField.SIGN_UP_EMAIL, validateEmail(state.signUp.email))
  add(AuthField.SIGN_UP_PASSWORD, validatePassword(state.signUp.password))
  add(
    AuthField.CONFIRM_PASSWORD,
    validateConfirmPassword(state.signUp.password, state.signUp.confirmPassword),
  )
  if (AuthField.TERMS in touched && !state.signUp.acceptedTerms) {
    put(AuthField.TERMS, AuthValidationError.TERMS_REQUIRED)
  }
}

internal val signInFields = setOf(
  AuthField.SIGN_IN_EMAIL,
  AuthField.SIGN_IN_PASSWORD,
)

internal val signUpFields = setOf(
  AuthField.DISPLAY_NAME,
  AuthField.USERNAME,
  AuthField.SIGN_UP_EMAIL,
  AuthField.SIGN_UP_PASSWORD,
  AuthField.CONFIRM_PASSWORD,
  AuthField.TERMS,
)
