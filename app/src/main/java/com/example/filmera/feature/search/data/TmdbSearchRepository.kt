package com.example.filmera.feature.search.data

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.common.getOrNull
import com.example.filmera.core.common.map
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.network.TmdbCatalogApi
import com.example.filmera.core.network.TmdbConfig
import com.example.filmera.core.network.TmdbPersonApi
import com.example.filmera.core.network.TmdbSearchApi
import com.example.filmera.core.network.dto.CollectionDto
import com.example.filmera.core.network.dto.PersonDto
import com.example.filmera.core.network.mapper.toDomain
import com.example.filmera.core.network.safeNetworkCall
import com.example.filmera.feature.search.domain.SearchCollection
import com.example.filmera.feature.search.domain.SearchContent
import com.example.filmera.feature.search.domain.SearchDiscovery
import com.example.filmera.feature.search.domain.SearchGenre
import com.example.filmera.feature.search.domain.SearchPerson
import com.example.filmera.feature.search.domain.SearchRepository
import com.example.filmera.feature.search.domain.SearchRequest
import com.example.filmera.feature.search.domain.SearchTopResult
import com.example.filmera.feature.search.domain.SearchType
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

class TmdbSearchRepository @Inject constructor(
  private val searchApi: TmdbSearchApi,
  private val catalogApi: TmdbCatalogApi,
  private val personApi: TmdbPersonApi,
  private val config: TmdbConfig,
) : SearchRepository {
  override suspend fun loadDiscovery(): DataResult<SearchDiscovery> {
    if (!config.isConfigured) return DataResult.Error(AppError.MissingApiToken)

    return supervisorScope {
      val moviesRequest = async {
        safeNetworkCall { catalogApi.getTrendingMovies() }
          .map { response -> response.results.map { it.toDomain() }.validMedia() }
      }
      val tvShowsRequest = async {
        safeNetworkCall { catalogApi.getTrendingTvShows() }
          .map { response -> response.results.map { it.toDomain() }.validMedia() }
      }
      val movieGenresRequest = async {
        safeNetworkCall { catalogApi.getMovieGenres() }
          .map { response -> response.genres.associate { it.id to it.name } }
      }
      val tvGenresRequest = async {
        safeNetworkCall { catalogApi.getTvGenres() }
          .map { response -> response.genres.associate { it.id to it.name } }
      }
      val peopleRequest = async {
        safeNetworkCall { personApi.getPopularPeople() }
          .map { response -> response.results.mapNotNull { it.toSearchPersonOrNull() } }
      }

      val movies = moviesRequest.await()
      val tvShows = tvShowsRequest.await()
      val movieGenres = movieGenresRequest.await()
      val tvGenres = tvGenresRequest.await()
      val people = peopleRequest.await()
      val dependencies = listOf(movies, tvShows, movieGenres, tvGenres, people)
      val trendingMedia = (movies.getOrNull().orEmpty() + tvShows.getOrNull().orEmpty())
        .distinctBy(MediaItem::key)
        .sortedByDescending(MediaItem::popularity)
      val genreNames = buildMap {
        putAll(movieGenres.getOrNull().orEmpty())
        putAll(tvGenres.getOrNull().orEmpty())
      }
      val discovery = SearchDiscovery(
        trendingQueries = trendingMedia
          .map(MediaItem::title)
          .filter(String::isNotBlank)
          .distinct()
          .take(TRENDING_QUERY_LIMIT),
        genres = buildGenreHighlights(trendingMedia, genreNames),
        popularPeople = people.getOrNull().orEmpty().take(POPULAR_PEOPLE_LIMIT),
        hasPartialFailures = dependencies.any { it is DataResult.Error },
      )

      if (discovery.hasVisibleContent || dependencies.any { it is DataResult.Success }) {
        DataResult.Success(discovery)
      } else {
        dependencies.filterIsInstance<DataResult.Error>().firstOrNull()
          ?: DataResult.Error(AppError.Unknown)
      }
    }
  }

  override suspend fun search(request: SearchRequest): DataResult<SearchContent> {
    if (!config.isConfigured) return DataResult.Error(AppError.MissingApiToken)

    val query = request.query.trim()
    if (query.isEmpty()) {
      return DataResult.Success(
        SearchContent(query = "", selectedType = request.type),
      )
    }

    return when (request.type) {
      SearchType.ALL -> searchAll(query)
      SearchType.MOVIES -> searchMovies(query, request.page)
      SearchType.TV_SHOWS -> searchTvShows(query, request.page)
      SearchType.PEOPLE -> searchPeople(query, request.page)
      SearchType.COLLECTIONS -> searchCollections(query, request.page)
    }
  }

  private suspend fun searchAll(query: String): DataResult<SearchContent> =
    supervisorScope {
      val moviesRequest = async {
        safeNetworkCall { searchApi.searchMovies(query = query) }
          .map { response -> response.results.map { it.toDomain() }.validMedia() }
      }
      val tvShowsRequest = async {
        safeNetworkCall { searchApi.searchTvShows(query = query) }
          .map { response -> response.results.map { it.toDomain() }.validMedia() }
      }
      val peopleRequest = async {
        safeNetworkCall { searchApi.searchPeople(query = query) }
          .map { response -> response.results.mapNotNull { it.toSearchPersonOrNull() } }
      }
      val collectionsRequest = async {
        safeNetworkCall { searchApi.searchCollections(query = query) }
          .map { response ->
            response.results.mapNotNull { it.toSearchCollectionOrNull() }
          }
      }

      val movies = moviesRequest.await()
      val tvShows = tvShowsRequest.await()
      val people = peopleRequest.await()
      val collections = collectionsRequest.await()
      val dependencies = listOf(movies, tvShows, people, collections)

      if (dependencies.none { it is DataResult.Success }) {
        dependencies.filterIsInstance<DataResult.Error>().firstOrNull()
          ?: DataResult.Error(AppError.Unknown)
      } else {
        val movieItems = movies.getOrNull().orEmpty()
        val tvItems = tvShows.getOrNull().orEmpty()
        val personItems = people.getOrNull().orEmpty()
        val collectionItems = collections.getOrNull().orEmpty()
        DataResult.Success(
          SearchContent(
            query = query,
            selectedType = SearchType.ALL,
            movies = movieItems,
            tvShows = tvItems,
            people = personItems,
            collections = collectionItems,
            topResult = selectTopResult(
              query = query,
              movies = movieItems,
              tvShows = tvItems,
              people = personItems,
              collections = collectionItems,
            ),
            hasPartialFailures = dependencies.any { it is DataResult.Error },
          ),
        )
      }
    }

  private suspend fun searchMovies(query: String, page: Int): DataResult<SearchContent> =
    safeNetworkCall { searchApi.searchMovies(query = query, page = page) }
      .map { response ->
        val movies = response.results.map { it.toDomain() }.validMedia()
        SearchContent(
          query = query,
          selectedType = SearchType.MOVIES,
          movies = movies,
          topResult = selectTopResult(query, movies = movies),
          page = response.page.coerceAtLeast(page),
          totalPages = response.totalPages,
        )
      }

  private suspend fun searchTvShows(query: String, page: Int): DataResult<SearchContent> =
    safeNetworkCall { searchApi.searchTvShows(query = query, page = page) }
      .map { response ->
        val tvShows = response.results.map { it.toDomain() }.validMedia()
        SearchContent(
          query = query,
          selectedType = SearchType.TV_SHOWS,
          tvShows = tvShows,
          topResult = selectTopResult(query, tvShows = tvShows),
          page = response.page.coerceAtLeast(page),
          totalPages = response.totalPages,
        )
      }

  private suspend fun searchPeople(query: String, page: Int): DataResult<SearchContent> =
    safeNetworkCall { searchApi.searchPeople(query = query, page = page) }
      .map { response ->
        val people = response.results.mapNotNull { it.toSearchPersonOrNull() }
        SearchContent(
          query = query,
          selectedType = SearchType.PEOPLE,
          people = people,
          topResult = selectTopResult(query, people = people),
          page = response.page.coerceAtLeast(page),
          totalPages = response.totalPages,
        )
      }

  private suspend fun searchCollections(query: String, page: Int): DataResult<SearchContent> =
    safeNetworkCall { searchApi.searchCollections(query = query, page = page) }
      .map { response ->
        val collections = response.results.mapNotNull { it.toSearchCollectionOrNull() }
        SearchContent(
          query = query,
          selectedType = SearchType.COLLECTIONS,
          collections = collections,
          topResult = selectTopResult(query, collections = collections),
          page = response.page.coerceAtLeast(page),
          totalPages = response.totalPages,
        )
      }

  private fun buildGenreHighlights(
    items: List<MediaItem>,
    genreNames: Map<Int, String>,
  ): List<SearchGenre> =
    items
      .asSequence()
      .flatMap { item -> item.genreIds.distinct().asSequence().map { it to item } }
      .groupBy(
        keySelector = { (genreId, _) -> genreId },
        valueTransform = { (_, item) -> item },
      )
      .mapNotNull { (genreId, genreItems) ->
        val name = genreNames[genreId]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        SearchGenre(
          id = genreId,
          name = name,
          titleCount = genreItems.distinctBy(MediaItem::key).size,
          backdropPath = genreItems.firstNotNullOfOrNull(MediaItem::backdropPath),
        )
      }
      .sortedByDescending(SearchGenre::titleCount)
      .take(GENRE_LIMIT)

  private fun selectTopResult(
    query: String,
    movies: List<MediaItem> = emptyList(),
    tvShows: List<MediaItem> = emptyList(),
    people: List<SearchPerson> = emptyList(),
    collections: List<SearchCollection> = emptyList(),
  ): SearchTopResult? {
    val candidates = buildList {
      movies.forEach { item ->
        add(
          RankedResult(
            result = SearchTopResult.Media(item),
            matchRank = item.title.matchRank(query),
            voteCount = item.voteCount,
            popularity = item.popularity,
          ),
        )
      }
      tvShows.forEach { item ->
        add(
          RankedResult(
            result = SearchTopResult.Media(item),
            matchRank = item.title.matchRank(query),
            voteCount = item.voteCount,
            popularity = item.popularity,
          ),
        )
      }
      people.forEach { person ->
        add(
          RankedResult(
            result = SearchTopResult.Person(person),
            matchRank = person.name.matchRank(query),
            popularity = person.popularity,
          ),
        )
      }
      collections.forEach { collection ->
        add(
          RankedResult(
            result = SearchTopResult.Collection(collection),
            matchRank = collection.name.matchRank(query),
            popularity = collection.popularity,
          ),
        )
      }
    }

    return candidates.maxWithOrNull(
      compareBy<RankedResult> { it.matchRank }
        .thenBy { it.voteCount }
        .thenBy { it.popularity },
    )?.result
  }

  private fun List<MediaItem>.validMedia(): List<MediaItem> =
    asSequence()
      .filter { it.id > 0 && it.title.isNotBlank() && !it.adult }
      .distinctBy(MediaItem::key)
      .toList()

  private fun PersonDto.toSearchPersonOrNull(): SearchPerson? {
    if (id <= 0 || name.isBlank() || adult) return null
    return SearchPerson(
      id = id,
      name = name,
      knownForDepartment = knownForDepartment.orEmpty(),
      profilePath = profilePath,
      knownFor = knownFor
        .mapNotNull { item -> (item.title ?: item.name)?.takeIf(String::isNotBlank) }
        .distinct()
        .take(3),
      popularity = popularity,
    )
  }

  private fun CollectionDto.toSearchCollectionOrNull(): SearchCollection? {
    if (id <= 0 || name.isBlank() || adult) return null
    return SearchCollection(
      id = id,
      name = name,
      overview = overview.orEmpty(),
      posterPath = posterPath,
      backdropPath = backdropPath,
      popularity = popularity,
    )
  }

  private fun String.matchRank(query: String): Int {
    val normalizedTitle = trim().lowercase()
    val normalizedQuery = query.trim().lowercase()
    return when {
      normalizedTitle == normalizedQuery -> 3
      normalizedTitle.startsWith(normalizedQuery) -> 2
      normalizedTitle.contains(normalizedQuery) -> 1
      else -> 0
    }
  }

  private data class RankedResult(
    val result: SearchTopResult,
    val matchRank: Int,
    val voteCount: Int = 0,
    val popularity: Double,
  )

  private companion object {
    const val TRENDING_QUERY_LIMIT = 8
    const val GENRE_LIMIT = 8
    const val POPULAR_PEOPLE_LIMIT = 10
  }
}
