package com.example.filmera.feature.person.data

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.network.TmdbAppendToResponse
import com.example.filmera.core.network.TmdbConfig
import com.example.filmera.core.network.TmdbPersonApi
import com.example.filmera.core.network.dto.ExternalIdsDto
import com.example.filmera.core.network.dto.CatalogResponseDto
import com.example.filmera.core.network.dto.PersonCombinedCreditsResponseDto
import com.example.filmera.core.network.dto.PersonDetailsDto
import com.example.filmera.core.network.dto.PersonDto
import com.example.filmera.core.network.dto.PersonImagesResponseDto
import com.example.filmera.core.network.dto.TranslationResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbPersonRepositoryTest {
  @Test
  fun `load person uses one appended details request`() = runBlocking {
    val api = FakePersonApi(PersonDetailsDto(id = 31, name = "Performer"))
    val repository = TmdbPersonRepository(
      personApi = api,
      config = TmdbConfig(bearerToken = "test-token"),
    )

    val result = repository.loadPerson(31)

    assertEquals("Performer", (result as DataResult.Success).value.name)
    assertEquals(1, api.requestCount)
    assertEquals(TmdbAppendToResponse.PERSON_DETAILS, api.appendToResponse)
  }

  @Test
  fun `load person fails before network when token is missing`() = runBlocking {
    val api = FakePersonApi(PersonDetailsDto(id = 31, name = "Performer"))
    val repository = TmdbPersonRepository(
      personApi = api,
      config = TmdbConfig(bearerToken = ""),
    )

    val result = repository.loadPerson(31)

    assertEquals(DataResult.Error(AppError.MissingApiToken), result)
    assertEquals(0, api.requestCount)
  }

  private class FakePersonApi(
    private val details: PersonDetailsDto,
  ) : TmdbPersonApi {
    var requestCount = 0
      private set
    var appendToResponse: String? = null
      private set

    override suspend fun getPopularPeople(
      language: String,
      page: Int,
    ): CatalogResponseDto<PersonDto> = error("Not used")

    override suspend fun getPersonDetails(
      personId: Int,
      language: String,
      appendToResponse: String?,
    ): PersonDetailsDto {
      requestCount += 1
      this.appendToResponse = appendToResponse
      return details
    }

    override suspend fun getPersonCombinedCredits(
      personId: Int,
      language: String,
    ): PersonCombinedCreditsResponseDto = error("Not used")

    override suspend fun getPersonExternalIds(personId: Int): ExternalIdsDto =
      error("Not used")

    override suspend fun getPersonImages(personId: Int): PersonImagesResponseDto =
      error("Not used")

    override suspend fun getPersonTranslations(personId: Int): TranslationResponseDto =
      error("Not used")
  }
}
