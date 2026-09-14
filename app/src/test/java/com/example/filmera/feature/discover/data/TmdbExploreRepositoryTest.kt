package com.example.filmera.feature.discover.data

import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.network.TmdbCatalogApi
import com.example.filmera.core.network.TmdbConfig
import com.example.filmera.core.network.dto.CatalogResponseDto
import com.example.filmera.core.network.dto.MovieDto
import com.example.filmera.core.network.dto.TvShowDto
import com.example.filmera.core.network.dto.WatchProviderDto
import com.example.filmera.core.network.dto.WatchProviderListResponseDto
import com.example.filmera.feature.discover.domain.ExploreCategory
import com.example.filmera.feature.discover.domain.ExploreContentType
import com.example.filmera.feature.discover.domain.ExploreFilterState
import com.example.filmera.feature.discover.domain.ExploreRegion
import com.example.filmera.feature.discover.domain.ExploreSort
import com.example.filmera.feature.discover.domain.ExploreYear
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbExploreRepositoryTest {

  @Test
  fun `all content merges movie and tv after separate requests`() = runBlocking {
    val calls = mutableListOf<String>()
    val repository = TmdbExploreRepository(
      catalogApi = catalogApi { method, _ ->
        calls += method
        when (method) {
          "discoverMovies" -> CatalogResponseDto(
            page = 1,
            totalPages = 4,
            totalResults = 80,
            results = (1..20).map(::movie),
          )
          "discoverTvShows" -> CatalogResponseDto(
            page = 1,
            totalPages = 5,
            totalResults = 100,
            results = (1..20).map(::series),
          )
          "getTrendingMovies" -> CatalogResponseDto(
            results = (1..20).map(::movie),
          )
          "getTrendingTvShows" -> CatalogResponseDto(
            results = (1..20).map(::series),
          )
          else -> error("Unexpected call: $method")
        }
      },
      config = TmdbConfig(bearerToken = "test-token"),
    )

    val result = repository.loadExplore(ExploreFilterState(), page = 1)

    assertTrue(result is DataResult.Success)
    val page = (result as DataResult.Success).value
    assertEquals(20, page.items.size)
    assertEquals(180, page.totalResults)
    assertTrue(page.items.any { it.media.type == MediaType.MOVIE })
    assertTrue(page.items.any { it.media.type == MediaType.TV_SHOW })
    assertTrue("discoverMovies" in calls)
    assertTrue("discoverTvShows" in calls)
  }

  @Test
  fun `tv filters are translated to one discover query`() = runBlocking {
    var capturedQuery: Map<String, String> = emptyMap()
    val repository = TmdbExploreRepository(
      catalogApi = catalogApi { method, arguments ->
        when (method) {
          "discoverTvShows" -> {
            @Suppress("UNCHECKED_CAST")
            capturedQuery = arguments?.get(0) as Map<String, String>
            CatalogResponseDto(
              page = 1,
              totalPages = 1,
              totalResults = 1,
              results = listOf(series(1)),
            )
          }
          else -> error("Unexpected call: $method")
        }
      },
      config = TmdbConfig(bearerToken = "test-token"),
    )
    val filters = ExploreFilterState(
      sort = ExploreSort.TOP_RATED,
      contentType = ExploreContentType.TV_SERIES,
      categories = setOf(ExploreCategory.DRAMA),
      region = ExploreRegion.KOREA,
      year = ExploreYear.YEAR_2026,
      minimumRating = 7.5,
    )

    val result = repository.loadExplore(filters, page = 1)

    assertTrue(result is DataResult.Success)
    assertEquals("vote_average.desc", capturedQuery["sort_by"])
    assertEquals("18", capturedQuery["with_genres"])
    assertEquals("KR", capturedQuery["with_origin_country"])
    assertEquals("2026-01-01", capturedQuery["first_air_date.gte"])
    assertEquals("2026-12-31", capturedQuery["first_air_date.lte"])
    assertEquals("7.5", capturedQuery["vote_average.gte"])
    assertEquals("200", capturedQuery["vote_count.gte"])
  }

  @Test
  fun `provider options are dynamically merged by provider id`() = runBlocking {
    val repository = TmdbExploreRepository(
      catalogApi = catalogApi { method, _ ->
        when (method) {
          "getMovieWatchProviders" -> WatchProviderListResponseDto(
            results = listOf(provider(8, "Netflix", 2)),
          )
          "getTvWatchProviders" -> WatchProviderListResponseDto(
            results = listOf(
              provider(8, "Netflix", 1),
              provider(337, "Disney Plus", 3),
            ),
          )
          else -> error("Unexpected call: $method")
        }
      },
      config = TmdbConfig(bearerToken = "test-token"),
    )

    val result = repository.loadProviderOptions()

    assertTrue(result is DataResult.Success)
    val providers = (result as DataResult.Success).value
    assertEquals(listOf(8, 337), providers.map { it.id })
    assertEquals(1, providers.first().displayPriority)
  }

  private fun catalogApi(
    response: (method: String, arguments: Array<out Any?>?) -> Any,
  ): TmdbCatalogApi =
    Proxy.newProxyInstance(
      TmdbCatalogApi::class.java.classLoader,
      arrayOf(TmdbCatalogApi::class.java),
    ) { _, method, arguments ->
      response(method.name, arguments)
    } as TmdbCatalogApi

  private fun movie(id: Int): MovieDto =
    MovieDto(
      id = id,
      title = "Movie $id",
      posterPath = "/movie-$id.jpg",
      releaseDate = "2026-07-01",
      voteAverage = 8.0,
      voteCount = 1_000,
      popularity = 100.0 - id,
      originalLanguage = "en",
      genreIds = listOf(18),
      originCountry = listOf("US"),
    )

  private fun series(id: Int): TvShowDto =
    TvShowDto(
      id = id + 1_000,
      name = "Series $id",
      posterPath = "/series-$id.jpg",
      firstAirDate = "2026-07-01",
      voteAverage = 8.0,
      voteCount = 1_000,
      popularity = 100.0 - id,
      originalLanguage = "ko",
      genreIds = listOf(18),
      originCountry = listOf("KR"),
    )

  private fun provider(
    id: Int,
    name: String,
    priority: Int,
  ): WatchProviderDto =
    WatchProviderDto(
      providerId = id,
      providerName = name,
      displayPriority = priority,
    )
}
