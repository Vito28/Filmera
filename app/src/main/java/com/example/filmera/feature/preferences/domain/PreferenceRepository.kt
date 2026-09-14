package com.example.filmera.feature.preferences.domain

import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
  fun observeProfile(): Flow<UserPreferenceProfile?>

  suspend fun getProfile(): UserPreferenceProfile?

  suspend fun saveProfile(profile: UserPreferenceProfile)

  suspend fun clearProfile()
}
