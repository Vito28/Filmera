package com.example.filmera.feature.home.data

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.common.getOrNull
import com.example.filmera.core.common.map
import com.example.filmera.core.model.CastMember
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.network.FilterCombination
import com.example.filmera.core.network.IdFilter
import com.example.filmera.core.network.MovieDiscoverQuery
import com.example.filmera.core.network.MovieSort
import com.example.filmera.core.network.TmdbApiDefaults
import com.example.filmera.core.network.TmdbCatalogApi
import com.example.filmera.core.network.TmdbConfig
import com.example.filmera.core.network.TmdbLocale
import com.example.filmera.core.network.TmdbMovieApi
import com.example.filmera.core.network.TmdbSearchApi
import com.example.filmera.core.network.TmdbTvSeriesApi
import com.example.filmera.core.network.TvDiscoverQuery
import com.example.filmera.core.network.TvSort
import com.example.filmera.core.network.dto.CatalogResponseDto
import com.example.filmera.core.network.dto.CollectionDto
import com.example.filmera.core.network.dto.MovieDto
import com.example.filmera.core.network.dto.TvShowDto
import com.example.filmera.core.network.mapper.toDomain
import com.example.filmera.core.network.safeNetworkCall
import com.example.filmera.feature.home.domain.HomeChannel
import com.example.filmera.feature.home.domain.HomeCollection
import com.example.filmera.feature.home.domain.HomeCollectionKind
import com.example.filmera.feature.home.domain.HomeContent
import com.example.filmera.feature.home.domain.HomeFeedKey
import com.example.filmera.feature.home.domain.HomeGenre
import com.example.filmera.feature.home.domain.HomePerson
import com.example.filmera.feature.home.domain.HomeRepository
import com.example.filmera.feature.home.domain.HomeSection
import com.example.filmera.feature.home.domain.HomeSectionPage
import com.example.filmera.feature.home.domain.HomeSectionType
import com.example.filmera.feature.home.domain.HomeSpotlight
import com.example.filmera.feature.home.domain.HomeSupplementalType
import com.example.filmera.feature.home.domain.HomeTrailer
import com.example.filmera.feature.home.domain.enforceSessionMediaLimit
import com.example.filmera.feature.home.domain.selectDiverseMedia
import com.example.filmera.feature.home.domain.selectHeroMedia
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.supervisorScope

class TmdbHomeRepository @Inject constructor(
  private val catalogApi: TmdbCatalogApi,
  private val movieApi: TmdbMovieApi,
  private val searchApi: TmdbSearchApi,
  private val tvSeriesApi: TmdbTvSeriesApi,
  private val config: TmdbConfig,
) : HomeRepository {

  override suspend fun loadHomeContent(feedKey: HomeFeedKey): DataResult<HomeContent> =
    observeHomeContent(feedKey).last()

  override fun observeHomeContent(feedKey: HomeFeedKey): Flow<DataResult<HomeContent>> = flow {
    if (!config.isConfigured) {
      emit(DataResult.Error(AppError.MissingApiToken))
      return@flow
    }

    val normalizedKey = feedKey.copy(animeTopic = feedKey.normalizedAnimeTopic)
    val locale = normalizedKey.toLocale()
    val sectionResults = mutableMapOf<HomeSectionType, DataResult<MediaPage>>()
    var genresResult: DataResult<List<HomeGenre>>? = null
    var peopleResult: DataResult<List<HomePerson>>? = null
    var trailersResult: DataResult<List<HomeTrailer>>? = null
    var collectionsResult: DataResult<List<HomeCollection>>? = null

    fun currentContent(): HomeContent {
      val sections = PAGED_SECTION_TYPES.map { sectionType ->
        sectionResults[sectionType]?.let { resultSection(sectionType, it) }
          ?: HomeSection(type = sectionType, isInitialLoading = true)
      }
      val spotlights = buildSpotlights(
        sections
          .asSequence()
          .filter { section ->
            section.type == HomeSectionType.TRENDING ||
              section.type == HomeSectionType.EDITORS_PICKS
          }
          .flatMap { it.items.asSequence() }
          .toList(),
      )
      return HomeContent(
        feedKey = normalizedKey,
        sections = sections,
        genres = genresResult?.getOrNull().orEmpty().take(GENRE_HIGHLIGHT_LIMIT),
        allGenres = genresResult?.getOrNull().orEmpty(),
        people = peopleResult?.getOrNull().orEmpty(),
        trailers = trailersResult?.getOrNull().orEmpty(),
        collections = collectionsResult?.getOrNull().orEmpty(),
        spotlights = spotlights,
        genresError = (genresResult as? DataResult.Error)?.error,
        peopleError = (peopleResult as? DataResult.Error)?.error,
        trailersError = (trailersResult as? DataResult.Error)?.error,
        collectionsError = (collectionsResult as? DataResult.Error)?.error,
        loadingSupplementals = buildSet {
          if (genresResult == null) add(HomeSupplementalType.GENRES)
          if (peopleResult == null) add(HomeSupplementalType.PEOPLE)
          if (trailersResult == null) add(HomeSupplementalType.TRAILERS)
          if (collectionsResult == null) add(HomeSupplementalType.COLLECTIONS)
        },
        hasPartialFailures = sections.any { it.error != null } ||
          genresResult is DataResult.Error ||
          peopleResult is DataResult.Error ||
          trailersResult is DataResult.Error ||
          collectionsResult is DataResult.Error,
      ).enforceSessionMediaLimit()
    }

    SECTION_LOAD_PHASES.forEach { phase ->
      val phaseResults = supervisorScope {
        phase.associateWith { sectionType ->
          async { loadMediaPage(normalizedKey, sectionType, FIRST_PAGE) }
        }.mapValues { (_, deferred) -> deferred.await() }
      }
      sectionResults.putAll(phaseResults)
      emit(DataResult.Success(currentContent()))
    }

    val completedSections = currentContent().sections
    val allMedia = completedSections
      .asSequence()
      .flatMap { it.items.asSequence() }
      .distinctBy(MediaItem::key)
      .toList()
    val heroItems = completedSections
      .firstOrNull { it.type == HomeSectionType.HERO }
      ?.items
      .orEmpty()

    genresResult = loadGenreNames(locale.language).map { genreNames ->
      buildGenreHighlights(
        items = allMedia,
        genreNames = genreNames,
        limit = Int.MAX_VALUE,
      )
    }
    emit(DataResult.Success(currentContent()))

    peopleResult = loadPopularPeople(
      featuredItems = heroItems,
      language = locale.language,
    )
    emit(DataResult.Success(currentContent()))

    trailersResult = loadTrailers(
      mediaItems = selectDiverseMedia(
        heroItems,
        completedSections
          .firstOrNull { it.type == HomeSectionType.UPCOMING }
          ?.items
          .orEmpty(),
        limit = TRAILER_REQUEST_LIMIT,
      ),
      language = locale.language,
    )
    emit(DataResult.Success(currentContent()))

    collectionsResult = loadCollections(
      mediaItems = allMedia,
      language = locale.language,
    )
    val finalContent = currentContent()
    if (finalContent.hasVisibleContent) {
      emit(DataResult.Success(finalContent))
    } else {
      emit(
        firstError(
          *sectionResults.values.toTypedArray(),
          requireNotNull(genresResult),
          requireNotNull(peopleResult),
          requireNotNull(trailersResult),
          requireNotNull(collectionsResult),
        ) ?: DataResult.Success(finalContent),
      )
    }
  }

  override suspend fun loadSectionPage(
    feedKey: HomeFeedKey,
    sectionType: HomeSectionType,
    page: Int,
  ): DataResult<HomeSectionPage> {
    if (!config.isConfigured) return DataResult.Error(AppError.MissingApiToken)
    require(page > 0) { "TMDB pages start at 1" }

    return loadMediaPage(
      feedKey = feedKey.copy(animeTopic = feedKey.normalizedAnimeTopic),
      sectionType = sectionType,
      page = page,
    ).map { mediaPage ->
      HomeSectionPage(
        type = sectionType,
        items = mediaPage.items,
        page = mediaPage.page,
        totalPages = mediaPage.totalPages,
      )
    }
  }

  private suspend fun loadMediaPage(
    feedKey: HomeFeedKey,
    sectionType: HomeSectionType,
    page: Int,
  ): DataResult<MediaPage> =
    when (sectionType) {
      HomeSectionType.HERO -> loadHero(feedKey, page)
      HomeSectionType.TRENDING -> loadTrending(feedKey, page)
      HomeSectionType.UPCOMING -> loadUpcoming(feedKey, page)
      HomeSectionType.TOP_RATED -> loadTopRated(feedKey, page)
      HomeSectionType.EDITORS_PICKS -> loadEditorsPicks(feedKey, page)
      HomeSectionType.HIDDEN_GEMS -> loadHiddenGems(feedKey, page)
      HomeSectionType.POPULAR_WORLDWIDE -> loadPopular(feedKey, page)
    }

  private suspend fun loadHero(
    feedKey: HomeFeedKey,
    page: Int,
  ): DataResult<MediaPage> {
    val locale = feedKey.toLocale()
    return if (feedKey.channel == HomeChannel.FOR_YOU) {
      supervisorScope {
        val movies = async {
          safeNetworkCall {
            catalogApi.getNowPlayingMovies(
              language = locale.language,
              region = null,
              page = page,
            )
          }.map { response ->
            MediaPage(
              items = response.results.map(MovieDto::toDomain),
              page = response.page,
              totalPages = response.totalPages,
            )
          }
        }
        val tv = async {
          safeNetworkCall {
            catalogApi.getOnTheAirTvShows(
              language = locale.language,
              page = page,
            )
          }.map { it.toTvPage() }
        }
        combineMediaPages(
          movies = movies.await(),
          tv = tv.await(),
          diversify = true,
          requireHeroVisuals = true,
        )
      }
    } else {
      loadDiscoverPair(
        feedKey = feedKey,
        page = page,
        movieSort = MovieSort.POPULARITY_DESC,
        tvSort = TvSort.POPULARITY_DESC,
        requireHeroVisuals = true,
      )
    }
  }

  private suspend fun loadTrending(
    feedKey: HomeFeedKey,
    page: Int,
  ): DataResult<MediaPage> {
    val locale = feedKey.toLocale()
    return if (feedKey.channel == HomeChannel.FOR_YOU) {
      supervisorScope {
        val movies = async {
          safeNetworkCall {
            catalogApi.getTrendingMovies(
              timeWindow = "week",
              language = locale.language,
              page = page,
            )
          }.map { it.toMoviePage() }
        }
        val tv = async {
          safeNetworkCall {
            catalogApi.getTrendingTvShows(
              timeWindow = "week",
              language = locale.language,
              page = page,
            )
          }.map { it.toTvPage() }
        }
        combineMediaPages(
          movies = movies.await(),
          tv = tv.await(),
          diversify = true,
        )
      }
    } else {
      loadDiscoverPair(
        feedKey = feedKey,
        page = page,
        movieSort = MovieSort.POPULARITY_DESC,
        tvSort = TvSort.POPULARITY_DESC,
        minimumVoteCount = MINIMUM_TRENDING_VOTE_COUNT,
      )
    }
  }

  private suspend fun loadUpcoming(
    feedKey: HomeFeedKey,
    page: Int,
  ): DataResult<MediaPage> {
    val locale = feedKey.toLocale()
    return if (feedKey.channel == HomeChannel.FOR_YOU) {
      safeNetworkCall {
        catalogApi.getUpcomingMovies(
          language = locale.language,
          region = null,
          page = page,
        )
      }.map { response ->
        MediaPage(
          items = response.results.map(MovieDto::toDomain).validItems(),
          page = response.page,
          totalPages = response.totalPages,
        )
      }
    } else {
      val today = LocalDate.now().toString()
      supervisorScope {
        val movies = async {
          discoverMovies(
            feedKey = feedKey,
            page = page,
            sort = MovieSort.PRIMARY_RELEASE_DATE_ASC,
            primaryReleaseDateFrom = today,
          )
        }
        val tv = async {
          discoverTv(
            feedKey = feedKey,
            page = page,
            sort = TvSort.FIRST_AIR_DATE_ASC,
            firstAirDateFrom = today,
          )
        }
        combineMediaPages(movies.await(), tv.await())
      }
    }
  }

  private suspend fun loadTopRated(
    feedKey: HomeFeedKey,
    page: Int,
  ): DataResult<MediaPage> {
    val locale = feedKey.toLocale()
    return if (feedKey.channel == HomeChannel.FOR_YOU) {
      supervisorScope {
        val movies = async {
          safeNetworkCall {
            catalogApi.getTopRatedMovies(
              language = locale.language,
              region = null,
              page = page,
            )
          }.map { it.toMoviePage() }
        }
        val tv = async {
          safeNetworkCall {
            catalogApi.getTopRatedTvShows(
              language = locale.language,
              page = page,
            )
          }.map { it.toTvPage() }
        }
        combineMediaPages(
          movies = movies.await(),
          tv = tv.await(),
          diversify = true,
          minimumVoteCount = MINIMUM_TOP_RATED_VOTE_COUNT,
        )
      }
    } else {
      loadDiscoverPair(
        feedKey = feedKey,
        page = page,
        movieSort = MovieSort.VOTE_AVERAGE_DESC,
        tvSort = TvSort.VOTE_AVERAGE_DESC,
        minimumVoteCount = MINIMUM_TOP_RATED_VOTE_COUNT,
      )
    }
  }

  private suspend fun loadEditorsPicks(
    feedKey: HomeFeedKey,
    page: Int,
  ): DataResult<MediaPage> =
    loadDiscoverPair(
      feedKey = feedKey,
      page = page,
      movieSort = MovieSort.VOTE_COUNT_DESC,
      tvSort = TvSort.VOTE_COUNT_DESC,
      minimumVoteCount = MINIMUM_EDITOR_VOTE_COUNT,
      minimumVoteAverage = MINIMUM_EDITOR_RATING,
    ).map { mediaPage ->
      mediaPage.copy(
        items = mediaPage.items
          .filter { !it.backdropPath.isNullOrBlank() }
          .sortedWith(
            compareByDescending<MediaItem>(MediaItem::voteAverage)
              .thenByDescending(MediaItem::voteCount),
          ),
      )
    }

  private suspend fun loadHiddenGems(
    feedKey: HomeFeedKey,
    page: Int,
  ): DataResult<MediaPage> =
    loadDiscoverPair(
      feedKey = feedKey,
      page = page,
      movieSort = MovieSort.VOTE_AVERAGE_DESC,
      tvSort = TvSort.VOTE_AVERAGE_DESC,
      minimumVoteCount = MINIMUM_HIDDEN_GEM_VOTE_COUNT,
      maximumVoteCount = MAXIMUM_HIDDEN_GEM_VOTE_COUNT,
      minimumVoteAverage = MINIMUM_HIDDEN_GEM_RATING,
    )

  private suspend fun loadPopular(
    feedKey: HomeFeedKey,
    page: Int,
  ): DataResult<MediaPage> {
    val locale = feedKey.toLocale()
    return if (feedKey.channel == HomeChannel.FOR_YOU) {
      supervisorScope {
        val movies = async {
          safeNetworkCall {
            catalogApi.getPopularMovies(
              language = locale.language,
              region = null,
              page = page,
            )
          }.map { it.toMoviePage() }
        }
        val tv = async {
          safeNetworkCall {
            catalogApi.getPopularTvShows(
              language = locale.language,
              page = page,
            )
          }.map { it.toTvPage() }
        }
        combineMediaPages(
          movies = movies.await(),
          tv = tv.await(),
          diversify = true,
        )
      }
    } else {
      loadDiscoverPair(
        feedKey = feedKey,
        page = page,
        movieSort = MovieSort.POPULARITY_DESC,
        tvSort = TvSort.POPULARITY_DESC,
      )
    }
  }

  private suspend fun loadDiscoverPair(
    feedKey: HomeFeedKey,
    page: Int,
    movieSort: MovieSort,
    tvSort: TvSort,
    minimumVoteCount: Int? = null,
    maximumVoteCount: Int? = null,
    minimumVoteAverage: Double? = null,
    requireHeroVisuals: Boolean = false,
  ): DataResult<MediaPage> =
    supervisorScope {
      val movies = async {
        discoverMovies(
          feedKey = feedKey,
          page = page,
          sort = movieSort,
          minimumVoteCount = minimumVoteCount,
          maximumVoteCount = maximumVoteCount,
          minimumVoteAverage = minimumVoteAverage,
        )
      }
      val tv = async {
        discoverTv(
          feedKey = feedKey,
          page = page,
          sort = tvSort,
          minimumVoteCount = minimumVoteCount,
          maximumVoteCount = maximumVoteCount,
          minimumVoteAverage = minimumVoteAverage,
        )
      }
      combineMediaPages(
        movies = movies.await(),
        tv = tv.await(),
        requireHeroVisuals = requireHeroVisuals,
      )
    }

  private suspend fun discoverMovies(
    feedKey: HomeFeedKey,
    page: Int,
    sort: MovieSort,
    minimumVoteCount: Int? = null,
    maximumVoteCount: Int? = null,
    minimumVoteAverage: Double? = null,
    primaryReleaseDateFrom: String? = null,
  ): DataResult<MediaPage> {
    val channel = feedKey.channel
    val animeGenres = if (channel.isAnime) {
      buildSet {
        add(ANIMATION_GENRE_ID)
        feedKey.normalizedAnimeTopic.movieGenreId?.let(::add)
      }
    } else {
      emptySet()
    }
    val query = MovieDiscoverQuery(
      locale = feedKey.toLocale(),
      page = page,
      sortBy = sort,
      primaryReleaseDateFrom = primaryReleaseDateFrom,
      withGenres = animeGenres
        .takeIf(Set<Int>::isNotEmpty)
        ?.let { IdFilter(it, FilterCombination.ALL) },
      withOriginCountry = channel.countryCode,
      withOriginalLanguage = channel.originalLanguage,
      minimumVoteAverage = minimumVoteAverage,
      minimumVoteCount = minimumVoteCount,
      maximumVoteCount = maximumVoteCount,
    )
    return safeNetworkCall {
      catalogApi.discoverMovies(query.toQueryMap())
    }.map { it.toMoviePage() }
  }

  private suspend fun discoverTv(
    feedKey: HomeFeedKey,
    page: Int,
    sort: TvSort,
    minimumVoteCount: Int? = null,
    maximumVoteCount: Int? = null,
    minimumVoteAverage: Double? = null,
    firstAirDateFrom: String? = null,
  ): DataResult<MediaPage> {
    val channel = feedKey.channel
    val animeGenres = if (channel.isAnime) {
      buildSet {
        add(ANIMATION_GENRE_ID)
        feedKey.normalizedAnimeTopic.tvGenreId?.let(::add)
      }
    } else {
      emptySet()
    }
    val query = TvDiscoverQuery(
      locale = feedKey.toLocale(),
      page = page,
      sortBy = sort,
      firstAirDateFrom = firstAirDateFrom,
      withGenres = animeGenres
        .takeIf(Set<Int>::isNotEmpty)
        ?.let { IdFilter(it, FilterCombination.ALL) },
      withOriginCountry = channel.countryCode,
      withOriginalLanguage = channel.originalLanguage,
      minimumVoteAverage = minimumVoteAverage,
      minimumVoteCount = minimumVoteCount,
      maximumVoteCount = maximumVoteCount,
    )
    return safeNetworkCall {
      catalogApi.discoverTvShows(query.toQueryMap())
    }.map { it.toTvPage() }
  }

  private suspend fun loadGenreNames(
    language: String,
  ): DataResult<Map<Int, String>> =
    supervisorScope {
      val movieGenres = async {
        safeNetworkCall { catalogApi.getMovieGenres(language) }
      }
      val tvGenres = async {
        safeNetworkCall { catalogApi.getTvGenres(language) }
      }
      val movieResult = movieGenres.await()
      val tvResult = tvGenres.await()
      val responses = listOfNotNull(
        movieResult.getOrNull(),
        tvResult.getOrNull(),
      )
      if (responses.isNotEmpty()) {
        DataResult.Success(
          responses
            .flatMap { it.genres }
            .filter { it.id > 0 && it.name.isNotBlank() }
            .associate { it.id to it.name },
        )
      } else {
        firstError(movieResult, tvResult) ?: DataResult.Success(emptyMap())
      }
    }

  private suspend fun loadPopularPeople(
    featuredItems: List<MediaItem>,
    language: String,
  ): DataResult<List<HomePerson>> {
    if (featuredItems.isEmpty()) return DataResult.Success(emptyList())

    return supervisorScope {
      val results = featuredItems
        .take(PEOPLE_CREDIT_REQUEST_LIMIT)
        .toList()
        .mapInBatches(MAX_PARALLEL_ENRICHMENT_REQUESTS) { media ->
          loadCredits(media, language).map { cast ->
            cast
              .asSequence()
              .filter { member ->
                member.id > 0 &&
                  member.name.isNotBlank() &&
                  !member.profilePath.isNullOrBlank()
              }
              .take(CAST_LIMIT_PER_TITLE)
              .map { member ->
                PersonAppearance(
                  member = member,
                  title = media.title,
                )
              }
              .toList()
          }
        }

      val appearances = results
        .filterIsInstance<DataResult.Success<List<PersonAppearance>>>()
        .flatMap { it.value }

      if (appearances.isNotEmpty()) {
        DataResult.Success(
          appearances
            .groupBy { it.member.id }
            .values
            .sortedWith(
              compareByDescending<List<PersonAppearance>> { it.size }
                .thenBy { group -> group.minOf { it.member.order } },
            )
            .take(POPULAR_PEOPLE_LIMIT)
            .map { group ->
              val person = group.first().member
              HomePerson(
                id = person.id,
                name = person.name,
                profilePath = person.profilePath,
                knownFor = group.map(PersonAppearance::title).distinct().take(2),
              )
            },
        )
      } else {
        results.filterIsInstance<DataResult.Error>().firstOrNull()
          ?: DataResult.Success(emptyList())
      }
    }
  }

  private suspend fun loadCredits(
    media: MediaItem,
    language: String,
  ): DataResult<List<CastMember>> =
    when (media.type) {
      MediaType.MOVIE ->
        safeNetworkCall {
          movieApi.getMovieCredits(
            movieId = media.id,
            language = language,
          )
        }.map { it.toDomain().cast }

      MediaType.TV_SHOW ->
        safeNetworkCall {
          tvSeriesApi.getTvCredits(
            seriesId = media.id,
            language = language,
          )
        }.map { it.toDomain().cast }
    }

  private suspend fun loadTrailers(
    mediaItems: List<MediaItem>,
    language: String,
  ): DataResult<List<HomeTrailer>> {
    if (mediaItems.isEmpty()) return DataResult.Success(emptyList())

    return supervisorScope {
      val results = mediaItems
        .take(TRAILER_REQUEST_LIMIT)
        .toList()
        .mapInBatches(MAX_PARALLEL_ENRICHMENT_REQUESTS) { media ->
          val response = when (media.type) {
            MediaType.MOVIE -> safeNetworkCall {
              movieApi.getMovieVideos(
                movieId = media.id,
                language = language,
              )
            }
            MediaType.TV_SHOW -> safeNetworkCall {
              tvSeriesApi.getTvVideos(
                seriesId = media.id,
                language = language,
              )
            }
          }
          response.map { videos ->
            videos.results
              .asSequence()
              .filter { video ->
                VIDEO_KEY_PATTERN.matches(video.key) &&
                  video.site.equals("YouTube", ignoreCase = true)
              }
              .sortedWith(
                compareByDescending<com.example.filmera.core.network.dto.VideoDto> {
                  it.official
                }.thenByDescending { video ->
                  when {
                    video.type.equals("Trailer", ignoreCase = true) -> 2
                    video.type.equals("Teaser", ignoreCase = true) -> 1
                    else -> 0
                  }
                }.thenByDescending { it.size },
              )
              .firstOrNull()
              ?.let { video ->
                HomeTrailer(
                  media = media,
                  videoKey = video.key,
                  name = video.name.ifBlank { media.title },
                  type = video.type.ifBlank { "Trailer" },
                )
              }
          }
        }

      val trailers = results
        .filterIsInstance<DataResult.Success<HomeTrailer?>>()
        .mapNotNull { it.value }
        .distinctBy(HomeTrailer::videoKey)
      if (trailers.isNotEmpty()) {
        DataResult.Success(trailers)
      } else {
        results.filterIsInstance<DataResult.Error>().firstOrNull()
          ?: DataResult.Success(emptyList())
      }
    }
  }

  private suspend fun loadCollections(
    mediaItems: List<MediaItem>,
    language: String,
  ): DataResult<List<HomeCollection>> =
    supervisorScope {
      if (mediaItems.isEmpty()) return@supervisorScope DataResult.Success(emptyList())

      val movieItems = mediaItems
        .asSequence()
        .filter { it.type == MediaType.MOVIE }
        .distinctBy(MediaItem::key)
        .take(COLLECTION_REQUEST_LIMIT)
        .toList()
      val results = movieItems.mapInBatches(MAX_PARALLEL_ENRICHMENT_REQUESTS) { media ->
        safeNetworkCall {
          movieApi.getMovieDetails(
            movieId = media.id,
            language = language,
          )
        }.map { details ->
          details.belongsToCollection?.let { collection ->
            HomeCollection(
              id = collection.id,
              name = collection.name,
              posterPath = collection.posterPath,
              backdropPath = collection.backdropPath,
              overview = "",
              itemCount = 0,
              kind = HomeCollectionKind.OFFICIAL_COLLECTION,
              featuredMedia = media,
            )
          }
        }
      }
      val discoveredCollections = results
        .filterIsInstance<DataResult.Success<HomeCollection?>>()
        .mapNotNull { it.value }
        .distinctBy(HomeCollection::id)
      val usedIds = discoveredCollections.map(HomeCollection::id).toSet()
      val searchedCollections = CURATED_COLLECTION_QUERIES
        .mapIndexed { index, query -> index to query }
        .mapInBatches(MAX_PARALLEL_ENRICHMENT_REQUESTS) { (index, query) ->
          safeNetworkCall {
            searchApi.searchCollections(
              query = query,
              language = language,
              page = FIRST_PAGE,
              includeAdult = false,
            )
          }.map { response ->
            response.results
              .asSequence()
              .filter { collection -> collection.isUsableCollection() }
              .firstOrNull { it.id !in usedIds }
              ?.toHomeCollection(
                featuredMedia = mediaItems[index % mediaItems.size],
              )
          }
        }
        .filterIsInstance<DataResult.Success<HomeCollection?>>()
        .mapNotNull { it.value }

      val officialCollections = (discoveredCollections + searchedCollections)
        .distinctBy(HomeCollection::id)
      val curatedUniverses = buildCuratedUniverses(
        mediaItems = mediaItems,
        count = (MINIMUM_HOME_COLLECTIONS - officialCollections.size).coerceAtLeast(1),
      )
      val collections = (officialCollections + curatedUniverses)
        .distinctBy { collection -> collection.kind to collection.id }
        .take(MAXIMUM_HOME_COLLECTIONS)

      if (collections.size >= MINIMUM_HOME_COLLECTIONS || collections.isNotEmpty()) {
        DataResult.Success(collections)
      } else {
        results.filterIsInstance<DataResult.Error>().firstOrNull()
          ?: DataResult.Success(emptyList())
      }
    }

  private fun buildGenreHighlights(
    items: List<MediaItem>,
    genreNames: Map<Int, String>,
    limit: Int,
  ): List<HomeGenre> {
    val itemsByGenre = items
      .asSequence()
      .flatMap { item -> item.genreIds.distinct().asSequence().map { it to item } }
      .groupBy(
        keySelector = { (genreId, _) -> genreId },
        valueTransform = { (_, item) -> item },
      )
    val preferredGenreIds = listOf(
      DRAMA_GENRE_ID,
      ANIMATION_GENRE_ID,
      ACTION_GENRE_ID,
      ADVENTURE_GENRE_ID,
      THRILLER_GENRE_ID,
      ROMANCE_GENRE_ID,
    )
    val orderedGenreIds = (
      preferredGenreIds +
        itemsByGenre.entries
          .sortedByDescending { (_, genreItems) -> genreItems.size }
          .map(Map.Entry<Int, List<MediaItem>>::key)
      ).distinct()
    val usedBackdrops = mutableSetOf<String>()

    return orderedGenreIds.mapNotNull { genreId ->
      val genreItems = itemsByGenre[genreId].orEmpty()
      val name = genreNames[genreId] ?: return@mapNotNull null
      val backdropPath = genreItems
        .asSequence()
        .mapNotNull(MediaItem::backdropPath)
        .firstOrNull(usedBackdrops::add)
        ?: return@mapNotNull null
      HomeGenre(
        id = genreId,
        name = name,
        titleCount = genreItems.distinctBy(MediaItem::key).size,
        backdropPath = backdropPath,
      )
    }.take(limit)
  }

  private fun buildSpotlights(
    items: List<MediaItem>,
  ): List<HomeSpotlight> =
    items
      .asSequence()
      .filter { item ->
        !item.backdropPath.isNullOrBlank() && item.overview.isNotBlank()
      }
      .distinctBy(MediaItem::key)
      .take(SPOTLIGHT_LIMIT)
      .mapIndexed { index, item ->
        HomeSpotlight(
          id = "${item.type.routeValue}-${item.id}",
          media = item,
          category = SPOTLIGHT_CATEGORIES[index % SPOTLIGHT_CATEGORIES.size],
          title = item.title,
          summary = item.overview,
          readMinutes = SPOTLIGHT_READ_MINUTES[index % SPOTLIGHT_READ_MINUTES.size],
        )
      }
      .toList()

  private fun CollectionDto.isUsableCollection(): Boolean =
    id > 0 &&
      name.isNotBlank() &&
      !adult &&
      (!backdropPath.isNullOrBlank() || !posterPath.isNullOrBlank())

  private fun CollectionDto.toHomeCollection(
    featuredMedia: MediaItem,
  ): HomeCollection =
    HomeCollection(
      id = id,
      name = name,
      posterPath = posterPath,
      backdropPath = backdropPath,
      overview = overview.orEmpty(),
      itemCount = 0,
      kind = HomeCollectionKind.OFFICIAL_COLLECTION,
      featuredMedia = featuredMedia,
    )

  private fun buildCuratedUniverses(
    mediaItems: List<MediaItem>,
    count: Int,
  ): List<HomeCollection> {
    if (mediaItems.isEmpty()) return emptyList()

    val usedMedia = mutableSetOf<com.example.filmera.core.model.MediaKey>()
    return CURATED_UNIVERSES
      .asSequence()
      .mapIndexed { index, universe ->
        val matchingItems = mediaItems.filter { universe.genreId in it.genreIds }
        val featured = matchingItems
          .firstOrNull { item ->
            !item.backdropPath.isNullOrBlank() && usedMedia.add(item.key)
          }
          ?: mediaItems.firstOrNull { item ->
            !item.backdropPath.isNullOrBlank() && usedMedia.add(item.key)
          }
          ?: mediaItems[index % mediaItems.size]
        HomeCollection(
          id = -(index + 1),
          name = universe.name,
          posterPath = featured.posterPath,
          backdropPath = featured.backdropPath,
          overview = universe.overview,
          itemCount = matchingItems.distinctBy(MediaItem::key).size.coerceAtLeast(1),
          kind = HomeCollectionKind.CINEMATIC_UNIVERSE,
          featuredMedia = featured,
        )
      }
      .take(count.coerceAtMost(CURATED_UNIVERSES.size))
      .toList()
  }

  private fun combineMediaPages(
    movies: DataResult<MediaPage>,
    tv: DataResult<MediaPage>,
    diversify: Boolean = false,
    requireHeroVisuals: Boolean = false,
    minimumVoteCount: Int? = null,
  ): DataResult<MediaPage> {
    val moviePage = movies.getOrNull()
    val tvPage = tv.getOrNull()
    if (moviePage == null && tvPage == null) {
      return firstError(movies, tv) ?: DataResult.Success(MediaPage())
    }

    val combined = interleave(
      moviePage?.items.orEmpty(),
      tvPage?.items.orEmpty(),
    ).filter { item ->
      minimumVoteCount == null || item.voteCount >= minimumVoteCount
    }
    val items = when {
      requireHeroVisuals -> selectHeroMedia(combined, limit = HOME_SECTION_ITEM_LIMIT)
      diversify -> selectDiverseMedia(combined, limit = HOME_SECTION_ITEM_LIMIT)
      else -> combined.validItems()
    }
    return DataResult.Success(
      MediaPage(
        items = items,
        page = maxOf(moviePage?.page ?: 1, tvPage?.page ?: 1),
        totalPages = maxOf(moviePage?.totalPages ?: 1, tvPage?.totalPages ?: 1),
      ),
    )
  }

  private fun resultSection(
    type: HomeSectionType,
    result: DataResult<MediaPage>,
  ): HomeSection =
    when (result) {
      is DataResult.Success -> HomeSection(
        type = type,
        items = if (type == HomeSectionType.TRENDING) {
          result.value.items.take(HOME_TRENDING_LIMIT)
        } else {
          result.value.items
        },
        page = result.value.page,
        totalPages = if (type == HomeSectionType.TRENDING) {
          result.value.page
        } else {
          result.value.totalPages.coerceAtLeast(1)
        },
      )
      is DataResult.Error -> HomeSection(
        type = type,
        error = result.error,
      )
    }

  private fun CatalogResponseDto<MovieDto>.toMoviePage(): MediaPage =
    MediaPage(
      items = results.map(MovieDto::toDomain).validItems(),
      page = page,
      totalPages = totalPages,
    )

  private fun CatalogResponseDto<TvShowDto>.toTvPage(): MediaPage =
    MediaPage(
      items = results.map(TvShowDto::toDomain).validItems(),
      page = page,
      totalPages = totalPages,
    )

  private fun List<MediaItem>.validItems(): List<MediaItem> =
    asSequence()
      .filter { item ->
        item.id > 0 &&
          item.title.isNotBlank() &&
          !item.adult
      }
      .distinctBy(MediaItem::key)
      .take(HOME_SECTION_ITEM_LIMIT)
      .toList()

  private fun interleave(
    first: List<MediaItem>,
    second: List<MediaItem>,
  ): List<MediaItem> = buildList {
    repeat(maxOf(first.size, second.size)) { index ->
      first.getOrNull(index)?.let(::add)
      second.getOrNull(index)?.let(::add)
    }
  }

  private fun firstError(
    vararg results: DataResult<*>,
  ): DataResult.Error? =
    results.filterIsInstance<DataResult.Error>().firstOrNull()

  private fun HomeFeedKey.toLocale(): TmdbLocale {
    val region = channel.countryCode ?: TmdbApiDefaults.REGION
    return TmdbLocale(
      language = TmdbApiDefaults.LANGUAGE,
      region = region,
      watchRegion = region,
    )
  }

  /** Keeps nested enrichment calls bounded even when a section has many candidates. */
  private suspend fun <T, R> List<T>.mapInBatches(
    batchSize: Int,
    transform: suspend (T) -> R,
  ): List<R> = buildList {
    this@mapInBatches.chunked(batchSize).forEach { batch ->
      addAll(
        supervisorScope {
          batch.map { item -> async { transform(item) } }.awaitAll()
        },
      )
    }
  }

  private data class MediaPage(
    val items: List<MediaItem> = emptyList(),
    val page: Int = FIRST_PAGE,
    val totalPages: Int = FIRST_PAGE,
  )

  private data class PersonAppearance(
    val member: CastMember,
    val title: String,
  )

  private data class CuratedUniverse(
    val name: String,
    val overview: String,
    val genreId: Int,
  )

  private companion object {
    val PAGED_SECTION_TYPES = listOf(
      HomeSectionType.HERO,
      HomeSectionType.TRENDING,
      HomeSectionType.EDITORS_PICKS,
      HomeSectionType.UPCOMING,
      HomeSectionType.TOP_RATED,
      HomeSectionType.HIDDEN_GEMS,
      HomeSectionType.POPULAR_WORLDWIDE,
    )
    val SECTION_LOAD_PHASES = listOf(
      listOf(HomeSectionType.HERO, HomeSectionType.TRENDING),
      listOf(HomeSectionType.EDITORS_PICKS, HomeSectionType.UPCOMING),
      listOf(
        HomeSectionType.TOP_RATED,
        HomeSectionType.HIDDEN_GEMS,
        HomeSectionType.POPULAR_WORLDWIDE,
      ),
    )
    const val FIRST_PAGE = 1
    const val HOME_SECTION_ITEM_LIMIT = 20
    const val HOME_TRENDING_LIMIT = 15
    const val MINIMUM_TRENDING_VOTE_COUNT = 50
    const val MINIMUM_TOP_RATED_VOTE_COUNT = 200
    const val MINIMUM_EDITOR_VOTE_COUNT = 500
    const val MINIMUM_EDITOR_RATING = 7.0
    const val MINIMUM_HIDDEN_GEM_VOTE_COUNT = 150
    const val MAXIMUM_HIDDEN_GEM_VOTE_COUNT = 5_000
    const val MINIMUM_HIDDEN_GEM_RATING = 7.0
    const val GENRE_HIGHLIGHT_LIMIT = 6
    const val PEOPLE_CREDIT_REQUEST_LIMIT = 4
    const val CAST_LIMIT_PER_TITLE = 8
    const val POPULAR_PEOPLE_LIMIT = 16
    const val TRAILER_REQUEST_LIMIT = 6
    const val COLLECTION_REQUEST_LIMIT = 6
    const val MAX_PARALLEL_ENRICHMENT_REQUESTS = 3
    const val MINIMUM_HOME_COLLECTIONS = 5
    const val MAXIMUM_HOME_COLLECTIONS = 10
    const val SPOTLIGHT_LIMIT = 3
    const val ANIMATION_GENRE_ID = 16
    const val DRAMA_GENRE_ID = 18
    const val ACTION_GENRE_ID = 28
    const val ADVENTURE_GENRE_ID = 12
    const val THRILLER_GENRE_ID = 53
    const val ROMANCE_GENRE_ID = 10749
    val SPOTLIGHT_CATEGORIES = listOf(
      "FILMERA SPOTLIGHT",
      "BEHIND THE SCREEN",
      "CINEMA STORIES",
    )
    val SPOTLIGHT_READ_MINUTES = listOf(5, 7, 6)
    val VIDEO_KEY_PATTERN = Regex("[A-Za-z0-9_-]{1,128}")
    val CURATED_COLLECTION_QUERIES = listOf(
      "Harry Potter",
      "Star Wars",
      "Mission Impossible",
      "Fast and Furious",
      "The Lord of the Rings",
      "John Wick",
    )
    val CURATED_UNIVERSES = listOf(
      CuratedUniverse(
        name = "Global Animation Worlds",
        overview = "Discover animated stories and connected worlds from across the globe.",
        genreId = ANIMATION_GENRE_ID,
      ),
      CuratedUniverse(
        name = "Science Fiction Universes",
        overview = "Explore ambitious futures, distant worlds, and stories beyond our own.",
        genreId = 878,
      ),
      CuratedUniverse(
        name = "Epic Fantasy Worlds",
        overview = "Enter sweeping realms filled with myth, adventure, and unforgettable heroes.",
        genreId = 14,
      ),
      CuratedUniverse(
        name = "Action Sagas",
        overview = "Follow high-energy stories, returning heroes, and escalating missions.",
        genreId = ACTION_GENRE_ID,
      ),
      CuratedUniverse(
        name = "Modern Crime Stories",
        overview = "Trace connected tales of mystery, ambition, justice, and consequence.",
        genreId = 80,
      ),
    )
  }
}
