package com.example.filmera.feature.home.presentation

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.get
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bumptech.glide.Glide
import com.example.filmera.R
import com.example.filmera.core.common.AppError
import com.example.filmera.core.designsystem.theme.FilmeraTheme
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.navigation.AppDestination
import com.example.filmera.core.navigation.BottomNavigationBar
import com.example.filmera.core.navigation.defaultBottomNavigationDestinations
import com.example.filmera.core.ui.LazyLayoutKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.component.EmptyState
import com.example.filmera.core.ui.component.ErrorState
import com.example.filmera.core.ui.component.InlineMessageBanner
import com.example.filmera.core.ui.messageResource
import com.example.filmera.feature.home.domain.AnimeTopic
import com.example.filmera.feature.home.domain.HomeBrowseKind
import com.example.filmera.feature.home.domain.HomeChannel
import com.example.filmera.feature.home.domain.HomeCollection
import com.example.filmera.feature.home.domain.HomeCollectionKind
import com.example.filmera.feature.home.domain.HomeContent
import com.example.filmera.feature.home.domain.HomeFeedKey
import com.example.filmera.feature.home.domain.HomeGenre
import com.example.filmera.feature.home.domain.HomePerson
import com.example.filmera.feature.home.domain.HomeSection
import com.example.filmera.feature.home.domain.HomeSectionType
import com.example.filmera.feature.home.domain.HomeSpotlight
import com.example.filmera.feature.home.domain.HomeSupplementalType
import com.example.filmera.feature.home.domain.HomeTrailer
import com.example.filmera.feature.home.presentation.component.CollectionsSection
import com.example.filmera.feature.home.presentation.component.CommunityPulseSection
import com.example.filmera.feature.home.presentation.component.EditorsPicksSection
import com.example.filmera.feature.home.presentation.component.GenreSection
import com.example.filmera.feature.home.presentation.component.HiddenGemsSection
import com.example.filmera.feature.home.presentation.component.HomeRatingBottomSheetContent
import com.example.filmera.feature.home.presentation.component.HomeReviewComposerSheetContent
import com.example.filmera.feature.home.presentation.component.HomeLoadingFeed
import com.example.filmera.feature.home.presentation.component.ImmersiveHeroSection
import com.example.filmera.feature.home.presentation.component.MovieSpotlightSection
import com.example.filmera.feature.home.presentation.component.NewTrailersSection
import com.example.filmera.feature.home.presentation.component.PopularPeopleSection
import com.example.filmera.feature.home.presentation.component.PopularRecommendationSection
import com.example.filmera.feature.home.presentation.component.TopRatedSection
import com.example.filmera.feature.home.presentation.component.TopPicksSection
import com.example.filmera.feature.home.presentation.component.TrendingSection
import com.example.filmera.feature.home.presentation.component.UpcomingSection
import com.example.filmera.feature.home.presentation.component.WatchlistPreviewSection
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

@Composable
fun HomeRoute(
  navController: NavHostController,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val navigateTopLevel: (AppDestination) -> Unit = { destination ->
    navController.navigate(destination.route) {
      popUpTo(AppDestination.Home.route) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  HomeScreen(
    state = state,
    initialScrollPosition = viewModel.scrollPosition(state.feedKey),
    onAction = viewModel::onAction,
    onMediaClick = onMediaClick,
    onPersonClick = onPersonClick,
    onPlayTrailer = { videoKey ->
      val trailer = (state.contentState as? LoadState.Success)
        ?.value
        ?.trailers
        ?.firstOrNull { it.videoKey == videoKey }
      trailer?.let {
        navController.navigate(
          AppDestination.Trailer.createRoute(
            mediaKey = it.media.key,
            videoKey = it.videoKey,
          ),
        )
      }
    },
    onSearchClick = { navigateTopLevel(AppDestination.Search) },
    onNotificationClick = {
      navController.navigate(AppDestination.Notifications.route) {
        launchSingleTop = true
      }
    },
    onBrowseClick = { kind ->
      navController.navigate(
        AppDestination.HomeBrowse.createRoute(
          kind = kind,
          feedKey = state.feedKey,
        ),
      ) {
        launchSingleTop = true
      }
    },
    onLibraryClick = { navigateTopLevel(AppDestination.Watchlist) },
    onCommunityClick = { navigateTopLevel(AppDestination.Community) },
    onReviewClick = { reviewId ->
      navController.navigate(AppDestination.ReviewDetail.createRoute(reviewId))
    },
    onCommunityMediaClick = { review ->
      navController.navigate(AppDestination.Detail.createRoute(review.mediaKey))
    },
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
fun HomeScreen(
  state: HomeUiState,
  onAction: (HomeAction) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  onPlayTrailer: (String) -> Unit,
  onSearchClick: () -> Unit,
  onNotificationClick: () -> Unit,
  onBrowseClick: (HomeBrowseKind) -> Unit,
  onLibraryClick: () -> Unit,
  onCommunityClick: () -> Unit = {},
  onReviewClick: (String) -> Unit = {},
  onCommunityMediaClick: (com.example.filmera.feature.community.domain.CommunityReview) -> Unit = {},
  initialScrollPosition: HomeScrollPosition = HomeScrollPosition(),
  modifier: Modifier = Modifier,
  bottomBar: @Composable () -> Unit = {},
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val favoriteErrorMessage = stringResource(R.string.favorite_update_error)
  val libraryErrorMessage = stringResource(R.string.library_write_error)
  val communityErrorMessage = stringResource(R.string.community_action_error)
  val saveableStateHolder = rememberSaveableStateHolder()
  var activeHeroPath by rememberSaveable { mutableStateOf<String?>(null) }
  var isFeedScrolled by remember { mutableStateOf(false) }
  var showRegionSheet by rememberSaveable { mutableStateOf(false) }
  var showDiscardReviewDialog by rememberSaveable { mutableStateOf(false) }
  val heroAccent = rememberHeroAccent(activeHeroPath)

  LaunchedEffect(state.favoriteWriteError) {
    if (state.favoriteWriteError != null) {
      snackbarHostState.showSnackbar(favoriteErrorMessage)
      onAction(HomeAction.FavoriteErrorDismissed)
    }
  }
  LaunchedEffect(state.libraryWriteError) {
    if (state.libraryWriteError != null) {
      snackbarHostState.showSnackbar(libraryErrorMessage)
      onAction(HomeAction.LibraryErrorDismissed)
    }
  }
  LaunchedEffect(state.communityWriteError) {
    if (state.communityWriteError != null) {
      snackbarHostState.showSnackbar(communityErrorMessage)
      onAction(HomeAction.CommunityErrorDismissed)
    }
  }
  val refreshErrorMessage = state.feedRefreshError?.let { error ->
    stringResource(error.messageResource())
  }
  LaunchedEffect(state.feedRefreshError, refreshErrorMessage) {
    if (refreshErrorMessage != null) {
      snackbarHostState.showSnackbar(refreshErrorMessage)
      onAction(HomeAction.RefreshErrorDismissed)
    }
  }

  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = bottomBar,
  ) { innerPadding ->
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val isExpanded = maxWidth >= 840.dp
      val layoutDirection = LocalLayoutDirection.current
      val horizontalInsets = PaddingValues(
        start = innerPadding.calculateStartPadding(layoutDirection),
        end = innerPadding.calculateEndPadding(layoutDirection),
      )

      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontalInsets),
      ) {
        when (val contentState = state.contentState) {
          LoadState.Loading -> HomeLoadingFeed(
            isExpanded = isExpanded,
            modifier = Modifier.padding(
              bottom = innerPadding.calculateBottomPadding(),
            ),
          )

          LoadState.Empty -> EmptyState(
            icon = Icons.Outlined.MovieFilter,
            title = stringResource(R.string.home_empty_title),
            message = stringResource(R.string.home_empty_message),
            modifier = Modifier.padding(
              top = homeChromeHeight(),
              bottom = innerPadding.calculateBottomPadding(),
            ),
          )

          is LoadState.Error -> ErrorState(
            error = contentState.error,
            onRetry = { onAction(HomeAction.Retried) },
            modifier = Modifier.padding(
              top = homeChromeHeight(),
              bottom = innerPadding.calculateBottomPadding(),
            ),
          )

          is LoadState.Success -> {
            val stateKey = contentState.value.feedKey.saveableKey()
            saveableStateHolder.SaveableStateProvider(stateKey) {
              HomeFeed(
                content = contentState.value,
                state = state,
                isExpanded = isExpanded,
                bottomPadding = innerPadding.calculateBottomPadding(),
                onAction = onAction,
                onMediaClick = onMediaClick,
                onPersonClick = onPersonClick,
                onPlayTrailer = onPlayTrailer,
                onBrowseClick = onBrowseClick,
                onLibraryClick = onLibraryClick,
                onCommunityClick = onCommunityClick,
                onReviewClick = onReviewClick,
                onCommunityMediaClick = onCommunityMediaClick,
                onActiveHeroChanged = { item ->
                  activeHeroPath = item?.backdropPath ?: item?.posterPath
                },
                onScrollStateChanged = { isFeedScrolled = it },
                initialScrollPosition = initialScrollPosition,
                onScrollPositionChanged = { position ->
                  onAction(
                    HomeAction.ScrollPositionChanged(
                      feedKey = contentState.value.feedKey,
                      position = position,
                    ),
                  )
                },
              )
            }
          }
        }

        AdaptiveHomeChrome(
          selectedChannel = state.selectedChannel,
          accentColor = heroAccent,
          isScrolled = isFeedScrolled,
          unreadNotificationCount = state.unreadNotificationCount,
          onRegionClick = { showRegionSheet = true },
          onSearchClick = onSearchClick,
          onNotificationClick = onNotificationClick,
          modifier = Modifier.align(Alignment.TopCenter),
        )

        if (state.isFeedRefreshing) {
          LinearProgressIndicator(
            modifier = Modifier
              .align(Alignment.TopCenter)
              .fillMaxWidth(),
          )
        }
      }
    }
  }

  if (showRegionSheet) {
    HomeRegionBottomSheet(
      selectedChannel = state.selectedChannel,
      selectedAnimeTopic = state.selectedAnimeTopic,
      onChannelSelected = {
        onAction(HomeAction.ChannelSelected(it))
      },
      onAnimeTopicSelected = { onAction(HomeAction.AnimeTopicSelected(it)) },
      onDismiss = { showRegionSheet = false },
    )
  }

  if (state.ratingSheet.isVisible) {
    ModalBottomSheet(
      onDismissRequest = { onAction(HomeAction.RatingDismissed) },
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
      HomeRatingBottomSheetContent(
        state = state.ratingSheet,
        onRatingSelected = { onAction(HomeAction.RatingSelected(it)) },
        onSubmit = { onAction(HomeAction.RatingSubmitted) },
        onRemove = { onAction(HomeAction.RatingRemoved) },
      )
    }
  }

  if (state.reviewComposer.isVisible) {
    ModalBottomSheet(
      onDismissRequest = {
        if (state.reviewComposer.isDirty) showDiscardReviewDialog = true
        else onAction(HomeAction.ReviewDismissed)
      },
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
      HomeReviewComposerSheetContent(
        state = state.reviewComposer,
        onRatingSelected = { onAction(HomeAction.ReviewRatingSelected(it)) },
        onHeadlineChanged = { onAction(HomeAction.ReviewHeadlineChanged(it)) },
        onBodyChanged = { onAction(HomeAction.ReviewBodyChanged(it)) },
        onSpoilerChanged = { onAction(HomeAction.ReviewSpoilerChanged(it)) },
        onPublish = { onAction(HomeAction.ReviewSubmitted) },
      )
    }
  }

  if (showDiscardReviewDialog) {
    AlertDialog(
      onDismissRequest = { showDiscardReviewDialog = false },
      title = { Text(stringResource(R.string.community_discard_review_title)) },
      text = { Text(stringResource(R.string.community_discard_review_message)) },
      confirmButton = {
        TextButton(
          onClick = {
            showDiscardReviewDialog = false
            onAction(HomeAction.ReviewDismissed)
          },
        ) {
          Text(stringResource(R.string.community_discard))
        }
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
private fun HomeFeed(
  content: HomeContent,
  state: HomeUiState,
  isExpanded: Boolean,
  bottomPadding: androidx.compose.ui.unit.Dp,
  onAction: (HomeAction) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  onPlayTrailer: (String) -> Unit,
  onBrowseClick: (HomeBrowseKind) -> Unit,
  onLibraryClick: () -> Unit,
  onCommunityClick: () -> Unit,
  onReviewClick: (String) -> Unit,
  onCommunityMediaClick: (com.example.filmera.feature.community.domain.CommunityReview) -> Unit,
  onActiveHeroChanged: (MediaItem?) -> Unit,
  onScrollStateChanged: (Boolean) -> Unit,
  initialScrollPosition: HomeScrollPosition,
  onScrollPositionChanged: (HomeScrollPosition) -> Unit,
) {
  val listState = rememberLazyListState(
    initialFirstVisibleItemIndex = initialScrollPosition.index,
    initialFirstVisibleItemScrollOffset = initialScrollPosition.offset,
  )
  LaunchedEffect(listState) {
    snapshotFlow {
      listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 56
    }
      .distinctUntilChanged()
      .collect(onScrollStateChanged)
  }
  LaunchedEffect(listState, content.feedKey) {
    snapshotFlow {
      HomeScrollPosition(
        index = listState.firstVisibleItemIndex,
        offset = listState.firstVisibleItemScrollOffset,
      )
    }
      .distinctUntilChanged()
      .collectLatest { position ->
        delay(150)
        onScrollPositionChanged(position)
      }
  }
  val visibleSectionKeys by remember(listState) {
    derivedStateOf { listState.layoutInfo.visibleItemsInfo.mapTo(mutableSetOf()) { it.key } }
  }
  val heroKey = LazyLayoutKey.of("home-section", HomeSectionType.HERO.name)
  val storyKey = "home-stories"
  val collectionKey = "home-collections"
  val regionalPresentation = state.selectedChannel.regionPresentation()
  val reviewStarter = remember(content) {
    content.section(HomeSectionType.EDITORS_PICKS)?.items?.firstOrNull()
      ?: content.section(HomeSectionType.TRENDING)?.items?.firstOrNull()
      ?: content.section(HomeSectionType.HERO)?.items?.firstOrNull()
  }

  LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = bottomPadding + 28.dp),
    verticalArrangement = Arrangement.spacedBy(32.dp),
  ) {
    item(key = heroKey) {
      ImmersiveHeroSection(
        section = content.section(HomeSectionType.HERO),
        trailers = content.trailers,
        favoriteKeys = state.favoriteKeys,
        favoriteMutations = state.favoriteMutations,
        watchlistKeys = state.watchlistKeys,
        watchlistMutations = state.watchlistMutations,
        isExpanded = isExpanded,
        isVisible = heroKey in visibleSectionKeys,
        onMediaClick = onMediaClick,
        onPlayTrailer = onPlayTrailer,
        onToggleFavorite = { onAction(HomeAction.FavoriteToggled(it)) },
        onToggleWatchlist = { onAction(HomeAction.WatchlistToggled(it)) },
        onActiveItemChanged = onActiveHeroChanged,
        onRetry = { onAction(HomeAction.RetrySection(HomeSectionType.HERO)) },
      )
    }

    item(key = "home-top-picks") {
      ConstrainedSection {
        TopPicksSection(
          content = content,
          preferredGenreIds = state.preferredGenreIds,
          onMediaClick = onMediaClick,
          onShowAll = { onBrowseClick(HomeBrowseKind.TOP_PICKS) },
        )
      }
    }

    item(key = LazyLayoutKey.of("home-section", HomeSectionType.TRENDING.name)) {
      ConstrainedSection {
        TrendingSection(
          section = content.section(HomeSectionType.TRENDING),
          onMediaClick = onMediaClick,
          onShowAll = { onBrowseClick(HomeBrowseKind.TRENDING) },
          onRetry = { onAction(HomeAction.RetrySection(HomeSectionType.TRENDING)) },
        )
      }
    }

    if (content.hasPartialFailures) {
      item(key = "partial-content-message") {
        ConstrainedSection {
          InlineMessageBanner(
            message = stringResource(R.string.partial_content_message),
          )
        }
      }
    }

    item(key = storyKey) {
      ConstrainedSection {
        MovieSpotlightSection(
          spotlights = content.spotlights,
          title = stringResource(regionalPresentation.storiesTitle),
          isVisible = storyKey in visibleSectionKeys,
          onStoryClick = { onBrowseClick(HomeBrowseKind.SPOTLIGHTS) },
          onShowAll = { onBrowseClick(HomeBrowseKind.SPOTLIGHTS) },
        )
      }
    }

    item(key = "home-community-pulse") {
      ConstrainedSection {
        CommunityPulseSection(
          state = state.communityPulseState,
          onSeeAll = onCommunityClick,
          onReviewClick = onReviewClick,
          onMediaClick = onCommunityMediaClick,
          onHelpfulClick = { onAction(HomeAction.HelpfulToggled(it)) },
          onCommentClick = onReviewClick,
          onRateClick = { onAction(HomeAction.RatingRequested(it)) },
          onReactionClick = { reviewId, reaction ->
            onAction(HomeAction.ReactionSelected(reviewId, reaction))
          },
          onWriteReview = {
            reviewStarter?.let { onAction(HomeAction.ReviewComposerRequested(it)) }
          },
          onRetry = { onAction(HomeAction.CommunityRetried) },
        )
      }
    }

    item(key = LazyLayoutKey.of("home-section", HomeSectionType.UPCOMING.name)) {
      ConstrainedSection {
        UpcomingSection(
          section = content.section(HomeSectionType.UPCOMING),
          onMediaClick = onMediaClick,
          onShowAll = { onBrowseClick(HomeBrowseKind.UPCOMING) },
          onLoadMore = { onAction(HomeAction.LoadMore(HomeSectionType.UPCOMING)) },
          onRetry = { onAction(HomeAction.RetrySection(HomeSectionType.UPCOMING)) },
        )
      }
    }

    item(key = "genres") {
      ConstrainedSection {
        GenreSection(
          genres = content.genres,
          error = content.genresError,
          isLoading = HomeSupplementalType.GENRES in content.loadingSupplementals,
          onGenreClick = { onBrowseClick(HomeBrowseKind.GENRES) },
          onShowAll = { onBrowseClick(HomeBrowseKind.GENRES) },
          onRetry = { onAction(HomeAction.Retried) },
        )
      }
    }

    item(key = "popular-people") {
      ConstrainedSection {
        PopularPeopleSection(
          people = content.people,
          error = content.peopleError,
          isLoading = HomeSupplementalType.PEOPLE in content.loadingSupplementals,
          title = stringResource(regionalPresentation.starsTitle),
          subtitle = stringResource(regionalPresentation.starsSubtitle),
          onPersonClick = onPersonClick,
          onShowAll = { onBrowseClick(HomeBrowseKind.PEOPLE) },
          onRetry = { onAction(HomeAction.Retried) },
        )
      }
    }

    item(key = "new-trailers") {
      ConstrainedSection {
        NewTrailersSection(
          trailers = content.trailers,
          error = content.trailersError,
          isLoading = HomeSupplementalType.TRAILERS in content.loadingSupplementals,
          title = stringResource(regionalPresentation.trailersTitle),
          onPlayTrailer = onPlayTrailer,
          onShowAll = { onBrowseClick(HomeBrowseKind.TRAILERS) },
          onRetry = { onAction(HomeAction.Retried) },
        )
      }
    }

    item(key = collectionKey) {
      ConstrainedSection {
        CollectionsSection(
          collections = content.collections,
          error = content.collectionsError,
          isLoading = HomeSupplementalType.COLLECTIONS in content.loadingSupplementals,
          title = stringResource(regionalPresentation.collectionsTitle),
          isVisible = collectionKey in visibleSectionKeys,
          onCollectionClick = { onBrowseClick(HomeBrowseKind.COLLECTIONS) },
          onShowAll = { onBrowseClick(HomeBrowseKind.COLLECTIONS) },
          onRetry = { onAction(HomeAction.Retried) },
        )
      }
    }

    if (state.watchlistItems.isNotEmpty()) {
      item(key = "watchlist-preview") {
        ConstrainedSection {
          WatchlistPreviewSection(
            items = state.watchlistItems,
            onMediaClick = onMediaClick,
            onViewAllClick = onLibraryClick,
          )
        }
      }
    } else {
      item(key = LazyLayoutKey.of("home-section", HomeSectionType.HIDDEN_GEMS.name)) {
        ConstrainedSection {
          HiddenGemsSection(
            section = content.section(HomeSectionType.HIDDEN_GEMS),
            onMediaClick = onMediaClick,
            onShowAll = { onBrowseClick(HomeBrowseKind.HIDDEN_GEMS) },
            onLoadMore = { onAction(HomeAction.LoadMore(HomeSectionType.HIDDEN_GEMS)) },
            onRetry = { onAction(HomeAction.RetrySection(HomeSectionType.HIDDEN_GEMS)) },
          )
        }
      }
    }
  }
}

@Composable
private fun ConstrainedSection(
  content: @Composable () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    contentAlignment = Alignment.TopCenter,
  ) {
    Box(
      modifier = Modifier
        .widthIn(max = 1120.dp)
        .fillMaxWidth(),
    ) {
      content()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdaptiveHomeChrome(
  selectedChannel: HomeChannel,
  accentColor: Color,
  isScrolled: Boolean,
  unreadNotificationCount: Int,
  onRegionClick: () -> Unit,
  onSearchClick: () -> Unit,
  onNotificationClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val targetColor = lerp(
    accentColor,
    MaterialTheme.filmeraColors.imageScrim,
    if (isScrolled) 0.7f else 0.5f,
  )
  val animatedColor by animateColorAsState(
    targetValue = targetColor,
    animationSpec = tween(450),
    label = "home chrome color",
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .shadow(if (isScrolled) 6.dp else 0.dp)
      .background(
        Brush.verticalGradient(
          colors = if (isScrolled) {
            listOf(
              animatedColor.copy(alpha = 0.99f),
              animatedColor.copy(alpha = 0.97f),
              animatedColor.copy(alpha = 0.93f),
            )
          } else {
            listOf(
              animatedColor.copy(alpha = 0.9f),
              animatedColor.copy(alpha = 0.72f),
              animatedColor.copy(alpha = 0.38f),
            )
          },
        ),
      ),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 1120.dp)
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.statusBars),
    ) {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.app_name),
            fontWeight = FontWeight.SemiBold,
          )
        },
        actions = {
          Surface(
            onClick = onRegionClick,
            modifier = Modifier.padding(end = 2.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.38f),
            contentColor = MaterialTheme.filmeraColors.onImage,
            border = BorderStroke(
              1.dp,
              MaterialTheme.filmeraColors.onImage.copy(alpha = 0.16f),
            ),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.Public,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
              )
              Text(
                text = selectedChannel.regionCode(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            }
          }
          IconButton(onClick = onSearchClick) {
            Icon(
              imageVector = Icons.Outlined.Search,
              contentDescription = stringResource(R.string.open_search),
            )
          }
          IconButton(onClick = onNotificationClick) {
            BadgedBox(
              badge = {
                if (unreadNotificationCount > 0) {
                  Badge {
                    Text(
                      text = if (unreadNotificationCount > 9) {
                        "9+"
                      } else {
                        unreadNotificationCount.toString()
                      },
                    )
                  }
                }
              },
            ) {
              Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = if (unreadNotificationCount > 0) {
                  pluralStringResource(
                    R.plurals.notification_unread_count,
                    unreadNotificationCount,
                    unreadNotificationCount,
                  )
                } else {
                  stringResource(R.string.open_notifications)
                },
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent,
          titleContentColor = MaterialTheme.filmeraColors.onImage,
          actionIconContentColor = MaterialTheme.filmeraColors.onImage,
        ),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeRegionBottomSheet(
  selectedChannel: HomeChannel,
  selectedAnimeTopic: AnimeTopic,
  onChannelSelected: (HomeChannel) -> Unit,
  onAnimeTopicSelected: (AnimeTopic) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 620.dp),
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item(key = "region-sheet-header") {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Text(
            text = stringResource(R.string.home_region_selector),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = stringResource(R.string.home_region_selector_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
      item(key = "metadata-language") {
        SelectorInfoRow(
          label = stringResource(R.string.home_metadata_language),
          value = stringResource(R.string.home_metadata_language_value),
        )
      }
      item(key = "watch-region") {
        SelectorInfoRow(
          label = stringResource(R.string.home_watch_region),
          value = selectedChannel.watchRegionCode(),
        )
      }
      item(key = "content-region-label") {
        Text(
          text = stringResource(R.string.home_content_region),
          modifier = Modifier.padding(top = 8.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
      }
      items(
        items = HomeChannel.entries,
        key = { channel -> LazyLayoutKey.of("home-region-sheet", channel.name) },
      ) { channel ->
        val selected = channel == selectedChannel
        Surface(
          onClick = { onChannelSelected(channel) },
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.large,
          color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
          } else {
            Color.Transparent
          },
          border = BorderStroke(
            1.dp,
            if (selected) {
              MaterialTheme.colorScheme.primary.copy(alpha = 0.76f)
            } else {
              MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.44f)
            },
          ),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
              Text(
                text = channel.regionCode(),
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text = stringResource(channel.labelResource()),
              modifier = Modifier.weight(1f),
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (selected) {
              Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
      if (selectedChannel.isAnime) {
        item(key = "anime-topic-selector") {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = stringResource(R.string.home_anime_focus),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(
                items = AnimeTopic.entries,
                key = { topic -> LazyLayoutKey.of("home-anime-sheet", topic.name) },
              ) { topic ->
                val selected = topic == selectedAnimeTopic
                Surface(
                  onClick = { onAnimeTopicSelected(topic) },
                  shape = MaterialTheme.shapes.extraLarge,
                  color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                  } else {
                    Color.Transparent
                  },
                  border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                  ),
                ) {
                  Text(
                    text = stringResource(topic.labelResource()),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SelectorInfoRow(
  label: String,
  value: String,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.Public,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(
          text = value,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }
  }
}

@Composable
private fun rememberHeroAccent(
  imagePath: String?,
): Color {
  val context = LocalContext.current.applicationContext
  val fallback = MaterialTheme.colorScheme.primary
  val accent by produceState(
    initialValue = fallback,
    key1 = imagePath,
    key2 = fallback,
  ) {
    val path = imagePath?.takeIf(String::isNotBlank)
    if (path == null) {
      value = fallback
      return@produceState
    }

    val target = Glide.with(context)
      .asBitmap()
      .load("https://image.tmdb.org/t/p/w342$path")
      .submit(PALETTE_BITMAP_SIZE, PALETTE_BITMAP_SIZE)
    try {
      val bitmap = withContext(Dispatchers.IO) { target.get() }
      value = withContext(Dispatchers.Default) {
        bitmap.extractCinematicAccent(fallback)
      }
    } catch (error: CancellationException) {
      target.cancel(true)
      throw error
    } catch (_: Exception) {
      value = fallback
    } finally {
      Glide.with(context).clear(target)
    }
  }
  return accent
}

private fun Bitmap.extractCinematicAccent(
  fallback: Color,
): Color {
  if (width <= 0 || height <= 0) return fallback

  data class Bucket(
    var count: Int = 0,
    var red: Long = 0,
    var green: Long = 0,
    var blue: Long = 0,
    var saturation: Long = 0,
  )

  val buckets = mutableMapOf<Int, Bucket>()
  val hsv = FloatArray(3)
  val step = max(1, min(width, height) / 24)
  for (y in 0 until height step step) {
    for (x in 0 until width step step) {
      val pixel = this[x, y]
      if (AndroidColor.alpha(pixel) < 180) continue
      val red = AndroidColor.red(pixel)
      val green = AndroidColor.green(pixel)
      val blue = AndroidColor.blue(pixel)
      val brightness = max(red, max(green, blue))
      if (brightness < 28 || brightness > 242) continue

      AndroidColor.RGBToHSV(red, green, blue, hsv)
      val key = (red shr 4 shl 8) or (green shr 4 shl 4) or (blue shr 4)
      val bucket = buckets.getOrPut(key) { Bucket() }
      bucket.count += 1
      bucket.red += red
      bucket.green += green
      bucket.blue += blue
      bucket.saturation += (hsv[1] * 100).toLong()
    }
  }
  val selected = buckets.values.maxByOrNull { bucket ->
    bucket.count * (100L + bucket.saturation / bucket.count.coerceAtLeast(1))
  } ?: return fallback
  return Color(
    red = (selected.red / selected.count).toInt(),
    green = (selected.green / selected.count).toInt(),
    blue = (selected.blue / selected.count).toInt(),
  )
}

private fun HomeChannel.labelResource(): Int =
  when (this) {
    HomeChannel.FOR_YOU -> R.string.region_for_you
    HomeChannel.INDONESIA -> R.string.region_indonesia
    HomeChannel.CHINA -> R.string.region_china
    HomeChannel.INDIA -> R.string.region_india
    HomeChannel.AMERICA -> R.string.region_america
    HomeChannel.KOREA -> R.string.region_korea
    HomeChannel.ANIME -> R.string.region_anime
  }

private fun HomeChannel.regionCode(): String = when (this) {
  HomeChannel.FOR_YOU -> "GLOBAL"
  HomeChannel.ANIME -> "ANIME"
  else -> countryCode.orEmpty()
}

private fun HomeChannel.watchRegionCode(): String = countryCode ?: "ID"

private data class RegionPresentation(
  @StringRes val starsTitle: Int,
  @StringRes val starsSubtitle: Int = R.string.home_stars_dynamic_subtitle,
  @StringRes val storiesTitle: Int,
  @StringRes val trailersTitle: Int,
  @StringRes val collectionsTitle: Int,
)

private fun HomeChannel.regionPresentation(): RegionPresentation = when (this) {
  HomeChannel.FOR_YOU -> RegionPresentation(
    starsTitle = R.string.home_stars_global,
    storiesTitle = R.string.home_stories_global,
    trailersTitle = R.string.home_trailers_global,
    collectionsTitle = R.string.home_collections_global,
  )
  HomeChannel.INDONESIA -> RegionPresentation(
    starsTitle = R.string.home_stars_indonesia,
    storiesTitle = R.string.home_stories_indonesia,
    trailersTitle = R.string.home_trailers_indonesia,
    collectionsTitle = R.string.home_collections_indonesia,
  )
  HomeChannel.KOREA -> RegionPresentation(
    starsTitle = R.string.home_stars_korea,
    storiesTitle = R.string.home_stories_korea,
    trailersTitle = R.string.home_trailers_korea,
    collectionsTitle = R.string.home_collections_korea,
  )
  HomeChannel.CHINA -> RegionPresentation(
    starsTitle = R.string.home_stars_china,
    storiesTitle = R.string.home_stories_china,
    trailersTitle = R.string.home_trailers_china,
    collectionsTitle = R.string.home_collections_china,
  )
  HomeChannel.INDIA -> RegionPresentation(
    starsTitle = R.string.home_stars_india,
    storiesTitle = R.string.home_stories_india,
    trailersTitle = R.string.home_trailers_india,
    collectionsTitle = R.string.home_collections_india,
  )
  HomeChannel.AMERICA -> RegionPresentation(
    starsTitle = R.string.home_stars_america,
    storiesTitle = R.string.home_stories_america,
    trailersTitle = R.string.home_trailers_america,
    collectionsTitle = R.string.home_collections_america,
  )
  HomeChannel.ANIME -> RegionPresentation(
    starsTitle = R.string.home_stars_anime,
    storiesTitle = R.string.home_stories_anime,
    trailersTitle = R.string.home_trailers_anime,
    collectionsTitle = R.string.home_collections_anime,
  )
}

private fun AnimeTopic.labelResource(): Int =
  when (this) {
    AnimeTopic.ALL -> R.string.anime_topic_all
    AnimeTopic.ACTION -> R.string.anime_topic_action
    AnimeTopic.ADVENTURE -> R.string.anime_topic_adventure
    AnimeTopic.FANTASY -> R.string.anime_topic_fantasy
    AnimeTopic.ROMANCE -> R.string.anime_topic_romance
    AnimeTopic.COMEDY -> R.string.anime_topic_comedy
    AnimeTopic.SPORTS -> R.string.anime_topic_sports
    AnimeTopic.SLICE_OF_LIFE -> R.string.anime_topic_slice_of_life
  }

private fun HomeFeedKey.saveableKey(): String =
  "${channel.name}:${normalizedAnimeTopic.name}"

private fun homeChromeHeight() = TOP_BAR_HEIGHT

@Preview(
  name = "Home compact dark",
  widthDp = 390,
  heightDp = 840,
  showBackground = true,
)
@Composable
private fun HomeCompactPreview() {
  FilmeraTheme {
    HomeScreen(
      state = previewHomeState(),
      onAction = {},
      onMediaClick = {},
      onPersonClick = {},
      onPlayTrailer = {},
      onSearchClick = {},
      onNotificationClick = {},
      onBrowseClick = {},
      onLibraryClick = {},
    )
  }
}

@Preview(
  name = "Home expanded dark",
  widthDp = 900,
  heightDp = 900,
  showBackground = true,
)
@Composable
private fun HomeExpandedPreview() {
  FilmeraTheme {
    HomeScreen(
      state = previewHomeState(),
      onAction = {},
      onMediaClick = {},
      onPersonClick = {},
      onPlayTrailer = {},
      onSearchClick = {},
      onNotificationClick = {},
      onBrowseClick = {},
      onLibraryClick = {},
    )
  }
}

@Preview(
  name = "Home anime",
  widthDp = 390,
  heightDp = 840,
  showBackground = true,
)
@Composable
private fun HomeAnimePreview() {
  FilmeraTheme {
    HomeScreen(
      state = previewHomeState().copy(
        selectedChannel = HomeChannel.ANIME,
        selectedAnimeTopic = AnimeTopic.FANTASY,
      ),
      onAction = {},
      onMediaClick = {},
      onPersonClick = {},
      onPlayTrailer = {},
      onSearchClick = {},
      onNotificationClick = {},
      onBrowseClick = {},
      onLibraryClick = {},
    )
  }
}

@Preview(
  name = "Home loading",
  widthDp = 390,
  heightDp = 840,
  showBackground = true,
)
@Composable
private fun HomeLoadingPreview() {
  FilmeraTheme {
    HomeScreen(
      state = HomeUiState(),
      onAction = {},
      onMediaClick = {},
      onPersonClick = {},
      onPlayTrailer = {},
      onSearchClick = {},
      onNotificationClick = {},
      onBrowseClick = {},
      onLibraryClick = {},
    )
  }
}

@Preview(
  name = "Home error",
  widthDp = 390,
  heightDp = 840,
  showBackground = true,
)
@Composable
private fun HomeErrorPreview() {
  FilmeraTheme {
    HomeScreen(
      state = HomeUiState(
        contentState = LoadState.Error(AppError.NetworkUnavailable),
      ),
      onAction = {},
      onMediaClick = {},
      onPersonClick = {},
      onPlayTrailer = {},
      onSearchClick = {},
      onNotificationClick = {},
      onBrowseClick = {},
      onLibraryClick = {},
    )
  }
}

private fun previewHomeState(): HomeUiState {
  val items = (1..20).map { index ->
    MediaItem(
      id = index,
      type = if (index % 3 == 0) MediaType.TV_SHOW else MediaType.MOVIE,
      title = if (index == 1) "Across the Quiet Sea" else "Filmera selection $index",
      originalTitle = "",
      overview = "A cinematic story of discovery, courage, and a journey across changing worlds.",
      posterPath = null,
      backdropPath = null,
      releaseDate = "2026-0${(index % 8) + 1}-1${index % 9}",
      voteAverage = 7.4 + (index / 10.0),
      voteCount = 500 + index,
      popularity = 100.0 - index,
      adult = false,
      originalLanguage = listOf("en", "ko", "zh", "id", "ja")[index % 5],
      genreIds = listOf(18, 28),
    )
  }
  val feedKey = HomeFeedKey(HomeChannel.FOR_YOU)
  val content = HomeContent(
    feedKey = feedKey,
    sections = HomeSectionType.entries.map { type ->
      HomeSection(
        type = type,
        items = if (type == HomeSectionType.TRENDING) items.take(15) else items,
      )
    },
    genres = listOf(
      HomeGenre(18, "Drama", 24, null),
      HomeGenre(16, "Animation", 20, null),
      HomeGenre(28, "Action", 18, null),
      HomeGenre(12, "Adventure", 16, null),
      HomeGenre(53, "Thriller", 12, null),
      HomeGenre(10749, "Romance", 10, null),
    ),
    people = listOf(
      HomePerson(101, "Maya Chen", null, listOf(items[0].title)),
      HomePerson(102, "Arjun Rao", null, listOf(items[1].title)),
      HomePerson(103, "Ji-eun Park", null, listOf(items[2].title)),
    ),
    trailers = listOf(
      HomeTrailer(items[0], "video-key", "Official trailer", "Trailer"),
    ),
    collections = (1..5).map { index ->
      val kind = if (index <= 3) {
        HomeCollectionKind.OFFICIAL_COLLECTION
      } else {
        HomeCollectionKind.CINEMATIC_UNIVERSE
      }
      HomeCollection(
        id = if (kind == HomeCollectionKind.OFFICIAL_COLLECTION) index else -index,
        name = if (kind == HomeCollectionKind.OFFICIAL_COLLECTION) {
          "Film Collection $index"
        } else {
          "Cinematic World $index"
        },
        posterPath = null,
        backdropPath = null,
        overview = "Explore every connected chapter in this cinematic journey.",
        itemCount = 5 + index,
        kind = kind,
        featuredMedia = items[index - 1],
      )
    },
    spotlights = items.take(3).mapIndexed { index, item ->
      HomeSpotlight(
        id = "preview-$index",
        media = item,
        category = "FILMERA SPOTLIGHT",
        title = item.title,
        summary = item.overview,
        readMinutes = 5 + index,
      )
    },
    hasPartialFailures = false,
  )
  return HomeUiState(
    contentState = LoadState.Success(content),
    selectedChannel = HomeChannel.FOR_YOU,
    unreadNotificationCount = 3,
    favoriteItems = items.take(2),
    watchlistItems = items.take(4),
    favoriteKeys = items.take(2).map(MediaItem::key).toSet(),
  )
}

private val TOP_BAR_HEIGHT = 64.dp
private const val PALETTE_BITMAP_SIZE = 48
