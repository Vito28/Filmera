package com.example.filmera.feature.community.data

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.feature.community.domain.CommentRealtimeEvent
import com.example.filmera.feature.community.domain.CommentRealtimeEventType
import com.example.filmera.feature.community.domain.CommentRealtimeSubscription
import com.example.filmera.feature.community.domain.CommunityComment
import com.example.filmera.feature.community.domain.CommunityCursor
import com.example.filmera.feature.community.domain.CommunityPage
import com.example.filmera.feature.community.domain.CommunityRepository
import com.example.filmera.feature.community.domain.CommunityReview
import com.example.filmera.feature.community.domain.ReviewReaction
import com.example.filmera.core.model.MediaKey
import com.example.filmera.feature.community.domain.PublishReviewRequest
import com.example.filmera.feature.community.domain.MediaRatingSummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.SessionRequiredException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.call.body
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Singleton
class SupabaseCommunityRepository @Inject constructor(
  private val supabase: SupabaseClient,
) : CommunityRepository {
  override suspend fun getReviews(
    cursor: CommunityCursor?,
    limit: Int,
  ): DataResult<CommunityPage> = communityCall {
    val reviews = supabase.postgrest.rpc(
      function = "get_community_reviews_v2",
      parameters = buildJsonObject {
        put("p_limit", limit.coerceIn(1, 50))
        cursor?.let {
          put("p_cursor_created_at", it.createdAt)
          put("p_cursor_id", it.id)
        }
      },
    ).decodeList<CommunityReviewDto>()
      .mapNotNull(CommunityReviewDto::toDomain)

    CommunityPage(
      reviews = reviews,
      nextCursor = reviews.lastOrNull()?.let { review ->
        CommunityCursor(createdAt = review.createdAt, id = review.id)
      },
    )
  }

  override suspend fun getReview(reviewId: String): DataResult<CommunityReview> = communityCall {
    supabase.postgrest.rpc(
      function = "get_review_detail_v2",
      parameters = buildJsonObject { put("p_review_id", reviewId) },
    ).decodeList<CommunityReviewDto>()
      .firstOrNull()
      ?.toDomain()
      ?: throw MissingCommunityRecordException()
  }

  override suspend fun getMediaReviews(
    mediaKey: MediaKey,
    limit: Int,
  ): DataResult<List<CommunityReview>> = communityCall {
    require(mediaKey.id > 0)
    supabase.postgrest.rpc(
      function = "get_media_community_reviews",
      parameters = buildJsonObject {
        put("p_media_type", mediaKey.type.routeValue)
        put("p_tmdb_id", mediaKey.id)
        put("p_limit", limit.coerceIn(1, 50))
      },
    ).decodeList<CommunityReviewDto>().mapNotNull(CommunityReviewDto::toDomain)
  }

  override suspend fun getMediaRatingSummary(
    mediaKey: MediaKey,
  ): DataResult<MediaRatingSummary?> = communityCall {
    require(mediaKey.id > 0)
    supabase.postgrest.rpc(
      function = "get_media_rating_summary",
      parameters = buildJsonObject {
        put("p_media_type", mediaKey.type.routeValue)
        put("p_tmdb_id", mediaKey.id)
      },
    ).decodeList<MediaRatingSummaryDto>().firstOrNull()?.let { summary ->
      MediaRatingSummary(
        mediaId = summary.mediaId,
        viewerRating = summary.viewerRating?.coerceIn(1, 10),
        filmeraRating = summary.filmeraRating?.coerceIn(0.0, 10.0),
        ratingCount = summary.ratingCount.coerceAtLeast(0),
      )
    }
  }

  override suspend fun publishReview(
    request: PublishReviewRequest,
  ): DataResult<CommunityReview> = communityCall {
    require(request.mediaKey.id > 0)
    require(request.rating in 1..10)
    require(request.headline.length <= 120)
    require(request.body.trim().length in 20..5_000)

    val ensuredMediaId = ensureMedia(request.mediaKey)

    val reviewId = supabase.postgrest.rpc(
      function = "publish_media_review",
      parameters = buildJsonObject {
        put("p_media_id", ensuredMediaId)
        put("p_rating", request.rating)
        put("p_headline", request.headline.trim())
        put("p_body", request.body.trim())
        put("p_contains_spoilers", request.containsSpoilers)
      },
    ).decodeAs<String>()

    when (val result = getReview(reviewId)) {
      is DataResult.Success -> result.value
      is DataResult.Error -> throw MissingCommunityRecordException()
    }
  }

  override suspend fun setHelpful(
    reviewId: String,
    helpful: Boolean,
  ): DataResult<Unit> = communityCall {
    supabase.postgrest.rpc(
      function = if (helpful) "mark_review_helpful" else "remove_review_helpful",
      parameters = buildJsonObject { put("p_review_id", reviewId) },
    )
    return@communityCall Unit
  }

  override suspend fun setReaction(
    reviewId: String,
    reaction: ReviewReaction?,
  ): DataResult<Unit> = communityCall {
    supabase.postgrest.rpc(
      function = if (reaction == null) "remove_review_reaction" else "set_review_reaction",
      parameters = buildJsonObject {
        put("p_review_id", reviewId)
        reaction?.let { put("p_reaction", it.wireValue) }
      },
    )
    return@communityCall Unit
  }

  override suspend fun setRating(
    mediaId: Long,
    rating: Int?,
  ): DataResult<Unit> = communityCall {
    writeRating(mediaId, rating)
    return@communityCall Unit
  }

  override suspend fun setMediaRating(
    mediaKey: MediaKey,
    rating: Int?,
  ): DataResult<Unit> = communityCall {
    val mediaId = ensureMedia(mediaKey)
    writeRating(mediaId, rating)
    return@communityCall Unit
  }

  override suspend fun getRootComments(
    reviewId: String,
    cursor: CommunityCursor?,
    limit: Int,
  ): DataResult<List<CommunityComment>> = communityCall {
    val viewerId = supabase.auth.currentUserOrNull()?.id
    supabase.postgrest.rpc(
      function = "get_review_comments",
      parameters = buildJsonObject {
        put("p_review_id", reviewId)
        put("p_limit", limit.coerceIn(1, 100))
        cursor?.let {
          put("p_cursor_created_at", it.createdAt)
          put("p_cursor_id", it.id)
        }
      },
    ).decodeList<RootCommentDto>().map { it.toDomain(viewerId) }
  }

  override suspend fun getReplies(
    rootCommentId: String,
    cursor: CommunityCursor?,
    limit: Int,
  ): DataResult<List<CommunityComment>> = communityCall {
    val viewerId = supabase.auth.currentUserOrNull()?.id
    supabase.postgrest.rpc(
      function = "get_comment_replies",
      parameters = buildJsonObject {
        put("p_parent_comment_id", rootCommentId)
        put("p_limit", limit.coerceIn(1, 100))
        cursor?.let {
          put("p_cursor_created_at", it.createdAt)
          put("p_cursor_id", it.id)
        }
      },
    ).decodeList<ReplyCommentDto>().map { it.toDomain(viewerId) }
  }

  override suspend fun getComment(commentId: String): DataResult<CommunityComment> = communityCall {
    supabase.postgrest.rpc(
      function = "get_review_comment",
      parameters = buildJsonObject { put("p_comment_id", commentId) },
    ).decodeList<CommunityCommentDto>()
      .firstOrNull()
      ?.toDomain()
      ?: throw MissingCommunityRecordException()
  }

  override suspend fun createComment(
    reviewId: String,
    body: String,
    parentCommentId: String?,
  ): DataResult<CommunityComment> = communityCall {
    val commentId = supabase.postgrest.rpc(
      function = "create_review_comment",
      parameters = buildJsonObject {
        put("p_review_id", reviewId)
        put("p_body", body.trim())
        parentCommentId?.let { put("p_parent_comment_id", it) }
      },
    ).decodeAs<String>()

    when (val result = getComment(commentId)) {
      is DataResult.Success -> result.value
      is DataResult.Error -> throw MissingCommunityRecordException()
    }
  }

  override suspend fun subscribeToComments(
    reviewId: String,
  ): DataResult<CommentRealtimeSubscription> = communityCall {
    require(REVIEW_ID_PATTERN.matches(reviewId))
    supabase.realtime.setAuth()
    val channel = supabase.realtime.channel("review:$reviewId:comments") {
      isPrivate = true
    }
    val events = merge(
      channel.eventFlow("comment_created", CommentRealtimeEventType.CREATED),
      channel.eventFlow("comment_updated", CommentRealtimeEventType.UPDATED),
      channel.eventFlow("comment_deleted", CommentRealtimeEventType.DELETED),
    )
    channel.subscribe(blockUntilSubscribed = true)
    SupabaseCommentSubscription(supabase, channel, events)
  }

  private suspend inline fun <T> communityCall(
    crossinline block: suspend () -> T,
  ): DataResult<T> = try {
    DataResult.Success(block())
  } catch (error: CancellationException) {
    throw error
  } catch (_: MissingCommunityRecordException) {
    DataResult.Error(AppError.NotFound)
  } catch (_: HttpRequestTimeoutException) {
    DataResult.Error(AppError.NetworkUnavailable)
  } catch (_: HttpRequestException) {
    DataResult.Error(AppError.NetworkUnavailable)
  } catch (_: SessionRequiredException) {
    DataResult.Error(AppError.Unauthorized)
  } catch (error: RestException) {
    DataResult.Error(error.toAppError())
  } catch (_: InvalidRatingWriteResponseException) {
    DataResult.Error(AppError.InvalidResponse)
  } catch (_: SerializationException) {
    DataResult.Error(AppError.InvalidResponse)
  } catch (_: IllegalArgumentException) {
    DataResult.Error(AppError.InvalidResponse)
  } catch (_: Exception) {
    DataResult.Error(AppError.Unknown)
  }

  private suspend fun ensureMedia(mediaKey: MediaKey): Long =
    supabase.functions.invoke(
      function = "ensure-media",
      body = buildJsonObject {
        put("media_type", mediaKey.type.routeValue)
        put("tmdb_id", mediaKey.id)
      },
      headers = Headers.build {
        append(HttpHeaders.ContentType, "application/json")
      },
    ).body<EnsureMediaResponse>().mediaId

  private suspend fun writeRating(
    mediaId: Long,
    rating: Int?,
  ) {
    require(mediaId > 0)
    require(rating == null || rating in 1..10)

    val response = if (rating == null) {
      supabase.postgrest.rpc(
        function = "remove_user_rating_with_summary",
        parameters = buildJsonObject { put("p_media_id", mediaId) },
      )
    } else {
      supabase.postgrest.rpc(
        function = "set_user_rating_with_summary",
        parameters = buildJsonObject {
          put("p_media_id", mediaId)
          put("p_rating", rating)
        },
      )
    }

    val summary = response.decodeList<MediaRatingSummaryDto>().singleOrNull()
      ?: throw InvalidRatingWriteResponseException()
    if (!summary.matchesRatingWrite(mediaId, rating)) {
      throw InvalidRatingWriteResponseException()
    }
  }

  private companion object {
    val REVIEW_ID_PATTERN = Regex(
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
    )
  }
}

private class SupabaseCommentSubscription(
  private val supabase: SupabaseClient,
  private val channel: RealtimeChannel,
  override val events: Flow<CommentRealtimeEvent>,
) : CommentRealtimeSubscription {
  override suspend fun close() {
    supabase.realtime.removeChannel(channel)
  }
}

private fun RealtimeChannel.eventFlow(
  event: String,
  type: CommentRealtimeEventType,
): Flow<CommentRealtimeEvent> = broadcastFlow<CommentBroadcastPayload>(event).map { payload ->
  CommentRealtimeEvent(
    type = type,
    commentId = payload.commentId,
    reviewId = payload.reviewId,
    parentCommentId = payload.parentCommentId,
    updatedAt = payload.updatedAt,
  )
}

private fun RestException.toAppError(): AppError = when (statusCode) {
  401, 403 -> AppError.Unauthorized
  404 -> AppError.NotFound
  429 -> AppError.RateLimited
  in 500..599 -> AppError.ServerUnavailable
  else -> AppError.Unknown
}

private class MissingCommunityRecordException : RuntimeException()

private class InvalidRatingWriteResponseException : RuntimeException()
