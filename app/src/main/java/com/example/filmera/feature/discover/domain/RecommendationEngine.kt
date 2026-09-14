package com.example.filmera.feature.discover.domain

import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.feature.preferences.domain.MediaPreference
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile
import com.example.filmera.feature.preferences.domain.supportedPreferenceGenres
import java.time.LocalDate
import kotlin.math.ln
import kotlin.math.roundToInt

class RecommendationEngine(
  private val currentYear: Int = LocalDate.now().year,
) {
  fun rank(
    candidates: List<RecommendationCandidate>,
    profile: UserPreferenceProfile,
    feedback: Map<MediaKey, RecommendationFeedbackAction>,
    selectedGenreId: Int? = null,
  ): List<RecommendedItem> {
    val hiddenActions = setOf(
      RecommendationFeedbackAction.NOT_INTERESTED,
      RecommendationFeedbackAction.ALREADY_WATCHED,
      RecommendationFeedbackAction.HIDE,
    )
    val deduplicated = candidates
      .asSequence()
      .filter { candidate -> candidate.media.isRecommendationCandidate() }
      .filter { candidate -> feedback[candidate.media.key] !in hiddenActions }
      .filter { candidate ->
        selectedGenreId == null || selectedGenreId in candidate.media.genreIds
      }
      .groupBy { candidate -> candidate.media.key }
      .map { (_, duplicates) ->
        duplicates.first().copy(
          originCountries = duplicates.flatMap { it.originCountries }.toSet(),
          sources = duplicates.flatMap { it.sources }.toSet(),
        )
      }

    val maximumPopularity = deduplicated.maxOfOrNull { it.media.popularity }
      ?.coerceAtLeast(1.0)
      ?: 1.0

    return deduplicated
      .map { candidate ->
        candidate.toRecommendedItem(
          profile = profile,
          action = feedback[candidate.media.key],
          maximumPopularity = maximumPopularity,
        )
      }
      .sortedWith(
        compareByDescending<RecommendedItem> { it.score }
          .thenByDescending { it.media.voteCount }
          .thenByDescending { it.media.popularity },
      )
  }

  fun diversify(
    rankedItems: List<RecommendedItem>,
    limit: Int,
  ): List<RecommendedItem> {
    if (rankedItems.size <= 2 || limit <= 2) return rankedItems.take(limit)

    val countryLimit = (limit * MAX_COUNTRY_SHARE).roundToInt().coerceAtLeast(2)
    val genreLimit = (limit * MAX_GENRE_SHARE).roundToInt().coerceAtLeast(3)
    val countryCounts = mutableMapOf<String, Int>()
    val genreCounts = mutableMapOf<Int, Int>()
    val selected = mutableListOf<RecommendedItem>()
    val deferred = mutableListOf<RecommendedItem>()

    rankedItems.forEach { item ->
      val country = item.originCountries.sorted().firstOrNull()
        ?: item.media.originalLanguage.toCountryCode()
      val primaryGenre = item.media.genreIds.firstOrNull()
      val countryAllowed = country == null || countryCounts.getOrDefault(country, 0) < countryLimit
      val genreAllowed = primaryGenre == null ||
        genreCounts.getOrDefault(primaryGenre, 0) < genreLimit

      if (selected.size < limit && countryAllowed && genreAllowed) {
        selected += item
        country?.let { countryCounts[it] = countryCounts.getOrDefault(it, 0) + 1 }
        primaryGenre?.let { genreCounts[it] = genreCounts.getOrDefault(it, 0) + 1 }
      } else {
        deferred += item
      }
    }

    if (selected.size < limit) {
      selected += deferred
        .asSequence()
        .filterNot { deferredItem ->
          selected.any { it.media.key == deferredItem.media.key }
        }
        .take(limit - selected.size)
    }
    return selected
  }

  private fun RecommendationCandidate.toRecommendedItem(
    profile: UserPreferenceProfile,
    action: RecommendationFeedbackAction?,
    maximumPopularity: Double,
  ): RecommendedItem {
    val genreMatches = media.genreIds.intersect(profile.preferredGenreIds)
    val genreScore = if (profile.preferredGenreIds.isEmpty()) {
      0.5
    } else {
      (genreMatches.size.toDouble() / minOf(profile.preferredGenreIds.size, 3))
        .coerceIn(0.0, 1.0)
    }
    val qualityScore = weightedRating(media) / MAX_RATING
    val selectedTitleScore =
      if (RecommendationSource.SELECTED_TITLE in sources) 1.0 else 0.0
    val mediaTypeScore = media.mediaTypeMatch(profile)
    val countryMatches = normalizedCountries()
      .intersect(profile.preferredCountries - UserPreferenceProfile.DEFAULT_COUNTRY)
    val countryScore = when {
      countryMatches.isNotEmpty() -> 1.0
      UserPreferenceProfile.DEFAULT_COUNTRY in profile.preferredCountries -> 0.55
      else -> 0.2
    }
    val popularityScore = (
      ln(media.popularity.coerceAtLeast(0.0) + 1.0) /
        ln(maximumPopularity + 1.0)
      ).coerceIn(0.0, 1.0)
    val personScore =
      if (RecommendationSource.PREFERRED_PERSON in sources) 1.0 else 0.0
    val releaseYear = media.releaseDate?.take(4)?.toIntOrNull()
    val recencyScore = when {
      releaseYear == null -> 0.35
      releaseYear >= currentYear - 2 -> 1.0
      releaseYear >= currentYear - 6 -> 0.65
      else -> 0.25
    }
    val diversityBonus =
      if (normalizedCountries().any { it !in COMMON_MARKETS }) 1.0 else 0.5
    val feedbackAdjustment = when (action) {
      RecommendationFeedbackAction.MORE_LIKE_THIS -> 0.08
      RecommendationFeedbackAction.LESS_LIKE_THIS -> -0.18
      else -> 0.0
    }

    val score = (
      genreScore * 0.25 +
        qualityScore * 0.20 +
        selectedTitleScore * 0.15 +
        mediaTypeScore * 0.10 +
        countryScore * 0.10 +
        popularityScore * 0.08 +
        personScore * 0.05 +
        recencyScore * 0.04 +
        diversityBonus * 0.03 +
        feedbackAdjustment
      ).coerceIn(0.0, 1.0)

    return RecommendedItem(
      media = media,
      score = score,
      matchPercentage = (score * 100).roundToInt(),
      reasons = buildReasons(
        media = media,
        genreMatches = genreMatches,
        countryMatches = countryMatches,
        qualityScore = qualityScore,
        selectedTitleMatch = selectedTitleScore > 0.0,
        personMatch = personScore > 0.0,
        isTrending = RecommendationSource.TRENDING in sources,
      ),
      originCountries = normalizedCountries(),
      sources = sources,
    )
  }

  private fun weightedRating(media: MediaItem): Double {
    val votes = media.voteCount.coerceAtLeast(0).toDouble()
    return (
      votes / (votes + MINIMUM_CONFIDENT_VOTES) * media.voteAverage.coerceIn(0.0, 10.0) +
        MINIMUM_CONFIDENT_VOTES / (votes + MINIMUM_CONFIDENT_VOTES) * CANDIDATE_MEAN_RATING
      )
  }

  private fun buildReasons(
    media: MediaItem,
    genreMatches: Set<Int>,
    countryMatches: Set<String>,
    qualityScore: Double,
    selectedTitleMatch: Boolean,
    personMatch: Boolean,
    isTrending: Boolean,
  ): List<String> = buildList {
    if (genreMatches.isNotEmpty()) {
      val names = supportedPreferenceGenres
        .filter { it.id in genreMatches }
        .map { it.name.lowercase() }
      add("Because you like ${names.take(2).joinToString(" and ")}")
    }
    if (selectedTitleMatch) {
      add("Similar to a title you selected")
    }
    if (personMatch) {
      add("Features an actor or creator you selected")
    }
    if (countryMatches.isNotEmpty()) {
      add("From a cinema world you follow")
    }
    if (qualityScore >= 0.72 && media.voteCount >= MINIMUM_CONFIDENT_VOTES) {
      add("Highly rated by a large audience")
    }
    if (isEmpty()) {
      add(
        if (isTrending) {
          "Trending worldwide"
        } else {
          "A quality pick from around the world"
        },
      )
    }
  }.take(MAX_REASON_COUNT)

  private fun RecommendationCandidate.normalizedCountries(): Set<String> =
    originCountries
      .filter(String::isNotBlank)
      .toSet()
      .ifEmpty {
        media.originalLanguage.toCountryCode()?.let(::setOf).orEmpty()
      }

  private fun MediaItem.mediaTypeMatch(profile: UserPreferenceProfile): Double =
    when (type) {
      MediaType.MOVIE -> when {
        MediaPreference.MOVIE in profile.preferredMediaTypes -> 1.0
        MediaPreference.DOCUMENTARY in profile.preferredMediaTypes && DOCUMENTARY_GENRE_ID in genreIds ->
          1.0
        MediaPreference.ANIMATION in profile.preferredMediaTypes && ANIMATION_GENRE_ID in genreIds ->
          0.9
        else -> 0.25
      }

      MediaType.TV_SHOW -> when {
        MediaPreference.TV_SERIES in profile.preferredMediaTypes -> 1.0
        MediaPreference.ANIME in profile.preferredMediaTypes &&
          ANIMATION_GENRE_ID in genreIds &&
          originalLanguage == JAPANESE_LANGUAGE -> 1.0
        MediaPreference.DOCUMENTARY in profile.preferredMediaTypes && DOCUMENTARY_GENRE_ID in genreIds ->
          0.9
        else -> 0.25
      }
    }

  private fun MediaItem.isRecommendationCandidate(): Boolean =
    id > 0 &&
      title.isNotBlank() &&
      !adult &&
      (posterPath != null || backdropPath != null) &&
      voteAverage >= MINIMUM_RATING &&
      (
        voteCount >= MINIMUM_VOTE_COUNT ||
          (releaseDate?.take(4)?.toIntOrNull() ?: 0) >= currentYear - 1 &&
          popularity >= MINIMUM_NEW_RELEASE_POPULARITY
        )

  private fun String.toCountryCode(): String? = LANGUAGE_COUNTRY[this]

  private companion object {
    const val MAX_RATING = 10.0
    const val MINIMUM_CONFIDENT_VOTES = 250.0
    const val CANDIDATE_MEAN_RATING = 6.5
    const val MINIMUM_RATING = 5.5
    const val MINIMUM_VOTE_COUNT = 80
    const val MINIMUM_NEW_RELEASE_POPULARITY = 12.0
    const val MAX_COUNTRY_SHARE = 0.35
    const val MAX_GENRE_SHARE = 0.40
    const val MAX_REASON_COUNT = 3
    const val ANIMATION_GENRE_ID = 16
    const val DOCUMENTARY_GENRE_ID = 99
    const val JAPANESE_LANGUAGE = "ja"
    val COMMON_MARKETS = setOf("US", "GB")
    val LANGUAGE_COUNTRY = mapOf(
      "id" to "ID",
      "en" to "US",
      "ko" to "KR",
      "ja" to "JP",
      "zh" to "CN",
      "hi" to "IN",
      "th" to "TH",
      "fr" to "FR",
      "de" to "DE",
      "es" to "ES",
      "it" to "IT",
    )
  }
}
