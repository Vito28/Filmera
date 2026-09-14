package com.example.filmera.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SyncMetadataDao {
  @Upsert
  suspend fun upsert(metadata: SyncMetadataEntity)

  @Query("SELECT * FROM sync_metadata WHERE ownerId = :ownerId AND metadataKey = :metadataKey AND syncState = 'PENDING'")
  suspend fun pending(
    ownerId: String,
    metadataKey: String = SEARCH_CLEAR_METADATA_KEY,
  ): SyncMetadataEntity?

  @Query("UPDATE sync_metadata SET syncState = 'SYNCED', timestampValue = :serverTimestamp WHERE ownerId = :ownerId AND metadataKey = :metadataKey AND timestampValue = :expectedTimestamp")
  suspend fun markSynced(
    ownerId: String,
    expectedTimestamp: Long,
    serverTimestamp: Long,
    metadataKey: String = SEARCH_CLEAR_METADATA_KEY,
  ): Int
}
