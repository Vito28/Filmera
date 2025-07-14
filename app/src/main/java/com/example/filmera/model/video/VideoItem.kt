package com.example.filmera.model.video

data class VideoItem(
  val iso_639_1: String,
  val iso_3166_1: String,
  val name: String,
  val key: String,
  val site: String,
  val size: Int,
  val type: String,
  val official: Boolean,
  val published_at: String,
  val id: String
)
