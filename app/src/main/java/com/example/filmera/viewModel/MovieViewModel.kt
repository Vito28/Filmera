package com.example.filmera.viewModel

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.model.Movie
import com.example.filmera.model.credit.CreditsResponse
import com.example.filmera.model.detail.MovieDetail
import com.example.filmera.model.video.VideoItem
import com.example.filmera.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieViewModel @Inject constructor (
  private val repository: MovieRepository
) : ViewModel() {
  private val _popularMovies = MutableStateFlow<List<Movie>>(emptyList())
  val popularMovies: StateFlow<List<Movie>> = _popularMovies

  private val _nowPlayingMovies = MutableStateFlow<List<Movie>>(emptyList())
  val nowPlayingMovies: StateFlow<List<Movie>> = _nowPlayingMovies

  private val _upcomingMovies = MutableStateFlow<List<Movie>>(emptyList())
  val upcomingMovies: StateFlow<List<Movie>> = _upcomingMovies

  private val _topRatedMovies = MutableStateFlow<List<Movie>>(emptyList())
  val topRatedMovies: StateFlow<List<Movie>> = _topRatedMovies

  private val _trendingMovies = MutableStateFlow<List<Movie>>(emptyList())
  val trendingMovies: StateFlow<List<Movie>> = _trendingMovies

  private val _movieDetail = MutableStateFlow<MovieDetail?>(null)
  val movieDetail: StateFlow<MovieDetail?> = _movieDetail

  // === TV ===
  private val _westernTv = MutableStateFlow<List<Movie>>(emptyList())
  val westernTv: StateFlow<List<Movie>> = _westernTv

  private val _kDrama = MutableStateFlow<List<Movie>>(emptyList())
  val kDrama: StateFlow<List<Movie>> = _kDrama

  private val _shortMovies = MutableStateFlow<List<Movie>>(emptyList())
  val shortMovies: StateFlow<List<Movie>> = _shortMovies

  private val _animeMovies = MutableStateFlow<List<Movie>>(emptyList())
  val animeMovies: StateFlow<List<Movie>> = _animeMovies

  private val _recommendations = MutableStateFlow<List<Movie>>(emptyList())
  val recommendations: StateFlow<List<Movie>> = _recommendations

  private val _movieTrailer = MutableStateFlow<VideoItem?>(null)
  val movieTrailer: StateFlow<VideoItem?> = _movieTrailer

  private val _credits = MutableStateFlow<CreditsResponse?>(null)
  val movieCredits: StateFlow<CreditsResponse?> = _credits

  private val _isLoading = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = _isLoading

  private val _movieDetailMap = mutableStateMapOf<Int, MovieDetail?>()
  val movieDetailMap: Map<Int, MovieDetail>
    get() = _movieDetailMap.filterValues { it != null }.mapValues { it.value!! }

  // === LOADERS ===
  fun loadPopularMovies() = launch { _popularMovies.value = repository.getPopularMovies() }
  fun loadNowPlayingMovies() = launch { _nowPlayingMovies.value = repository.getNowPlayingMovies() }
  fun loadUpcomingMovies() = launch { _upcomingMovies.value = repository.getUpcomingMovies() }
  fun loadTopRatedMovies() = launch { _topRatedMovies.value = repository.getTopRatedMovies() }
  fun loadTrendingMovies() = launch { _trendingMovies.value = repository.getTrendingMovies() }

  fun loadWesternTv() = launch { _westernTv.value = repository.getWesternTv() }
  fun loadKDrama() = launch { _kDrama.value = repository.getKDrama() }
  fun loadShortMovies() = launch { _shortMovies.value = repository.getShortMovies() }
  fun loadAnimeMovies() = launch { _animeMovies.value = repository.getAnimeMovies() }

  fun loadRecommendations(movieId: Int) = launch {
    _recommendations.value = repository.getRecommendations(movieId)
  }

  fun loadMovieTrailer(movieId: Int) = launch {
      _movieTrailer.value = repository.getMovieVideo(movieId)
  }

  fun loadCredits(movieId: Int) = launch {
      val response = repository.getMovieCredits(movieId)
      _credits.value = response
  }

  fun loadMovieDetail(movieId: Int) {
    viewModelScope.launch {
      if (_movieDetailMap.containsKey(movieId)) return@launch // cached

      try {
        val result = repository.getMovieDetail(movieId)
        _movieDetailMap[movieId] = result
      } catch (e: Exception) {
        // log error if needed
      }
    }
  }

  fun loadAllHomeMovies() {
    viewModelScope.launch {
      _isLoading.value = true

      try {
        val jobs = listOf(
          launch { loadPopularMovies() },
          launch { loadNowPlayingMovies() },
          launch { loadUpcomingMovies() },
          launch { loadTopRatedMovies() },
          launch { loadTrendingMovies() },
          launch { loadWesternTv() },
          launch { loadKDrama() },
          launch { loadShortMovies() },
          launch { loadAnimeMovies() }
        )
        jobs.joinAll()
      } catch (e: Exception) {
        // Log error if needed
      } finally {
        _isLoading.value = false
      }
    }
  }


  private fun launch(block: suspend () -> Unit) {
    viewModelScope.launch { block() }
  }
}