package com.example.filmera.feature.detail.domain

import com.example.filmera.core.model.Credits
import com.example.filmera.core.model.MediaDetails
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaVideo
import com.example.filmera.core.model.WatchProviderResult

data class DetailContent(
  val details: MediaDetails,
  val credits: Credits,
  val trailer: MediaVideo?,
  val videos: List<MediaVideo> = listOfNotNull(trailer),
  val watchProviders: Map<String, WatchProviderResult> = emptyMap(),
  val recommendations: List<MediaItem>,
  val hasPartialFailures: Boolean,
)
