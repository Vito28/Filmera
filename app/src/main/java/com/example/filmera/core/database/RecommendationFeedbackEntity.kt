package com.example.filmera.core.database

import androidx.room.Entity

@Entity(
  tableName = "recommendation_feedback",
  primaryKeys = ["mediaId", "mediaType"],
)
data class RecommendationFeedbackEntity(
  val mediaId: Int,
  val mediaType: String,
  val action: String,
  val updatedAt: Long,
)
