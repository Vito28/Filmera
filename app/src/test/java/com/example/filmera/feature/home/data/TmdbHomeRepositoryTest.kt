package com.example.filmera.feature.home.data

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.network.TmdbCatalogApi
import com.example.filmera.core.network.TmdbConfig
import com.example.filmera.core.network.TmdbMovieApi
import com.example.filmera.core.network.TmdbSearchApi
import com.example.filmera.core.network.TmdbTvSeriesApi
import com.example.filmera.core.network.dto.CatalogResponseDto
import com.example.filmera.core.network.dto.CreditsResponseDto
import com.example.filmera.core.network.dto.DatedCatalogResponseDto
import com.example.filmera.core.network.dto.GenreListResponseDto
import com.example.filmera.core.network.dto.MovieDetailsDto
import com.example.filmera.core.network.dto.MovieDto
import com.example.filmera.core.network.dto.TvShowDto
import com.example.filmera.core.network.dto.VideoResponseDto
import com.example.filmera.feature.home.domain.AnimeTopic
import com.example.filmera.feature.home.domain.HomeChannel
import com.example.filmera.feature.home.domain.HomeCollectionKind
import com.example.filmera.feature.home.domain.HomeFeedKey
import com.example.filmera.feature.home.domain.HomeSectionType
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbHomeRepositoryTest {
  @Test
  fun `home emits critical catalog before enrichment completes`() = runBlocking {
    val repository = repository(RecordingCatalogApi())

    val emissions = repository
      .observeHomeContent(HomeFeedKey(HomeChannel.FOR_YOU))
      .toList()
      .filterIsInstance<DataResult.Success<com.example.filmera.feature.home.domain.HomeContent>>()

    assertTrue(emissions.size >= 4)
    val first = emissions.first().value
    assertTrue(first.section(HomeSectionType.HERO)?.isInitialLoading == false)
    assertTrue(first.section(HomeSectionType.UPCOMING)?.isInitialLoading == true)
    assertTrue(first.loadingSupplementals.isNotEmpty())
    assertTrue(emissions.last().value.hasPendingContent.not())
  }

  @Test
  fun `For You uses global endpoints with default English metadata`() = runBlocking {
    val catalog = RecordingCatalogApi()
    val repository = repository(catalog)

    val result = repository.loadHomeContent(HomeFeedKey(HomeChannel.FOR_YOU))

    assertTrue(result is DataResult.Success)
    assertEquals("en-US", catalog.nowPlayingLanguage)
    assertNull(catalog.nowPlayingRegion)
    assertTrue(catalog.trendingMovieRequested)
    assertTrue(catalog.trendingTvRequested)
  }

  @Test
  fun `korea filters origin without changing default metadata language`() = runBlocking {
    val catalog = RecordingCatalogApi()
    val repository = repository(catalog)

    val result = repository.loadHomeContent(HomeFeedKey(HomeChannel.KOREA))

    assertTrue(result is DataResult.Success)
    assertTrue(catalog.movieDiscoverQueries.isNotEmpty())
    catalog.movieDiscoverQueries.forEach { query ->
      assertEquals("KR", query["region"])
      assertEquals("KR", query["with_origin_country"])
      assertEquals("ko", query["with_original_language"])
      assertEquals("en-US", query["language"])
    }
    assertTrue(catalog.tvDiscoverQueries.all {
      it["with_origin_country"] == "KR" && it["with_original_language"] == "ko"
    })
  }

  @Test
  fun `anime fantasy combines Japanese animation and topic genres`() = runBlocking {
    val catalog = RecordingCatalogApi()
    val repository = repository(catalog)

    repository.loadSectionPage(
      feedKey = HomeFeedKey(HomeChannel.ANIME, AnimeTopic.FANTASY),
      sectionType = HomeSectionType.HERO,
      page = 1,
    )

    assertEquals("14,16", catalog.movieDiscoverQueries.single()["with_genres"])
    assertEquals("16,10765", catalog.tvDiscoverQueries.single()["with_genres"])
    assertEquals("JP", catalog.movieDiscoverQueries.single()["with_origin_country"])
    assertEquals("ja", catalog.movieDiscoverQueries.single()["with_original_language"])
  }

  @Test
  fun `section pagination forwards requested page and response metadata`() = runBlocking {
    val catalog = RecordingCatalogApi(totalPages = 4)
    val repository = repository(catalog)

    val result = repository.loadSectionPage(
      feedKey = HomeFeedKey(HomeChannel.KOREA),
      sectionType = HomeSectionType.TRENDING,
      page = 2,
    ) as DataResult.Success

    assertEquals(2, result.value.page)
    assertEquals(4, result.value.totalPages)
    assertTrue(catalog.movieDiscoverQueries.all { it["page"] == "2" })
  }

  @Test
  fun `missing token prevents all home network requests`() = runBlocking {
    val catalog = RecordingCatalogApi()
    val repository = TmdbHomeRepository(
      catalogApi = catalog.api,
      movieApi = movieApi(),
      searchApi = searchApi(),
      tvSeriesApi = tvApi(),
      config = TmdbConfig(bearerToken = ""),
    )

    val result = repository.loadHomeContent(HomeFeedKey(HomeChannel.FOR_YOU))

    assertEquals(DataResult.Error(AppError.MissingApiToken), result)
    assertEquals(0, catalog.requestCount)
  }

  @Test
  fun `one failed section keeps successful channel content`() = runBlocking {
    val catalog = RecordingCatalogApi(
      upcomingFailure = RuntimeException("failed endpoint"),
      discoveredMovies = listOf(sampleMovie),
    )
    val repository = repository(catalog)

    val result = repository.loadHomeContent(HomeFeedKey(HomeChannel.INDIA))

    val content = (result as DataResult.Success).value
    assertTrue(content.hasPartialFailures)
    assertTrue(content.section(HomeSectionType.HERO)?.items?.isNotEmpty() == true)
    assertEquals(
      AppError.Unknown,
      content.section(HomeSectionType.UPCOMING)?.error,
    )
  }

  @Test
  fun `Home limits trending and prepares editorial carousels`() = runBlocking {
    val movies = (1..20).map(::sampleMovie)
    val repository = repository(
      RecordingCatalogApi(
        discoveredMovies = movies,
        totalPages = 4,
      ),
    )

    val result = repository.loadHomeContent(HomeFeedKey(HomeChannel.FOR_YOU))

    val content = (result as DataResult.Success).value
    val trending = requireNotNull(content.section(HomeSectionType.TRENDING))
    assertEquals(15, trending.items.size)
    assertEquals(1, trending.totalPages)
    assertEquals(3, content.spotlights.size)
    assertEquals(5, content.collections.size)
    assertTrue(content.collections.any { it.kind == HomeCollectionKind.CINEMATIC_UNIVERSE })
  }

  @Test
  fun `Trending Show All preserves pagination beyond the Home limit`() = runBlocking {
    val repository = repository(
      RecordingCatalogApi(
        discoveredMovies = (1..20).map(::sampleMovie),
        totalPages = 4,
      ),
    )

    val result = repository.loadSectionPage(
      feedKey = HomeFeedKey(HomeChannel.FOR_YOU),
      sectionType = HomeSectionType.TRENDING,
      page = 2,
    ) as DataResult.Success

    assertEquals(20, result.value.items.size)
    assertEquals(2, result.value.page)
    assertEquals(4, result.value.totalPages)
  }

  private fun repository(catalog: RecordingCatalogApi) =
    TmdbHomeRepository(
      catalogApi = catalog.api,
      movieApi = movieApi(),
      searchApi = searchApi(),
      tvSeriesApi = tvApi(),
      config = TmdbConfig(bearerToken = "test-token"),
    )

  private inner class RecordingCatalogApi(
    private val upcomingFailure: Throwable? = null,
    private val discoveredMovies: List<MovieDto> = listOf(sampleMovie),
    private val totalPages: Int = 3,
  ) {
    val movieDiscoverQueries = mutableListOf<Map<String, String>>()
    val tvDiscoverQueries = mutableListOf<Map<String, String>>()
    var requestCount = 0
    var nowPlayingLanguage: String? = null
    var nowPlayingRegion: String? = "not-called"
    var trendingMovieRequested = false
    var trendingTvRequested = false

    val api: TmdbCatalogApi = proxy { methodName, arguments ->
      requestCount += 1
      when (methodName) {
        "getNowPlayingMovies" -> {
          nowPlayingLanguage = arguments[0] as String
          nowPlayingRegion = arguments[1] as String?
          DatedCatalogResponseDto(
            results = discoveredMovies,
            totalPages = totalPages,
          )
        }
        "getUpcomingMovies" -> DatedCatalogResponseDto(
          results = discoveredMovies,
          totalPages = totalPages,
        )
        "getOnTheAirTvShows", "getTopRatedTvShows", "getPopularTvShows" ->
          CatalogResponseDto<TvShowDto>(totalPages = totalPages)
        "getTrendingMovies" -> {
          trendingMovieRequested = true
          CatalogResponseDto(
            page = arguments[2] as Int,
            results = discoveredMovies,
            totalPages = totalPages,
          )
        }
        "getTrendingTvShows" -> {
          trendingTvRequested = true
          CatalogResponseDto<TvShowDto>(
            page = arguments[2] as Int,
            totalPages = totalPages,
          )
        }
        "getTopRatedMovies", "getPopularMovies" -> CatalogResponseDto(
          results = discoveredMovies,
          totalPages = totalPages,
        )
        "discoverMovies" -> {
          @Suppress("UNCHECKED_CAST")
          val query = arguments.first() as Map<String, String>
          movieDiscoverQueries += query
          if (
            upcomingFailure != null &&
            query["sort_by"] == "primary_release_date.asc"
          ) {
            throw upcomingFailure
          }
          CatalogResponseDto(
            page = query["page"]?.toInt() ?: 1,
            results = discoveredMovies,
            totalPages = totalPages,
          )
        }
        "discoverTvShows" -> {
          @Suppress("UNCHECKED_CAST")
          val query = arguments.first() as Map<String, String>
          tvDiscoverQueries += query
          if (
            upcomingFailure != null &&
            query["sort_by"] == "first_air_date.asc"
          ) {
            throw upcomingFailure
          }
          CatalogResponseDto<TvShowDto>(
            page = query["page"]?.toInt() ?: 1,
            totalPages = totalPages,
          )
        }
        "getMovieGenres", "getTvGenres" -> GenreListResponseDto()
        else -> error("Unexpected catalog call: $methodName")
      }
    }
  }

  private fun movieApi(): TmdbMovieApi =
    proxy { methodName, _ ->
      when (methodName) {
        "getMovieCredits" -> CreditsResponseDto()
        "getMovieVideos" -> VideoResponseDto()
        "getMovieDetails" -> MovieDetailsDto()
        else -> error("Unexpected movie call: $methodName")
      }
    }

  private fun tvApi(): TmdbTvSeriesApi =
    proxy { methodName, _ ->
      when (methodName) {
        "getTvCredits" -> CreditsResponseDto()
        "getTvVideos" -> VideoResponseDto()
        else -> error("Unexpected TV call: $methodName")
      }
    }

  private fun searchApi(): TmdbSearchApi =
    proxy { methodName, _ ->
      when (methodName) {
        "searchCollections" -> CatalogResponseDto<com.example.filmera.core.network.dto.CollectionDto>()
        else -> error("Unexpected search call: $methodName")
      }
    }

  private inline fun <reified T> proxy(
    crossinline answer: (methodName: String, arguments: List<Any?>) -> Any?,
  ): T =
    Proxy.newProxyInstance(
      T::class.java.classLoader,
      arrayOf(T::class.java),
    ) { _, method, arguments ->
      answer(
        method.name,
        arguments?.toList().orEmpty(),
      )
    } as T

  private companion object {
    val sampleMovie = sampleMovie(17)

    fun sampleMovie(id: Int) = MovieDto(
      id = id,
      title = "Channel movie $id",
      overview = "Overview",
      backdropPath = "/backdrop-$id.jpg",
      posterPath = "/poster-$id.jpg",
      voteCount = 1_000,
      voteAverage = 8.0,
      originalLanguage = "en",
      genreIds = listOf(18, 28),
    )
  }
}
