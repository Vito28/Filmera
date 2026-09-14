package com.example.filmera.feature.discover.presentation

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.FilmeraTheme
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.navigation.BottomNavigationBar
import com.example.filmera.core.navigation.defaultBottomNavigationDestinations
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.LazyLayoutKey
import com.example.filmera.core.ui.component.EmptyState
import com.example.filmera.core.ui.component.ErrorState
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.feature.discover.domain.RecommendationFeedbackAction
import com.example.filmera.feature.discover.domain.RECOMMENDATION_PEOPLE_SECTION_ID
import com.example.filmera.feature.discover.domain.RecommendationSection
import com.example.filmera.feature.discover.domain.RecommendedContent
import com.example.filmera.feature.discover.domain.RecommendedItem
import com.example.filmera.feature.discover.domain.RecommendedPerson
import com.example.filmera.feature.discover.domain.RankedMediaItem
import com.example.filmera.feature.preferences.domain.supportedPreferenceGenres
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun DiscoverRoute(
  navController: NavHostController,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  onNotificationClick: () -> Unit,
  onOpenSearch: () -> Unit,
  onUpdatePreferences: () -> Unit,
  onSeeAllRecommendations: (String) -> Unit,
  viewModel: DiscoverViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  DiscoverScreen(
    state = state,
    onAction = viewModel::onAction,
    onMediaClick = onMediaClick,
    onPersonClick = onPersonClick,
    onNotificationClick = onNotificationClick,
    onOpenSearch = onOpenSearch,
    onUpdatePreferences = onUpdatePreferences,
    onSeeAllRecommendations = onSeeAllRecommendations,
    bottomBar = {
      BottomNavigationBar(
        navController = navController,
        destinations = defaultBottomNavigationDestinations,
      )
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
  state: DiscoverUiState,
  onAction: (DiscoverAction) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  onNotificationClick: () -> Unit,
  onOpenSearch: () -> Unit,
  onUpdatePreferences: () -> Unit,
  onSeeAllRecommendations: (String) -> Unit,
  modifier: Modifier = Modifier,
  bottomBar: @Composable () -> Unit = {},
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val saveableStateHolder = rememberSaveableStateHolder()
  var selectedTabName by rememberSaveable { mutableStateOf(DiscoverTab.RECOMMENDED.name) }
  var isScrolled by remember { mutableStateOf(false) }
  val selectedTab = runCatching { DiscoverTab.valueOf(selectedTabName) }
    .getOrDefault(DiscoverTab.RECOMMENDED)

  LaunchedEffect(selectedTab) {
    when (selectedTab) {
      DiscoverTab.RANKINGS -> onAction(DiscoverAction.RankingsOpened)
      DiscoverTab.EXPLORE -> onAction(DiscoverAction.ExploreOpened)
      DiscoverTab.RECOMMENDED -> Unit
    }
  }

  val feedbackMessage = stringResource(R.string.discover_feedback_saved)
  LaunchedEffect(state.feedbackSaved) {
    if (state.feedbackSaved != null) {
      snackbarHostState.showSnackbar(feedbackMessage)
      onAction(DiscoverAction.FeedbackMessageDismissed)
    }
  }

  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      DiscoverChrome(
        selectedTab = selectedTab,
        isScrolled = isScrolled,
        onTabSelected = {
          selectedTabName = it.name
          isScrolled = false
        },
        onSearchClick = onOpenSearch,
        onNotificationClick = onNotificationClick,
      )
    },
    bottomBar = bottomBar,
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      saveableStateHolder.SaveableStateProvider(selectedTab.name) {
        when (selectedTab) {
          DiscoverTab.RECOMMENDED -> RecommendedTab(
            state = state,
            onAction = onAction,
            onMediaClick = onMediaClick,
            onPersonClick = onPersonClick,
            onUpdatePreferences = onUpdatePreferences,
            onSeeAllRecommendations = onSeeAllRecommendations,
            onScrollStateChanged = { isScrolled = it },
          )
          DiscoverTab.RANKINGS -> RankingsTab(
            rankingsState = state.rankingsState,
            onMediaClick = onMediaClick,
            onRetry = { onAction(DiscoverAction.RankingsRetryRequested) },
            onScrollStateChanged = { isScrolled = it },
          )
          DiscoverTab.EXPLORE -> ExploreTab(
            state = state.explore,
            onAction = onAction,
            onMediaClick = onMediaClick,
            onScrollStateChanged = { isScrolled = it },
          )
        }
      }

      if (state.isRefreshing && selectedTab == DiscoverTab.RECOMMENDED) {
        LinearProgressIndicator(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .semantics {
              contentDescription = "Refreshing recommendations"
            },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverChrome(
  selectedTab: DiscoverTab,
  isScrolled: Boolean,
  onTabSelected: (DiscoverTab) -> Unit,
  onSearchClick: () -> Unit,
  onNotificationClick: () -> Unit,
) {
  val containerColor by animateColorAsState(
    targetValue = if (isScrolled) {
      MaterialTheme.colorScheme.surface
    } else {
      MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
    },
    animationSpec = tween(220),
    label = "discover-app-bar-color",
  )

  Surface(
    color = containerColor,
    tonalElevation = if (isScrolled) 3.dp else 0.dp,
  ) {
    Column {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.discover_title),
            fontWeight = FontWeight.Bold,
          )
        },
        actions = {
          IconButton(onClick = onSearchClick) {
            Icon(
              imageVector = Icons.Outlined.Search,
              contentDescription = stringResource(R.string.open_search),
            )
          }
          IconButton(onClick = onNotificationClick) {
            Icon(
              imageVector = Icons.Outlined.Notifications,
              contentDescription = stringResource(R.string.open_notifications),
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent,
        ),
      )
      PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Color.Transparent,
      ) {
        DiscoverTab.entries.forEach { tab ->
          Tab(
            selected = tab == selectedTab,
            onClick = { onTabSelected(tab) },
            text = {
              Text(
                text = stringResource(
                  when (tab) {
                    DiscoverTab.RECOMMENDED -> R.string.discover_tab_recommended
                    DiscoverTab.RANKINGS -> R.string.discover_tab_rankings
                    DiscoverTab.EXPLORE -> R.string.discover_tab_explore
                  },
                ),
              )
            },
          )
        }
      }
    }
  }
}

@Composable
private fun RecommendedTab(
  state: DiscoverUiState,
  onAction: (DiscoverAction) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  onUpdatePreferences: () -> Unit,
  onSeeAllRecommendations: (String) -> Unit,
  onScrollStateChanged: (Boolean) -> Unit,
) {
  when (val contentState = state.contentState) {
    LoadState.Loading -> RecommendationLoadingState()
    LoadState.Empty -> RecommendationEmptyState(onUpdatePreferences)
    is LoadState.Error -> ErrorState(
      error = contentState.error,
      onRetry = { onAction(DiscoverAction.RetryRequested) },
    )
    is LoadState.Success -> RecommendationFeed(
      content = contentState.value,
      selectedGenreId = state.selectedGenreId,
      isRefreshing = state.isRefreshing,
      onAction = onAction,
      onMediaClick = onMediaClick,
      onPersonClick = onPersonClick,
      onSeeAllRecommendations = onSeeAllRecommendations,
      onScrollStateChanged = onScrollStateChanged,
    )
  }
}

@Composable
private fun RankingsTab(
  rankingsState: LoadState<List<RankedMediaItem>>,
  onMediaClick: (MediaItem) -> Unit,
  onRetry: () -> Unit,
  onScrollStateChanged: (Boolean) -> Unit,
) {
  when (rankingsState) {
    LoadState.Loading -> RankingLoadingState()
    LoadState.Empty -> EmptyState(
      title = stringResource(R.string.rankings_empty_title),
      message = stringResource(R.string.rankings_empty_message),
    )
    is LoadState.Error -> ErrorState(
      error = rankingsState.error,
      onRetry = onRetry,
    )
    is LoadState.Success -> RankingList(
      rankings = rankingsState.value,
      onMediaClick = onMediaClick,
      onScrollStateChanged = onScrollStateChanged,
    )
  }
}

@Composable
private fun RankingList(
  rankings: List<RankedMediaItem>,
  onMediaClick: (MediaItem) -> Unit,
  onScrollStateChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()

  LaunchedEffect(listState) {
    snapshotFlow {
      listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 32
    }
      .distinctUntilChanged()
      .collect(onScrollStateChanged)
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .widthIn(max = 840.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(bottom = 32.dp),
    ) {
      item(key = "rankings-header") {
        RankingHeader(itemCount = rankings.size)
      }
      itemsIndexed(
        items = rankings,
        key = { _, item -> LazyLayoutKey.media("weekly-ranking", item.media) },
      ) { index, item ->
        RankingListTile(
          item = item,
          onClick = { onMediaClick(item.media) },
        )
        if (index < rankings.lastIndex) {
          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
          )
        }
      }
    }
  }
}

@Composable
private fun RankingHeader(
  itemCount: Int,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 24.dp),
  ) {
    Text(
      text = stringResource(R.string.rankings_title),
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = stringResource(R.string.rankings_subtitle),
      modifier = Modifier.padding(top = 6.dp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyMedium,
    )
    Text(
      text = pluralStringResource(
        R.plurals.rankings_item_count,
        itemCount,
        itemCount,
      ),
      modifier = Modifier.padding(top = 12.dp),
      color = MaterialTheme.colorScheme.primary,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
private fun RankingListTile(
  item: RankedMediaItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val mediaTypeLabel = stringResource(
    when (item.media.type) {
      MediaType.MOVIE -> R.string.rankings_movie
      MediaType.TV_SHOW -> R.string.rankings_series
    },
  )
  val releaseYear = item.media.releaseDate
    ?.take(4)
    ?.takeIf { year -> year.all(Char::isDigit) }
  val metadata = if (releaseYear != null) {
    stringResource(R.string.rankings_metadata, mediaTypeLabel, releaseYear)
  } else {
    mediaTypeLabel
  }
  val accessibilityLabel = stringResource(
    R.string.rankings_item_description,
    item.rank,
    item.media.title,
    mediaTypeLabel,
    item.media.voteAverage,
  )

  Row(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 120.dp)
      .clickable(onClick = onClick)
      .semantics(mergeDescendants = true) {
        contentDescription = accessibilityLabel
      }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = item.rank.toString(),
      modifier = Modifier.width(44.dp),
      color = if (item.rank <= 3) {
        MaterialTheme.colorScheme.primary
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant
      },
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    TmdbImage(
      path = item.media.posterPath,
      contentDescription = null,
      size = "w185",
      modifier = Modifier
        .width(64.dp)
        .height(96.dp)
        .clip(RoundedCornerShape(10.dp)),
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = 14.dp),
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = item.media.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = metadata,
        modifier = Modifier.padding(top = 6.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
      )
      Text(
        text = stringResource(R.string.rankings_rating, item.media.voteAverage),
        modifier = Modifier.padding(top = 4.dp),
        color = MaterialTheme.colorScheme.tertiary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}

@Composable
private fun RankingLoadingState(
  modifier: Modifier = Modifier,
) {
  val loadingDescription = stringResource(R.string.rankings_loading)

  Box(
    modifier = modifier
      .fillMaxSize()
      .semantics {
        contentDescription = loadingDescription
      },
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 840.dp)
        .fillMaxSize(),
      userScrollEnabled = false,
    ) {
      item(key = "rankings-loading-header") {
        RankingHeader(itemCount = 100)
      }
      items(
        count = 7,
        key = { index -> LazyLayoutKey.indexed("ranking-loading", index) },
      ) { index ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = (index + 1).toString(),
            modifier = Modifier.width(44.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          Box(
            modifier = Modifier
              .width(64.dp)
              .height(96.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.surfaceContainerHighest),
          )
          Column(
            modifier = Modifier
              .weight(1f)
              .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
            Box(
              modifier = Modifier
                .fillMaxWidth(0.42f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
            Box(
              modifier = Modifier
                .width(52.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RecommendationFeed(
  content: RecommendedContent,
  selectedGenreId: Int?,
  isRefreshing: Boolean,
  onAction: (DiscoverAction) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  onSeeAllRecommendations: (String) -> Unit,
  onScrollStateChanged: (Boolean) -> Unit,
) {
  val listState = rememberLazyListState()
  var feedbackItem by remember { mutableStateOf<RecommendedItem?>(null) }

  LaunchedEffect(listState) {
    snapshotFlow {
      listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 32
    }
      .distinctUntilChanged()
      .collect(onScrollStateChanged)
  }

  LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(28.dp),
  ) {
    if (content.heroItems.isNotEmpty()) {
      item(key = "recommended-hero") {
        PersonalizedHero(
          items = content.heroItems,
          onMediaClick = onMediaClick,
        )
      }
    }
    item(key = "recommended-genre-filter") {
      GenreFilterRow(
        genreIds = content.availableGenres,
        selectedGenreId = selectedGenreId,
        onGenreSelected = { onAction(DiscoverAction.GenreSelected(it)) },
      )
    }
    items(
      items = content.sections,
      key = RecommendationSection::id,
    ) { section ->
      RecommendationMediaSection(
        section = section,
        onMediaClick = onMediaClick,
        onFeedbackClick = { feedbackItem = it },
        onSeeAll = { onSeeAllRecommendations(section.id) },
      )
    }
    if (content.people.isNotEmpty()) {
      item(key = "recommended-people") {
        RecommendedPeopleSection(
          people = content.people,
          onPersonClick = onPersonClick,
          onSeeAll = {
            onSeeAllRecommendations(RECOMMENDATION_PEOPLE_SECTION_ID)
          },
        )
      }
    }
    item(key = "recommended-refresh") {
      Button(
        onClick = { onAction(DiscoverAction.RefreshRequested) },
        enabled = !isRefreshing,
        modifier = Modifier
          .padding(horizontal = 16.dp)
          .fillMaxWidth()
          .height(52.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = null,
        )
        Text(
          text = stringResource(
            if (isRefreshing) {
              R.string.discover_refreshing
            } else {
              R.string.discover_refresh
            },
          ),
          modifier = Modifier.padding(start = 8.dp),
        )
      }
    }
  }

  feedbackItem?.let { item ->
    RecommendationFeedbackSheet(
      item = item,
      onDismiss = { feedbackItem = null },
      onFeedback = { feedback ->
        onAction(DiscoverAction.FeedbackSubmitted(item.media, feedback))
        feedbackItem = null
      },
    )
  }
}

@Composable
private fun GenreFilterRow(
  genreIds: List<Int>,
  selectedGenreId: Int?,
  onGenreSelected: (Int?) -> Unit,
) {
  LazyRow(
    contentPadding = PaddingValues(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item(key = "genre-all") {
      FilterChip(
        selected = selectedGenreId == null,
        onClick = { onGenreSelected(null) },
        label = { Text(stringResource(R.string.discover_filter_all)) },
      )
    }
    items(
      items = genreIds,
      key = { genreId -> "genre-$genreId" },
    ) { genreId ->
      val genre = supportedPreferenceGenres.firstOrNull { it.id == genreId }
      if (genre != null) {
        FilterChip(
          selected = selectedGenreId == genreId,
          onClick = { onGenreSelected(genreId) },
          label = { Text(genre.name) },
          modifier = Modifier.semantics {
            contentDescription = "Filter recommendations by ${genre.name}"
          },
        )
      }
    }
  }
}

@Composable
private fun PersonalizedHero(
  items: List<RecommendedItem>,
  onMediaClick: (MediaItem) -> Unit,
) {
  val pagerState = rememberPagerState(pageCount = items::size)
  LaunchedEffect(pagerState, items.size) {
    if (items.size > 1) {
      while (true) {
        delay(HERO_AUTO_ADVANCE_MILLIS)
        pagerState.animateScrollToPage((pagerState.currentPage + 1) % items.size)
      }
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val heroHeight = if (maxWidth >= 600.dp) 390.dp else 330.dp
    HorizontalPager(
      state = pagerState,
      modifier = Modifier
        .fillMaxWidth()
        .height(heroHeight),
    ) { page ->
      val item = items[page]
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clickable { onMediaClick(item.media) },
      ) {
        TmdbImage(
          path = item.media.backdropPath ?: item.media.posterPath,
          contentDescription = stringResource(
            R.string.discover_backdrop_description,
            item.media.title,
          ),
          size = "w1280",
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color.Transparent,
                  MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                  MaterialTheme.colorScheme.background,
                ),
              ),
            ),
        )
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .widthIn(max = 700.dp)
            .padding(horizontal = 20.dp, vertical = 22.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = stringResource(R.string.discover_top_pick).uppercase(),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = item.media.title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = item.reasons.firstOrNull().orEmpty(),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            MatchBadge(item.matchPercentage)
            Text(
              text = stringResource(
                R.string.discover_rating_year,
                item.media.voteAverage,
                item.media.releaseDate?.take(4)
                  ?: stringResource(R.string.not_available_short),
              ),
              color = MaterialTheme.colorScheme.onBackground,
              style = MaterialTheme.typography.labelLarge,
            )
          }
          Button(onClick = { onMediaClick(item.media) }) {
            Text(stringResource(R.string.discover_view_details))
          }
        }
      }
    }
  }
}

@Composable
private fun RecommendationMediaSection(
  section: RecommendationSection,
  onMediaClick: (MediaItem) -> Unit,
  onFeedbackClick: (RecommendedItem) -> Unit,
  onSeeAll: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = section.title,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = section.subtitle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      TextButton(onClick = onSeeAll) {
        Text(stringResource(R.string.show_all))
      }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val cardWidth = if (maxWidth >= 600.dp) 190.dp else 158.dp
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(
          items = section.items,
          key = { item -> "${section.id}-${item.media.type}-${item.media.id}" },
        ) { item ->
          RecommendationPosterCard(
            item = item,
            onClick = { onMediaClick(item.media) },
            onFeedbackClick = { onFeedbackClick(item) },
            modifier = Modifier.width(cardWidth),
          )
        }
      }
    }
  }
}

@Composable
private fun RecommendationPosterCard(
  item: RecommendedItem,
  onClick: () -> Unit,
  onFeedbackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(2f / 3f),
      ) {
        TmdbImage(
          path = item.media.posterPath,
          contentDescription = stringResource(
            R.string.discover_poster_description,
            item.media.title,
          ),
          modifier = Modifier.fillMaxSize(),
        )
        MatchBadge(
          percentage = item.matchPercentage,
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp),
        )
        IconButton(
          onClick = onFeedbackClick,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(
              R.string.discover_more_actions,
              item.media.title,
            ),
          )
        }
      }
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = item.media.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          minLines = 2,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = stringResource(R.string.rating),
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.tertiary,
          )
          Text(
            text = stringResource(R.string.rating_value, item.media.voteAverage),
            modifier = Modifier.padding(start = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
          )
          Text(
            text = " · ${
              if (item.media.type == MediaType.TV_SHOW) {
                stringResource(R.string.media_type_tv)
              } else {
                item.media.releaseDate?.take(4)
                  ?: stringResource(R.string.not_available_short)
              }
            }",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
          )
        }
        Text(
          text = item.reasons.firstOrNull().orEmpty(),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
          minLines = 2,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun MatchBadge(
  percentage: Int,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = CircleShape,
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
  ) {
    Text(
      text = stringResource(R.string.discover_match, percentage),
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
      color = MaterialTheme.colorScheme.onPrimaryContainer,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun RecommendedPeopleSection(
  people: List<RecommendedPerson>,
  onPersonClick: (Int) -> Unit,
  onSeeAll: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = stringResource(R.string.discover_people_title),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = stringResource(R.string.discover_people_subtitle),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      TextButton(onClick = onSeeAll) {
        Text(stringResource(R.string.show_all))
      }
    }
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      items(
        items = people,
        key = RecommendedPerson::id,
      ) { person ->
        Column(
          modifier = Modifier
            .width(126.dp)
            .clickable { onPersonClick(person.id) },
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          TmdbImage(
            path = person.profilePath,
            contentDescription = stringResource(
              R.string.profile_image_description,
              person.name,
            ),
            size = "w185",
            modifier = Modifier
              .size(108.dp)
              .clip(CircleShape),
          )
          Text(
            text = person.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = person.reason,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendationFeedbackSheet(
  item: RecommendedItem,
  onDismiss: () -> Unit,
  onFeedback: (RecommendationFeedbackAction) -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 24.dp),
    ) {
      Text(
        text = item.media.title,
        modifier = Modifier.padding(horizontal = 24.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = stringResource(R.string.discover_why_title),
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      item.reasons.forEach { reason ->
        Text(
          text = "• $reason",
          modifier = Modifier.padding(start = 28.dp, top = 6.dp, end = 24.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      FeedbackActionRow(
        icon = Icons.Outlined.ThumbUp,
        label = stringResource(R.string.discover_more_like_this),
        onClick = { onFeedback(RecommendationFeedbackAction.MORE_LIKE_THIS) },
      )
      FeedbackActionRow(
        icon = Icons.Outlined.ThumbDown,
        label = stringResource(R.string.discover_less_like_this),
        onClick = { onFeedback(RecommendationFeedbackAction.LESS_LIKE_THIS) },
      )
      FeedbackActionRow(
        icon = Icons.Outlined.Block,
        label = stringResource(R.string.discover_not_interested),
        onClick = { onFeedback(RecommendationFeedbackAction.NOT_INTERESTED) },
      )
      FeedbackActionRow(
        icon = Icons.Outlined.CheckCircle,
        label = stringResource(R.string.discover_already_watched),
        onClick = { onFeedback(RecommendationFeedbackAction.ALREADY_WATCHED) },
      )
      FeedbackActionRow(
        icon = Icons.Outlined.HideSource,
        label = stringResource(R.string.discover_hide_title),
        onClick = { onFeedback(RecommendationFeedbackAction.HIDE) },
      )
    }
  }
}

@Composable
private fun FeedbackActionRow(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
) {
  ListItem(
    headlineContent = { Text(label) },
    leadingContent = {
      Icon(
        imageVector = icon,
        contentDescription = null,
      )
    },
    modifier = Modifier.clickable(onClick = onClick),
  )
}

@Composable
private fun RecommendationLoadingState() {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(28.dp),
  ) {
    item(key = "loading-hero") {
      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .height(330.dp)
          .background(MaterialTheme.colorScheme.surfaceContainerHighest),
      )
    }
    items(4, key = { "loading-section-$it" }) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(
          modifier = Modifier
            .padding(horizontal = 16.dp)
            .width(220.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          repeat(3) {
            Spacer(
              modifier = Modifier
                .width(148.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RecommendationEmptyState(
  onUpdatePreferences: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = Icons.Outlined.MovieFilter,
      contentDescription = null,
      modifier = Modifier.size(44.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = stringResource(R.string.discover_empty_title),
      modifier = Modifier.padding(top = 16.dp),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = stringResource(R.string.discover_empty_message),
      modifier = Modifier.padding(top = 8.dp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyMedium,
    )
    Button(
      onClick = onUpdatePreferences,
      modifier = Modifier.padding(top = 20.dp),
    ) {
      Text(stringResource(R.string.discover_update_preferences))
    }
  }
}

private const val HERO_AUTO_ADVANCE_MILLIS = 7_000L

@Preview(
  name = "Rankings compact",
  showBackground = true,
  widthDp = 412,
  heightDp = 900,
)
@Preview(
  name = "Rankings expanded dark",
  showBackground = true,
  widthDp = 900,
  heightDp = 900,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RankingListPreview() {
  FilmeraTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
      RankingList(
        rankings = (1..8).map(::previewRanking),
        onMediaClick = {},
        onScrollStateChanged = {},
      )
    }
  }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun DiscoverLoadingPreview() {
  FilmeraTheme {
    DiscoverScreen(
      state = DiscoverUiState(),
      onAction = {},
      onMediaClick = {},
      onPersonClick = {},
      onNotificationClick = {},
      onOpenSearch = {},
      onUpdatePreferences = {},
      onSeeAllRecommendations = { _ -> },
    )
  }
}

private fun previewRanking(rank: Int): RankedMediaItem =
  RankedMediaItem(
    rank = rank,
    media = MediaItem(
      id = rank,
      type = if (rank % 3 == 0) MediaType.TV_SHOW else MediaType.MOVIE,
      title = if (rank == 1) {
        "A Long Cinematic Title for the Weekly Chart"
      } else {
        "Weekly title $rank"
      },
      originalTitle = "Weekly title $rank",
      overview = "Preview overview",
      posterPath = null,
      backdropPath = null,
      releaseDate = "2026-07-30",
      voteAverage = 8.4 - (rank * 0.1),
      voteCount = 2_000,
      popularity = 100.0,
      adult = false,
      originalLanguage = "en",
      genreIds = listOf(18),
    ),
  )
