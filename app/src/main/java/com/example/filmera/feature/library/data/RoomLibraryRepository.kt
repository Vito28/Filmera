package com.example.filmera.feature.library.data

import com.example.filmera.core.database.LibraryItemDao
import com.example.filmera.core.database.LibraryItemEntity
import com.example.filmera.core.database.toLibraryEntity
import com.example.filmera.core.database.toMediaItem
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.sync.CurrentUserProvider
import com.example.filmera.core.sync.LegacyCurrentUserProvider
import com.example.filmera.core.sync.NoOpSyncRequestScheduler
import com.example.filmera.core.sync.SyncRequestScheduler
import com.example.filmera.feature.library.domain.LibraryItem
import com.example.filmera.feature.library.domain.LibraryRepository
import com.example.filmera.feature.library.domain.WatchStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomLibraryRepository @Inject constructor(
  private val dao: LibraryItemDao,
  private val currentUserProvider: CurrentUserProvider = LegacyCurrentUserProvider,
  private val syncScheduler: SyncRequestScheduler = NoOpSyncRequestScheduler,
) : LibraryRepository {
  override fun observeLibrary(): Flow<List<LibraryItem>> =
    dao.observeLibrary(ownerId())
      .map { items -> items.map(LibraryItemEntity::toDomain) }
      .distinctUntilChanged()

  override suspend fun setWatchStatus(
    item: MediaItem,
    status: WatchStatus,
  ) {
    val now = System.currentTimeMillis()
    dao.setWatchStatus(
      ownerId = ownerId(),
      candidate = item.toLibraryEntity(
        ownerId = ownerId(),
        watchStatus = WatchStatus.NONE.name,
        isFavorite = false,
        addedAt = now,
        updatedAt = now,
      ),
      watchStatus = status.name,
    )
    syncScheduler.schedule()
  }

  override suspend fun setFavorite(
    item: MediaItem,
    isFavorite: Boolean,
  ) {
    val now = System.currentTimeMillis()
    dao.setFavorite(
      ownerId = ownerId(),
      candidate = item.toLibraryEntity(
        ownerId = ownerId(),
        watchStatus = WatchStatus.NONE.name,
        isFavorite = isFavorite,
        addedAt = now,
        updatedAt = now,
      ),
      isFavorite = isFavorite,
    )
    syncScheduler.schedule()
  }

  override suspend fun restore(item: LibraryItem) {
    val ownerId = ownerId()
    val existing = dao.find(ownerId, item.media.id, item.media.type.name)
    dao.upsert(
      item.toEntity(ownerId).copy(
        remoteMediaId = existing?.remoteMediaId,
        updatedAt = System.currentTimeMillis(),
      ),
    )
    syncScheduler.schedule()
  }

  private fun ownerId(): String =
    currentUserProvider.currentUserId() ?: error("An authenticated user is required")
}

private fun LibraryItemEntity.toDomain(): LibraryItem = LibraryItem(
  media = toMediaItem(),
  watchStatus = watchStatus.toWatchStatus(),
  isFavorite = isFavorite,
  addedAt = addedAt,
  updatedAt = updatedAt,
)

private fun LibraryItem.toEntity(ownerId: String): LibraryItemEntity = media.toLibraryEntity(
  ownerId = ownerId,
  watchStatus = watchStatus.name,
  isFavorite = isFavorite,
  addedAt = addedAt,
  updatedAt = updatedAt,
)

private fun String.toWatchStatus(): WatchStatus =
  WatchStatus.entries.firstOrNull { it.name == this } ?: WatchStatus.NONE
