package com.example.filmera.model.credit

data class CreditsResponse(
  val id: Int,
  val cast: List<Cast>,
  val crew: List<Crew>
)