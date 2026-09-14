package com.example.filmera.feature.preferences.data

import com.example.filmera.core.database.UserPreferenceDao
import com.example.filmera.core.database.toDomain
import com.example.filmera.core.database.toEntity
import com.example.filmera.feature.preferences.domain.PreferenceRepository
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile
import com.example.filmera.core.sync.CurrentUserProvider
import com.example.filmera.core.sync.LegacyCurrentUserProvider
import com.example.filmera.core.sync.NoOpSyncRequestScheduler
import com.example.filmera.core.sync.SyncRequestScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPreferenceRepository @Inject constructor(
  private val preferenceDao: UserPreferenceDao,
  private val currentUserProvider: CurrentUserProvider = LegacyCurrentUserProvider,
  private val syncScheduler: SyncRequestScheduler = NoOpSyncRequestScheduler,
) : PreferenceRepository {
  override fun observeProfile(): Flow<UserPreferenceProfile?> =
    preferenceDao.observeProfile(ownerId()).map { entity -> entity?.toDomain() }

  override suspend fun getProfile(): UserPreferenceProfile? =
    preferenceDao.getProfile(ownerId())?.toDomain()

  override suspend fun saveProfile(profile: UserPreferenceProfile) {
    preferenceDao.upsert(profile.toEntity(ownerId()))
    syncScheduler.schedule()
  }

  override suspend fun clearProfile() {
    preferenceDao.upsert(
      UserPreferenceProfile(
        preferredCountries = emptySet(),
        onboardingCompleted = false,
        updatedAt = System.currentTimeMillis(),
      ).toEntity(ownerId()),
    )
    syncScheduler.schedule()
  }

  private fun ownerId(): String =
    currentUserProvider.currentUserId() ?: error("An authenticated user is required")
}
