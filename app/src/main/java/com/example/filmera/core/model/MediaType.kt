package com.example.filmera.core.model

enum class MediaType(val routeValue: String) {
  MOVIE("movie"),
  TV_SHOW("tv");

  companion object {
    fun fromRoute(value: String?): MediaType? =
      entries.firstOrNull { it.routeValue == value }
  }
}

data class MediaKey(
  val id: Int,
  val type: MediaType,
)
