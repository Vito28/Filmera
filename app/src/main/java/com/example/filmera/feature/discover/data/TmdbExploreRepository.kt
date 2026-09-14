package com.example.filmera.feature.discover.data

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.network.FilterCombination
import com.example.filmera.core.network.IdFilter
import com.example.filmera.core.network.MovieDiscoverQuery
import com.example.filmera.core.network.MovieSort
import com.example.filmera.core.network.TmdbApiDefaults
import com.example.filmera.core.network.TmdbCatalogApi
import com.example.filmera.core.network.TmdbConfig
import com.example.filmera.core.network.TmdbLocale
import com.example.filmera.core.network.TvDiscoverQuery
import com.example.filmera.core.network.TvSort
import com.example.filmera.core.network.WatchMonetizationType
import com.example.filmera.core.network.dto.CatalogResponseDto
import com.example.filmera.core.network.dto.MovieDto
import com.example.filmera.core.network.dto.TvShowDto
import com.example.filmera.core.network.dto.WatchProviderListResponseDto
import com.example.filmera.core.network.mapper.toDomain
import com.example.filmera.core.network.safeNetworkCall
import com.example.filmera.feature.discover.domain.EXPLORE_PAGE_SIZE
import com.example.filmera.feature.discover.domain.ExploreAvailability
import com.example.filmera.feature.discover.domain.ExploreCandidate
import com.example.filmera.feature.discover.domain.ExploreCategory
import com.example.filmera.feature.discover.domain.ExploreContentType
import com.example.filmera.feature.discover.domain.ExploreFilterState
import com.example.filmera.feature.discover.domain.ExplorePage
import com.example.filmera.feature.discover.domain.ExploreProvider
import com.example.filmera.feature.discover.domain.ExploreRankingEngine
import com.example.filmera.feature.discover.domain.ExploreRegion
import com.example.filmera.feature.discover.domain.ExploreRepository
import com.example.filmera.feature.discover.domain.ExploreSort
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

class TmdbExploreRepository @Inject constructor(
  private val catalogApi: TmdbCatalogApi,
  private val config: TmdbConfig,
) : ExploreRepository {
  private val rankingEngine = ExploreRankingEngine()

  override suspend fun loadExplore(
    filters: ExploreFilterState,
    page: Int,
  ): DataResult<ExplorePage> {
    if (!config.isConfigured) return DataResult.Error(AppError.MissingApiToken)
    require(page > 0)

    return supervisorScope {
      val includesMovies = filters.contentType.includesMovies
      val includesTv = filters.contentType.includesTv
      val moviePageDeferred = if (includesMovies) {
        async {
          safeNetworkCall {
            catalogApi.discoverMovies(filters.toMovieQuery(page).toQueryMap())
          }
        }
      } else {
        null
      }
      val tvPageDeferred = if (includesTv) {
        async {
          safeNetworkCall {
            catalogApi.discoverTvShows(filters.toTvQuery(page).toQueryMap())
          }
        }
      } else {
        null
      }
      val trendingMoviesDeferred = if (includesMovies && filters.sort == ExploreSort.HOT) {
        async {
          safeNetworkCall {
            catalogApi.getTrendingMovies(
              timeWindow = HOT_TIME_WINDOW,
              language = TmdbApiDefaults.LANGUAGE,
              page = 1,
            )
          }
        }
      } else {
        null
      }
      val trendingTvDeferred = if (includesTv && filters.sort == ExploreSort.HOT) {
        async {
          safeNetworkCall {
            catalogApi.getTrendingTvShows(
              timeWindow = HOT_TIME_WINDOW,
              language = TmdbApiDefaults.LANGUAGE,
              page = 1,
            )
          }
        }
      } else {
        null
      }

      val movieResult = moviePageDeferred?.await()
      val tvResult = tvPageDeferred?.await()
      val successfulSourceCount = listOfNotNull(movieResult, tvResult)
        .count { it is DataResult.Success }
      val requestedSourceCount = listOf(includesMovies, includesTv).count { it }
      if (successfulSourceCount == 0) {
        return@supervisorScope listOfNotNull(movieResult, tvResult)
          .filterIsInstance<DataResult.Error>()
          .firstOrNull()
          ?: DataResult.Error(AppError.NotFound)
      }

      val moviePage = (movieResult as? DataResult.Success)?.value
      val tvPage = (tvResult as? DataResult.Success)?.value
      val trendingRanks = buildTrendingRanks(
        movieResult = trendingMoviesDeferred?.await(),
        tvResult = trendingTvDeferred?.await(),
      )
      val candidates = buildList {
        moviePage?.results
          ?.map(MovieDto::toDomain)
          ?.filter { it.matchesClientFilters(filters) }
          ?.forEach { media ->
            add(ExploreCandidate(media, trendingRanks[media.key]))
          }
        tvPage?.results
          ?.map(TvShowDto::toDomain)
          ?.filter { it.matchesClientFilters(filters) }
          ?.forEach { media ->
            add(ExploreCandidate(media, trendingRanks[media.key]))
          }
      }
      val items = rankingEngine.rank(
        candidates = candidates,
        sort = filters.sort,
        limit = EXPLORE_PAGE_SIZE,
        applyGlobalDiversity = filters.region == ExploreRegion.ALL,
      )
      val totalResults = (moviePage?.totalResults ?: 0) + (tvPage?.totalResults ?: 0)
      val hasMore = (moviePage?.let { page < it.totalPages } == true) ||
        (tvPage?.let { page < it.totalPages } == true)

      DataResult.Success(
        ExplorePage(
          items = items,
          page = page,
          totalResults = totalResults,
          hasMore = hasMore,
          isPartial = successfulSourceCount < requestedSourceCount,
        ),
      )
    }
  }

  override suspend fun loadProviderOptions(): DataResult<List<ExploreProvider>> {
    if (!config.isConfigured) return DataResult.Error(AppError.MissingApiToken)

    return supervisorScope {
      val movieProviders = async {
        safeNetworkCall {
          catalogApi.getMovieWatchProviders(
            language = TmdbApiDefaults.LANGUAGE,
            watchRegion = DEFAULT_WATCH_REGION,
          )
        }
      }
      val tvProviders = async {
        safeNetworkCall {
          catalogApi.getTvWatchProviders(
            language = TmdbApiDefaults.LANGUAGE,
            watchRegion = DEFAULT_WATCH_REGION,
          )
        }
      }
      val results = listOf(movieProviders.await(), tvProviders.await())
      val providerLists = results
        .filterIsInstance<DataResult.Success<WatchProviderListResponseDto>>()
        .flatMap { it.value.results }
      if (providerLists.isEmpty()) {
        results
          .filterIsInstance<DataResult.Error>()
          .firstOrNull()
          ?: DataResult.Success(emptyList())
      } else {
        DataResult.Success(
          providerLists
            .asSequence()
            .filter { it.providerId > 0 && it.providerName.isNotBlank() }
            .groupBy { it.providerId }
            .map { (_, choices) -> choices.minBy { it.displayPriority } }
            .sortedBy { it.displayPriority }
            .map { provider ->
              ExploreProvider(
                id = provider.providerId,
                name = provider.providerName,
                logoPath = provider.logoPath,
                displayPriority = provider.displayPriority,
              )
            },
        )
      }
    }
  }

  private fun buildTrendingRanks(
    movieResult: DataResult<CatalogResponseDto<MovieDto>>?,
    tvResult: DataResult<CatalogResponseDto<TvShowDto>>?,
  ): Map<MediaKey, Int> = buildMap {
    (movieResult as? DataResult.Success)
      ?.value
      ?.results
      ?.forEachIndexed { index, dto ->
        put(MediaKey(dto.id, MediaType.MOVIE), index + 1)
      }
    (tvResult as? DataResult.Success)
      ?.value
      ?.results
      ?.forEachIndexed { index, dto ->
        put(MediaKey(dto.id, MediaType.TV_SHOW), index + 1)
      }
  }

  private fun ExploreFilterState.toMovieQuery(page: Int): MovieDiscoverQuery =
    MovieDiscoverQuery(
      locale = TmdbLocale(),
      page = page,
      sortBy = when (sort) {
        ExploreSort.LATEST -> MovieSort.PRIMARY_RELEASE_DATE_DESC
        ExploreSort.TOP_RATED -> MovieSort.VOTE_AVERAGE_DESC
        ExploreSort.HOT,
        ExploreSort.POPULARITY,
        -> MovieSort.POPULARITY_DESC
      },
      primaryReleaseDateFrom = year.startDate,
      primaryReleaseDateTo = year.endDate,
      withGenres = movieGenreFilter(),
      withOriginCountry = originCountryQuery(),
      withOriginalLanguage = effectiveLanguageCode(),
      minimumRuntimeMinutes = duration.minimumMinutes,
      maximumRuntimeMinutes = duration.maximumMinutes,
      minimumVoteAverage = minimumRating,
      minimumVoteCount = minimumVoteCount ?: defaultVoteThreshold(),
      watchProviders = providerIds.toIdFilterOrNull(),
      watchMonetizationTypes = availability.toMonetizationTypes(),
    )

  private fun ExploreFilterState.toTvQuery(page: Int): TvDiscoverQuery =
    TvDiscoverQuery(
      locale = TmdbLocale(),
      page = page,
      sortBy = when (sort) {
        ExploreSort.LATEST -> TvSort.FIRST_AIR_DATE_DESC
        ExploreSort.TOP_RATED -> TvSort.VOTE_AVERAGE_DESC
        ExploreSort.HOT,
        ExploreSort.POPULARITY,
        -> TvSort.POPULARITY_DESC
      },
      firstAirDateFrom = year.startDate,
      firstAirDateTo = year.endDate,
      withGenres = tvGenreFilter(),
      withOriginCountry = originCountryQuery(),
      withOriginalLanguage = effectiveLanguageCode(),
      minimumRuntimeMinutes = duration.minimumMinutes,
      maximumRuntimeMinutes = duration.maximumMinutes,
      minimumVoteAverage = minimumRating,
      minimumVoteCount = minimumVoteCount ?: defaultVoteThreshold(),
      watchProviders = providerIds.toIdFilterOrNull(),
      watchMonetizationTypes = availability.toMonetizationTypes(),
      statuses = setOfNotNull(tvStatus.tmdbId),
    )

  private fun ExploreFilterState.movieGenreFilter(): IdFilter? {
    val classificationGenres = when (contentType) {
      ExploreContentType.ANIME -> setOf(ANIMATION_GENRE_ID)
      ExploreContentType.DOCUMENTARY -> setOf(DOCUMENTARY_GENRE_ID)
      else -> emptySet()
    }
    val categoryGenres = categories.flatMap(ExploreCategory::movieGenreIds).toSet()
    val genres = classificationGenres.ifEmpty { categoryGenres }
    return genres.toIdFilterOrNull(FilterCombination.ANY)
  }

  private fun ExploreFilterState.tvGenreFilter(): IdFilter? {
    val classificationGenres = when (contentType) {
      ExploreContentType.ANIME -> setOf(ANIMATION_GENRE_ID)
      ExploreContentType.ENTERTAINMENT -> setOf(REALITY_GENRE_ID, TALK_GENRE_ID)
      ExploreContentType.DOCUMENTARY -> setOf(DOCUMENTARY_GENRE_ID)
      else -> emptySet()
    }
    val categoryGenres = categories.flatMap(ExploreCategory::tvGenreIds).toSet()
    val genres = classificationGenres.ifEmpty { categoryGenres }
    return genres.toIdFilterOrNull(FilterCombination.ANY)
  }

  private fun ExploreFilterState.originCountryQuery(): String? {
    if (contentType == ExploreContentType.ANIME) return JAPAN_COUNTRY_CODE
    return region.originCountryCodes
      .takeIf(Set<String>::isNotEmpty)
      ?.sorted()
      ?.joinToString("|")
  }

  private fun ExploreFilterState.effectiveLanguageCode(): String? =
    if (contentType == ExploreContentType.ANIME) {
      JAPANESE_LANGUAGE_CODE
    } else {
      language.code
    }

  private fun ExploreFilterState.defaultVoteThreshold(): Int? =
    when (sort) {
      ExploreSort.HOT -> 20
      ExploreSort.LATEST -> null
      ExploreSort.TOP_RATED -> 200
      ExploreSort.POPULARITY -> 20
    }

  private fun MediaItem.matchesClientFilters(filters: ExploreFilterState): Boolean {
    if (filters.region == ExploreRegion.OTHER) {
      val countries = originCountries.toSet()
      if (countries.isEmpty() || countries.any { it in FEATURED_COUNTRY_CODES }) return false
    }
    if (filters.categories.isNotEmpty()) {
      val allowedGenres = if (type == MediaType.MOVIE) {
        filters.categories.flatMap(ExploreCategory::movieGenreIds)
      } else {
        filters.categories.flatMap(ExploreCategory::tvGenreIds)
      }
      if (genreIds.none { it in allowedGenres }) return false
    }
    return when (filters.contentType) {
      ExploreContentType.ALL,
      ExploreContentType.MOVIES,
      ExploreContentType.TV_SERIES,
      -> true
      ExploreContentType.ANIME ->
        ANIMATION_GENRE_ID in genreIds &&
          originalLanguage == JAPANESE_LANGUAGE_CODE &&
          JAPAN_COUNTRY_CODE in originCountries
      ExploreContentType.ENTERTAINMENT ->
        type == MediaType.TV_SHOW &&
          genreIds.any { it == REALITY_GENRE_ID || it == TALK_GENRE_ID }
      ExploreContentType.DOCUMENTARY -> DOCUMENTARY_GENRE_ID in genreIds
    }
  }

  private fun Set<Int>.toIdFilterOrNull(
    combination: FilterCombination = FilterCombination.ALL,
  ): IdFilter? =
    takeIf(Set<Int>::isNotEmpty)?.let { IdFilter(it, combination) }

  private fun ExploreAvailability.toMonetizationTypes(): Set<WatchMonetizationType> =
    when (this) {
      ExploreAvailability.ANY -> emptySet()
      ExploreAvailability.STREAM -> setOf(WatchMonetizationType.FLATRATE)
      ExploreAvailability.FREE -> setOf(WatchMonetizationType.FREE)
      ExploreAvailability.WITH_ADS -> setOf(WatchMonetizationType.ADS)
      ExploreAvailability.RENT -> setOf(WatchMonetizationType.RENT)
      ExploreAvailability.BUY -> setOf(WatchMonetizationType.BUY)
    }

  private val ExploreContentType.includesMovies: Boolean
    get() = this in setOf(
      ExploreContentType.ALL,
      ExploreContentType.MOVIES,
      ExploreContentType.ANIME,
      ExploreContentType.DOCUMENTARY,
    )

  private val ExploreContentType.includesTv: Boolean
    get() = this in setOf(
      ExploreContentType.ALL,
      ExploreContentType.TV_SERIES,
      ExploreContentType.ANIME,
      ExploreContentType.ENTERTAINMENT,
      ExploreContentType.DOCUMENTARY,
    )

  private companion object {
    const val HOT_TIME_WINDOW = "day"
    const val DEFAULT_WATCH_REGION = "ID"
    const val JAPAN_COUNTRY_CODE = "JP"
    const val JAPANESE_LANGUAGE_CODE = "ja"
    const val ANIMATION_GENRE_ID = 16
    const val DOCUMENTARY_GENRE_ID = 99
    const val REALITY_GENRE_ID = 10764
    const val TALK_GENRE_ID = 10767
    val FEATURED_COUNTRY_CODES = ExploreRegion.entries
      .filterNot { it == ExploreRegion.ALL || it == ExploreRegion.OTHER }
      .flatMap(ExploreRegion::originCountryCodes)
      .toSet()
  }
}
