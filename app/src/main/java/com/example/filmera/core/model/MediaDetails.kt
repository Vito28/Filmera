package com.example.filmera.core.model

data class MediaDetails(
  val id: Int,
  val type: MediaType,
  val title: String,
  val originalTitle: String,
  val overview: String,
  val tagline: String?,
  val posterPath: String?,
  val backdropPath: String?,
  val releaseDate: String?,
  val runtimeMinutes: Int?,
  val voteAverage: Double,
  val voteCount: Int,
  val popularity: Double,
  val originalLanguage: String,
  val status: String?,
  val homepage: String?,
  val externalId: String?,
  val budget: Long?,
  val revenue: Long?,
  val genres: List<String>,
  val productionCompanies: List<String>,
  val productionCountries: List<String>,
  val spokenLanguages: List<String>,
) {
  val key: MediaKey = MediaKey(id = id, type = type)

  fun toMediaItem(): MediaItem = MediaItem(
    id = id,
    type = type,
    title = title,
    originalTitle = originalTitle,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
    popularity = popularity,
    adult = false,
    originalLanguage = originalLanguage,
    genreIds = emptyList(),
  )
}
