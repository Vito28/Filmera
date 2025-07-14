package com.example.filmera.repository

import com.example.filmera.data.local.dao.FavoriteDao
import com.example.filmera.data.local.entity.FavoriteMovie
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
  private val dao: FavoriteDao
) {

  fun getFavorites(): Flow<List<FavoriteMovie>> = dao.getAllFavorites()

  suspend fun isFavorite(id: Int): Boolean = dao.isFavorite(id)

  suspend fun add(movie: FavoriteMovie) = dao.addToFavorites(movie)

  suspend fun remove(movie: FavoriteMovie) = dao.removeFromFavorites(movie)
}
