package com.example.filmera.feature.discover.domain

import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreRankingEngineTest {
  private val engine = ExploreRankingEngine(currentDate = LocalDate.of(2026, 7, 30))

  @Test
  fun `top rated uses vote confidence instead of raw rating`() {
    val lowConfidence = candidate(
      id = 1,
      rating = 9.8,
      votes = 3,
    )
    val established = candidate(
      id = 2,
      rating = 8.4,
      votes = 8_000,
    )
    val catalogBaseline = (3..22).map { id ->
      candidate(
        id = id,
        rating = 6.5,
        votes = 1_000,
      )
    }

    val ranked = engine.rank(
      candidates = listOf(lowConfidence, established) + catalogBaseline,
      sort = ExploreSort.TOP_RATED,
    )

    assertEquals(2, ranked.first().media.id)
    assertTrue(
      ranked.first().score >
        ranked.first { it.media.id == lowConfidence.media.id }.score,
    )
  }

  @Test
  fun `popularity is normalized independently for movies and tv`() {
    val movie = candidate(
      id = 1,
      type = MediaType.MOVIE,
      popularity = 500.0,
      country = "US",
    )
    val series = candidate(
      id = 2,
      type = MediaType.TV_SHOW,
      popularity = 50.0,
      country = "KR",
    )

    val ranked = engine.rank(
      candidates = listOf(movie, series),
      sort = ExploreSort.POPULARITY,
      applyGlobalDiversity = false,
    )

    assertEquals(2, ranked.size)
    assertEquals(ranked[0].score, ranked[1].score, 0.0001)
  }

  @Test
  fun `global results cap a dominant country when alternatives exist`() {
    val candidates = buildList {
      repeat(12) { index ->
        add(candidate(id = index + 1, country = "US", popularity = 200.0 - index))
      }
      repeat(6) { index ->
        add(candidate(id = index + 20, country = "KR", popularity = 100.0 - index))
      }
      repeat(6) { index ->
        add(candidate(id = index + 30, country = "JP", popularity = 90.0 - index))
      }
    }

    val ranked = engine.rank(
      candidates = candidates,
      sort = ExploreSort.POPULARITY,
      limit = 20,
    )

    assertEquals(20, ranked.size)
    assertTrue(ranked.count { "US" in it.media.originCountries } <= 8)
  }

  @Test
  fun `latest applies metadata and quality floor`() {
    val missingPoster = candidate(id = 1).copy(
      media = candidate(id = 1).media.copy(posterPath = null),
    )
    val lowQualityEstablished = candidate(
      id = 2,
      rating = 4.0,
      votes = 1_000,
    )
    val promisingNewTitle = candidate(
      id = 3,
      rating = 0.0,
      votes = 0,
    )

    val ranked = engine.rank(
      candidates = listOf(missingPoster, lowQualityEstablished, promisingNewTitle),
      sort = ExploreSort.LATEST,
    )

    assertEquals(listOf(3), ranked.map { it.media.id })
    assertFalse(ranked.any { it.media.posterPath == null })
  }

  private fun candidate(
    id: Int,
    type: MediaType = MediaType.MOVIE,
    rating: Double = 8.0,
    votes: Int = 1_000,
    popularity: Double = 100.0,
    country: String = "JP",
  ): ExploreCandidate =
    ExploreCandidate(
      media = MediaItem(
        id = id,
        type = type,
        title = "Title $id",
        originalTitle = "Title $id",
        overview = "Overview",
        posterPath = "/poster-$id.jpg",
        backdropPath = "/backdrop-$id.jpg",
        releaseDate = "2026-07-01",
        voteAverage = rating,
        voteCount = votes,
        popularity = popularity,
        adult = false,
        originalLanguage = if (country == "JP") "ja" else "en",
        genreIds = listOf(18),
        originCountries = listOf(country),
      ),
      trendingRank = null,
    )
}
