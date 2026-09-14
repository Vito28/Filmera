package com.example.filmera.feature.preferences.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.discover.domain.RecommendationRepository
import com.example.filmera.feature.preferences.domain.PreferenceRepository
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PreferenceOnboardingViewModel @Inject constructor(
  private val preferenceRepository: PreferenceRepository,
  private val recommendationRepository: RecommendationRepository,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  private val editing = savedStateHandle.get<Boolean>(EDITING_ARGUMENT) == true
  private val _uiState = MutableStateFlow(
    PreferenceOnboardingUiState(isEditing = editing),
  )
  val uiState: StateFlow<PreferenceOnboardingUiState> = _uiState.asStateFlow()

  private val _completed = Channel<Unit>(Channel.BUFFERED)
  val completed = _completed.receiveAsFlow()
  private var existingProfile: UserPreferenceProfile? = null

  init {
    viewModelScope.launch {
      if (editing) {
        preferenceRepository.getProfile()?.let(::applyExistingProfile)
      }
    }
    loadChoices()
  }

  fun onAction(action: PreferenceOnboardingAction) {
    when (action) {
      is PreferenceOnboardingAction.MediaTypeToggled -> toggleMediaType(action)
      is PreferenceOnboardingAction.GenreToggled -> toggleGenre(action.genreId)
      is PreferenceOnboardingAction.CountryToggled -> toggleCountry(action.countryCode)
      is PreferenceOnboardingAction.TitleToggled -> toggleTitle(action)
      is PreferenceOnboardingAction.PersonToggled -> togglePerson(action.personId)
      PreferenceOnboardingAction.ContinueClicked -> continueToNextStep()
      PreferenceOnboardingAction.BackClicked -> moveBack()
      PreferenceOnboardingAction.SkipOptionalClicked -> skipOptionalStep()
      PreferenceOnboardingAction.RetryChoicesClicked -> loadChoices()
      PreferenceOnboardingAction.SaveClicked -> save()
      PreferenceOnboardingAction.ResetConfirmed -> reset()
    }
  }

  private fun applyExistingProfile(profile: UserPreferenceProfile) {
    existingProfile = profile
    _uiState.update {
      it.copy(
        selectedMediaTypes = profile.preferredMediaTypes,
        selectedGenreIds = profile.preferredGenreIds,
        selectedCountries = profile.preferredCountries.ifEmpty {
          setOf(UserPreferenceProfile.DEFAULT_COUNTRY)
        },
        selectedPersonIds = profile.preferredPersonIds,
      )
    }
  }

  private fun toggleMediaType(action: PreferenceOnboardingAction.MediaTypeToggled) {
    _uiState.update { state ->
      state.copy(
        selectedMediaTypes = state.selectedMediaTypes.toggle(action.mediaType),
        validationError = null,
      )
    }
  }

  private fun toggleGenre(genreId: Int) {
    _uiState.update { state ->
      val currentlySelected = genreId in state.selectedGenreIds
      if (
        !currentlySelected &&
        state.selectedGenreIds.size >= UserPreferenceProfile.MAXIMUM_GENRE_COUNT
      ) {
        state.copy(validationError = PreferenceValidationError.GENRES_LIMIT)
      } else {
        state.copy(
          selectedGenreIds = state.selectedGenreIds.toggle(genreId),
          validationError = null,
        )
      }
    }
  }

  private fun toggleCountry(countryCode: String) {
    _uiState.update { state ->
      val currentlySelected = countryCode in state.selectedCountries
      val additionalCountries = state.selectedCountries -
        UserPreferenceProfile.DEFAULT_COUNTRY
      if (
        !currentlySelected &&
        countryCode != UserPreferenceProfile.DEFAULT_COUNTRY &&
        additionalCountries.size >= MAXIMUM_ADDITIONAL_COUNTRIES
      ) {
        state.copy(validationError = PreferenceValidationError.COUNTRIES_LIMIT)
      } else {
        val toggled = state.selectedCountries.toggle(countryCode)
        state.copy(
          selectedCountries = toggled.ifEmpty {
            setOf(UserPreferenceProfile.DEFAULT_COUNTRY)
          },
          validationError = null,
        )
      }
    }
  }

  private fun toggleTitle(action: PreferenceOnboardingAction.TitleToggled) {
    _uiState.update { state ->
      val key = action.media.key
      val titles = when {
        key in state.selectedTitles -> state.selectedTitles - key
        state.selectedTitles.size < MAXIMUM_SELECTED_TITLES ->
          state.selectedTitles + (key to action.media)
        else -> state.selectedTitles
      }
      state.copy(selectedTitles = titles, validationError = null)
    }
  }

  private fun togglePerson(personId: Int) {
    _uiState.update { state ->
      state.copy(
        selectedPersonIds = state.selectedPersonIds.toggle(personId),
        validationError = null,
      )
    }
  }

  private fun continueToNextStep() {
    val state = _uiState.value
    val validationError = when (state.step) {
      PreferenceStep.CONTENT_TYPES ->
        if (state.selectedMediaTypes.isEmpty()) {
          PreferenceValidationError.CONTENT_TYPE_REQUIRED
        } else {
          null
        }
      PreferenceStep.GENRES ->
        if (state.selectedGenreIds.size < UserPreferenceProfile.MINIMUM_GENRE_COUNT) {
          PreferenceValidationError.GENRES_REQUIRED
        } else {
          null
        }
      PreferenceStep.COUNTRIES -> null
      PreferenceStep.TITLES ->
        if (
          state.choicesState is LoadState.Success &&
          finalSelectedTitleCount(state) < MINIMUM_SELECTED_TITLES
        ) {
          PreferenceValidationError.TITLES_REQUIRED
        } else {
          null
        }
      PreferenceStep.PEOPLE,
      PreferenceStep.COMPLETE,
      -> null
    }
    if (validationError != null) {
      _uiState.update { it.copy(validationError = validationError) }
      return
    }

    val nextStep = PreferenceStep.entries.getOrNull(state.step.ordinal + 1) ?: state.step
    _uiState.update { it.copy(step = nextStep, validationError = null) }
  }

  private fun moveBack() {
    _uiState.update { state ->
      val previous = PreferenceStep.entries.getOrNull(state.step.ordinal - 1)
        ?: state.step
      state.copy(step = previous, validationError = null)
    }
  }

  private fun skipOptionalStep() {
    when (_uiState.value.step) {
      PreferenceStep.TITLES -> {
        if (_uiState.value.choicesState !is LoadState.Success) {
          _uiState.update {
            it.copy(step = PreferenceStep.PEOPLE, validationError = null)
          }
        }
      }
      PreferenceStep.PEOPLE -> _uiState.update {
        it.copy(step = PreferenceStep.COMPLETE, validationError = null)
      }
      else -> Unit
    }
  }

  private fun loadChoices() {
    viewModelScope.launch {
      _uiState.update { it.copy(choicesState = LoadState.Loading) }
      val state = when (val result = recommendationRepository.loadOnboardingChoices()) {
        is DataResult.Success -> if (
          result.value.titles.isEmpty() && result.value.people.isEmpty()
        ) {
          LoadState.Empty
        } else {
          LoadState.Success(result.value)
        }
        is DataResult.Error -> LoadState.Error(result.error)
      }
      val existingProfile = if (editing && state is LoadState.Success) {
        preferenceRepository.getProfile()
      } else {
        null
      }
      _uiState.update { current ->
        val selectedFromProfile = if (editing && state is LoadState.Success) {
          state.value.titles
            .filter { item ->
              when (item.type) {
                MediaType.MOVIE -> item.id in existingProfile?.preferredMovieIds.orEmpty()
                MediaType.TV_SHOW -> item.id in existingProfile?.preferredTvIds.orEmpty()
              }
            }
            .associateBy { it.key }
        } else {
          current.selectedTitles
        }
        current.copy(
          choicesState = state,
          selectedTitles = selectedFromProfile,
        )
      }
      if (existingProfile != null) {
        this@PreferenceOnboardingViewModel.existingProfile = existingProfile
      }
    }
  }

  private fun save() {
    val state = _uiState.value
    if (state.isSaving) return

    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true, validationError = null) }
      try {
        val previous = preferenceRepository.getProfile()
        val availableTitles = (state.choicesState as? LoadState.Success)
          ?.value
          ?.titles
          .orEmpty()
        val availableMovieIds = availableTitles
          .filter { it.type == MediaType.MOVIE }
          .map { it.id }
          .toSet()
        val availableTvIds = availableTitles
          .filter { it.type == MediaType.TV_SHOW }
          .map { it.id }
          .toSet()
        val selectedMovieIds = state.selectedTitles.values
          .filter { it.type == MediaType.MOVIE }
          .map { it.id }
          .toSet()
        val selectedTvIds = state.selectedTitles.values
          .filter { it.type == MediaType.TV_SHOW }
          .map { it.id }
          .toSet()
        preferenceRepository.saveProfile(
          UserPreferenceProfile(
            preferredMediaTypes = state.selectedMediaTypes,
            preferredGenreIds = state.selectedGenreIds,
            preferredCountries = state.selectedCountries,
            preferredMovieIds = selectedMovieIds +
              previous?.preferredMovieIds.orEmpty().filterNot { it in availableMovieIds },
            preferredTvIds = selectedTvIds +
              previous?.preferredTvIds.orEmpty().filterNot { it in availableTvIds },
            preferredPersonIds = state.selectedPersonIds,
            preferredLanguageCodes = previous?.preferredLanguageCodes.orEmpty(),
            onboardingCompleted = true,
            updatedAt = System.currentTimeMillis(),
          ),
        )
        _completed.send(Unit)
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        _uiState.update {
          it.copy(
            isSaving = false,
            validationError = PreferenceValidationError.SAVE_FAILED,
          )
        }
      }
    }
  }

  private fun reset() {
    viewModelScope.launch {
      try {
        preferenceRepository.clearProfile()
        recommendationRepository.clearFeedback()
        existingProfile = null
        _uiState.value = PreferenceOnboardingUiState(isEditing = true)
        loadChoices()
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        _uiState.update {
          it.copy(validationError = PreferenceValidationError.SAVE_FAILED)
        }
      }
    }
  }

  private fun finalSelectedTitleCount(state: PreferenceOnboardingUiState): Int {
    val availableKeys = (state.choicesState as? LoadState.Success)
      ?.value
      ?.titles
      .orEmpty()
      .map { it.key }
      .toSet()
    val retainedMovieKeys = existingProfile
      ?.preferredMovieIds
      .orEmpty()
      .map { com.example.filmera.core.model.MediaKey(it, MediaType.MOVIE) }
      .filterNot { it in availableKeys }
    val retainedTvKeys = existingProfile
      ?.preferredTvIds
      .orEmpty()
      .map { com.example.filmera.core.model.MediaKey(it, MediaType.TV_SHOW) }
      .filterNot { it in availableKeys }
    return (state.selectedTitles.keys + retainedMovieKeys + retainedTvKeys).size
  }

  private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value

  companion object {
    const val EDITING_ARGUMENT = "editing"
    private const val MINIMUM_SELECTED_TITLES = 3
    private const val MAXIMUM_SELECTED_TITLES = 15
    private const val MAXIMUM_ADDITIONAL_COUNTRIES = 5
  }
}
