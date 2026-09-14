package com.example.filmera.feature.library.presentation

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.filmera.R
import com.example.filmera.core.common.AppError
import com.example.filmera.core.designsystem.theme.FilmeraTheme
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.navigation.BottomNavigationBar
import com.example.filmera.core.navigation.defaultBottomNavigationDestinations
import com.example.filmera.core.ui.LazyLayoutKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.component.ErrorState
import com.example.filmera.feature.library.domain.LibraryItem
import com.example.filmera.feature.library.domain.WatchStatus

@Composable
fun WatchlistRoute(
  navController: NavHostController,
  onMediaClick: (MediaItem) -> Unit,
  onExploreClick: () -> Unit,
  viewModel: LibraryViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val undoLabel = stringResource(R.string.undo)
  val writeErrorMessage = stringResource(R.string.library_write_error)
  val mutationMessages = mapOf(
    LibraryMutationMessage.ADDED_TO_WATCHLIST to
      stringResource(R.string.library_added_watchlist),
    LibraryMutationMessage.MOVED_TO_WATCHING to
      stringResource(R.string.library_moved_watching),
    LibraryMutationMessage.MOVED_TO_COMPLETED to
      stringResource(R.string.library_moved_completed),
    LibraryMutationMessage.REMOVED_FROM_LIBRARY to
      stringResource(R.string.library_removed),
    LibraryMutationMessage.ADDED_TO_FAVORITES to
      stringResource(R.string.library_added_favorites),
    LibraryMutationMessage.REMOVED_FROM_FAVORITES to
      stringResource(R.string.library_removed_favorites),
  )

  LaunchedEffect(
    viewModel,
    snackbarHostState,
    mutationMessages,
    undoLabel,
    writeErrorMessage,
  ) {
    viewModel.effects.collect { effect ->
      when (effect) {
        is LibraryEffect.ShowUndo -> {
          val result = snackbarHostState.showSnackbar(
            message = mutationMessages.getValue(effect.message),
            actionLabel = undoLabel,
            withDismissAction = true,
          )
          if (result == SnackbarResult.ActionPerformed) {
            viewModel.onEvent(LibraryUiEvent.UndoLastMutation)
          }
        }
        LibraryEffect.ShowWriteError -> snackbarHostState.showSnackbar(
          message = writeErrorMessage,
          withDismissAction = true,
        )
      }
    }
  }

  WatchlistScreen(
    state = state,
    snackbarHostState = snackbarHostState,
    onEvent = viewModel::onEvent,
    onMediaClick = onMediaClick,
    onExploreClick = onExploreClick,
    bottomBar = {
      BottomNavigationBar(
        navController = navController,
        destinations = defaultBottomNavigationDestinations,
      )
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
  state: LibraryUiState,
  snackbarHostState: SnackbarHostState,
  onEvent: (LibraryUiEvent) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onExploreClick: () -> Unit,
  modifier: Modifier = Modifier,
  bottomBar: @Composable () -> Unit = {},
) {
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val density = LocalDensity.current
  val isImeVisible = WindowInsets.ime.getBottom(density) > 0
  var showSortSheet by remember { mutableStateOf(false) }
  var actionItem by remember { mutableStateOf<LibraryItem?>(null) }

  LaunchedEffect(state.isSearching) {
    if (state.isSearching) {
      focusRequester.requestFocus()
      keyboardController?.show()
    }
  }

  BackHandler(enabled = state.isSearching && isImeVisible) {
    keyboardController?.hide()
    focusManager.clearFocus()
  }
  BackHandler(enabled = state.isSearching && !isImeVisible) {
    onEvent(LibraryUiEvent.SearchClosed)
  }

  if (showSortSheet) {
    LibrarySortBottomSheet(
      selectedSort = state.selectedSort,
      onSelect = {
        onEvent(LibraryUiEvent.SortSelected(it))
        showSortSheet = false
      },
      onDismiss = { showSortSheet = false },
    )
  }

  actionItem?.let { item ->
    LibraryStatusBottomSheet(
      item = item,
      onStatusSelected = {
        if (it != item.watchStatus) {
          onEvent(LibraryUiEvent.WatchStatusChanged(item, it))
        }
        actionItem = null
      },
      onFavoriteToggled = {
        onEvent(LibraryUiEvent.FavoriteToggled(item))
        actionItem = null
      },
      onRemove = {
        onEvent(LibraryUiEvent.WatchStatusRemoved(item))
        actionItem = null
      },
      onDismiss = { actionItem = null },
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      LibraryTopAppBar(
        state = state,
        focusRequester = focusRequester,
        onQueryChange = { onEvent(LibraryUiEvent.QueryChanged(it)) },
        onSearchOpen = { onEvent(LibraryUiEvent.SearchOpened) },
        onKeyboardDone = {
          keyboardController?.hide()
          focusManager.clearFocus()
        },
        onSearchClose = {
          keyboardController?.hide()
          focusManager.clearFocus()
          onEvent(LibraryUiEvent.SearchClosed)
        },
      )
    },
    bottomBar = bottomBar,
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      if (!state.isSearching) {
        LibrarySummaryContent(state.summary)
      }
      LibraryTabRow(
        selectedTab = state.selectedTab,
        onTabSelected = { onEvent(LibraryUiEvent.TabSelected(it)) },
      )
      LibraryFilterBar(
        selectedFilter = state.selectedMediaFilter,
        selectedSort = state.selectedSort,
        onFilterSelected = { onEvent(LibraryUiEvent.MediaFilterSelected(it)) },
        onSortClick = { showSortSheet = true },
      )
      LibraryContent(
        state = state,
        onEvent = onEvent,
        onMediaClick = onMediaClick,
        onExploreClick = onExploreClick,
        onMore = { actionItem = it },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopAppBar(
  state: LibraryUiState,
  focusRequester: FocusRequester,
  onQueryChange: (String) -> Unit,
  onSearchOpen: () -> Unit,
  onKeyboardDone: () -> Unit,
  onSearchClose: () -> Unit,
) {
  TopAppBar(
    title = {
      if (state.isSearching) {
        TextField(
          value = state.query,
          onValueChange = onQueryChange,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .focusRequester(focusRequester),
          placeholder = { Text(stringResource(R.string.library_search)) },
          singleLine = true,
          leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
          },
          shape = RoundedCornerShape(18.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = { onKeyboardDone() }),
          colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
          ),
        )
      } else {
        Text(
          text = stringResource(R.string.library_title),
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
    navigationIcon = {
      if (state.isSearching) {
        IconButton(onClick = onSearchClose) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.navigate_back),
          )
        }
      }
    },
    actions = {
      if (state.isSearching) {
        if (state.query.isNotEmpty()) {
          IconButton(onClick = { onQueryChange("") }) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = stringResource(R.string.clear_search),
            )
          }
        }
      } else {
        IconButton(onClick = onSearchOpen) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.library_open_search),
          )
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
  )
}

@Composable
private fun LibrarySummaryContent(summary: LibrarySummary) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(
      text = pluralStringResource(
        R.plurals.library_summary_saved,
        summary.totalCount,
        summary.totalCount,
      ),
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Medium,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        text = "${
          pluralStringResource(
            R.plurals.library_summary_movies,
            summary.movieCount,
            summary.movieCount,
          )
        } · ${
          pluralStringResource(
            R.plurals.library_summary_tv_shows,
            summary.tvShowCount,
            summary.tvShowCount,
          )
        }",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
      )
      if (summary.watchingCount > 0) {
        Text(
          text = pluralStringResource(
            R.plurals.library_summary_watching,
            summary.watchingCount,
            summary.watchingCount,
          ),
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }
  }
}

@Composable
private fun LibraryTabRow(
  selectedTab: LibraryTab,
  onTabSelected: (LibraryTab) -> Unit,
) {
  LazyRow(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(
      items = LibraryTab.entries,
      key = { tab -> LazyLayoutKey.of("library-tab", tab.name) },
    ) { tab ->
      FilterChip(
        selected = tab == selectedTab,
        onClick = { onTabSelected(tab) },
        label = { Text(stringResource(tab.labelResource())) },
      )
    }
  }
}

@Composable
private fun LibraryFilterBar(
  selectedFilter: LibraryMediaFilter,
  selectedSort: LibrarySort,
  onFilterSelected: (LibraryMediaFilter) -> Unit,
  onSortClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LazyRow(
      modifier = Modifier.weight(1f),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      items(
        items = LibraryMediaFilter.entries,
        key = { filter -> LazyLayoutKey.of("library-filter", filter.name) },
      ) { filter ->
        FilterChip(
          selected = filter == selectedFilter,
          onClick = { onFilterSelected(filter) },
          label = { Text(stringResource(filter.labelResource())) },
        )
      }
    }
    OutlinedButton(onClick = onSortClick) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Sort,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = stringResource(selectedSort.shortLabelResource()),
        modifier = Modifier.padding(start = 6.dp),
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun LibraryContent(
  state: LibraryUiState,
  onEvent: (LibraryUiEvent) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onExploreClick: () -> Unit,
  onMore: (LibraryItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  when (val contentState = state.contentState) {
    LoadState.Loading -> LibraryLoadingState(modifier)
    LoadState.Empty -> LibraryEmptyState(
      tab = state.selectedTab,
      hasQuery = state.query.isNotBlank(),
      onAction = {
        if (state.query.isNotBlank()) {
          onEvent(LibraryUiEvent.QueryChanged(""))
        } else if (state.selectedTab == LibraryTab.WATCHING) {
          onEvent(LibraryUiEvent.TabSelected(LibraryTab.WATCHLIST))
        } else {
          onExploreClick()
        }
      },
      modifier = modifier,
    )
    is LoadState.Error -> Box(modifier = modifier) {
      ErrorState(
        error = contentState.error,
        onRetry = { onEvent(LibraryUiEvent.Retry) },
      )
    }
    is LoadState.Success -> {
      if (contentState.value.isEmpty()) {
        LibraryEmptyState(
          tab = state.selectedTab,
          hasQuery = state.query.isNotBlank(),
          onAction = {
            if (state.query.isNotBlank()) {
              onEvent(LibraryUiEvent.QueryChanged(""))
            } else if (state.selectedTab == LibraryTab.WATCHING) {
              onEvent(LibraryUiEvent.TabSelected(LibraryTab.WATCHLIST))
            } else {
              onExploreClick()
            }
          },
          modifier = modifier,
        )
      } else {
        LibraryItemList(
          items = contentState.value,
          selectedTab = state.selectedTab,
          mutationKeys = state.mutationKeys,
          onEvent = onEvent,
          onMediaClick = onMediaClick,
          onMore = onMore,
          modifier = modifier,
        )
      }
    }
  }
}

@Composable
private fun LibraryItemList(
  items: List<LibraryItem>,
  selectedTab: LibraryTab,
  mutationKeys: Set<com.example.filmera.core.model.MediaKey>,
  onEvent: (LibraryUiEvent) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onMore: (LibraryItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier = modifier) {
    if (maxWidth >= 840.dp) {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        gridItems(
          items = items,
          key = { item -> LazyLayoutKey.media("library-media", item.media) },
        ) { item ->
          LibraryItemEntry(
            item = item,
            selectedTab = selectedTab,
            isMutating = item.media.key in mutationKeys,
            onEvent = onEvent,
            onMediaClick = onMediaClick,
            onMore = onMore,
          )
        }
      }
    } else {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
      ) {
        LazyColumn(
          modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 760.dp),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(
            items = items,
            key = { item -> LazyLayoutKey.media("library-media", item.media) },
          ) { item ->
            LibraryItemEntry(
              item = item,
              selectedTab = selectedTab,
              isMutating = item.media.key in mutationKeys,
              onEvent = onEvent,
              onMediaClick = onMediaClick,
              onMore = onMore,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LibraryItemEntry(
  item: LibraryItem,
  selectedTab: LibraryTab,
  isMutating: Boolean,
  onEvent: (LibraryUiEvent) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onMore: (LibraryItem) -> Unit,
) {
  val primaryActionLabel = when (selectedTab) {
    LibraryTab.WATCHLIST -> stringResource(R.string.library_start_watching)
    LibraryTab.WATCHING -> stringResource(R.string.library_mark_completed)
    LibraryTab.COMPLETED -> stringResource(R.string.library_watch_again)
    LibraryTab.FAVORITES -> if (item.watchStatus == WatchStatus.NONE) {
      stringResource(R.string.library_add_watchlist)
    } else {
      stringResource(R.string.view_details)
    }
  }
  LibraryItemCard(
    item = item,
    primaryActionLabel = primaryActionLabel,
    statusLabel = item.watchStatus
      .takeUnless { it == WatchStatus.NONE }
      ?.let { stringResource(it.labelResource()) },
    isMutating = isMutating,
    onClick = { onMediaClick(item.media) },
    onPrimaryAction = {
      when (selectedTab) {
        LibraryTab.WATCHLIST -> onEvent(
          LibraryUiEvent.WatchStatusChanged(item, WatchStatus.WATCHING),
        )
        LibraryTab.WATCHING -> onEvent(
          LibraryUiEvent.WatchStatusChanged(item, WatchStatus.COMPLETED),
        )
        LibraryTab.COMPLETED -> onEvent(
          LibraryUiEvent.WatchStatusChanged(item, WatchStatus.WATCHING),
        )
        LibraryTab.FAVORITES -> {
          if (item.watchStatus == WatchStatus.NONE) {
            onEvent(LibraryUiEvent.WatchStatusChanged(item, WatchStatus.WATCHLIST))
          } else {
            onMediaClick(item.media)
          }
        }
      }
    },
    onToggleFavorite = { onEvent(LibraryUiEvent.FavoriteToggled(item)) },
    onMore = { onMore(item) },
  )
}

@Composable
private fun LibraryEmptyState(
  tab: LibraryTab,
  hasQuery: Boolean,
  onAction: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val content = if (hasQuery) {
    LibraryEmptyContent(
      icon = Icons.Default.Search,
      titleResource = R.string.library_empty_search_title,
      messageResource = R.string.library_empty_search_message,
      actionResource = R.string.clear_search,
    )
  } else {
    tab.emptyContent()
  }
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = content.icon,
      contentDescription = null,
      modifier = Modifier.size(52.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = stringResource(content.titleResource),
      modifier = Modifier.padding(top = 16.dp),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
    )
    Text(
      text = stringResource(content.messageResource),
      modifier = Modifier.padding(top = 8.dp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyMedium,
    )
    Button(
      onClick = onAction,
      modifier = Modifier.padding(top = 20.dp),
    ) {
      Text(stringResource(content.actionResource))
    }
  }
}

@Composable
private fun LibraryLoadingState(modifier: Modifier = Modifier) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(count = 4, key = { index -> LazyLayoutKey.indexed("library-loading", index) }) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .height(150.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
      ) {
        Row(modifier = Modifier.padding(12.dp)) {
          Box(
            modifier = Modifier
              .size(width = 84.dp, height = 126.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceContainerHighest),
          )
          Column(
            modifier = Modifier
              .weight(1f)
              .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            repeat(3) { index ->
              Box(
                modifier = Modifier
                  .fillMaxWidth(if (index == 0) 0.8f else 0.5f)
                  .height(if (index == 0) 20.dp else 14.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.surfaceContainerHighest),
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySortBottomSheet(
  selectedSort: LibrarySort,
  onSelect: (LibrarySort) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Text(
      text = stringResource(R.string.library_sort_title),
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
    )
    LibrarySort.entries.forEach { sort ->
      ListItem(
        headlineContent = { Text(stringResource(sort.labelResource())) },
        leadingContent = {
          RadioButton(
            selected = sort == selectedSort,
            onClick = null,
          )
        },
        modifier = Modifier.clickable { onSelect(sort) },
      )
    }
    Spacer(modifier = Modifier.height(24.dp))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryStatusBottomSheet(
  item: LibraryItem,
  onStatusSelected: (WatchStatus) -> Unit,
  onFavoriteToggled: () -> Unit,
  onRemove: () -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Text(
      text = stringResource(R.string.library_save_status_title, item.media.title),
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
    )
    listOf(
      WatchStatus.WATCHLIST,
      WatchStatus.WATCHING,
      WatchStatus.COMPLETED,
    ).forEach { status ->
      ListItem(
        headlineContent = { Text(stringResource(status.labelResource())) },
        leadingContent = {
          RadioButton(
            selected = status == item.watchStatus,
            onClick = null,
          )
        },
        modifier = Modifier.clickable { onStatusSelected(status) },
      )
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    ListItem(
      headlineContent = {
        Text(
          stringResource(
            if (item.isFavorite) {
              R.string.library_toggle_favorite_remove
            } else {
              R.string.library_toggle_favorite_add
            },
          ),
        )
      },
      leadingContent = {
        Icon(
          imageVector = if (item.isFavorite) Icons.Default.Favorite
          else Icons.Outlined.FavoriteBorder,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      },
      modifier = Modifier.clickable(onClick = onFavoriteToggled),
    )
    if (item.watchStatus != WatchStatus.NONE) {
      ListItem(
        headlineContent = {
          Text(
            text = stringResource(R.string.library_remove_status),
            color = MaterialTheme.colorScheme.error,
          )
        },
        leadingContent = {
          Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
          )
        },
        modifier = Modifier.clickable(onClick = onRemove),
      )
    }
    Spacer(modifier = Modifier.height(24.dp))
  }
}

private data class LibraryEmptyContent(
  val icon: ImageVector,
  val titleResource: Int,
  val messageResource: Int,
  val actionResource: Int,
)

private fun LibraryTab.emptyContent(): LibraryEmptyContent =
  when (this) {
    LibraryTab.WATCHLIST -> LibraryEmptyContent(
      icon = Icons.Outlined.BookmarkBorder,
      titleResource = R.string.library_empty_watchlist_title,
      messageResource = R.string.library_empty_watchlist_message,
      actionResource = R.string.library_explore_titles,
    )
    LibraryTab.WATCHING -> LibraryEmptyContent(
      icon = Icons.Outlined.PlayCircleOutline,
      titleResource = R.string.library_empty_watching_title,
      messageResource = R.string.library_empty_watching_message,
      actionResource = R.string.library_open_watchlist,
    )
    LibraryTab.COMPLETED -> LibraryEmptyContent(
      icon = Icons.Outlined.CheckCircleOutline,
      titleResource = R.string.library_empty_completed_title,
      messageResource = R.string.library_empty_completed_message,
      actionResource = R.string.library_explore_titles,
    )
    LibraryTab.FAVORITES -> LibraryEmptyContent(
      icon = Icons.Outlined.FavoriteBorder,
      titleResource = R.string.library_empty_favorites_title,
      messageResource = R.string.library_empty_favorites_message,
      actionResource = R.string.library_explore_titles,
    )
  }

private fun LibraryTab.labelResource(): Int =
  when (this) {
    LibraryTab.WATCHLIST -> R.string.library_tab_watchlist
    LibraryTab.WATCHING -> R.string.library_tab_watching
    LibraryTab.COMPLETED -> R.string.library_tab_completed
    LibraryTab.FAVORITES -> R.string.library_tab_favorites
  }

private fun LibraryMediaFilter.labelResource(): Int =
  when (this) {
    LibraryMediaFilter.ALL -> R.string.library_filter_all
    LibraryMediaFilter.MOVIES -> R.string.library_filter_movies
    LibraryMediaFilter.TV_SHOWS -> R.string.library_filter_tv_shows
  }

private fun LibrarySort.labelResource(): Int =
  when (this) {
    LibrarySort.RECENTLY_ADDED -> R.string.library_sort_recently_added
    LibrarySort.OLDEST_ADDED -> R.string.library_sort_oldest_added
    LibrarySort.TITLE_ASCENDING -> R.string.library_sort_title_ascending
    LibrarySort.HIGHEST_RATED -> R.string.library_sort_highest_rated
    LibrarySort.RELEASE_DATE -> R.string.library_sort_release_date
  }

private fun LibrarySort.shortLabelResource(): Int =
  if (this == LibrarySort.RECENTLY_ADDED) R.string.library_sort else labelResource()

private fun WatchStatus.labelResource(): Int =
  when (this) {
    WatchStatus.NONE -> R.string.library_status_none
    WatchStatus.WATCHLIST -> R.string.library_tab_watchlist
    WatchStatus.WATCHING -> R.string.library_tab_watching
    WatchStatus.COMPLETED -> R.string.library_tab_completed
  }

private val previewMedia = MediaItem(
  id = 1,
  type = MediaType.MOVIE,
  title = "Dune: Part Two",
  originalTitle = "Dune: Part Two",
  overview = "Paul Atreides unites with Chani and the Fremen.",
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

private val previewLibraryItem = LibraryItem(
  media = previewMedia,
  watchStatus = WatchStatus.WATCHLIST,
  isFavorite = true,
  addedAt = 1L,
  updatedAt = 1L,
)

@Preview(name = "Library populated", showBackground = true)
@Composable
private fun LibraryPopulatedPreview() {
  FilmeraTheme {
    WatchlistScreen(
      state = LibraryUiState(
        contentState = LoadState.Success(listOf(previewLibraryItem)),
        summary = LibrarySummary(totalCount = 1, movieCount = 1),
      ),
      snackbarHostState = remember { SnackbarHostState() },
      onEvent = {},
      onMediaClick = {},
      onExploreClick = {},
    )
  }
}

@Preview(name = "Library empty", showBackground = true)
@Composable
private fun LibraryEmptyPreview() {
  FilmeraTheme {
    WatchlistScreen(
      state = LibraryUiState(contentState = LoadState.Success(emptyList())),
      snackbarHostState = remember { SnackbarHostState() },
      onEvent = {},
      onMediaClick = {},
      onExploreClick = {},
    )
  }
}

@Preview(name = "Library loading", showBackground = true)
@Composable
private fun LibraryLoadingPreview() {
  FilmeraTheme {
    WatchlistScreen(
      state = LibraryUiState(),
      snackbarHostState = remember { SnackbarHostState() },
      onEvent = {},
      onMediaClick = {},
      onExploreClick = {},
    )
  }
}

@Preview(name = "Library error", showBackground = true)
@Composable
private fun LibraryErrorPreview() {
  FilmeraTheme {
    WatchlistScreen(
      state = LibraryUiState(contentState = LoadState.Error(AppError.Unknown)),
      snackbarHostState = remember { SnackbarHostState() },
      onEvent = {},
      onMediaClick = {},
      onExploreClick = {},
    )
  }
}
