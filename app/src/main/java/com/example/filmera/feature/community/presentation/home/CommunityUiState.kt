package com.example.filmera.feature.community.presentation.home

import com.example.filmera.core.common.AppError
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.community.domain.CommunityReview

enum class CommunityTab {
  FOR_YOU,
  REVIEWS,
  FORUM,
}

data class CommunityUiState(
  val selectedTab: CommunityTab = CommunityTab.FOR_YOU,
  val reviewsState: LoadState<List<CommunityReview>> = LoadState.Loading,
  val isLoadingMore: Boolean = false,
  val hasMore: Boolean = true,
  val writeError: AppError? = null,
)

sealed interface CommunityAction {
  data class TabSelected(val tab: CommunityTab) : CommunityAction
  data class HelpfulToggled(val reviewId: String) : CommunityAction
  data object LoadMore : CommunityAction
  data object Retry : CommunityAction
  data object ErrorDismissed : CommunityAction
}
