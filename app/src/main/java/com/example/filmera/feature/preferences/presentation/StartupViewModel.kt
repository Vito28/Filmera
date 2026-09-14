package com.example.filmera.feature.preferences.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.feature.auth.domain.AuthRepository
import com.example.filmera.feature.preferences.domain.PreferenceRepository
import com.example.filmera.core.sync.NoOpUserDataSyncer
import com.example.filmera.core.sync.UserDataSyncer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StartupDestination {
  AUTH,
  PREFERENCES,
  HOME,
}

@HiltViewModel
class StartupViewModel @Inject constructor(
  private val authRepository: AuthRepository,
  private val preferenceRepository: PreferenceRepository,
  private val userDataSyncer: UserDataSyncer = NoOpUserDataSyncer,
) : ViewModel() {
  private val _destination = MutableStateFlow<StartupDestination?>(null)
  val destination: StateFlow<StartupDestination?> = _destination.asStateFlow()

  init {
    viewModelScope.launch {
      _destination.value = try {
        if (!authRepository.awaitStartupSession()) {
          StartupDestination.AUTH
        } else {
          try {
            userDataSyncer.syncNow()
          } catch (error: CancellationException) {
            throw error
          } catch (_: Exception) {
            // Room remains usable when the device starts without a connection.
          }
          if (preferenceRepository.getProfile()?.isReadyForRecommendations == true) {
            StartupDestination.HOME
          } else {
            StartupDestination.PREFERENCES
          }
        }
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        StartupDestination.AUTH
      }
    }
  }
}
