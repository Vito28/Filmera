package com.example.filmera.feature.discover.domain

import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaItem

data class RankedMediaItem(
  val rank: Int,
  val media: MediaItem,
) {
  init {
    require(rank > 0) { "rank must be greater than zero" }
  }
}

interface RankingRepository {
  suspend fun loadWeeklyRankings(): DataResult<List<RankedMediaItem>>
}

internal fun buildWeeklyRankings(
  pages: List<List<MediaItem>>,
  limit: Int = WEEKLY_RANKING_LIMIT,
): List<RankedMediaItem> {
  require(limit > 0) { "limit must be greater than zero" }

  return pages
    .asSequence()
    .flatten()
    .filter { media ->
      media.id > 0 &&
        media.title.isNotBlank() &&
        !media.adult
    }
    .distinctBy(MediaItem::key)
    .take(limit)
    .mapIndexed { index, media ->
      RankedMediaItem(
        rank = index + 1,
        media = media,
      )
    }
    .toList()
}

const val WEEKLY_RANKING_LIMIT = 100
