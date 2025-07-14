package com.example.filmera.model

data class CatalogResponse(
  val page: Int,
  val results: List<Movie>,
  val total_pages: Int,
  val total_results: Int
)