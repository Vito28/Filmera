package com.example.filmera.core.database

import androidx.room.Entity
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType

@Entity(
  tableName = "library_items",
  primaryKeys = ["ownerId", "mediaId", "mediaType"],
)
data class LibraryItemEntity(
  val ownerId: String,
  val mediaId: Int,
  val mediaType: String,
  val remoteMediaId: Long?,
  val title: String,
  val originalTitle: String,
  val posterPath: String?,
  val backdropPath: String?,
  val overview: String,
  val releaseDate: String?,
  val voteAverage: Double,
  val voteCount: Int,
  val popularity: Double,
  val originalLanguage: String,
  val watchStatus: String,
  val isFavorite: Boolean,
  val addedAt: Long,
  val updatedAt: Long,
  val isDeleted: Boolean,
  val syncState: String,
)

fun MediaItem.toLibraryEntity(
  ownerId: String,
  watchStatus: String,
  isFavorite: Boolean,
  addedAt: Long,
  updatedAt: Long,
): LibraryItemEntity = LibraryItemEntity(
  ownerId = ownerId,
  mediaId = id,
  mediaType = type.name,
  remoteMediaId = null,
  title = title,
  originalTitle = originalTitle,
  posterPath = posterPath,
  backdropPath = backdropPath,
  overview = overview,
  releaseDate = releaseDate,
  voteAverage = voteAverage,
  voteCount = voteCount,
  popularity = popularity,
  originalLanguage = originalLanguage,
  watchStatus = watchStatus,
  isFavorite = isFavorite,
  addedAt = addedAt,
  updatedAt = updatedAt,
  isDeleted = false,
  syncState = SYNC_STATE_PENDING,
)

fun LibraryItemEntity.toMediaItem(): MediaItem = MediaItem(
  id = mediaId,
  type = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.MOVIE),
  title = title,
  originalTitle = originalTitle,
  overview = overview,
  posterPath = posterPath,
  backdropPath = backdropPath,
  releaseDate = releaseDate,
  voteAverage = voteAverage,
  voteCount = voteCount,
  popularity = popularity,
  adult = false,
  originalLanguage = originalLanguage,
  genreIds = emptyList(),
)
