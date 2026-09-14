package com.example.filmera.feature.auth.data

import com.example.filmera.feature.auth.domain.AuthFailure
import com.example.filmera.feature.auth.domain.AuthRepository
import com.example.filmera.feature.auth.domain.AuthResult
import com.example.filmera.feature.auth.domain.SignUpOutcome
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.plugins.HttpRequestTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Singleton
class SupabaseAuthRepository @Inject constructor(
  private val supabase: SupabaseClient,
  private val sessionPolicy: AuthSessionPolicy,
) : AuthRepository {
  override val isAuthenticated: Flow<Boolean> = supabase.auth.sessionStatus
    .map { status -> status is SessionStatus.Authenticated }
    .distinctUntilChanged()

  override suspend fun awaitStartupSession(): Boolean {
    val status = supabase.auth.sessionStatus.first { current ->
      current !is SessionStatus.Initializing
    }
    if (status !is SessionStatus.Authenticated) return false
    if (sessionPolicy.shouldPersistSession()) return true

    supabase.auth.signOut()
    return false
  }

  override suspend fun signIn(
    email: String,
    password: String,
    rememberMe: Boolean,
  ): AuthResult<Unit> = authCall {
    sessionPolicy.setPersistSession(rememberMe)
    supabase.auth.signInWith(Email) {
      this.email = email.trim()
      this.password = password
    }
  }

  override suspend fun signUp(
    displayName: String,
    username: String,
    email: String,
    password: String,
  ): AuthResult<SignUpOutcome> = authCall {
    sessionPolicy.setPersistSession(true)
    supabase.auth.signUpWith(Email) {
      this.email = email.trim()
      this.password = password
      data = buildJsonObject {
        put("display_name", displayName.trim())
        put("username", username.trim().lowercase())
      }
    }
    if (supabase.auth.currentSessionOrNull() == null) {
      SignUpOutcome.EMAIL_CONFIRMATION_REQUIRED
    } else {
      SignUpOutcome.AUTHENTICATED
    }
  }

  override suspend fun signInWithGoogle(): AuthResult<Unit> = authCall {
    sessionPolicy.setPersistSession(true)
    supabase.auth.signInWith(Google)
  }

  override suspend fun sendPasswordReset(email: String): AuthResult<Unit> = authCall {
    supabase.auth.resetPasswordForEmail(email.trim())
  }

  override suspend fun isUsernameAvailable(username: String): AuthResult<Boolean> = authCall {
    supabase.postgrest.rpc(
      function = "is_username_available",
      parameters = buildJsonObject {
        put("p_username", username.trim().lowercase())
      },
    ).decodeAs<Boolean>()
  }

  private suspend inline fun <T> authCall(block: () -> T): AuthResult<T> = try {
    AuthResult.Success(block())
  } catch (error: CancellationException) {
    throw error
  } catch (error: AuthWeakPasswordException) {
    AuthResult.Failure(AuthFailure.WEAK_PASSWORD)
  } catch (error: AuthRestException) {
    AuthResult.Failure(error.toAuthFailure())
  } catch (error: HttpRequestTimeoutException) {
    AuthResult.Failure(AuthFailure.NETWORK)
  } catch (error: HttpRequestException) {
    AuthResult.Failure(AuthFailure.NETWORK)
  } catch (_: Exception) {
    AuthResult.Failure(AuthFailure.UNKNOWN)
  }
}

internal fun AuthRestException.toAuthFailure(): AuthFailure = when (errorCode) {
  AuthErrorCode.InvalidCredentials,
  AuthErrorCode.UserNotFound,
  -> AuthFailure.INVALID_CREDENTIALS
  AuthErrorCode.EmailNotConfirmed -> AuthFailure.EMAIL_NOT_CONFIRMED
  AuthErrorCode.EmailExists,
  AuthErrorCode.UserAlreadyExists,
  AuthErrorCode.Conflict,
  -> AuthFailure.EMAIL_ALREADY_REGISTERED
  AuthErrorCode.EmailAddressInvalid,
  AuthErrorCode.ValidationFailed,
  -> AuthFailure.INVALID_EMAIL
  AuthErrorCode.WeakPassword -> AuthFailure.WEAK_PASSWORD
  AuthErrorCode.OverRequestRateLimit,
  AuthErrorCode.OverEmailSendRateLimit,
  AuthErrorCode.RequestTimeout,
  -> AuthFailure.RATE_LIMITED
  AuthErrorCode.SignupDisabled -> AuthFailure.SIGN_UP_DISABLED
  AuthErrorCode.ProviderDisabled,
  AuthErrorCode.EmailProviderDisabled,
  AuthErrorCode.OauthProviderNotSupported,
  -> AuthFailure.PROVIDER_UNAVAILABLE
  else -> AuthFailure.UNKNOWN
}
