package com.example.filmera.feature.discover.data

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.network.TmdbCatalogApi
import com.example.filmera.core.network.TmdbConfig
import com.example.filmera.core.network.dto.CatalogResponseDto
import com.example.filmera.core.network.dto.MultiSearchItemDto
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbRankingRepositoryTest {

  @Test
  fun `weekly trending pages produce exactly one hundred ranked media titles`() = runBlocking {
    val requestedPages = mutableListOf<Int>()
    val api = catalogApi { page ->
      requestedPages += page
      CatalogResponseDto(
        page = page,
        totalPages = 8,
        results = (1..20).map { index ->
          val id = (page - 1) * 20 + index
          when {
            index % 5 == 0 -> MultiSearchItemDto(
              mediaType = "person",
              id = id,
              name = "Person $id",
            )
            index % 2 == 0 -> MultiSearchItemDto(
              mediaType = "tv",
              id = id,
              name = "Series $id",
              posterPath = "/poster-$id.jpg",
            )
            else -> MultiSearchItemDto(
              mediaType = "movie",
              id = id,
              title = "Movie $id",
              posterPath = "/poster-$id.jpg",
            )
          }
        },
      )
    }
    val repository = TmdbRankingRepository(
      catalogApi = api,
      config = TmdbConfig(bearerToken = "test-token"),
    )

    val result = repository.loadWeeklyRankings()

    assertTrue(result is DataResult.Success)
    val rankings = (result as DataResult.Success).value
    assertEquals(100, rankings.size)
    assertEquals((1..100).toList(), rankings.map { it.rank })
    assertEquals((1..8).toSet(), requestedPages.toSet())
  }

  @Test
  fun `missing token prevents ranking requests`() = runBlocking {
    var requestCount = 0
    val repository = TmdbRankingRepository(
      catalogApi = catalogApi {
        requestCount += 1
        CatalogResponseDto()
      },
      config = TmdbConfig(bearerToken = ""),
    )

    val result = repository.loadWeeklyRankings()

    assertEquals(DataResult.Error(AppError.MissingApiToken), result)
    assertEquals(0, requestCount)
  }

  private fun catalogApi(
    response: (page: Int) -> CatalogResponseDto<MultiSearchItemDto>,
  ): TmdbCatalogApi =
    Proxy.newProxyInstance(
      TmdbCatalogApi::class.java.classLoader,
      arrayOf(TmdbCatalogApi::class.java),
    ) { _, method, arguments ->
      when (method.name) {
        "getTrendingAll" -> response(arguments?.get(2) as Int)
        else -> error("Unexpected catalog call: ${method.name}")
      }
    } as TmdbCatalogApi
}
