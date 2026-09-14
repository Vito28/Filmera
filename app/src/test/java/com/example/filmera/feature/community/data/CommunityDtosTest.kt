package com.example.filmera.feature.community.data

import com.example.filmera.core.model.MediaType
import com.example.filmera.feature.community.domain.ReviewReaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityDtosTest {
  @Test
  fun `review dto separates author viewer and aggregate ratings`() {
    val review = reviewDto().toDomain()!!

    assertEquals(9, review.authorRating)
    assertEquals(7, review.viewerRating)
    assertEquals(8.4, review.filmeraRating!!, 0.0)
    assertEquals(125L, review.ratingCount)
    assertEquals(MediaType.MOVIE, review.mediaKey.type)
  }

  @Test
  fun `reaction mapper preserves selected reaction and clamps invalid counts`() {
    val review = reviewDto(
      viewerReaction = "mind_blown",
      loveCount = -4,
      mindBlownCount = 12,
    ).toDomain()!!

    assertEquals(ReviewReaction.MIND_BLOWN, review.reactions.selectedReaction)
    assertEquals(0, review.reactions.count(ReviewReaction.LOVE))
    assertEquals(12, review.reactions.count(ReviewReaction.MIND_BLOWN))
  }

  @Test
  fun `unknown media type is rejected instead of creating an unsafe route`() {
    assertNull(reviewDto(mediaType = "person").toDomain())
  }

  @Test
  fun `rating write verification requires matching media and viewer value`() {
    val response = MediaRatingSummaryDto(
      mediaId = 17,
      viewerRating = 8,
      filmeraRating = 8.0,
      ratingCount = 1,
    )

    assertTrue(response.matchesRatingWrite(expectedMediaId = 17, expectedRating = 8))
    assertFalse(response.matchesRatingWrite(expectedMediaId = 18, expectedRating = 8))
    assertFalse(response.matchesRatingWrite(expectedMediaId = 17, expectedRating = 7))
  }

  @Test
  fun `rating removal verification requires authoritative null viewer value`() {
    val removed = MediaRatingSummaryDto(
      mediaId = 17,
      viewerRating = null,
      filmeraRating = null,
      ratingCount = 0,
    )

    assertTrue(removed.matchesRatingWrite(expectedMediaId = 17, expectedRating = null))
    assertFalse(removed.copy(viewerRating = 8).matchesRatingWrite(17, null))
  }

  private fun reviewDto(
    mediaType: String = "movie",
    viewerReaction: String? = null,
    loveCount: Int = 2,
    mindBlownCount: Int = 0,
  ) = CommunityReviewDto(
    reviewId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    mediaId = 17,
    mediaType = mediaType,
    tmdbId = 550,
    mediaTitle = "Fight Club",
    posterPath = "/poster.jpg",
    releaseDate = "1999-10-15",
    authorId = "11111111-1111-4111-8111-111111111111",
    authorUsername = "cinephile",
    authorDisplayName = "Cinephile",
    reviewBody = "A sufficiently long community review body for mapping tests.",
    containsSpoilers = false,
    userRating = 9,
    viewerRating = 7,
    filmeraRating = 8.4,
    ratingCount = 125,
    helpfulCount = 8,
    commentCount = 3,
    viewerHasMarkedHelpful = true,
    viewerReaction = viewerReaction,
    loveCount = loveCount,
    mindBlownCount = mindBlownCount,
    createdAt = "2026-08-04T10:00:00Z",
    updatedAt = "2026-08-04T10:00:00Z",
  )
}
