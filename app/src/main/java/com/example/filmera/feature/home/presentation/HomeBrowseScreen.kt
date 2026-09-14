package com.example.filmera.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalMovies
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.Recommend
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LazyLayoutKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.component.EmptyState
import com.example.filmera.core.ui.component.ErrorState
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.feature.home.domain.HomeBrowseKind
import com.example.filmera.feature.home.domain.HomeCollection
import com.example.filmera.feature.home.domain.HomeCollectionKind
import com.example.filmera.feature.home.domain.HomeGenre
import com.example.filmera.feature.home.domain.HomePerson
import com.example.filmera.feature.home.domain.HomeSpotlight
import com.example.filmera.feature.home.domain.HomeTrailer
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun HomeBrowseRoute(
  onBack: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  onPlayTrailer: (HomeTrailer) -> Unit,
  viewModel: HomeBrowseViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  HomeBrowseScreen(
    state = state,
    onAction = viewModel::onAction,
    onBack = onBack,
    onMediaClick = onMediaClick,
    onPersonClick = onPersonClick,
    onPlayTrailer = onPlayTrailer,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBrowseScreen(
  state: HomeBrowseUiState,
  onAction: (HomeBrowseAction) -> Unit,
  onBack: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  onPlayTrailer: (HomeTrailer) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      LargeTopAppBar(
        title = {
          Text(
            text = stringResource(state.kind.titleResource()),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = stringResource(R.string.browse_back),
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    when (val contentState = state.contentState) {
      LoadState.Loading -> BrowseLoading(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      )
      LoadState.Empty -> EmptyState(
        icon = Icons.Outlined.MovieFilter,
        title = stringResource(R.string.browse_empty_title),
        message = stringResource(R.string.browse_empty_message),
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      )
      is LoadState.Error -> ErrorState(
        error = contentState.error,
        onRetry = { onAction(HomeBrowseAction.Retried) },
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      )
      is LoadState.Success -> BrowseContent(
        state = state,
        content = contentState.value,
        contentPadding = innerPadding,
        onLoadMore = { onAction(HomeBrowseAction.LoadMore) },
        onMediaClick = onMediaClick,
        onPersonClick = onPersonClick,
        onPlayTrailer = onPlayTrailer,
      )
    }
  }
}

@Composable
private fun BrowseContent(
  state: HomeBrowseUiState,
  content: HomeBrowseContent,
  contentPadding: PaddingValues,
  onLoadMore: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  onPlayTrailer: (HomeTrailer) -> Unit,
) {
  when (state.kind) {
    HomeBrowseKind.TRENDING -> TrendingBrowseContent(
      items = content.mediaItems,
      state = state,
      contentPadding = contentPadding,
      onLoadMore = onLoadMore,
      onMediaClick = onMediaClick,
    )
    HomeBrowseKind.NOW_PLAYING,
    HomeBrowseKind.TOP_PICKS,
    HomeBrowseKind.UPCOMING,
    HomeBrowseKind.EDITORS_PICKS,
    HomeBrowseKind.TOP_RATED,
    HomeBrowseKind.HIDDEN_GEMS,
    HomeBrowseKind.RECOMMENDATIONS,
    -> MediaBrowseContent(
      items = content.mediaItems,
      state = state,
      contentPadding = contentPadding,
      onLoadMore = onLoadMore,
      onMediaClick = onMediaClick,
    )
    HomeBrowseKind.PEOPLE -> PeopleBrowseContent(
      kind = state.kind,
      people = content.people,
      contentPadding = contentPadding,
      onPersonClick = onPersonClick,
    )
    HomeBrowseKind.SPOTLIGHTS -> SpotlightBrowseContent(
      kind = state.kind,
      spotlights = content.spotlights,
      contentPadding = contentPadding,
      onMediaClick = onMediaClick,
    )
    HomeBrowseKind.TRAILERS -> TrailerBrowseContent(
      kind = state.kind,
      trailers = content.trailers,
      contentPadding = contentPadding,
      onPlayTrailer = onPlayTrailer,
    )
    HomeBrowseKind.GENRES -> GenreBrowseContent(
      kind = state.kind,
      genres = content.genres,
      contentPadding = contentPadding,
    )
    HomeBrowseKind.COLLECTIONS -> CollectionBrowseContent(
      kind = state.kind,
      collections = content.collections,
      contentPadding = contentPadding,
    )
  }
}

@Composable
private fun MediaBrowseContent(
  items: List<MediaItem>,
  state: HomeBrowseUiState,
  contentPadding: PaddingValues,
  onLoadMore: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val filteredItems = remember(items, selectedTab) {
    when (selectedTab) {
      1 -> items.filter { it.type == MediaType.MOVIE }
      2 -> items.filter { it.type == MediaType.TV_SHOW }
      else -> items
    }
  }
  val gridState = rememberLazyGridState()
  BrowseGridLoadMoreEffect(
    itemCount = filteredItems.size,
    canLoadMore = state.canLoadMore,
    lastVisibleIndex = {
      gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
    },
    onLoadMore = onLoadMore,
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(top = contentPadding.calculateTopPadding()),
  ) {
    BrowseDescription(state.kind)
    MediaTypeTabs(
      selectedTab = selectedTab,
      onTabSelected = { selectedTab = it },
    )
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 148.dp),
      state = gridState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = 16.dp,
        top = 16.dp,
        end = 16.dp,
        bottom = contentPadding.calculateBottomPadding() + 24.dp,
      ),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      items(
        items = filteredItems,
        key = { item -> LazyLayoutKey.media("home-browse-media", item) },
      ) { item ->
        BrowseMediaCard(
          item = item,
          onClick = { onMediaClick(item) },
        )
      }
      if (state.isLoadingMore || state.paginationError != null) {
        item(
          key = "browse-pagination",
          span = { GridItemSpan(maxLineSpan) },
        ) {
          BrowsePaginationTail(
            state = state,
            onRetry = onLoadMore,
          )
        }
      }
    }
  }
}

@Composable
private fun TrendingBrowseContent(
  items: List<MediaItem>,
  state: HomeBrowseUiState,
  contentPadding: PaddingValues,
  onLoadMore: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
) {
  val listState = rememberLazyListState()
  BrowseListLoadMoreEffect(
    itemCount = items.size,
    canLoadMore = state.canLoadMore,
    lastVisibleIndex = {
      listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
    },
    onLoadMore = onLoadMore,
  )

  LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 16.dp,
      top = contentPadding.calculateTopPadding() + 8.dp,
      end = 16.dp,
      bottom = contentPadding.calculateBottomPadding() + 24.dp,
    ),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(key = "browse-description") {
      BrowseDescription(state.kind, includeHorizontalPadding = false)
    }
    itemsIndexed(
      items = items,
      key = { _, item -> LazyLayoutKey.media("home-browse-trending", item) },
    ) { index, item ->
      TrendingBrowseRow(
        rank = index + 1,
        item = item,
        onClick = { onMediaClick(item) },
      )
    }
    if (state.isLoadingMore || state.paginationError != null) {
      item(key = "browse-pagination") {
        BrowsePaginationTail(
          state = state,
          onRetry = onLoadMore,
        )
      }
    }
  }
}

@Composable
private fun PeopleBrowseContent(
  kind: HomeBrowseKind,
  people: List<HomePerson>,
  contentPadding: PaddingValues,
  onPersonClick: (Int) -> Unit,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 132.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 16.dp,
      top = contentPadding.calculateTopPadding() + 12.dp,
      end = 16.dp,
      bottom = contentPadding.calculateBottomPadding() + 24.dp,
    ),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item(
      key = "browse-description",
      span = { GridItemSpan(maxLineSpan) },
    ) {
      BrowseDescription(
        kind = kind,
        includeHorizontalPadding = false,
      )
    }
    items(
      items = people,
      key = { person -> LazyLayoutKey.identified("home-browse-person", person.id) },
    ) { person ->
      PersonBrowseCard(
        person = person,
        onClick = { onPersonClick(person.id) },
      )
    }
  }
}

@Composable
private fun SpotlightBrowseContent(
  kind: HomeBrowseKind,
  spotlights: List<HomeSpotlight>,
  contentPadding: PaddingValues,
  onMediaClick: (MediaItem) -> Unit,
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 980.dp),
    contentPadding = PaddingValues(
      start = 16.dp,
      top = contentPadding.calculateTopPadding() + 12.dp,
      end = 16.dp,
      bottom = contentPadding.calculateBottomPadding() + 24.dp,
    ),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item(key = "browse-description") {
      BrowseDescription(
        kind = kind,
        includeHorizontalPadding = false,
      )
    }
    itemsIndexed(
      items = spotlights,
      key = { _, story -> LazyLayoutKey.of("home-browse-spotlight", story.id) },
    ) { index, story ->
      SpotlightBrowseCard(
        story = story,
        featured = index == 0,
        onClick = { onMediaClick(story.media) },
      )
    }
  }
}

@Composable
private fun TrailerBrowseContent(
  kind: HomeBrowseKind,
  trailers: List<HomeTrailer>,
  contentPadding: PaddingValues,
  onPlayTrailer: (HomeTrailer) -> Unit,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 280.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 16.dp,
      top = contentPadding.calculateTopPadding() + 12.dp,
      end = 16.dp,
      bottom = contentPadding.calculateBottomPadding() + 24.dp,
    ),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item(
      key = "browse-description",
      span = { GridItemSpan(maxLineSpan) },
    ) {
      BrowseDescription(
        kind = kind,
        includeHorizontalPadding = false,
      )
    }
    items(
      items = trailers,
      key = { trailer -> LazyLayoutKey.of("home-browse-trailer", trailer.videoKey) },
    ) { trailer ->
      TrailerBrowseCard(
        trailer = trailer,
        onClick = { onPlayTrailer(trailer) },
      )
    }
  }
}

@Composable
private fun GenreBrowseContent(
  kind: HomeBrowseKind,
  genres: List<HomeGenre>,
  contentPadding: PaddingValues,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 220.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 16.dp,
      top = contentPadding.calculateTopPadding() + 12.dp,
      end = 16.dp,
      bottom = contentPadding.calculateBottomPadding() + 24.dp,
    ),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(
      key = "browse-description",
      span = { GridItemSpan(maxLineSpan) },
    ) {
      BrowseDescription(
        kind = kind,
        includeHorizontalPadding = false,
      )
    }
    items(
      items = genres,
      key = { genre -> LazyLayoutKey.identified("home-browse-genre", genre.id) },
    ) { genre ->
      GenreBrowseCard(genre)
    }
  }
}

@Composable
private fun CollectionBrowseContent(
  kind: HomeBrowseKind,
  collections: List<HomeCollection>,
  contentPadding: PaddingValues,
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val filtered = remember(collections, selectedTab) {
    collections.filter { collection ->
      if (selectedTab == 0) {
        collection.kind == HomeCollectionKind.OFFICIAL_COLLECTION
      } else {
        collection.kind == HomeCollectionKind.CINEMATIC_UNIVERSE
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(top = contentPadding.calculateTopPadding()),
  ) {
    BrowseDescription(kind)
    PrimaryTabRow(selectedTabIndex = selectedTab) {
      listOf(
        stringResource(R.string.browse_collections),
        stringResource(R.string.browse_universes),
      ).forEachIndexed { index, label ->
        Tab(
          selected = selectedTab == index,
          onClick = { selectedTab = index },
          text = { Text(label) },
        )
      }
    }
    if (filtered.isEmpty()) {
      EmptyState(
        icon = Icons.Outlined.CollectionsBookmark,
        title = stringResource(R.string.browse_collection_category_empty_title),
        message = stringResource(R.string.browse_collection_category_empty_message),
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 980.dp),
        contentPadding = PaddingValues(
          start = 16.dp,
          top = 16.dp,
          end = 16.dp,
          bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        items(
          items = filtered,
          key = { collection ->
            LazyLayoutKey.of(
              "home-browse-collection",
              collection.kind.name,
              collection.id.toString(),
            )
          },
        ) { collection ->
          CollectionBrowseCard(collection)
        }
      }
    }
  }
}

@Composable
private fun MediaTypeTabs(
  selectedTab: Int,
  onTabSelected: (Int) -> Unit,
) {
  val tabs = listOf(
    stringResource(R.string.browse_all),
    stringResource(R.string.browse_movies),
    stringResource(R.string.browse_series),
  )
  PrimaryTabRow(selectedTabIndex = selectedTab) {
    tabs.forEachIndexed { index, title ->
      Tab(
        selected = selectedTab == index,
        onClick = { onTabSelected(index) },
        text = { Text(title) },
      )
    }
  }
}

@Composable
private fun BrowseDescription(
  kind: HomeBrowseKind,
  includeHorizontalPadding: Boolean = true,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(
        horizontal = if (includeHorizontalPadding) 16.dp else 0.dp,
        vertical = 8.dp,
      ),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ) {
        Icon(
          imageVector = kind.browseIcon(),
          contentDescription = null,
          modifier = Modifier.padding(11.dp),
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = stringResource(R.string.browse_curated_label),
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = stringResource(kind.descriptionResource()),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}

@Composable
private fun BrowseMediaCard(
  item: MediaItem,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
  ) {
    TmdbImage(
      path = item.posterPath ?: item.backdropPath,
      contentDescription = stringResource(R.string.poster_content_description, item.title),
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(2f / 3f),
      size = "w342",
    )
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = item.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      MediaMetadata(item)
    }
  }
}

@Composable
private fun TrendingBrowseRow(
  rank: Int,
  item: MediaItem,
  onClick: () -> Unit,
) {
  val rankDescription = stringResource(R.string.browse_rank_description, rank, item.title)
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clearAndSetSemantics { contentDescription = rankDescription },
    onClick = onClick,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = rank.toString().padStart(2, '0'),
        modifier = Modifier.width(48.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black,
      )
      TmdbImage(
        path = item.posterPath ?: item.backdropPath,
        contentDescription = null,
        modifier = Modifier
          .width(72.dp)
          .aspectRatio(2f / 3f)
          .clip(MaterialTheme.shapes.medium),
        size = "w185",
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        MediaMetadata(item)
      }
    }
  }
}

@Composable
private fun PersonBrowseCard(
  person: HomePerson,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    TmdbImage(
      path = person.profilePath,
      contentDescription = stringResource(R.string.profile_image_description, person.name),
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(3f / 4f),
      size = "w342",
    )
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = person.name,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      person.knownFor.firstOrNull()?.let { title ->
        Text(
          text = stringResource(R.string.browse_known_for, title),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun SpotlightBrowseCard(
  story: HomeSpotlight,
  featured: Boolean,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
  ) {
    Column {
      TmdbImage(
        path = story.media.backdropPath ?: story.media.posterPath,
        contentDescription = stringResource(
          R.string.backdrop_content_description,
          story.media.title,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(if (featured) 16f / 8.5f else 16f / 7f),
        size = "w780",
      )
      Column(
        modifier = Modifier.padding(if (featured) 20.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "${story.category} • ${
            stringResource(R.string.home_spotlight_read_time, story.readMinutes)
          }",
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = story.title,
          style = if (featured) {
            MaterialTheme.typography.headlineSmall
          } else {
            MaterialTheme.typography.titleLarge
          },
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = story.summary,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = if (featured) 4 else 3,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun TrailerBrowseCard(
  trailer: HomeTrailer,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Box {
      TmdbImage(
        path = trailer.media.backdropPath ?: trailer.media.posterPath,
        contentDescription = stringResource(
          R.string.backdrop_content_description,
          trailer.media.title,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f),
        size = "w500",
      )
      Surface(
        modifier = Modifier
          .align(Alignment.Center)
          .size(52.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
      ) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = stringResource(R.string.play_trailer),
          modifier = Modifier.padding(12.dp),
          tint = MaterialTheme.colorScheme.onPrimary,
        )
      }
    }
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Text(
        text = trailer.media.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = trailer.name,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

private fun HomeBrowseKind.browseIcon(): ImageVector =
  when (this) {
    HomeBrowseKind.NOW_PLAYING -> Icons.Outlined.LocalMovies
    HomeBrowseKind.TRENDING -> Icons.AutoMirrored.Outlined.TrendingUp
    HomeBrowseKind.TOP_PICKS -> Icons.Outlined.AutoAwesome
    HomeBrowseKind.UPCOMING -> Icons.Outlined.Event
    HomeBrowseKind.EDITORS_PICKS -> Icons.Outlined.AutoAwesome
    HomeBrowseKind.TOP_RATED -> Icons.Outlined.StarOutline
    HomeBrowseKind.HIDDEN_GEMS -> Icons.Outlined.Diamond
    HomeBrowseKind.RECOMMENDATIONS -> Icons.Outlined.Recommend
    HomeBrowseKind.GENRES -> Icons.Outlined.Category
    HomeBrowseKind.PEOPLE -> Icons.Outlined.Groups
    HomeBrowseKind.SPOTLIGHTS -> Icons.AutoMirrored.Outlined.Article
    HomeBrowseKind.TRAILERS -> Icons.Outlined.SmartDisplay
    HomeBrowseKind.COLLECTIONS -> Icons.Outlined.CollectionsBookmark
  }

@Composable
private fun GenreBrowseCard(
  genre: HomeGenre,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(16f / 8f)
      .clip(MaterialTheme.shapes.large),
  ) {
    TmdbImage(
      path = genre.backdropPath,
      contentDescription = stringResource(R.string.genre_image_description, genre.name),
      modifier = Modifier.fillMaxSize(),
      size = "w500",
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              Color.Transparent,
              MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.82f),
            ),
          ),
        ),
    )
    Text(
      text = genre.name,
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(14.dp),
      color = MaterialTheme.filmeraColors.onImage,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun CollectionBrowseCard(
  collection: HomeCollection,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 7.5f),
    ) {
      TmdbImage(
        path = collection.backdropPath ?: collection.featuredMedia.backdropPath,
        contentDescription = collection.name,
        modifier = Modifier.fillMaxSize(),
        size = "w780",
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.horizontalGradient(
              listOf(
                MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.94f),
                MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.28f),
              ),
            ),
          ),
      )
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .fillMaxWidth(0.78f)
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        Text(
          text = stringResource(
            if (collection.kind == HomeCollectionKind.OFFICIAL_COLLECTION) {
              R.string.home_collection_official
            } else {
              R.string.home_collection_universe
            },
          ),
          color = MaterialTheme.colorScheme.primaryContainer,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = collection.name,
          color = MaterialTheme.filmeraColors.onImage,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        collection.overview.takeIf(String::isNotBlank)?.let { overview ->
          Text(
            text = overview,
            color = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (collection.itemCount > 0) {
          Text(
            text = pluralStringResource(
              R.plurals.home_title_count,
              collection.itemCount,
              collection.itemCount,
            ),
            color = MaterialTheme.filmeraColors.onImage,
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun MediaMetadata(
  item: MediaItem,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.Default.Star,
      contentDescription = stringResource(R.string.rating),
      modifier = Modifier.size(14.dp),
      tint = MaterialTheme.colorScheme.tertiary,
    )
    Text(
      text = stringResource(R.string.rating_value, item.voteAverage),
      style = MaterialTheme.typography.labelMedium,
    )
    Text(
      text = "• ${
        stringResource(
          if (item.type == MediaType.MOVIE) R.string.media_type_movie
          else R.string.media_type_tv_show,
        )
      }",
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelMedium,
    )
  }
}

@Composable
private fun BrowsePaginationTail(
  state: HomeBrowseUiState,
  onRetry: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(72.dp),
    contentAlignment = Alignment.Center,
  ) {
    if (state.isLoadingMore) {
      CircularProgressIndicator(
        modifier = Modifier.size(28.dp),
        strokeWidth = 2.dp,
      )
    } else {
      state.paginationError?.let {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = stringResource(R.string.load_more_failed),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
          )
          TextButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
          }
        }
      }
    }
  }
}

@Composable
private fun BrowseLoading(
  modifier: Modifier = Modifier,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 148.dp),
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    items(
      count = 10,
      key = { index -> "browse-loading-$index" },
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Box(
          modifier = Modifier
            .fillMaxWidth(0.78f)
            .height(14.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
      }
    }
  }
}

@Composable
private fun BrowseGridLoadMoreEffect(
  itemCount: Int,
  canLoadMore: Boolean,
  lastVisibleIndex: () -> Int,
  onLoadMore: () -> Unit,
) {
  LaunchedEffect(itemCount, canLoadMore) {
    if (canLoadMore && itemCount == 0) {
      onLoadMore()
      return@LaunchedEffect
    }
    snapshotFlow(lastVisibleIndex)
      .distinctUntilChanged()
      .collect { lastVisible ->
        if (canLoadMore && itemCount > 0 && lastVisible >= itemCount - 4) {
          onLoadMore()
        }
      }
  }
}

@Composable
private fun BrowseListLoadMoreEffect(
  itemCount: Int,
  canLoadMore: Boolean,
  lastVisibleIndex: () -> Int,
  onLoadMore: () -> Unit,
) {
  LaunchedEffect(itemCount, canLoadMore) {
    snapshotFlow(lastVisibleIndex)
      .distinctUntilChanged()
      .collect { lastVisible ->
        if (canLoadMore && itemCount > 0 && lastVisible >= itemCount - 4) {
          onLoadMore()
        }
      }
  }
}

private fun HomeBrowseKind.titleResource(): Int =
  when (this) {
    HomeBrowseKind.NOW_PLAYING -> R.string.browse_now_playing_title
    HomeBrowseKind.TRENDING -> R.string.browse_trending_title
    HomeBrowseKind.TOP_PICKS -> R.string.browse_top_picks_title
    HomeBrowseKind.UPCOMING -> R.string.browse_upcoming_title
    HomeBrowseKind.EDITORS_PICKS -> R.string.browse_editors_title
    HomeBrowseKind.TOP_RATED -> R.string.browse_top_rated_title
    HomeBrowseKind.HIDDEN_GEMS -> R.string.browse_hidden_gems_title
    HomeBrowseKind.RECOMMENDATIONS -> R.string.browse_recommendations_title
    HomeBrowseKind.PEOPLE -> R.string.browse_people_title
    HomeBrowseKind.SPOTLIGHTS -> R.string.browse_spotlights_title
    HomeBrowseKind.TRAILERS -> R.string.browse_trailers_title
    HomeBrowseKind.GENRES -> R.string.browse_genres_title
    HomeBrowseKind.COLLECTIONS -> R.string.browse_collections_title
  }

private fun HomeBrowseKind.descriptionResource(): Int =
  when (this) {
    HomeBrowseKind.NOW_PLAYING -> R.string.browse_now_playing_description
    HomeBrowseKind.TRENDING -> R.string.browse_trending_description
    HomeBrowseKind.TOP_PICKS -> R.string.browse_top_picks_description
    HomeBrowseKind.UPCOMING -> R.string.browse_upcoming_description
    HomeBrowseKind.EDITORS_PICKS -> R.string.browse_editors_description
    HomeBrowseKind.TOP_RATED -> R.string.browse_top_rated_description
    HomeBrowseKind.HIDDEN_GEMS -> R.string.browse_hidden_gems_description
    HomeBrowseKind.RECOMMENDATIONS -> R.string.browse_recommendations_description
    HomeBrowseKind.PEOPLE -> R.string.browse_people_description
    HomeBrowseKind.SPOTLIGHTS -> R.string.browse_spotlights_description
    HomeBrowseKind.TRAILERS -> R.string.browse_trailers_description
    HomeBrowseKind.GENRES -> R.string.browse_genres_description
    HomeBrowseKind.COLLECTIONS -> R.string.browse_collections_description
  }
