package com.example.filmera.feature.auth.domain

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
  val isAuthenticated: Flow<Boolean>

  suspend fun awaitStartupSession(): Boolean

  suspend fun signIn(
    email: String,
    password: String,
    rememberMe: Boolean,
  ): AuthResult<Unit>

  suspend fun signUp(
    displayName: String,
    username: String,
    email: String,
    password: String,
  ): AuthResult<SignUpOutcome>

  suspend fun signInWithGoogle(): AuthResult<Unit>

  suspend fun sendPasswordReset(email: String): AuthResult<Unit>

  suspend fun isUsernameAvailable(username: String): AuthResult<Boolean>
}

sealed interface AuthResult<out T> {
  data class Success<T>(val value: T) : AuthResult<T>
  data class Failure(val reason: AuthFailure) : AuthResult<Nothing>
}

enum class SignUpOutcome {
  AUTHENTICATED,
  EMAIL_CONFIRMATION_REQUIRED,
}

enum class AuthFailure {
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
}
