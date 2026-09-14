package com.example.filmera.feature.discover.domain

import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.feature.preferences.domain.MediaPreference
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
  private val engine = RecommendationEngine(currentYear = 2026)
  private val profile = UserPreferenceProfile(
    preferredMediaTypes = setOf(MediaPreference.MOVIE),
    preferredGenreIds = setOf(18, 28, 878),
    preferredCountries = setOf("GLOBAL", "JP"),
    onboardingCompleted = true,
  )

  @Test
  fun `weighted rating favors meaningful vote confidence`() {
    val lowConfidence = candidate(
      id = 1,
      rating = 9.5,
      votes = 100,
      popularity = 40.0,
    )
    val established = candidate(
      id = 2,
      rating = 8.3,
      votes = 8_000,
      popularity = 40.0,
    )

    val ranked = engine.rank(
      candidates = listOf(lowConfidence, established),
      profile = profile,
      feedback = emptyMap(),
    )

    assertEquals(2, ranked.first().media.id)
    assertTrue(ranked.first().score > ranked.last().score)
  }

  @Test
  fun `selected genre is an actual filter`() {
    val drama = candidate(id = 1, genres = listOf(18))
    val action = candidate(id = 2, genres = listOf(28))

    val ranked = engine.rank(
      candidates = listOf(drama, action),
      profile = profile,
      feedback = emptyMap(),
      selectedGenreId = 28,
    )

    assertEquals(listOf(2), ranked.map { it.media.id })
  }

  @Test
  fun `not interested feedback excludes title`() {
    val hidden = candidate(id = 1)
    val visible = candidate(id = 2)

    val ranked = engine.rank(
      candidates = listOf(hidden, visible),
      profile = profile,
      feedback = mapOf(
        hidden.media.key to RecommendationFeedbackAction.NOT_INTERESTED,
      ),
    )

    assertFalse(ranked.any { it.media.id == 1 })
    assertTrue(ranked.any { it.media.id == 2 })
  }

  @Test
  fun `diversification caps a dominant country when alternatives exist`() {
    val candidates = buildList {
      repeat(6) { index ->
        add(candidate(id = index + 1, country = "US", genres = listOf(18 + index)))
      }
      repeat(3) { index ->
        add(candidate(id = index + 20, country = "JP", genres = listOf(40 + index)))
      }
      repeat(3) { index ->
        add(candidate(id = index + 30, country = "KR", genres = listOf(50 + index)))
      }
    }
    val ranked = engine.rank(candidates, profile, emptyMap())

    val diversified = engine.diversify(ranked, limit = 10)

    assertEquals(10, diversified.size)
    assertTrue(diversified.count { "US" in it.originCountries } <= 4)
    assertTrue(diversified.map { it.originCountries.first() }.toSet().size >= 3)
  }

  @Test
  fun `match percentage is derived deterministically from score`() {
    val candidate = candidate(id = 1)

    val first = engine.rank(listOf(candidate), profile, emptyMap()).single()
    val second = engine.rank(listOf(candidate), profile, emptyMap()).single()

    assertEquals(first.score, second.score, 0.0)
    assertTrue(
      kotlin.math.abs((first.score * 100).toInt() - first.matchPercentage) <= 1,
    )
  }

  private fun candidate(
    id: Int,
    rating: Double = 8.0,
    votes: Int = 2_000,
    popularity: Double = 50.0,
    country: String = "JP",
    genres: List<Int> = listOf(18, 878),
  ): RecommendationCandidate {
    val media = MediaItem(
      id = id,
      type = MediaType.MOVIE,
      title = "Movie $id",
      originalTitle = "Movie $id",
      overview = "Overview",
      posterPath = "/poster-$id.jpg",
      backdropPath = "/backdrop-$id.jpg",
      releaseDate = "2024-01-01",
      voteAverage = rating,
      voteCount = votes,
      popularity = popularity,
      adult = false,
      originalLanguage = if (country == "JP") "ja" else "en",
      genreIds = genres,
      originCountries = listOf(country),
    )
    return RecommendationCandidate(
      media = media,
      originCountries = setOf(country),
      sources = setOf(RecommendationSource.PREFERRED_GENRE),
    )
  }
}
