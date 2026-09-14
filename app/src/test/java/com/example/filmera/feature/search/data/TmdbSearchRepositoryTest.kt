package com.example.filmera.feature.search.data

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.network.TmdbCatalogApi
import com.example.filmera.core.network.TmdbConfig
import com.example.filmera.core.network.TmdbPersonApi
import com.example.filmera.core.network.TmdbSearchApi
import com.example.filmera.core.network.dto.CatalogResponseDto
import com.example.filmera.core.network.dto.CollectionDto
import com.example.filmera.core.network.dto.MovieDto
import com.example.filmera.core.network.dto.PersonDto
import com.example.filmera.core.network.dto.TvShowDto
import com.example.filmera.feature.search.domain.SearchRequest
import com.example.filmera.feature.search.domain.SearchTopResult
import com.example.filmera.feature.search.domain.SearchType
import java.io.IOException
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbSearchRepositoryTest {
  @Test
  fun `all search keeps movie tv person and collection in separate groups`() = runBlocking {
    val api = FakeSearchApi(
      movieResponse = CatalogResponseDto(
        results = listOf(
          MovieDto(
            id = 1,
            title = "Arrival",
            popularity = 10.0,
            voteCount = 100,
          ),
        ),
      ),
      tvResponse = CatalogResponseDto(
        results = listOf(
          TvShowDto(
            id = 2,
            name = "Dark",
            popularity = 20.0,
            voteCount = 200,
          ),
        ),
      ),
      peopleResponse = CatalogResponseDto(
        results = listOf(
          PersonDto(id = 3, name = "Amy Adams", knownForDepartment = "Acting"),
        ),
      ),
      collectionResponse = CatalogResponseDto(
        results = listOf(CollectionDto(id = 4, name = "Dune Collection")),
      ),
    )
    val repository = createRepository(api)

    val result = repository.search(SearchRequest("science"))

    val content = (result as DataResult.Success).value
    assertEquals(listOf("Arrival"), content.movies.map { it.title })
    assertEquals(listOf(MediaType.TV_SHOW), content.tvShows.map { it.type })
    assertEquals(listOf("Amy Adams"), content.people.map { it.name })
    assertEquals(listOf("Dune Collection"), content.collections.map { it.name })
    assertTrue(content.topResult is SearchTopResult.Media)
    assertTrue(!content.hasPartialFailures)
  }

  @Test
  fun `all search keeps successful groups when one endpoint fails`() = runBlocking {
    val api = FakeSearchApi(
      movieResponse = CatalogResponseDto(
        results = listOf(MovieDto(id = 1, title = "Arrival")),
      ),
      tvFailure = IOException("offline"),
    )
    val repository = createRepository(api)

    val result = repository.search(SearchRequest("arrival"))

    val content = (result as DataResult.Success).value
    assertEquals(listOf("Arrival"), content.movies.map { it.title })
    assertTrue(content.tvShows.isEmpty())
    assertTrue(content.hasPartialFailures)
  }

  @Test
  fun `specific search exposes safe pagination metadata`() = runBlocking {
    val api = FakeSearchApi(
      peopleResponse = CatalogResponseDto(
        page = 2,
        totalPages = 5,
        results = listOf(PersonDto(id = 7, name = "Lee Jung-jae")),
      ),
    )
    val repository = createRepository(api)

    val result = repository.search(
      SearchRequest(query = "Lee", type = SearchType.PEOPLE, page = 2),
    )

    val content = (result as DataResult.Success).value
    assertEquals(2, content.page)
    assertEquals(5, content.totalPages)
    assertTrue(content.canLoadMore)
    assertEquals(listOf("Lee Jung-jae"), content.people.map { it.name })
  }

  @Test
  fun `search fails before network access when token is missing`() = runBlocking {
    val api = FakeSearchApi()
    val repository = createRepository(
      api = api,
      config = TmdbConfig(bearerToken = ""),
    )

    val result = repository.search(SearchRequest("anything"))

    assertEquals(DataResult.Error(AppError.MissingApiToken), result)
    assertEquals(0, api.requestCount)
  }

  private fun createRepository(
    api: TmdbSearchApi,
    config: TmdbConfig = TmdbConfig(bearerToken = "test-token"),
  ) = TmdbSearchRepository(
    searchApi = api,
    catalogApi = unusedApi(),
    personApi = unusedApi(),
    config = config,
  )

  @Suppress("UNCHECKED_CAST")
  private inline fun <reified T> unusedApi(): T =
    Proxy.newProxyInstance(
      T::class.java.classLoader,
      arrayOf(T::class.java),
    ) { _, method, _ ->
      error("${method.name} should not be called by this test")
    } as T

  private class FakeSearchApi(
    private val movieResponse: CatalogResponseDto<MovieDto> = CatalogResponseDto(),
    private val tvResponse: CatalogResponseDto<TvShowDto> = CatalogResponseDto(),
    private val peopleResponse: CatalogResponseDto<PersonDto> = CatalogResponseDto(),
    private val collectionResponse: CatalogResponseDto<CollectionDto> = CatalogResponseDto(),
    private val movieFailure: Throwable? = null,
    private val tvFailure: Throwable? = null,
    private val peopleFailure: Throwable? = null,
    private val collectionFailure: Throwable? = null,
  ) : TmdbSearchApi {
    var requestCount = 0
      private set

    override suspend fun searchMovies(
      query: String,
      language: String,
      region: String,
      page: Int,
      includeAdult: Boolean,
      year: Int?,
      primaryReleaseYear: Int?,
    ): CatalogResponseDto<MovieDto> {
      requestCount += 1
      movieFailure?.let { throw it }
      return movieResponse
    }

    override suspend fun searchTvShows(
      query: String,
      language: String,
      page: Int,
      includeAdult: Boolean,
      firstAirDateYear: Int?,
      year: Int?,
    ): CatalogResponseDto<TvShowDto> {
      requestCount += 1
      tvFailure?.let { throw it }
      return tvResponse
    }

    override suspend fun searchPeople(
      query: String,
      language: String,
      page: Int,
      includeAdult: Boolean,
    ): CatalogResponseDto<PersonDto> {
      requestCount += 1
      peopleFailure?.let { throw it }
      return peopleResponse
    }

    override suspend fun searchCollections(
      query: String,
      language: String,
      page: Int,
      includeAdult: Boolean,
    ): CatalogResponseDto<CollectionDto> {
      requestCount += 1
      collectionFailure?.let { throw it }
      return collectionResponse
    }

    override suspend fun searchMulti(
      query: String,
      language: String,
      page: Int,
      includeAdult: Boolean,
    ) = error("Not required by this repository test")
  }
}

