package com.example.filmera.feature.profile.presentation

import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.home.domain.HomeContent
import com.example.filmera.feature.home.domain.HomeFeedKey
import com.example.filmera.feature.home.domain.HomeRepository
import com.example.filmera.feature.home.domain.HomeSectionPage
import com.example.filmera.feature.home.domain.HomeSectionType
import com.example.filmera.feature.library.domain.LibraryItem
import com.example.filmera.feature.library.domain.LibraryRepository
import com.example.filmera.feature.library.domain.WatchStatus
import com.example.filmera.feature.profile.domain.ProfileRepository
import com.example.filmera.feature.profile.domain.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class ProfileViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `summary keeps favorite independent from watch status`() = runTest {
    val repository = FakeLibraryRepository(
      listOf(
        libraryItem(id = 1, status = WatchStatus.WATCHLIST, isFavorite = true),
        libraryItem(id = 2, status = WatchStatus.WATCHING, isFavorite = false),
        libraryItem(id = 3, status = WatchStatus.NONE, isFavorite = true),
      ),
    )
    val viewModel = ProfileViewModel(
      libraryRepository = repository,
      profileRepository = FakeProfileRepository(),
      homeRepository = FakeHomeRepository(),
    )
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
    }

    advanceUntilIdle()

    val summary = (viewModel.uiState.value.libraryState as LoadState.Success).value.summary
    assertEquals(3, summary.savedCount)
    assertEquals(1, summary.watchlistCount)
    assertEquals(1, summary.watchingCount)
    assertEquals(2, summary.favoriteCount)
  }

  @Test
  fun `header prioritizes real library posters before trending fallback`() = runTest {
    val viewModel = ProfileViewModel(
      libraryRepository = FakeLibraryRepository(
        listOf(
          libraryItem(
            id = 1,
            status = WatchStatus.WATCHLIST,
            isFavorite = true,
            posterPath = "/favorite.jpg",
          ),
        ),
      ),
      profileRepository = FakeProfileRepository(),
      homeRepository = FakeHomeRepository(
        items = listOf(
          mediaItem(id = 2, posterPath = "/trending.jpg"),
        ),
      ),
    )
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
    }

    advanceUntilIdle()

    val content = (viewModel.uiState.value.libraryState as LoadState.Success).value
    assertEquals(
      listOf("/favorite.jpg", "/trending.jpg"),
      content.headerPosters.map(ProfileHeaderPoster::path),
    )
  }

  private class FakeLibraryRepository(
    initialItems: List<LibraryItem>,
  ) : LibraryRepository {
    private val items = MutableStateFlow(initialItems)

    override fun observeLibrary(): Flow<List<LibraryItem>> = items

    override suspend fun setWatchStatus(item: MediaItem, status: WatchStatus) = Unit

    override suspend fun setFavorite(item: MediaItem, isFavorite: Boolean) = Unit

    override suspend fun restore(item: LibraryItem) = Unit
  }

  private class FakeProfileRepository : ProfileRepository {
    override suspend fun getCurrentProfile(): DataResult<UserProfile> = DataResult.Success(
      UserProfile(
        displayName = "Raka Wijaya",
        username = "rakawijaya",
        avatarUrl = null,
        bio = null,
      ),
    )
  }

  private class FakeHomeRepository(
    private val items: List<MediaItem> = emptyList(),
  ) : HomeRepository {
    override suspend fun loadHomeContent(feedKey: HomeFeedKey): DataResult<HomeContent> =
      error("Not needed by this test")

    override suspend fun loadSectionPage(
      feedKey: HomeFeedKey,
      sectionType: HomeSectionType,
      page: Int,
    ): DataResult<HomeSectionPage> = DataResult.Success(
      HomeSectionPage(
        type = sectionType,
        items = items,
        page = page,
        totalPages = 1,
      ),
    )
  }

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

private fun libraryItem(
  id: Int,
  status: WatchStatus,
  isFavorite: Boolean,
  posterPath: String? = null,
): LibraryItem = LibraryItem(
  media = mediaItem(id = id, posterPath = posterPath),
  watchStatus = status,
  isFavorite = isFavorite,
  addedAt = id.toLong(),
  updatedAt = id.toLong(),
)

private fun mediaItem(
  id: Int,
  posterPath: String? = null,
): MediaItem = MediaItem(
  id = id,
  type = MediaType.MOVIE,
  title = "Movie $id",
  originalTitle = "Movie $id",
  overview = "",
  posterPath = posterPath,
  backdropPath = null,
  releaseDate = "2026-01-01",
  voteAverage = 8.0,
  voteCount = 100,
  popularity = 10.0,
  adult = false,
  originalLanguage = "en",
  genreIds = emptyList(),
)
