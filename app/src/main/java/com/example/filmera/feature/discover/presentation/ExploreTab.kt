package com.example.filmera.feature.discover.presentation

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.FilmeraTheme
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LazyLayoutKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.component.ErrorState
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.feature.discover.domain.ExploreAvailability
import com.example.filmera.feature.discover.domain.ExploreCategory
import com.example.filmera.feature.discover.domain.ExploreContent
import com.example.filmera.feature.discover.domain.ExploreContentType
import com.example.filmera.feature.discover.domain.ExploreDuration
import com.example.filmera.feature.discover.domain.ExploreFilterState
import com.example.filmera.feature.discover.domain.ExploreLanguage
import com.example.filmera.feature.discover.domain.ExploreLayoutMode
import com.example.filmera.feature.discover.domain.ExploreMediaItem
import com.example.filmera.feature.discover.domain.ExploreProvider
import com.example.filmera.feature.discover.domain.ExploreRegion
import com.example.filmera.feature.discover.domain.ExploreSort
import com.example.filmera.feature.discover.domain.ExploreTvStatus
import com.example.filmera.feature.discover.domain.ExploreYear
import com.example.filmera.feature.discover.domain.MediaClassification
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExploreTab(
  state: ExploreUiState,
  onAction: (DiscoverAction) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onScrollStateChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val gridState = rememberLazyGridState(
    initialFirstVisibleItemIndex = state.gridFirstVisibleIndex,
    initialFirstVisibleItemScrollOffset = state.gridFirstVisibleOffset,
  )
  val listState = rememberLazyListState(
    initialFirstVisibleItemIndex = state.listFirstVisibleIndex,
    initialFirstVisibleItemScrollOffset = state.listFirstVisibleOffset,
  )
  val isResultsScrolled by remember(state.layoutMode) {
    derivedStateOf {
      when (state.layoutMode) {
        ExploreLayoutMode.GRID ->
          gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 32
        ExploreLayoutMode.LIST ->
          listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 32
      }
    }
  }
  var isFilterPanelExpanded by rememberSaveable { mutableStateOf(!isResultsScrolled) }
  var showAdvancedFilters by rememberSaveable { mutableStateOf(false) }
  var previewItem by remember { mutableStateOf<ExploreMediaItem?>(null) }
  var observedFilters by remember { mutableStateOf(state.filters) }

  LaunchedEffect(isResultsScrolled) {
    onScrollStateChanged(isResultsScrolled)
    if (isResultsScrolled) isFilterPanelExpanded = false
  }
  LaunchedEffect(state.filters) {
    if (observedFilters != state.filters) {
      observedFilters = state.filters
      gridState.scrollToItem(0)
      listState.scrollToItem(0)
      isFilterPanelExpanded = true
    }
  }
  ExploreScrollPersistenceEffect(
    layoutMode = state.layoutMode,
    gridState = gridState,
    listIndex = { listState.firstVisibleItemIndex },
    listOffset = { listState.firstVisibleItemScrollOffset },
    onScrollChanged = { layout, index, offset ->
      onAction(DiscoverAction.ExploreScrollChanged(layout, index, offset))
    },
  )

  Column(modifier = modifier.fillMaxSize()) {
    AnimatedContent(
      targetState = isFilterPanelExpanded,
      transitionSpec = {
        (fadeIn(tween(180)) togetherWith fadeOut(tween(120)))
          .using(SizeTransform(clip = false))
      },
      label = "explore-filter-panel",
    ) { expanded ->
      if (expanded) {
        ExploreExpandedHeader(
          filters = state.filters,
          onFiltersChanged = {
            onAction(DiscoverAction.ExploreFiltersChanged(it))
          },
          onOpenAdvanced = { showAdvancedFilters = true },
          onCollapse = { isFilterPanelExpanded = false },
        )
      } else {
        ExploreCollapsedHeader(
          filters = state.filters,
          onExpand = { isFilterPanelExpanded = true },
        )
      }
    }

    AnimatedVisibility(visible = state.isRefreshing) {
      LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
      )
    }

    ExploreResultHeader(
      contentState = state.contentState,
      layoutMode = state.layoutMode,
      onLayoutChanged = {
        onAction(DiscoverAction.ExploreLayoutChanged(it))
      },
    )

    state.refreshError?.let {
      ExploreInlineNotice(
        message = stringResource(R.string.explore_refresh_error),
        onRetry = { onAction(DiscoverAction.ExploreRetryRequested) },
      )
    }
    val content = (state.contentState as? LoadState.Success)?.value
    if (content?.isPartial == true) {
      ExploreInlineNotice(
        message = stringResource(R.string.explore_partial_results),
      )
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      contentAlignment = Alignment.TopCenter,
    ) {
      when (val contentState = state.contentState) {
        LoadState.Loading -> ExploreLoadingState(state.layoutMode)
        LoadState.Empty -> ExploreEmptyState(
          onRelaxFilters = {
            onAction(
              DiscoverAction.ExploreFiltersChanged(
                state.filters.relaxMostRestrictiveFilter(),
              ),
            )
          },
        )
        is LoadState.Error -> ErrorState(
          error = contentState.error,
          onRetry = { onAction(DiscoverAction.ExploreRetryRequested) },
        )
        is LoadState.Success -> when (state.layoutMode) {
          ExploreLayoutMode.GRID -> ExploreMediaGrid(
            content = contentState.value,
            gridState = gridState,
            isLoadingMore = state.isLoadingMore,
            hasPaginationError = state.paginationError != null,
            onLoadMore = { onAction(DiscoverAction.ExploreLoadMoreRequested) },
            onRetryLoadMore = { onAction(DiscoverAction.ExploreLoadMoreRequested) },
            onMediaClick = onMediaClick,
            onQuickPreview = { previewItem = it },
          )
          ExploreLayoutMode.LIST -> ExploreMediaList(
            content = contentState.value,
            listState = listState,
            isLoadingMore = state.isLoadingMore,
            hasPaginationError = state.paginationError != null,
            onLoadMore = { onAction(DiscoverAction.ExploreLoadMoreRequested) },
            onRetryLoadMore = { onAction(DiscoverAction.ExploreLoadMoreRequested) },
            onMediaClick = onMediaClick,
            onQuickPreview = { previewItem = it },
          )
        }
      }
    }
  }

  if (showAdvancedFilters) {
    AdvancedFilterBottomSheet(
      filters = state.filters,
      providerState = state.providerState,
      estimatedResultCount = (state.contentState as? LoadState.Success)
        ?.value
        ?.totalResults,
      onDismiss = { showAdvancedFilters = false },
      onApply = { filters ->
        showAdvancedFilters = false
        onAction(DiscoverAction.ExploreFiltersChanged(filters))
      },
      onRetryProviders = {
        onAction(DiscoverAction.ExploreProvidersRetryRequested)
      },
    )
  }

  previewItem?.let { item ->
    ExploreQuickPreviewSheet(
      item = item,
      onDismiss = { previewItem = null },
      onViewDetails = {
        previewItem = null
        onMediaClick(item.media)
      },
    )
  }
}

@Composable
private fun ExploreExpandedHeader(
  filters: ExploreFilterState,
  onFiltersChanged: (ExploreFilterState) -> Unit,
  onOpenAdvanced: () -> Unit,
  onCollapse: () -> Unit,
) {
  val colorScheme = MaterialTheme.colorScheme
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        Brush.verticalGradient(
          listOf(
            colorScheme.surfaceContainer,
            colorScheme.surface,
          ),
        ),
      ),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 1080.dp)
        .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      ExploreIntroduction(onOpenAdvanced, onCollapse)
      Spacer(modifier = Modifier.height(8.dp))
      ExploreFilterRow(
        label = stringResource(R.string.explore_filter_sort),
        choices = ExploreSort.entries,
        isSelected = { it == filters.sort },
        choiceLabel = { stringResource(it.labelResource) },
        onSelected = { onFiltersChanged(filters.copy(sort = it)) },
      )
      ExploreFilterRow(
        label = stringResource(R.string.explore_filter_type),
        choices = ExploreContentType.entries,
        isSelected = { it == filters.contentType },
        choiceLabel = { stringResource(it.labelResource) },
        onSelected = {
          onFiltersChanged(
            filters.copy(
              contentType = it,
              tvStatus = if (it.supportsTvStatus) {
                filters.tvStatus
              } else {
                ExploreTvStatus.ANY
              },
            ),
          )
        },
      )
      ExploreCategoryFilterRow(
        filters = filters,
        onFiltersChanged = onFiltersChanged,
      )
      ExploreFilterRow(
        label = stringResource(R.string.explore_filter_region),
        choices = ExploreRegion.entries,
        isSelected = { it == filters.region },
        choiceLabel = { stringResource(it.labelResource) },
        onSelected = { onFiltersChanged(filters.copy(region = it)) },
      )
      ExploreFilterRow(
        label = stringResource(R.string.explore_filter_year),
        choices = ExploreYear.entries,
        isSelected = { it == filters.year },
        choiceLabel = { stringResource(it.labelResource) },
        onSelected = { onFiltersChanged(filters.copy(year = it)) },
      )
      ExploreMoreFilterRow(onOpenAdvanced)
      ActiveFilterSummary(
        filters = filters,
        onFiltersChanged = onFiltersChanged,
      )
    }
  }
}

@Composable
private fun ExploreIntroduction(
  onOpenAdvanced: () -> Unit,
  onCollapse: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Text(
        text = stringResource(R.string.discover_explore_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = stringResource(R.string.discover_explore_subtitle),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
    IconButton(onClick = onOpenAdvanced) {
      Icon(
        imageVector = Icons.Outlined.Tune,
        contentDescription = stringResource(R.string.explore_more_filters),
      )
    }
    IconButton(onClick = onCollapse) {
      Icon(
        imageVector = Icons.Outlined.ExpandLess,
        contentDescription = stringResource(R.string.explore_collapse_filters),
      )
    }
  }
}

@Composable
private fun <T> ExploreFilterRow(
  label: String,
  choices: List<T>,
  isSelected: (T) -> Boolean,
  choiceLabel: @Composable (T) -> String,
  onSelected: (T) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(48.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      modifier = Modifier.width(FILTER_LABEL_WIDTH),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Medium,
    )
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(end = 8.dp),
    ) {
      items(
        items = choices,
        key = { it.toString() },
      ) { choice ->
        ExploreFilterBadge(
          label = choiceLabel(choice),
          selected = isSelected(choice),
          onClick = { onSelected(choice) },
        )
      }
    }
  }
}

@Composable
private fun ExploreCategoryFilterRow(
  filters: ExploreFilterState,
  onFiltersChanged: (ExploreFilterState) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(48.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(R.string.explore_filter_category),
      modifier = Modifier.width(FILTER_LABEL_WIDTH),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Medium,
    )
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(end = 8.dp),
    ) {
      item(key = "all-categories") {
        ExploreFilterBadge(
          label = stringResource(R.string.explore_category_all),
          selected = filters.categories.isEmpty(),
          onClick = {
            onFiltersChanged(filters.copy(categories = emptySet()))
          },
        )
      }
      items(
        items = ExploreCategory.entries,
        key = ExploreCategory::name,
      ) { category ->
        ExploreFilterBadge(
          label = stringResource(category.labelResource),
          selected = category in filters.categories,
          onClick = {
            onFiltersChanged(
              filters.copy(
                categories = filters.categories.toggle(category),
              ),
            )
          },
        )
      }
    }
  }
}

@Composable
private fun ExploreMoreFilterRow(
  onOpenAdvanced: () -> Unit,
) {
  val labels = listOf(
    R.string.explore_more_language,
    R.string.explore_more_provider,
    R.string.explore_more_availability,
    R.string.explore_more_rating,
    R.string.explore_more_duration,
    R.string.explore_more_status,
  )
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(48.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(R.string.explore_filter_more),
      modifier = Modifier.width(FILTER_LABEL_WIDTH),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Medium,
    )
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(end = 8.dp),
    ) {
      items(labels, key = { it }) { label ->
        ExploreFilterBadge(
          label = stringResource(label),
          selected = false,
          onClick = onOpenAdvanced,
        )
      }
    }
  }
}

@Composable
private fun ExploreFilterBadge(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .height(48.dp)
      .defaultMinSize(minWidth = 48.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      onClick = onClick,
      modifier = Modifier
        .height(34.dp)
        .semantics { this.selected = selected },
      shape = CircleShape,
      color = if (selected) {
        MaterialTheme.colorScheme.primary
      } else {
        Color.Transparent
      },
      contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant
      },
      border = BorderStroke(
        width = 1.dp,
        color = if (selected) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.outlineVariant
        },
      ),
    ) {
      Box(
        modifier = Modifier.padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = label,
          maxLines = 1,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
      }
    }
  }
}

@Composable
private fun ActiveFilterSummary(
  filters: ExploreFilterState,
  onFiltersChanged: (ExploreFilterState) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LazyRow(
      modifier = Modifier.weight(1f),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      contentPadding = PaddingValues(end = 8.dp),
    ) {
      item("sort") {
        SummaryChip(stringResource(filters.sort.labelResource))
      }
      item("type") {
        SummaryChip(stringResource(filters.contentType.labelResource))
      }
      item("region") {
        SummaryChip(stringResource(filters.region.labelResource))
      }
      if (filters.categories.isEmpty()) {
        item("all-categories") {
          SummaryChip(stringResource(R.string.explore_category_all))
        }
      } else {
        items(
          items = filters.categories.sortedBy(ExploreCategory::ordinal),
          key = ExploreCategory::name,
        ) { category ->
          SummaryChip(
            label = stringResource(category.labelResource),
            onRemove = {
              onFiltersChanged(
                filters.copy(categories = filters.categories - category),
              )
            },
          )
        }
      }
      if (filters.language != ExploreLanguage.ALL) {
        item("language") {
          SummaryChip(stringResource(filters.language.labelResource))
        }
      }
      if (filters.minimumRating != null) {
        item("rating") {
          SummaryChip(
            stringResource(R.string.explore_rating_value, filters.minimumRating),
          )
        }
      }
      if (filters.providerIds.isNotEmpty()) {
        item("providers") {
          SummaryChip(stringResource(R.string.explore_more_provider))
        }
      }
    }
    AnimatedVisibility(visible = !filters.isDefault) {
      TextButton(
        onClick = { onFiltersChanged(ExploreFilterState()) },
      ) {
        Text(stringResource(R.string.explore_clear_filters))
      }
    }
  }
}

@Composable
private fun SummaryChip(
  label: String,
  onRemove: (() -> Unit)? = null,
) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
  ) {
    Row(
      modifier = Modifier
        .heightIn(min = 30.dp)
        .padding(start = 10.dp, end = if (onRemove == null) 10.dp else 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
      )
      onRemove?.let {
        IconButton(
          onClick = it,
          modifier = Modifier.size(30.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = stringResource(R.string.remove_filter, label),
            modifier = Modifier.size(14.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun ExploreCollapsedHeader(
  filters: ExploreFilterState,
  onExpand: () -> Unit,
) {
  Surface(
    onClick = onExpand,
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceContainer,
    tonalElevation = 2.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .padding(horizontal = 16.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.FilterList,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.width(10.dp))
      LazyRow(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        userScrollEnabled = false,
      ) {
        item { SummaryChip(stringResource(filters.sort.labelResource)) }
        item { SummaryChip(stringResource(filters.contentType.labelResource)) }
        item { SummaryChip(stringResource(filters.region.labelResource)) }
        filters.categories.firstOrNull()?.let { category ->
          item { SummaryChip(stringResource(category.labelResource)) }
        }
      }
      Icon(
        imageVector = Icons.Outlined.ExpandMore,
        contentDescription = stringResource(R.string.explore_expand_filters),
      )
    }
  }
}

@Composable
private fun ExploreResultHeader(
  contentState: LoadState<ExploreContent>,
  layoutMode: ExploreLayoutMode,
  onLayoutChanged: (ExploreLayoutMode) -> Unit,
) {
  val totalResults = (contentState as? LoadState.Success)?.value?.totalResults
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.explore_top_results),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = totalResults?.let {
          pluralStringResource(R.plurals.explore_title_count, it, it)
        } ?: stringResource(R.string.not_available_short),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
      )
    }
    ExploreLayoutToggle(layoutMode, onLayoutChanged)
  }
}

@Composable
private fun ExploreLayoutToggle(
  layoutMode: ExploreLayoutMode,
  onLayoutChanged: (ExploreLayoutMode) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Row(modifier = Modifier.padding(3.dp)) {
      ExploreLayoutButton(
        selected = layoutMode == ExploreLayoutMode.GRID,
        icon = {
          Icon(
            imageVector = Icons.Outlined.GridView,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
        },
        contentDescription = stringResource(R.string.explore_show_grid),
        onClick = { onLayoutChanged(ExploreLayoutMode.GRID) },
      )
      ExploreLayoutButton(
        selected = layoutMode == ExploreLayoutMode.LIST,
        icon = {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ViewList,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
        },
        contentDescription = stringResource(R.string.explore_show_list),
        onClick = { onLayoutChanged(ExploreLayoutMode.LIST) },
      )
    }
  }
}

@Composable
private fun ExploreLayoutButton(
  selected: Boolean,
  icon: @Composable () -> Unit,
  contentDescription: String,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    modifier = Modifier
      .size(40.dp)
      .semantics {
        this.selected = selected
      },
    shape = RoundedCornerShape(9.dp),
    color = if (selected) {
      MaterialTheme.colorScheme.primaryContainer
    } else {
      Color.Transparent
    },
    contentColor = if (selected) {
      MaterialTheme.colorScheme.onPrimaryContainer
    } else {
      MaterialTheme.colorScheme.onSurfaceVariant
    },
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.semantics {
        this.contentDescription = contentDescription
      },
    ) {
      icon()
    }
  }
}

@Composable
private fun ExploreInlineNotice(
  message: String,
  onRetry: (() -> Unit)? = null,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
  ) {
    Row(
      modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = message,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodySmall,
      )
      onRetry?.let {
        TextButton(onClick = it) {
          Text(stringResource(R.string.retry))
        }
      }
    }
  }
}

@Composable
private fun ExploreMediaGrid(
  content: ExploreContent,
  gridState: LazyGridState,
  isLoadingMore: Boolean,
  hasPaginationError: Boolean,
  onLoadMore: () -> Unit,
  onRetryLoadMore: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onQuickPreview: (ExploreMediaItem) -> Unit,
) {
  ExploreGridLoadMoreEffect(
    itemCount = content.items.size,
    canLoadMore = content.hasMore,
    gridState = gridState,
    onLoadMore = onLoadMore,
  )
  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 1120.dp),
  ) {
    val columns = when {
      maxWidth < 400.dp -> GridCells.Fixed(2)
      maxWidth < 600.dp -> GridCells.Fixed(3)
      else -> GridCells.Adaptive(148.dp)
    }
    LazyVerticalGrid(
      columns = columns,
      state = gridState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = 16.dp,
        top = 4.dp,
        end = 16.dp,
        bottom = 28.dp,
      ),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      items(
        items = content.items,
        key = { item -> LazyLayoutKey.media("explore-grid", item.media) },
      ) { item ->
        ExploreMediaCard(
          item = item,
          onClick = { onMediaClick(item.media) },
          onLongClick = { onQuickPreview(item) },
        )
      }
      item(
        key = "explore-grid-tail",
        span = { GridItemSpan(maxLineSpan) },
      ) {
        ExplorePaginationTail(
          isLoading = isLoadingMore,
          hasError = hasPaginationError,
          hasMore = content.hasMore,
          onRetry = onRetryLoadMore,
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExploreMediaCard(
  item: ExploreMediaItem,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  val clickLabel = stringResource(R.string.explore_view_details)
  val longClickLabel = stringResource(R.string.explore_quick_preview_hint, item.media.title)
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .combinedClickable(
        role = Role.Button,
        onClickLabel = clickLabel,
        onClick = onClick,
        onLongClickLabel = longClickLabel,
        onLongClick = onLongClick,
      ),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(2f / 3f)
        .clip(RoundedCornerShape(16.dp)),
    ) {
      TmdbImage(
        path = item.media.posterPath,
        contentDescription = stringResource(
          R.string.explore_poster_description,
          item.media.title,
        ),
        modifier = Modifier.matchParentSize(),
        size = "w342",
      )
      Box(
        modifier = Modifier
          .matchParentSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color.Transparent,
                Color.Transparent,
                MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.68f),
              ),
            ),
          ),
      )
      ExploreMediaBadges(
        item = item,
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(8.dp),
      )
      Surface(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(8.dp),
        shape = CircleShape,
        color = MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.72f),
        contentColor = MaterialTheme.filmeraColors.onImage,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.tertiary,
          )
          Text(
            text = stringResource(R.string.rating_value, item.media.voteAverage),
            modifier = Modifier.padding(start = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
    Text(
      text = item.media.title,
      modifier = Modifier.padding(top = 8.dp),
      maxLines = 2,
      minLines = 2,
      overflow = TextOverflow.Ellipsis,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
    )
    Text(
      text = item.media.compactOriginMetadata(
        globalLabel = stringResource(R.string.explore_global),
      ),
      modifier = Modifier.padding(top = 2.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun ExploreMediaBadges(
  item: ExploreMediaItem,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    MediaBadge(
      label = stringResource(
        if (item.media.type == MediaType.MOVIE) {
          R.string.explore_media_movie
        } else {
          R.string.explore_media_series
        },
      ),
    )
    item.classifications.firstOrNull()?.let { classification ->
      MediaBadge(label = stringResource(classification.labelResource))
    }
  }
}

@Composable
private fun MediaBadge(
  label: String,
) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.72f),
    contentColor = MaterialTheme.filmeraColors.onImage,
  ) {
    Text(
      text = label.uppercase(),
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun ExploreMediaList(
  content: ExploreContent,
  listState: androidx.compose.foundation.lazy.LazyListState,
  isLoadingMore: Boolean,
  hasPaginationError: Boolean,
  onLoadMore: () -> Unit,
  onRetryLoadMore: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onQuickPreview: (ExploreMediaItem) -> Unit,
) {
  ExploreListLoadMoreEffect(
    itemCount = content.items.size,
    canLoadMore = content.hasMore,
    lastVisibleIndex = {
      listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
    },
    onLoadMore = onLoadMore,
  )
  LazyColumn(
    state = listState,
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 900.dp),
    contentPadding = PaddingValues(
      start = 16.dp,
      top = 4.dp,
      end = 16.dp,
      bottom = 28.dp,
    ),
  ) {
    items(
      items = content.items,
      key = { item -> LazyLayoutKey.media("explore-list", item.media) },
    ) { item ->
      ExploreMediaListItem(
        item = item,
        onClick = { onMediaClick(item.media) },
        onLongClick = { onQuickPreview(item) },
      )
      HorizontalDivider(
        modifier = Modifier.padding(start = 88.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
      )
    }
    item(key = "explore-list-tail") {
      ExplorePaginationTail(
        isLoading = isLoadingMore,
        hasError = hasPaginationError,
        hasMore = content.hasMore,
        onRetry = onRetryLoadMore,
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExploreMediaListItem(
  item: ExploreMediaItem,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  val clickLabel = stringResource(R.string.explore_view_details)
  val longClickLabel = stringResource(R.string.explore_quick_preview_hint, item.media.title)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .combinedClickable(
        role = Role.Button,
        onClickLabel = clickLabel,
        onClick = onClick,
        onLongClickLabel = longClickLabel,
        onLongClick = onLongClick,
      )
      .padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    TmdbImage(
      path = item.media.posterPath,
      contentDescription = stringResource(
        R.string.explore_poster_description,
        item.media.title,
      ),
      modifier = Modifier
        .width(72.dp)
        .aspectRatio(2f / 3f)
        .clip(RoundedCornerShape(12.dp)),
      size = "w185",
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = 14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = item.media.title,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        InlineMediaBadge(
          stringResource(
            if (item.media.type == MediaType.MOVIE) {
              R.string.explore_media_movie
            } else {
              R.string.explore_media_series
            },
          ),
        )
        item.classifications.firstOrNull()?.let {
          InlineMediaBadge(stringResource(it.labelResource))
        }
      }
      Text(
        text = stringResource(
          R.string.explore_media_metadata,
          item.media.voteAverage,
          item.media.releaseDate?.take(4)
            ?: stringResource(R.string.not_available_short),
          item.media.originCountries.firstOrNull()
            ?: stringResource(R.string.explore_global),
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
      )
      Text(
        text = item.media.overview,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

@Composable
private fun InlineMediaBadge(label: String) {
  Surface(
    shape = RoundedCornerShape(5.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
  ) {
    Text(
      text = label.uppercase(),
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun ExplorePaginationTail(
  isLoading: Boolean,
  hasError: Boolean,
  hasMore: Boolean,
  onRetry: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 72.dp),
    contentAlignment = Alignment.Center,
  ) {
    when {
      isLoading -> CircularProgressIndicator(
        modifier = Modifier.size(28.dp),
        strokeWidth = 2.dp,
      )
      hasError -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = stringResource(R.string.explore_load_more_failed),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onRetry) {
          Text(stringResource(R.string.retry))
        }
      }
      !hasMore -> Text(
        text = stringResource(R.string.explore_end_of_results),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

@Composable
private fun ExploreLoadingState(layoutMode: ExploreLayoutMode) {
  val loadingDescription = stringResource(R.string.explore_loading_results)
  if (layoutMode == ExploreLayoutMode.LIST) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 900.dp)
        .semantics {
          contentDescription = loadingDescription
        },
      contentPadding = PaddingValues(16.dp),
    ) {
      items(6, key = { "explore-list-loading-$it" }) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        ) {
          SkeletonBox(
            modifier = Modifier
              .width(72.dp)
              .aspectRatio(2f / 3f),
          )
          Column(
            modifier = Modifier.padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
          ) {
            SkeletonBox(
              modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(16.dp),
            )
            SkeletonBox(
              modifier = Modifier
                .width(96.dp)
                .height(12.dp),
            )
            SkeletonBox(
              modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            )
          }
        }
      }
    }
  } else {
    LazyVerticalGrid(
      columns = GridCells.Adaptive(128.dp),
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 1120.dp)
        .semantics {
          contentDescription = loadingDescription
        },
      contentPadding = PaddingValues(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      items(9, key = { "explore-grid-loading-$it" }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SkeletonBox(
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(2f / 3f),
          )
          SkeletonBox(
            modifier = Modifier
              .fillMaxWidth(0.82f)
              .height(14.dp),
          )
          SkeletonBox(
            modifier = Modifier
              .fillMaxWidth(0.52f)
              .height(11.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun SkeletonBox(modifier: Modifier) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerHighest),
  )
}

@Composable
private fun ExploreEmptyState(
  onRelaxFilters: () -> Unit,
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
      tint = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = stringResource(R.string.explore_empty_title),
      modifier = Modifier.padding(top = 16.dp),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
    )
    Text(
      text = stringResource(R.string.explore_empty_message),
      modifier = Modifier.padding(top = 8.dp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyMedium,
    )
    OutlinedButton(
      onClick = onRelaxFilters,
      modifier = Modifier.padding(top = 20.dp),
    ) {
      Text(stringResource(R.string.explore_clear_some_filters))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedFilterBottomSheet(
  filters: ExploreFilterState,
  providerState: LoadState<List<ExploreProvider>>,
  estimatedResultCount: Int?,
  onDismiss: () -> Unit,
  onApply: (ExploreFilterState) -> Unit,
  onRetryProviders: () -> Unit,
) {
  var draft by remember(filters) { mutableStateOf(filters) }
  ModalBottomSheet(
    onDismissRequest = onDismiss,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.92f),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 20.dp),
      ) {
        Text(
          text = stringResource(R.string.explore_advanced_title),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = stringResource(R.string.explore_advanced_subtitle),
          modifier = Modifier.padding(top = 4.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .padding(top = 12.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
      ) {
        item("language") {
          AdvancedChoiceSection(
            title = stringResource(R.string.explore_more_language),
            choices = ExploreLanguage.entries,
            label = { stringResource(it.labelResource) },
            selected = { it == draft.language },
            onSelected = { draft = draft.copy(language = it) },
          )
        }
        item("provider") {
          ProviderChoiceSection(
            providerState = providerState,
            selectedProviderIds = draft.providerIds,
            onSelectedProviderIds = {
              draft = draft.copy(providerIds = it)
            },
            onRetry = onRetryProviders,
          )
        }
        item("availability") {
          AdvancedChoiceSection(
            title = stringResource(R.string.explore_more_availability),
            choices = ExploreAvailability.entries,
            label = { stringResource(it.labelResource) },
            selected = { it == draft.availability },
            onSelected = { draft = draft.copy(availability = it) },
          )
        }
        item("rating") {
          AdvancedChoiceSection(
            title = stringResource(R.string.rating),
            choices = listOf(null, 6.0, 7.0, 7.5, 8.0, 8.5),
            label = {
              it?.let { rating ->
                stringResource(R.string.explore_rating_value, rating)
              } ?: stringResource(R.string.explore_rating_any)
            },
            selected = { it == draft.minimumRating },
            onSelected = { draft = draft.copy(minimumRating = it) },
          )
        }
        item("votes") {
          AdvancedChoiceSection(
            title = stringResource(R.string.explore_more_rating),
            choices = listOf(null, 50, 100, 500, 1_000, 10_000),
            label = {
              it?.let { votes ->
                stringResource(R.string.explore_votes_value, votes)
              } ?: stringResource(R.string.explore_votes_any)
            },
            selected = { it == draft.minimumVoteCount },
            onSelected = { draft = draft.copy(minimumVoteCount = it) },
          )
        }
        item("duration") {
          AdvancedChoiceSection(
            title = stringResource(R.string.explore_more_duration),
            choices = ExploreDuration.entries,
            label = { stringResource(it.labelResource) },
            selected = { it == draft.duration },
            onSelected = { draft = draft.copy(duration = it) },
          )
        }
        if (draft.contentType.supportsTvStatus) {
          item("status") {
            AdvancedChoiceSection(
              title = stringResource(R.string.explore_more_status),
              choices = ExploreTvStatus.entries,
              label = { stringResource(it.labelResource) },
              selected = { it == draft.tvStatus },
              onSelected = { draft = draft.copy(tvStatus = it) },
            )
          }
        }
      }
      HorizontalDivider()
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = { draft = ExploreFilterState() },
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text(stringResource(R.string.explore_filter_reset))
        }
        Button(
          onClick = { onApply(draft) },
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp),
        ) {
          val resultLabel = estimatedResultCount?.let {
            pluralStringResource(R.plurals.explore_title_count, it, it)
          } ?: stringResource(R.string.explore_top_results)
          Text(stringResource(R.string.explore_show_results, resultLabel))
        }
      }
    }
  }
}

@Composable
private fun <T> AdvancedChoiceSection(
  title: String,
  choices: List<T>,
  label: @Composable (T) -> String,
  selected: (T) -> Boolean,
  onSelected: (T) -> Unit,
) {
  Column(
    modifier = Modifier.padding(top = 14.dp),
  ) {
    Text(
      text = title,
      modifier = Modifier.padding(horizontal = 20.dp),
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
    )
    LazyRow(
      contentPadding = PaddingValues(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(choices, key = { it?.toString() ?: "all" }) { choice ->
        ExploreFilterBadge(
          label = label(choice),
          selected = selected(choice),
          onClick = { onSelected(choice) },
        )
      }
    }
  }
}

@Composable
private fun ProviderChoiceSection(
  providerState: LoadState<List<ExploreProvider>>,
  selectedProviderIds: Set<Int>,
  onSelectedProviderIds: (Set<Int>) -> Unit,
  onRetry: () -> Unit,
) {
  Column(modifier = Modifier.padding(top = 14.dp)) {
    Text(
      text = stringResource(R.string.explore_more_provider),
      modifier = Modifier.padding(horizontal = 20.dp),
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
    )
    Text(
      text = stringResource(R.string.explore_provider_region),
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodySmall,
    )
    when (providerState) {
      LoadState.Loading -> Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          strokeWidth = 2.dp,
        )
        Text(
          text = stringResource(R.string.explore_provider_loading),
          modifier = Modifier.padding(start = 10.dp),
          style = MaterialTheme.typography.bodySmall,
        )
      }
      LoadState.Empty -> Text(
        text = stringResource(R.string.explore_provider_unavailable),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
      )
      is LoadState.Error -> Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.explore_provider_unavailable),
          modifier = Modifier.weight(1f),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onRetry) {
          Text(stringResource(R.string.retry))
        }
      }
      is LoadState.Success -> LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        item("all-providers") {
          ExploreFilterBadge(
            label = stringResource(R.string.explore_provider_all),
            selected = selectedProviderIds.isEmpty(),
            onClick = { onSelectedProviderIds(emptySet()) },
          )
        }
        items(
          items = providerState.value.take(MAXIMUM_VISIBLE_PROVIDERS),
          key = ExploreProvider::id,
        ) { provider ->
          ExploreFilterBadge(
            label = provider.name,
            selected = provider.id in selectedProviderIds,
            onClick = {
              onSelectedProviderIds(selectedProviderIds.toggle(provider.id))
            },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreQuickPreviewSheet(
  item: ExploreMediaItem,
  onDismiss: () -> Unit,
  onViewDetails: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 24.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 8.5f),
      ) {
        TmdbImage(
          path = item.media.backdropPath ?: item.media.posterPath,
          contentDescription = null,
          modifier = Modifier.matchParentSize(),
          size = "w780",
        )
        Box(
          modifier = Modifier
            .matchParentSize()
            .background(
              Brush.verticalGradient(
                listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
              ),
            ),
        )
        Text(
          text = stringResource(R.string.explore_quick_preview),
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(16.dp),
          color = MaterialTheme.filmeraColors.onImage,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Column(
        modifier = Modifier.padding(horizontal = 20.dp),
      ) {
        Text(
          text = item.media.title,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = stringResource(
            R.string.explore_media_metadata,
            item.media.voteAverage,
            item.media.releaseDate?.take(4)
              ?: stringResource(R.string.not_available_short),
            item.media.originCountries.firstOrNull()
              ?: stringResource(R.string.explore_global),
          ),
          modifier = Modifier.padding(top = 6.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
        )
        Text(
          text = item.media.overview,
          modifier = Modifier.padding(top = 12.dp),
          maxLines = 5,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodyMedium,
        )
        Button(
          onClick = onViewDetails,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .heightIn(min = 48.dp),
        ) {
          Text(stringResource(R.string.explore_view_details))
        }
      }
    }
  }
}

@Composable
private fun ExploreGridLoadMoreEffect(
  itemCount: Int,
  canLoadMore: Boolean,
  gridState: LazyGridState,
  onLoadMore: () -> Unit,
) {
  LaunchedEffect(itemCount, canLoadMore) {
    snapshotFlow {
      gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
    }
      .distinctUntilChanged()
      .collect { lastVisible ->
        if (canLoadMore && itemCount > 0 && lastVisible >= itemCount - 5) {
          onLoadMore()
        }
      }
  }
}

@Composable
private fun ExploreListLoadMoreEffect(
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

@Composable
private fun ExploreScrollPersistenceEffect(
  layoutMode: ExploreLayoutMode,
  gridState: LazyGridState,
  listIndex: () -> Int,
  listOffset: () -> Int,
  onScrollChanged: (ExploreLayoutMode, Int, Int) -> Unit,
) {
  LaunchedEffect(layoutMode, gridState) {
    snapshotFlow {
      when (layoutMode) {
        ExploreLayoutMode.GRID ->
          gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        ExploreLayoutMode.LIST -> listIndex() to listOffset()
      }
    }
      .distinctUntilChanged()
      .collectLatest { (index, offset) ->
        delay(SCROLL_PERSISTENCE_DELAY_MILLIS)
        onScrollChanged(layoutMode, index, offset)
      }
  }
}

private fun ExploreFilterState.relaxMostRestrictiveFilter(): ExploreFilterState =
  when {
    minimumRating != null -> copy(minimumRating = null)
    minimumVoteCount != null -> copy(minimumVoteCount = null)
    providerIds.isNotEmpty() -> copy(providerIds = emptySet())
    categories.isNotEmpty() -> copy(categories = emptySet())
    region != ExploreRegion.ALL -> copy(region = ExploreRegion.ALL)
    year != ExploreYear.ALL -> copy(year = ExploreYear.ALL)
    else -> ExploreFilterState()
  }

private fun <T> Set<T>.toggle(value: T): Set<T> =
  if (value in this) this - value else this + value

private fun MediaItem.compactOriginMetadata(globalLabel: String): String =
  listOfNotNull(
    releaseDate?.take(4),
    originCountries.firstOrNull(),
  ).joinToString(" · ").ifBlank { globalLabel }

private val ExploreContentType.supportsTvStatus: Boolean
  get() = this in setOf(
    ExploreContentType.TV_SERIES,
    ExploreContentType.ANIME,
    ExploreContentType.ENTERTAINMENT,
  )

@get:StringRes
private val ExploreSort.labelResource: Int
  get() = when (this) {
    ExploreSort.HOT -> R.string.explore_sort_hot
    ExploreSort.LATEST -> R.string.explore_sort_latest
    ExploreSort.TOP_RATED -> R.string.explore_sort_top_rated
    ExploreSort.POPULARITY -> R.string.explore_sort_popularity
  }

@get:StringRes
private val ExploreContentType.labelResource: Int
  get() = when (this) {
    ExploreContentType.ALL -> R.string.explore_type_all
    ExploreContentType.MOVIES -> R.string.explore_type_movies
    ExploreContentType.TV_SERIES -> R.string.explore_type_series
    ExploreContentType.ANIME -> R.string.explore_type_anime
    ExploreContentType.ENTERTAINMENT -> R.string.explore_type_entertainment
    ExploreContentType.DOCUMENTARY -> R.string.explore_type_documentary
  }

@get:StringRes
private val ExploreCategory.labelResource: Int
  get() = when (this) {
    ExploreCategory.ACTION -> R.string.explore_category_action
    ExploreCategory.DRAMA -> R.string.explore_category_drama
    ExploreCategory.ROMANCE -> R.string.explore_category_romance
    ExploreCategory.COMEDY -> R.string.explore_category_comedy
    ExploreCategory.FANTASY -> R.string.explore_category_fantasy
    ExploreCategory.THRILLER -> R.string.explore_category_thriller
    ExploreCategory.ADVENTURE -> R.string.explore_category_adventure
    ExploreCategory.CRIME -> R.string.explore_category_crime
    ExploreCategory.MYSTERY -> R.string.explore_category_mystery
    ExploreCategory.SCIENCE_FICTION -> R.string.explore_category_science_fiction
    ExploreCategory.FAMILY -> R.string.explore_category_family
    ExploreCategory.HORROR -> R.string.explore_category_horror
    ExploreCategory.HISTORY -> R.string.explore_category_history
    ExploreCategory.MUSIC -> R.string.explore_category_music
    ExploreCategory.WAR -> R.string.explore_category_war
    ExploreCategory.WESTERN -> R.string.explore_category_western
  }

@get:StringRes
private val ExploreRegion.labelResource: Int
  get() = when (this) {
    ExploreRegion.ALL -> R.string.explore_region_all
    ExploreRegion.INDONESIA -> R.string.explore_region_indonesia
    ExploreRegion.KOREA -> R.string.explore_region_korea
    ExploreRegion.JAPAN -> R.string.explore_region_japan
    ExploreRegion.CHINA -> R.string.explore_region_china
    ExploreRegion.INDIA -> R.string.explore_region_india
    ExploreRegion.AMERICA -> R.string.explore_region_america
    ExploreRegion.THAILAND -> R.string.explore_region_thailand
    ExploreRegion.UNITED_KINGDOM -> R.string.explore_region_united_kingdom
    ExploreRegion.EUROPE -> R.string.explore_region_europe
    ExploreRegion.OTHER -> R.string.explore_region_other
  }

@get:StringRes
private val ExploreYear.labelResource: Int
  get() = when (this) {
    ExploreYear.ALL -> R.string.explore_year_all
    ExploreYear.YEAR_2026 -> R.string.explore_year_2026
    ExploreYear.YEAR_2025 -> R.string.explore_year_2025
    ExploreYear.YEAR_2024 -> R.string.explore_year_2024
    ExploreYear.YEAR_2023 -> R.string.explore_year_2023
    ExploreYear.YEARS_2020S -> R.string.explore_year_2020s
    ExploreYear.YEARS_2010S -> R.string.explore_year_2010s
    ExploreYear.YEARS_2000S -> R.string.explore_year_2000s
    ExploreYear.BEFORE_2000 -> R.string.explore_year_before_2000
  }

@get:StringRes
private val ExploreLanguage.labelResource: Int
  get() = when (this) {
    ExploreLanguage.ALL -> R.string.explore_language_all
    ExploreLanguage.ENGLISH -> R.string.explore_language_english
    ExploreLanguage.INDONESIAN -> R.string.explore_language_indonesian
    ExploreLanguage.KOREAN -> R.string.explore_language_korean
    ExploreLanguage.JAPANESE -> R.string.explore_language_japanese
    ExploreLanguage.CHINESE -> R.string.explore_language_chinese
    ExploreLanguage.HINDI -> R.string.explore_language_hindi
    ExploreLanguage.THAI -> R.string.explore_language_thai
    ExploreLanguage.SPANISH -> R.string.explore_language_spanish
    ExploreLanguage.FRENCH -> R.string.explore_language_french
  }

@get:StringRes
private val ExploreAvailability.labelResource: Int
  get() = when (this) {
    ExploreAvailability.ANY -> R.string.explore_availability_any
    ExploreAvailability.STREAM -> R.string.explore_availability_stream
    ExploreAvailability.FREE -> R.string.explore_availability_free
    ExploreAvailability.WITH_ADS -> R.string.explore_availability_ads
    ExploreAvailability.RENT -> R.string.explore_availability_rent
    ExploreAvailability.BUY -> R.string.explore_availability_buy
  }

@get:StringRes
private val ExploreDuration.labelResource: Int
  get() = when (this) {
    ExploreDuration.ANY -> R.string.explore_duration_any
    ExploreDuration.SHORT -> R.string.explore_duration_short
    ExploreDuration.STANDARD -> R.string.explore_duration_standard
    ExploreDuration.LONG -> R.string.explore_duration_long
  }

@get:StringRes
private val ExploreTvStatus.labelResource: Int
  get() = when (this) {
    ExploreTvStatus.ANY -> R.string.explore_status_any
    ExploreTvStatus.RETURNING -> R.string.explore_status_returning
    ExploreTvStatus.PLANNED -> R.string.explore_status_planned
    ExploreTvStatus.IN_PRODUCTION -> R.string.explore_status_production
    ExploreTvStatus.ENDED -> R.string.explore_status_ended
    ExploreTvStatus.CANCELED -> R.string.explore_status_canceled
    ExploreTvStatus.PILOT -> R.string.explore_status_pilot
  }

@get:StringRes
private val MediaClassification.labelResource: Int
  get() = when (this) {
    MediaClassification.ANIME -> R.string.explore_classification_anime
    MediaClassification.ENTERTAINMENT -> R.string.explore_classification_entertainment
    MediaClassification.DOCUMENTARY -> R.string.explore_classification_documentary
  }

private const val MAXIMUM_VISIBLE_PROVIDERS = 18
private const val SCROLL_PERSISTENCE_DELAY_MILLIS = 140L
private val FILTER_LABEL_WIDTH = 72.dp

private val previewExploreContent = ExploreContent(
  items = listOf(
    ExploreMediaItem(
      media = previewExploreMedia(
        id = 1,
        title = "The Last Horizon",
        type = MediaType.MOVIE,
        country = "US",
      ),
      score = 0.92,
      classifications = emptySet(),
    ),
    ExploreMediaItem(
      media = previewExploreMedia(
        id = 2,
        title = "Seoul After Midnight",
        type = MediaType.TV_SHOW,
        country = "KR",
      ),
      score = 0.89,
      classifications = setOf(MediaClassification.ENTERTAINMENT),
    ),
    ExploreMediaItem(
      media = previewExploreMedia(
        id = 3,
        title = "Skybound Chronicle",
        type = MediaType.TV_SHOW,
        country = "JP",
      ),
      score = 0.86,
      classifications = setOf(MediaClassification.ANIME),
    ),
  ),
  currentPage = 1,
  totalResults = 1_248,
  hasMore = true,
  isPartial = false,
)

private fun previewExploreMedia(
  id: Int,
  title: String,
  type: MediaType,
  country: String,
): MediaItem =
  MediaItem(
    id = id,
    type = type,
    title = title,
    originalTitle = title,
    overview = "A cinematic story about people crossing borders, ambitions, and unexpected worlds.",
    posterPath = null,
    backdropPath = null,
    releaseDate = "2026-04-12",
    voteAverage = 8.4,
    voteCount = 3_200,
    popularity = 128.0,
    adult = false,
    originalLanguage = if (country == "JP") "ja" else "en",
    genreIds = listOf(18, 12),
    originCountries = listOf(country),
  )

@Preview(name = "Explore compact", showBackground = true, widthDp = 390, heightDp = 840)
@Composable
private fun ExploreCompactPreview() {
  FilmeraTheme {
    ExploreTab(
      state = ExploreUiState(
        contentState = LoadState.Success(previewExploreContent),
      ),
      onAction = {},
      onMediaClick = {},
      onScrollStateChanged = {},
    )
  }
}

@Preview(
  name = "Explore expanded dark",
  showBackground = true,
  widthDp = 900,
  heightDp = 700,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ExploreExpandedDarkPreview() {
  FilmeraTheme {
    ExploreTab(
      state = ExploreUiState(
        contentState = LoadState.Success(previewExploreContent),
        layoutMode = ExploreLayoutMode.LIST,
      ),
      onAction = {},
      onMediaClick = {},
      onScrollStateChanged = {},
    )
  }
}
