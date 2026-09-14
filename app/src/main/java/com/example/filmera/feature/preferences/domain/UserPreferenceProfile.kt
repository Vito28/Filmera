package com.example.filmera.feature.preferences.domain

enum class MediaPreference {
  MOVIE,
  TV_SERIES,
  ANIME,
  DOCUMENTARY,
  ANIMATION,
}

data class UserPreferenceProfile(
  val preferredMediaTypes: Set<MediaPreference> = emptySet(),
  val preferredGenreIds: Set<Int> = emptySet(),
  val preferredCountries: Set<String> = setOf(DEFAULT_COUNTRY),
  val preferredMovieIds: Set<Int> = emptySet(),
  val preferredTvIds: Set<Int> = emptySet(),
  val preferredPersonIds: Set<Int> = emptySet(),
  val preferredLanguageCodes: Set<String> = emptySet(),
  val onboardingCompleted: Boolean = false,
  val updatedAt: Long = 0L,
) {
  val isReadyForRecommendations: Boolean
    get() = onboardingCompleted &&
      preferredMediaTypes.isNotEmpty() &&
      preferredGenreIds.size >= MINIMUM_GENRE_COUNT

  companion object {
    const val DEFAULT_COUNTRY = "GLOBAL"
    const val MINIMUM_GENRE_COUNT = 3
    const val MAXIMUM_GENRE_COUNT = 8
  }
}

data class PreferenceGenre(
  val id: Int,
  val name: String,
)

val supportedPreferenceGenres = listOf(
  PreferenceGenre(28, "Action"),
  PreferenceGenre(12, "Adventure"),
  PreferenceGenre(16, "Animation"),
  PreferenceGenre(35, "Comedy"),
  PreferenceGenre(80, "Crime"),
  PreferenceGenre(99, "Documentary"),
  PreferenceGenre(18, "Drama"),
  PreferenceGenre(10751, "Family"),
  PreferenceGenre(14, "Fantasy"),
  PreferenceGenre(36, "History"),
  PreferenceGenre(27, "Horror"),
  PreferenceGenre(10402, "Music"),
  PreferenceGenre(9648, "Mystery"),
  PreferenceGenre(10749, "Romance"),
  PreferenceGenre(878, "Science Fiction"),
  PreferenceGenre(53, "Thriller"),
  PreferenceGenre(10752, "War"),
  PreferenceGenre(37, "Western"),
)

data class PreferenceCountry(
  val code: String,
  val name: String,
  val description: String,
)

val supportedPreferenceCountries = listOf(
  PreferenceCountry("GLOBAL", "Global cinema", "Stories from around the world"),
  PreferenceCountry("ID", "Indonesia", "Indonesian stories"),
  PreferenceCountry("US", "United States", "American movies and series"),
  PreferenceCountry("KR", "South Korea", "Korean movies and series"),
  PreferenceCountry("JP", "Japan", "Japanese anime and cinema"),
  PreferenceCountry("CN", "China", "Chinese movies and series"),
  PreferenceCountry("IN", "India", "Indian cinema"),
  PreferenceCountry("TH", "Thailand", "Thai movies and series"),
  PreferenceCountry("GB", "United Kingdom", "British movies and series"),
  PreferenceCountry("EU", "Europe", "Cinema from across Europe"),
)
