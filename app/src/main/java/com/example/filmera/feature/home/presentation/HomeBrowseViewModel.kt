package com.example.filmera.feature.home.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.home.domain.AnimeTopic
import com.example.filmera.feature.home.domain.HomeBrowseKind
import com.example.filmera.feature.home.domain.HomeChannel
import com.example.filmera.feature.home.domain.HomeCollection
import com.example.filmera.feature.home.domain.HomeContent
import com.example.filmera.feature.home.domain.HomeFeedKey
import com.example.filmera.feature.home.domain.HomeGenre
import com.example.filmera.feature.home.domain.HomePerson
import com.example.filmera.feature.home.domain.HomeRepository
import com.example.filmera.feature.home.domain.HomeSectionType
import com.example.filmera.feature.home.domain.HomeSpotlight
import com.example.filmera.feature.home.domain.HomeTrailer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeBrowseContent(
  val mediaItems: List<MediaItem> = emptyList(),
  val people: List<HomePerson> = emptyList(),
  val spotlights: List<HomeSpotlight> = emptyList(),
  val trailers: List<HomeTrailer> = emptyList(),
  val genres: List<HomeGenre> = emptyList(),
  val collections: List<HomeCollection> = emptyList(),
) {
  fun hasItems(kind: HomeBrowseKind): Boolean =
    when (kind) {
      HomeBrowseKind.GENRES -> genres.isNotEmpty()
      HomeBrowseKind.PEOPLE -> people.isNotEmpty()
      HomeBrowseKind.SPOTLIGHTS -> spotlights.isNotEmpty()
      HomeBrowseKind.TRAILERS -> trailers.isNotEmpty()
      HomeBrowseKind.COLLECTIONS -> collections.isNotEmpty()
      else -> mediaItems.isNotEmpty()
    }
}

data class HomeBrowseUiState(
  val kind: HomeBrowseKind = HomeBrowseKind.TRENDING,
  val feedKey: HomeFeedKey = HomeFeedKey(HomeChannel.FOR_YOU),
  val contentState: LoadState<HomeBrowseContent> = LoadState.Loading,
  val page: Int = 1,
  val totalPages: Int = 1,
  val isLoadingMore: Boolean = false,
  val paginationError: AppError? = null,
) {
  val canLoadMore: Boolean
    get() = kind.isMediaList && !isLoadingMore && page < totalPages
}

sealed interface HomeBrowseAction {
  data object Retried : HomeBrowseAction
  data object LoadMore : HomeBrowseAction
}

@HiltViewModel
class HomeBrowseViewModel @Inject constructor(
  private val homeRepository: HomeRepository,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  private val kind = savedStateHandle.get<String>(BROWSE_KIND_ARGUMENT).toBrowseKind()
  private val feedKey = HomeFeedKey(
    channel = savedStateHandle.get<String>(CHANNEL_ARGUMENT).toHomeChannel(),
    animeTopic = savedStateHandle.get<String>(ANIME_TOPIC_ARGUMENT).toAnimeTopic(),
  ).let { key -> key.copy(animeTopic = key.normalizedAnimeTopic) }
  private val _uiState = MutableStateFlow(
    HomeBrowseUiState(
      kind = kind,
      feedKey = feedKey,
    ),
  )
  val uiState: StateFlow<HomeBrowseUiState> = _uiState.asStateFlow()

  init {
    loadInitial()
  }

  fun onAction(action: HomeBrowseAction) {
    when (action) {
      HomeBrowseAction.Retried -> loadInitial()
      HomeBrowseAction.LoadMore -> loadMore()
    }
  }

  private fun loadInitial() {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          contentState = LoadState.Loading,
          page = 1,
          totalPages = 1,
          isLoadingMore = false,
          paginationError = null,
        )
      }
      try {
        if (kind.isMediaList) {
          loadMediaPage(page = 1, append = false)
        } else {
          when (val result = homeRepository.loadHomeContent(feedKey)) {
            is DataResult.Success -> {
              val content = HomeBrowseContent(
                people = result.value.people,
                spotlights = result.value.spotlights,
                trailers = result.value.trailers,
                genres = result.value.allGenres,
                collections = result.value.collections,
              )
              val supportingError = result.value.supportingError(kind)
              _uiState.update {
                it.copy(
                  contentState = when {
                    content.hasItems(kind) -> LoadState.Success(content)
                    supportingError != null -> LoadState.Error(supportingError)
                    else -> LoadState.Empty
                  },
                )
              }
            }
            is DataResult.Error -> {
              _uiState.update { it.copy(contentState = LoadState.Error(result.error)) }
            }
          }
        }
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        _uiState.update { it.copy(contentState = LoadState.Error(AppError.Unknown)) }
      }
    }
  }

  private fun loadMore() {
    if (!_uiState.value.canLoadMore) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingMore = true, paginationError = null) }
      try {
        loadMediaPage(page = _uiState.value.page + 1, append = true)
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        _uiState.update {
          it.copy(
            isLoadingMore = false,
            paginationError = AppError.Unknown,
          )
        }
      }
    }
  }

  private suspend fun loadMediaPage(
    page: Int,
    append: Boolean,
  ) {
    val sectionType = requireNotNull(kind.sectionType)
    when (
      val result = homeRepository.loadSectionPage(
        feedKey = feedKey,
        sectionType = sectionType,
        page = page,
      )
    ) {
      is DataResult.Success -> {
        val previous = if (append) {
          (_uiState.value.contentState as? LoadState.Success)
            ?.value
            ?.mediaItems
            .orEmpty()
        } else {
          emptyList()
        }
        val content = HomeBrowseContent(
          mediaItems = (previous + result.value.items).distinctBy(MediaItem::key),
        )
        _uiState.update {
          it.copy(
            contentState = if (content.mediaItems.isEmpty()) {
              LoadState.Empty
            } else {
              LoadState.Success(content)
            },
            page = result.value.page,
            totalPages = result.value.totalPages.coerceAtLeast(result.value.page),
            isLoadingMore = false,
            paginationError = null,
          )
        }
      }
      is DataResult.Error -> {
        _uiState.update { current ->
          if (append && current.contentState is LoadState.Success) {
            current.copy(
              isLoadingMore = false,
              paginationError = result.error,
            )
          } else {
            current.copy(
              contentState = LoadState.Error(result.error),
              isLoadingMore = false,
            )
          }
        }
      }
    }
  }

  private fun String?.toBrowseKind(): HomeBrowseKind =
    HomeBrowseKind.entries.firstOrNull { it.name == this } ?: HomeBrowseKind.TRENDING

  private fun String?.toHomeChannel(): HomeChannel =
    HomeChannel.entries.firstOrNull { it.name == this } ?: HomeChannel.FOR_YOU

  private fun String?.toAnimeTopic(): AnimeTopic =
    AnimeTopic.entries.firstOrNull { it.name == this } ?: AnimeTopic.ALL

  companion object {
    const val BROWSE_KIND_ARGUMENT = "browseKind"
    const val CHANNEL_ARGUMENT = "channel"
    const val ANIME_TOPIC_ARGUMENT = "animeTopic"
  }
}

private fun HomeContent.supportingError(kind: HomeBrowseKind): AppError? =
  when (kind) {
    HomeBrowseKind.GENRES -> genresError
    HomeBrowseKind.PEOPLE -> peopleError
    HomeBrowseKind.TRAILERS -> trailersError
    HomeBrowseKind.COLLECTIONS -> collectionsError
    HomeBrowseKind.SPOTLIGHTS ->
      section(HomeSectionType.TRENDING)?.error
        ?: section(HomeSectionType.EDITORS_PICKS)?.error
    else -> null
  }
