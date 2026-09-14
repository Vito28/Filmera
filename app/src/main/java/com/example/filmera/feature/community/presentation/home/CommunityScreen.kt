package com.example.filmera.feature.community.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.core.navigation.AppDestination
import com.example.filmera.core.navigation.BottomNavigationBar
import com.example.filmera.core.navigation.defaultBottomNavigationDestinations
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.core.ui.messageResource
import com.example.filmera.feature.community.domain.CommunityReview

@Composable
fun CommunityRoute(
  navController: NavHostController,
  onOpenSearch: () -> Unit,
  onOpenNotifications: () -> Unit,
  viewModel: CommunityViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  CommunityScreen(
    state = state,
    onAction = viewModel::onAction,
    onOpenReview = { navController.navigate(AppDestination.ReviewDetail.createRoute(it)) },
    onOpenMedia = { navController.navigate(AppDestination.Detail.createRoute(it.mediaKey)) },
    onOpenSearch = onOpenSearch,
    onOpenNotifications = onOpenNotifications,
    bottomBar = {
      BottomNavigationBar(navController, defaultBottomNavigationDestinations)
    },
  )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
  state: CommunityUiState,
  onAction: (CommunityAction) -> Unit,
  onOpenReview: (String) -> Unit,
  onOpenMedia: (CommunityReview) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenNotifications: () -> Unit,
  bottomBar: @Composable () -> Unit = {},
) {
  val snackbar = remember { SnackbarHostState() }
  val errorMessage = state.writeError?.let { stringResource(it.messageResource()) }
  LaunchedEffect(errorMessage) {
    errorMessage?.let {
      snackbar.showSnackbar(it)
      onAction(CommunityAction.ErrorDismissed)
    }
  }
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbar) },
    bottomBar = bottomBar,
    topBar = {
      TopAppBar(
        title = {
          Text(stringResource(R.string.community_title), fontWeight = FontWeight.Bold)
        },
        actions = {
          IconButton(onClick = onOpenSearch) {
            Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.open_search))
          }
          IconButton(onClick = onOpenNotifications) {
            Icon(
              Icons.Outlined.Notifications,
              contentDescription = stringResource(R.string.open_notifications),
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
  ) { padding ->
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(listState, state.hasMore) {
      derivedStateOf {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        state.hasMore && last >= listState.layoutInfo.totalItemsCount - 4
      }
    }
    LaunchedEffect(shouldLoadMore) {
      if (shouldLoadMore) onAction(CommunityAction.LoadMore)
    }
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = 16.dp,
        top = padding.calculateTopPadding() + 8.dp,
        end = 16.dp,
        bottom = padding.calculateBottomPadding() + 24.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item(key = "community-hero") { CommunityHero(state.selectedTab) }
      item(key = "community-tabs") {
        CommunityTabs(
          selected = state.selectedTab,
          onSelect = { onAction(CommunityAction.TabSelected(it)) },
        )
      }
      if (state.selectedTab == CommunityTab.FORUM) {
        item(key = "forum-placeholder") { ForumPlaceholder() }
      } else {
        when (val reviews = state.reviewsState) {
          LoadState.Loading -> items(4, key = { "review-skeleton-$it" }) {
            Box(
              Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
          }
          LoadState.Empty -> item(key = "community-empty") { CommunityEmpty() }
          is LoadState.Error -> item(key = "community-error") {
            CommunityError(
              message = stringResource(reviews.error.messageResource()),
              onRetry = { onAction(CommunityAction.Retry) },
            )
          }
          is LoadState.Success -> items(
            items = reviews.value,
            key = CommunityReview::id,
          ) { review ->
            CommunityReviewListCard(
              review = review,
              onOpen = { onOpenReview(review.id) },
              onMedia = { onOpenMedia(review) },
              onHelpful = { onAction(CommunityAction.HelpfulToggled(review.id)) },
            )
          }
        }
        if (state.isLoadingMore) {
          item(key = "community-load-more") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CommunityHero(tab: CommunityTab) {
  val title = when (tab) {
    CommunityTab.FOR_YOU -> stringResource(R.string.community_hero_for_you)
    CommunityTab.REVIEWS -> stringResource(R.string.community_hero_reviews)
    CommunityTab.FORUM -> stringResource(R.string.community_hero_forum)
  }
  val subtitle = when (tab) {
    CommunityTab.FOR_YOU -> stringResource(R.string.community_hero_for_you_subtitle)
    CommunityTab.REVIEWS -> stringResource(R.string.community_hero_reviews_subtitle)
    CommunityTab.FORUM -> stringResource(R.string.community_hero_forum_subtitle)
  }
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = Color.Transparent,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
  ) {
    Box(
      Modifier.background(
        Brush.linearGradient(
          listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f),
            MaterialTheme.colorScheme.surfaceContainerLow,
          ),
        ),
      ),
    ) {
      AnimatedContent(
        targetState = title to subtitle,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "community hero",
      ) { copy ->
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
          Text(copy.first, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
          Text(
            copy.second,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }
  }
}

@Composable
private fun CommunityTabs(selected: CommunityTab, onSelect: (CommunityTab) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    CommunityTab.entries.forEach { tab ->
      val active = tab == selected
      Surface(
        onClick = { onSelect(tab) },
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else Color.Transparent,
        border = BorderStroke(
          1.dp,
          if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
        ),
      ) {
        Text(
          text = when (tab) {
            CommunityTab.FOR_YOU -> stringResource(R.string.community_tab_for_you)
            CommunityTab.REVIEWS -> stringResource(R.string.community_tab_reviews)
            CommunityTab.FORUM -> stringResource(R.string.community_tab_forum)
          },
          modifier = Modifier.padding(vertical = 10.dp),
          color = if (active) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun CommunityReviewListCard(
  review: CommunityReview,
  onOpen: () -> Unit,
  onMedia: () -> Unit,
  onHelpful: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Text(review.author.displayName.take(1).uppercase(), color = MaterialTheme.colorScheme.primary)
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
        review.authorRating?.let {
          Text(
            "$it/10",
            color = MaterialTheme.filmeraColors.rating,
            fontWeight = FontWeight.Bold,
          )
        }
      }
      Surface(
        onClick = onMedia,
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
      ) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
          TmdbImage(
            review.posterPath,
            contentDescription = null,
            modifier = Modifier
              .size(width = 46.dp, height = 62.dp)
              .clip(RoundedCornerShape(10.dp)),
            size = "w185",
          )
          Spacer(Modifier.width(10.dp))
          Text(
            review.mediaTitle,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Column {
        review.headline?.let {
          Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Spacer(Modifier.height(4.dp))
        }
        Text(
          if (review.containsSpoilers) stringResource(R.string.community_spoiler_preview)
          else review.body,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onHelpful, enabled = !review.helpful.isSubmitting) {
          if (review.helpful.isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
          } else {
            Icon(
              if (review.helpful.isHelpful) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
              contentDescription = null,
              tint = if (review.helpful.isHelpful) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(Modifier.width(6.dp))
          Text(review.helpful.count.toString())
        }
        TextButton(onClick = onOpen) {
          Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text(review.commentCount.toString())
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onOpen) { Text(stringResource(R.string.community_read_more)) }
      }
    }
  }
}

@Composable
private fun ForumPlaceholder() {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(
      Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(Icons.Outlined.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      Text(stringResource(R.string.community_forum_preparing), fontWeight = FontWeight.Bold)
      Text(
        stringResource(R.string.community_forum_preparing_message),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CommunityEmpty() {
  Column(
    Modifier
      .fillMaxWidth()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(Icons.Outlined.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(9.dp))
    Text(stringResource(R.string.community_empty_title), fontWeight = FontWeight.Bold)
    Text(
      stringResource(R.string.community_empty_message),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun CommunityError(message: String, onRetry: () -> Unit) {
  Column(
    Modifier
      .fillMaxWidth()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
  }
}
