package com.example.filmera.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.data.local.entity.FavoriteMovie
import com.example.filmera.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteMovieViewModel @Inject constructor(
  private val repository: FavoriteRepository
) : ViewModel() {

  val favorites: StateFlow<List<FavoriteMovie>> = repository.getFavorites()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  suspend fun isFavorite(id: Int): Boolean = repository.isFavorite(id)

  fun toggleFavorite(movie: FavoriteMovie) {
    viewModelScope.launch {
      if (repository.isFavorite(movie.id)) {
        repository.remove(movie)
      } else {
        repository.add(movie)
      }
    }
  }
}