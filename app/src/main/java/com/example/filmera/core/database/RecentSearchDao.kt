package com.example.filmera.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {
  @Query("SELECT query FROM recent_searches WHERE ownerId = :ownerId AND isDeleted = 0 ORDER BY searchedAt DESC LIMIT :limit")
  fun observeRecentSearches(ownerId: String, limit: Int): Flow<List<String>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: RecentSearchEntity)

  @Query("SELECT * FROM recent_searches WHERE ownerId = :ownerId AND normalizedQuery = :normalizedQuery LIMIT 1")
  suspend fun find(ownerId: String, normalizedQuery: String): RecentSearchEntity?

  @Query("SELECT * FROM recent_searches WHERE ownerId = :ownerId AND syncState = 'PENDING'")
  suspend fun pending(ownerId: String): List<RecentSearchEntity>

  @Query("UPDATE recent_searches SET syncState = 'SYNCED' WHERE ownerId = :ownerId AND normalizedQuery = :normalizedQuery AND updatedAt = :expectedUpdatedAt")
  suspend fun markSynced(ownerId: String, normalizedQuery: String, expectedUpdatedAt: Long): Int

  @Transaction
  suspend fun save(ownerId: String, query: String, normalizedQuery: String, searchedAt: Long) {
    insert(
      RecentSearchEntity(
        ownerId = ownerId,
        normalizedQuery = normalizedQuery,
        query = query,
        searchedAt = searchedAt,
        updatedAt = searchedAt,
        isDeleted = false,
        syncState = SYNC_STATE_PENDING,
      ),
    )
  }

  @Query("UPDATE recent_searches SET isDeleted = 1, updatedAt = :updatedAt, syncState = 'PENDING' WHERE ownerId = :ownerId AND normalizedQuery = :normalizedQuery")
  suspend fun remove(ownerId: String, normalizedQuery: String, updatedAt: Long)

  @Query("UPDATE recent_searches SET isDeleted = 1, updatedAt = :updatedAt, syncState = 'PENDING' WHERE ownerId = :ownerId AND isDeleted = 0")
  suspend fun clear(ownerId: String, updatedAt: Long)

  @Query(
    """
    INSERT OR IGNORE INTO recent_searches (
      ownerId, normalizedQuery, query, searchedAt, updatedAt, isDeleted, syncState
    )
    SELECT :ownerId, normalizedQuery, query, searchedAt, updatedAt, isDeleted, 'PENDING'
    FROM recent_searches WHERE ownerId = :legacyOwnerId
    """,
  )
  suspend fun copyLegacy(ownerId: String, legacyOwnerId: String = LEGACY_OWNER_ID)

  @Query("DELETE FROM recent_searches WHERE ownerId = :legacyOwnerId")
  suspend fun deleteLegacy(legacyOwnerId: String = LEGACY_OWNER_ID)

  @Transaction
  suspend fun claimLegacy(ownerId: String) {
    copyLegacy(ownerId)
    deleteLegacy()
  }
}
