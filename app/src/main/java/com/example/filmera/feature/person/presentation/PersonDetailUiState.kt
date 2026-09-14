package com.example.filmera.feature.person.presentation

import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.person.domain.PersonDetails

data class PersonDetailUiState(
  val contentState: LoadState<PersonDetails> = LoadState.Loading,
)
