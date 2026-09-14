package com.example.filmera.feature.home.presentation

import com.example.filmera.core.common.AppError
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.home.domain.AnimeTopic
import com.example.filmera.feature.home.domain.HomeChannel
import com.example.filmera.feature.home.domain.HomeContent
import com.example.filmera.feature.home.domain.HomeFeedKey
import com.example.filmera.feature.home.domain.HomeSectionType
import com.example.filmera.feature.community.domain.CommunityReview
import com.example.filmera.feature.community.domain.ReviewReaction
import com.example.filmera.feature.community.presentation.CommunityReviewComposerState

typealias HomeReviewComposerState = CommunityReviewComposerState

data class HomeUiState(
  val contentState: LoadState<HomeContent> = LoadState.Loading,
  val selectedChannel: HomeChannel = HomeChannel.FOR_YOU,
  val selectedAnimeTopic: AnimeTopic = AnimeTopic.ALL,
  val isFeedRefreshing: Boolean = false,
  val feedRefreshError: AppError? = null,
  val unreadNotificationCount: Int = 0,
  val preferredGenreIds: Set<Int> = emptySet(),
  val favoriteItems: List<MediaItem> = emptyList(),
  val watchlistItems: List<MediaItem> = emptyList(),
  val favoriteKeys: Set<MediaKey> = emptySet(),
  val favoriteMutations: Set<MediaKey> = emptySet(),
  val favoriteWriteError: AppError? = null,
  val watchlistKeys: Set<MediaKey> = emptySet(),
  val watchlistMutations: Set<MediaKey> = emptySet(),
  val libraryWriteError: AppError? = null,
  val communityPulseState: LoadState<List<CommunityReview>> = LoadState.Loading,
  val ratingSheet: HomeRatingSheetState = HomeRatingSheetState(),
  val communityWriteError: AppError? = null,
  val reviewComposer: HomeReviewComposerState = HomeReviewComposerState(),
) {
  val feedKey: HomeFeedKey
    get() = HomeFeedKey(
      channel = selectedChannel,
      animeTopic = if (selectedChannel.isAnime) selectedAnimeTopic else AnimeTopic.ALL,
    )
}

data class HomeScrollPosition(
  val index: Int = 0,
  val offset: Int = 0,
)

data class HomeRatingSheetState(
  val reviewId: String? = null,
  val mediaId: Long? = null,
  val mediaTitle: String = "",
  val selectedRating: Int? = null,
  val existingRating: Int? = null,
  val isSubmitting: Boolean = false,
) {
  val isVisible: Boolean
    get() = reviewId != null && mediaId != null
}

sealed interface HomeAction {
  data class ChannelSelected(val channel: HomeChannel) : HomeAction
  data class AnimeTopicSelected(val topic: AnimeTopic) : HomeAction
  data class LoadMore(val sectionType: HomeSectionType) : HomeAction
  data class RetrySection(val sectionType: HomeSectionType) : HomeAction
  data class FavoriteToggled(val item: MediaItem) : HomeAction
  data class WatchlistToggled(val item: MediaItem) : HomeAction
  data class HelpfulToggled(val reviewId: String) : HomeAction
  data class ReactionSelected(
    val reviewId: String,
    val reaction: ReviewReaction,
  ) : HomeAction
  data class RatingRequested(val reviewId: String) : HomeAction
  data class RatingSelected(val rating: Int) : HomeAction
  data class ReviewComposerRequested(val media: MediaItem) : HomeAction
  data class ReviewRatingSelected(val rating: Int) : HomeAction
  data class ReviewHeadlineChanged(val value: String) : HomeAction
  data class ReviewBodyChanged(val value: String) : HomeAction
  data class ReviewSpoilerChanged(val value: Boolean) : HomeAction
  data class ScrollPositionChanged(
    val feedKey: HomeFeedKey,
    val position: HomeScrollPosition,
  ) : HomeAction
  data object Retried : HomeAction
  data object RefreshErrorDismissed : HomeAction
  data object FavoriteErrorDismissed : HomeAction
  data object LibraryErrorDismissed : HomeAction
  data object CommunityRetried : HomeAction
  data object RatingSubmitted : HomeAction
  data object RatingRemoved : HomeAction
  data object RatingDismissed : HomeAction
  data object CommunityErrorDismissed : HomeAction
  data object ReviewSubmitted : HomeAction
  data object ReviewDismissed : HomeAction
}
