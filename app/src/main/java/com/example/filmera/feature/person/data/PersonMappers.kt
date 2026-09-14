package com.example.filmera.feature.person.data

import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.network.dto.PersonCreditDto
import com.example.filmera.core.network.dto.PersonDetailsDto
import com.example.filmera.feature.person.domain.PersonCredit
import com.example.filmera.feature.person.domain.PersonDetails
import com.example.filmera.feature.person.domain.PersonExternalIds
import com.example.filmera.feature.person.domain.PersonGender

fun PersonDetailsDto.toDomain(): PersonDetails {
  val credits = buildList {
    combinedCredits?.cast.orEmpty().mapNotNullTo(this) { it.toDomain(isCast = true) }
    combinedCredits?.crew.orEmpty().mapNotNullTo(this) { it.toDomain(isCast = false) }
  }
    .distinctBy(PersonCredit::stableKey)
    .sortedWith(
      compareByDescending<PersonCredit> { it.media.releaseDate.orEmpty() }
        .thenByDescending { it.media.popularity },
    )

  return PersonDetails(
    id = id,
    name = name.ifBlank { "Unknown person" },
    biography = localizedBiography(),
    birthday = birthday.clean(),
    deathday = deathday.clean(),
    gender = gender.toDomain(),
    placeOfBirth = placeOfBirth.clean(),
    knownForDepartment = knownForDepartment.clean(),
    profilePath = profilePath.clean(),
    alsoKnownAs = alsoKnownAs.mapNotNull(String::clean).distinct(),
    homepage = homepage.clean(),
    popularity = popularity,
    externalIds = PersonExternalIds(
      imdbId = externalIds?.imdbId.clean() ?: imdbId.clean(),
      wikidataId = externalIds?.wikidataId.clean(),
      facebookId = externalIds?.facebookId.clean(),
      instagramId = externalIds?.instagramId.clean(),
      twitterId = externalIds?.twitterId.clean(),
      tiktokId = externalIds?.tiktokId.clean(),
      youtubeId = externalIds?.youtubeId.clean(),
    ),
    photos = buildList {
      profilePath.clean()?.let(::add)
      images?.profiles
        .orEmpty()
        .sortedByDescending { it.voteAverage }
        .mapNotNullTo(this) { it.filePath.clean() }
    }.distinct(),
    movieCredits = credits.filter { it.media.type == MediaType.MOVIE },
    tvCredits = credits.filter { it.media.type == MediaType.TV_SHOW },
  )
}

private fun PersonCreditDto.toDomain(isCast: Boolean): PersonCredit? {
  val type = MediaType.fromRoute(mediaType) ?: return null
  if (id <= 0) return null

  val localizedTitle = when (type) {
    MediaType.MOVIE -> title.clean()
    MediaType.TV_SHOW -> name.clean()
  }
  val fallbackTitle = when (type) {
    MediaType.MOVIE -> originalTitle.clean()
    MediaType.TV_SHOW -> originalName.clean()
  }
  val displayTitle = localizedTitle ?: fallbackTitle ?: return null

  val item = MediaItem(
    id = id,
    type = type,
    title = displayTitle,
    originalTitle = fallbackTitle.orEmpty(),
    overview = overview.orEmpty(),
    posterPath = posterPath.clean(),
    backdropPath = backdropPath.clean(),
    releaseDate = when (type) {
      MediaType.MOVIE -> releaseDate.clean()
      MediaType.TV_SHOW -> firstAirDate.clean()
    },
    voteAverage = voteAverage,
    voteCount = voteCount,
    popularity = popularity,
    adult = adult,
    originalLanguage = originalLanguage.orEmpty(),
    genreIds = genreIds,
  )

  return PersonCredit(
    creditId = creditId.orEmpty(),
    media = item,
    contribution = if (isCast) character.clean().orEmpty() else job.clean().orEmpty(),
    department = department.clean(),
    episodeCount = episodeCount?.takeIf { it > 0 },
    isCast = isCast,
  )
}

private fun PersonDetailsDto.localizedBiography(): String {
  biography.clean()?.let { return it }

  return translations
    ?.translations
    .orEmpty()
    .sortedBy {
      when (it.languageCode) {
        "id" -> 0
        "en" -> 1
        else -> 2
      }
    }
    .firstNotNullOfOrNull { it.data.biography.clean() }
    .orEmpty()
}

private fun Int.toDomain(): PersonGender =
  when (this) {
    1 -> PersonGender.FEMALE
    2 -> PersonGender.MALE
    3 -> PersonGender.NON_BINARY
    else -> PersonGender.NOT_SPECIFIED
  }

private fun String?.clean(): String? = this?.trim()?.takeIf(String::isNotEmpty)
