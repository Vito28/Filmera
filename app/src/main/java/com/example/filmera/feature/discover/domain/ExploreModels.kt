package com.example.filmera.feature.discover.domain

import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.math.ln

enum class ExploreSort {
  HOT,
  LATEST,
  TOP_RATED,
  POPULARITY,
}

enum class ExploreContentType {
  ALL,
  MOVIES,
  TV_SERIES,
  ANIME,
  ENTERTAINMENT,
  DOCUMENTARY,
}

enum class ExploreCategory(
  val movieGenreIds: Set<Int>,
  val tvGenreIds: Set<Int>,
) {
  ACTION(setOf(28), setOf(10759)),
  DRAMA(setOf(18), setOf(18)),
  ROMANCE(setOf(10749), setOf(18)),
  COMEDY(setOf(35), setOf(35)),
  FANTASY(setOf(14), setOf(10765)),
  THRILLER(setOf(53), setOf(9648)),
  ADVENTURE(setOf(12), setOf(10759)),
  CRIME(setOf(80), setOf(80)),
  MYSTERY(setOf(9648), setOf(9648)),
  SCIENCE_FICTION(setOf(878), setOf(10765)),
  FAMILY(setOf(10751), setOf(10751)),
  HORROR(setOf(27), setOf(9648)),
  HISTORY(setOf(36), setOf(36)),
  MUSIC(setOf(10402), setOf(10766)),
  WAR(setOf(10752), setOf(10768)),
  WESTERN(setOf(37), setOf(37)),
}

enum class ExploreRegion(
  val originCountryCodes: Set<String>,
) {
  ALL(emptySet()),
  INDONESIA(setOf("ID")),
  KOREA(setOf("KR")),
  JAPAN(setOf("JP")),
  CHINA(setOf("CN")),
  INDIA(setOf("IN")),
  AMERICA(setOf("US")),
  THAILAND(setOf("TH")),
  UNITED_KINGDOM(setOf("GB")),
  EUROPE(
    setOf(
      "FR",
      "DE",
      "ES",
      "IT",
      "SE",
      "NO",
      "DK",
      "NL",
      "BE",
      "PL",
    ),
  ),
  OTHER(emptySet()),
}

enum class ExploreYear(
  val startDate: String?,
  val endDate: String?,
) {
  ALL(null, null),
  YEAR_2026("2026-01-01", "2026-12-31"),
  YEAR_2025("2025-01-01", "2025-12-31"),
  YEAR_2024("2024-01-01", "2024-12-31"),
  YEAR_2023("2023-01-01", "2023-12-31"),
  YEARS_2020S("2020-01-01", "2029-12-31"),
  YEARS_2010S("2010-01-01", "2019-12-31"),
  YEARS_2000S("2000-01-01", "2009-12-31"),
  BEFORE_2000(null, "1999-12-31"),
}

enum class ExploreLanguage(val code: String?) {
  ALL(null),
  ENGLISH("en"),
  INDONESIAN("id"),
  KOREAN("ko"),
  JAPANESE("ja"),
  CHINESE("zh"),
  HINDI("hi"),
  THAI("th"),
  SPANISH("es"),
  FRENCH("fr"),
}

enum class ExploreAvailability(val apiValue: String?) {
  ANY(null),
  STREAM("flatrate"),
  FREE("free"),
  WITH_ADS("ads"),
  RENT("rent"),
  BUY("buy"),
}

enum class ExploreDuration(
  val minimumMinutes: Int?,
  val maximumMinutes: Int?,
) {
  ANY(null, null),
  SHORT(null, 60),
  STANDARD(60, 120),
  LONG(120, null),
}

enum class ExploreTvStatus(val tmdbId: Int?) {
  ANY(null),
  RETURNING(0),
  PLANNED(1),
  IN_PRODUCTION(2),
  ENDED(3),
  CANCELED(4),
  PILOT(5),
}

enum class ExploreLayoutMode {
  GRID,
  LIST,
}

enum class MediaClassification {
  ANIME,
  ENTERTAINMENT,
  DOCUMENTARY,
}

data class ExploreProvider(
  val id: Int,
  val name: String,
  val logoPath: String?,
  val displayPriority: Int,
)

data class ExploreFilterState(
  val sort: ExploreSort = ExploreSort.HOT,
  val contentType: ExploreContentType = ExploreContentType.ALL,
  val categories: Set<ExploreCategory> = emptySet(),
  val region: ExploreRegion = ExploreRegion.ALL,
  val year: ExploreYear = ExploreYear.ALL,
  val language: ExploreLanguage = ExploreLanguage.ALL,
  val providerIds: Set<Int> = emptySet(),
  val availability: ExploreAvailability = ExploreAvailability.ANY,
  val minimumRating: Double? = null,
  val minimumVoteCount: Int? = null,
  val duration: ExploreDuration = ExploreDuration.ANY,
  val tvStatus: ExploreTvStatus = ExploreTvStatus.ANY,
) {
  val isDefault: Boolean
    get() = this == ExploreFilterState()
}

data class ExploreMediaItem(
  val media: MediaItem,
  val score: Double,
  val classifications: Set<MediaClassification>,
)

data class ExplorePage(
  val items: List<ExploreMediaItem>,
  val page: Int,
  val totalResults: Int,
  val hasMore: Boolean,
  val isPartial: Boolean,
)

data class ExploreContent(
  val items: List<ExploreMediaItem>,
  val currentPage: Int,
  val totalResults: Int,
  val hasMore: Boolean,
  val isPartial: Boolean,
)

data class ExploreCandidate(
  val media: MediaItem,
  val trendingRank: Int?,
)

interface ExploreRepository {
  suspend fun loadExplore(
    filters: ExploreFilterState,
    page: Int,
  ): DataResult<ExplorePage>

  suspend fun loadProviderOptions(): DataResult<List<ExploreProvider>>
}

class ExploreRankingEngine(
  private val currentDate: LocalDate = LocalDate.now(),
) {

  fun rank(
    candidates: List<ExploreCandidate>,
    sort: ExploreSort,
    limit: Int = EXPLORE_PAGE_SIZE,
    applyGlobalDiversity: Boolean = true,
  ): List<ExploreMediaItem> {
    require(limit > 0)

    val validCandidates = candidates
      .asSequence()
      .filter { candidate ->
        val media = candidate.media
        media.id > 0 &&
          media.title.isNotBlank() &&
          !media.adult &&
          (sort != ExploreSort.LATEST || media.passesLatestQualityFloor())
      }
      .distinctBy { it.media.key }
      .toList()
    if (validCandidates.isEmpty()) return emptyList()

    val groupedByType = validCandidates.groupBy { it.media.type }
    val normalizedPopularity = groupedByType.values
      .flatMap { group ->
        val maximum = group.maxOfOrNull { it.media.popularity }?.coerceAtLeast(1.0) ?: 1.0
        group.map { it.media.key to (it.media.popularity / maximum).coerceIn(0.0, 1.0) }
      }
      .toMap()
    val normalizedEngagement = groupedByType.values
      .flatMap { group ->
        val maximum = group
          .maxOfOrNull { ln(1.0 + it.media.voteCount.coerceAtLeast(0)) }
          ?.coerceAtLeast(1.0)
          ?: 1.0
        group.map { candidate ->
          candidate.media.key to
            (ln(1.0 + candidate.media.voteCount.coerceAtLeast(0)) / maximum)
              .coerceIn(0.0, 1.0)
        }
      }
      .toMap()
    val averageRating = validCandidates
      .map { it.media.voteAverage }
      .filter { it > 0.0 }
      .average()
      .takeUnless(Double::isNaN)
      ?: DEFAULT_CANDIDATE_RATING

    val scored = validCandidates.map { candidate ->
      val media = candidate.media
      val weightedQuality = weightedRating(
        rating = media.voteAverage,
        votes = media.voteCount,
        candidateAverage = averageRating,
        minimumConfidence = if (sort == ExploreSort.TOP_RATED) 500 else 100,
      ) / 10.0
      val popularity = normalizedPopularity[media.key] ?: 0.0
      val engagement = normalizedEngagement[media.key] ?: 0.0
      val trendingMomentum = candidate.trendingRank
        ?.let { rank -> (1.0 - ((rank - 1).coerceAtLeast(0) / 40.0)).coerceIn(0.0, 1.0) }
        ?: 0.0
      val releaseRelevance = releaseRelevance(media.releaseDate)
      val score = when (sort) {
        ExploreSort.HOT ->
          0.30 * trendingMomentum +
            0.25 * popularity +
            0.20 * weightedQuality +
            0.15 * engagement +
            0.10 * releaseRelevance
        ExploreSort.LATEST ->
          0.70 * releaseRelevance +
            0.20 * weightedQuality +
            0.10 * popularity
        ExploreSort.TOP_RATED -> weightedQuality
        ExploreSort.POPULARITY ->
          0.65 * popularity +
            0.20 * engagement +
            0.10 * weightedQuality +
            0.05 * trendingMomentum
      }
      ExploreMediaItem(
        media = media,
        score = score.coerceIn(0.0, 1.0),
        classifications = media.classifications(),
      )
    }
    val sorted = when (sort) {
      ExploreSort.LATEST -> scored.sortedWith(
        compareByDescending<ExploreMediaItem> { it.media.releaseDate.orEmpty() }
          .thenByDescending(ExploreMediaItem::score),
      )
      else -> scored.sortedByDescending(ExploreMediaItem::score)
    }

    return if (applyGlobalDiversity) {
      diversifyByCountry(sorted, limit)
    } else {
      sorted.take(limit)
    }
  }

  private fun weightedRating(
    rating: Double,
    votes: Int,
    candidateAverage: Double,
    minimumConfidence: Int,
  ): Double {
    val safeVotes = votes.coerceAtLeast(0).toDouble()
    val confidence = minimumConfidence.toDouble()
    return (safeVotes / (safeVotes + confidence) * rating.coerceIn(0.0, 10.0)) +
      (confidence / (safeVotes + confidence) * candidateAverage.coerceIn(0.0, 10.0))
  }

  private fun releaseRelevance(releaseDate: String?): Double {
    val date = releaseDate.toLocalDateOrNull() ?: return 0.0
    val ageInDays = kotlin.math.abs(currentDate.toEpochDay() - date.toEpochDay())
    return when {
      ageInDays <= 30 -> 1.0
      ageInDays <= 180 -> 0.85
      ageInDays <= 365 -> 0.70
      ageInDays <= 1_095 -> 0.45
      else -> 0.20
    }
  }

  private fun diversifyByCountry(
    ranked: List<ExploreMediaItem>,
    limit: Int,
  ): List<ExploreMediaItem> {
    val countryLimit = (limit * MAXIMUM_COUNTRY_SHARE).toInt().coerceAtLeast(1)
    val selected = mutableListOf<ExploreMediaItem>()
    val deferred = mutableListOf<ExploreMediaItem>()
    val countryCounts = mutableMapOf<String, Int>()

    ranked.forEach { item ->
      if (selected.size >= limit) return@forEach
      val country = item.media.originCountries.firstOrNull()
      if (country == null || countryCounts.getOrDefault(country, 0) < countryLimit) {
        selected += item
        country?.let { countryCounts[it] = countryCounts.getOrDefault(it, 0) + 1 }
      } else {
        deferred += item
      }
    }
    if (selected.size < limit) {
      selected += deferred.take(limit - selected.size)
    }
    return selected
  }

  private fun MediaItem.passesLatestQualityFloor(): Boolean =
    posterPath != null &&
      releaseDate.toLocalDateOrNull() != null &&
      (voteCount < 50 || voteAverage >= 5.5)

  private fun MediaItem.classifications(): Set<MediaClassification> = buildSet {
    if (
      ANIMATION_GENRE_ID in genreIds &&
      originalLanguage == "ja" &&
      "JP" in originCountries
    ) {
      add(MediaClassification.ANIME)
    }
    if (
      type == MediaType.TV_SHOW &&
      genreIds.any { it == REALITY_GENRE_ID || it == TALK_GENRE_ID }
    ) {
      add(MediaClassification.ENTERTAINMENT)
    }
    if (DOCUMENTARY_GENRE_ID in genreIds) {
      add(MediaClassification.DOCUMENTARY)
    }
  }

  private fun String?.toLocalDateOrNull(): LocalDate? =
    try {
      this?.takeIf(String::isNotBlank)?.let(LocalDate::parse)
    } catch (_: DateTimeParseException) {
      null
    }

  private companion object {
    const val DEFAULT_CANDIDATE_RATING = 6.5
    const val MAXIMUM_COUNTRY_SHARE = 0.40
    const val ANIMATION_GENRE_ID = 16
    const val DOCUMENTARY_GENRE_ID = 99
    const val REALITY_GENRE_ID = 10764
    const val TALK_GENRE_ID = 10767
  }
}

const val EXPLORE_PAGE_SIZE = 20
