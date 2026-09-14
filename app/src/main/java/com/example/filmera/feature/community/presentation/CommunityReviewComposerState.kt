package com.example.filmera.feature.community.presentation

import com.example.filmera.core.model.MediaItem

data class CommunityReviewComposerState(
  val media: MediaItem? = null,
  val rating: Int? = null,
  val headline: String = "",
  val body: String = "",
  val containsSpoilers: Boolean = false,
  val headlineInvalid: Boolean = false,
  val bodyInvalid: Boolean = false,
  val isSubmitting: Boolean = false,
) {
  val isVisible: Boolean
    get() = media != null

  val isDirty: Boolean
    get() = rating != null || headline.isNotBlank() || body.isNotBlank() || containsSpoilers
}
