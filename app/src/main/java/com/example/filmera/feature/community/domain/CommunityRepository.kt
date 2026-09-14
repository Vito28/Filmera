package com.example.filmera.feature.community.domain

import com.example.filmera.core.common.DataResult
import com.example.filmera.core.common.AppError
import com.example.filmera.core.model.MediaKey
import kotlinx.coroutines.flow.Flow

interface CommentRealtimeSubscription {
  val events: Flow<CommentRealtimeEvent>
  suspend fun close()
}

interface CommunityRepository {
  suspend fun getReviews(
    cursor: CommunityCursor? = null,
    limit: Int = 20,
  ): DataResult<CommunityPage>

  suspend fun getReview(reviewId: String): DataResult<CommunityReview>

  suspend fun getMediaReviews(
    mediaKey: MediaKey,
    limit: Int = 20,
  ): DataResult<List<CommunityReview>> = DataResult.Error(AppError.Unknown)

  suspend fun getMediaRatingSummary(
    mediaKey: MediaKey,
  ): DataResult<MediaRatingSummary?> = DataResult.Error(AppError.Unknown)

  suspend fun publishReview(request: PublishReviewRequest): DataResult<CommunityReview>

  suspend fun setHelpful(reviewId: String, helpful: Boolean): DataResult<Unit>

  suspend fun setReaction(
    reviewId: String,
    reaction: ReviewReaction?,
  ): DataResult<Unit>

  suspend fun setRating(mediaId: Long, rating: Int?): DataResult<Unit>

  suspend fun setMediaRating(
    mediaKey: MediaKey,
    rating: Int?,
  ): DataResult<Unit> = DataResult.Error(AppError.Unknown)

  suspend fun getRootComments(
    reviewId: String,
    cursor: CommunityCursor? = null,
    limit: Int = 30,
  ): DataResult<List<CommunityComment>>

  suspend fun getReplies(
    rootCommentId: String,
    cursor: CommunityCursor? = null,
    limit: Int = 20,
  ): DataResult<List<CommunityComment>>

  suspend fun getComment(commentId: String): DataResult<CommunityComment>

  suspend fun createComment(
    reviewId: String,
    body: String,
    parentCommentId: String? = null,
  ): DataResult<CommunityComment>

  suspend fun subscribeToComments(
    reviewId: String,
  ): DataResult<CommentRealtimeSubscription>
}
