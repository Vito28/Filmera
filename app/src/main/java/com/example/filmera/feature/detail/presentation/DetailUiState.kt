package com.example.filmera.feature.detail.presentation

import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.detail.domain.DetailContent
import com.example.filmera.feature.library.domain.WatchStatus
import com.example.filmera.feature.community.domain.CommunityReview
import com.example.filmera.feature.community.presentation.CommunityReviewComposerState

data class DetailUiState(
  val contentState: LoadState<DetailContent> = LoadState.Loading,
  val favoriteKeys: Set<MediaKey> = emptySet(),
  val watchStatus: WatchStatus = WatchStatus.NONE,
  val hasFavoriteWriteError: Boolean = false,
  val hasLibraryWriteError: Boolean = false,
  val communityState: LoadState<List<CommunityReview>> = LoadState.Loading,
  val selectedRating: Int? = null,
  val filmeraRating: Double? = null,
  val ratingCount: Long = 0,
  val isRatingSubmitting: Boolean = false,
  val pendingHelpfulReviewIds: Set<String> = emptySet(),
  val hasCommunityWriteError: Boolean = false,
  val reviewComposer: CommunityReviewComposerState = CommunityReviewComposerState(),
)
