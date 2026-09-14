package com.example.filmera.feature.home.domain

import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeContentTest {
  @Test
  fun `hero selection removes duplicates and invalid visual candidates`() {
    val first = media(id = 1, title = "First")
    val duplicate = media(id = 1, title = "First duplicate")
    val missingBackdrop = media(id = 2, title = "No backdrop", backdropPath = null)
    val missingOverview = media(id = 3, title = "No overview", overview = "")
    val adult = media(id = 4, title = "Adult", adult = true)
    val second = media(id = 5, title = "Second", language = "ko")

    val featured = selectHeroMedia(
      listOf(first, missingBackdrop, missingOverview),
      listOf(duplicate, adult, second),
    )

    assertEquals(listOf(1, 5), featured.map(MediaItem::id))
  }

  @Test
  fun `global diversity limits early domination by one language`() {
    val english = (1..6).map { media(it, "English $it", language = "en") }
    val mixed = listOf(
      media(20, "Korean", language = "ko"),
      media(21, "Chinese", language = "zh"),
      media(22, "Indonesian", language = "id"),
      media(23, "Japanese", language = "ja"),
    )

    val selected = selectDiverseMedia(
      english,
      mixed,
      limit = 6,
      maxPerLanguage = 2,
    )

    assertEquals(2, selected.count { it.originalLanguage == "en" })
    assertEquals(
      setOf("en", "ko", "zh", "id", "ja"),
      selected.map(MediaItem::originalLanguage).toSet(),
    )
  }

  @Test
  fun `india does not force a single original language`() {
    assertEquals("IN", HomeChannel.INDIA.countryCode)
    assertNull(HomeChannel.INDIA.originalLanguage)
  }

  @Test
  fun `home session limits one media to two section appearances`() {
    val repeated = media(7, "Repeated")
    val content = HomeContent(
      feedKey = HomeFeedKey(HomeChannel.FOR_YOU),
      sections = HomeSectionType.entries.take(4).map { type ->
        HomeSection(type = type, items = listOf(repeated, media(type.ordinal + 20, type.name)))
      },
      genres = emptyList(),
      people = emptyList(),
      hasPartialFailures = false,
    ).enforceSessionMediaLimit()

    assertEquals(
      2,
      content.sections.sumOf { section -> section.items.count { it.key == repeated.key } },
    )
  }

  private fun media(
    id: Int,
    title: String,
    backdropPath: String? = "/backdrop-$id.jpg",
    overview: String = "Overview",
    adult: Boolean = false,
    language: String = "en",
  ) = MediaItem(
    id = id,
    type = MediaType.MOVIE,
    title = title,
    originalTitle = title,
    overview = overview,
    posterPath = "/poster-$id.jpg",
    backdropPath = backdropPath,
    releaseDate = "2026-07-01",
    voteAverage = 8.0,
    voteCount = 500,
    popularity = 100.0,
    adult = adult,
    originalLanguage = language,
    genreIds = listOf(18),
  )
}
