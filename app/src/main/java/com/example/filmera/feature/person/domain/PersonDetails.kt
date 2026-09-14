package com.example.filmera.feature.person.domain

import com.example.filmera.core.model.MediaItem

data class PersonDetails(
  val id: Int,
  val name: String,
  val biography: String,
  val birthday: String?,
  val deathday: String?,
  val gender: PersonGender,
  val placeOfBirth: String?,
  val knownForDepartment: String?,
  val profilePath: String?,
  val alsoKnownAs: List<String>,
  val homepage: String?,
  val popularity: Double,
  val externalIds: PersonExternalIds,
  val photos: List<String>,
  val movieCredits: List<PersonCredit>,
  val tvCredits: List<PersonCredit>,
)

enum class PersonGender {
  NOT_SPECIFIED,
  FEMALE,
  MALE,
  NON_BINARY,
}

data class PersonExternalIds(
  val imdbId: String?,
  val wikidataId: String?,
  val facebookId: String?,
  val instagramId: String?,
  val twitterId: String?,
  val tiktokId: String?,
  val youtubeId: String?,
)

data class PersonCredit(
  val creditId: String,
  val media: MediaItem,
  val contribution: String,
  val department: String?,
  val episodeCount: Int?,
  val isCast: Boolean,
) {
  val stableKey: String = creditId.ifBlank {
    "${media.type.routeValue}:${media.id}:$contribution:$isCast"
  }
}
