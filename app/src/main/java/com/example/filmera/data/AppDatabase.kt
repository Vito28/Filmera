package com.example.filmera.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.filmera.data.local.dao.FavoriteDao
import com.example.filmera.data.local.entity.FavoriteMovie

@Database(
  entities = [FavoriteMovie::class],
  version = 2,
  exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {
  abstract fun favoriteDao(): FavoriteDao
}