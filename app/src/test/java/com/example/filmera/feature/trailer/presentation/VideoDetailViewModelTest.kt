package com.example.filmera.feature.trailer.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.Credits
import com.example.filmera.core.model.MediaDetails
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.model.MediaVideo
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.detail.domain.DetailContent
import com.example.filmera.feature.detail.domain.DetailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class VideoDetailViewModelTest {
  @get:Rule
  val mainDispatcherRule = VideoMainDispatcherRule()

  @Test
  fun `valid video route loads title context and keeps requested video active`() = runTest {
    val requestedKey = "requested-video"
    val repository = FakeDetailRepository(
      DataResult.Success(sampleContent(requestedKey)),
    )

    val viewModel = createViewModel(
      repository = repository,
      mediaType = MediaType.MOVIE.routeValue,
      mediaId = 44,
      videoKey = requestedKey,
    )
    advanceUntilIdle()

    assertEquals(listOf(MediaKey(44, MediaType.MOVIE)), repository.requests)
    assertEquals(requestedKey, viewModel.uiState.value.activeVideo?.key)
    assertTrue(viewModel.uiState.value.contentState is LoadState.Success)
  }

  @Test
  fun `video selection switches player without reloading details`() = runTest {
    val repository = FakeDetailRepository(
      DataResult.Success(sampleContent("first-video")),
    )
    val viewModel = createViewModel(
      repository = repository,
      mediaType = MediaType.MOVIE.routeValue,
      mediaId = 44,
      videoKey = "first-video",
    )
    advanceUntilIdle()

    viewModel.onAction(VideoDetailAction.VideoSelected("second-video"))

    assertEquals("second-video", viewModel.uiState.value.activeVideo?.key)
    assertEquals(1, repository.requests.size)
  }

  @Test
  fun `invalid media route returns not found without repository request`() = runTest {
    val repository = FakeDetailRepository(
      DataResult.Success(sampleContent("video")),
    )

    val viewModel = createViewModel(
      repository = repository,
      mediaType = "person",
      mediaId = 0,
      videoKey = "video",
    )
    advanceUntilIdle()

    assertTrue(repository.requests.isEmpty())
    assertEquals(
      com.example.filmera.core.common.AppError.NotFound,
      (viewModel.uiState.value.contentState as LoadState.Error).error,
    )
  }

  private fun createViewModel(
    repository: DetailRepository,
    mediaType: String,
    mediaId: Int,
    videoKey: String,
  ) = VideoDetailViewModel(
    detailRepository = repository,
    savedStateHandle = SavedStateHandle(
      mapOf(
        VideoDetailViewModel.MEDIA_TYPE_ARGUMENT to mediaType,
        VideoDetailViewModel.MEDIA_ID_ARGUMENT to mediaId,
        VideoDetailViewModel.VIDEO_KEY_ARGUMENT to videoKey,
      ),
    ),
  )

  private class FakeDetailRepository(
    private val result: DataResult<DetailContent>,
  ) : DetailRepository {
    val requests = mutableListOf<MediaKey>()

    override suspend fun loadDetails(key: MediaKey): DataResult<DetailContent> {
      requests += key
      return result
    }
  }

  private fun sampleContent(activeKey: String): DetailContent {
    val videos = listOf(
      MediaVideo(
        key = activeKey,
        name = "Official trailer",
        site = "YouTube",
        type = "Trailer",
        isOfficial = true,
      ),
      MediaVideo(
        key = "second-video",
        name = "Featurette",
        site = "YouTube",
        type = "Featurette",
        isOfficial = true,
      ),
    )
    return DetailContent(
      details = sampleDetails(),
      credits = Credits(cast = emptyList(), crew = emptyList()),
      trailer = videos.first(),
      videos = videos,
      recommendations = emptyList<MediaItem>(),
      hasPartialFailures = false,
    )
  }

  private fun sampleDetails() = MediaDetails(
    id = 44,
    type = MediaType.MOVIE,
    title = "Sample title",
    originalTitle = "Sample title",
    overview = "Overview",
    tagline = null,
    posterPath = null,
    backdropPath = null,
    releaseDate = "2026-07-01",
    runtimeMinutes = 120,
    voteAverage = 8.0,
    voteCount = 100,
    popularity = 10.0,
    originalLanguage = "en",
    status = "Released",
    homepage = null,
    externalId = null,
    budget = null,
    revenue = null,
    genres = emptyList(),
    productionCompanies = emptyList(),
    productionCountries = emptyList(),
    spokenLanguages = listOf("English"),
  )

  class VideoMainDispatcherRule(
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
