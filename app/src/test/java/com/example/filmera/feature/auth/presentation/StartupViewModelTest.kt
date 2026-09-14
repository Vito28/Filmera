package com.example.filmera.feature.auth.presentation

import com.example.filmera.feature.auth.domain.AuthRepository
import com.example.filmera.feature.auth.domain.AuthResult
import com.example.filmera.feature.auth.domain.SignUpOutcome
import com.example.filmera.feature.preferences.domain.MediaPreference
import com.example.filmera.feature.preferences.domain.PreferenceRepository
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile
import com.example.filmera.feature.preferences.presentation.StartupDestination
import com.example.filmera.feature.preferences.presentation.StartupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class StartupViewModelTest {
  @get:Rule
  val mainDispatcherRule = StartupMainDispatcherRule()

  @Test
  fun `unauthenticated session always opens auth`() = runTest {
    val viewModel = StartupViewModel(
      authRepository = StartupAuthRepository(authenticated = false),
      preferenceRepository = StartupPreferenceRepository(readyProfile()),
    )
    advanceUntilIdle()

    assertEquals(StartupDestination.AUTH, viewModel.destination.value)
  }

  @Test
  fun `authenticated ready profile opens home`() = runTest {
    val viewModel = StartupViewModel(
      authRepository = StartupAuthRepository(authenticated = true),
      preferenceRepository = StartupPreferenceRepository(readyProfile()),
    )
    advanceUntilIdle()

    assertEquals(StartupDestination.HOME, viewModel.destination.value)
  }

  private fun readyProfile() = UserPreferenceProfile(
    preferredMediaTypes = setOf(MediaPreference.MOVIE),
    preferredGenreIds = setOf(18, 28, 53),
    onboardingCompleted = true,
  )

  private class StartupAuthRepository(
    private val authenticated: Boolean,
  ) : AuthRepository {
    override val isAuthenticated: Flow<Boolean> = MutableStateFlow(authenticated)
    override suspend fun awaitStartupSession(): Boolean = authenticated
    override suspend fun signIn(email: String, password: String, rememberMe: Boolean) =
      AuthResult.Success(Unit)
    override suspend fun signUp(
      displayName: String,
      username: String,
      email: String,
      password: String,
    ) = AuthResult.Success(SignUpOutcome.AUTHENTICATED)
    override suspend fun signInWithGoogle() = AuthResult.Success(Unit)
    override suspend fun sendPasswordReset(email: String) = AuthResult.Success(Unit)
    override suspend fun isUsernameAvailable(username: String) = AuthResult.Success(true)
  }

  private class StartupPreferenceRepository(
    private val profile: UserPreferenceProfile?,
  ) : PreferenceRepository {
    override fun observeProfile(): Flow<UserPreferenceProfile?> = MutableStateFlow(profile)
    override suspend fun getProfile(): UserPreferenceProfile? = profile
    override suspend fun saveProfile(profile: UserPreferenceProfile) = Unit
    override suspend fun clearProfile() = Unit
  }

  class StartupMainDispatcherRule(
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
