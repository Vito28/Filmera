package com.example.filmera.feature.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.filmera.core.designsystem.theme.FilmeraTheme
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.home.domain.HomeSection
import com.example.filmera.feature.home.domain.HomeSectionType
import com.example.filmera.feature.home.presentation.component.TrendingSection
import org.junit.Rule
import org.junit.Test

class HomeChromeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun Home_appBar_usesSearchAndNotification_withoutProfile() {
    composeRule.setContent {
      FilmeraTheme {
        HomeScreen(
          state = HomeUiState(
            contentState = LoadState.Empty,
            unreadNotificationCount = 3,
          ),
          onAction = {},
          onMediaClick = {},
          onPersonClick = {},
          onPlayTrailer = {},
          onSearchClick = {},
          onNotificationClick = {},
          onBrowseClick = {},
          onLibraryClick = {},
        )
      }
    }

    composeRule.onAllNodesWithContentDescription("Open search").assertCountEquals(1)
    composeRule.onAllNodesWithContentDescription("3 unread notifications").assertCountEquals(1)
    composeRule.onAllNodesWithContentDescription("Open profile").assertCountEquals(0)
  }

  @Test
  fun Trending_compactLayout_usesSmallFixedRankBadges() {
    composeRule.setContent {
      FilmeraTheme {
        Box(modifier = Modifier.width(390.dp)) {
          TrendingSection(
            section = HomeSection(
              type = HomeSectionType.TRENDING,
              items = (1..6).map(::trendingMedia),
            ),
            onMediaClick = {},
            onShowAll = {},
            onRetry = {},
          )
        }
      }
    }

    composeRule
      .onNodeWithTag("home_trending_rank_1", useUnmergedTree = true)
      .assertWidthIsEqualTo(44.dp)
    composeRule
      .onNodeWithTag("home_trending_rank_2", useUnmergedTree = true)
      .assertWidthIsEqualTo(34.dp)
    composeRule
      .onNodeWithTag("home_trending_rank_4", useUnmergedTree = true)
      .assertWidthIsEqualTo(34.dp)
  }

  private fun trendingMedia(rank: Int) = MediaItem(
    id = rank,
    type = if (rank % 2 == 0) MediaType.TV_SHOW else MediaType.MOVIE,
    title = "Trending $rank",
    originalTitle = "Trending $rank",
    overview = "Preview",
    posterPath = null,
    backdropPath = null,
    releaseDate = "2026-07-30",
    voteAverage = 8.0,
    voteCount = 100,
    popularity = 100.0,
    adult = false,
    originalLanguage = "en",
    genreIds = emptyList(),
  )
}
