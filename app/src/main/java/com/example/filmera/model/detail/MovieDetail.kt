package com.example.filmera.model.detail

import com.example.filmera.model.Movie
import com.example.filmera.ui.screen.home.components.MovieCard

data class MovieDetail(
  val adult: Boolean,
  val backdrop_path: String?,
  val belongs_to_collection: CollectionInfo?,
  val budget: Int,
  val genres: List<Genre>,
  val homepage: String?,
  val id: Int,
  val imdb_id: String?,
  val origin_country: List<String>,
  val original_language: String,
  val original_title: String,
  val overview: String?,
  val popularity: Float,
  val poster_path: String?,
  val production_companies: List<ProductionCompany>,
  val production_countries: List<ProductionCountry>,
  val release_date: String?,
  val revenue: Long,
  val runtime: Int?,
  val spoken_languages: List<SpokenLanguage>,
  val status: String,
  val tagline: String?,
  val title: String,
  val video: Boolean,
  val vote_average: Float,
  val vote_count: Int
)

fun MovieDetail.toMovie(): Movie {
  return Movie(
    id = id,
    title = title,
    poster_path = poster_path ?: "",
    overview = overview ?: "",
    popularity = popularity,
    release_date = release_date ?: "",
    vote_average = vote_average,
    backdrop_path = backdrop_path ?: "",
    vote_count = vote_count,
    adult = adult,
    original_language = original_language,
    original_title = original_title,
    genre_ids = emptyList()
  )
}


