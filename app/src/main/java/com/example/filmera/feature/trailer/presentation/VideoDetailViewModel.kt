package com.example.filmera.feature.trailer.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.model.MediaVideo
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.detail.domain.DetailContent
import com.example.filmera.feature.detail.domain.DetailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VideoDetailUiState(
  val mediaKey: MediaKey? = null,
  val requestedVideoKey: String = "",
  val activeVideoKey: String = "",
  val selectedRegionCode: String = DEFAULT_WATCH_REGION,
  val contentState: LoadState<DetailContent> = LoadState.Loading,
) {
  val activeVideo: MediaVideo?
    get() {
      val content = (contentState as? LoadState.Success)?.value
      return content
        ?.videos
        ?.firstOrNull { it.key == activeVideoKey }
        ?: content?.trailer
    }

  companion object {
    const val DEFAULT_WATCH_REGION = "ID"
  }
}

sealed interface VideoDetailAction {
  data object Retried : VideoDetailAction
  data class VideoSelected(val videoKey: String) : VideoDetailAction
  data class RegionSelected(val regionCode: String) : VideoDetailAction
}

@HiltViewModel
class VideoDetailViewModel @Inject constructor(
  private val detailRepository: DetailRepository,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  private val requestedVideoKey = savedStateHandle
    .get<String>(VIDEO_KEY_ARGUMENT)
    .orEmpty()
  private val mediaKey = MediaType
    .fromRoute(savedStateHandle[MEDIA_TYPE_ARGUMENT])
    ?.let { type ->
      savedStateHandle
        .get<Int>(MEDIA_ID_ARGUMENT)
        ?.takeIf { it > 0 }
        ?.let { id -> MediaKey(id = id, type = type) }
    }
  private val _uiState = MutableStateFlow(
    VideoDetailUiState(
      mediaKey = mediaKey,
      requestedVideoKey = requestedVideoKey,
      activeVideoKey = requestedVideoKey,
    ),
  )
  val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()

  init {
    load()
  }

  fun onAction(action: VideoDetailAction) {
    when (action) {
      VideoDetailAction.Retried -> load()
      is VideoDetailAction.VideoSelected -> {
        if (action.videoKey.isNotBlank()) {
          _uiState.update { it.copy(activeVideoKey = action.videoKey) }
        }
      }
      is VideoDetailAction.RegionSelected -> {
        if (action.regionCode.isNotBlank()) {
          _uiState.update {
            it.copy(selectedRegionCode = action.regionCode.uppercase())
          }
        }
      }
    }
  }

  private fun load() {
    val key = mediaKey
    if (key == null || requestedVideoKey.isBlank()) {
      _uiState.update { it.copy(contentState = LoadState.Error(AppError.NotFound)) }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(contentState = LoadState.Loading) }
      try {
        when (val result = detailRepository.loadDetails(key)) {
          is DataResult.Success -> {
            val content = result.value.withRequestedVideo(requestedVideoKey)
            _uiState.update {
              it.copy(
                activeVideoKey = content.videos
                  .firstOrNull { video -> video.key == requestedVideoKey }
                  ?.key
                  ?: content.trailer?.key
                  ?: requestedVideoKey,
                contentState = LoadState.Success(content),
              )
            }
          }
          is DataResult.Error -> {
            _uiState.update { it.copy(contentState = LoadState.Error(result.error)) }
          }
        }
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        _uiState.update { it.copy(contentState = LoadState.Error(AppError.Unknown)) }
      }
    }
  }

  private fun DetailContent.withRequestedVideo(videoKey: String): DetailContent {
    if (videos.any { it.key == videoKey }) return this
    val fallback = MediaVideo(
      key = videoKey,
      name = trailer?.name.orEmpty().ifBlank { details.title },
      site = "YouTube",
      type = trailer?.type.orEmpty().ifBlank { "Trailer" },
      isOfficial = trailer?.isOfficial == true,
    )
    return copy(
      trailer = trailer ?: fallback,
      videos = listOf(fallback) + videos,
    )
  }

  companion object {
    const val VIDEO_KEY_ARGUMENT = "videoKey"
    const val MEDIA_TYPE_ARGUMENT = "mediaType"
    const val MEDIA_ID_ARGUMENT = "mediaId"
  }
}
