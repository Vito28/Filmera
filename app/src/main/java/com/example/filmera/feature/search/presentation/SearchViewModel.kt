package com.example.filmera.feature.search.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.library.domain.LibraryRepository
import com.example.filmera.feature.library.domain.WatchStatus
import com.example.filmera.feature.search.domain.RecentSearchRepository
import com.example.filmera.feature.search.domain.SearchContent
import com.example.filmera.feature.search.domain.SearchRepository
import com.example.filmera.feature.search.domain.SearchRequest
import com.example.filmera.feature.search.domain.SearchType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
  private val searchRepository: SearchRepository,
  private val recentSearchRepository: RecentSearchRepository,
  private val libraryRepository: LibraryRepository,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
  private val mutableUiState = MutableStateFlow(
    SearchUiState(
      query = savedStateHandle.get<String>(QUERY_KEY).orEmpty(),
      selectedType = savedStateHandle.get<String>(SEARCH_TYPE_KEY).toSearchType(),
    ),
  )
  val uiState: StateFlow<SearchUiState> = mutableUiState.asStateFlow()

  private var searchJob: Job? = null
  private var discoveryJob: Job? = null

  init {
    observeRecentSearches()
    observeLibrary()
    loadDiscovery()
    if (mutableUiState.value.query.isNotBlank()) {
      scheduleSearch(debounce = false, preserveResults = false)
    }
  }

  fun onEvent(event: SearchUiEvent) {
    when (event) {
      is SearchUiEvent.QueryChanged -> updateQuery(event.query)
      is SearchUiEvent.TypeSelected -> selectType(event.type)
      is SearchUiEvent.SuggestionSelected -> selectSuggestedQuery(event.suggestion.text)
      is SearchUiEvent.RecentSearchSelected -> selectSuggestedQuery(event.query)
      is SearchUiEvent.RemoveRecentSearch -> removeRecentSearch(event.query)
      is SearchUiEvent.FavoriteToggled -> toggleFavorite(event.item)
      is SearchUiEvent.WatchlistToggled -> toggleWatchlist(event.item)
      SearchUiEvent.SearchSubmitted -> submitSearch()
      SearchUiEvent.ResultOpened -> saveCurrentQuery()
      SearchUiEvent.ClearQuery -> updateQuery("")
      SearchUiEvent.ClearRecentSearchesRequested -> mutableUiState.update {
        it.copy(showClearRecentConfirmation = true)
      }
      SearchUiEvent.ClearRecentSearchesConfirmed -> clearRecentSearches()
      SearchUiEvent.ClearRecentSearchesDismissed -> mutableUiState.update {
        it.copy(showClearRecentConfirmation = false)
      }
      SearchUiEvent.Retry -> retry()
      SearchUiEvent.LoadMore -> loadMore()
      SearchUiEvent.FavoriteErrorDismissed -> mutableUiState.update {
        it.copy(favoriteWriteError = null)
      }
      SearchUiEvent.LibraryErrorDismissed -> mutableUiState.update {
        it.copy(libraryWriteError = null)
      }
    }
  }

  private fun updateQuery(value: String) {
    savedStateHandle[QUERY_KEY] = value
    mutableUiState.update { state ->
      state.copy(
        query = value,
        resultsState = if (value.isBlank()) SearchResultsState.Idle else state.resultsState,
        paginationError = null,
      )
    }
    searchJob?.cancel()
    if (value.isNotBlank()) {
      scheduleSearch(debounce = true, preserveResults = false)
    }
  }

  private fun selectType(type: SearchType) {
    if (type == mutableUiState.value.selectedType) return

    savedStateHandle[SEARCH_TYPE_KEY] = type.name
    mutableUiState.update { it.copy(selectedType = type, paginationError = null) }
    searchJob?.cancel()
    if (mutableUiState.value.query.isNotBlank()) {
      scheduleSearch(debounce = false, preserveResults = true)
    }
  }

  private fun selectSuggestedQuery(query: String) {
    savedStateHandle[QUERY_KEY] = query
    mutableUiState.update { it.copy(query = query, paginationError = null) }
    submitSearch()
  }

  private fun submitSearch() {
    searchJob?.cancel()
    saveCurrentQuery()
    scheduleSearch(debounce = false, preserveResults = false)
  }

  private fun retry() {
    if (mutableUiState.value.query.isBlank()) {
      loadDiscovery()
    } else {
      searchJob?.cancel()
      scheduleSearch(
        debounce = false,
        preserveResults = mutableUiState.value.visibleContent != null,
      )
    }
  }

  private fun scheduleSearch(
    debounce: Boolean,
    preserveResults: Boolean,
  ) {
    val request = SearchRequest(
      query = mutableUiState.value.query.trim(),
      type = mutableUiState.value.selectedType,
    )
    if (request.query.isEmpty()) return

    searchJob = viewModelScope.launch {
      if (debounce) delay(SEARCH_DEBOUNCE_MILLIS)
      performSearch(request, preserveResults)
    }
  }

  private suspend fun performSearch(
    request: SearchRequest,
    preserveResults: Boolean,
  ) {
    val previous = mutableUiState.value.visibleContent.takeIf { preserveResults }
    mutableUiState.update {
      it.copy(
        resultsState = SearchResultsState.Loading(previous),
        isLoadingMore = false,
        paginationError = null,
      )
    }

    val nextState = when (val result = searchRepository.search(request)) {
      is DataResult.Success -> {
        if (result.value.isEmpty) {
          SearchResultsState.Empty(request.query)
        } else {
          SearchResultsState.Success(result.value)
        }
      }
      is DataResult.Error -> SearchResultsState.Error(result.error, previous)
    }
    if (
      mutableUiState.value.query.trim() == request.query &&
      mutableUiState.value.selectedType == request.type
    ) {
      mutableUiState.update { it.copy(resultsState = nextState) }
    }
  }

  private fun loadMore() {
    val current = (mutableUiState.value.resultsState as? SearchResultsState.Success)?.content
      ?: return
    if (
      !current.canLoadMore ||
      mutableUiState.value.isLoadingMore ||
      current.selectedType != mutableUiState.value.selectedType
    ) {
      return
    }

    viewModelScope.launch {
      mutableUiState.update { it.copy(isLoadingMore = true, paginationError = null) }
      val result = searchRepository.search(
        SearchRequest(
          query = current.query,
          type = current.selectedType,
          page = current.page + 1,
        ),
      )
      when (result) {
        is DataResult.Success -> mutableUiState.update { state ->
          val activeContent = (state.resultsState as? SearchResultsState.Success)?.content
          if (
            activeContent?.query == current.query &&
            activeContent.selectedType == current.selectedType
          ) {
            state.copy(
              resultsState = SearchResultsState.Success(
                activeContent.append(result.value),
              ),
              isLoadingMore = false,
            )
          } else {
            state.copy(isLoadingMore = false)
          }
        }
        is DataResult.Error -> mutableUiState.update {
          it.copy(isLoadingMore = false, paginationError = result.error)
        }
      }
    }
  }

  private fun loadDiscovery() {
    discoveryJob?.cancel()
    discoveryJob = viewModelScope.launch {
      mutableUiState.update { it.copy(discoveryState = LoadState.Loading) }
      val nextState = when (val result = searchRepository.loadDiscovery()) {
        is DataResult.Success -> {
          if (result.value.hasVisibleContent) LoadState.Success(result.value) else LoadState.Empty
        }
        is DataResult.Error -> LoadState.Error(result.error)
      }
      mutableUiState.update { it.copy(discoveryState = nextState) }
    }
  }

  private fun observeRecentSearches() {
    viewModelScope.launch {
      recentSearchRepository.observeRecentSearches()
        .catch { emit(emptyList()) }
        .collect { recentSearches ->
          mutableUiState.update { it.copy(recentSearches = recentSearches) }
        }
    }
  }

  private fun observeLibrary() {
    viewModelScope.launch {
      libraryRepository.observeLibrary()
        .catch { emit(emptyList()) }
        .collect { items ->
          mutableUiState.update {
            it.copy(
              favoriteKeys = items
                .filter { libraryItem -> libraryItem.isFavorite }
                .map { libraryItem -> libraryItem.media.key }
                .toSet(),
              watchlistKeys = items
                .filter { libraryItem -> libraryItem.watchStatus != WatchStatus.NONE }
                .map { libraryItem -> libraryItem.media.key }
                .toSet(),
            )
          }
        }
    }
  }

  private fun saveCurrentQuery() {
    val query = mutableUiState.value.query.trim()
    if (query.isEmpty()) return
    viewModelScope.launch { recentSearchRepository.save(query) }
  }

  private fun removeRecentSearch(query: String) {
    viewModelScope.launch { recentSearchRepository.remove(query) }
  }

  private fun clearRecentSearches() {
    mutableUiState.update { it.copy(showClearRecentConfirmation = false) }
    viewModelScope.launch { recentSearchRepository.clear() }
  }

  private fun toggleFavorite(item: MediaItem) {
    if (item.key in mutableUiState.value.favoriteMutations) return

    viewModelScope.launch {
      mutableUiState.update {
        it.copy(
          favoriteMutations = it.favoriteMutations + item.key,
          favoriteWriteError = null,
        )
      }
      try {
        val shouldBeFavorite = item.key !in mutableUiState.value.favoriteKeys
        libraryRepository.setFavorite(item, shouldBeFavorite)
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        mutableUiState.update { it.copy(favoriteWriteError = AppError.Unknown) }
      } finally {
        mutableUiState.update {
          it.copy(favoriteMutations = it.favoriteMutations - item.key)
        }
      }
    }
  }

  private fun toggleWatchlist(item: MediaItem) {
    if (item.key in mutableUiState.value.watchlistMutations) return

    viewModelScope.launch {
      mutableUiState.update {
        it.copy(
          watchlistMutations = it.watchlistMutations + item.key,
          libraryWriteError = null,
        )
      }
      try {
        val nextStatus = if (item.key in mutableUiState.value.watchlistKeys) {
          WatchStatus.NONE
        } else {
          WatchStatus.WATCHLIST
        }
        libraryRepository.setWatchStatus(item, nextStatus)
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        mutableUiState.update { it.copy(libraryWriteError = AppError.Unknown) }
      } finally {
        mutableUiState.update {
          it.copy(watchlistMutations = it.watchlistMutations - item.key)
        }
      }
    }
  }

  private fun SearchContent.append(next: SearchContent): SearchContent =
    copy(
      movies = (movies + next.movies).distinctBy(MediaItem::key),
      tvShows = (tvShows + next.tvShows).distinctBy(MediaItem::key),
      people = (people + next.people).distinctBy { it.id },
      collections = (collections + next.collections).distinctBy { it.id },
      page = next.page,
      totalPages = next.totalPages,
      hasPartialFailures = hasPartialFailures || next.hasPartialFailures,
    )

  private fun String?.toSearchType(): SearchType =
    SearchType.entries.firstOrNull { it.name == this } ?: SearchType.ALL

  private companion object {
    const val QUERY_KEY = "search_query"
    const val SEARCH_TYPE_KEY = "search_type"
    const val SEARCH_DEBOUNCE_MILLIS = 300L
  }
}
