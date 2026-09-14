package com.example.filmera.feature.library.domain

import com.example.filmera.core.model.MediaItem

enum class WatchStatus {
  NONE,
  WATCHLIST,
  WATCHING,
  COMPLETED,
}

data class LibraryItem(
  val media: MediaItem,
  val watchStatus: WatchStatus,
  val isFavorite: Boolean,
  val addedAt: Long,
  val updatedAt: Long,
)
