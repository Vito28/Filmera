package com.example.filmera.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.model.search.SearchResult
import com.example.filmera.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
  private val searchRepo: SearchRepository
) : ViewModel() {

  private val _movieResults = MutableStateFlow<List<SearchResult>>(emptyList())
  val movieResults: StateFlow<List<SearchResult>> = _movieResults

  private val _tvResults = MutableStateFlow<List<SearchResult>>(emptyList())
  val tvResults: StateFlow<List<SearchResult>> = _tvResults

  private val _multiResults = MutableStateFlow<List<SearchResult>>(emptyList())
  val multiResults: StateFlow<List<SearchResult>> = _multiResults

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error

  private val _searchResults = MutableStateFlow<List<Movie>>(emptyList())
  val searchResults: StateFlow<List<Movie>> = _searchResults.asStateFlow()

  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

  private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
  val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

  fun searchMovies(query: String) {
    viewModelScope.launch {
      _isSearching.value = true
      try {
        val results = movieRepository.searchMovies(query)
        _searchResults.value = results

        // Add to search history
        val currentHistory = _searchHistory.value.toMutableList()
        if (!currentHistory.contains(query)) {
          currentHistory.add(0, query)
          // Keep only last 10 searches
          if (currentHistory.size > 10) {
            currentHistory.removeAt(currentHistory.size - 1)
          }
          _searchHistory.value = currentHistory
        }
      } catch (e: Exception) {
        // Handle error
        _searchResults.value = emptyList()
      } finally {
        _isSearching.value = false
      }
    }
  }

  fun clearSearch() {
    _searchResults.value = emptyList()
    _isSearching.value = false
  }

  fun clearSearchHistory() {
    _searchHistory.value = emptyList()
  }

  fun searchTv(query: String, page: Int = 1, language: String? = null) {
    launchWithLoading {
      _tvResults.value = searchRepo.searchTv(query, page, language)
    }
  }

  fun searchMulti(query: String, page: Int = 1, language: String? = null) {
    launchWithLoading {
      _multiResults.value = searchRepo.searchMulti(query, page, language)
    }
  }

  private fun launchWithLoading(block: suspend () -> Unit) {
    viewModelScope.launch {
      try {
        _isLoading.value = true
        _error.value = null
        block()
      } catch (e: Exception) {
        _error.value = e.localizedMessage ?: "Unknown error"
      } finally {
        _isLoading.value = false
      }
    }
  }
}
