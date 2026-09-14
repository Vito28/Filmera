package com.example.filmera.feature.search.presentation

import com.example.filmera.core.common.AppError
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.search.domain.SearchContent
import com.example.filmera.feature.search.domain.SearchDiscovery
import com.example.filmera.feature.search.domain.SearchType

data class SearchUiState(
  val query: String = "",
  val selectedType: SearchType = SearchType.ALL,
  val recentSearches: List<String> = emptyList(),
  val discoveryState: LoadState<SearchDiscovery> = LoadState.Loading,
  val resultsState: SearchResultsState = SearchResultsState.Idle,
  val favoriteKeys: Set<MediaKey> = emptySet(),
  val favoriteMutations: Set<MediaKey> = emptySet(),
  val favoriteWriteError: AppError? = null,
  val watchlistKeys: Set<MediaKey> = emptySet(),
  val watchlistMutations: Set<MediaKey> = emptySet(),
  val libraryWriteError: AppError? = null,
  val isLoadingMore: Boolean = false,
  val paginationError: AppError? = null,
  val showClearRecentConfirmation: Boolean = false,
) {
  val isSearching: Boolean
    get() = resultsState is SearchResultsState.Loading

  val visibleContent: SearchContent?
    get() = when (val state = resultsState) {
      is SearchResultsState.Loading -> state.previous
      is SearchResultsState.Success -> state.content
      is SearchResultsState.Error -> state.previous
      SearchResultsState.Idle,
      is SearchResultsState.Empty,
      -> null
    }

  val suggestions: List<SearchSuggestion>
    get() {
      val normalizedQuery = query.trim()
      if (normalizedQuery.isEmpty()) return emptyList()

      val discovery = (discoveryState as? LoadState.Success)?.value
      val content = visibleContent
      return buildList {
        recentSearches.forEach { add(SearchSuggestion(it, SearchSuggestionType.RECENT)) }
        discovery?.trendingQueries?.forEach {
          add(SearchSuggestion(it, SearchSuggestionType.TITLE))
        }
        discovery?.genres?.forEach {
          add(SearchSuggestion(it.name, SearchSuggestionType.GENRE))
        }
        content?.movies?.forEach {
          add(SearchSuggestion(it.title, SearchSuggestionType.TITLE))
        }
        content?.tvShows?.forEach {
          add(SearchSuggestion(it.title, SearchSuggestionType.TITLE))
        }
        content?.people?.forEach {
          add(SearchSuggestion(it.name, SearchSuggestionType.PERSON))
        }
        content?.collections?.forEach {
          add(SearchSuggestion(it.name, SearchSuggestionType.COLLECTION))
        }
      }
        .filter { it.text.contains(normalizedQuery, ignoreCase = true) }
        .distinctBy { it.text.lowercase() }
        .sortedWith(
          compareByDescending<SearchSuggestion> {
            it.text.equals(normalizedQuery, ignoreCase = true)
          }.thenBy { it.text.length },
        )
        .take(6)
    }
}

sealed interface SearchResultsState {
  data object Idle : SearchResultsState

  data class Loading(
    val previous: SearchContent? = null,
  ) : SearchResultsState

  data class Success(
    val content: SearchContent,
  ) : SearchResultsState

  data class Empty(
    val query: String,
  ) : SearchResultsState

  data class Error(
    val error: AppError,
    val previous: SearchContent? = null,
  ) : SearchResultsState
}

data class SearchSuggestion(
  val text: String,
  val type: SearchSuggestionType,
)

enum class SearchSuggestionType {
  RECENT,
  TITLE,
  PERSON,
  GENRE,
  COLLECTION,
}

sealed interface SearchUiEvent {
  data class QueryChanged(val query: String) : SearchUiEvent
  data class TypeSelected(val type: SearchType) : SearchUiEvent
  data class SuggestionSelected(val suggestion: SearchSuggestion) : SearchUiEvent
  data class RecentSearchSelected(val query: String) : SearchUiEvent
  data class RemoveRecentSearch(val query: String) : SearchUiEvent
  data class FavoriteToggled(val item: MediaItem) : SearchUiEvent
  data class WatchlistToggled(val item: MediaItem) : SearchUiEvent
  data object SearchSubmitted : SearchUiEvent
  data object ResultOpened : SearchUiEvent
  data object ClearQuery : SearchUiEvent
  data object ClearRecentSearchesRequested : SearchUiEvent
  data object ClearRecentSearchesConfirmed : SearchUiEvent
  data object ClearRecentSearchesDismissed : SearchUiEvent
  data object Retry : SearchUiEvent
  data object LoadMore : SearchUiEvent
  data object FavoriteErrorDismissed : SearchUiEvent
  data object LibraryErrorDismissed : SearchUiEvent
}
