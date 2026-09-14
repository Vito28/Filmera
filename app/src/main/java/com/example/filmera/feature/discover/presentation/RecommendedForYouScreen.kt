package com.example.filmera.feature.discover.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.FilmeraTheme
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LazyLayoutKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.component.EmptyState
import com.example.filmera.core.ui.component.ErrorState
import com.example.filmera.core.ui.component.LoadingState
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.feature.discover.domain.RECOMMENDATION_ALL_SECTION_ID
import com.example.filmera.feature.discover.domain.RECOMMENDATION_PEOPLE_SECTION_ID
import com.example.filmera.feature.discover.domain.RecommendationSection
import com.example.filmera.feature.discover.domain.RecommendationSectionType
import com.example.filmera.feature.discover.domain.RecommendedContent
import com.example.filmera.feature.discover.domain.RecommendedItem
import com.example.filmera.feature.discover.domain.RecommendedPerson

private enum class RecommendedMediaFilter {
  ALL,
  MOVIES,
  SERIES,
  ANIME,
}

private enum class RecommendedSort {
  BEST_MATCH,
  HIGHEST_RATED,
  NEWEST,
}

@Composable
fun RecommendedForYouRoute(
  sectionId: String,
  onBack: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  viewModel: DiscoverViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  RecommendedForYouScreen(
    sectionId = sectionId,
    state = state,
    onAction = viewModel::onAction,
    onBack = onBack,
    onMediaClick = onMediaClick,
    onPersonClick = onPersonClick,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendedForYouScreen(
  sectionId: String,
  state: DiscoverUiState,
  onAction: (DiscoverAction) -> Unit,
  onBack: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val content = (state.contentState as? LoadState.Success)?.value
  val section = remember(content, sectionId) {
    content?.sections?.firstOrNull { it.id == sectionId }
  }
  val isPeople = sectionId == RECOMMENDATION_PEOPLE_SECTION_ID
  val title = when {
    isPeople -> stringResource(R.string.recommended_people_all_title)
    section != null -> section.title
    else -> stringResource(R.string.recommended_all_title)
  }
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      LargeTopAppBar(
        title = {
          Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = stringResource(R.string.navigate_back),
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
      LoadState.Loading -> LoadingState(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      )
      LoadState.Empty -> EmptyState(
        icon = Icons.Outlined.MovieFilter,
        title = stringResource(R.string.recommended_browse_empty_title),
        message = stringResource(R.string.recommended_browse_empty_message),
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      )
      is LoadState.Error -> ErrorState(
        error = contentState.error,
        onRetry = { onAction(DiscoverAction.RetryRequested) },
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      )
      is LoadState.Success -> {
        if (isPeople) {
          RecommendedPeopleBrowse(
            people = contentState.value.people,
            contentPadding = innerPadding,
            onPersonClick = onPersonClick,
          )
        } else {
          val destinationItems = when (sectionId) {
            RECOMMENDATION_ALL_SECTION_ID -> contentState.value.allItems
            else -> section?.items.orEmpty()
          }
          RecommendedMediaBrowse(
            title = title,
            subtitle = section?.subtitle
              ?: stringResource(R.string.recommended_all_subtitle),
            items = destinationItems,
            contentPadding = innerPadding,
            onMediaClick = onMediaClick,
          )
        }
      }
    }
  }
}

@Composable
private fun RecommendedMediaBrowse(
  title: String,
  subtitle: String,
  items: List<RecommendedItem>,
  contentPadding: PaddingValues,
  onMediaClick: (MediaItem) -> Unit,
) {
  var selectedFilterName by rememberSaveable {
    mutableStateOf(RecommendedMediaFilter.ALL.name)
  }
  var selectedSortName by rememberSaveable {
    mutableStateOf(RecommendedSort.BEST_MATCH.name)
  }
  val selectedFilter = selectedFilterName.toRecommendedMediaFilter()
  val selectedSort = selectedSortName.toRecommendedSort()
  val availableFilters = remember(items) {
    RecommendedMediaFilter.entries.filter { filter ->
      filter == RecommendedMediaFilter.ALL || items.any(filter::matches)
    }
  }
  val visibleItems = remember(items, selectedFilter, selectedSort) {
    items
      .filter(selectedFilter::matches)
      .sortedWith(selectedSort.comparator)
  }

  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 150.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 16.dp,
      top = contentPadding.calculateTopPadding() + 8.dp,
      end = 16.dp,
      bottom = contentPadding.calculateBottomPadding() + 28.dp,
    ),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item(
      key = "recommended-browse-hero",
      span = { GridItemSpan(maxLineSpan) },
    ) {
      RecommendedBrowseHero(
        title = title,
        subtitle = subtitle,
        featuredItem = items.firstOrNull(),
        itemCount = items.size,
      )
    }
    if (items.isNotEmpty()) {
      item(
        key = "recommended-browse-filters",
        span = { GridItemSpan(maxLineSpan) },
      ) {
        RecommendedBrowseControls(
          availableFilters = availableFilters,
          selectedFilter = selectedFilter,
          selectedSort = selectedSort,
          onFilterSelected = { selectedFilterName = it.name },
          onSortSelected = { selectedSortName = it.name },
        )
      }
    }
    if (visibleItems.isEmpty()) {
      item(
        key = "recommended-browse-empty",
        span = { GridItemSpan(maxLineSpan) },
      ) {
        EmptyState(
          icon = Icons.Outlined.MovieFilter,
          title = stringResource(R.string.recommended_browse_filter_empty_title),
          message = stringResource(R.string.recommended_browse_filter_empty_message),
          modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        )
      }
    } else {
      items(
        items = visibleItems,
        key = { item -> LazyLayoutKey.media("recommended-browse", item.media) },
      ) { item ->
        RecommendedBrowseCard(
          item = item,
          onClick = { onMediaClick(item.media) },
        )
      }
    }
  }
}

@Composable
private fun RecommendedBrowseHero(
  title: String,
  subtitle: String,
  featuredItem: RecommendedItem?,
  itemCount: Int,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(16f / 8.6f)
      .clip(MaterialTheme.shapes.extraLarge)
      .background(MaterialTheme.colorScheme.surfaceContainer),
  ) {
    featuredItem?.let { item ->
      TmdbImage(
        path = item.media.backdropPath ?: item.media.posterPath,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        size = "w780",
      )
    }
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.horizontalGradient(
            colorStops = arrayOf(
              0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
              0.66f to MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
              1f to MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
            ),
          ),
        ),
    )
    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .fillMaxWidth(0.82f)
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Outlined.AutoAwesome,
          contentDescription = null,
          modifier = Modifier.size(17.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = stringResource(R.string.recommended_browse_curated_label),
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = pluralStringResource(
          R.plurals.recommended_browse_title_count,
          itemCount,
          itemCount,
        ),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun RecommendedBrowseControls(
  availableFilters: List<RecommendedMediaFilter>,
  selectedFilter: RecommendedMediaFilter,
  selectedSort: RecommendedSort,
  onFilterSelected: (RecommendedMediaFilter) -> Unit,
  onSortSelected: (RecommendedSort) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(R.string.recommended_browse_type_label),
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(
        count = availableFilters.size,
        key = { index -> availableFilters[index].name },
      ) { index ->
        val filter = availableFilters[index]
        FilterChip(
          selected = selectedFilter == filter,
          onClick = { onFilterSelected(filter) },
          label = { Text(stringResource(filter.labelResource)) },
        )
      }
    }
    Text(
      text = stringResource(R.string.recommended_browse_sort_label),
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(
        count = RecommendedSort.entries.size,
        key = { index -> RecommendedSort.entries[index].name },
      ) { index ->
        val sort = RecommendedSort.entries[index]
        FilterChip(
          selected = selectedSort == sort,
          onClick = { onSortSelected(sort) },
          label = { Text(stringResource(sort.labelResource)) },
        )
      }
    }
  }
}

@Composable
private fun RecommendedBrowseCard(
  item: RecommendedItem,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(2f / 3f),
    ) {
      TmdbImage(
        path = item.media.posterPath ?: item.media.backdropPath,
        contentDescription = stringResource(
          R.string.discover_poster_description,
          item.media.title,
        ),
        modifier = Modifier.fillMaxSize(),
        size = "w342",
      )
      Surface(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ) {
        Text(
          text = stringResource(
            R.string.recommended_browse_match,
            item.matchPercentage,
          ),
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
        )
      }
    }
    Column(
      modifier = Modifier.padding(11.dp),
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
      Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = stringResource(R.string.rating),
          modifier = Modifier.size(15.dp),
          tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
          text = stringResource(R.string.rating_value, item.media.voteAverage),
          style = MaterialTheme.typography.labelMedium,
        )
        Text(
          text = "• ${item.media.releaseDate?.take(4)
            ?: stringResource(R.string.not_available_short)}",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.labelMedium,
        )
      }
      Text(
        text = item.reasons.firstOrNull()
          ?: stringResource(R.string.recommended_browse_default_reason),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun RecommendedPeopleBrowse(
  people: List<RecommendedPerson>,
  contentPadding: PaddingValues,
  onPersonClick: (Int) -> Unit,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 150.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 16.dp,
      top = contentPadding.calculateTopPadding() + 8.dp,
      end = 16.dp,
      bottom = contentPadding.calculateBottomPadding() + 28.dp,
    ),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item(
      key = "recommended-people-intro",
      span = { GridItemSpan(maxLineSpan) },
    ) {
      Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
      ) {
        Row(
          modifier = Modifier.padding(20.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ) {
            Icon(
              imageVector = Icons.Outlined.Groups,
              contentDescription = null,
              modifier = Modifier.padding(13.dp),
            )
          }
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = stringResource(R.string.recommended_people_all_title),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = stringResource(R.string.recommended_people_all_subtitle),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.bodyMedium,
            )
            Text(
              text = pluralStringResource(
                R.plurals.recommended_browse_people_count,
                people.size,
                people.size,
              ),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
    if (people.isEmpty()) {
      item(
        key = "recommended-people-empty",
        span = { GridItemSpan(maxLineSpan) },
      ) {
        EmptyState(
          title = stringResource(R.string.recommended_people_empty_title),
          message = stringResource(R.string.recommended_people_empty_message),
          modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        )
      }
    } else {
      items(
        items = people,
        key = { person -> LazyLayoutKey.identified("recommended-person", person.id) },
      ) { person ->
        RecommendedPersonBrowseCard(
          person = person,
          onClick = { onPersonClick(person.id) },
        )
      }
    }
  }
}

@Composable
private fun RecommendedPersonBrowseCard(
  person: RecommendedPerson,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    TmdbImage(
      path = person.profilePath,
      contentDescription = stringResource(
        R.string.profile_image_description,
        person.name,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(3f / 4f),
      size = "w342",
    )
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Text(
        text = person.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = person.knownForDepartment,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
      )
      person.knownFor.firstOrNull()?.let { knownFor ->
        Text(
          text = stringResource(R.string.browse_known_for, knownFor),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

private fun String.toRecommendedMediaFilter(): RecommendedMediaFilter =
  RecommendedMediaFilter.entries.firstOrNull { it.name == this }
    ?: RecommendedMediaFilter.ALL

private fun String.toRecommendedSort(): RecommendedSort =
  RecommendedSort.entries.firstOrNull { it.name == this }
    ?: RecommendedSort.BEST_MATCH

private fun RecommendedMediaFilter.matches(item: RecommendedItem): Boolean =
  when (this) {
    RecommendedMediaFilter.ALL -> true
    RecommendedMediaFilter.MOVIES -> item.media.type == MediaType.MOVIE
    RecommendedMediaFilter.SERIES ->
      item.media.type == MediaType.TV_SHOW && !item.isAnime()
    RecommendedMediaFilter.ANIME -> item.isAnime()
  }

private val RecommendedSort.comparator: Comparator<RecommendedItem>
  get() = when (this) {
    RecommendedSort.BEST_MATCH ->
      compareByDescending<RecommendedItem>(RecommendedItem::score)
        .thenByDescending { it.media.voteAverage }
    RecommendedSort.HIGHEST_RATED ->
      compareByDescending<RecommendedItem> { it.media.voteAverage }
        .thenByDescending { it.media.voteCount }
    RecommendedSort.NEWEST ->
      compareByDescending<RecommendedItem> { it.media.releaseDate.orEmpty() }
        .thenByDescending(RecommendedItem::score)
  }

private val RecommendedMediaFilter.labelResource: Int
  get() = when (this) {
    RecommendedMediaFilter.ALL -> R.string.recommended_all_filter_all
    RecommendedMediaFilter.MOVIES -> R.string.recommended_all_filter_movies
    RecommendedMediaFilter.SERIES -> R.string.recommended_all_filter_series
    RecommendedMediaFilter.ANIME -> R.string.recommended_all_filter_anime
  }

private val RecommendedSort.labelResource: Int
  get() = when (this) {
    RecommendedSort.BEST_MATCH -> R.string.recommended_browse_sort_best_match
    RecommendedSort.HIGHEST_RATED -> R.string.recommended_browse_sort_rating
    RecommendedSort.NEWEST -> R.string.recommended_browse_sort_newest
  }

private fun RecommendedItem.isAnime(): Boolean =
  media.type == MediaType.TV_SHOW &&
    16 in media.genreIds &&
    media.originalLanguage == "ja"

@Preview(showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun RecommendedBrowsePreview() {
  val sampleMedia = MediaItem(
    id = 42,
    type = MediaType.MOVIE,
    title = "The Last Horizon",
    originalTitle = "The Last Horizon",
    overview = "",
    posterPath = null,
    backdropPath = null,
    releaseDate = "2026-05-12",
    voteAverage = 8.4,
    voteCount = 6_200,
    popularity = 150.0,
    adult = false,
    originalLanguage = "en",
    genreIds = listOf(12, 878),
  )
  val sampleItem = RecommendedItem(
    media = sampleMedia,
    score = 0.94,
    matchPercentage = 94,
    reasons = listOf("Because you like science fiction"),
    originCountries = setOf("US"),
    sources = emptySet(),
  )
  val section = RecommendationSection(
    id = "top-picks",
    type = RecommendationSectionType.TOP_PICKS,
    title = "Top picks for you",
    subtitle = "Selected from your preferences and quality signals",
    items = listOf(sampleItem),
  )
  FilmeraTheme {
    RecommendedForYouScreen(
      sectionId = section.id,
      state = DiscoverUiState(
        contentState = LoadState.Success(
          RecommendedContent(
            heroItems = listOf(sampleItem),
            sections = listOf(section),
            people = emptyList(),
            availableGenres = listOf(12, 878),
          ),
        ),
      ),
      onAction = {},
      onBack = {},
      onMediaClick = {},
      onPersonClick = {},
    )
  }
}
