package com.example.filmera.core.model

data class MediaItem(
  val id: Int,
  val type: MediaType,
  val title: String,
  val originalTitle: String,
  val overview: String,
  val posterPath: String?,
  val backdropPath: String?,
  val releaseDate: String?,
  val voteAverage: Double,
  val voteCount: Int,
  val popularity: Double,
  val adult: Boolean,
  val originalLanguage: String,
  val genreIds: List<Int>,
  val originCountries: List<String> = emptyList(),
) {
  val key: MediaKey = MediaKey(id = id, type = type)
}
