package com.example.filmera.feature.detail.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.navigation.AppDestination
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.detail.domain.DetailContent
import com.example.filmera.feature.detail.domain.DetailRepository
import com.example.filmera.feature.library.domain.LibraryItem
import com.example.filmera.feature.library.domain.LibraryRepository
import com.example.filmera.feature.library.domain.WatchStatus
import com.example.filmera.feature.community.domain.CommunityRepository
import com.example.filmera.feature.community.domain.CommunityReview
import com.example.filmera.feature.community.domain.PublishReviewRequest
import com.example.filmera.feature.community.presentation.CommunityReviewComposerState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val detailRepository: DetailRepository,
  private val libraryRepository: LibraryRepository,
  private val communityRepository: CommunityRepository,
) : ViewModel() {
  private val mediaKey = createMediaKey(savedStateHandle)
  private val contentState = MutableStateFlow<LoadState<DetailContent>>(LoadState.Loading)
  private val hasFavoriteWriteError = MutableStateFlow(false)
  private val hasLibraryWriteError = MutableStateFlow(false)
  private val communityUi = MutableStateFlow(DetailCommunityUi())
  private val librarySelection = libraryRepository.observeLibrary()
    .map(::createLibrarySelection)
    .catch { emit(LibrarySelection()) }

  val uiState: StateFlow<DetailUiState> = combine(
    contentState,
    librarySelection,
    hasFavoriteWriteError,
    hasLibraryWriteError,
    communityUi,
  ) { content, library, hasFavoriteError, hasLibraryError, community ->
    DetailUiState(
      contentState = content,
      favoriteKeys = library.favoriteKeys,
      watchStatus = library.watchStatus,
      hasFavoriteWriteError = hasFavoriteError,
      hasLibraryWriteError = hasLibraryError,
      communityState = community.state,
      selectedRating = community.selectedRating,
      filmeraRating = community.filmeraRating,
      ratingCount = community.ratingCount,
      isRatingSubmitting = community.isRatingSubmitting,
      pendingHelpfulReviewIds = community.pendingHelpfulReviewIds,
      hasCommunityWriteError = community.hasWriteError,
      reviewComposer = community.reviewComposer,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
    initialValue = DetailUiState(),
  )
  private var loadJob: Job? = null

  init {
    loadDetails()
    loadCommunity()
  }

  fun retry() {
    loadDetails(force = true)
    loadCommunity()
  }

  fun retryCommunity() = loadCommunity()

  fun openReviewComposer(item: MediaItem) {
    communityUi.value = communityUi.value.copy(
      reviewComposer = CommunityReviewComposerState(media = item),
      hasWriteError = false,
    )
  }

  fun dismissReviewComposer() {
    if (!communityUi.value.reviewComposer.isSubmitting) {
      communityUi.value = communityUi.value.copy(reviewComposer = CommunityReviewComposerState())
    }
  }

  fun selectReviewRating(rating: Int) {
    val composer = communityUi.value.reviewComposer
    if (!composer.isSubmitting) {
      communityUi.value = communityUi.value.copy(
        reviewComposer = composer.copy(rating = rating.coerceIn(1, 10)),
      )
    }
  }

  fun changeReviewHeadline(value: String) {
    val composer = communityUi.value.reviewComposer
    if (!composer.isSubmitting) {
      communityUi.value = communityUi.value.copy(
        reviewComposer = composer.copy(
          headline = value.take(120),
          headlineInvalid = false,
        ),
      )
    }
  }

  fun changeReviewBody(value: String) {
    val composer = communityUi.value.reviewComposer
    if (!composer.isSubmitting) {
      communityUi.value = communityUi.value.copy(
        reviewComposer = composer.copy(
          body = value.take(5_000),
          bodyInvalid = false,
        ),
      )
    }
  }

  fun changeReviewSpoiler(value: Boolean) {
    val composer = communityUi.value.reviewComposer
    if (!composer.isSubmitting) {
      communityUi.value = communityUi.value.copy(
        reviewComposer = composer.copy(containsSpoilers = value),
      )
    }
  }

  fun publishReview() {
    val composer = communityUi.value.reviewComposer
    val media = composer.media ?: return
    val rating = composer.rating
    val normalizedBody = composer.body.trim()
    val invalidBody = normalizedBody.length !in 20..5_000
    if (composer.isSubmitting || rating == null || invalidBody) {
      communityUi.value = communityUi.value.copy(
        reviewComposer = composer.copy(bodyInvalid = invalidBody),
      )
      return
    }
    communityUi.value = communityUi.value.copy(
      reviewComposer = composer.copy(isSubmitting = true),
      hasWriteError = false,
    )
    viewModelScope.launch {
      when (val result = communityRepository.publishReview(
        PublishReviewRequest(
          mediaKey = media.key,
          rating = rating,
          headline = composer.headline.trim(),
          body = normalizedBody,
          containsSpoilers = composer.containsSpoilers,
        ),
      )) {
        is DataResult.Success -> {
          val reviews = (listOf(result.value) + communityUi.value.reviews)
            .distinctBy(CommunityReview::id)
          communityUi.value = communityUi.value.copy(
            state = LoadState.Success(reviews),
            reviews = reviews,
            selectedRating = result.value.viewerRating ?: rating,
            filmeraRating = result.value.filmeraRating ?: communityUi.value.filmeraRating,
            ratingCount = result.value.ratingCount,
            reviewComposer = CommunityReviewComposerState(),
          )
        }
        is DataResult.Error -> communityUi.value = communityUi.value.copy(
          reviewComposer = composer.copy(isSubmitting = false),
          hasWriteError = true,
        )
      }
    }
  }

  fun submitRating(rating: Int) {
    val key = mediaKey ?: return
    if (rating !in 1..10 || communityUi.value.isRatingSubmitting) return
    val previousRating = communityUi.value.selectedRating
    viewModelScope.launch {
      communityUi.value = communityUi.value.copy(
        selectedRating = rating,
        isRatingSubmitting = true,
        hasWriteError = false,
      )
      when (communityRepository.setMediaRating(key, rating)) {
        is DataResult.Success -> {
          communityUi.value = communityUi.value.copy(isRatingSubmitting = false)
          loadCommunity(preserveSelectedRating = rating)
        }
        is DataResult.Error -> communityUi.value = communityUi.value.copy(
          selectedRating = previousRating,
          isRatingSubmitting = false,
          hasWriteError = true,
        )
      }
    }
  }

  fun toggleHelpful(reviewId: String) {
    val review = communityUi.value.reviews.firstOrNull { it.id == reviewId } ?: return
    if (reviewId in communityUi.value.pendingHelpfulReviewIds) return
    val previous = review.helpful
    val next = previous.copy(
      isHelpful = !previous.isHelpful,
      count = (previous.count + if (previous.isHelpful) -1 else 1).coerceAtLeast(0),
      isSubmitting = true,
    )
    updateReview(reviewId) { it.copy(helpful = next) }
    communityUi.value = communityUi.value.copy(
      pendingHelpfulReviewIds = communityUi.value.pendingHelpfulReviewIds + reviewId,
      hasWriteError = false,
    )
    viewModelScope.launch {
      when (communityRepository.setHelpful(reviewId, next.isHelpful)) {
        is DataResult.Success -> {
          updateReview(reviewId) { it.copy(helpful = next.copy(isSubmitting = false)) }
          communityUi.value = communityUi.value.copy(
            pendingHelpfulReviewIds = communityUi.value.pendingHelpfulReviewIds - reviewId,
          )
        }
        is DataResult.Error -> {
          updateReview(reviewId) { it.copy(helpful = previous) }
          communityUi.value = communityUi.value.copy(
            pendingHelpfulReviewIds = communityUi.value.pendingHelpfulReviewIds - reviewId,
            hasWriteError = true,
          )
        }
      }
    }
  }

  fun toggleFavorite(item: MediaItem) {
    viewModelScope.launch {
      try {
        val shouldBeFavorite = item.key !in uiState.value.favoriteKeys
        libraryRepository.setFavorite(item, shouldBeFavorite)
        hasFavoriteWriteError.value = false
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        hasFavoriteWriteError.value = true
      }
    }
  }

  fun toggleWatchlist(item: MediaItem) {
    viewModelScope.launch {
      try {
        val nextStatus = if (uiState.value.watchStatus == WatchStatus.NONE) {
          WatchStatus.WATCHLIST
        } else {
          WatchStatus.NONE
        }
        libraryRepository.setWatchStatus(item, nextStatus)
        hasLibraryWriteError.value = false
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        hasLibraryWriteError.value = true
      }
    }
  }

  private fun loadDetails(force: Boolean = false) {
    val key = mediaKey
    if (key == null) {
      contentState.value = LoadState.Error(AppError.NotFound)
      return
    }
    if (!force && loadJob?.isActive == true) return
    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      contentState.value = LoadState.Loading
      val state = when (val result = detailRepository.loadDetails(key)) {
        is DataResult.Success -> LoadState.Success(result.value)
        is DataResult.Error -> LoadState.Error(result.error)
      }
      contentState.value = state
    }
  }

  private fun loadCommunity(preserveSelectedRating: Int? = null) {
    val key = mediaKey ?: return
    viewModelScope.launch {
      communityUi.value = communityUi.value.copy(
        state = LoadState.Loading,
        pendingHelpfulReviewIds = emptySet(),
      )
      val summary = when (val result = communityRepository.getMediaRatingSummary(key)) {
        is DataResult.Success -> result.value
        is DataResult.Error -> null
      }
      when (val result = communityRepository.getMediaReviews(key, limit = 20)) {
        is DataResult.Success -> {
          val reviews = result.value
          communityUi.value = communityUi.value.copy(
            state = if (reviews.isEmpty()) LoadState.Empty else LoadState.Success(reviews),
            reviews = reviews,
            selectedRating = preserveSelectedRating
              ?: summary?.viewerRating
              ?: reviews.firstOrNull()?.viewerRating
              ?: communityUi.value.selectedRating,
            filmeraRating = summary?.filmeraRating ?: reviews.firstOrNull()?.filmeraRating,
            ratingCount = summary?.ratingCount ?: reviews.firstOrNull()?.ratingCount ?: 0,
            isRatingSubmitting = false,
            pendingHelpfulReviewIds = emptySet(),
          )
        }
        is DataResult.Error -> communityUi.value = communityUi.value.copy(
          state = LoadState.Error(result.error),
          isRatingSubmitting = false,
          pendingHelpfulReviewIds = emptySet(),
        )
      }
    }
  }

  private fun updateReview(
    reviewId: String,
    transform: (CommunityReview) -> CommunityReview,
  ) {
    val reviews = communityUi.value.reviews.map { review ->
      if (review.id == reviewId) transform(review) else review
    }
    communityUi.value = communityUi.value.copy(
      reviews = reviews,
      state = if (reviews.isEmpty()) LoadState.Empty else LoadState.Success(reviews),
    )
  }

  private fun createMediaKey(savedStateHandle: SavedStateHandle): MediaKey? {
    val mediaId = savedStateHandle.get<Int>(AppDestination.Detail.MEDIA_ID_ARGUMENT) ?: return null
    val mediaType = MediaType.fromRoute(
      savedStateHandle.get(AppDestination.Detail.MEDIA_TYPE_ARGUMENT),
    ) ?: return null
    return mediaId.takeIf { it > 0 }?.let { MediaKey(it, mediaType) }
  }

  private fun createLibrarySelection(items: List<LibraryItem>): LibrarySelection =
    LibrarySelection(
      favoriteKeys = items
        .filter(LibraryItem::isFavorite)
        .map { it.media.key }
        .toSet(),
      watchStatus = items
        .firstOrNull { it.media.key == mediaKey }
        ?.watchStatus
        ?: WatchStatus.NONE,
    )

  private data class LibrarySelection(
    val favoriteKeys: Set<MediaKey> = emptySet(),
    val watchStatus: WatchStatus = WatchStatus.NONE,
  )

  private data class DetailCommunityUi(
    val state: LoadState<List<CommunityReview>> = LoadState.Loading,
    val reviews: List<CommunityReview> = emptyList(),
    val selectedRating: Int? = null,
    val filmeraRating: Double? = null,
    val ratingCount: Long = 0,
    val isRatingSubmitting: Boolean = false,
    val pendingHelpfulReviewIds: Set<String> = emptySet(),
    val hasWriteError: Boolean = false,
    val reviewComposer: CommunityReviewComposerState = CommunityReviewComposerState(),
  )
}
