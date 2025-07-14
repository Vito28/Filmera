package com.example.filmera.model.search

data class SearchResult(
  val id: Int,
  val media_type: String,
  val title: String?,
  val name: String?,
  val original_title: String?,
  val original_name: String?,
  val overview: String?,
  val poster_path: String?,
  val backdrop_path: String?,
  val vote_average: Float,
  val vote_count: Int,
  val popularity: Float,
  val release_date: String?,
  val first_air_date: String?,
  val genre_ids: List<Int> = emptyList(),
  val original_language: String,
  val origin_country: List<String> = emptyList()
)