package com.example.filmera.feature.auth.presentation

import com.example.filmera.feature.auth.domain.AuthFailure
import com.example.filmera.feature.auth.domain.AuthRepository
import com.example.filmera.feature.auth.domain.AuthResult
import com.example.filmera.feature.auth.domain.SignUpOutcome
import com.example.filmera.feature.preferences.domain.PreferenceRepository
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
  @get:Rule
  val mainDispatcherRule = AuthMainDispatcherRule()

  @Test
  fun `username availability is debounced and latest value wins`() = runTest {
    val authRepository = FakeAuthRepository()
    val viewModel = AuthViewModel(authRepository, FakePreferenceRepository())

    viewModel.onEvent(AuthEvent.ModeChanged(AuthMode.SIGN_UP))
    viewModel.onEvent(AuthEvent.UsernameChanged("movie_fan"))
    viewModel.onEvent(AuthEvent.UsernameChanged("movie_fan26"))
    advanceTimeBy(499)
    runCurrent()

    assertTrue(authRepository.usernameChecks.isEmpty())

    advanceTimeBy(1)
    advanceUntilIdle()

    assertEquals(listOf("movie_fan26"), authRepository.usernameChecks)
    assertEquals(UsernameAvailability.AVAILABLE, viewModel.uiState.value.usernameAvailability)
  }

  @Test
  fun `confirmed sign up prepares sign in without exposing password`() = runTest {
    val authRepository = FakeAuthRepository(
      signUpResult = AuthResult.Success(SignUpOutcome.EMAIL_CONFIRMATION_REQUIRED),
    )
    val viewModel = AuthViewModel(authRepository, FakePreferenceRepository())

    viewModel.onEvent(AuthEvent.ModeChanged(AuthMode.SIGN_UP))
    viewModel.onEvent(AuthEvent.DisplayNameChanged("Movie Fan"))
    viewModel.onEvent(AuthEvent.UsernameChanged("movie_fan26"))
    viewModel.onEvent(AuthEvent.SignUpEmailChanged("fan@example.com"))
    viewModel.onEvent(AuthEvent.SignUpPasswordChanged("Cinema!2026"))
    viewModel.onEvent(AuthEvent.ConfirmPasswordChanged("Cinema!2026"))
    viewModel.onEvent(AuthEvent.TermsChanged(true))
    advanceUntilIdle()

    viewModel.onEvent(AuthEvent.Submit)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(AuthMode.SIGN_IN, state.mode)
    assertEquals("fan@example.com", state.signIn.email)
    assertEquals("", state.signIn.password)
    assertEquals(AuthMessage.CONFIRMATION_EMAIL_SENT, state.message)
    assertEquals(1, authRepository.signUpCalls)
  }

  @Test
  fun `successful sign in emits one destination effect`() = runTest {
    val authRepository = FakeAuthRepository()
    val viewModel = AuthViewModel(authRepository, FakePreferenceRepository())
    val effect = async { viewModel.effects.first() }

    viewModel.onEvent(AuthEvent.SignInEmailChanged("fan@example.com"))
    viewModel.onEvent(AuthEvent.SignInPasswordChanged("Cinema!2026"))
    viewModel.onEvent(AuthEvent.Submit)
    advanceUntilIdle()

    assertEquals(
      AuthEffect.Navigate(AuthNextDestination.PREFERENCES),
      effect.await(),
    )
    assertTrue(viewModel.uiState.value.isSuccess)
    assertEquals(1, authRepository.signInCalls)
  }

  @Test
  fun `invalid form never calls backend`() = runTest {
    val authRepository = FakeAuthRepository()
    val viewModel = AuthViewModel(authRepository, FakePreferenceRepository())

    viewModel.onEvent(AuthEvent.SignInEmailChanged("invalid"))
    viewModel.onEvent(AuthEvent.Submit)
    advanceUntilIdle()

    assertEquals(0, authRepository.signInCalls)
    assertFalse(viewModel.uiState.value.isSubmitting)
    assertEquals(
      AuthValidationError.INVALID_EMAIL,
      viewModel.uiState.value.errors[AuthField.SIGN_IN_EMAIL],
    )
  }

  private class FakeAuthRepository(
    private val signUpResult: AuthResult<SignUpOutcome> =
      AuthResult.Success(SignUpOutcome.AUTHENTICATED),
    private val usernameResult: AuthResult<Boolean> = AuthResult.Success(true),
  ) : AuthRepository {
    private val authenticated = MutableStateFlow(false)
    override val isAuthenticated: Flow<Boolean> = authenticated
    val usernameChecks = mutableListOf<String>()
    var signInCalls = 0
    var signUpCalls = 0

    override suspend fun awaitStartupSession(): Boolean = authenticated.value

    override suspend fun signIn(
      email: String,
      password: String,
      rememberMe: Boolean,
    ): AuthResult<Unit> {
      signInCalls++
      return AuthResult.Success(Unit)
    }

    override suspend fun signUp(
      displayName: String,
      username: String,
      email: String,
      password: String,
    ): AuthResult<SignUpOutcome> {
      signUpCalls++
      return signUpResult
    }

    override suspend fun signInWithGoogle(): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun sendPasswordReset(email: String): AuthResult<Unit> =
      AuthResult.Success(Unit)

    override suspend fun isUsernameAvailable(username: String): AuthResult<Boolean> {
      usernameChecks += username
      return usernameResult
    }
  }

  private class FakePreferenceRepository(
    private val profile: UserPreferenceProfile? = null,
  ) : PreferenceRepository {
    override fun observeProfile(): Flow<UserPreferenceProfile?> = MutableStateFlow(profile)

    override suspend fun getProfile(): UserPreferenceProfile? = profile

    override suspend fun saveProfile(profile: UserPreferenceProfile) = Unit

    override suspend fun clearProfile() = Unit
  }

  class AuthMainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
  ) : TestWatcher() {
    override fun starting(description: Description) {
      Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
      Dispatchers.resetMain()
    }
  }
}
