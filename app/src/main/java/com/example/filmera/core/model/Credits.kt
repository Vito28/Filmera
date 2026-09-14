package com.example.filmera.core.model

data class CastMember(
  val id: Int,
  val name: String,
  val character: String,
  val profilePath: String?,
  val order: Int,
)

data class CrewMember(
  val id: Int,
  val name: String,
  val job: String,
  val department: String,
  val profilePath: String?,
)

data class Credits(
  val cast: List<CastMember>,
  val crew: List<CrewMember>,
)
