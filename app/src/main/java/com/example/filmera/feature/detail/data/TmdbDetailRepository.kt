package com.example.filmera.feature.detail.data

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.common.map
import com.example.filmera.core.model.Credits
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.model.MediaVideo
import com.example.filmera.core.model.WatchProvider
import com.example.filmera.core.model.WatchProviderResult
import com.example.filmera.core.network.TmdbAppendToResponse
import com.example.filmera.core.network.TmdbConfig
import com.example.filmera.core.network.TmdbMovieApi
import com.example.filmera.core.network.TmdbTvSeriesApi
import com.example.filmera.core.network.dto.MovieDetailsDto
import com.example.filmera.core.network.dto.TvDetailsDto
import com.example.filmera.core.network.dto.WatchProviderDto
import com.example.filmera.core.network.dto.WatchProviderRegionDto
import com.example.filmera.core.network.mapper.toDomain
import com.example.filmera.core.network.safeNetworkCall
import com.example.filmera.feature.detail.domain.DetailContent
import com.example.filmera.feature.detail.domain.DetailRepository
import javax.inject.Inject

class TmdbDetailRepository @Inject constructor(
  private val movieApi: TmdbMovieApi,
  private val tvSeriesApi: TmdbTvSeriesApi,
  private val config: TmdbConfig,
) : DetailRepository {

  override suspend fun loadDetails(key: MediaKey): DataResult<DetailContent> {
    if (!config.isConfigured) return DataResult.Error(AppError.MissingApiToken)

    return when (key.type) {
      MediaType.MOVIE -> safeNetworkCall {
        movieApi.getMovieDetails(
          movieId = key.id,
          appendToResponse = TmdbAppendToResponse.MOVIE_DETAILS,
        )
      }.map { it.toDetailContent() }

      MediaType.TV_SHOW -> safeNetworkCall {
        tvSeriesApi.getTvDetails(
          seriesId = key.id,
          appendToResponse = TmdbAppendToResponse.TV_DETAILS,
        )
      }.map { it.toDetailContent() }
    }
  }

  private fun MovieDetailsDto.toDetailContent(): DetailContent =
    videos
      ?.results
      .orEmpty()
      .map { it.toDomain() }
      .validVideos()
      .let { mediaVideos ->
        DetailContent(
          details = toDomain(),
          credits = credits?.toDomain() ?: Credits(emptyList(), emptyList()),
          trailer = mediaVideos.selectTrailer(),
          videos = mediaVideos,
          watchProviders = watchProviders.toDomain(),
          recommendations = recommendations
            ?.results
            .orEmpty()
            .map { it.toDomain() }
            .validRecommendations(),
          hasPartialFailures = listOf(
            credits,
            videos,
            recommendations,
            watchProviders,
          ).any { it == null },
        )
      }

  private fun TvDetailsDto.toDetailContent(): DetailContent =
    videos
      ?.results
      .orEmpty()
      .map { it.toDomain() }
      .validVideos()
      .let { mediaVideos ->
        DetailContent(
          details = toDomain(),
          credits = aggregateCredits?.toDomain()
            ?: credits?.toDomain()
            ?: Credits(emptyList(), emptyList()),
          trailer = mediaVideos.selectTrailer(),
          videos = mediaVideos,
          watchProviders = watchProviders.toDomain(),
          recommendations = recommendations
            ?.results
            .orEmpty()
            .map { it.toDomain() }
            .validRecommendations(),
          hasPartialFailures = listOf(
            aggregateCredits ?: credits,
            videos,
            recommendations,
            watchProviders,
          ).any { it == null },
        )
      }

  private fun List<MediaVideo>.validVideos(): List<MediaVideo> =
    asSequence()
      .filter { video ->
        video.key.isNotBlank() &&
          video.site.equals("YouTube", ignoreCase = true)
      }
      .distinctBy(MediaVideo::key)
      .sortedWith(
        compareByDescending<MediaVideo>(MediaVideo::isOfficial)
          .thenBy { video -> video.type.videoTypePriority() }
          .thenByDescending(MediaVideo::publishedAt),
      )
      .toList()

  private fun List<MediaVideo>.selectTrailer(): MediaVideo? =
    firstOrNull { it.type.equals("Trailer", true) && it.isOfficial }
      ?: firstOrNull { it.type.equals("Trailer", true) }
      ?: firstOrNull()

  private fun List<MediaItem>.validRecommendations(): List<MediaItem> =
    filter { it.id > 0 && it.title.isNotBlank() }

  private fun com.example.filmera.core.network.dto.WatchProvidersResponseDto?.toDomain():
    Map<String, WatchProviderResult> =
    this
      ?.results
      .orEmpty()
      .mapNotNull { (regionCode, result) ->
        regionCode
          .takeIf(String::isNotBlank)
          ?.uppercase()
          ?.let { normalizedCode ->
            normalizedCode to result.toDomain(normalizedCode)
          }
      }
      .toMap()

  private fun WatchProviderRegionDto.toDomain(regionCode: String) =
    WatchProviderResult(
      regionCode = regionCode,
      link = link?.takeIf(String::isNotBlank),
      stream = streaming.toDomain(),
      free = free.toDomain(),
      ads = ads.toDomain(),
      rent = rent.toDomain(),
      buy = buy.toDomain(),
    )

  private fun List<WatchProviderDto>?.toDomain(): List<WatchProvider> =
    orEmpty()
      .asSequence()
      .filter { provider -> provider.providerId > 0 && provider.providerName.isNotBlank() }
      .distinctBy(WatchProviderDto::providerId)
      .sortedBy(WatchProviderDto::displayPriority)
      .map { provider ->
        WatchProvider(
          id = provider.providerId,
          name = provider.providerName,
          logoPath = provider.logoPath,
          displayPriority = provider.displayPriority,
        )
      }
      .toList()

  private fun String.videoTypePriority(): Int =
    when {
      equals("Trailer", true) -> 0
      equals("Teaser", true) -> 1
      equals("Featurette", true) -> 2
      equals("Clip", true) -> 3
      equals("Behind the Scenes", true) -> 4
      equals("Interview", true) -> 5
      else -> 6
    }
}
