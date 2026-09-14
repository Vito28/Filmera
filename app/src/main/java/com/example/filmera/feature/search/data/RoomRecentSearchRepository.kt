package com.example.filmera.feature.search.data

import com.example.filmera.core.database.RecentSearchDao
import com.example.filmera.core.database.SEARCH_CLEAR_METADATA_KEY
import com.example.filmera.core.database.SYNC_STATE_PENDING
import com.example.filmera.core.database.SyncMetadataDao
import com.example.filmera.core.database.SyncMetadataEntity
import com.example.filmera.core.sync.CurrentUserProvider
import com.example.filmera.core.sync.LegacyCurrentUserProvider
import com.example.filmera.core.sync.NoOpSyncRequestScheduler
import com.example.filmera.core.sync.SyncRequestScheduler
import com.example.filmera.feature.search.domain.RecentSearchRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class RoomRecentSearchRepository @Inject constructor(
  private val dao: RecentSearchDao,
  private val syncMetadataDao: SyncMetadataDao,
  private val currentUserProvider: CurrentUserProvider = LegacyCurrentUserProvider,
  private val syncScheduler: SyncRequestScheduler = NoOpSyncRequestScheduler,
) : RecentSearchRepository {
  override fun observeRecentSearches(): Flow<List<String>> =
    dao.observeRecentSearches(ownerId(), RECENT_SEARCH_LIMIT).distinctUntilChanged()

  override suspend fun save(query: String) {
    val displayQuery = query.trim().take(MAX_QUERY_LENGTH)
    val normalizedQuery = displayQuery.lowercase()
    if (normalizedQuery.isNotEmpty()) {
      dao.save(
        ownerId = ownerId(),
        query = displayQuery,
        normalizedQuery = normalizedQuery,
        searchedAt = System.currentTimeMillis(),
      )
      syncScheduler.schedule()
    }
  }

  override suspend fun remove(query: String) {
    dao.remove(ownerId(), query.trim().lowercase(), System.currentTimeMillis())
    syncScheduler.schedule()
  }

  override suspend fun clear() {
    val ownerId = ownerId()
    val now = System.currentTimeMillis()
    dao.clear(ownerId, now)
    syncMetadataDao.upsert(
      SyncMetadataEntity(
        ownerId = ownerId,
        metadataKey = SEARCH_CLEAR_METADATA_KEY,
        timestampValue = now,
        syncState = SYNC_STATE_PENDING,
      ),
    )
    syncScheduler.schedule()
  }

  private fun ownerId(): String =
    currentUserProvider.currentUserId() ?: error("An authenticated user is required")

  private companion object {
    const val RECENT_SEARCH_LIMIT = 8
    const val MAX_QUERY_LENGTH = 200
  }
}
