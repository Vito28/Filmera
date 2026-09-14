package com.example.filmera.feature.community.domain

import com.example.filmera.core.model.MediaKey

data class PublishReviewRequest(
  val mediaKey: MediaKey,
  val rating: Int,
  val headline: String,
  val body: String,
  val containsSpoilers: Boolean,
)

data class CommunityAuthor(
  val id: String,
  val username: String,
  val displayName: String,
  val avatarUrl: String?,
)

enum class ReviewReaction(val wireValue: String, val emoji: String) {
  LOVE("love", "😍"),
  FIRE("fire", "🔥"),
  MIND_BLOWN("mind_blown", "🤯"),
  MOVING("moving", "🥹");

  companion object {
    fun fromWireValue(value: String?): ReviewReaction? =
      entries.firstOrNull { it.wireValue == value }
  }
}

data class ReactionSummary(
  val selectedReaction: ReviewReaction? = null,
  val counts: Map<ReviewReaction, Int> = emptyMap(),
  val isSubmitting: Boolean = false,
) {
  fun count(reaction: ReviewReaction): Int = counts[reaction] ?: 0

  val totalCount: Int
    get() = counts.values.sum()
}

data class HelpfulState(
  val isHelpful: Boolean,
  val count: Int,
  val isSubmitting: Boolean = false,
)

data class CommunityReview(
  val id: String,
  val mediaId: Long,
  val mediaKey: MediaKey,
  val mediaTitle: String,
  val posterPath: String?,
  val releaseYear: String?,
  val author: CommunityAuthor,
  val headline: String?,
  val body: String,
  val containsSpoilers: Boolean,
  val authorRating: Int?,
  val viewerRating: Int?,
  val filmeraRating: Double?,
  val ratingCount: Long,
  val helpful: HelpfulState,
  val commentCount: Int,
  val reactions: ReactionSummary,
  val createdAt: String,
  val updatedAt: String,
)

data class CommunityCursor(
  val createdAt: String,
  val id: String,
)

data class CommunityPage(
  val reviews: List<CommunityReview>,
  val nextCursor: CommunityCursor?,
)

data class MediaRatingSummary(
  val mediaId: Long,
  val viewerRating: Int?,
  val filmeraRating: Double?,
  val ratingCount: Long,
)

data class CommunityComment(
  val id: String,
  val reviewId: String,
  val parentCommentId: String?,
  val author: CommunityAuthor,
  val body: String,
  val replyCount: Int,
  val isOwnedByViewer: Boolean,
  val createdAt: String,
  val updatedAt: String,
)

enum class CommentRealtimeEventType {
  CREATED,
  UPDATED,
  DELETED,
}

data class CommentRealtimeEvent(
  val type: CommentRealtimeEventType,
  val commentId: String,
  val reviewId: String,
  val parentCommentId: String?,
  val updatedAt: String,
)
