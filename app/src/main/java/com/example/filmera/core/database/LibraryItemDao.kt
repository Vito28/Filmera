package com.example.filmera.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryItemDao {
  @Query(
    "SELECT * FROM library_items WHERE ownerId = :ownerId AND isDeleted = 0",
  )
  fun observeLibrary(ownerId: String): Flow<List<LibraryItemEntity>>

  @Query(
    """
    SELECT * FROM library_items
    WHERE ownerId = :ownerId AND mediaId = :mediaId AND mediaType = :mediaType
    LIMIT 1
    """,
  )
  suspend fun find(
    ownerId: String,
    mediaId: Int,
    mediaType: String,
  ): LibraryItemEntity?

  @Upsert
  suspend fun upsert(item: LibraryItemEntity)

  @Query("SELECT * FROM library_items WHERE ownerId = :ownerId AND syncState = 'PENDING'")
  suspend fun pending(ownerId: String): List<LibraryItemEntity>

  @Query("UPDATE library_items SET syncState = 'SYNCED', remoteMediaId = :remoteMediaId WHERE ownerId = :ownerId AND mediaId = :mediaId AND mediaType = :mediaType AND updatedAt = :expectedUpdatedAt")
  suspend fun markSynced(
    ownerId: String,
    mediaId: Int,
    mediaType: String,
    expectedUpdatedAt: Long,
    remoteMediaId: Long,
  ): Int

  @Transaction
  suspend fun setFavorite(
    ownerId: String,
    candidate: LibraryItemEntity,
    isFavorite: Boolean,
  ) {
    val existing = find(ownerId, candidate.mediaId, candidate.mediaType)
    val updated = candidate.copy(
      watchStatus = existing?.watchStatus ?: candidate.watchStatus,
      isFavorite = isFavorite,
      addedAt = existing?.addedAt ?: candidate.addedAt,
      remoteMediaId = existing?.remoteMediaId,
      updatedAt = candidate.updatedAt,
      isDeleted = false,
      syncState = SYNC_STATE_PENDING,
    )
    persistOrDelete(updated)
  }

  @Transaction
  suspend fun setWatchStatus(
    ownerId: String,
    candidate: LibraryItemEntity,
    watchStatus: String,
  ) {
    val existing = find(ownerId, candidate.mediaId, candidate.mediaType)
    val updated = candidate.copy(
      watchStatus = watchStatus,
      isFavorite = existing?.isFavorite ?: candidate.isFavorite,
      addedAt = existing?.addedAt ?: candidate.addedAt,
      remoteMediaId = existing?.remoteMediaId,
      updatedAt = candidate.updatedAt,
      isDeleted = false,
      syncState = SYNC_STATE_PENDING,
    )
    persistOrDelete(updated)
  }

  private suspend fun persistOrDelete(item: LibraryItemEntity) {
    upsert(
      if (item.watchStatus == WATCH_STATUS_NONE && !item.isFavorite) {
        item.copy(isDeleted = true, syncState = SYNC_STATE_PENDING)
      } else {
        item.copy(isDeleted = false, syncState = SYNC_STATE_PENDING)
      },
    )
  }

  @Query(
    """
    INSERT OR IGNORE INTO library_items (
      ownerId, mediaId, mediaType, remoteMediaId, title, originalTitle,
      posterPath, backdropPath, overview, releaseDate, voteAverage, voteCount,
      popularity, originalLanguage, watchStatus, isFavorite, addedAt, updatedAt,
      isDeleted, syncState
    )
    SELECT :ownerId, mediaId, mediaType, remoteMediaId, title, originalTitle,
      posterPath, backdropPath, overview, releaseDate, voteAverage, voteCount,
      popularity, originalLanguage, watchStatus, isFavorite, addedAt, updatedAt,
      isDeleted, 'PENDING'
    FROM library_items WHERE ownerId = :legacyOwnerId
    """,
  )
  suspend fun copyLegacy(ownerId: String, legacyOwnerId: String = LEGACY_OWNER_ID)

  @Query("DELETE FROM library_items WHERE ownerId = :legacyOwnerId")
  suspend fun deleteLegacy(legacyOwnerId: String = LEGACY_OWNER_ID)

  @Transaction
  suspend fun claimLegacy(ownerId: String) {
    copyLegacy(ownerId)
    deleteLegacy()
  }

  private companion object {
    const val WATCH_STATUS_NONE = "NONE"
  }
}
