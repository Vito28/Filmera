package com.example.filmera.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TmdbQueryModelsTest {
  @Test
  fun `anime movie preset combines genre origin and original language`() {
    val query = MovieDiscoverQuery.japaneseAnime(
      locale = TmdbLocale(language = "en-US", region = "US"),
      page = 3,
    ).toQueryMap()

    assertEquals("en-US", query["language"])
    assertEquals("US", query["region"])
    assertEquals("3", query["page"])
    assertEquals("16", query["with_genres"])
    assertEquals("JP", query["with_origin_country"])
    assertEquals("ja", query["with_original_language"])
  }

  @Test
  fun `regional drama presets keep metadata language separate from content origin`() {
    val locale = TmdbLocale(language = "id-ID", region = "ID")

    val korean = TvDiscoverQuery.koreanDrama(locale).toQueryMap()
    val chinese = TvDiscoverQuery.chineseDrama(locale).toQueryMap()

    assertEquals("id-ID", korean["language"])
    assertEquals("KR", korean["with_origin_country"])
    assertEquals("ko", korean["with_original_language"])
    assertEquals("18", korean["with_genres"])
    assertEquals("CN", chinese["with_origin_country"])
    assertEquals("zh", chinese["with_original_language"])
  }

  @Test
  fun `complex movie discover query serializes TMDB filter combinations`() {
    val query = MovieDiscoverQuery(
      page = 2,
      sortBy = MovieSort.VOTE_COUNT_DESC,
      includeAdult = true,
      withGenres = IdFilter(setOf(878, 28), FilterCombination.ANY),
      withoutGenres = IdFilter(setOf(27)),
      minimumRuntimeMinutes = 80,
      maximumRuntimeMinutes = 150,
      minimumVoteAverage = 7.5,
      minimumVoteCount = 500,
      watchProviders = IdFilter(setOf(8, 337), FilterCombination.ANY),
      watchMonetizationTypes = setOf(
        WatchMonetizationType.RENT,
        WatchMonetizationType.FLATRATE,
      ),
    ).toQueryMap()

    assertEquals("2", query["page"])
    assertEquals("vote_count.desc", query["sort_by"])
    assertEquals("true", query["include_adult"])
    assertEquals("28|878", query["with_genres"])
    assertEquals("27", query["without_genres"])
    assertEquals("80", query["with_runtime.gte"])
    assertEquals("150", query["with_runtime.lte"])
    assertEquals("7.5", query["vote_average.gte"])
    assertEquals("500", query["vote_count.gte"])
    assertEquals("8|337", query["with_watch_providers"])
    assertEquals("flatrate|rent", query["with_watch_monetization_types"])
  }

  @Test
  fun `invalid locale and page fail before a network request`() {
    assertThrows(IllegalArgumentException::class.java) {
      TmdbLocale(language = "indonesia", region = "ID")
    }
    assertThrows(IllegalArgumentException::class.java) {
      TmdbCatalogRequest(feed = TmdbCatalogFeed.POPULAR_MOVIES, page = 0)
    }
  }
}
