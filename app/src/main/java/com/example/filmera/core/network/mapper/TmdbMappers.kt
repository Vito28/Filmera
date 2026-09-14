package com.example.filmera.core.network.mapper

import com.example.filmera.core.model.CastMember
import com.example.filmera.core.model.Credits
import com.example.filmera.core.model.CrewMember
import com.example.filmera.core.model.MediaDetails
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.model.MediaVideo
import com.example.filmera.core.network.dto.CreditsResponseDto
import com.example.filmera.core.network.dto.AggregateCreditsResponseDto
import com.example.filmera.core.network.dto.MovieDetailsDto
import com.example.filmera.core.network.dto.MovieDto
import com.example.filmera.core.network.dto.MultiSearchItemDto
import com.example.filmera.core.network.dto.SpokenLanguageDto
import com.example.filmera.core.network.dto.TvDetailsDto
import com.example.filmera.core.network.dto.TvShowDto
import com.example.filmera.core.network.dto.VideoDto
import java.util.Locale

fun MovieDto.toDomain(): MediaItem = MediaItem(
  id = id,
  type = MediaType.MOVIE,
  title = title.orEmpty().ifBlank { originalTitle.orEmpty().ifBlank { "Untitled movie" } },
  originalTitle = originalTitle.orEmpty(),
  overview = overview.orEmpty(),
  posterPath = posterPath,
  backdropPath = backdropPath,
  releaseDate = releaseDate?.takeIf(String::isNotBlank),
  voteAverage = voteAverage,
  voteCount = voteCount,
  popularity = popularity,
  adult = adult,
  originalLanguage = originalLanguage.orEmpty(),
  genreIds = genreIds,
  originCountries = originCountry,
)

fun TvShowDto.toDomain(): MediaItem = MediaItem(
  id = id,
  type = MediaType.TV_SHOW,
  title = name.orEmpty().ifBlank { originalName.orEmpty().ifBlank { "Untitled TV show" } },
  originalTitle = originalName.orEmpty(),
  overview = overview.orEmpty(),
  posterPath = posterPath,
  backdropPath = backdropPath,
  releaseDate = firstAirDate?.takeIf(String::isNotBlank),
  voteAverage = voteAverage,
  voteCount = voteCount,
  popularity = popularity,
  adult = adult,
  originalLanguage = originalLanguage.orEmpty(),
  genreIds = genreIds,
  originCountries = originCountry,
)

fun MovieDetailsDto.toDomain(): MediaDetails = MediaDetails(
  id = id,
  type = MediaType.MOVIE,
  title = title.orEmpty().ifBlank { originalTitle.orEmpty().ifBlank { "Untitled movie" } },
  originalTitle = originalTitle.orEmpty(),
  overview = overview.orEmpty(),
  tagline = tagline?.takeIf(String::isNotBlank),
  posterPath = posterPath,
  backdropPath = backdropPath,
  releaseDate = releaseDate?.takeIf(String::isNotBlank),
  runtimeMinutes = runtime,
  voteAverage = voteAverage,
  voteCount = voteCount,
  popularity = popularity,
  originalLanguage = originalLanguage.orEmpty(),
  status = status,
  homepage = homepage?.takeIf(String::isNotBlank),
  externalId = imdbId,
  budget = budget.takeIf { it > 0L },
  revenue = revenue.takeIf { it > 0L },
  genres = genres.mapNotNull { it.name.takeIf(String::isNotBlank) },
  productionCompanies = productionCompanies.mapNotNull { it.name.takeIf(String::isNotBlank) },
  productionCountries = productionCountries.mapNotNull { it.name.takeIf(String::isNotBlank) },
  spokenLanguages = spokenLanguages.mapNotNull(SpokenLanguageDto::defaultDisplayName),
)

fun TvDetailsDto.toDomain(): MediaDetails = MediaDetails(
  id = id,
  type = MediaType.TV_SHOW,
  title = name.orEmpty().ifBlank { originalName.orEmpty().ifBlank { "Untitled TV show" } },
  originalTitle = originalName.orEmpty(),
  overview = overview.orEmpty(),
  tagline = tagline?.takeIf(String::isNotBlank),
  posterPath = posterPath,
  backdropPath = backdropPath,
  releaseDate = firstAirDate?.takeIf(String::isNotBlank),
  runtimeMinutes = episodeRunTime.firstOrNull(),
  voteAverage = voteAverage,
  voteCount = voteCount,
  popularity = popularity,
  originalLanguage = originalLanguage.orEmpty(),
  status = status,
  homepage = homepage?.takeIf(String::isNotBlank),
  externalId = null,
  budget = null,
  revenue = null,
  genres = genres.mapNotNull { it.name.takeIf(String::isNotBlank) },
  productionCompanies = productionCompanies.mapNotNull { it.name.takeIf(String::isNotBlank) },
  productionCountries = productionCountries.mapNotNull { it.name.takeIf(String::isNotBlank) },
  spokenLanguages = spokenLanguages.mapNotNull(SpokenLanguageDto::defaultDisplayName),
)

fun CreditsResponseDto.toDomain(): Credits = Credits(
  cast = cast
    .filter { it.name.isNotBlank() }
    .sortedBy { it.order }
    .map { CastMember(it.id, it.name, it.character, it.profilePath, it.order) },
  crew = crew
    .filter { it.name.isNotBlank() }
    .map { CrewMember(it.id, it.name, it.job, it.department, it.profilePath) },
)

fun AggregateCreditsResponseDto.toDomain(): Credits = Credits(
  cast = cast
    .filter { it.name.isNotBlank() }
    .sortedBy { it.order }
    .map { member ->
      CastMember(
        id = member.id,
        name = member.name,
        character = member.roles
          .mapNotNull { it.character.takeIf(String::isNotBlank) }
          .distinct()
          .joinToString(),
        profilePath = member.profilePath,
        order = member.order,
      )
    },
  crew = crew
    .filter { it.name.isNotBlank() }
    .map { member ->
      CrewMember(
        id = member.id,
        name = member.name,
        job = member.jobs
          .mapNotNull { it.job.takeIf(String::isNotBlank) }
          .distinct()
          .joinToString(),
        department = member.department,
        profilePath = member.profilePath,
      )
    },
)

fun MultiSearchItemDto.toMediaItemOrNull(): MediaItem? =
  when (mediaType) {
    "movie" -> MovieDto(
      id = id,
      title = title,
      originalTitle = originalTitle,
      overview = overview,
      posterPath = posterPath,
      backdropPath = backdropPath,
      releaseDate = releaseDate,
      voteAverage = voteAverage,
      voteCount = voteCount,
      popularity = popularity,
      adult = adult,
      originalLanguage = originalLanguage,
      genreIds = genreIds,
    ).toDomain()

    "tv" -> TvShowDto(
      id = id,
      name = name,
      originalName = originalName,
      overview = overview,
      posterPath = posterPath,
      backdropPath = backdropPath,
      firstAirDate = firstAirDate,
      voteAverage = voteAverage,
      voteCount = voteCount,
      popularity = popularity,
      adult = adult,
      originalLanguage = originalLanguage,
      genreIds = genreIds,
    ).toDomain()

    else -> null
  }

fun VideoDto.toDomain(): MediaVideo = MediaVideo(
  key = key,
  name = name,
  site = site,
  type = type,
  isOfficial = official,
  id = id,
  languageCode = languageCode,
  countryCode = countryCode,
  publishedAt = publishedAt,
)

/**
 * Returns a default English language label without falling back to an endonym.
 *
 * Native labels such as "普通话" belong to a future localized presentation
 * layer. Until then, TMDB's English label or an English ISO-639 fallback keeps
 * language names consistent with the app's default UI language.
 */
internal fun SpokenLanguageDto.defaultDisplayName(): String? {
  englishName?.takeIf(String::isNotBlank)?.let { return it }
  val code = languageCode?.takeIf(String::isNotBlank) ?: return null
  val displayName = Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH)
  return displayName.takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) }
    ?: code.lowercase(Locale.ROOT)
}
