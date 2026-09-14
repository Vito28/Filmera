package com.example.filmera.feature.detail.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.core.model.MediaDetails
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.navigation.FilmeraScaffold
import com.example.filmera.core.navigation.AppDestination
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.component.EmptyState
import com.example.filmera.core.ui.component.ErrorState
import com.example.filmera.core.ui.component.LoadingState
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.core.ui.component.InlineMessageBanner
import com.example.filmera.feature.detail.domain.DetailContent
import com.example.filmera.feature.community.domain.CommunityReview
import com.example.filmera.feature.library.domain.WatchStatus
import com.example.filmera.feature.detail.presentation.component.CreditsSection
import com.example.filmera.core.ui.component.MediaPosterCarousel
import com.example.filmera.core.ui.messageResource
import com.example.filmera.feature.home.presentation.component.HomeReviewComposerSheetContent
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun DetailRoute(
  navController: NavHostController,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  onPlayTrailer: (String) -> Unit,
  viewModel: DetailViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  DetailScreen(
    state = state,
    navController = navController,
    onNavigateBack = navController::popBackStack,
    onMediaClick = onMediaClick,
    onPersonClick = onPersonClick,
    onPlayTrailer = onPlayTrailer,
    onToggleFavorite = viewModel::toggleFavorite,
    onToggleWatchlist = viewModel::toggleWatchlist,
    onOpenReview = { reviewId ->
      navController.navigate(AppDestination.ReviewDetail.createRoute(reviewId))
    },
    onSubmitRating = viewModel::submitRating,
    onToggleHelpful = viewModel::toggleHelpful,
    onRetryCommunity = viewModel::retryCommunity,
    onWriteReview = viewModel::openReviewComposer,
    onDismissReview = viewModel::dismissReviewComposer,
    onReviewRatingSelected = viewModel::selectReviewRating,
    onReviewHeadlineChanged = viewModel::changeReviewHeadline,
    onReviewBodyChanged = viewModel::changeReviewBody,
    onReviewSpoilerChanged = viewModel::changeReviewSpoiler,
    onPublishReview = viewModel::publishReview,
    onRetry = viewModel::retry,
  )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
  state: DetailUiState,
  navController: NavHostController,
  onNavigateBack: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  onPlayTrailer: (String) -> Unit,
  onToggleFavorite: (MediaItem) -> Unit,
  onToggleWatchlist: (MediaItem) -> Unit,
  onOpenReview: (String) -> Unit,
  onSubmitRating: (Int) -> Unit,
  onToggleHelpful: (String) -> Unit,
  onRetryCommunity: () -> Unit,
  onWriteReview: (MediaItem) -> Unit,
  onDismissReview: () -> Unit,
  onReviewRatingSelected: (Int) -> Unit,
  onReviewHeadlineChanged: (String) -> Unit,
  onReviewBodyChanged: (String) -> Unit,
  onReviewSpoilerChanged: (Boolean) -> Unit,
  onPublishReview: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showDiscardReviewDialog by remember { mutableStateOf(false) }
  val title = (state.contentState as? LoadState.Success)?.value?.details?.title
    ?: stringResource(R.string.detail_title)

  FilmeraScaffold(
    title = title,
    navController = navController,
    modifier = modifier,
    showBottomBar = false,
    onNavigateBack = onNavigateBack,
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
    ) {
      when (val contentState = state.contentState) {
        LoadState.Loading -> LoadingState()
        LoadState.Empty -> EmptyState(
          title = stringResource(R.string.detail_empty_title),
          message = stringResource(R.string.detail_empty_message),
        )
        is LoadState.Error -> ErrorState(contentState.error, onRetry)
        is LoadState.Success -> DetailContent(
          content = contentState.value,
          favoriteKeys = state.favoriteKeys,
          hasFavoriteWriteError = state.hasFavoriteWriteError,
          hasLibraryWriteError = state.hasLibraryWriteError,
          watchStatus = state.watchStatus,
          onMediaClick = onMediaClick,
          onPersonClick = onPersonClick,
          onPlayTrailer = onPlayTrailer,
          onToggleFavorite = onToggleFavorite,
          onToggleWatchlist = onToggleWatchlist,
          communityState = state.communityState,
          selectedRating = state.selectedRating,
          filmeraRating = state.filmeraRating,
          ratingCount = state.ratingCount,
          isRatingSubmitting = state.isRatingSubmitting,
          pendingHelpfulReviewIds = state.pendingHelpfulReviewIds,
          hasCommunityWriteError = state.hasCommunityWriteError,
          onOpenReview = onOpenReview,
          onSubmitRating = onSubmitRating,
          onToggleHelpful = onToggleHelpful,
          onRetryCommunity = onRetryCommunity,
          onWriteReview = onWriteReview,
        )
      }
    }
  }

  if (state.reviewComposer.isVisible) {
    ModalBottomSheet(
      onDismissRequest = {
        if (state.reviewComposer.isDirty) showDiscardReviewDialog = true else onDismissReview()
      },
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
      HomeReviewComposerSheetContent(
        state = state.reviewComposer,
        onRatingSelected = onReviewRatingSelected,
        onHeadlineChanged = onReviewHeadlineChanged,
        onBodyChanged = onReviewBodyChanged,
        onSpoilerChanged = onReviewSpoilerChanged,
        onPublish = onPublishReview,
      )
    }
  }

  if (showDiscardReviewDialog) {
    AlertDialog(
      onDismissRequest = { showDiscardReviewDialog = false },
      title = { Text(stringResource(R.string.community_discard_review_title)) },
      text = { Text(stringResource(R.string.community_discard_review_message)) },
      confirmButton = {
        TextButton(onClick = {
          showDiscardReviewDialog = false
          onDismissReview()
        }) { Text(stringResource(R.string.community_discard)) }
      },
      dismissButton = {
        TextButton(onClick = { showDiscardReviewDialog = false }) {
          Text(stringResource(R.string.community_keep_editing))
        }
      },
    )
  }
}

@Composable
private fun DetailCommunitySection(
  state: LoadState<List<CommunityReview>>,
  selectedRating: Int?,
  filmeraRating: Double?,
  ratingCount: Long,
  isRatingSubmitting: Boolean,
  pendingHelpfulReviewIds: Set<String>,
  hasWriteError: Boolean,
  onSubmitRating: (Int) -> Unit,
  onToggleHelpful: (String) -> Unit,
  onOpenReview: (String) -> Unit,
  onRetry: () -> Unit,
  onWriteReview: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.detail_community_title),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = stringResource(R.string.detail_community_subtitle),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      OutlinedButton(onClick = onWriteReview) {
        Icon(Icons.Outlined.Edit, contentDescription = null)
        Text(
          text = stringResource(R.string.detail_write_review),
          modifier = Modifier.padding(start = 7.dp),
        )
      }
    }

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      color = Color.Transparent,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
    ) {
      Column(
        modifier = Modifier
          .background(
            Brush.linearGradient(
              listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                MaterialTheme.colorScheme.surfaceContainerLow,
              ),
            ),
          )
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.filmeraColors.rating,
            modifier = Modifier.size(28.dp),
          )
          Spacer(Modifier.width(9.dp))
          Text(
            text = filmeraRating?.let {
              stringResource(R.string.detail_filmera_rating_value, it)
            } ?: stringResource(R.string.detail_filmera_rating_empty),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
          )
          Spacer(Modifier.weight(1f))
          Text(
            text = stringResource(
              R.string.detail_rating_count,
              ratingCount,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
          )
        }
        Text(
          text = stringResource(R.string.detail_choose_rating),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items((1..10).toList(), key = { it }) { rating ->
            val selected = selectedRating == rating
            Surface(
              onClick = { onSubmitRating(rating) },
              enabled = !isRatingSubmitting,
              modifier = Modifier.size(46.dp),
              shape = CircleShape,
              color = if (selected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceContainerHigh,
              contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurface,
              border = BorderStroke(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
              ),
            ) {
              Box(contentAlignment = Alignment.Center) {
                if (selected && isRatingSubmitting) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(19.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                } else {
                  Text(rating.toString(), fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }

    if (hasWriteError) {
      InlineMessageBanner(
        message = stringResource(R.string.detail_community_write_error),
        isError = true,
      )
    }

    when (state) {
      LoadState.Loading -> repeat(2) {
        Box(
          Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
      }
      LoadState.Empty -> Surface(
        onClick = onWriteReview,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
      ) {
        Column(
          modifier = Modifier.padding(22.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          Icon(Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text(stringResource(R.string.community_write_first_review), fontWeight = FontWeight.Bold)
          Text(
            stringResource(R.string.detail_community_empty_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      is LoadState.Error -> Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(state.error.messageResource()),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
      }
      is LoadState.Success -> state.value.take(4).forEach { review ->
        DetailReviewCard(
          review = review,
          isHelpfulPending = review.id in pendingHelpfulReviewIds,
          onHelpful = { onToggleHelpful(review.id) },
          onOpen = { onOpenReview(review.id) },
        )
      }
    }
  }
}

@Composable
private fun DetailReviewCard(
  review: CommunityReview,
  isHelpfulPending: Boolean,
  onHelpful: () -> Unit,
  onOpen: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            review.author.displayName.take(1).uppercase(),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(review.author.displayName, fontWeight = FontWeight.SemiBold)
          Text(
            "@${review.author.username}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
          )
        }
        review.authorRating?.let { rating ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.filmeraColors.rating.copy(alpha = 0.14f),
          ) {
            Text(
              text = stringResource(R.string.community_rating_value, rating),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              color = MaterialTheme.filmeraColors.rating,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
      review.headline?.let {
        Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
      Text(
        text = if (review.containsSpoilers) stringResource(R.string.community_spoiler_preview)
        else review.body,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onHelpful, enabled = !isHelpfulPending) {
          if (isHelpfulPending) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          } else {
            Icon(
              imageVector = if (review.helpful.isHelpful) Icons.Filled.ThumbUp
              else Icons.Outlined.ThumbUp,
              contentDescription = stringResource(R.string.community_mark_helpful, review.helpful.count),
              tint = if (review.helpful.isHelpful) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = stringResource(R.string.detail_helpful_count, review.helpful.count),
            modifier = Modifier.padding(start = 7.dp),
          )
        }
        TextButton(onClick = onOpen) {
          Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null)
          Text(
            text = stringResource(R.string.detail_comment_count, review.commentCount),
            modifier = Modifier.padding(start = 7.dp),
          )
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onOpen) { Text(stringResource(R.string.community_read_more)) }
      }
    }
  }
}

@Composable
private fun DetailContent(
  content: DetailContent,
  favoriteKeys: Set<com.example.filmera.core.model.MediaKey>,
  hasFavoriteWriteError: Boolean,
  hasLibraryWriteError: Boolean,
  watchStatus: WatchStatus,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  onPlayTrailer: (String) -> Unit,
  onToggleFavorite: (MediaItem) -> Unit,
  onToggleWatchlist: (MediaItem) -> Unit,
  communityState: LoadState<List<CommunityReview>>,
  selectedRating: Int?,
  filmeraRating: Double?,
  ratingCount: Long,
  isRatingSubmitting: Boolean,
  pendingHelpfulReviewIds: Set<String>,
  hasCommunityWriteError: Boolean,
  onOpenReview: (String) -> Unit,
  onSubmitRating: (Int) -> Unit,
  onToggleHelpful: (String) -> Unit,
  onRetryCommunity: () -> Unit,
  onWriteReview: (MediaItem) -> Unit,
) {
  val item = content.details.toMediaItem()
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    if (hasFavoriteWriteError) {
      item(key = "detail-favorite-write-error") {
        InlineMessageBanner(
          message = stringResource(R.string.favorite_update_error),
          isError = true,
        )
      }
    }
    if (hasLibraryWriteError) {
      item(key = "detail-library-write-error") {
        InlineMessageBanner(
          message = stringResource(R.string.library_write_error),
          isError = true,
        )
      }
    }
    if (content.hasPartialFailures) {
      item(key = "detail-partial-content-message") {
        InlineMessageBanner(
          message = stringResource(R.string.detail_partial_content_message),
        )
      }
    }
    item(key = "detail-hero") {
      DetailHero(
        details = content.details,
        isFavorite = item.key in favoriteKeys,
        watchStatus = watchStatus,
        hasTrailer = content.trailer != null,
        onPlayTrailer = { content.trailer?.key?.let(onPlayTrailer) },
        onToggleFavorite = { onToggleFavorite(item) },
        onToggleWatchlist = { onToggleWatchlist(item) },
      )
    }
    item(key = "detail-overview") { OverviewSection(content.details) }
    item(key = "detail-community") {
      DetailCommunitySection(
        state = communityState,
        selectedRating = selectedRating,
        filmeraRating = filmeraRating,
        ratingCount = ratingCount,
        isRatingSubmitting = isRatingSubmitting,
        pendingHelpfulReviewIds = pendingHelpfulReviewIds,
        hasWriteError = hasCommunityWriteError,
        onSubmitRating = onSubmitRating,
        onToggleHelpful = onToggleHelpful,
        onOpenReview = onOpenReview,
        onRetry = onRetryCommunity,
        onWriteReview = { onWriteReview(item) },
      )
    }
    item(key = "detail-information") { InformationSection(content.details) }
    item(key = "detail-credits") {
      CreditsSection(
        credits = content.credits,
        onCastClick = { onPersonClick(it.id) },
      )
    }
    if (content.recommendations.isNotEmpty()) {
      item(key = "detail-recommendations") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = stringResource(R.string.recommendations),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
          )
          MediaPosterCarousel(
            items = content.recommendations,
            favoriteKeys = favoriteKeys,
            onMediaClick = onMediaClick,
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

@Composable
private fun DetailHero(
  details: MediaDetails,
  isFavorite: Boolean,
  watchStatus: WatchStatus,
  hasTrailer: Boolean,
  onPlayTrailer: () -> Unit,
  onToggleFavorite: () -> Unit,
  onToggleWatchlist: () -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth()) {
    TmdbImage(
      path = details.posterPath,
      contentDescription = stringResource(R.string.poster_content_description, details.title),
      modifier = Modifier
        .size(width = 128.dp, height = 192.dp)
        .clip(RoundedCornerShape(18.dp)),
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = details.title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = listOfNotNull(
          details.releaseDate?.take(4),
          details.runtimeMinutes?.let { stringResource(R.string.runtime_minutes, it) },
          if (details.type == MediaType.TV_SHOW) stringResource(R.string.media_type_tv_show) else null,
        ).joinToString(" • "),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = null,
          tint = MaterialTheme.filmeraColors.rating,
        )
        Text(
          text = stringResource(R.string.rating_format, details.voteAverage),
          modifier = Modifier.padding(start = 4.dp),
          style = MaterialTheme.typography.titleMedium,
        )
      }
      Button(
        onClick = onPlayTrailer,
        enabled = hasTrailer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Text(stringResource(R.string.watch_trailer), modifier = Modifier.padding(start = 8.dp))
      }
      OutlinedButton(
        onClick = onToggleWatchlist,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(
          imageVector = if (watchStatus == WatchStatus.NONE) {
            Icons.Outlined.BookmarkBorder
          } else {
            Icons.Default.Bookmark
          },
          contentDescription = null,
        )
        Text(
          text = stringResource(
            if (watchStatus == WatchStatus.NONE) {
              R.string.add_to_watchlist
            } else {
              R.string.library_remove_status
            },
          ),
          modifier = Modifier.padding(start = 8.dp),
        )
      }
      OutlinedButton(
        onClick = onToggleFavorite,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(
          imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
          contentDescription = null,
        )
        Text(
          text = stringResource(
            if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites,
          ),
          modifier = Modifier.padding(start = 8.dp),
        )
      }
    }
  }
}

@Composable
private fun OverviewSection(details: MediaDetails) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    details.tagline?.let {
      Text(
        text = stringResource(R.string.quoted_text, it),
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleMedium,
      )
    }
    Text(
      text = stringResource(R.string.overview),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
    )
    Text(
      text = details.overview.ifBlank { stringResource(R.string.overview_unavailable) },
      style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@Composable
private fun InformationSection(details: MediaDetails) {
  val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
    currency = Currency.getInstance("USD")
    maximumFractionDigits = 0
  }
  val rows = listOfNotNull(
    details.originalTitle.takeIf { it.isNotBlank() }?.let { stringResource(R.string.original_title) to it },
    details.genres.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.genres) to it.joinToString() },
    details.spokenLanguages.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.languages) to it.joinToString() },
    details.productionCountries.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.countries) to it.joinToString() },
    details.productionCompanies.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.companies) to it.joinToString() },
    details.budget?.let { stringResource(R.string.budget) to currencyFormatter.format(it) },
    details.revenue?.let { stringResource(R.string.revenue) to currencyFormatter.format(it) },
    details.status?.takeIf(String::isNotBlank)?.let { stringResource(R.string.status) to it },
    details.releaseDate?.let { stringResource(R.string.release_date) to it },
  )

  if (rows.isEmpty()) return
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(R.string.information),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
      )
      rows.forEachIndexed { index, (label, value) ->
        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, modifier = Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}
