package com.example.filmera.core.database

import androidx.room.Entity
@Entity(
  tableName = "recent_searches",
  primaryKeys = ["ownerId", "normalizedQuery"],
)
data class RecentSearchEntity(
  val ownerId: String,
  val normalizedQuery: String,
  val query: String,
  val searchedAt: Long,
  val updatedAt: Long,
  val isDeleted: Boolean,
  val syncState: String,
)
