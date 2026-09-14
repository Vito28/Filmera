package com.example.filmera.feature.search.presentation

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filmera.R
import com.example.filmera.core.common.AppError
import com.example.filmera.core.designsystem.theme.FilmeraTheme
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LazyLayoutKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.messageResource
import com.example.filmera.core.ui.component.InlineMessageBanner
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.feature.search.domain.SearchCollection
import com.example.filmera.feature.search.domain.SearchContent
import com.example.filmera.feature.search.domain.SearchDiscovery
import com.example.filmera.feature.search.domain.SearchGenre
import com.example.filmera.feature.search.domain.SearchPerson
import com.example.filmera.feature.search.domain.SearchTopResult
import com.example.filmera.feature.search.domain.SearchType
import kotlinx.coroutines.delay

@Composable
fun SearchRoute(
  onBack: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  bottomBar: @Composable () -> Unit = {},
  viewModel: SearchViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  SearchScreen(
    state = state,
    onEvent = viewModel::onEvent,
    onBack = onBack,
    onMediaClick = { item ->
      viewModel.onEvent(SearchUiEvent.ResultOpened)
      onMediaClick(item)
    },
    onPersonClick = { personId ->
      viewModel.onEvent(SearchUiEvent.ResultOpened)
      onPersonClick(personId)
    },
    bottomBar = bottomBar,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
  state: SearchUiState,
  onEvent: (SearchUiEvent) -> Unit,
  onBack: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
  bottomBar: @Composable () -> Unit = {},
) {
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val density = LocalDensity.current
  val isImeVisible = WindowInsets.ime.getBottom(density) > 0
  val snackbarHostState = remember { SnackbarHostState() }
  val favoriteErrorMessage = stringResource(R.string.favorite_update_error)
  val libraryErrorMessage = stringResource(R.string.library_write_error)
  var hasRequestedInitialFocus by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(hasRequestedInitialFocus) {
    if (!hasRequestedInitialFocus) {
      delay(INITIAL_FOCUS_DELAY_MILLIS)
      focusRequester.requestFocus()
      keyboardController?.show()
      hasRequestedInitialFocus = true
    }
  }

  LaunchedEffect(state.favoriteWriteError) {
    if (state.favoriteWriteError != null) {
      snackbarHostState.showSnackbar(favoriteErrorMessage)
      onEvent(SearchUiEvent.FavoriteErrorDismissed)
    }
  }
  LaunchedEffect(state.libraryWriteError) {
    if (state.libraryWriteError != null) {
      snackbarHostState.showSnackbar(libraryErrorMessage)
      onEvent(SearchUiEvent.LibraryErrorDismissed)
    }
  }

  BackHandler(enabled = isImeVisible) {
    keyboardController?.hide()
    focusManager.clearFocus()
  }
  BackHandler(enabled = !isImeVisible) {
    onBack()
  }

  if (state.showClearRecentConfirmation) {
    ClearRecentSearchesSheet(
      onConfirm = { onEvent(SearchUiEvent.ClearRecentSearchesConfirmed) },
      onDismiss = { onEvent(SearchUiEvent.ClearRecentSearchesDismissed) },
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = bottomBar,
    topBar = {
      SearchTopArea(
        state = state,
        focusRequester = focusRequester,
        onEvent = onEvent,
        onBack = {
          keyboardController?.hide()
          focusManager.clearFocus()
          onBack()
        },
        onKeyboardSearch = {
          onEvent(SearchUiEvent.SearchSubmitted)
          keyboardController?.hide()
          focusManager.clearFocus()
        },
        onClear = {
          onEvent(SearchUiEvent.ClearQuery)
          focusRequester.requestFocus()
          keyboardController?.show()
        },
      )
    },
  ) { contentPadding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
        .imePadding(),
    ) {
      val isExpanded = maxWidth >= 840.dp
      SearchContentArea(
        state = state,
        isExpanded = isExpanded,
        onEvent = onEvent,
        onMediaClick = {
          keyboardController?.hide()
          focusManager.clearFocus()
          onMediaClick(it)
        },
        onPersonClick = {
          keyboardController?.hide()
          focusManager.clearFocus()
          onPersonClick(it)
        },
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxHeight()
          .widthIn(max = 1040.dp),
      )
    }
  }
}

@Composable
private fun SearchTopArea(
  state: SearchUiState,
  focusRequester: FocusRequester,
  onEvent: (SearchUiEvent) -> Unit,
  onBack: () -> Unit,
  onKeyboardSearch: () -> Unit,
  onClear: () -> Unit,
) {
  val searchFieldDescription = stringResource(R.string.search_field_description)
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 0.dp,
  ) {
    Column(modifier = Modifier.statusBarsPadding()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 4.dp, end = 12.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.navigate_back),
          )
        }
        TextField(
          value = state.query,
          onValueChange = { onEvent(SearchUiEvent.QueryChanged(it)) },
          modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .focusRequester(focusRequester)
            .semantics {
              contentDescription = searchFieldDescription
            },
          singleLine = true,
          placeholder = { Text(stringResource(R.string.search_placeholder)) },
          leadingIcon = {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
          },
          trailingIcon = {
            if (state.query.isNotEmpty()) {
              IconButton(onClick = onClear) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = stringResource(R.string.clear_search),
                )
              }
            }
          },
          shape = RoundedCornerShape(18.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { onKeyboardSearch() }),
          colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
        )
      }
      SearchTypeChips(
        selectedType = state.selectedType,
        onTypeSelected = { onEvent(SearchUiEvent.TypeSelected(it)) },
      )
      if (state.isSearching && state.visibleContent != null) {
        LinearProgressIndicator(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp),
        )
      } else {
        Spacer(modifier = Modifier.height(2.dp))
      }
    }
  }
}

@Composable
private fun SearchTypeChips(
  selectedType: SearchType,
  onTypeSelected: (SearchType) -> Unit,
) {
  LazyRow(
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    items(
      items = SearchType.entries,
      key = { type -> LazyLayoutKey.of("search-type", type.name) },
    ) { type ->
      FilterChip(
        selected = type == selectedType,
        onClick = { onTypeSelected(type) },
        label = {
          Text(
            text = stringResource(type.labelResource()),
            maxLines = 1,
          )
        },
      )
    }
  }
}

@Composable
private fun SearchContentArea(
  state: SearchUiState,
  isExpanded: Boolean,
  onEvent: (SearchUiEvent) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val pane = when {
    state.query.isBlank() -> SearchPane.DISCOVERY
    state.resultsState is SearchResultsState.Loading && state.visibleContent == null ->
      SearchPane.LOADING
    state.resultsState is SearchResultsState.Empty -> SearchPane.EMPTY
    state.resultsState is SearchResultsState.Error && state.visibleContent == null ->
      SearchPane.ERROR
    else -> SearchPane.RESULTS
  }

  Box(modifier = modifier) {
    when (pane) {
      SearchPane.DISCOVERY -> SearchDiscoveryContent(
        state = state,
        isExpanded = isExpanded,
        onEvent = onEvent,
        onPersonClick = onPersonClick,
      )
      SearchPane.LOADING -> SearchLoadingContent(
        suggestions = state.suggestions,
        query = state.query,
        onSuggestionSelected = {
          onEvent(SearchUiEvent.SuggestionSelected(it))
        },
      )
      SearchPane.EMPTY -> SearchEmptyContent(
        query = (state.resultsState as? SearchResultsState.Empty)?.query ?: state.query,
        suggestion = state.recentSearches.firstOrNull()
          ?: (state.discoveryState as? LoadState.Success)
            ?.value
            ?.trendingQueries
            ?.firstOrNull(),
        onClear = { onEvent(SearchUiEvent.ClearQuery) },
        onSuggestionSelected = {
          onEvent(
            SearchUiEvent.SuggestionSelected(
              SearchSuggestion(it, SearchSuggestionType.TITLE),
            ),
          )
        },
      )
      SearchPane.ERROR -> SearchErrorContent(
        error = (state.resultsState as? SearchResultsState.Error)?.error
          ?: AppError.Unknown,
        recentSearches = state.recentSearches,
        onRetry = { onEvent(SearchUiEvent.Retry) },
        onRecentSelected = { onEvent(SearchUiEvent.RecentSearchSelected(it)) },
      )
      SearchPane.RESULTS -> {
        val content = state.visibleContent
        if (content != null) {
          SearchResultContent(
            state = state,
            content = content,
            onEvent = onEvent,
            onMediaClick = onMediaClick,
            onPersonClick = onPersonClick,
          )
        } else {
          SearchLoadingContent(
            suggestions = state.suggestions,
            query = state.query,
            onSuggestionSelected = {
              onEvent(SearchUiEvent.SuggestionSelected(it))
            },
          )
        }
      }
    }
  }
}

@Composable
private fun SearchDiscoveryContent(
  state: SearchUiState,
  isExpanded: Boolean,
  onEvent: (SearchUiEvent) -> Unit,
  onPersonClick: (Int) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (state.recentSearches.isNotEmpty()) {
      item(key = "recent-searches") {
        RecentSearchSection(
          recentSearches = state.recentSearches,
          onSelect = { onEvent(SearchUiEvent.RecentSearchSelected(it)) },
          onRemove = { onEvent(SearchUiEvent.RemoveRecentSearch(it)) },
          onClearAll = { onEvent(SearchUiEvent.ClearRecentSearchesRequested) },
        )
      }
    }

    when (val discoveryState = state.discoveryState) {
      LoadState.Loading -> item(key = "discovery-loading") {
        DiscoverySkeleton()
      }
      LoadState.Empty -> item(key = "discovery-empty") {
        InlineStateCard(
          icon = Icons.Outlined.Search,
          title = stringResource(R.string.search_discovery_empty_title),
          message = stringResource(R.string.search_discovery_empty_message),
        )
      }
      is LoadState.Error -> item(key = "discovery-error") {
        InlineErrorCard(
          error = discoveryState.error,
          onRetry = { onEvent(SearchUiEvent.Retry) },
        )
      }
      is LoadState.Success -> {
        val discovery = discoveryState.value
        if (discovery.hasPartialFailures) {
          item(key = "discovery-partial") {
            InlineMessageBanner(
              message = stringResource(R.string.search_discovery_partial),
            )
          }
        }
        if (discovery.trendingQueries.isNotEmpty()) {
          item(key = "trending-searches") {
            TrendingSearchSection(
              queries = discovery.trendingQueries,
              onSelect = {
                onEvent(
                  SearchUiEvent.SuggestionSelected(
                    SearchSuggestion(it, SearchSuggestionType.TITLE),
                  ),
                )
              },
            )
          }
        }
        if (discovery.genres.isNotEmpty()) {
          item(key = "genre-searches") {
            GenreSearchSection(
              genres = discovery.genres,
              isExpanded = isExpanded,
              onSelect = {
                onEvent(
                  SearchUiEvent.SuggestionSelected(
                    SearchSuggestion(it, SearchSuggestionType.GENRE),
                  ),
                )
              },
            )
          }
        }
        if (discovery.popularPeople.isNotEmpty()) {
          item(key = "popular-people") {
            PopularPeopleSearchSection(
              people = discovery.popularPeople,
              onSelect = { onPersonClick(it.id) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RecentSearchSection(
  recentSearches: List<String>,
  onSelect: (String) -> Unit,
  onRemove: (String) -> Unit,
  onClearAll: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    SectionHeading(
      title = stringResource(R.string.search_recent_title),
      actionLabel = stringResource(R.string.clear_all),
      onAction = onClearAll,
    )
    recentSearches.take(MAX_DISCOVERY_RECENT_SEARCHES).forEach { query ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(12.dp))
          .clickable(role = Role.Button) { onSelect(query) }
          .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Outlined.History,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = query,
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 12.dp),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodyLarge,
        )
        IconButton(onClick = { onRemove(query) }) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.remove_recent_search, query),
          )
        }
      }
    }
  }
}

@Composable
private fun TrendingSearchSection(
  queries: List<String>,
  onSelect: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    SectionHeading(title = stringResource(R.string.search_trending_title))
    queries.take(MAX_DISCOVERY_TRENDING_SEARCHES).forEachIndexed { index, query ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(12.dp))
          .clickable(role = Role.Button) { onSelect(query) }
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = (index + 1).toString().padStart(2, '0'),
          modifier = Modifier.width(36.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = query,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
        )
      }
    }
  }
}

@Composable
private fun GenreSearchSection(
  genres: List<SearchGenre>,
  isExpanded: Boolean,
  onSelect: (String) -> Unit,
) {
  val columns = if (isExpanded) 3 else 2
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    SectionHeading(title = stringResource(R.string.search_browse_genre))
    genres.chunked(columns).forEach { rowGenres ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        rowGenres.forEach { genre ->
          GenreSearchCard(
            genre = genre,
            onClick = { onSelect(genre.name) },
            modifier = Modifier.weight(1f),
          )
        }
        repeat(columns - rowGenres.size) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun GenreSearchCard(
  genre: SearchGenre,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier.height(88.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(start = 4.dp, end = 8.dp),
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = genre.name,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = pluralStringResource(
            R.plurals.search_genre_titles,
            genre.titleCount,
            genre.titleCount,
          ),
          modifier = Modifier.padding(top = 2.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodySmall,
        )
      }
      TmdbImage(
        path = genre.backdropPath,
        contentDescription = null,
        modifier = Modifier
          .size(72.dp)
          .clip(RoundedCornerShape(12.dp)),
        size = "w342",
      )
    }
  }
}

@Composable
private fun PopularPeopleSearchSection(
  people: List<SearchPerson>,
  onSelect: (SearchPerson) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    SectionHeading(title = stringResource(R.string.search_popular_people))
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(end = 4.dp),
    ) {
      items(
        items = people,
        key = { person -> LazyLayoutKey.identified("search-popular-person", person.id) },
      ) { person ->
        Column(
          modifier = Modifier
            .width(92.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button) { onSelect(person) }
            .padding(4.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          TmdbImage(
            path = person.profilePath,
            contentDescription = stringResource(R.string.profile_image_description, person.name),
            modifier = Modifier
              .size(76.dp)
              .clip(CircleShape),
          )
          Text(
            text = person.name,
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
          )
          if (person.knownForDepartment.isNotBlank()) {
            Text(
              text = person.knownForDepartment,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SearchLoadingContent(
  suggestions: List<SearchSuggestion>,
  query: String,
  onSuggestionSelected: (SearchSuggestion) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (suggestions.isNotEmpty()) {
      item(key = "loading-suggestions") {
        SearchSuggestionSection(
          suggestions = suggestions,
          query = query,
          onSelect = onSuggestionSelected,
        )
      }
    }
    items(count = 5, key = { index -> LazyLayoutKey.indexed("search-skeleton", index) }) {
      SearchResultSkeleton()
    }
  }
}

@Composable
private fun SearchResultContent(
  state: SearchUiState,
  content: SearchContent,
  onEvent: (SearchUiEvent) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (state.suggestions.isNotEmpty()) {
      item(key = "suggestions") {
        SearchSuggestionSection(
          suggestions = state.suggestions,
          query = state.query,
          onSelect = { onEvent(SearchUiEvent.SuggestionSelected(it)) },
        )
      }
    }
    if (state.resultsState is SearchResultsState.Error) {
      item(key = "preserved-result-error") {
        InlineErrorCard(
          error = state.resultsState.error,
          onRetry = { onEvent(SearchUiEvent.Retry) },
        )
      }
    }
    if (content.hasPartialFailures) {
      item(key = "partial-results") {
        InlineMessageBanner(
          message = stringResource(R.string.search_partial_results),
        )
      }
    }
    content.topResult?.let { topResult ->
      item(key = topResult.lazyLayoutKey) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          SectionHeading(title = stringResource(R.string.search_top_result))
          TopResultCard(
            result = topResult,
            favoriteKeys = state.favoriteKeys,
            favoriteMutations = state.favoriteMutations,
            watchlistKeys = state.watchlistKeys,
            watchlistMutations = state.watchlistMutations,
            onMediaClick = onMediaClick,
            onPersonClick = onPersonClick,
            onToggleFavorite = { onEvent(SearchUiEvent.FavoriteToggled(it)) },
            onToggleWatchlist = { onEvent(SearchUiEvent.WatchlistToggled(it)) },
          )
        }
      }
    }

    when (content.selectedType) {
      SearchType.ALL -> {
        mediaResultSection(
          sectionKey = "movies",
          titleResource = R.string.search_movies,
          items = content.movies.take(4),
          favoriteKeys = state.favoriteKeys,
          favoriteMutations = state.favoriteMutations,
          watchlistKeys = state.watchlistKeys,
          watchlistMutations = state.watchlistMutations,
          onSeeAll = { onEvent(SearchUiEvent.TypeSelected(SearchType.MOVIES)) },
          onMediaClick = onMediaClick,
          onToggleFavorite = { onEvent(SearchUiEvent.FavoriteToggled(it)) },
          onToggleWatchlist = { onEvent(SearchUiEvent.WatchlistToggled(it)) },
        )
        mediaResultSection(
          sectionKey = "tv-shows",
          titleResource = R.string.search_tv_shows,
          items = content.tvShows.take(3),
          favoriteKeys = state.favoriteKeys,
          favoriteMutations = state.favoriteMutations,
          watchlistKeys = state.watchlistKeys,
          watchlistMutations = state.watchlistMutations,
          onSeeAll = { onEvent(SearchUiEvent.TypeSelected(SearchType.TV_SHOWS)) },
          onMediaClick = onMediaClick,
          onToggleFavorite = { onEvent(SearchUiEvent.FavoriteToggled(it)) },
          onToggleWatchlist = { onEvent(SearchUiEvent.WatchlistToggled(it)) },
        )
        peopleResultSection(
          people = content.people.take(3),
          onSeeAll = { onEvent(SearchUiEvent.TypeSelected(SearchType.PEOPLE)) },
          onPersonClick = onPersonClick,
        )
        collectionResultSection(
          collections = content.collections.take(3),
          onSeeAll = { onEvent(SearchUiEvent.TypeSelected(SearchType.COLLECTIONS)) },
        )
      }
      SearchType.MOVIES -> mediaResultSection(
        sectionKey = "movies",
        titleResource = R.string.search_movies,
        items = content.movies,
        favoriteKeys = state.favoriteKeys,
        favoriteMutations = state.favoriteMutations,
        watchlistKeys = state.watchlistKeys,
        watchlistMutations = state.watchlistMutations,
        onMediaClick = onMediaClick,
        onToggleFavorite = { onEvent(SearchUiEvent.FavoriteToggled(it)) },
        onToggleWatchlist = { onEvent(SearchUiEvent.WatchlistToggled(it)) },
      )
      SearchType.TV_SHOWS -> mediaResultSection(
        sectionKey = "tv-shows",
        titleResource = R.string.search_tv_shows,
        items = content.tvShows,
        favoriteKeys = state.favoriteKeys,
        favoriteMutations = state.favoriteMutations,
        watchlistKeys = state.watchlistKeys,
        watchlistMutations = state.watchlistMutations,
        onMediaClick = onMediaClick,
        onToggleFavorite = { onEvent(SearchUiEvent.FavoriteToggled(it)) },
        onToggleWatchlist = { onEvent(SearchUiEvent.WatchlistToggled(it)) },
      )
      SearchType.PEOPLE -> peopleResultSection(
        people = content.people,
        onPersonClick = onPersonClick,
      )
      SearchType.COLLECTIONS -> collectionResultSection(
        collections = content.collections,
      )
    }

    if (state.paginationError != null) {
      item(key = "pagination-error") {
        InlineErrorCard(
          error = state.paginationError,
          onRetry = { onEvent(SearchUiEvent.LoadMore) },
        )
      }
    }
    if (content.canLoadMore || state.isLoadingMore) {
      item(key = "load-more") {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          if (state.isLoadingMore) {
            LinearProgressIndicator(modifier = Modifier.widthIn(max = 240.dp))
          } else {
            OutlinedButton(onClick = { onEvent(SearchUiEvent.LoadMore) }) {
              Text(stringResource(R.string.load_more))
            }
          }
        }
      }
    }
  }
}

private fun androidx.compose.foundation.lazy.LazyListScope.mediaResultSection(
  sectionKey: String,
  @androidx.annotation.StringRes titleResource: Int,
  items: List<MediaItem>,
  favoriteKeys: Set<com.example.filmera.core.model.MediaKey>,
  favoriteMutations: Set<com.example.filmera.core.model.MediaKey>,
  watchlistKeys: Set<com.example.filmera.core.model.MediaKey>,
  watchlistMutations: Set<com.example.filmera.core.model.MediaKey>,
  onSeeAll: (() -> Unit)? = null,
  onMediaClick: (MediaItem) -> Unit,
  onToggleFavorite: (MediaItem) -> Unit,
  onToggleWatchlist: (MediaItem) -> Unit,
) {
  if (items.isEmpty()) return
  item(key = LazyLayoutKey.of("search-heading", sectionKey)) {
    SectionHeading(
      title = stringResource(titleResource),
      actionLabel = onSeeAll?.let { stringResource(R.string.view_all) },
      onAction = onSeeAll,
    )
  }
  items(
    items = items,
    key = { LazyLayoutKey.media("search-media-result", it) },
  ) { item ->
    MediaSearchResultRow(
      item = item,
      isFavorite = item.key in favoriteKeys,
      isFavoriteMutationInProgress = item.key in favoriteMutations,
      isWatchlisted = item.key in watchlistKeys,
      isWatchlistMutationInProgress = item.key in watchlistMutations,
      onClick = { onMediaClick(item) },
      onToggleFavorite = { onToggleFavorite(item) },
      onToggleWatchlist = { onToggleWatchlist(item) },
    )
  }
}

private val SearchTopResult.lazyLayoutKey: String
  get() = when (this) {
    is SearchTopResult.Media -> LazyLayoutKey.media("search-top-result", item)
    is SearchTopResult.Person ->
      LazyLayoutKey.identified("search-top-result-person", person.id)
    is SearchTopResult.Collection ->
      LazyLayoutKey.identified("search-top-result-collection", collection.id)
  }

private fun androidx.compose.foundation.lazy.LazyListScope.peopleResultSection(
  people: List<SearchPerson>,
  onSeeAll: (() -> Unit)? = null,
  onPersonClick: (Int) -> Unit,
) {
  if (people.isEmpty()) return
  item(key = "heading-people") {
    SectionHeading(
      title = stringResource(R.string.search_people),
      actionLabel = onSeeAll?.let { stringResource(R.string.view_all) },
      onAction = onSeeAll,
    )
  }
  items(
    items = people,
    key = { LazyLayoutKey.identified("search-person-result", it.id) },
  ) { person ->
    PersonSearchResultRow(
      person = person,
      onClick = { onPersonClick(person.id) },
    )
  }
}

private fun androidx.compose.foundation.lazy.LazyListScope.collectionResultSection(
  collections: List<SearchCollection>,
  onSeeAll: (() -> Unit)? = null,
) {
  if (collections.isEmpty()) return
  item(key = "heading-collections") {
    SectionHeading(
      title = stringResource(R.string.search_collections),
      actionLabel = onSeeAll?.let { stringResource(R.string.view_all) },
      onAction = onSeeAll,
    )
  }
  items(
    items = collections,
    key = { LazyLayoutKey.identified("search-collection-result", it.id) },
  ) { collection ->
    CollectionSearchResultRow(collection)
  }
}

@Composable
private fun SearchSuggestionSection(
  suggestions: List<SearchSuggestion>,
  query: String,
  onSelect: (SearchSuggestion) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    SectionHeading(title = stringResource(R.string.search_suggestions))
    suggestions.forEach { suggestion ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(12.dp))
          .clickable(role = Role.Button) { onSelect(suggestion) }
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = suggestion.type.icon(),
          contentDescription = null,
          modifier = Modifier.size(20.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = highlightedText(suggestion.text, query),
          modifier = Modifier.padding(start = 12.dp),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodyLarge,
        )
      }
    }
  }
}

@Composable
private fun TopResultCard(
  result: SearchTopResult,
  favoriteKeys: Set<com.example.filmera.core.model.MediaKey>,
  favoriteMutations: Set<com.example.filmera.core.model.MediaKey>,
  watchlistKeys: Set<com.example.filmera.core.model.MediaKey>,
  watchlistMutations: Set<com.example.filmera.core.model.MediaKey>,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  onToggleFavorite: (MediaItem) -> Unit,
  onToggleWatchlist: (MediaItem) -> Unit,
) {
  ElevatedCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
  ) {
    when (result) {
      is SearchTopResult.Media -> MediaTopResult(
        item = result.item,
        isFavorite = result.item.key in favoriteKeys,
        isFavoriteMutationInProgress = result.item.key in favoriteMutations,
        isWatchlisted = result.item.key in watchlistKeys,
        isWatchlistMutationInProgress = result.item.key in watchlistMutations,
        onClick = { onMediaClick(result.item) },
        onToggleFavorite = { onToggleFavorite(result.item) },
        onToggleWatchlist = { onToggleWatchlist(result.item) },
      )
      is SearchTopResult.Person -> PersonTopResult(
        person = result.person,
        onClick = { onPersonClick(result.person.id) },
      )
      is SearchTopResult.Collection -> CollectionTopResult(result.collection)
    }
  }
}

@Composable
private fun MediaTopResult(
  item: MediaItem,
  isFavorite: Boolean,
  isFavoriteMutationInProgress: Boolean,
  isWatchlisted: Boolean,
  isWatchlistMutationInProgress: Boolean,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onToggleWatchlist: () -> Unit,
) {
  Row(modifier = Modifier.padding(14.dp)) {
    TmdbImage(
      path = item.posterPath,
      contentDescription = stringResource(R.string.poster_content_description, item.title),
      modifier = Modifier
        .width(96.dp)
        .aspectRatio(2f / 3f)
        .clip(RoundedCornerShape(14.dp)),
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = 16.dp),
    ) {
      Text(
        text = item.title,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      MediaMetadata(item)
      Text(
        text = item.overview.ifBlank { stringResource(R.string.overview_unavailable) },
        modifier = Modifier.padding(top = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium,
      )
      Row(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(onClick = onClick) {
          Text(stringResource(R.string.view_details))
        }
        IconButton(
          onClick = onToggleWatchlist,
          enabled = !isWatchlistMutationInProgress,
        ) {
          Icon(
            imageVector = if (isWatchlisted) {
              Icons.Default.Bookmark
            } else {
              Icons.Outlined.BookmarkBorder
            },
            contentDescription = stringResource(
              if (isWatchlisted) R.string.remove_from_watchlist else R.string.add_to_watchlist,
            ),
          )
        }
        IconButton(
          onClick = onToggleFavorite,
          enabled = !isFavoriteMutationInProgress,
        ) {
          Icon(
            imageVector = if (isFavorite) {
              Icons.Default.Favorite
            } else {
              Icons.Outlined.FavoriteBorder
            },
            contentDescription = stringResource(
              if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites,
            ),
            tint = if (isFavorite) MaterialTheme.colorScheme.primary else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          )
        }
      }
    }
  }
}

@Composable
private fun PersonTopResult(
  person: SearchPerson,
  onClick: () -> Unit,
) {
  Row(modifier = Modifier.padding(14.dp)) {
    TmdbImage(
      path = person.profilePath,
      contentDescription = stringResource(R.string.profile_image_description, person.name),
      modifier = Modifier
        .size(width = 96.dp, height = 128.dp)
        .clip(RoundedCornerShape(18.dp)),
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = 16.dp),
    ) {
      Text(
        text = person.name,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      if (person.knownForDepartment.isNotBlank()) {
        Text(
          text = person.knownForDepartment,
          modifier = Modifier.padding(top = 4.dp),
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.labelLarge,
        )
      }
      if (person.knownFor.isNotEmpty()) {
        Text(
          text = stringResource(R.string.search_known_for, person.knownFor.joinToString()),
          modifier = Modifier.padding(top = 8.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      Button(
        onClick = onClick,
        modifier = Modifier.padding(top = 12.dp),
      ) {
        Text(stringResource(R.string.view_details))
      }
    }
  }
}

@Composable
private fun CollectionTopResult(collection: SearchCollection) {
  Row(modifier = Modifier.padding(14.dp)) {
    TmdbImage(
      path = collection.posterPath ?: collection.backdropPath,
      contentDescription = null,
      modifier = Modifier
        .size(width = 96.dp, height = 128.dp)
        .clip(RoundedCornerShape(14.dp)),
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = 16.dp),
    ) {
      Text(
        text = collection.name,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = stringResource(R.string.search_collection_label),
        modifier = Modifier.padding(top = 4.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
      )
      Text(
        text = collection.overview.ifBlank { stringResource(R.string.overview_unavailable) },
        modifier = Modifier.padding(top = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

@Composable
private fun MediaSearchResultRow(
  item: MediaItem,
  isFavorite: Boolean,
  isFavoriteMutationInProgress: Boolean,
  isWatchlisted: Boolean,
  isWatchlistMutationInProgress: Boolean,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onToggleWatchlist: () -> Unit,
) {
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Row(modifier = Modifier.padding(10.dp)) {
      TmdbImage(
        path = item.posterPath,
        contentDescription = stringResource(R.string.poster_content_description, item.title),
        modifier = Modifier
          .size(width = 82.dp, height = 123.dp)
          .clip(RoundedCornerShape(12.dp)),
      )
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(start = 12.dp),
      ) {
        Row(verticalAlignment = Alignment.Top) {
          Text(
            text = item.title,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
          IconButton(
            onClick = onToggleWatchlist,
            enabled = !isWatchlistMutationInProgress,
            modifier = Modifier.size(48.dp),
          ) {
            Icon(
              imageVector = if (isWatchlisted) {
                Icons.Default.Bookmark
              } else {
                Icons.Outlined.BookmarkBorder
              },
              contentDescription = stringResource(
                if (isWatchlisted) R.string.remove_from_watchlist else R.string.add_to_watchlist,
              ),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = onToggleFavorite,
            enabled = !isFavoriteMutationInProgress,
            modifier = Modifier.size(48.dp),
          ) {
            Icon(
              imageVector = if (isFavorite) {
                Icons.Default.Favorite
              } else {
                Icons.Outlined.FavoriteBorder
              },
              contentDescription = stringResource(
                if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites,
              ),
              tint = if (isFavorite) MaterialTheme.colorScheme.primary else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            )
          }
        }
        MediaMetadata(item)
        Text(
          text = item.overview.ifBlank { stringResource(R.string.overview_unavailable) },
          modifier = Modifier.padding(top = 6.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }
  }
}

@Composable
private fun MediaMetadata(item: MediaItem) {
  Row(
    modifier = Modifier.padding(top = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(
        if (item.type == MediaType.MOVIE) R.string.media_type_movie
        else R.string.media_type_tv_show,
      ),
      color = MaterialTheme.colorScheme.primary,
      style = MaterialTheme.typography.labelMedium,
    )
    Text(
      text = " · ${item.releaseDate?.take(4) ?: stringResource(R.string.not_available_short)} · ",
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodySmall,
    )
    Icon(
      imageVector = Icons.Default.Star,
      contentDescription = null,
      modifier = Modifier.size(14.dp),
      tint = MaterialTheme.filmeraColors.rating,
    )
    Text(
      text = stringResource(R.string.rating_value, item.voteAverage),
      modifier = Modifier.padding(start = 3.dp),
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun PersonSearchResultRow(
  person: SearchPerson,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TmdbImage(
        path = person.profilePath,
        contentDescription = stringResource(R.string.profile_image_description, person.name),
        modifier = Modifier
          .size(84.dp)
          .clip(RoundedCornerShape(20.dp)),
      )
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(start = 14.dp),
      ) {
        Text(
          text = person.name,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
        if (person.knownForDepartment.isNotBlank()) {
          Text(
            text = person.knownForDepartment,
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
          )
        }
        if (person.knownFor.isNotEmpty()) {
          Text(
            text = stringResource(R.string.search_known_for, person.knownFor.joinToString()),
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }
  }
}

@Composable
private fun CollectionSearchResultRow(collection: SearchCollection) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Row(modifier = Modifier.padding(10.dp)) {
      TmdbImage(
        path = collection.posterPath ?: collection.backdropPath,
        contentDescription = null,
        modifier = Modifier
          .size(width = 82.dp, height = 123.dp)
          .clip(RoundedCornerShape(12.dp)),
      )
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(start = 12.dp),
      ) {
        Text(
          text = collection.name,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = stringResource(R.string.search_collection_label),
          modifier = Modifier.padding(top = 4.dp),
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.labelMedium,
        )
        Text(
          text = collection.overview.ifBlank { stringResource(R.string.overview_unavailable) },
          modifier = Modifier.padding(top = 8.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }
  }
}

@Composable
private fun SearchEmptyContent(
  query: String,
  suggestion: String?,
  onClear: () -> Unit,
  onSuggestionSelected: (String) -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = Icons.Outlined.SearchOff,
      contentDescription = null,
      modifier = Modifier.size(56.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = stringResource(R.string.search_no_results_title),
      modifier = Modifier.padding(top = 18.dp),
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = stringResource(R.string.search_no_results_message, query),
      modifier = Modifier.padding(top = 8.dp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyLarge,
    )
    Text(
      text = stringResource(R.string.search_no_results_hint),
      modifier = Modifier.padding(top = 8.dp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyMedium,
    )
    Button(
      onClick = onClear,
      modifier = Modifier.padding(top = 20.dp),
    ) {
      Text(stringResource(R.string.clear_search))
    }
    suggestion?.let {
      TextButton(
        onClick = { onSuggestionSelected(it) },
        modifier = Modifier.padding(top = 6.dp),
      ) {
        Text(stringResource(R.string.search_try_suggestion, it))
      }
    }
  }
}

@Composable
private fun SearchErrorContent(
  error: AppError,
  recentSearches: List<String>,
  onRetry: () -> Unit,
  onRecentSelected: (String) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    item(key = "search-error") {
      InlineErrorCard(error = error, onRetry = onRetry)
    }
    if (recentSearches.isNotEmpty()) {
      item(key = "error-recents") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          SectionHeading(title = stringResource(R.string.search_recent_title))
          recentSearches.take(5).forEach { query ->
            Surface(
              onClick = { onRecentSelected(query) },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Outlined.History,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp),
                )
                Text(text = query, modifier = Modifier.padding(start = 12.dp))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun InlineErrorCard(
  error: AppError,
  onRetry: () -> Unit,
) {
  InlineStateCard(
    icon = Icons.Outlined.ErrorOutline,
    title = stringResource(R.string.search_error_title),
    message = stringResource(error.messageResource()),
    actionLabel = stringResource(R.string.retry),
    onAction = onRetry,
  )
}

@Composable
private fun InlineStateCard(
  icon: ImageVector,
  title: String,
  message: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        tint = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = title,
        modifier = Modifier.padding(top = 12.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = message,
        modifier = Modifier.padding(top = 6.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
      )
      if (actionLabel != null && onAction != null) {
        Button(
          onClick = onAction,
          modifier = Modifier.padding(top = 16.dp),
        ) {
          Text(actionLabel)
        }
      }
    }
  }
}

@Composable
private fun DiscoverySkeleton() {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SkeletonBox(
      modifier = Modifier
        .width(180.dp)
        .height(24.dp),
    )
    repeat(4) {
      SkeletonBox(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp),
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      repeat(2) {
        SkeletonBox(
          modifier = Modifier
            .weight(1f)
            .height(88.dp),
        )
      }
    }
  }
}

@Composable
private fun SearchResultSkeleton() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(142.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerLow)
      .padding(10.dp),
  ) {
    SkeletonBox(
      modifier = Modifier
        .width(82.dp)
        .fillMaxHeight(),
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      SkeletonBox(
        modifier = Modifier
          .fillMaxWidth(0.72f)
          .height(20.dp),
      )
      SkeletonBox(
        modifier = Modifier
          .fillMaxWidth(0.45f)
          .height(14.dp),
      )
      SkeletonBox(
        modifier = Modifier
          .fillMaxWidth()
          .height(14.dp),
      )
      SkeletonBox(
        modifier = Modifier
          .fillMaxWidth(0.8f)
          .height(14.dp),
      )
    }
  }
}

@Composable
private fun SkeletonBox(modifier: Modifier) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerHighest),
  )
}

@Composable
private fun SectionHeading(
  title: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .semantics { heading() },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )
    if (actionLabel != null && onAction != null) {
      TextButton(onClick = onAction) {
        Text(actionLabel)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClearRecentSearchesSheet(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = Icons.Outlined.ClearAll,
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        tint = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = stringResource(R.string.clear_recent_searches_title),
        modifier = Modifier.padding(top = 12.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = stringResource(R.string.clear_recent_searches_message),
        modifier = Modifier.padding(top = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 20.dp),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(onClick = onDismiss) {
          Text(stringResource(R.string.cancel))
        }
        Button(
          onClick = onConfirm,
          modifier = Modifier.padding(start = 8.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = stringResource(R.string.clear_all),
            modifier = Modifier.padding(start = 8.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun highlightedText(
  text: String,
  query: String,
) = buildAnnotatedString {
  val start = text.indexOf(query, ignoreCase = true)
  if (start < 0 || query.isEmpty()) {
    append(text)
    return@buildAnnotatedString
  }
  append(text.substring(0, start))
  withStyle(
    SpanStyle(
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
    ),
  ) {
    append(text.substring(start, start + query.length))
  }
  append(text.substring(start + query.length))
}

private fun SearchType.labelResource(): Int =
  when (this) {
    SearchType.ALL -> R.string.search_type_all
    SearchType.MOVIES -> R.string.search_movies
    SearchType.TV_SHOWS -> R.string.search_tv_shows
    SearchType.PEOPLE -> R.string.search_people
    SearchType.COLLECTIONS -> R.string.search_collections
  }

private fun SearchSuggestionType.icon(): ImageVector =
  when (this) {
    SearchSuggestionType.RECENT -> Icons.Outlined.History
    SearchSuggestionType.TITLE -> Icons.Outlined.Search
    SearchSuggestionType.PERSON -> Icons.Outlined.Person
    SearchSuggestionType.GENRE -> Icons.Outlined.Movie
    SearchSuggestionType.COLLECTION -> Icons.Outlined.Collections
  }

private enum class SearchPane {
  DISCOVERY,
  LOADING,
  RESULTS,
  EMPTY,
  ERROR,
}

private const val INITIAL_FOCUS_DELAY_MILLIS = 200L
private const val MAX_DISCOVERY_RECENT_SEARCHES = 5
private const val MAX_DISCOVERY_TRENDING_SEARCHES = 5

private val previewMovie = MediaItem(
  id = 1,
  type = MediaType.MOVIE,
  title = "Dune: Part Two",
  originalTitle = "Dune: Part Two",
  overview = "Paul Atreides unites with Chani and the Fremen while seeking revenge.",
  posterPath = null,
  backdropPath = null,
  releaseDate = "2024-02-27",
  voteAverage = 8.2,
  voteCount = 6_100,
  popularity = 120.0,
  adult = false,
  originalLanguage = "en",
  genreIds = listOf(12, 878),
)

private val previewPerson = SearchPerson(
  id = 2,
  name = "Timothée Chalamet",
  knownForDepartment = "Acting",
  profilePath = null,
  knownFor = listOf("Dune", "Wonka"),
  popularity = 80.0,
)

@Preview(name = "Search discovery", showBackground = true)
@Composable
private fun SearchDiscoveryPreview() {
  FilmeraTheme {
    SearchScreen(
      state = SearchUiState(
        recentSearches = listOf("Dune", "Korean thriller"),
        discoveryState = LoadState.Success(
          SearchDiscovery(
            trendingQueries = listOf("Dune: Part Two", "The Last of Us", "Squid Game"),
            genres = listOf(
              SearchGenre(12, "Adventure", 8, null),
              SearchGenre(18, "Drama", 6, null),
            ),
            popularPeople = listOf(previewPerson),
            hasPartialFailures = false,
          ),
        ),
      ),
      onEvent = {},
      onBack = {},
      onMediaClick = {},
      onPersonClick = {},
    )
  }
}

@Preview(name = "Search results", showBackground = true)
@Composable
private fun SearchResultsPreview() {
  val content = SearchContent(
    query = "dune",
    selectedType = SearchType.ALL,
    movies = listOf(previewMovie),
    people = listOf(previewPerson),
    topResult = SearchTopResult.Media(previewMovie),
  )
  FilmeraTheme {
    SearchScreen(
      state = SearchUiState(
        query = "dune",
        discoveryState = LoadState.Empty,
        resultsState = SearchResultsState.Success(content),
      ),
      onEvent = {},
      onBack = {},
      onMediaClick = {},
      onPersonClick = {},
    )
  }
}

@Preview(name = "Search loading", showBackground = true)
@Composable
private fun SearchLoadingPreview() {
  FilmeraTheme {
    SearchScreen(
      state = SearchUiState(
        query = "avatar",
        resultsState = SearchResultsState.Loading(),
      ),
      onEvent = {},
      onBack = {},
      onMediaClick = {},
      onPersonClick = {},
    )
  }
}

@Preview(name = "Search empty", showBackground = true)
@Composable
private fun SearchEmptyPreview() {
  FilmeraTheme {
    SearchScreen(
      state = SearchUiState(
        query = "unknown title",
        resultsState = SearchResultsState.Empty("unknown title"),
      ),
      onEvent = {},
      onBack = {},
      onMediaClick = {},
      onPersonClick = {},
    )
  }
}

@Preview(name = "Search error", showBackground = true)
@Composable
private fun SearchErrorPreview() {
  FilmeraTheme {
    SearchScreen(
      state = SearchUiState(
        query = "dune",
        recentSearches = listOf("Dune", "Arrival"),
        resultsState = SearchResultsState.Error(AppError.NetworkUnavailable),
      ),
      onEvent = {},
      onBack = {},
      onMediaClick = {},
      onPersonClick = {},
    )
  }
}
