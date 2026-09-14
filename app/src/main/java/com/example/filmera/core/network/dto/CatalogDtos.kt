package com.example.filmera.core.network.dto

import com.google.gson.annotations.SerializedName

data class CatalogResponseDto<T>(
  @SerializedName("page") val page: Int = 1,
  @SerializedName("results") val results: List<T> = emptyList(),
  @SerializedName("total_pages") val totalPages: Int = 0,
  @SerializedName("total_results") val totalResults: Int = 0,
)

data class DatedCatalogResponseDto<T>(
  @SerializedName("dates") val dates: DateRangeDto? = null,
  @SerializedName("page") val page: Int = 1,
  @SerializedName("results") val results: List<T> = emptyList(),
  @SerializedName("total_pages") val totalPages: Int = 0,
  @SerializedName("total_results") val totalResults: Int = 0,
)

data class DateRangeDto(
  @SerializedName("maximum") val maximum: String? = null,
  @SerializedName("minimum") val minimum: String? = null,
)

data class MovieDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("title") val title: String? = null,
  @SerializedName("original_title") val originalTitle: String? = null,
  @SerializedName("overview") val overview: String? = null,
  @SerializedName("poster_path") val posterPath: String? = null,
  @SerializedName("backdrop_path") val backdropPath: String? = null,
  @SerializedName("release_date") val releaseDate: String? = null,
  @SerializedName("vote_average") val voteAverage: Double = 0.0,
  @SerializedName("vote_count") val voteCount: Int = 0,
  @SerializedName("popularity") val popularity: Double = 0.0,
  @SerializedName("adult") val adult: Boolean = false,
  @SerializedName("original_language") val originalLanguage: String? = null,
  @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
  @SerializedName("origin_country") val originCountry: List<String> = emptyList(),
)

data class TvShowDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("name") val name: String? = null,
  @SerializedName("original_name") val originalName: String? = null,
  @SerializedName("overview") val overview: String? = null,
  @SerializedName("poster_path") val posterPath: String? = null,
  @SerializedName("backdrop_path") val backdropPath: String? = null,
  @SerializedName("first_air_date") val firstAirDate: String? = null,
  @SerializedName("vote_average") val voteAverage: Double = 0.0,
  @SerializedName("vote_count") val voteCount: Int = 0,
  @SerializedName("popularity") val popularity: Double = 0.0,
  @SerializedName("adult") val adult: Boolean = false,
  @SerializedName("original_language") val originalLanguage: String? = null,
  @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
  @SerializedName("origin_country") val originCountry: List<String> = emptyList(),
)

/**
 * Raw multi-search/trending result. Consumers must branch on [mediaType].
 */
data class MultiSearchItemDto(
  @SerializedName("media_type") val mediaType: String = "",
  @SerializedName("id") val id: Int = 0,
  @SerializedName("title") val title: String? = null,
  @SerializedName("original_title") val originalTitle: String? = null,
  @SerializedName("release_date") val releaseDate: String? = null,
  @SerializedName("name") val name: String? = null,
  @SerializedName("original_name") val originalName: String? = null,
  @SerializedName("first_air_date") val firstAirDate: String? = null,
  @SerializedName("overview") val overview: String? = null,
  @SerializedName("poster_path") val posterPath: String? = null,
  @SerializedName("backdrop_path") val backdropPath: String? = null,
  @SerializedName("profile_path") val profilePath: String? = null,
  @SerializedName("vote_average") val voteAverage: Double = 0.0,
  @SerializedName("vote_count") val voteCount: Int = 0,
  @SerializedName("popularity") val popularity: Double = 0.0,
  @SerializedName("adult") val adult: Boolean = false,
  @SerializedName("original_language") val originalLanguage: String? = null,
  @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
  @SerializedName("known_for_department") val knownForDepartment: String? = null,
)

data class PersonDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("name") val name: String = "",
  @SerializedName("original_name") val originalName: String? = null,
  @SerializedName("known_for_department") val knownForDepartment: String? = null,
  @SerializedName("profile_path") val profilePath: String? = null,
  @SerializedName("known_for") val knownFor: List<MultiSearchItemDto> = emptyList(),
  @SerializedName("popularity") val popularity: Double = 0.0,
  @SerializedName("adult") val adult: Boolean = false,
)

data class CollectionDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("name") val name: String = "",
  @SerializedName("overview") val overview: String? = null,
  @SerializedName("poster_path") val posterPath: String? = null,
  @SerializedName("backdrop_path") val backdropPath: String? = null,
  @SerializedName("popularity") val popularity: Double = 0.0,
  @SerializedName("adult") val adult: Boolean = false,
)
