package com.example.filmera.core.database

import androidx.room.Entity

@Entity(
  tableName = "sync_metadata",
  primaryKeys = ["ownerId", "metadataKey"],
)
data class SyncMetadataEntity(
  val ownerId: String,
  val metadataKey: String,
  val timestampValue: Long,
  val syncState: String,
)

const val SYNC_STATE_PENDING = "PENDING"
const val SYNC_STATE_SYNCED = "SYNCED"
const val LEGACY_OWNER_ID = "__legacy__"
const val SEARCH_CLEAR_METADATA_KEY = "search_clear"
