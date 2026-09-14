package com.example.filmera.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferenceDao {
  @Query("SELECT * FROM user_preferences WHERE ownerId = :ownerId")
  fun observeProfile(ownerId: String): Flow<UserPreferenceEntity?>

  @Query("SELECT * FROM user_preferences WHERE ownerId = :ownerId")
  suspend fun getProfile(ownerId: String): UserPreferenceEntity?

  @Upsert
  suspend fun upsert(profile: UserPreferenceEntity)

  @Query("DELETE FROM user_preferences WHERE ownerId = :ownerId")
  suspend fun clear(ownerId: String)

  @Query("SELECT * FROM user_preferences WHERE ownerId = :ownerId AND syncState = 'PENDING'")
  suspend fun pending(ownerId: String): UserPreferenceEntity?

  @Query("UPDATE user_preferences SET syncState = 'SYNCED' WHERE ownerId = :ownerId AND updatedAt = :expectedUpdatedAt")
  suspend fun markSynced(ownerId: String, expectedUpdatedAt: Long): Int

  @Query(
    """
    INSERT OR IGNORE INTO user_preferences (
      ownerId, preferredMediaTypes, preferredGenreIds, preferredCountries,
      preferredMovieIds, preferredTvIds, preferredPersonIds,
      preferredLanguageCodes, onboardingCompleted, updatedAt, syncState
    )
    SELECT :ownerId, preferredMediaTypes, preferredGenreIds, preferredCountries,
      preferredMovieIds, preferredTvIds, preferredPersonIds,
      preferredLanguageCodes, onboardingCompleted, updatedAt, 'PENDING'
    FROM user_preferences WHERE ownerId = :legacyOwnerId
    """,
  )
  suspend fun copyLegacy(ownerId: String, legacyOwnerId: String = LEGACY_OWNER_ID)

  @Query("DELETE FROM user_preferences WHERE ownerId = :legacyOwnerId")
  suspend fun deleteLegacy(legacyOwnerId: String = LEGACY_OWNER_ID)

  @androidx.room.Transaction
  suspend fun claimLegacy(ownerId: String) {
    copyLegacy(ownerId)
    deleteLegacy()
  }
}
