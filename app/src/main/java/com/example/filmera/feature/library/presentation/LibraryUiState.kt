package com.example.filmera.feature.library.presentation

import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.library.domain.LibraryItem
import com.example.filmera.feature.library.domain.WatchStatus

enum class LibraryTab {
  WATCHLIST,
  WATCHING,
  COMPLETED,
  FAVORITES,
}

enum class LibraryMediaFilter {
  ALL,
  MOVIES,
  TV_SHOWS,
}

enum class LibrarySort {
  RECENTLY_ADDED,
  OLDEST_ADDED,
  TITLE_ASCENDING,
  HIGHEST_RATED,
  RELEASE_DATE,
}

data class LibrarySummary(
  val totalCount: Int = 0,
  val movieCount: Int = 0,
  val tvShowCount: Int = 0,
  val watchingCount: Int = 0,
)

data class LibraryUiState(
  val contentState: LoadState<List<LibraryItem>> = LoadState.Loading,
  val selectedTab: LibraryTab = LibraryTab.WATCHLIST,
  val selectedMediaFilter: LibraryMediaFilter = LibraryMediaFilter.ALL,
  val selectedSort: LibrarySort = LibrarySort.RECENTLY_ADDED,
  val query: String = "",
  val isSearching: Boolean = false,
  val mutationKeys: Set<MediaKey> = emptySet(),
  val summary: LibrarySummary = LibrarySummary(),
)

sealed interface LibraryUiEvent {
  data class TabSelected(val tab: LibraryTab) : LibraryUiEvent
  data class MediaFilterSelected(val filter: LibraryMediaFilter) : LibraryUiEvent
  data class SortSelected(val sort: LibrarySort) : LibraryUiEvent
  data class QueryChanged(val query: String) : LibraryUiEvent
  data class WatchStatusChanged(
    val item: LibraryItem,
    val status: WatchStatus,
  ) : LibraryUiEvent
  data class FavoriteToggled(val item: LibraryItem) : LibraryUiEvent
  data class WatchStatusRemoved(val item: LibraryItem) : LibraryUiEvent
  data object SearchOpened : LibraryUiEvent
  data object SearchClosed : LibraryUiEvent
  data object UndoLastMutation : LibraryUiEvent
  data object Retry : LibraryUiEvent
}

enum class LibraryMutationMessage {
  ADDED_TO_WATCHLIST,
  MOVED_TO_WATCHING,
  MOVED_TO_COMPLETED,
  REMOVED_FROM_LIBRARY,
  ADDED_TO_FAVORITES,
  REMOVED_FROM_FAVORITES,
}

sealed interface LibraryEffect {
  data class ShowUndo(val message: LibraryMutationMessage) : LibraryEffect
  data object ShowWriteError : LibraryEffect
}

internal fun filterAndSortLibrary(
  items: List<LibraryItem>,
  tab: LibraryTab,
  mediaFilter: LibraryMediaFilter,
  query: String,
  sort: LibrarySort,
): List<LibraryItem> {
  val normalizedQuery = query.trim()
  val filtered = items.asSequence()
    .filter { item ->
      when (tab) {
        LibraryTab.WATCHLIST -> item.watchStatus == WatchStatus.WATCHLIST
        LibraryTab.WATCHING -> item.watchStatus == WatchStatus.WATCHING
        LibraryTab.COMPLETED -> item.watchStatus == WatchStatus.COMPLETED
        LibraryTab.FAVORITES -> item.isFavorite
      }
    }
    .filter { item ->
      when (mediaFilter) {
        LibraryMediaFilter.ALL -> true
        LibraryMediaFilter.MOVIES -> item.media.type == MediaType.MOVIE
        LibraryMediaFilter.TV_SHOWS -> item.media.type == MediaType.TV_SHOW
      }
    }
    .filter { item ->
      normalizedQuery.isEmpty() ||
        item.media.title.contains(normalizedQuery, ignoreCase = true) ||
        item.media.originalTitle.contains(normalizedQuery, ignoreCase = true)
    }
    .toList()

  return when (sort) {
    LibrarySort.RECENTLY_ADDED -> filtered.sortedByDescending(LibraryItem::addedAt)
    LibrarySort.OLDEST_ADDED -> filtered.sortedBy(LibraryItem::addedAt)
    LibrarySort.TITLE_ASCENDING -> filtered.sortedBy { it.media.title.lowercase() }
    LibrarySort.HIGHEST_RATED -> filtered.sortedByDescending { it.media.voteAverage }
    LibrarySort.RELEASE_DATE -> filtered.sortedByDescending { it.media.releaseDate.orEmpty() }
  }
}
