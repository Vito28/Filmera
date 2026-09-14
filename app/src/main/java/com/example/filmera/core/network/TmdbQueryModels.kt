package com.example.filmera.core.network

import com.example.filmera.core.network.dto.CatalogResponseDto
import com.example.filmera.core.network.dto.MovieDto
import com.example.filmera.core.network.dto.TvShowDto

/**
 * Locale used for localized TMDB metadata.
 *
 * TMDB localizes responses through the `language` query. Translation lists are
 * separate resource endpoints such as `movie/{id}/translations`.
 */
data class TmdbLocale(
  val language: String = TmdbApiDefaults.LANGUAGE,
  val region: String = TmdbApiDefaults.REGION,
  val watchRegion: String = region,
) {
  init {
    require(LANGUAGE_PATTERN.matches(language)) {
      "TMDB language must use a language-region value such as id-ID"
    }
    require(REGION_PATTERN.matches(region)) {
      "TMDB region must be a two-letter ISO-3166-1 code"
    }
    require(REGION_PATTERN.matches(watchRegion)) {
      "TMDB watch region must be a two-letter ISO-3166-1 code"
    }
  }

  private companion object {
    val LANGUAGE_PATTERN = Regex("^[a-z]{2,3}-[A-Z]{2}$")
    val REGION_PATTERN = Regex("^[A-Z]{2}$")
  }
}

enum class TmdbTimeWindow(val apiValue: String) {
  DAY("day"),
  WEEK("week"),
}

enum class TmdbCatalogFeed {
  NOW_PLAYING_MOVIES,
  POPULAR_MOVIES,
  UPCOMING_MOVIES,
  TOP_RATED_MOVIES,
  TRENDING_MOVIES,
  ANIME_MOVIES,
  KOREAN_MOVIES,
  CHINESE_MOVIES,
  SHORT_MOVIES,
  AIRING_TODAY_TV,
  ON_THE_AIR_TV,
  POPULAR_TV,
  TOP_RATED_TV,
  TRENDING_TV,
  ANIME_TV,
  WESTERN_TV,
  KOREAN_DRAMA,
  CHINESE_DRAMA,
}

data class TmdbCatalogRequest(
  val feed: TmdbCatalogFeed,
  val locale: TmdbLocale = TmdbLocale(),
  val page: Int = 1,
  val timeWindow: TmdbTimeWindow = TmdbTimeWindow.WEEK,
) {
  init {
    require(page > 0) { "TMDB page starts at 1" }
  }
}

sealed interface TmdbCatalogPageDto {
  data class Movies(
    val response: CatalogResponseDto<MovieDto>,
  ) : TmdbCatalogPageDto

  data class TvShows(
    val response: CatalogResponseDto<TvShowDto>,
  ) : TmdbCatalogPageDto
}

enum class FilterCombination(val separator: String) {
  ALL(","),
  ANY("|"),
}

data class IdFilter(
  val ids: Set<Int>,
  val combination: FilterCombination = FilterCombination.ALL,
) {
  init {
    require(ids.isNotEmpty()) { "At least one positive TMDB id is required" }
    require(ids.all { it > 0 }) { "TMDB filter ids must be positive" }
  }

  fun toQueryValue(): String =
    ids.sorted().joinToString(combination.separator)
}

enum class MovieSort(val apiValue: String) {
  POPULARITY_ASC("popularity.asc"),
  POPULARITY_DESC("popularity.desc"),
  PRIMARY_RELEASE_DATE_ASC("primary_release_date.asc"),
  PRIMARY_RELEASE_DATE_DESC("primary_release_date.desc"),
  REVENUE_ASC("revenue.asc"),
  REVENUE_DESC("revenue.desc"),
  TITLE_ASC("title.asc"),
  TITLE_DESC("title.desc"),
  VOTE_AVERAGE_ASC("vote_average.asc"),
  VOTE_AVERAGE_DESC("vote_average.desc"),
  VOTE_COUNT_ASC("vote_count.asc"),
  VOTE_COUNT_DESC("vote_count.desc"),
}

enum class TvSort(val apiValue: String) {
  FIRST_AIR_DATE_ASC("first_air_date.asc"),
  FIRST_AIR_DATE_DESC("first_air_date.desc"),
  NAME_ASC("name.asc"),
  NAME_DESC("name.desc"),
  POPULARITY_ASC("popularity.asc"),
  POPULARITY_DESC("popularity.desc"),
  VOTE_AVERAGE_ASC("vote_average.asc"),
  VOTE_AVERAGE_DESC("vote_average.desc"),
  VOTE_COUNT_ASC("vote_count.asc"),
  VOTE_COUNT_DESC("vote_count.desc"),
}

enum class WatchMonetizationType(val apiValue: String) {
  ADS("ads"),
  BUY("buy"),
  FLATRATE("flatrate"),
  FREE("free"),
  RENT("rent"),
}

data class MovieDiscoverQuery(
  val locale: TmdbLocale = TmdbLocale(),
  val page: Int = 1,
  val sortBy: MovieSort = MovieSort.POPULARITY_DESC,
  val includeAdult: Boolean = false,
  val includeVideo: Boolean = false,
  val primaryReleaseDateFrom: String? = null,
  val primaryReleaseDateTo: String? = null,
  val releaseDateFrom: String? = null,
  val releaseDateTo: String? = null,
  val releaseTypes: IdFilter? = null,
  val withGenres: IdFilter? = null,
  val withoutGenres: IdFilter? = null,
  val withKeywords: IdFilter? = null,
  val withoutKeywords: IdFilter? = null,
  val withCast: IdFilter? = null,
  val withCrew: IdFilter? = null,
  val withPeople: IdFilter? = null,
  val withCompanies: IdFilter? = null,
  val withOriginCountry: String? = null,
  val withOriginalLanguage: String? = null,
  val minimumRuntimeMinutes: Int? = null,
  val maximumRuntimeMinutes: Int? = null,
  val minimumVoteAverage: Double? = null,
  val maximumVoteAverage: Double? = null,
  val minimumVoteCount: Int? = null,
  val maximumVoteCount: Int? = null,
  val certificationCountry: String? = null,
  val certification: String? = null,
  val minimumCertification: String? = null,
  val maximumCertification: String? = null,
  val watchProviders: IdFilter? = null,
  val watchMonetizationTypes: Set<WatchMonetizationType> = emptySet(),
) {
  init {
    require(page > 0) { "TMDB page starts at 1" }
    require(minimumRuntimeMinutes == null || minimumRuntimeMinutes >= 0)
    require(maximumRuntimeMinutes == null || maximumRuntimeMinutes >= 0)
    require(minimumVoteCount == null || minimumVoteCount >= 0)
    require(maximumVoteCount == null || maximumVoteCount >= 0)
  }

  fun toQueryMap(): Map<String, String> = buildMap {
    put("language", locale.language)
    put("region", locale.region)
    put("watch_region", locale.watchRegion)
    put("page", page.toString())
    put("sort_by", sortBy.apiValue)
    put("include_adult", includeAdult.toString())
    put("include_video", includeVideo.toString())
    putIfNotNull("primary_release_date.gte", primaryReleaseDateFrom)
    putIfNotNull("primary_release_date.lte", primaryReleaseDateTo)
    putIfNotNull("release_date.gte", releaseDateFrom)
    putIfNotNull("release_date.lte", releaseDateTo)
    putIfNotNull("with_release_type", releaseTypes?.toQueryValue())
    putIfNotNull("with_genres", withGenres?.toQueryValue())
    putIfNotNull("without_genres", withoutGenres?.toQueryValue())
    putIfNotNull("with_keywords", withKeywords?.toQueryValue())
    putIfNotNull("without_keywords", withoutKeywords?.toQueryValue())
    putIfNotNull("with_cast", withCast?.toQueryValue())
    putIfNotNull("with_crew", withCrew?.toQueryValue())
    putIfNotNull("with_people", withPeople?.toQueryValue())
    putIfNotNull("with_companies", withCompanies?.toQueryValue())
    putIfNotNull("with_origin_country", withOriginCountry)
    putIfNotNull("with_original_language", withOriginalLanguage)
    putIfNotNull("with_runtime.gte", minimumRuntimeMinutes?.toString())
    putIfNotNull("with_runtime.lte", maximumRuntimeMinutes?.toString())
    putIfNotNull("vote_average.gte", minimumVoteAverage?.toString())
    putIfNotNull("vote_average.lte", maximumVoteAverage?.toString())
    putIfNotNull("vote_count.gte", minimumVoteCount?.toString())
    putIfNotNull("vote_count.lte", maximumVoteCount?.toString())
    putIfNotNull("certification_country", certificationCountry)
    putIfNotNull("certification", certification)
    putIfNotNull("certification.gte", minimumCertification)
    putIfNotNull("certification.lte", maximumCertification)
    putIfNotNull("with_watch_providers", watchProviders?.toQueryValue())
    putIfNotNull(
      "with_watch_monetization_types",
      watchMonetizationTypes
        .map(WatchMonetizationType::apiValue)
        .sorted()
        .takeIf { it.isNotEmpty() }
        ?.joinToString("|"),
    )
  }

  companion object {
    fun japaneseAnime(
      locale: TmdbLocale = TmdbLocale(),
      page: Int = 1,
    ) = MovieDiscoverQuery(
      locale = locale,
      page = page,
      withGenres = IdFilter(setOf(ANIMATION_GENRE_ID)),
      withOriginCountry = "JP",
      withOriginalLanguage = "ja",
    )

    fun korean(
      locale: TmdbLocale = TmdbLocale(),
      page: Int = 1,
    ) = MovieDiscoverQuery(
      locale = locale,
      page = page,
      withOriginCountry = "KR",
      withOriginalLanguage = "ko",
    )

    fun chinese(
      locale: TmdbLocale = TmdbLocale(),
      page: Int = 1,
    ) = MovieDiscoverQuery(
      locale = locale,
      page = page,
      withOriginCountry = "CN",
      withOriginalLanguage = "zh",
    )

    private const val ANIMATION_GENRE_ID = 16
  }
}

data class TvDiscoverQuery(
  val locale: TmdbLocale = TmdbLocale(),
  val page: Int = 1,
  val sortBy: TvSort = TvSort.POPULARITY_DESC,
  val includeAdult: Boolean = false,
  val airDateFrom: String? = null,
  val airDateTo: String? = null,
  val firstAirDateFrom: String? = null,
  val firstAirDateTo: String? = null,
  val firstAirDateYear: Int? = null,
  val withGenres: IdFilter? = null,
  val withoutGenres: IdFilter? = null,
  val withKeywords: IdFilter? = null,
  val withoutKeywords: IdFilter? = null,
  val withNetworks: IdFilter? = null,
  val withCompanies: IdFilter? = null,
  val withOriginCountry: String? = null,
  val withOriginalLanguage: String? = null,
  val minimumRuntimeMinutes: Int? = null,
  val maximumRuntimeMinutes: Int? = null,
  val minimumVoteAverage: Double? = null,
  val maximumVoteAverage: Double? = null,
  val minimumVoteCount: Int? = null,
  val maximumVoteCount: Int? = null,
  val includeNullFirstAirDates: Boolean = false,
  val screenedTheatrically: Boolean? = null,
  val timezone: String = TmdbApiDefaults.TIMEZONE,
  val watchProviders: IdFilter? = null,
  val watchMonetizationTypes: Set<WatchMonetizationType> = emptySet(),
  val statuses: Set<Int> = emptySet(),
  val types: IdFilter? = null,
) {
  init {
    require(page > 0) { "TMDB page starts at 1" }
    require(firstAirDateYear == null || firstAirDateYear > 1800)
    require(minimumRuntimeMinutes == null || minimumRuntimeMinutes >= 0)
    require(maximumRuntimeMinutes == null || maximumRuntimeMinutes >= 0)
    require(statuses.all { it in 0..5 }) { "TMDB TV status must be between 0 and 5" }
  }

  fun toQueryMap(): Map<String, String> = buildMap {
    put("language", locale.language)
    put("watch_region", locale.watchRegion)
    put("page", page.toString())
    put("sort_by", sortBy.apiValue)
    put("include_adult", includeAdult.toString())
    put("include_null_first_air_dates", includeNullFirstAirDates.toString())
    put("timezone", timezone)
    putIfNotNull("air_date.gte", airDateFrom)
    putIfNotNull("air_date.lte", airDateTo)
    putIfNotNull("first_air_date.gte", firstAirDateFrom)
    putIfNotNull("first_air_date.lte", firstAirDateTo)
    putIfNotNull("first_air_date_year", firstAirDateYear?.toString())
    putIfNotNull("with_genres", withGenres?.toQueryValue())
    putIfNotNull("without_genres", withoutGenres?.toQueryValue())
    putIfNotNull("with_keywords", withKeywords?.toQueryValue())
    putIfNotNull("without_keywords", withoutKeywords?.toQueryValue())
    putIfNotNull("with_networks", withNetworks?.toQueryValue())
    putIfNotNull("with_companies", withCompanies?.toQueryValue())
    putIfNotNull("with_origin_country", withOriginCountry)
    putIfNotNull("with_original_language", withOriginalLanguage)
    putIfNotNull("with_runtime.gte", minimumRuntimeMinutes?.toString())
    putIfNotNull("with_runtime.lte", maximumRuntimeMinutes?.toString())
    putIfNotNull("vote_average.gte", minimumVoteAverage?.toString())
    putIfNotNull("vote_average.lte", maximumVoteAverage?.toString())
    putIfNotNull("vote_count.gte", minimumVoteCount?.toString())
    putIfNotNull("vote_count.lte", maximumVoteCount?.toString())
    putIfNotNull("screened_theatrically", screenedTheatrically?.toString())
    putIfNotNull("with_watch_providers", watchProviders?.toQueryValue())
    putIfNotNull(
      "with_watch_monetization_types",
      watchMonetizationTypes
        .map(WatchMonetizationType::apiValue)
        .sorted()
        .takeIf { it.isNotEmpty() }
        ?.joinToString("|"),
    )
    putIfNotNull(
      "with_status",
      statuses
        .sorted()
        .takeIf(List<Int>::isNotEmpty)
        ?.joinToString("|"),
    )
    putIfNotNull("with_type", types?.toQueryValue())
  }

  companion object {
    fun japaneseAnime(
      locale: TmdbLocale = TmdbLocale(),
      page: Int = 1,
    ) = TvDiscoverQuery(
      locale = locale,
      page = page,
      withGenres = IdFilter(setOf(ANIMATION_GENRE_ID)),
      withOriginCountry = "JP",
      withOriginalLanguage = "ja",
    )

    fun koreanDrama(
      locale: TmdbLocale = TmdbLocale(),
      page: Int = 1,
    ) = TvDiscoverQuery(
      locale = locale,
      page = page,
      withGenres = IdFilter(setOf(DRAMA_GENRE_ID)),
      withOriginCountry = "KR",
      withOriginalLanguage = "ko",
    )

    fun chineseDrama(
      locale: TmdbLocale = TmdbLocale(),
      page: Int = 1,
    ) = TvDiscoverQuery(
      locale = locale,
      page = page,
      withGenres = IdFilter(setOf(DRAMA_GENRE_ID)),
      withOriginCountry = "CN",
      withOriginalLanguage = "zh",
    )

    private const val ANIMATION_GENRE_ID = 16
    private const val DRAMA_GENRE_ID = 18
  }
}

private fun MutableMap<String, String>.putIfNotNull(
  key: String,
  value: String?,
) {
  if (!value.isNullOrBlank()) put(key, value)
}
