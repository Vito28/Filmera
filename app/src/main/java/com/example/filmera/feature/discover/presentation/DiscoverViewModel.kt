package com.example.filmera.feature.discover.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.discover.domain.ExploreAvailability
import com.example.filmera.feature.discover.domain.ExploreCategory
import com.example.filmera.feature.discover.domain.ExploreContent
import com.example.filmera.feature.discover.domain.ExploreContentType
import com.example.filmera.feature.discover.domain.ExploreDuration
import com.example.filmera.feature.discover.domain.ExploreFilterState
import com.example.filmera.feature.discover.domain.ExploreLanguage
import com.example.filmera.feature.discover.domain.ExploreLayoutMode
import com.example.filmera.feature.discover.domain.ExploreRegion
import com.example.filmera.feature.discover.domain.ExploreRepository
import com.example.filmera.feature.discover.domain.ExploreSort
import com.example.filmera.feature.discover.domain.ExploreTvStatus
import com.example.filmera.feature.discover.domain.ExploreYear
import com.example.filmera.feature.discover.domain.RecommendationFeedbackAction
import com.example.filmera.feature.discover.domain.RecommendationRepository
import com.example.filmera.feature.discover.domain.RecommendedContent
import com.example.filmera.feature.discover.domain.RankingRepository
import com.example.filmera.feature.preferences.domain.PreferenceRepository
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DiscoverViewModel @Inject constructor(
  private val recommendationRepository: RecommendationRepository,
  private val rankingRepository: RankingRepository,
  private val exploreRepository: ExploreRepository,
  preferenceRepository: PreferenceRepository,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
  private val _uiState = MutableStateFlow(
    DiscoverUiState(
      explore = ExploreUiState(
        filters = restoreExploreFilters(),
        layoutMode = savedEnum(
          key = SAVED_EXPLORE_LAYOUT,
          default = ExploreLayoutMode.GRID,
        ),
        gridFirstVisibleIndex = savedStateHandle[SAVED_EXPLORE_GRID_INDEX] ?: 0,
        gridFirstVisibleOffset = savedStateHandle[SAVED_EXPLORE_GRID_OFFSET] ?: 0,
        listFirstVisibleIndex = savedStateHandle[SAVED_EXPLORE_LIST_INDEX] ?: 0,
        listFirstVisibleOffset = savedStateHandle[SAVED_EXPLORE_LIST_OFFSET] ?: 0,
      ),
    ),
  )
  val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

  private var activeProfile: UserPreferenceProfile? = null
  private var loadJob: Job? = null
  private var rankingsLoadJob: Job? = null
  private var rankingsRequested = false
  private var exploreLoadJob: Job? = null
  private var exploreFilterJob: Job? = null
  private var exploreProviderJob: Job? = null
  private var exploreRequested = false
  private var refreshGeneration = 0
  private var lastRefreshAt = 0L

  init {
    viewModelScope.launch {
      preferenceRepository.observeProfile().collectLatest { profile ->
        activeProfile = profile
        if (profile?.isReadyForRecommendations == true) {
          loadRecommendations(preserveContent = false)
        } else {
          loadJob?.cancel()
          _uiState.update { it.copy(contentState = LoadState.Empty, isRefreshing = false) }
        }
      }
    }
  }

  fun onAction(action: DiscoverAction) {
    when (action) {
      is DiscoverAction.GenreSelected -> selectGenre(action.genreId)
      is DiscoverAction.FeedbackSubmitted -> submitFeedback(action)
      DiscoverAction.RefreshRequested -> refresh()
      DiscoverAction.RetryRequested -> loadRecommendations(preserveContent = false)
      DiscoverAction.RankingsOpened -> loadRankings(force = false)
      DiscoverAction.RankingsRetryRequested -> loadRankings(force = true)
      DiscoverAction.ExploreOpened -> openExplore()
      is DiscoverAction.ExploreFiltersChanged -> updateExploreFilters(action.filters)
      is DiscoverAction.ExploreLayoutChanged -> updateExploreLayout(action.layoutMode)
      is DiscoverAction.ExploreScrollChanged -> updateExploreScroll(action)
      DiscoverAction.ExploreRetryRequested -> loadExplore(preserveContent = false)
      DiscoverAction.ExploreLoadMoreRequested -> loadMoreExplore()
      DiscoverAction.ExploreProvidersRetryRequested -> loadExploreProviders()
      DiscoverAction.FeedbackMessageDismissed ->
        _uiState.update { it.copy(feedbackSaved = null) }
    }
  }

  private fun selectGenre(genreId: Int?) {
    if (_uiState.value.selectedGenreId == genreId) return
    _uiState.update { it.copy(selectedGenreId = genreId) }
    loadRecommendations(preserveContent = true)
  }

  private fun refresh() {
    val now = System.currentTimeMillis()
    if (now - lastRefreshAt < REFRESH_COOLDOWN_MILLIS || _uiState.value.isRefreshing) return
    lastRefreshAt = now
    refreshGeneration += 1
    loadRecommendations(preserveContent = true)
  }

  private fun submitFeedback(action: DiscoverAction.FeedbackSubmitted) {
    viewModelScope.launch {
      try {
        recommendationRepository.recordFeedback(action.media, action.feedback)
        _uiState.update { state ->
          state.copy(
            contentState = state.contentState.removeMediaIfNeeded(
              media = action.media,
              feedback = action.feedback,
            ),
            feedbackSaved = action.feedback,
          )
        }
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        _uiState.update {
          it.copy(contentState = LoadState.Error(AppError.Unknown))
        }
      }
    }
  }

  private fun loadRecommendations(preserveContent: Boolean) {
    val profile = activeProfile ?: return
    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      val previous = _uiState.value.contentState
      if (preserveContent && previous is LoadState.Success) {
        _uiState.update { it.copy(isRefreshing = true) }
      } else {
        _uiState.update { it.copy(contentState = LoadState.Loading, isRefreshing = false) }
      }

      try {
        when (
          val result = recommendationRepository.loadRecommendations(
            profile = profile,
            selectedGenreId = _uiState.value.selectedGenreId,
            refreshGeneration = refreshGeneration,
          )
        ) {
          is DataResult.Success -> {
            _uiState.update {
              it.copy(
                contentState = if (result.value.hasContent) {
                  LoadState.Success(result.value)
                } else {
                  LoadState.Empty
                },
                isRefreshing = false,
              )
            }
          }
          is DataResult.Error -> {
            _uiState.update {
              it.copy(
                contentState = if (preserveContent && previous is LoadState.Success) {
                  previous
                } else {
                  LoadState.Error(result.error)
                },
                isRefreshing = false,
              )
            }
          }
        }
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        _uiState.update {
          it.copy(
            contentState = if (preserveContent && previous is LoadState.Success) {
              previous
            } else {
              LoadState.Error(AppError.Unknown)
            },
            isRefreshing = false,
          )
        }
      }
    }
  }

  private fun loadRankings(force: Boolean) {
    if (!force && rankingsRequested) return

    rankingsRequested = true
    rankingsLoadJob?.cancel()
    rankingsLoadJob = viewModelScope.launch {
      _uiState.update { it.copy(rankingsState = LoadState.Loading) }
      val nextState = try {
        when (val result = rankingRepository.loadWeeklyRankings()) {
          is DataResult.Success -> if (result.value.isEmpty()) {
            LoadState.Empty
          } else {
            LoadState.Success(result.value)
          }
          is DataResult.Error -> LoadState.Error(result.error)
        }
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        LoadState.Error(AppError.Unknown)
      }
      _uiState.update { it.copy(rankingsState = nextState) }
    }
  }

  private fun openExplore() {
    if (exploreRequested) return
    exploreRequested = true
    loadExploreProviders()
    loadExplore(preserveContent = false)
  }

  private fun updateExploreFilters(filters: ExploreFilterState) {
    if (_uiState.value.explore.filters == filters) return

    persistExploreFilters(filters)
    exploreLoadJob?.cancel()
    exploreFilterJob?.cancel()
    _uiState.update { state ->
      state.copy(
        explore = state.explore.copy(
          filters = filters,
          isRefreshing = state.explore.contentState is LoadState.Success,
          refreshError = null,
          paginationError = null,
          gridFirstVisibleIndex = 0,
          gridFirstVisibleOffset = 0,
          listFirstVisibleIndex = 0,
          listFirstVisibleOffset = 0,
        ),
      )
    }
    exploreFilterJob = viewModelScope.launch {
      delay(EXPLORE_FILTER_DEBOUNCE_MILLIS)
      loadExplore(preserveContent = true)
    }
    resetExploreScrollPersistence()
  }

  private fun updateExploreLayout(layoutMode: ExploreLayoutMode) {
    if (_uiState.value.explore.layoutMode == layoutMode) return
    savedStateHandle[SAVED_EXPLORE_LAYOUT] = layoutMode.name
    _uiState.update { state ->
      state.copy(explore = state.explore.copy(layoutMode = layoutMode))
    }
  }

  private fun updateExploreScroll(action: DiscoverAction.ExploreScrollChanged) {
    val index = action.firstVisibleIndex.coerceAtLeast(0)
    val offset = action.firstVisibleOffset.coerceAtLeast(0)
    _uiState.update { state ->
      state.copy(
        explore = when (action.layoutMode) {
          ExploreLayoutMode.GRID -> state.explore.copy(
            gridFirstVisibleIndex = index,
            gridFirstVisibleOffset = offset,
          )
          ExploreLayoutMode.LIST -> state.explore.copy(
            listFirstVisibleIndex = index,
            listFirstVisibleOffset = offset,
          )
        },
      )
    }
    when (action.layoutMode) {
      ExploreLayoutMode.GRID -> {
        savedStateHandle[SAVED_EXPLORE_GRID_INDEX] = index
        savedStateHandle[SAVED_EXPLORE_GRID_OFFSET] = offset
      }
      ExploreLayoutMode.LIST -> {
        savedStateHandle[SAVED_EXPLORE_LIST_INDEX] = index
        savedStateHandle[SAVED_EXPLORE_LIST_OFFSET] = offset
      }
    }
  }

  private fun loadExplore(preserveContent: Boolean) {
    exploreRequested = true
    exploreLoadJob?.cancel()
    exploreLoadJob = viewModelScope.launch {
      val previous = _uiState.value.explore.contentState
      _uiState.update { state ->
        state.copy(
          explore = state.explore.copy(
            contentState = if (preserveContent && previous is LoadState.Success) {
              previous
            } else {
              LoadState.Loading
            },
            isRefreshing = preserveContent && previous is LoadState.Success,
            isLoadingMore = false,
            refreshError = null,
            paginationError = null,
          ),
        )
      }

      try {
        when (
          val result = exploreRepository.loadExplore(
            filters = _uiState.value.explore.filters,
            page = FIRST_EXPLORE_PAGE,
          )
        ) {
          is DataResult.Success -> {
            val page = result.value
            _uiState.update { state ->
              state.copy(
                explore = state.explore.copy(
                  contentState = if (page.items.isEmpty()) {
                    LoadState.Empty
                  } else {
                    LoadState.Success(
                      ExploreContent(
                        items = page.items,
                        currentPage = page.page,
                        totalResults = page.totalResults,
                        hasMore = page.hasMore,
                        isPartial = page.isPartial,
                      ),
                    )
                  },
                  isRefreshing = false,
                ),
              )
            }
          }
          is DataResult.Error -> {
            _uiState.update { state ->
              state.copy(
                explore = state.explore.copy(
                  contentState = if (preserveContent && previous is LoadState.Success) {
                    previous
                  } else {
                    LoadState.Error(result.error)
                  },
                  isRefreshing = false,
                  refreshError = result.error.takeIf {
                    preserveContent && previous is LoadState.Success
                  },
                ),
              )
            }
          }
        }
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        _uiState.update { state ->
          state.copy(
            explore = state.explore.copy(
              contentState = if (preserveContent && previous is LoadState.Success) {
                previous
              } else {
                LoadState.Error(AppError.Unknown)
              },
              isRefreshing = false,
              refreshError = AppError.Unknown.takeIf {
                preserveContent && previous is LoadState.Success
              },
            ),
          )
        }
      }
    }
  }

  private fun loadMoreExplore() {
    val exploreState = _uiState.value.explore
    val content = (exploreState.contentState as? LoadState.Success)?.value ?: return
    if (!content.hasMore || exploreState.isLoadingMore) return

    exploreLoadJob?.cancel()
    exploreLoadJob = viewModelScope.launch {
      _uiState.update { state ->
        state.copy(
          explore = state.explore.copy(
            isLoadingMore = true,
            paginationError = null,
          ),
        )
      }
      try {
        when (
          val result = exploreRepository.loadExplore(
            filters = _uiState.value.explore.filters,
            page = content.currentPage + 1,
          )
        ) {
          is DataResult.Success -> {
            val page = result.value
            val mergedItems = (content.items + page.items)
              .distinctBy { it.media.key }
            _uiState.update { state ->
              state.copy(
                explore = state.explore.copy(
                  contentState = LoadState.Success(
                    content.copy(
                      items = mergedItems,
                      currentPage = page.page,
                      totalResults = page.totalResults,
                      hasMore = page.hasMore,
                      isPartial = content.isPartial || page.isPartial,
                    ),
                  ),
                  isLoadingMore = false,
                ),
              )
            }
          }
          is DataResult.Error -> {
            _uiState.update { state ->
              state.copy(
                explore = state.explore.copy(
                  isLoadingMore = false,
                  paginationError = result.error,
                ),
              )
            }
          }
        }
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        _uiState.update { state ->
          state.copy(
            explore = state.explore.copy(
              isLoadingMore = false,
              paginationError = AppError.Unknown,
            ),
          )
        }
      }
    }
  }

  private fun loadExploreProviders() {
    exploreProviderJob?.cancel()
    exploreProviderJob = viewModelScope.launch {
      _uiState.update { state ->
        state.copy(
          explore = state.explore.copy(providerState = LoadState.Loading),
        )
      }
      val nextState = try {
        when (val result = exploreRepository.loadProviderOptions()) {
          is DataResult.Success -> if (result.value.isEmpty()) {
            LoadState.Empty
          } else {
            LoadState.Success(result.value)
          }
          is DataResult.Error -> LoadState.Error(result.error)
        }
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        LoadState.Error(AppError.Unknown)
      }
      _uiState.update { state ->
        state.copy(explore = state.explore.copy(providerState = nextState))
      }
    }
  }

  private fun restoreExploreFilters(): ExploreFilterState =
    ExploreFilterState(
      sort = savedEnum(SAVED_EXPLORE_SORT, ExploreSort.HOT),
      contentType = savedEnum(SAVED_EXPLORE_TYPE, ExploreContentType.ALL),
      categories = savedStateHandle
        .get<ArrayList<String>>(SAVED_EXPLORE_CATEGORIES)
        .orEmpty()
        .mapNotNull { name -> enumValueOrNull<ExploreCategory>(name) }
        .toSet(),
      region = savedEnum(SAVED_EXPLORE_REGION, ExploreRegion.ALL),
      year = savedEnum(SAVED_EXPLORE_YEAR, ExploreYear.ALL),
      language = savedEnum(SAVED_EXPLORE_LANGUAGE, ExploreLanguage.ALL),
      providerIds = savedStateHandle
        .get<IntArray>(SAVED_EXPLORE_PROVIDERS)
        ?.toSet()
        .orEmpty(),
      availability = savedEnum(SAVED_EXPLORE_AVAILABILITY, ExploreAvailability.ANY),
      minimumRating = savedStateHandle[SAVED_EXPLORE_RATING],
      minimumVoteCount = savedStateHandle[SAVED_EXPLORE_VOTES],
      duration = savedEnum(SAVED_EXPLORE_DURATION, ExploreDuration.ANY),
      tvStatus = savedEnum(SAVED_EXPLORE_STATUS, ExploreTvStatus.ANY),
    )

  private fun persistExploreFilters(filters: ExploreFilterState) {
    savedStateHandle[SAVED_EXPLORE_SORT] = filters.sort.name
    savedStateHandle[SAVED_EXPLORE_TYPE] = filters.contentType.name
    savedStateHandle[SAVED_EXPLORE_CATEGORIES] =
      ArrayList(filters.categories.map(ExploreCategory::name))
    savedStateHandle[SAVED_EXPLORE_REGION] = filters.region.name
    savedStateHandle[SAVED_EXPLORE_YEAR] = filters.year.name
    savedStateHandle[SAVED_EXPLORE_LANGUAGE] = filters.language.name
    savedStateHandle[SAVED_EXPLORE_PROVIDERS] = filters.providerIds.toIntArray()
    savedStateHandle[SAVED_EXPLORE_AVAILABILITY] = filters.availability.name
    savedStateHandle[SAVED_EXPLORE_RATING] = filters.minimumRating
    savedStateHandle[SAVED_EXPLORE_VOTES] = filters.minimumVoteCount
    savedStateHandle[SAVED_EXPLORE_DURATION] = filters.duration.name
    savedStateHandle[SAVED_EXPLORE_STATUS] = filters.tvStatus.name
  }

  private fun resetExploreScrollPersistence() {
    savedStateHandle[SAVED_EXPLORE_GRID_INDEX] = 0
    savedStateHandle[SAVED_EXPLORE_GRID_OFFSET] = 0
    savedStateHandle[SAVED_EXPLORE_LIST_INDEX] = 0
    savedStateHandle[SAVED_EXPLORE_LIST_OFFSET] = 0
  }

  private inline fun <reified T : Enum<T>> savedEnum(
    key: String,
    default: T,
  ): T =
    savedStateHandle.get<String>(key)
      ?.let(::enumValueOrNull)
      ?: default

  private inline fun <reified T : Enum<T>> enumValueOrNull(name: String): T? =
    enumValues<T>().firstOrNull { it.name == name }

  private fun LoadState<RecommendedContent>.removeMediaIfNeeded(
    media: com.example.filmera.core.model.MediaItem,
    feedback: RecommendationFeedbackAction,
  ): LoadState<RecommendedContent> {
    if (
      feedback !in setOf(
        RecommendationFeedbackAction.NOT_INTERESTED,
        RecommendationFeedbackAction.ALREADY_WATCHED,
        RecommendationFeedbackAction.HIDE,
      )
    ) {
      return this
    }
    val content = (this as? LoadState.Success)?.value ?: return this
    return LoadState.Success(
      content.copy(
        heroItems = content.heroItems.filterNot { it.media.key == media.key },
        sections = content.sections.map { section ->
          section.copy(items = section.items.filterNot { it.media.key == media.key })
        },
      ),
    )
  }

  private companion object {
    const val REFRESH_COOLDOWN_MILLIS = 8_000L
    const val EXPLORE_FILTER_DEBOUNCE_MILLIS = 300L
    const val FIRST_EXPLORE_PAGE = 1
    const val SAVED_EXPLORE_SORT = "explore.sort"
    const val SAVED_EXPLORE_TYPE = "explore.type"
    const val SAVED_EXPLORE_CATEGORIES = "explore.categories"
    const val SAVED_EXPLORE_REGION = "explore.region"
    const val SAVED_EXPLORE_YEAR = "explore.year"
    const val SAVED_EXPLORE_LANGUAGE = "explore.language"
    const val SAVED_EXPLORE_PROVIDERS = "explore.providers"
    const val SAVED_EXPLORE_AVAILABILITY = "explore.availability"
    const val SAVED_EXPLORE_RATING = "explore.rating"
    const val SAVED_EXPLORE_VOTES = "explore.votes"
    const val SAVED_EXPLORE_DURATION = "explore.duration"
    const val SAVED_EXPLORE_STATUS = "explore.status"
    const val SAVED_EXPLORE_LAYOUT = "explore.layout"
    const val SAVED_EXPLORE_GRID_INDEX = "explore.grid.index"
    const val SAVED_EXPLORE_GRID_OFFSET = "explore.grid.offset"
    const val SAVED_EXPLORE_LIST_INDEX = "explore.list.index"
    const val SAVED_EXPLORE_LIST_OFFSET = "explore.list.offset"
  }
}
