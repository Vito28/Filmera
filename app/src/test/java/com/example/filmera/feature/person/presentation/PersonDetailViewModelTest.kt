package com.example.filmera.feature.person.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.navigation.AppDestination
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.person.domain.PersonDetails
import com.example.filmera.feature.person.domain.PersonExternalIds
import com.example.filmera.feature.person.domain.PersonGender
import com.example.filmera.feature.person.domain.PersonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class PersonDetailViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `valid person id loads person details`() = runTest {
    val repository = FakePersonRepository(DataResult.Success(samplePerson()))
    val viewModel = PersonDetailViewModel(
      savedStateHandle = SavedStateHandle(
        mapOf(AppDestination.PersonDetail.PERSON_ID_ARGUMENT to 31),
      ),
      personRepository = repository,
    )

    advanceUntilIdle()

    val state = viewModel.uiState.value.contentState as LoadState.Success
    assertEquals("Performer", state.value.name)
    assertEquals(listOf(31), repository.requestedIds)
  }

  @Test
  fun `invalid person id returns not found without repository access`() = runTest {
    val repository = FakePersonRepository(DataResult.Success(samplePerson()))
    val viewModel = PersonDetailViewModel(
      savedStateHandle = SavedStateHandle(
        mapOf(AppDestination.PersonDetail.PERSON_ID_ARGUMENT to 0),
      ),
      personRepository = repository,
    )

    advanceUntilIdle()

    assertEquals(emptyList<Int>(), repository.requestedIds)
    assertEquals(
      com.example.filmera.core.common.AppError.NotFound,
      (viewModel.uiState.value.contentState as LoadState.Error).error,
    )
  }

  private class FakePersonRepository(
    private val result: DataResult<PersonDetails>,
  ) : PersonRepository {
    val requestedIds = mutableListOf<Int>()

    override suspend fun loadPerson(personId: Int): DataResult<PersonDetails> {
      requestedIds += personId
      return result
    }
  }

  private fun samplePerson() = PersonDetails(
    id = 31,
    name = "Performer",
    biography = "Biography",
    birthday = null,
    deathday = null,
    gender = PersonGender.NOT_SPECIFIED,
    placeOfBirth = null,
    knownForDepartment = "Acting",
    profilePath = null,
    alsoKnownAs = emptyList(),
    homepage = null,
    popularity = 1.0,
    externalIds = PersonExternalIds(
      imdbId = null,
      wikidataId = null,
      facebookId = null,
      instagramId = null,
      twitterId = null,
      tiktokId = null,
      youtubeId = null,
    ),
    photos = emptyList(),
    movieCredits = emptyList(),
    tvCredits = emptyList(),
  )

  class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
  ) : TestWatcher() {
    override fun starting(description: Description) {
      Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
      Dispatchers.resetMain()
    }
  }
}
