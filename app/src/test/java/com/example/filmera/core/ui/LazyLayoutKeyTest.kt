package com.example.filmera.core.ui

import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LazyLayoutKeyTest {
  @Test
  fun `media key is a String and includes media type identity`() {
    val movieKey: String = LazyLayoutKey.media(
      scope = "search",
      item = media(id = 7, type = MediaType.MOVIE),
    )
    val tvKey: String = LazyLayoutKey.media(
      scope = "search",
      item = media(id = 7, type = MediaType.TV_SHOW),
    )

    assertTrue(movieKey.isNotBlank())
    assertNotEquals(movieKey, tvKey)
  }

  @Test
  fun `length-prefixed parts cannot collide at separators`() {
    val first: String = LazyLayoutKey.of("section", "a|b", "c")
    val second: String = LazyLayoutKey.of("section", "a", "b|c")

    assertNotEquals(first, second)
  }

  private fun media(
    id: Int,
    type: MediaType,
  ): MediaItem = MediaItem(
    id = id,
    type = type,
    title = "Title",
    originalTitle = "Title",
    overview = "",
    posterPath = null,
    backdropPath = null,
    releaseDate = null,
    voteAverage = 0.0,
    voteCount = 0,
    popularity = 0.0,
    adult = false,
    originalLanguage = "en",
    genreIds = emptyList(),
  )
}
