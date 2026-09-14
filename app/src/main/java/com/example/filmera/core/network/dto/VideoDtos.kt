package com.example.filmera.core.network.dto

import com.google.gson.annotations.SerializedName

data class VideoResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("results") val results: List<VideoDto> = emptyList(),
)

data class VideoDto(
  @SerializedName("key") val key: String = "",
  @SerializedName("name") val name: String = "",
  @SerializedName("site") val site: String = "",
  @SerializedName("type") val type: String = "",
  @SerializedName("official") val official: Boolean = false,
  @SerializedName("iso_639_1") val languageCode: String? = null,
  @SerializedName("iso_3166_1") val countryCode: String? = null,
  @SerializedName("size") val size: Int = 0,
  @SerializedName("published_at") val publishedAt: String? = null,
  @SerializedName("id") val id: String = "",
)
