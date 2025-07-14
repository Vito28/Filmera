package com.example.filmera.model.search

data class SearchResponse(
  val page: Int,
  val results: List<SearchResult>,
  val total_pages: Int,
  val total_results: Int
)