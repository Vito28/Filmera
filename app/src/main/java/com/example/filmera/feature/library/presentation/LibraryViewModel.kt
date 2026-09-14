package com.example.filmera.feature.library.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.core.common.AppError
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.library.domain.LibraryItem
import com.example.filmera.feature.library.domain.LibraryRepository
import com.example.filmera.feature.library.domain.WatchStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel @Inject constructor(
  private val repository: LibraryRepository,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
  private val selectedTab = MutableStateFlow(
    savedStateHandle.get<String>(SELECTED_TAB_KEY).toEnumOrDefault(LibraryTab.WATCHLIST),
  )
  private val selectedMediaFilter = MutableStateFlow(
    savedStateHandle.get<String>(MEDIA_FILTER_KEY).toEnumOrDefault(LibraryMediaFilter.ALL),
  )
  private val selectedSort = MutableStateFlow(
    savedStateHandle.get<String>(SORT_KEY).toEnumOrDefault(LibrarySort.RECENTLY_ADDED),
  )
  private val query = MutableStateFlow(savedStateHandle.get<String>(QUERY_KEY).orEmpty())
  private val isSearching = MutableStateFlow(
    savedStateHandle.get<Boolean>(IS_SEARCHING_KEY) ?: query.value.isNotEmpty(),
  )
  private val mutationKeys = MutableStateFlow<Set<MediaKey>>(emptySet())
  private val observationGeneration = MutableStateFlow(0)
  private val mutableEffects = Channel<LibraryEffect>(Channel.BUFFERED)
  val effects = mutableEffects.receiveAsFlow()

  private var lastUndoItem: LibraryItem? = null

  private val libraryState = observationGeneration.flatMapLatest {
    repository.observeLibrary()
      .map<List<LibraryItem>, LoadState<List<LibraryItem>>> { LoadState.Success(it) }
      .onStart { emit(LoadState.Loading) }
      .catch { emit(LoadState.Error(AppError.Unknown)) }
  }

  private val controls = combine(
    selectedTab,
    selectedMediaFilter,
    selectedSort,
    query,
    isSearching,
  ) { tab, filter, sort, activeQuery, searching ->
    LibraryControls(
      tab = tab,
      filter = filter,
      sort = sort,
      query = activeQuery,
      isSearching = searching,
    )
  }

  val uiState: StateFlow<LibraryUiState> = combine(
    libraryState,
    controls,
    mutationKeys,
  ) { state, controls, mutations ->
    val allItems = (state as? LoadState.Success)?.value.orEmpty()
    LibraryUiState(
      contentState = when (state) {
        LoadState.Loading -> LoadState.Loading
        LoadState.Empty -> LoadState.Success(emptyList())
        is LoadState.Error -> state
        is LoadState.Success -> LoadState.Success(
          filterAndSortLibrary(
            items = state.value,
            tab = controls.tab,
            mediaFilter = controls.filter,
            query = controls.query,
            sort = controls.sort,
          ),
        )
      },
      selectedTab = controls.tab,
      selectedMediaFilter = controls.filter,
      selectedSort = controls.sort,
      query = controls.query,
      isSearching = controls.isSearching,
      mutationKeys = mutations,
      summary = LibrarySummary(
        totalCount = allItems.size,
        movieCount = allItems.count { it.media.type == MediaType.MOVIE },
        tvShowCount = allItems.count { it.media.type == MediaType.TV_SHOW },
        watchingCount = allItems.count { it.watchStatus == WatchStatus.WATCHING },
      ),
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
    initialValue = LibraryUiState(
      selectedTab = selectedTab.value,
      selectedMediaFilter = selectedMediaFilter.value,
      selectedSort = selectedSort.value,
      query = query.value,
      isSearching = isSearching.value,
    ),
  )

  fun onEvent(event: LibraryUiEvent) {
    when (event) {
      is LibraryUiEvent.TabSelected -> selectTab(event.tab)
      is LibraryUiEvent.MediaFilterSelected -> selectMediaFilter(event.filter)
      is LibraryUiEvent.SortSelected -> selectSort(event.sort)
      is LibraryUiEvent.QueryChanged -> updateQuery(event.query)
      is LibraryUiEvent.WatchStatusChanged -> changeWatchStatus(event.item, event.status)
      is LibraryUiEvent.FavoriteToggled -> toggleFavorite(event.item)
      is LibraryUiEvent.WatchStatusRemoved -> removeWatchStatus(event.item)
      LibraryUiEvent.SearchOpened -> openSearch()
      LibraryUiEvent.SearchClosed -> closeSearch()
      LibraryUiEvent.UndoLastMutation -> undoLastMutation()
      LibraryUiEvent.Retry -> observationGeneration.value += 1
    }
  }

  private fun selectTab(tab: LibraryTab) {
    savedStateHandle[SELECTED_TAB_KEY] = tab.name
    selectedTab.value = tab
  }

  private fun selectMediaFilter(filter: LibraryMediaFilter) {
    savedStateHandle[MEDIA_FILTER_KEY] = filter.name
    selectedMediaFilter.value = filter
  }

  private fun selectSort(sort: LibrarySort) {
    savedStateHandle[SORT_KEY] = sort.name
    selectedSort.value = sort
  }

  private fun updateQuery(value: String) {
    savedStateHandle[QUERY_KEY] = value
    query.value = value
  }

  private fun openSearch() {
    savedStateHandle[IS_SEARCHING_KEY] = true
    isSearching.value = true
  }

  private fun closeSearch() {
    savedStateHandle[QUERY_KEY] = ""
    savedStateHandle[IS_SEARCHING_KEY] = false
    query.value = ""
    isSearching.value = false
  }

  private fun changeWatchStatus(
    item: LibraryItem,
    status: WatchStatus,
  ) {
    mutate(
      item = item,
      message = status.message(),
    ) {
      repository.setWatchStatus(item.media, status)
    }
  }

  private fun toggleFavorite(item: LibraryItem) {
    mutate(
      item = item,
      message = if (item.isFavorite) {
        LibraryMutationMessage.REMOVED_FROM_FAVORITES
      } else {
        LibraryMutationMessage.ADDED_TO_FAVORITES
      },
    ) {
      repository.setFavorite(item.media, !item.isFavorite)
    }
  }

  private fun removeWatchStatus(item: LibraryItem) {
    if (item.watchStatus == WatchStatus.NONE) return

    mutate(item, LibraryMutationMessage.REMOVED_FROM_LIBRARY) {
      repository.setWatchStatus(item.media, WatchStatus.NONE)
    }
  }

  private fun mutate(
    item: LibraryItem,
    message: LibraryMutationMessage,
    operation: suspend () -> Unit,
  ) {
    if (item.media.key in mutationKeys.value) return

    viewModelScope.launch {
      mutationKeys.update { it + item.media.key }
      try {
        operation()
        lastUndoItem = item
        mutableEffects.send(LibraryEffect.ShowUndo(message))
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        mutableEffects.send(LibraryEffect.ShowWriteError)
      } finally {
        mutationKeys.update { it - item.media.key }
      }
    }
  }

  private fun undoLastMutation() {
    val item = lastUndoItem ?: return
    lastUndoItem = null
    viewModelScope.launch {
      try {
        repository.restore(item)
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        mutableEffects.send(LibraryEffect.ShowWriteError)
      }
    }
  }

  private fun WatchStatus.message(): LibraryMutationMessage =
    when (this) {
      WatchStatus.NONE -> LibraryMutationMessage.REMOVED_FROM_LIBRARY
      WatchStatus.WATCHLIST -> LibraryMutationMessage.ADDED_TO_WATCHLIST
      WatchStatus.WATCHING -> LibraryMutationMessage.MOVED_TO_WATCHING
      WatchStatus.COMPLETED -> LibraryMutationMessage.MOVED_TO_COMPLETED
    }

  private data class LibraryControls(
    val tab: LibraryTab,
    val filter: LibraryMediaFilter,
    val sort: LibrarySort,
    val query: String,
    val isSearching: Boolean,
  )

  private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default

  private companion object {
    const val SELECTED_TAB_KEY = "library_selected_tab"
    const val MEDIA_FILTER_KEY = "library_media_filter"
    const val SORT_KEY = "library_sort"
    const val QUERY_KEY = "library_query"
    const val IS_SEARCHING_KEY = "library_is_searching"
  }
}
