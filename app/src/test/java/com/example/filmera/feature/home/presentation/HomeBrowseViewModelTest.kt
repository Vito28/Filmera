package com.example.filmera.feature.home.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.home.domain.AnimeTopic
import com.example.filmera.feature.home.domain.HomeBrowseKind
import com.example.filmera.feature.home.domain.HomeChannel
import com.example.filmera.feature.home.domain.HomeContent
import com.example.filmera.feature.home.domain.HomeFeedKey
import com.example.filmera.feature.home.domain.HomeRepository
import com.example.filmera.feature.home.domain.HomeSectionPage
import com.example.filmera.feature.home.domain.HomeSectionType
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
class HomeBrowseViewModelTest {
  @get:Rule
  val mainDispatcherRule = BrowseMainDispatcherRule()

  @Test
  fun `media browse loads the dedicated section for the active feed`() = runTest {
    val repository = FakeBrowseRepository()
    val viewModel = createViewModel(
      repository = repository,
      kind = HomeBrowseKind.TOP_RATED,
      channel = HomeChannel.KOREA,
    )

    advanceUntilIdle()

    assertEquals(
      listOf(PageRequest(HomeSectionType.TOP_RATED, HomeChannel.KOREA, 1)),
      repository.pageRequests,
    )
    val state = viewModel.uiState.value
    assertEquals(HomeBrowseKind.TOP_RATED, state.kind)
    assertTrue(state.contentState is LoadState.Success)
  }

  @Test
  fun `media browse appends unique results during pagination`() = runTest {
    val repository = FakeBrowseRepository()
    val viewModel = createViewModel(repository, HomeBrowseKind.UPCOMING)
    advanceUntilIdle()

    viewModel.onAction(HomeBrowseAction.LoadMore)
    advanceUntilIdle()

    val content = (viewModel.uiState.value.contentState as LoadState.Success).value
    assertEquals(listOf(1, 2), content.mediaItems.map(MediaItem::id))
    assertEquals(2, viewModel.uiState.value.page)
  }

  @Test
  fun `non media browse uses supporting Home content instead of Search`() = runTest {
    val repository = FakeBrowseRepository()
    val viewModel = createViewModel(repository, HomeBrowseKind.GENRES)
    advanceUntilIdle()

    assertEquals(listOf(HomeFeedKey(HomeChannel.FOR_YOU)), repository.homeRequests)
    assertTrue(viewModel.uiState.value.contentState is LoadState.Empty)
    assertTrue(repository.pageRequests.isEmpty())
  }

  @Test
  fun `supporting browse exposes its section error instead of a false empty state`() = runTest {
    val repository = FakeBrowseRepository(
      genresError = AppError.NetworkUnavailable,
    )
    val viewModel = createViewModel(repository, HomeBrowseKind.GENRES)

    advanceUntilIdle()

    assertEquals(
      LoadState.Error(AppError.NetworkUnavailable),
      viewModel.uiState.value.contentState,
    )
  }

  private fun createViewModel(
    repository: HomeRepository,
    kind: HomeBrowseKind,
    channel: HomeChannel = HomeChannel.FOR_YOU,
  ): HomeBrowseViewModel =
    HomeBrowseViewModel(
      homeRepository = repository,
      savedStateHandle = SavedStateHandle(
        mapOf(
          HomeBrowseViewModel.BROWSE_KIND_ARGUMENT to kind.name,
          HomeBrowseViewModel.CHANNEL_ARGUMENT to channel.name,
          HomeBrowseViewModel.ANIME_TOPIC_ARGUMENT to AnimeTopic.ALL.name,
        ),
      ),
    )

  private class FakeBrowseRepository(
    private val genresError: AppError? = null,
  ) : HomeRepository {
    val pageRequests = mutableListOf<PageRequest>()
    val homeRequests = mutableListOf<HomeFeedKey>()

    override suspend fun loadHomeContent(feedKey: HomeFeedKey): DataResult<HomeContent> {
      homeRequests += feedKey
      return DataResult.Success(
        HomeContent(
          feedKey = feedKey,
          sections = emptyList(),
          genres = emptyList(),
          people = emptyList(),
          genresError = genresError,
          hasPartialFailures = false,
        ),
      )
    }

    override suspend fun loadSectionPage(
      feedKey: HomeFeedKey,
      sectionType: HomeSectionType,
      page: Int,
    ): DataResult<HomeSectionPage> {
      pageRequests += PageRequest(sectionType, feedKey.channel, page)
      return DataResult.Success(
        HomeSectionPage(
          type = sectionType,
          items = if (page == 1) {
            listOf(sampleMedia(1))
          } else {
            listOf(sampleMedia(1), sampleMedia(2))
          },
          page = page,
          totalPages = 2,
        ),
      )
    }
  }

  class BrowseMainDispatcherRule(
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

private data class PageRequest(
  val type: HomeSectionType,
  val channel: HomeChannel,
  val page: Int,
)

private fun sampleMedia(id: Int): MediaItem =
  MediaItem(
    id = id,
    type = MediaType.MOVIE,
    title = "Movie $id",
    originalTitle = "Movie $id",
    overview = "Overview",
    posterPath = "/poster-$id.jpg",
    backdropPath = "/backdrop-$id.jpg",
    releaseDate = "2026-07-01",
    voteAverage = 8.0,
    voteCount = 500,
    popularity = 100.0,
    adult = false,
    originalLanguage = "en",
    genreIds = listOf(18),
  )
