package com.example.filmera.core.database

import androidx.room.Entity
import com.example.filmera.feature.preferences.domain.MediaPreference
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
  @androidx.room.PrimaryKey val ownerId: String,
  val preferredMediaTypes: String,
  val preferredGenreIds: String,
  val preferredCountries: String,
  val preferredMovieIds: String,
  val preferredTvIds: String,
  val preferredPersonIds: String,
  val preferredLanguageCodes: String,
  val onboardingCompleted: Boolean,
  val updatedAt: Long,
  val syncState: String,
)

internal fun UserPreferenceProfile.toEntity(ownerId: String): UserPreferenceEntity =
  UserPreferenceEntity(
    ownerId = ownerId,
    preferredMediaTypes = preferredMediaTypes
      .map(MediaPreference::name)
      .sorted()
      .joinToString(CSV_SEPARATOR),
    preferredGenreIds = preferredGenreIds.toCsv(),
    preferredCountries = preferredCountries.toCsv(),
    preferredMovieIds = preferredMovieIds.toCsv(),
    preferredTvIds = preferredTvIds.toCsv(),
    preferredPersonIds = preferredPersonIds.toCsv(),
    preferredLanguageCodes = preferredLanguageCodes.toCsv(),
    onboardingCompleted = onboardingCompleted,
    updatedAt = updatedAt,
    syncState = SYNC_STATE_PENDING,
  )

internal fun UserPreferenceEntity.toDomain(): UserPreferenceProfile =
  UserPreferenceProfile(
    preferredMediaTypes = preferredMediaTypes
      .csvValues()
      .mapNotNull { value -> runCatching { MediaPreference.valueOf(value) }.getOrNull() }
      .toSet(),
    preferredGenreIds = preferredGenreIds.csvInts(),
    preferredCountries = preferredCountries.csvValues().toSet(),
    preferredMovieIds = preferredMovieIds.csvInts(),
    preferredTvIds = preferredTvIds.csvInts(),
    preferredPersonIds = preferredPersonIds.csvInts(),
    preferredLanguageCodes = preferredLanguageCodes.csvValues().toSet(),
    onboardingCompleted = onboardingCompleted,
    updatedAt = updatedAt,
  )

private fun Iterable<*>.toCsv(): String =
  joinToString(CSV_SEPARATOR) { value -> value.toString().trim() }

private fun String.csvValues(): List<String> =
  split(CSV_SEPARATOR)
    .map(String::trim)
    .filter(String::isNotEmpty)

private fun String.csvInts(): Set<Int> =
  csvValues().mapNotNull(String::toIntOrNull).filter { it > 0 }.toSet()

private const val CSV_SEPARATOR = ","
