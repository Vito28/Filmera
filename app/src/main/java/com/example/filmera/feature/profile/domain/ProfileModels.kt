package com.example.filmera.feature.profile.domain

import com.example.filmera.core.common.DataResult

data class UserProfile(
  val displayName: String,
  val username: String,
  val avatarUrl: String?,
  val bio: String?,
)

interface ProfileRepository {
  suspend fun getCurrentProfile(): DataResult<UserProfile>
}
