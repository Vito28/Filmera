package com.example.filmera.core.model

data class MediaVideo(
  val key: String,
  val name: String,
  val site: String,
  val type: String,
  val isOfficial: Boolean,
  val id: String = "",
  val languageCode: String? = null,
  val countryCode: String? = null,
  val publishedAt: String? = null,
)
