package com.example.filmera.feature.profile.presentation

import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.profile.domain.UserProfile

data class ProfileLibrarySummary(
  val savedCount: Int = 0,
  val watchlistCount: Int = 0,
  val watchingCount: Int = 0,
  val favoriteCount: Int = 0,
)

data class ProfileHeaderPoster(
  val path: String,
  val title: String,
)

data class ProfileLibraryContent(
  val summary: ProfileLibrarySummary = ProfileLibrarySummary(),
  val headerPosters: List<ProfileHeaderPoster> = emptyList(),
)

data class ProfileUiState(
  val identityState: LoadState<UserProfile> = LoadState.Loading,
  val libraryState: LoadState<ProfileLibraryContent> = LoadState.Loading,
)
