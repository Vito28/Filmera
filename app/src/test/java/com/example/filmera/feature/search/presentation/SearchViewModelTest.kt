package com.example.filmera.feature.search.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaItem
import com.example.filmera.feature.library.domain.LibraryItem
import com.example.filmera.feature.library.domain.LibraryRepository
import com.example.filmera.feature.library.domain.WatchStatus
import com.example.filmera.feature.search.domain.RecentSearchRepository
import com.example.filmera.feature.search.domain.SearchContent
import com.example.filmera.feature.search.domain.SearchDiscovery
import com.example.filmera.feature.search.domain.SearchRepository
import com.example.filmera.feature.search.domain.SearchRequest
import com.example.filmera.feature.search.domain.SearchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class SearchViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `typing is debounced and only latest query is searched`() = runTest {
    val searchRepository = FakeSearchRepository()
    val viewModel = createViewModel(searchRepository = searchRepository)
    collectState(viewModel)
    advanceUntilIdle()
    searchRepository.requests.clear()

    viewModel.onEvent(SearchUiEvent.QueryChanged("d"))
    advanceTimeBy(150)
    viewModel.onEvent(SearchUiEvent.QueryChanged("dark"))
    advanceTimeBy(299)

    assertEquals(emptyList<SearchRequest>(), searchRepository.requests)

    advanceTimeBy(1)
    advanceUntilIdle()

    assertEquals(
      listOf(SearchRequest(query = "dark", type = SearchType.ALL)),
      searchRepository.requests,
    )
  }

  @Test
  fun `selecting type preserves query and searches immediately`() = runTest {
    val searchRepository = FakeSearchRepository()
    val viewModel = createViewModel(searchRepository = searchRepository)
    collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(SearchUiEvent.QueryChanged("Dune"))
    advanceTimeBy(300)
    advanceUntilIdle()
    searchRepository.requests.clear()

    viewModel.onEvent(SearchUiEvent.TypeSelected(SearchType.PEOPLE))
    advanceUntilIdle()

    assertEquals("Dune", viewModel.uiState.value.query)
    assertEquals(SearchType.PEOPLE, viewModel.uiState.value.selectedType)
    assertEquals(
      listOf(SearchRequest(query = "Dune", type = SearchType.PEOPLE)),
      searchRepository.requests,
    )
  }

  @Test
  fun `keyboard submit saves normalized recent query`() = runTest {
    val recentRepository = FakeRecentSearchRepository()
    val viewModel = createViewModel(recentRepository = recentRepository)
    collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(SearchUiEvent.QueryChanged("  Arrival  "))
    viewModel.onEvent(SearchUiEvent.SearchSubmitted)
    advanceUntilIdle()

    assertEquals(listOf("Arrival"), recentRepository.savedQueries)
  }

  @Test
  fun `clearing query returns to discovery without deleting recent searches`() = runTest {
    val recentRepository = FakeRecentSearchRepository(
      initial = listOf("Dune"),
    )
    val viewModel = createViewModel(recentRepository = recentRepository)
    collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(SearchUiEvent.QueryChanged("Dune"))
    advanceTimeBy(300)
    advanceUntilIdle()
    viewModel.onEvent(SearchUiEvent.ClearQuery)

    assertEquals("", viewModel.uiState.value.query)
    assertTrue(viewModel.uiState.value.resultsState is SearchResultsState.Idle)
    assertEquals(listOf("Dune"), viewModel.uiState.value.recentSearches)
  }

  private fun createViewModel(
    searchRepository: FakeSearchRepository = FakeSearchRepository(),
    recentRepository: FakeRecentSearchRepository = FakeRecentSearchRepository(),
  ) = SearchViewModel(
    searchRepository = searchRepository,
    recentSearchRepository = recentRepository,
    libraryRepository = FakeLibraryRepository(),
    savedStateHandle = SavedStateHandle(),
  )

  private fun kotlinx.coroutines.test.TestScope.collectState(viewModel: SearchViewModel) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
    }
  }

  private class FakeSearchRepository : SearchRepository {
    val requests = mutableListOf<SearchRequest>()

    override suspend fun loadDiscovery(): DataResult<SearchDiscovery> =
      DataResult.Success(
        SearchDiscovery(
          trendingQueries = listOf("Dune"),
          genres = emptyList(),
          popularPeople = emptyList(),
          hasPartialFailures = false,
        ),
      )

    override suspend fun search(request: SearchRequest): DataResult<SearchContent> {
      requests += request
      return DataResult.Success(
        SearchContent(
          query = request.query,
          selectedType = request.type,
          page = request.page,
        ),
      )
    }
  }

  private class FakeRecentSearchRepository(
    initial: List<String> = emptyList(),
  ) : RecentSearchRepository {
    private val recents = MutableStateFlow(initial)
    val savedQueries = mutableListOf<String>()

    override fun observeRecentSearches(): Flow<List<String>> = recents

    override suspend fun save(query: String) {
      savedQueries += query
      recents.value = listOf(query) + recents.value.filterNot {
        it.equals(query, ignoreCase = true)
      }
    }

    override suspend fun remove(query: String) {
      recents.value = recents.value.filterNot { it.equals(query, ignoreCase = true) }
    }

    override suspend fun clear() {
      recents.value = emptyList()
    }
  }

  private class FakeLibraryRepository : LibraryRepository {
    private val items = MutableStateFlow<List<LibraryItem>>(emptyList())

    override fun observeLibrary(): Flow<List<LibraryItem>> = items

    override suspend fun setWatchStatus(item: MediaItem, status: WatchStatus) = Unit

    override suspend fun setFavorite(item: MediaItem, isFavorite: Boolean) = Unit

    override suspend fun restore(item: LibraryItem) = Unit
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
