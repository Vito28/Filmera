package com.example.filmera.feature.preferences.presentation

import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.discover.domain.OnboardingChoices
import com.example.filmera.feature.preferences.domain.MediaPreference
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile

enum class PreferenceStep {
  CONTENT_TYPES,
  GENRES,
  COUNTRIES,
  TITLES,
  PEOPLE,
  COMPLETE,
}

enum class PreferenceValidationError {
  CONTENT_TYPE_REQUIRED,
  GENRES_REQUIRED,
  GENRES_LIMIT,
  COUNTRIES_LIMIT,
  TITLES_REQUIRED,
  SAVE_FAILED,
}

data class PreferenceOnboardingUiState(
  val step: PreferenceStep = PreferenceStep.CONTENT_TYPES,
  val selectedMediaTypes: Set<MediaPreference> = emptySet(),
  val selectedGenreIds: Set<Int> = emptySet(),
  val selectedCountries: Set<String> = setOf(UserPreferenceProfile.DEFAULT_COUNTRY),
  val selectedTitles: Map<MediaKey, MediaItem> = emptyMap(),
  val selectedPersonIds: Set<Int> = emptySet(),
  val choicesState: LoadState<OnboardingChoices> = LoadState.Loading,
  val validationError: PreferenceValidationError? = null,
  val isSaving: Boolean = false,
  val isEditing: Boolean = false,
) {
  val stepNumber: Int
    get() = (step.ordinal + 1).coerceAtMost(TOTAL_QUESTION_STEPS)

  companion object {
    const val TOTAL_QUESTION_STEPS = 5
  }
}

sealed interface PreferenceOnboardingAction {
  data class MediaTypeToggled(val mediaType: MediaPreference) : PreferenceOnboardingAction
  data class GenreToggled(val genreId: Int) : PreferenceOnboardingAction
  data class CountryToggled(val countryCode: String) : PreferenceOnboardingAction
  data class TitleToggled(val media: MediaItem) : PreferenceOnboardingAction
  data class PersonToggled(val personId: Int) : PreferenceOnboardingAction
  data object ContinueClicked : PreferenceOnboardingAction
  data object BackClicked : PreferenceOnboardingAction
  data object SkipOptionalClicked : PreferenceOnboardingAction
  data object RetryChoicesClicked : PreferenceOnboardingAction
  data object SaveClicked : PreferenceOnboardingAction
  data object ResetConfirmed : PreferenceOnboardingAction
}
