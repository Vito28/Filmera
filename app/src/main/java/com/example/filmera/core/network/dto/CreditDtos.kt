package com.example.filmera.core.network.dto

import com.google.gson.annotations.SerializedName

data class CreditsResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("cast") val cast: List<CastMemberDto> = emptyList(),
  @SerializedName("crew") val crew: List<CrewMemberDto> = emptyList(),
)

data class CastMemberDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("name") val name: String = "",
  @SerializedName("character") val character: String = "",
  @SerializedName("profile_path") val profilePath: String? = null,
  @SerializedName("order") val order: Int = Int.MAX_VALUE,
  @SerializedName("credit_id") val creditId: String? = null,
)

data class CrewMemberDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("name") val name: String = "",
  @SerializedName("job") val job: String = "",
  @SerializedName("department") val department: String = "",
  @SerializedName("profile_path") val profilePath: String? = null,
  @SerializedName("credit_id") val creditId: String? = null,
)

data class AggregateCreditsResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("cast") val cast: List<AggregateCastMemberDto> = emptyList(),
  @SerializedName("crew") val crew: List<AggregateCrewMemberDto> = emptyList(),
)

data class AggregateCastMemberDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("name") val name: String = "",
  @SerializedName("original_name") val originalName: String? = null,
  @SerializedName("profile_path") val profilePath: String? = null,
  @SerializedName("known_for_department") val knownForDepartment: String? = null,
  @SerializedName("roles") val roles: List<AggregateCastRoleDto> = emptyList(),
  @SerializedName("total_episode_count") val totalEpisodeCount: Int = 0,
  @SerializedName("order") val order: Int = Int.MAX_VALUE,
)

data class AggregateCastRoleDto(
  @SerializedName("credit_id") val creditId: String = "",
  @SerializedName("character") val character: String = "",
  @SerializedName("episode_count") val episodeCount: Int = 0,
)

data class AggregateCrewMemberDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("name") val name: String = "",
  @SerializedName("original_name") val originalName: String? = null,
  @SerializedName("profile_path") val profilePath: String? = null,
  @SerializedName("known_for_department") val knownForDepartment: String? = null,
  @SerializedName("department") val department: String = "",
  @SerializedName("jobs") val jobs: List<AggregateCrewJobDto> = emptyList(),
  @SerializedName("total_episode_count") val totalEpisodeCount: Int = 0,
)

data class AggregateCrewJobDto(
  @SerializedName("credit_id") val creditId: String = "",
  @SerializedName("job") val job: String = "",
  @SerializedName("episode_count") val episodeCount: Int = 0,
)

data class EpisodeCreditsResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("cast") val cast: List<CastMemberDto> = emptyList(),
  @SerializedName("crew") val crew: List<CrewMemberDto> = emptyList(),
  @SerializedName("guest_stars") val guestStars: List<CastMemberDto> = emptyList(),
)
