package com.example.filmera.feature.community.presentation.review

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filmera.R
import com.example.filmera.core.common.AppError
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.core.ui.messageResource
import com.example.filmera.feature.community.domain.CommunityComment
import com.example.filmera.feature.community.domain.CommunityReview
import com.example.filmera.feature.community.domain.ReviewReaction
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.launch

@Composable
fun ReviewDetailRoute(
  onBack: () -> Unit,
  onMediaClick: (MediaKey) -> Unit,
  viewModel: ReviewDetailViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  ReviewDetailScreen(
    state = state,
    onAction = viewModel::onAction,
    onBack = onBack,
    onMediaClick = onMediaClick,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDetailScreen(
  state: ReviewDetailUiState,
  onAction: (ReviewDetailAction) -> Unit,
  onBack: () -> Unit,
  onMediaClick: (MediaKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val density = LocalDensity.current
  val imeVisible = WindowInsets.ime.getBottom(density) > 0

  BackHandler(enabled = imeVisible || state.replyTarget != null) {
    if (imeVisible) {
      keyboardController?.hide()
      focusManager.clearFocus()
    } else {
      onAction(ReviewDetailAction.ReplyCancelled)
    }
  }

  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = stringResource(R.string.community_review_detail_title),
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = stringResource(R.string.community_review_detail_subtitle),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelSmall,
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = stringResource(R.string.navigate_back),
            )
          }
        },
        actions = {
          IconButton(onClick = {}) {
            Icon(Icons.Outlined.MoreHoriz, contentDescription = stringResource(R.string.more_actions))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
        ),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = innerPadding.calculateTopPadding())
        .navigationBarsPadding(),
    ) {
      when (val reviewState = state.reviewState) {
        LoadState.Loading -> ReviewDetailSkeleton()
        LoadState.Empty -> ReviewDetailMessage(
          message = stringResource(R.string.community_review_missing),
          onRetry = { onAction(ReviewDetailAction.Retried) },
        )
        is LoadState.Error -> ReviewDetailMessage(
          message = stringResource(reviewState.error.messageResource()),
          onRetry = { onAction(ReviewDetailAction.Retried) },
        )
        is LoadState.Success -> ReviewDetailContent(
          review = reviewState.value,
          commentsState = state.commentsState,
          liveUpdatesPaused = state.liveUpdatesPaused,
          onAction = onAction,
          onMediaClick = onMediaClick,
        )
      }

      if (state.reviewState is LoadState.Success) {
        CommentComposer(
          state = state,
          onAction = onAction,
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .imePadding(),
        )
      }
    }
  }
}

@Composable
private fun ReviewDetailContent(
  review: CommunityReview,
  commentsState: LoadState<CommentThreadState>,
  liveUpdatesPaused: Boolean,
  onAction: (ReviewDetailAction) -> Unit,
  onMediaClick: (MediaKey) -> Unit,
) {
  val listState = rememberLazyListState()
  LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 126.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(key = "review-card") {
      FullReviewCard(review = review, onMediaClick = { onMediaClick(review.mediaKey) })
    }
    item(key = "comments-header") {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Outlined.ChatBubbleOutline,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(9.dp))
        Text(
          text = stringResource(R.string.community_comments_count, review.commentCount),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
      }
    }
    if (liveUpdatesPaused) {
      item(key = "realtime-paused") {
        RealtimePausedBanner(onReconnect = { onAction(ReviewDetailAction.ReconnectRealtime) })
      }
    }
    when (commentsState) {
      LoadState.Loading -> items(3, key = { "comment-skeleton-$it" }) { CommentSkeleton() }
      LoadState.Empty -> item(key = "comments-empty") { CommentsEmptyState() }
      is LoadState.Error -> item(key = "comments-error") {
        ReviewDetailMessage(
          message = stringResource(commentsState.error.messageResource()),
          onRetry = { onAction(ReviewDetailAction.Retried) },
        )
      }
      is LoadState.Success -> items(
        items = commentsState.value.rootComments(),
        key = { it.comment.id },
      ) { root ->
        CommentGroup(
          root = root,
          replies = commentsState.value.replies(root.comment.id),
          expanded = root.comment.id in commentsState.value.expandedRootIds,
          loadingReplies = root.comment.id in commentsState.value.loadingReplyRootIds,
          replyError = commentsState.value.replyErrors[root.comment.id],
          onReply = { username ->
            onAction(ReviewDetailAction.ReplySelected(root.comment.id, username))
          },
          onToggleReplies = {
            onAction(ReviewDetailAction.RepliesToggled(root.comment.id))
          },
          onRetryComment = { onAction(ReviewDetailAction.RetryComment(it)) },
          onDeleteFailed = { onAction(ReviewDetailAction.DeleteFailedComment(it)) },
        )
      }
    }
  }
}

@Composable
private fun FullReviewCard(
  review: CommunityReview,
  onMediaClick: () -> Unit,
) {
  var spoilerRevealed by rememberSaveable(review.id) { mutableStateOf(false) }
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f)),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        AuthorAvatar(review.author.displayName)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(review.author.displayName, fontWeight = FontWeight.Bold)
          Text(
            text = "@${review.author.username} · ${relativeTime(review.createdAt)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
          )
        }
        review.authorRating?.let { rating -> RatingBadge(rating) }
      }
      Surface(
        onClick = onMediaClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          horizontalArrangement = Arrangement.spacedBy(11.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TmdbImage(
            path = review.posterPath,
            contentDescription = null,
            modifier = Modifier
              .size(width = 54.dp, height = 72.dp)
              .clip(RoundedCornerShape(12.dp)),
            size = "w185",
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = review.mediaTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = listOfNotNull(review.releaseYear, review.filmeraRating?.let { "Filmera %.1f".format(it) })
                .joinToString(" · "),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      }
      review.headline?.let {
        Text(it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      }
      if (review.containsSpoilers && !spoilerRevealed) {
        Surface(
          onClick = { spoilerRevealed = true },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.filmeraColors.warning.copy(alpha = 0.1f),
          border = BorderStroke(1.dp, MaterialTheme.filmeraColors.warning.copy(alpha = 0.4f)),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              stringResource(R.string.community_spoiler_hidden),
              color = MaterialTheme.filmeraColors.warning,
              fontWeight = FontWeight.Bold,
            )
            Text(
              stringResource(R.string.community_tap_reveal),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      } else {
        Text(
          text = review.body,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReviewReaction.entries.forEach { reaction ->
          val count = review.reactions.count(reaction)
          if (count > 0) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
              Text(
                text = "${reaction.emoji} $count",
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CommentGroup(
  root: CommentItemUi,
  replies: List<CommentItemUi>,
  expanded: Boolean,
  loadingReplies: Boolean,
  replyError: AppError?,
  onReply: (String) -> Unit,
  onToggleReplies: () -> Unit,
  onRetryComment: (String) -> Unit,
  onDeleteFailed: (String) -> Unit,
) {
  Column(modifier = Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    CommentCard(
      item = root,
      onReply = { onReply(root.comment.author.username) },
      onRetry = { onRetryComment(root.comment.id) },
      onDeleteFailed = { onDeleteFailed(root.comment.id) },
    )
    if (root.comment.replyCount > 0 || replies.isNotEmpty()) {
      TextButton(onClick = onToggleReplies, modifier = Modifier.padding(start = 44.dp)) {
        if (loadingReplies) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
          Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
          )
        }
        Spacer(Modifier.width(6.dp))
        Text(
          if (expanded) stringResource(R.string.community_hide_replies)
          else stringResource(R.string.community_view_replies, root.comment.replyCount),
        )
      }
    }
    AnimatedVisibility(
      visible = expanded,
      enter = fadeIn() + slideInVertically { it / 5 },
      exit = fadeOut() + slideOutVertically { it / 5 },
    ) {
      Row(modifier = Modifier.padding(start = 20.dp)) {
        Box(
          modifier = Modifier
            .width(2.dp)
            .heightIn(min = 52.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
        )
        Column(
          modifier = Modifier
            .weight(1f)
            .padding(start = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          replies.forEach { reply ->
            CommentCard(
              item = reply,
              compact = true,
              onReply = { onReply(reply.comment.author.username) },
              onRetry = { onRetryComment(reply.comment.id) },
              onDeleteFailed = { onDeleteFailed(reply.comment.id) },
            )
          }
          replyError?.let {
            TextButton(onClick = onToggleReplies) {
              Icon(Icons.Outlined.Refresh, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text(stringResource(R.string.retry))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CommentCard(
  item: CommentItemUi,
  onReply: () -> Unit,
  onRetry: () -> Unit,
  onDeleteFailed: () -> Unit,
  compact: Boolean = false,
) {
  val comment = item.comment
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(if (compact) 16.dp else 18.dp),
    color = when (item.deliveryState) {
      CommentDeliveryState.SENDING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
      CommentDeliveryState.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.24f)
      CommentDeliveryState.SENT -> MaterialTheme.colorScheme.surfaceContainerLow
    },
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
  ) {
    Column(
      modifier = Modifier.padding(if (compact) 12.dp else 14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        AuthorAvatar(comment.author.displayName, size = if (compact) 30 else 34)
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            comment.author.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            relativeTime(comment.createdAt),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
          )
        }
        if (item.deliveryState == CommentDeliveryState.SENDING) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        }
      }
      Text(comment.body, style = MaterialTheme.typography.bodyMedium)
      if (item.deliveryState == CommentDeliveryState.FAILED) {
        Text(
          text = stringResource(R.string.community_comment_send_failed),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.labelMedium,
        )
        Row {
          TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
          TextButton(onClick = onDeleteFailed) { Text(stringResource(R.string.delete)) }
        }
      } else {
        TextButton(onClick = onReply, contentPadding = PaddingValues(horizontal = 2.dp)) {
          Icon(Icons.AutoMirrored.Outlined.Reply, contentDescription = null, modifier = Modifier.size(17.dp))
          Spacer(Modifier.width(5.dp))
          Text(stringResource(R.string.community_reply))
        }
      }
    }
  }
}

@Composable
private fun CommentComposer(
  state: ReviewDetailUiState,
  onAction: (ReviewDetailAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current
  var focused by remember { mutableStateOf(false) }

  LaunchedEffect(state.focusRequestNonce) {
    if (state.focusRequestNonce > 0) {
      focusRequester.requestFocus()
      keyboardController?.show()
    }
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.98f),
    border = BorderStroke(
      1.dp,
      if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.64f)
      else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f),
    ),
    shadowElevation = 10.dp,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      AnimatedContent(
        targetState = state.replyTarget,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "reply target",
      ) { target ->
        if (target != null) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = stringResource(R.string.community_replying_to, target.targetUsername),
              modifier = Modifier.weight(1f),
              color = MaterialTheme.colorScheme.primary,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = { onAction(ReviewDetailAction.ReplyCancelled) }) {
              Text(stringResource(R.string.cancel))
            }
          }
        } else {
          Spacer(Modifier.height(0.dp))
        }
      }
      Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = state.composerText,
          onValueChange = { onAction(ReviewDetailAction.ComposerTextChanged(it)) },
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 52.dp, max = 128.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused },
          enabled = !state.isSending,
          placeholder = { Text(stringResource(R.string.community_write_comment)) },
          shape = RoundedCornerShape(20.dp),
          minLines = 1,
          maxLines = 5,
          keyboardActions = KeyboardActions(onDone = {
            if (state.composerText.isNotBlank()) onAction(ReviewDetailAction.SendComment)
          }),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
          ),
        )
        Surface(
          onClick = { onAction(ReviewDetailAction.SendComment) },
          modifier = Modifier
            .size(52.dp)
            .semantics { contentDescription = "Send comment" },
          enabled = state.composerText.isNotBlank() && !state.isSending,
          shape = RoundedCornerShape(18.dp),
          color = if (state.composerText.isNotBlank()) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.surfaceContainerHighest,
          contentColor = if (state.composerText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
          Box(contentAlignment = Alignment.Center) {
            if (state.isSending) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            }
          }
        }
      }
      if (state.composerText.length > 1_600) {
        Text(
          text = stringResource(R.string.community_comment_character_count, state.composerText.length),
          modifier = Modifier.align(Alignment.End),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.labelSmall,
        )
      }
    }
  }
}

@Composable
private fun RatingBadge(rating: Int) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.filmeraColors.rating.copy(alpha = 0.13f),
    border = BorderStroke(1.dp, MaterialTheme.filmeraColors.rating.copy(alpha = 0.38f)),
  ) {
    Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
      Icon(
        Icons.Filled.Star,
        contentDescription = null,
        modifier = Modifier.size(15.dp),
        tint = MaterialTheme.filmeraColors.rating,
      )
      Spacer(Modifier.width(4.dp))
      Text("$rating/10", color = MaterialTheme.filmeraColors.rating, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun AuthorAvatar(name: String, size: Int = 38) {
  Box(
    modifier = Modifier
      .size(size.dp)
      .clip(CircleShape)
      .background(
        Brush.linearGradient(
          listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
          ),
        ),
      ),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = name.firstOrNull()?.uppercase() ?: "F",
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun RealtimePausedBanner(onReconnect: () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.filmeraColors.warning.copy(alpha = 0.09f),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.filmeraColors.warning)
      Text(
        stringResource(R.string.community_live_paused),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodySmall,
      )
      TextButton(onClick = onReconnect) { Text(stringResource(R.string.community_reconnect)) }
    }
  }
}

@Composable
private fun CommentsEmptyState() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    Icon(
      Icons.Outlined.ChatBubbleOutline,
      contentDescription = null,
      modifier = Modifier.size(32.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Text(stringResource(R.string.community_comments_empty_title), fontWeight = FontWeight.Bold)
    Text(
      stringResource(R.string.community_comments_empty_message),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun ReviewDetailSkeleton() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      Modifier
        .fillMaxWidth()
        .height(340.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
    repeat(3) { CommentSkeleton() }
  }
}

@Composable
private fun CommentSkeleton() {
  Box(
    Modifier
      .fillMaxWidth()
      .height(112.dp)
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colorScheme.surfaceContainer),
  )
}

@Composable
private fun ReviewDetailMessage(message: String, onRetry: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
  }
}

@Composable
private fun relativeTime(value: String): String = remember(value) {
  runCatching {
    val duration = Duration.between(Instant.parse(value), Instant.now())
    when {
      duration.isNegative || duration.toMinutes() < 1 -> "now"
      duration.toHours() < 1 -> "${duration.toMinutes()}m"
      duration.toDays() < 1 -> "${duration.toHours()}h"
      duration.toDays() < 7 -> "${duration.toDays()}d"
      else -> "${duration.toDays() / 7}w"
    }
  }.getOrDefault("recently")
}
