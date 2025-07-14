package com.example.filmera.repository

import android.util.Log
import com.example.filmera.model.Movie
import com.example.filmera.model.credit.CreditsResponse
import com.example.filmera.model.detail.MovieDetail
import com.example.filmera.model.search.SearchResult
import com.example.filmera.model.video.VideoItem
import com.example.filmera.network.TMDBApi
import javax.inject.Inject

class MovieRepository @Inject constructor(
  private val api: TMDBApi
) {

  private val TAG = "MovieRepository"

  suspend fun getPopularMovies(page: Int = 1, language: String? = null): List<Movie> =
    handler("getPopularMovies") {
      api.getPopularMovies(page, language).results
    }

  suspend fun getNowPlayingMovies(page: Int = 1, language: String? = null): List<Movie> =
    handler("getNowPlayingMovies") {
      api.getNowPlayingMovies(page, language).results
    }

  suspend fun getUpcomingMovies(page: Int = 1, language: String? = null): List<Movie> =
    handler("getUpcomingMovies") {
      api.getUpcomingMovies(page, language).results
    }

  suspend fun getTopRatedMovies(page: Int = 1, language: String? = null): List<Movie> =
    handler("getTopRatedMovies") {
      api.getTopRatedMovies(page, language).results
    }

  suspend fun getTrendingMovies(language: String? = null): List<Movie> =
    handler("getTrendingMovies") {
      api.getTrendingMovies(language).results
    }

  suspend fun getWesternTv(page: Int = 1, language: String? = null): List<Movie> =
    handler("getWesternTv") {
      api.getWesternTV(page = page, language = language).results
    }

  suspend fun getKDrama(page: Int = 1, language: String? = null): List<Movie> =
    handler("getKDrama") {
      api.getKDrama(page = page, language = language).results
    }

  suspend fun getShortMovies(page: Int = 1, language: String? = null): List<Movie> =
    handler("getShortMovies") {
      api.getShortMovies(page = page, language = language).results
    }

  suspend fun getAnimeMovies(page: Int = 1, language: String? = null): List<Movie> =
    handler("getAnimeMovies") {
      api.getAnimeMovies(page = page, language = language).results
    }

  suspend fun getRecommendations(movieId: Int, page: Int = 1, language: String? = null): List<Movie> =
    handler("getRecommendations") {
      api.getMovieRecommendations(movieId, page, language).results
    }

  suspend fun getMovieDetail(movieId: Int, language: String? = null): MovieDetail? {
    return try {
      api.getMovieDetail(movieId, language)
    } catch (e: Exception) {
      Log.e("SearchRepository", "Error getTvDetail: ${e.localizedMessage}")
      null
    }
  }

  suspend fun getTvDetail(tvId: Int, language: String? = null): MovieDetail? {
    return try {
      api.getTvDetail(tvId, language)
    } catch (e: Exception) {
      Log.e("SearchRepository", "Error getTvDetail: ${e.localizedMessage}")
      null
    }
  }

  suspend fun getMovieVideo(movieId: Int): VideoItem? {
    return try {
      val response = api.getMovieVideos(movieId)
      response.results.firstOrNull {
        it.site == "YouTube" && it.type == "Trailer"
      }
    } catch (e: Exception) {
      Log.e("SearchRepository", "Error getMovieVideo: ${e.localizedMessage}")
      null
    }
  }

  suspend fun getMovieCredits(movieId: Int): CreditsResponse? {
    return try {
      api.getMovieCredits(movieId)
    } catch (e: Exception) {
      Log.e("SearchRepository", "Error getMovieCredits: ${e.localizedMessage}")
      null
    }
  }

  private suspend fun handler(tag: String, block: suspend () -> List<Movie>): List<Movie> {
    return try {
      block()
    } catch (e: Exception) {
      Log.e(TAG, "Error in $tag: ${e.localizedMessage}")
      emptyList()
    }
  }
}
