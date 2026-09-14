package com.example.filmera.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface RecommendationFeedbackDao {
  @Query("SELECT * FROM recommendation_feedback")
  suspend fun getAll(): List<RecommendationFeedbackEntity>

  @Upsert
  suspend fun upsert(feedback: RecommendationFeedbackEntity)

  @Query("DELETE FROM recommendation_feedback")
  suspend fun clear()
}
