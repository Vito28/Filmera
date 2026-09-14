package com.example.filmera.feature.person.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.navigation.AppDestination
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.person.domain.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val personRepository: PersonRepository,
) : ViewModel() {
  private val personId = savedStateHandle
    .get<Int>(AppDestination.PersonDetail.PERSON_ID_ARGUMENT)
    ?.takeIf { it > 0 }
  private val _uiState = MutableStateFlow(PersonDetailUiState())
  val uiState: StateFlow<PersonDetailUiState> = _uiState.asStateFlow()
  private var loadJob: Job? = null

  init {
    loadPerson()
  }

  fun retry() {
    loadPerson(force = true)
  }

  private fun loadPerson(force: Boolean = false) {
    val id = personId
    if (id == null) {
      _uiState.value = PersonDetailUiState(LoadState.Error(AppError.NotFound))
      return
    }
    if (!force && loadJob?.isActive == true) return

    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      _uiState.value = PersonDetailUiState(LoadState.Loading)
      _uiState.value = PersonDetailUiState(
        contentState = when (val result = personRepository.loadPerson(id)) {
          is DataResult.Success -> {
            if (result.value.name.isBlank()) LoadState.Empty
            else LoadState.Success(result.value)
          }
          is DataResult.Error -> LoadState.Error(result.error)
        },
      )
    }
  }
}
