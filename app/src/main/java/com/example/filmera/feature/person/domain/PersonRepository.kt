package com.example.filmera.feature.person.domain

import com.example.filmera.core.common.DataResult

interface PersonRepository {
  suspend fun loadPerson(personId: Int): DataResult<PersonDetails>
}
