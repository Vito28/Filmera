package com.example.filmera.feature.community.presentation.review

import com.example.filmera.core.common.AppError
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.community.domain.CommunityComment
import com.example.filmera.feature.community.domain.CommunityReview

enum class CommentDeliveryState {
  SENDING,
  SENT,
  FAILED,
}

data class CommentItemUi(
  val comment: CommunityComment,
  val deliveryState: CommentDeliveryState = CommentDeliveryState.SENT,
)

data class CommentThreadState(
  val rootCommentIds: List<String> = emptyList(),
  val commentsById: Map<String, CommentItemUi> = emptyMap(),
  val replyIdsByRootId: Map<String, List<String>> = emptyMap(),
  val expandedRootIds: Set<String> = emptySet(),
  val loadingReplyRootIds: Set<String> = emptySet(),
  val replyErrors: Map<String, AppError> = emptyMap(),
) {
  fun rootComments(): List<CommentItemUi> = rootCommentIds.mapNotNull(commentsById::get)
  fun replies(rootId: String): List<CommentItemUi> =
    replyIdsByRootId[rootId].orEmpty().mapNotNull(commentsById::get)
}

data class ReplyTarget(
  val rootCommentId: String,
  val targetUsername: String,
)

data class ReviewDetailUiState(
  val reviewState: LoadState<CommunityReview> = LoadState.Loading,
  val commentsState: LoadState<CommentThreadState> = LoadState.Loading,
  val composerText: String = "",
  val replyTarget: ReplyTarget? = null,
  val isSending: Boolean = false,
  val composerError: AppError? = null,
  val liveUpdatesPaused: Boolean = false,
  val focusRequestNonce: Int = 0,
)

sealed interface ReviewDetailAction {
  data object Retried : ReviewDetailAction
  data object ReconnectRealtime : ReviewDetailAction
  data class ComposerTextChanged(val text: String) : ReviewDetailAction
  data class ReplySelected(
    val rootCommentId: String,
    val targetUsername: String,
  ) : ReviewDetailAction
  data object ReplyCancelled : ReviewDetailAction
  data object SendComment : ReviewDetailAction
  data class RepliesToggled(val rootCommentId: String) : ReviewDetailAction
  data class RetryComment(val temporaryId: String) : ReviewDetailAction
  data class DeleteFailedComment(val temporaryId: String) : ReviewDetailAction
}
