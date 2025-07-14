package com.example.filmera.model

import com.example.filmera.data.local.entity.FavoriteMovie

data class Movie(
  val id: Int,
  val title: String,
  val overview: String,
  val poster_path: String?,
  val backdrop_path: String?,
  val release_date: String?,
  val vote_average: Float,
  val vote_count: Int,
  val popularity: Float,
  val adult: Boolean,
  val original_language: String,
  val original_title: String,
  val genre_ids: List<Int>?,
)

fun Movie.toFavoriteMovie(): FavoriteMovie {
  return FavoriteMovie(
    id = this.id,
    title = this.title,
    posterPath = this.poster_path ?: "",
    overview = this.overview,
  )
}

fun FavoriteMovie.toMovie(): Movie = Movie(
  id = id,
  title = title,
  poster_path = posterPath,
  overview = overview,
  backdrop_path = null,
  release_date = null,
  vote_average = 0f,
  vote_count = 0,
  popularity = 0f,
  adult = false,
  original_language = "en",
  original_title = title,
  genre_ids = emptyList()
)

