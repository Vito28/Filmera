package com.example.filmera.feature.community.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.community.domain.CommunityCursor
import com.example.filmera.feature.community.domain.CommunityRepository
import com.example.filmera.feature.community.domain.CommunityReview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CommunityViewModel @Inject constructor(
  private val repository: CommunityRepository,
) : ViewModel() {
  private val mutableState = MutableStateFlow(CommunityUiState())
  val uiState: StateFlow<CommunityUiState> = mutableState.asStateFlow()

  private var cursor: CommunityCursor? = null

  init {
    loadInitial()
  }

  fun onAction(action: CommunityAction) {
    when (action) {
      is CommunityAction.TabSelected -> mutableState.update { it.copy(selectedTab = action.tab) }
      is CommunityAction.HelpfulToggled -> toggleHelpful(action.reviewId)
      CommunityAction.LoadMore -> loadMore()
      CommunityAction.Retry -> loadInitial()
      CommunityAction.ErrorDismissed -> mutableState.update { it.copy(writeError = null) }
    }
  }

  private fun loadInitial() {
    cursor = null
    mutableState.update { it.copy(reviewsState = LoadState.Loading, hasMore = true) }
    viewModelScope.launch {
      when (val result = repository.getReviews(limit = PAGE_SIZE)) {
        is DataResult.Success -> {
          cursor = result.value.nextCursor
          mutableState.update {
            it.copy(
              reviewsState = if (result.value.reviews.isEmpty()) LoadState.Empty
              else LoadState.Success(result.value.reviews),
              hasMore = result.value.reviews.size == PAGE_SIZE,
            )
          }
        }
        is DataResult.Error -> mutableState.update {
          it.copy(reviewsState = LoadState.Error(result.error))
        }
      }
    }
  }

  private fun loadMore() {
    val current = (mutableState.value.reviewsState as? LoadState.Success)?.value ?: return
    if (mutableState.value.isLoadingMore || !mutableState.value.hasMore) return
    mutableState.update { it.copy(isLoadingMore = true) }
    viewModelScope.launch {
      when (val result = repository.getReviews(cursor = cursor, limit = PAGE_SIZE)) {
        is DataResult.Success -> {
          cursor = result.value.nextCursor
          mutableState.update {
            it.copy(
              reviewsState = LoadState.Success(
                (current + result.value.reviews).distinctBy(CommunityReview::id),
              ),
              isLoadingMore = false,
              hasMore = result.value.reviews.size == PAGE_SIZE,
            )
          }
        }
        is DataResult.Error -> mutableState.update {
          it.copy(isLoadingMore = false, writeError = result.error)
        }
      }
    }
  }

  private fun toggleHelpful(reviewId: String) {
    val reviews = (mutableState.value.reviewsState as? LoadState.Success)?.value ?: return
    val review = reviews.firstOrNull { it.id == reviewId } ?: return
    if (review.helpful.isSubmitting) return
    val previous = review.helpful
    val optimistic = previous.copy(
      isHelpful = !previous.isHelpful,
      count = (previous.count + if (previous.isHelpful) -1 else 1).coerceAtLeast(0),
      isSubmitting = true,
    )
    updateReview(reviewId) { it.copy(helpful = optimistic) }
    viewModelScope.launch {
      when (val result = repository.setHelpful(reviewId, optimistic.isHelpful)) {
        is DataResult.Success -> updateReview(reviewId) {
          it.copy(helpful = optimistic.copy(isSubmitting = false))
        }
        is DataResult.Error -> {
          updateReview(reviewId) { it.copy(helpful = previous) }
          mutableState.update { it.copy(writeError = result.error) }
        }
      }
    }
  }

  private fun updateReview(
    reviewId: String,
    transform: (CommunityReview) -> CommunityReview,
  ) {
    val current = (mutableState.value.reviewsState as? LoadState.Success)?.value ?: return
    mutableState.update {
      it.copy(
        reviewsState = LoadState.Success(
          current.map { review -> if (review.id == reviewId) transform(review) else review },
        ),
      )
    }
  }

  private companion object {
    const val PAGE_SIZE = 20
  }
}
