package com.example.filmera.feature.discover.data

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.network.TmdbApiDefaults
import com.example.filmera.core.network.TmdbCatalogApi
import com.example.filmera.core.network.TmdbConfig
import com.example.filmera.core.network.dto.CatalogResponseDto
import com.example.filmera.core.network.dto.MultiSearchItemDto
import com.example.filmera.core.network.mapper.toMediaItemOrNull
import com.example.filmera.core.network.safeNetworkCall
import com.example.filmera.feature.discover.domain.RankedMediaItem
import com.example.filmera.feature.discover.domain.RankingRepository
import com.example.filmera.feature.discover.domain.WEEKLY_RANKING_LIMIT
import com.example.filmera.feature.discover.domain.buildWeeklyRankings
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

class TmdbRankingRepository @Inject constructor(
  private val catalogApi: TmdbCatalogApi,
  private val config: TmdbConfig,
) : RankingRepository {

  override suspend fun loadWeeklyRankings(): DataResult<List<RankedMediaItem>> {
    if (!config.isConfigured) return DataResult.Error(AppError.MissingApiToken)

    val firstPageResult = loadPage(FIRST_PAGE)
    val firstPage = when (firstPageResult) {
      is DataResult.Success -> firstPageResult.value
      is DataResult.Error -> return firstPageResult
    }
    val lastPage = firstPage.totalPages
      .coerceAtLeast(FIRST_PAGE)
      .coerceAtMost(MAXIMUM_PAGE_REQUESTS)
    val remainingResults = if (lastPage > FIRST_PAGE) {
      supervisorScope {
        (FIRST_PAGE + 1..lastPage)
          .map { page ->
            async { page to loadPage(page) }
          }
          .awaitAll()
      }
    } else {
      emptyList()
    }
    val orderedResults = listOf(FIRST_PAGE to firstPageResult) +
      remainingResults.sortedBy { (page, _) -> page }
    val rankingPages = orderedResults.mapNotNull { (_, result) ->
      (result as? DataResult.Success)
        ?.value
        ?.results
        ?.mapNotNull(MultiSearchItemDto::toMediaItemOrNull)
    }
    val rankings = buildWeeklyRankings(rankingPages)

    if (rankings.size < WEEKLY_RANKING_LIMIT) {
      orderedResults
        .asSequence()
        .map { (_, result) -> result }
        .filterIsInstance<DataResult.Error>()
        .firstOrNull()
        ?.let { return it }
    }

    return if (rankings.isEmpty()) {
      DataResult.Error(AppError.NotFound)
    } else {
      DataResult.Success(rankings)
    }
  }

  private suspend fun loadPage(
    page: Int,
  ): DataResult<CatalogResponseDto<MultiSearchItemDto>> =
    safeNetworkCall {
      catalogApi.getTrendingAll(
        timeWindow = WEEK_TIME_WINDOW,
        language = TmdbApiDefaults.LANGUAGE,
        page = page,
      )
    }

  private companion object {
    const val FIRST_PAGE = 1
    const val MAXIMUM_PAGE_REQUESTS = 8
    const val WEEK_TIME_WINDOW = "week"
  }
}
