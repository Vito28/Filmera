package com.example.filmera.core.database

import com.example.filmera.feature.preferences.domain.MediaPreference
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPreferenceEntityTest {
  @Test
  fun `profile round trip preserves local recommendation preferences`() {
    val profile = UserPreferenceProfile(
      preferredMediaTypes = setOf(MediaPreference.MOVIE, MediaPreference.ANIME),
      preferredGenreIds = setOf(16, 18, 878),
      preferredCountries = setOf("GLOBAL", "ID", "JP"),
      preferredMovieIds = setOf(11, 22),
      preferredTvIds = setOf(33),
      preferredPersonIds = setOf(44, 55),
      preferredLanguageCodes = setOf("id", "en"),
      onboardingCompleted = true,
      updatedAt = 123_456L,
    )

    val restored = profile.toEntity("user-a").toDomain()

    assertEquals(profile, restored)
    assertTrue(restored.isReadyForRecommendations)
  }

  @Test
  fun `malformed stored values are ignored safely`() {
    val restored = UserPreferenceEntity(
      ownerId = "user-a",
      preferredMediaTypes = "MOVIE,UNKNOWN",
      preferredGenreIds = "18,invalid,-2",
      preferredCountries = "GLOBAL,JP",
      preferredMovieIds = "",
      preferredTvIds = "",
      preferredPersonIds = "",
      preferredLanguageCodes = "en",
      onboardingCompleted = false,
      updatedAt = 0L,
      syncState = SYNC_STATE_SYNCED,
    ).toDomain()

    assertEquals(setOf(MediaPreference.MOVIE), restored.preferredMediaTypes)
    assertEquals(setOf(18), restored.preferredGenreIds)
  }
}
