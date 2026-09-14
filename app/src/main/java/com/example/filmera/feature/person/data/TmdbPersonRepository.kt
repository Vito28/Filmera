package com.example.filmera.feature.person.data

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.common.map
import com.example.filmera.core.network.TmdbAppendToResponse
import com.example.filmera.core.network.TmdbConfig
import com.example.filmera.core.network.TmdbPersonApi
import com.example.filmera.core.network.safeNetworkCall
import com.example.filmera.feature.person.domain.PersonDetails
import com.example.filmera.feature.person.domain.PersonRepository
import javax.inject.Inject

class TmdbPersonRepository @Inject constructor(
  private val personApi: TmdbPersonApi,
  private val config: TmdbConfig,
) : PersonRepository {

  override suspend fun loadPerson(personId: Int): DataResult<PersonDetails> {
    if (personId <= 0) return DataResult.Error(AppError.NotFound)
    if (!config.isConfigured) return DataResult.Error(AppError.MissingApiToken)

    return safeNetworkCall {
      personApi.getPersonDetails(
        personId = personId,
        appendToResponse = TmdbAppendToResponse.PERSON_DETAILS,
      )
    }.map { it.toDomain() }
  }
}
