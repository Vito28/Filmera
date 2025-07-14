package com.example.filmera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteMovie(
  @PrimaryKey val id: Int,
  val title: String,
  val posterPath: String?,
  val overview: String
)