package com.example.filmera.repository

import android.util.Log
import com.example.filmera.model.Movie
import com.example.filmera.model.detail.MovieDetail
import com.example.filmera.model.search.SearchResult
import com.example.filmera.network.TMDBApi
import javax.inject.Inject

class SearchRepository @Inject constructor(
  private val api: TMDBApi
) {

  private val TAG = "SearchRepository"

  suspend fun searchMovies(query: String, page: Int = 1, language: String? = null): List<SearchResult> =
    handler("searchMovies") {
      api.searchMovies(query, page, language).results
    }

  suspend fun searchTv(query: String, page: Int = 1, language: String? = null): List<SearchResult> =
    handler("searchTv") {
      api.searchTv(query, page, language).results
    }

  suspend fun searchMulti(query: String, page: Int = 1, language: String? = null): List<SearchResult> =
    handler("searchMulti") {
      api.searchMulti(query, page, language).results
    }

  private suspend fun handler(tag: String, block: suspend () -> List<SearchResult>): List<SearchResult> {
    return try {
      block()
    } catch (e: Exception) {
      Log.e(TAG, "Error in $tag: ${e.localizedMessage}")
      emptyList()
    }
  }
}
