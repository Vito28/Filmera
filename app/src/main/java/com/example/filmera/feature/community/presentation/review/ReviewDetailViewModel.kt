package com.example.filmera.feature.community.presentation.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.navigation.AppDestination
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.community.domain.CommentRealtimeEvent
import com.example.filmera.feature.community.domain.CommentRealtimeEventType
import com.example.filmera.feature.community.domain.CommentRealtimeSubscription
import com.example.filmera.feature.community.domain.CommunityAuthor
import com.example.filmera.feature.community.domain.CommunityComment
import com.example.filmera.feature.community.domain.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val repository: CommunityRepository,
) : ViewModel() {
  private val reviewId = savedStateHandle
    .get<String>(AppDestination.ReviewDetail.REVIEW_ID_ARGUMENT)
    .orEmpty()
  private val mutableState = MutableStateFlow(ReviewDetailUiState())
  val uiState: StateFlow<ReviewDetailUiState> = mutableState.asStateFlow()

  private var realtimeJob: Job? = null

  init {
    load()
  }

  fun onAction(action: ReviewDetailAction) {
    when (action) {
      ReviewDetailAction.Retried -> load()
      ReviewDetailAction.ReconnectRealtime -> connectRealtime()
      is ReviewDetailAction.ComposerTextChanged -> mutableState.update {
        it.copy(composerText = action.text.take(COMMENT_MAX_LENGTH), composerError = null)
      }
      is ReviewDetailAction.ReplySelected -> mutableState.update {
        it.copy(
          replyTarget = ReplyTarget(action.rootCommentId, action.targetUsername),
          focusRequestNonce = it.focusRequestNonce + 1,
        )
      }
      ReviewDetailAction.ReplyCancelled -> mutableState.update { it.copy(replyTarget = null) }
      ReviewDetailAction.SendComment -> sendComment()
      is ReviewDetailAction.RepliesToggled -> toggleReplies(action.rootCommentId)
      is ReviewDetailAction.RetryComment -> retryComment(action.temporaryId)
      is ReviewDetailAction.DeleteFailedComment -> removeComment(action.temporaryId)
    }
  }

  private fun load() {
    if (!AppDestination.ReviewDetail.isValidReviewId(reviewId)) {
      mutableState.value = ReviewDetailUiState(
        reviewState = LoadState.Error(AppError.NotFound),
        commentsState = LoadState.Error(AppError.NotFound),
      )
      return
    }
    loadReview()
    connectRealtime()
  }

  private fun loadReview() {
    viewModelScope.launch {
      mutableState.update { it.copy(reviewState = LoadState.Loading) }
      when (val result = repository.getReview(reviewId)) {
        is DataResult.Success -> mutableState.update {
          it.copy(reviewState = LoadState.Success(result.value))
        }
        is DataResult.Error -> mutableState.update {
          it.copy(reviewState = LoadState.Error(result.error))
        }
      }
    }
  }

  private fun connectRealtime() {
    realtimeJob?.cancel()
    realtimeJob = viewModelScope.launch {
      var subscription: CommentRealtimeSubscription? = null
      try {
        subscription = when (val result = repository.subscribeToComments(reviewId)) {
          is DataResult.Success -> result.value
          is DataResult.Error -> {
            mutableState.update { it.copy(liveUpdatesPaused = true) }
            null
          }
        }
        loadRootComments()
        mutableState.update { it.copy(liveUpdatesPaused = subscription == null) }
        subscription?.events?.collect(::handleRealtimeEvent)
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        mutableState.update { it.copy(liveUpdatesPaused = true) }
      } finally {
        runCatching { subscription?.close() }
      }
    }
  }

  private suspend fun loadRootComments() {
    mutableState.update { state ->
      if (state.commentsState is LoadState.Success) state
      else state.copy(commentsState = LoadState.Loading)
    }
    when (val result = repository.getRootComments(reviewId = reviewId)) {
      is DataResult.Success -> {
        val current = (mutableState.value.commentsState as? LoadState.Success)?.value
        val roots = result.value
        val incoming = roots.associate { it.id to CommentItemUi(it) }
        val thread = (current ?: CommentThreadState()).copy(
          rootCommentIds = roots.map(CommunityComment::id),
          commentsById = current?.commentsById.orEmpty() + incoming,
        )
        mutableState.update {
          it.copy(
            commentsState = if (roots.isEmpty()) LoadState.Empty else LoadState.Success(thread),
          )
        }
      }
      is DataResult.Error -> mutableState.update { state ->
        if (state.commentsState is LoadState.Success) state
        else state.copy(commentsState = LoadState.Error(result.error))
      }
    }
  }

  private fun toggleReplies(rootId: String) {
    val thread = (mutableState.value.commentsState as? LoadState.Success)?.value ?: return
    if (rootId in thread.expandedRootIds) {
      updateThread { it.copy(expandedRootIds = it.expandedRootIds - rootId) }
      return
    }
    if (thread.replyIdsByRootId.containsKey(rootId)) {
      updateThread { it.copy(expandedRootIds = it.expandedRootIds + rootId) }
      return
    }
    if (rootId in thread.loadingReplyRootIds) return

    updateThread {
      it.copy(
        expandedRootIds = it.expandedRootIds + rootId,
        loadingReplyRootIds = it.loadingReplyRootIds + rootId,
        replyErrors = it.replyErrors - rootId,
      )
    }
    viewModelScope.launch {
      when (val result = repository.getReplies(rootId)) {
        is DataResult.Success -> updateThread { current ->
          current.copy(
            commentsById = current.commentsById + result.value.associate {
              it.id to CommentItemUi(it)
            },
            replyIdsByRootId = current.replyIdsByRootId +
              (rootId to result.value.map(CommunityComment::id)),
            loadingReplyRootIds = current.loadingReplyRootIds - rootId,
          )
        }
        is DataResult.Error -> updateThread {
          it.copy(
            loadingReplyRootIds = it.loadingReplyRootIds - rootId,
            replyErrors = it.replyErrors + (rootId to result.error),
          )
        }
      }
    }
  }

  private fun sendComment() {
    val state = mutableState.value
    val body = state.composerText.trim()
    if (body.isBlank() || state.isSending) return
    val temporaryId = "local-${UUID.randomUUID()}"
    val parentId = state.replyTarget?.rootCommentId
    val pending = CommunityComment(
      id = temporaryId,
      reviewId = reviewId,
      parentCommentId = parentId,
      author = CommunityAuthor("current-user", "you", "You", null),
      body = body,
      replyCount = 0,
      isOwnedByViewer = true,
      createdAt = Instant.now().toString(),
      updatedAt = Instant.now().toString(),
    )
    insertPendingComment(CommentItemUi(pending, CommentDeliveryState.SENDING))
    mutableState.update {
      it.copy(
        composerText = "",
        replyTarget = null,
        isSending = true,
        composerError = null,
      )
    }
    submitPendingComment(temporaryId, body, parentId)
  }

  private fun retryComment(temporaryId: String) {
    val item = currentThread()?.commentsById?.get(temporaryId) ?: return
    if (item.deliveryState != CommentDeliveryState.FAILED) return
    updateThread { thread ->
      thread.copy(commentsById = thread.commentsById + (
        temporaryId to item.copy(deliveryState = CommentDeliveryState.SENDING)
      ))
    }
    submitPendingComment(temporaryId, item.comment.body, item.comment.parentCommentId)
  }

  private fun submitPendingComment(
    temporaryId: String,
    body: String,
    parentId: String?,
  ) {
    viewModelScope.launch {
      when (val result = repository.createComment(reviewId, body, parentId)) {
        is DataResult.Success -> replaceTemporaryComment(temporaryId, result.value)
        is DataResult.Error -> {
          updateThread { thread ->
            val item = thread.commentsById[temporaryId] ?: return@updateThread thread
            thread.copy(commentsById = thread.commentsById + (
              temporaryId to item.copy(deliveryState = CommentDeliveryState.FAILED)
            ))
          }
          mutableState.update { it.copy(composerError = result.error) }
        }
      }
      mutableState.update { it.copy(isSending = false) }
    }
  }

  private fun insertPendingComment(item: CommentItemUi) {
    val thread = currentThread() ?: CommentThreadState()
    val comment = item.comment
    val updated = if (comment.parentCommentId == null) {
      thread.copy(
        rootCommentIds = thread.rootCommentIds + comment.id,
        commentsById = thread.commentsById + (comment.id to item),
      )
    } else {
      thread.copy(
        commentsById = thread.commentsById + (comment.id to item),
        replyIdsByRootId = thread.replyIdsByRootId + (
          comment.parentCommentId to (
            thread.replyIdsByRootId[comment.parentCommentId].orEmpty() + comment.id
          )
        ),
        expandedRootIds = thread.expandedRootIds + comment.parentCommentId,
      )
    }
    mutableState.update { it.copy(commentsState = LoadState.Success(updated)) }
  }

  private fun replaceTemporaryComment(temporaryId: String, comment: CommunityComment) {
    updateThread { thread ->
      val wasRoot = temporaryId in thread.rootCommentIds
      val rootIds = if (wasRoot) {
        thread.rootCommentIds.map { if (it == temporaryId) comment.id else it }
      } else {
        thread.rootCommentIds
      }
      val replies = thread.replyIdsByRootId.mapValues { (_, ids) ->
        ids.map { if (it == temporaryId) comment.id else it }
      }
      thread.copy(
        rootCommentIds = rootIds.distinct(),
        replyIdsByRootId = replies,
        commentsById = (thread.commentsById - temporaryId) +
          (comment.id to CommentItemUi(comment)),
      )
    }
  }

  private suspend fun handleRealtimeEvent(event: CommentRealtimeEvent) {
    if (event.reviewId != reviewId) return
    if (event.type == CommentRealtimeEventType.DELETED) {
      removeComment(event.commentId)
      return
    }
    when (val result = repository.getComment(event.commentId)) {
      is DataResult.Success -> mergeServerComment(result.value)
      is DataResult.Error -> Unit
    }
  }

  private fun mergeServerComment(comment: CommunityComment) {
    val thread = currentThread() ?: CommentThreadState()
    val current = thread.commentsById[comment.id]?.comment
    if (current != null && current.updatedAt >= comment.updatedAt) return

    val next = if (comment.parentCommentId == null) {
      thread.copy(
        rootCommentIds = (thread.rootCommentIds + comment.id).distinct(),
        commentsById = thread.commentsById + (comment.id to CommentItemUi(comment)),
      )
    } else {
      val rootId = comment.parentCommentId
      thread.copy(
        commentsById = thread.commentsById + (comment.id to CommentItemUi(comment)),
        replyIdsByRootId = thread.replyIdsByRootId + (
          rootId to (thread.replyIdsByRootId[rootId].orEmpty() + comment.id).distinct()
        ),
      )
    }
    mutableState.update { it.copy(commentsState = LoadState.Success(next)) }
  }

  private fun removeComment(commentId: String) {
    val thread = currentThread() ?: return
    val next = thread.copy(
      rootCommentIds = thread.rootCommentIds - commentId,
      commentsById = thread.commentsById - commentId,
      replyIdsByRootId = (thread.replyIdsByRootId - commentId).mapValues { (_, ids) ->
        ids - commentId
      },
      expandedRootIds = thread.expandedRootIds - commentId,
    )
    mutableState.update {
      it.copy(commentsState = if (next.rootCommentIds.isEmpty()) LoadState.Empty else LoadState.Success(next))
    }
  }

  private fun currentThread(): CommentThreadState? =
    (mutableState.value.commentsState as? LoadState.Success)?.value

  private fun updateThread(transform: (CommentThreadState) -> CommentThreadState) {
    val current = currentThread() ?: return
    mutableState.update { it.copy(commentsState = LoadState.Success(transform(current))) }
  }

  private companion object {
    const val COMMENT_MAX_LENGTH = 2_000
  }
}
