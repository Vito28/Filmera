package com.example.filmera.core.network.dto

import com.google.gson.annotations.SerializedName

data class PersonDetailsDto(
  @SerializedName("adult") val adult: Boolean = false,
  @SerializedName("also_known_as") val alsoKnownAs: List<String> = emptyList(),
  @SerializedName("biography") val biography: String? = null,
  @SerializedName("birthday") val birthday: String? = null,
  @SerializedName("deathday") val deathday: String? = null,
  @SerializedName("gender") val gender: Int = 0,
  @SerializedName("homepage") val homepage: String? = null,
  @SerializedName("id") val id: Int = 0,
  @SerializedName("imdb_id") val imdbId: String? = null,
  @SerializedName("known_for_department") val knownForDepartment: String? = null,
  @SerializedName("name") val name: String = "",
  @SerializedName("place_of_birth") val placeOfBirth: String? = null,
  @SerializedName("popularity") val popularity: Double = 0.0,
  @SerializedName("profile_path") val profilePath: String? = null,
  @SerializedName("combined_credits")
  val combinedCredits: PersonCombinedCreditsResponseDto? = null,
  @SerializedName("external_ids") val externalIds: ExternalIdsDto? = null,
  @SerializedName("images") val images: PersonImagesResponseDto? = null,
  @SerializedName("translations") val translations: TranslationResponseDto? = null,
)

data class PersonCombinedCreditsResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("cast") val cast: List<PersonCreditDto> = emptyList(),
  @SerializedName("crew") val crew: List<PersonCreditDto> = emptyList(),
)

data class PersonCreditDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("media_type") val mediaType: String = "",
  @SerializedName("adult") val adult: Boolean = false,
  @SerializedName("title") val title: String? = null,
  @SerializedName("original_title") val originalTitle: String? = null,
  @SerializedName("release_date") val releaseDate: String? = null,
  @SerializedName("name") val name: String? = null,
  @SerializedName("original_name") val originalName: String? = null,
  @SerializedName("first_air_date") val firstAirDate: String? = null,
  @SerializedName("overview") val overview: String? = null,
  @SerializedName("poster_path") val posterPath: String? = null,
  @SerializedName("backdrop_path") val backdropPath: String? = null,
  @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
  @SerializedName("original_language") val originalLanguage: String? = null,
  @SerializedName("popularity") val popularity: Double = 0.0,
  @SerializedName("vote_average") val voteAverage: Double = 0.0,
  @SerializedName("vote_count") val voteCount: Int = 0,
  @SerializedName("character") val character: String? = null,
  @SerializedName("job") val job: String? = null,
  @SerializedName("department") val department: String? = null,
  @SerializedName("credit_id") val creditId: String? = null,
  @SerializedName("episode_count") val episodeCount: Int? = null,
)

data class PersonImagesResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("profiles") val profiles: List<ImageAssetDto> = emptyList(),
)
