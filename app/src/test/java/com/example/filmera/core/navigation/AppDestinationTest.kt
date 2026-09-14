package com.example.filmera.core.navigation

import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.feature.home.domain.AnimeTopic
import com.example.filmera.feature.home.domain.HomeBrowseKind
import com.example.filmera.feature.home.domain.HomeChannel
import com.example.filmera.feature.home.domain.HomeFeedKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppDestinationTest {
  @Test
  fun `detail route contains media type and id`() {
    val route = AppDestination.Detail.createRoute(
      MediaKey(id = 123, type = MediaType.TV_SHOW),
    )

    assertEquals("detail/tv/123", route)
  }

  @Test
  fun `person detail route contains person id`() {
    assertEquals("person/31", AppDestination.PersonDetail.createRoute(31))
  }

  @Test
  fun `home browse route preserves section and active feed`() {
    val route = AppDestination.HomeBrowse.createRoute(
      kind = HomeBrowseKind.SPOTLIGHTS,
      feedKey = HomeFeedKey(HomeChannel.ANIME, AnimeTopic.SPORTS),
    )

    assertEquals("home/browse/SPOTLIGHTS/ANIME/SPORTS", route)
  }

  @Test
  fun `recommended browse route preserves its source section`() {
    assertEquals(
      "discover/recommended/hidden-gems",
      AppDestination.RecommendedBrowse.createRoute("hidden-gems"),
    )
  }

  @Test
  fun `video route contains its media context`() {
    val route = AppDestination.Trailer.createRoute(
      mediaKey = MediaKey(id = 42, type = MediaType.MOVIE),
      videoKey = "youtube-key",
    )

    assertEquals("video/movie/42/youtube-key", route)
  }

  @Test
  fun `video route rejects path injection characters`() {
    assertThrows(IllegalArgumentException::class.java) {
      AppDestination.Trailer.createRoute(
        mediaKey = MediaKey(id = 42, type = MediaType.MOVIE),
        videoKey = "../unexpected/route",
      )
    }
  }
}
