package com.example.filmera.core.network

import com.example.filmera.core.network.dto.CatalogResponseDto
import com.example.filmera.core.network.dto.DatedCatalogResponseDto
import com.example.filmera.core.network.dto.MovieDto
import com.example.filmera.core.network.dto.TvShowDto
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbCatalogFlowTest {
  @Test
  fun `now playing feed preserves locale page and movie result type`() = runBlocking {
    var receivedArguments: List<Any?> = emptyList()
    val api = proxyCatalogApi { methodName, arguments ->
      assertEquals("getNowPlayingMovies", methodName)
      receivedArguments = arguments
      DatedCatalogResponseDto(
        page = 4,
        results = listOf(MovieDto(id = 10, title = "Movie")),
      )
    }

    val result = api.fetchCatalog(
      TmdbCatalogRequest(
        feed = TmdbCatalogFeed.NOW_PLAYING_MOVIES,
        locale = TmdbLocale(language = "en-US", region = "US"),
        page = 4,
      ),
    ) as TmdbCatalogPageDto.Movies

    assertEquals(listOf("en-US", "US", 4), receivedArguments)
    assertEquals(10, result.response.results.single().id)
  }

  @Test
  fun `Korean drama feed resolves to TV discover filters`() = runBlocking {
    var receivedFilters: Map<String, String> = emptyMap()
    val api = proxyCatalogApi { methodName, arguments ->
      assertEquals("discoverTvShows", methodName)
      @Suppress("UNCHECKED_CAST")
      receivedFilters = arguments.single() as Map<String, String>
      CatalogResponseDto(
        results = listOf(TvShowDto(id = 20, name = "Drama")),
      )
    }

    val result = api.fetchCatalog(
      TmdbCatalogRequest(
        feed = TmdbCatalogFeed.KOREAN_DRAMA,
        locale = TmdbLocale(language = "id-ID", region = "ID"),
      ),
    ) as TmdbCatalogPageDto.TvShows

    assertEquals("KR", receivedFilters["with_origin_country"])
    assertEquals("ko", receivedFilters["with_original_language"])
    assertEquals("18", receivedFilters["with_genres"])
    assertEquals(20, result.response.results.single().id)
  }

  private fun proxyCatalogApi(
    response: (methodName: String, arguments: List<Any?>) -> Any,
  ): TmdbCatalogApi =
    Proxy.newProxyInstance(
      TmdbCatalogApi::class.java.classLoader,
      arrayOf(TmdbCatalogApi::class.java),
    ) { _, method, rawArguments ->
      val argumentsWithoutContinuation = rawArguments
        .orEmpty()
        .dropLast(1)
      response(method.name, argumentsWithoutContinuation)
    } as TmdbCatalogApi
}
