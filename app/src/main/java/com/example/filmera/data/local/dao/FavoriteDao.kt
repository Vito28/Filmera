package com.example.filmera.data.local.dao

import androidx.room.*
import com.example.filmera.data.local.entity.FavoriteMovie
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
  @Query("SELECT * FROM favorites")
  fun getAllFavorites(): Flow<List<FavoriteMovie>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun addToFavorites(movie: FavoriteMovie)

  @Delete
  suspend fun removeFromFavorites(movie: FavoriteMovie)

  @Query("SELECT EXISTS(SELECT * FROM favorites WHERE id = :id)")
  suspend fun isFavorite(id: Int): Boolean
}