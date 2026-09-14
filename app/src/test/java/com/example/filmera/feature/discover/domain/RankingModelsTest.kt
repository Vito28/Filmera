package com.example.filmera.feature.discover.domain

import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RankingModelsTest {

  @Test
  fun `weekly ranking keeps API order and assigns one through one hundred`() {
    val pages = (1..120)
      .map(::sampleMedia)
      .chunked(20)

    val rankings = buildWeeklyRankings(pages)

    assertEquals(100, rankings.size)
    assertEquals((1..100).toList(), rankings.map(RankedMediaItem::rank))
    assertEquals((1..100).toList(), rankings.map { it.media.id })
  }

  @Test
  fun `invalid and duplicate titles are removed without leaving rank gaps`() {
    val duplicate = sampleMedia(1)
    val adult = sampleMedia(2).copy(adult = true)
    val missingTitle = sampleMedia(3).copy(title = "")

    val rankings = buildWeeklyRankings(
      pages = listOf(
        listOf(duplicate, adult, missingTitle),
        listOf(duplicate, sampleMedia(4), sampleMedia(5)),
      ),
    )

    assertEquals(listOf(1, 2, 3), rankings.map(RankedMediaItem::rank))
    assertEquals(listOf(1, 4, 5), rankings.map { it.media.id })
    assertFalse(rankings.any { it.media.adult })
  }

  private fun sampleMedia(id: Int): MediaItem =
    MediaItem(
      id = id,
      type = if (id % 2 == 0) MediaType.TV_SHOW else MediaType.MOVIE,
      title = "Title $id",
      originalTitle = "Title $id",
      overview = "Overview",
      posterPath = "/poster-$id.jpg",
      backdropPath = "/backdrop-$id.jpg",
      releaseDate = "2026-07-30",
      voteAverage = 8.0,
      voteCount = 1_000,
      popularity = 100.0,
      adult = false,
      originalLanguage = "en",
      genreIds = listOf(18),
    )
}
