package com.example.filmera.feature.discover.presentation

import com.example.filmera.core.common.AppError
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.discover.domain.ExploreContent
import com.example.filmera.feature.discover.domain.ExploreFilterState
import com.example.filmera.feature.discover.domain.ExploreLayoutMode
import com.example.filmera.feature.discover.domain.ExploreProvider
import com.example.filmera.feature.discover.domain.RecommendationFeedbackAction
import com.example.filmera.feature.discover.domain.RecommendedContent
import com.example.filmera.feature.discover.domain.RankedMediaItem

enum class DiscoverTab {
  RECOMMENDED,
  RANKINGS,
  EXPLORE,
}

data class DiscoverUiState(
  val contentState: LoadState<RecommendedContent> = LoadState.Loading,
  val rankingsState: LoadState<List<RankedMediaItem>> = LoadState.Loading,
  val explore: ExploreUiState = ExploreUiState(),
  val selectedGenreId: Int? = null,
  val isRefreshing: Boolean = false,
  val feedbackSaved: RecommendationFeedbackAction? = null,
)

data class ExploreUiState(
  val contentState: LoadState<ExploreContent> = LoadState.Loading,
  val providerState: LoadState<List<ExploreProvider>> = LoadState.Loading,
  val filters: ExploreFilterState = ExploreFilterState(),
  val layoutMode: ExploreLayoutMode = ExploreLayoutMode.GRID,
  val isRefreshing: Boolean = false,
  val isLoadingMore: Boolean = false,
  val refreshError: AppError? = null,
  val paginationError: AppError? = null,
  val gridFirstVisibleIndex: Int = 0,
  val gridFirstVisibleOffset: Int = 0,
  val listFirstVisibleIndex: Int = 0,
  val listFirstVisibleOffset: Int = 0,
)

sealed interface DiscoverAction {
  data class GenreSelected(val genreId: Int?) : DiscoverAction
  data class FeedbackSubmitted(
    val media: MediaItem,
    val feedback: RecommendationFeedbackAction,
  ) : DiscoverAction
  data object RefreshRequested : DiscoverAction
  data object RetryRequested : DiscoverAction
  data object RankingsOpened : DiscoverAction
  data object RankingsRetryRequested : DiscoverAction
  data object ExploreOpened : DiscoverAction
  data class ExploreFiltersChanged(
    val filters: ExploreFilterState,
  ) : DiscoverAction
  data class ExploreLayoutChanged(
    val layoutMode: ExploreLayoutMode,
  ) : DiscoverAction
  data class ExploreScrollChanged(
    val layoutMode: ExploreLayoutMode,
    val firstVisibleIndex: Int,
    val firstVisibleOffset: Int,
  ) : DiscoverAction
  data object ExploreRetryRequested : DiscoverAction
  data object ExploreLoadMoreRequested : DiscoverAction
  data object ExploreProvidersRetryRequested : DiscoverAction
  data object FeedbackMessageDismissed : DiscoverAction
}
