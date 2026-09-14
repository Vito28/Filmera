package com.example.filmera.feature.community.data

import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.feature.community.domain.CommunityAuthor
import com.example.filmera.feature.community.domain.CommunityComment
import com.example.filmera.feature.community.domain.CommunityReview
import com.example.filmera.feature.community.domain.HelpfulState
import com.example.filmera.feature.community.domain.ReactionSummary
import com.example.filmera.feature.community.domain.ReviewReaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EnsureMediaResponse(
  @SerialName("media_id") val mediaId: Long,
)

@Serializable
internal data class MediaRatingSummaryDto(
  @SerialName("media_id") val mediaId: Long,
  @SerialName("viewer_rating") val viewerRating: Int? = null,
  @SerialName("filmera_rating") val filmeraRating: Double? = null,
  @SerialName("rating_count") val ratingCount: Long = 0,
)

internal fun MediaRatingSummaryDto.matchesRatingWrite(
  expectedMediaId: Long,
  expectedRating: Int?,
): Boolean =
  mediaId == expectedMediaId && viewerRating == expectedRating

@Serializable
internal data class CommunityReviewDto(
  @SerialName("review_id") val reviewId: String,
  @SerialName("media_id") val mediaId: Long,
  @SerialName("media_type") val mediaType: String,
  @SerialName("tmdb_id") val tmdbId: Int,
  @SerialName("media_title") val mediaTitle: String,
  @SerialName("poster_path") val posterPath: String? = null,
  @SerialName("release_date") val releaseDate: String? = null,
  @SerialName("author_id") val authorId: String,
  @SerialName("author_username") val authorUsername: String,
  @SerialName("author_display_name") val authorDisplayName: String,
  @SerialName("author_avatar_url") val authorAvatarUrl: String? = null,
  val headline: String? = null,
  @SerialName("review_body") val reviewBody: String,
  @SerialName("contains_spoilers") val containsSpoilers: Boolean,
  @SerialName("user_rating") val userRating: Int? = null,
  @SerialName("viewer_rating") val viewerRating: Int? = null,
  @SerialName("filmera_rating") val filmeraRating: Double? = null,
  @SerialName("rating_count") val ratingCount: Long = 0,
  @SerialName("helpful_count") val helpfulCount: Int = 0,
  @SerialName("comment_count") val commentCount: Int = 0,
  @SerialName("viewer_has_marked_helpful") val viewerHasMarkedHelpful: Boolean = false,
  @SerialName("viewer_reaction") val viewerReaction: String? = null,
  @SerialName("love_count") val loveCount: Int = 0,
  @SerialName("fire_count") val fireCount: Int = 0,
  @SerialName("mind_blown_count") val mindBlownCount: Int = 0,
  @SerialName("moving_count") val movingCount: Int = 0,
  @SerialName("created_at") val createdAt: String,
  @SerialName("updated_at") val updatedAt: String,
)

@Serializable
internal data class RootCommentDto(
  @SerialName("comment_id") val commentId: String,
  @SerialName("review_id") val reviewId: String,
  @SerialName("author_id") val authorId: String,
  @SerialName("author_username") val authorUsername: String,
  @SerialName("author_display_name") val authorDisplayName: String,
  @SerialName("author_avatar_url") val authorAvatarUrl: String? = null,
  val body: String,
  @SerialName("reply_count") val replyCount: Int = 0,
  @SerialName("created_at") val createdAt: String,
  @SerialName("updated_at") val updatedAt: String,
)

@Serializable
internal data class ReplyCommentDto(
  @SerialName("comment_id") val commentId: String,
  @SerialName("review_id") val reviewId: String,
  @SerialName("parent_comment_id") val parentCommentId: String,
  @SerialName("author_id") val authorId: String,
  @SerialName("author_username") val authorUsername: String,
  @SerialName("author_display_name") val authorDisplayName: String,
  @SerialName("author_avatar_url") val authorAvatarUrl: String? = null,
  val body: String,
  @SerialName("created_at") val createdAt: String,
  @SerialName("updated_at") val updatedAt: String,
)

@Serializable
internal data class CommunityCommentDto(
  @SerialName("comment_id") val commentId: String,
  @SerialName("review_id") val reviewId: String,
  @SerialName("parent_comment_id") val parentCommentId: String? = null,
  @SerialName("author_id") val authorId: String,
  @SerialName("author_username") val authorUsername: String,
  @SerialName("author_display_name") val authorDisplayName: String,
  @SerialName("author_avatar_url") val authorAvatarUrl: String? = null,
  val body: String,
  @SerialName("reply_count") val replyCount: Int = 0,
  @SerialName("is_owned_by_viewer") val isOwnedByViewer: Boolean = false,
  @SerialName("created_at") val createdAt: String,
  @SerialName("updated_at") val updatedAt: String,
)

@Serializable
internal data class CommentBroadcastPayload(
  @SerialName("comment_id") val commentId: String,
  @SerialName("review_id") val reviewId: String,
  @SerialName("parent_comment_id") val parentCommentId: String? = null,
  @SerialName("updated_at") val updatedAt: String,
)

internal fun CommunityReviewDto.toDomain(): CommunityReview? {
  val type = MediaType.fromRoute(mediaType) ?: return null
  if (tmdbId <= 0 || mediaId <= 0L || reviewId.isBlank()) return null
  return CommunityReview(
    id = reviewId,
    mediaId = mediaId,
    mediaKey = MediaKey(id = tmdbId, type = type),
    mediaTitle = mediaTitle,
    posterPath = posterPath,
    releaseYear = releaseDate?.take(4)?.takeIf { it.all(Char::isDigit) },
    author = CommunityAuthor(
      id = authorId,
      username = authorUsername,
      displayName = authorDisplayName,
      avatarUrl = authorAvatarUrl,
    ),
    headline = headline?.trim()?.takeIf(String::isNotEmpty),
    body = reviewBody,
    containsSpoilers = containsSpoilers,
    authorRating = userRating?.coerceIn(1, 10),
    viewerRating = viewerRating?.coerceIn(1, 10),
    filmeraRating = filmeraRating?.coerceIn(0.0, 10.0),
    ratingCount = ratingCount.coerceAtLeast(0),
    helpful = HelpfulState(
      isHelpful = viewerHasMarkedHelpful,
      count = helpfulCount.coerceAtLeast(0),
    ),
    commentCount = commentCount.coerceAtLeast(0),
    reactions = ReactionSummary(
      selectedReaction = ReviewReaction.fromWireValue(viewerReaction),
      counts = mapOf(
        ReviewReaction.LOVE to loveCount.coerceAtLeast(0),
        ReviewReaction.FIRE to fireCount.coerceAtLeast(0),
        ReviewReaction.MIND_BLOWN to mindBlownCount.coerceAtLeast(0),
        ReviewReaction.MOVING to movingCount.coerceAtLeast(0),
      ),
    ),
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
}

internal fun RootCommentDto.toDomain(viewerId: String?): CommunityComment = CommunityComment(
  id = commentId,
  reviewId = reviewId,
  parentCommentId = null,
  author = CommunityAuthor(authorId, authorUsername, authorDisplayName, authorAvatarUrl),
  body = body,
  replyCount = replyCount.coerceAtLeast(0),
  isOwnedByViewer = authorId == viewerId,
  createdAt = createdAt,
  updatedAt = updatedAt,
)

internal fun ReplyCommentDto.toDomain(viewerId: String?): CommunityComment = CommunityComment(
  id = commentId,
  reviewId = reviewId,
  parentCommentId = parentCommentId,
  author = CommunityAuthor(authorId, authorUsername, authorDisplayName, authorAvatarUrl),
  body = body,
  replyCount = 0,
  isOwnedByViewer = authorId == viewerId,
  createdAt = createdAt,
  updatedAt = updatedAt,
)

internal fun CommunityCommentDto.toDomain(): CommunityComment = CommunityComment(
  id = commentId,
  reviewId = reviewId,
  parentCommentId = parentCommentId,
  author = CommunityAuthor(authorId, authorUsername, authorDisplayName, authorAvatarUrl),
  body = body,
  replyCount = replyCount.coerceAtLeast(0),
  isOwnedByViewer = isOwnedByViewer,
  createdAt = createdAt,
  updatedAt = updatedAt,
)
