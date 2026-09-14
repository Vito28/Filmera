package com.example.filmera.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.feature.auth.domain.AuthFailure
import com.example.filmera.feature.auth.domain.AuthRepository
import com.example.filmera.feature.auth.domain.AuthResult
import com.example.filmera.feature.auth.domain.SignUpOutcome
import com.example.filmera.feature.preferences.domain.PreferenceRepository
import com.example.filmera.core.sync.NoOpUserDataSyncer
import com.example.filmera.core.sync.UserDataSyncer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
  private val authRepository: AuthRepository,
  private val preferenceRepository: PreferenceRepository,
  private val userDataSyncer: UserDataSyncer = NoOpUserDataSyncer,
) : ViewModel() {
  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  private val effectChannel = Channel<AuthEffect>(Channel.BUFFERED)
  val effects = effectChannel.receiveAsFlow()

  private var usernameCheckJob: Job? = null
  private var navigationSent = false

  init {
    viewModelScope.launch {
      authRepository.isAuthenticated.collect { authenticated ->
        if (authenticated) finishAuthentication()
      }
    }
  }

  fun onEvent(event: AuthEvent) {
    when (event) {
      is AuthEvent.ModeChanged -> changeMode(event.mode)
      is AuthEvent.SignInEmailChanged -> updateState {
        copy(signIn = signIn.copy(email = event.value), message = null)
      }
      is AuthEvent.SignInPasswordChanged -> updateState {
        copy(signIn = signIn.copy(password = event.value), message = null)
      }
      is AuthEvent.RememberMeChanged -> updateState {
        copy(signIn = signIn.copy(rememberMe = event.checked))
      }
      is AuthEvent.DisplayNameChanged -> updateState {
        copy(signUp = signUp.copy(displayName = event.value), message = null)
      }
      is AuthEvent.UsernameChanged -> updateUsername(event.value)
      is AuthEvent.SignUpEmailChanged -> updateState {
        copy(signUp = signUp.copy(email = event.value), message = null)
      }
      is AuthEvent.SignUpPasswordChanged -> updateState {
        copy(
          signUp = signUp.copy(password = event.value),
          passwordStrength = passwordStrength(event.value),
          message = null,
        )
      }
      is AuthEvent.ConfirmPasswordChanged -> updateState {
        copy(signUp = signUp.copy(confirmPassword = event.value), message = null)
      }
      is AuthEvent.TermsChanged -> updateState {
        copy(signUp = signUp.copy(acceptedTerms = event.checked), message = null)
      }
      is AuthEvent.FieldBlurred -> markTouched(event.field)
      AuthEvent.Submit -> submit()
      AuthEvent.ContinueWithGoogle -> continueWithGoogle()
      AuthEvent.ForgotPassword -> forgotPassword()
      AuthEvent.RetryUsernameAvailability -> checkUsername(immediate = true)
      AuthEvent.DismissMessage -> updateState { copy(message = null) }
    }
  }

  private fun changeMode(mode: AuthMode) {
    usernameCheckJob?.cancel()
    updateState {
      copy(
        mode = mode,
        touchedFields = emptySet(),
        errors = emptyMap(),
        message = null,
      )
    }
    if (mode == AuthMode.SIGN_UP && validateUsername(_uiState.value.signUp.username) == null) {
      checkUsername(immediate = false)
    }
  }

  private fun updateUsername(value: String) {
    usernameCheckJob?.cancel()
    val normalized = value.lowercase().take(USERNAME_MAX_LENGTH)
    updateState {
      copy(
        signUp = signUp.copy(username = normalized),
        usernameAvailability = if (validateUsername(normalized) == null) {
          UsernameAvailability.CHECKING
        } else {
          UsernameAvailability.IDLE
        },
        message = null,
      )
    }
    if (validateUsername(normalized) == null) checkUsername(immediate = false)
  }

  private fun checkUsername(immediate: Boolean) {
    val username = _uiState.value.signUp.username
    if (validateUsername(username) != null) return
    usernameCheckJob?.cancel()
    updateState { copy(usernameAvailability = UsernameAvailability.CHECKING) }
    usernameCheckJob = viewModelScope.launch {
      if (!immediate) delay(USERNAME_DEBOUNCE_MILLIS)
      when (val result = authRepository.isUsernameAvailable(username)) {
        is AuthResult.Success -> updateState {
          copy(
            usernameAvailability = if (result.value) {
              UsernameAvailability.AVAILABLE
            } else {
              UsernameAvailability.TAKEN
            },
          )
        }
        is AuthResult.Failure -> updateState {
          copy(usernameAvailability = UsernameAvailability.CHECK_FAILED)
        }
      }
    }
  }

  private fun markTouched(field: AuthField) {
    updateState { copy(touchedFields = touchedFields + field) }
  }

  private fun submit() {
    val state = _uiState.value
    if (state.isSubmitting || state.isSuccess) return
    val requiredFields = if (state.mode == AuthMode.SIGN_IN) signInFields else signUpFields
    val touched = state.touchedFields + requiredFields
    updateState { copy(touchedFields = touched) }
    if (validationErrors(_uiState.value, touched).isNotEmpty()) return

    if (state.mode == AuthMode.SIGN_IN) signIn() else signUp()
  }

  private fun signIn() {
    val form = _uiState.value.signIn
    viewModelScope.launch {
      updateState { copy(isSubmitting = true, message = null) }
      when (
        val result = authRepository.signIn(
          email = form.email,
          password = form.password,
          rememberMe = form.rememberMe,
        )
      ) {
        is AuthResult.Success -> finishAuthentication()
        is AuthResult.Failure -> showFailure(result.reason)
      }
    }
  }

  private fun signUp() {
    val form = _uiState.value.signUp
    viewModelScope.launch {
      updateState { copy(isSubmitting = true, message = null) }
      when (val availability = authRepository.isUsernameAvailable(form.username)) {
        is AuthResult.Failure -> {
          updateState {
            copy(
              isSubmitting = false,
              usernameAvailability = UsernameAvailability.CHECK_FAILED,
              message = availability.reason.toMessage(),
            )
          }
          return@launch
        }
        is AuthResult.Success -> if (!availability.value) {
          updateState {
            copy(
              isSubmitting = false,
              usernameAvailability = UsernameAvailability.TAKEN,
              message = AuthMessage.USERNAME_TAKEN,
            )
          }
          return@launch
        }
      }

      when (
        val result = authRepository.signUp(
          displayName = form.displayName,
          username = form.username,
          email = form.email,
          password = form.password,
        )
      ) {
        is AuthResult.Failure -> showFailure(result.reason)
        is AuthResult.Success -> when (result.value) {
          SignUpOutcome.AUTHENTICATED -> finishAuthentication()
          SignUpOutcome.EMAIL_CONFIRMATION_REQUIRED -> updateState {
            copy(
              mode = AuthMode.SIGN_IN,
              signIn = signIn.copy(email = form.email, password = ""),
              touchedFields = emptySet(),
              errors = emptyMap(),
              isSubmitting = false,
              message = AuthMessage.CONFIRMATION_EMAIL_SENT,
            )
          }
        }
      }
    }
  }

  private fun continueWithGoogle() {
    val state = _uiState.value
    if (state.isGoogleLoading || state.isSubmitting || state.isSuccess) return
    viewModelScope.launch {
      updateState { copy(isGoogleLoading = true, message = null) }
      when (val result = authRepository.signInWithGoogle()) {
        is AuthResult.Success -> updateState { copy(isGoogleLoading = false) }
        is AuthResult.Failure -> {
          updateState {
            copy(isGoogleLoading = false, message = result.reason.toMessage())
          }
        }
      }
    }
  }

  private fun forgotPassword() {
    val state = _uiState.value
    if (state.isResettingPassword || state.isSubmitting) return
    if (validateEmail(state.signIn.email) != null) {
      updateState {
        copy(
          touchedFields = touchedFields + AuthField.SIGN_IN_EMAIL,
          message = AuthMessage.RESET_REQUIRES_VALID_EMAIL,
        )
      }
      return
    }
    viewModelScope.launch {
      updateState { copy(isResettingPassword = true, message = null) }
      when (val result = authRepository.sendPasswordReset(state.signIn.email)) {
        is AuthResult.Success -> updateState {
          copy(isResettingPassword = false, message = AuthMessage.RESET_EMAIL_SENT)
        }
        is AuthResult.Failure -> updateState {
          copy(isResettingPassword = false, message = result.reason.toMessage())
        }
      }
    }
  }

  private suspend fun finishAuthentication() {
    if (navigationSent) return
    navigationSent = true
    updateState {
      copy(
        isSubmitting = false,
        isGoogleLoading = false,
        isSuccess = true,
        message = null,
      )
    }
    try {
      userDataSyncer.syncNow()
    } catch (error: CancellationException) {
      throw error
    } catch (_: Exception) {
      // Authentication still succeeds offline; pending Room data will retry later.
    }
    val profile = try {
      preferenceRepository.getProfile()
    } catch (error: CancellationException) {
      throw error
    } catch (_: Exception) {
      null
    }
    val destination = profile
      ?.takeIf { it.isReadyForRecommendations }
      ?.let { AuthNextDestination.HOME }
      ?: AuthNextDestination.PREFERENCES
    effectChannel.send(AuthEffect.Navigate(destination))
  }

  private fun showFailure(failure: AuthFailure) {
    updateState {
      copy(
        isSubmitting = false,
        isGoogleLoading = false,
        isResettingPassword = false,
        message = failure.toMessage(),
      )
    }
  }

  private inline fun updateState(transform: AuthUiState.() -> AuthUiState) {
    val updated = _uiState.value.transform()
    _uiState.value = updated.copy(
      errors = validationErrors(updated),
    )
  }

  private companion object {
    const val USERNAME_MAX_LENGTH = 24
    const val USERNAME_DEBOUNCE_MILLIS = 500L
  }
}

private fun AuthFailure.toMessage(): AuthMessage = when (this) {
  AuthFailure.INVALID_CREDENTIALS -> AuthMessage.INVALID_CREDENTIALS
  AuthFailure.EMAIL_NOT_CONFIRMED -> AuthMessage.EMAIL_NOT_CONFIRMED
  AuthFailure.EMAIL_ALREADY_REGISTERED -> AuthMessage.EMAIL_ALREADY_REGISTERED
  AuthFailure.INVALID_EMAIL -> AuthMessage.INVALID_EMAIL
  AuthFailure.WEAK_PASSWORD -> AuthMessage.WEAK_PASSWORD
  AuthFailure.RATE_LIMITED -> AuthMessage.RATE_LIMITED
  AuthFailure.SIGN_UP_DISABLED -> AuthMessage.SIGN_UP_DISABLED
  AuthFailure.PROVIDER_UNAVAILABLE -> AuthMessage.PROVIDER_UNAVAILABLE
  AuthFailure.NETWORK -> AuthMessage.NETWORK
  AuthFailure.UNKNOWN -> AuthMessage.UNKNOWN
}
