package com.example.filmera.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.home.domain.HomeChannel
import com.example.filmera.feature.home.domain.HomeFeedKey
import com.example.filmera.feature.home.domain.HomeRepository
import com.example.filmera.feature.home.domain.HomeSectionType
import com.example.filmera.feature.library.domain.LibraryItem
import com.example.filmera.feature.library.domain.LibraryRepository
import com.example.filmera.feature.library.domain.WatchStatus
import com.example.filmera.feature.profile.domain.ProfileRepository
import com.example.filmera.feature.profile.domain.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProfileViewModel @Inject constructor(
  libraryRepository: LibraryRepository,
  profileRepository: ProfileRepository,
  homeRepository: HomeRepository,
) : ViewModel() {
  private val identityState = flow<LoadState<UserProfile>> {
    emit(
      when (val result = profileRepository.getCurrentProfile()) {
        is DataResult.Success -> LoadState.Success(result.value)
        is DataResult.Error -> LoadState.Error(result.error)
      },
    )
  }.onStart { emit(LoadState.Loading) }

  private val libraryState = libraryRepository.observeLibrary()
    .map<List<LibraryItem>, LoadState<ProfileLibraryContent>> { items ->
      val localPosters = items.toLibraryHeaderPosters()
      LoadState.Success(
        ProfileLibraryContent(
          summary = ProfileLibrarySummary(
            savedCount = items.size,
            watchlistCount = items.count { it.watchStatus == WatchStatus.WATCHLIST },
            watchingCount = items.count { it.watchStatus == WatchStatus.WATCHING },
            favoriteCount = items.count(LibraryItem::isFavorite),
          ),
          headerPosters = localPosters,
        ),
      )
    }
    .onStart { emit(LoadState.Loading) }
    .catch {
      emit(LoadState.Error(AppError.Unknown))
    }

  private val trendingPosters = flow {
    val result = homeRepository.loadSectionPage(
      feedKey = HomeFeedKey(HomeChannel.FOR_YOU),
      sectionType = HomeSectionType.TRENDING,
      page = 1,
    )
    emit(
      when (result) {
        is DataResult.Success -> result.value.items.toMediaHeaderPosters()
        is DataResult.Error -> emptyList()
      },
    )
  }.onStart { emit(emptyList()) }
    .catch { emit(emptyList()) }

  val uiState: StateFlow<ProfileUiState> = combine(
    identityState,
    libraryState,
    trendingPosters,
  ) { identity, library, fallbackPosters ->
    ProfileUiState(
      identityState = identity,
      libraryState = library.withPosterFallback(fallbackPosters),
    )
  }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
      initialValue = ProfileUiState(),
    )
}

private fun LoadState<ProfileLibraryContent>.withPosterFallback(
  fallbackPosters: List<ProfileHeaderPoster>,
): LoadState<ProfileLibraryContent> = when (this) {
  is LoadState.Success -> LoadState.Success(
    value.copy(
      headerPosters = (value.headerPosters + fallbackPosters)
        .distinctBy(ProfileHeaderPoster::path)
        .take(MAXIMUM_HEADER_POSTERS),
    ),
  )
  else -> this
}

private fun List<LibraryItem>.toLibraryHeaderPosters(): List<ProfileHeaderPoster> =
  sortedWith(
    compareByDescending<LibraryItem>(LibraryItem::isFavorite)
      .thenByDescending(LibraryItem::updatedAt),
  ).map(LibraryItem::media).toMediaHeaderPosters()

private fun List<MediaItem>.toMediaHeaderPosters(): List<ProfileHeaderPoster> =
  asSequence()
    .filterNot(MediaItem::adult)
    .mapNotNull { media ->
      val path = media.posterPath?.takeIf(String::isNotBlank)
        ?: media.backdropPath?.takeIf(String::isNotBlank)
        ?: return@mapNotNull null
      ProfileHeaderPoster(path = path, title = media.title)
    }
    .distinctBy(ProfileHeaderPoster::path)
    .take(MAXIMUM_HEADER_POSTERS)
    .toList()

private const val MAXIMUM_HEADER_POSTERS = 5
