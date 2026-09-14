package com.example.filmera.feature.library.domain

import com.example.filmera.core.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
  fun observeLibrary(): Flow<List<LibraryItem>>

  suspend fun setWatchStatus(
    item: MediaItem,
    status: WatchStatus,
  )

  suspend fun setFavorite(
    item: MediaItem,
    isFavorite: Boolean,
  )

  suspend fun restore(item: LibraryItem)
}
