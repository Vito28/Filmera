package com.example.filmera.core.navigation

import com.example.filmera.core.model.MediaKey
import com.example.filmera.feature.home.domain.HomeBrowseKind
import com.example.filmera.feature.home.domain.HomeFeedKey

sealed class AppDestination(val route: String) {
  data object Splash : AppDestination("splash")
  data object Welcome : AppDestination("welcome")
  data object Auth : AppDestination("auth")
  data object AuthSignUp : AppDestination("auth/sign-up")
  data object Home : AppDestination("home")
  data object Discover : AppDestination("discover")
  data object Community : AppDestination("community")
  data object RecommendedBrowse :
    AppDestination("discover/recommended/{recommendationSectionId}") {
    const val SECTION_ID_ARGUMENT = "recommendationSectionId"

    fun createRoute(sectionId: String): String {
      require(sectionId.matches(SECTION_ID_PATTERN)) {
        "sectionId contains unsupported route characters"
      }
      return "discover/recommended/$sectionId"
    }

    private val SECTION_ID_PATTERN = Regex("[a-z0-9-]+")
  }
  data object Search : AppDestination("search")
  data object Watchlist : AppDestination("watchlist")
  data object Profile : AppDestination("profile")
  data object Notifications : AppDestination("notifications")

  data object Preferences : AppDestination("preferences?editing={editing}") {
    const val EDITING_ARGUMENT = "editing"

    fun createRoute(editing: Boolean): String = "preferences?editing=$editing"
  }

  data object HomeBrowse :
    AppDestination("home/browse/{browseKind}/{channel}/{animeTopic}") {
    const val BROWSE_KIND_ARGUMENT = "browseKind"
    const val CHANNEL_ARGUMENT = "channel"
    const val ANIME_TOPIC_ARGUMENT = "animeTopic"

    fun createRoute(
      kind: HomeBrowseKind,
      feedKey: HomeFeedKey,
    ): String =
      "home/browse/${kind.name}/${feedKey.channel.name}/${feedKey.normalizedAnimeTopic.name}"
  }

  data object Detail : AppDestination("detail/{mediaType}/{mediaId}") {
    const val MEDIA_TYPE_ARGUMENT = "mediaType"
    const val MEDIA_ID_ARGUMENT = "mediaId"

    fun createRoute(key: MediaKey): String =
      "detail/${key.type.routeValue}/${key.id}"
  }

  data object ReviewDetail : AppDestination("community/review/{reviewId}") {
    const val REVIEW_ID_ARGUMENT = "reviewId"

    fun createRoute(reviewId: String): String {
      require(isValidReviewId(reviewId)) { "reviewId must be a valid UUID" }
      return "community/review/$reviewId"
    }

    fun isValidReviewId(reviewId: String): Boolean = REVIEW_ID_PATTERN.matches(reviewId)

    private val REVIEW_ID_PATTERN = Regex(
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
    )
  }

  data object PersonDetail : AppDestination("person/{personId}") {
    const val PERSON_ID_ARGUMENT = "personId"

    fun createRoute(personId: Int): String {
      require(personId > 0) { "personId must be greater than zero" }
      return "person/$personId"
    }
  }

  data object Trailer : AppDestination("video/{mediaType}/{mediaId}/{videoKey}") {
    const val MEDIA_TYPE_ARGUMENT = "mediaType"
    const val MEDIA_ID_ARGUMENT = "mediaId"
    const val VIDEO_KEY_ARGUMENT = "videoKey"

    fun createRoute(
      mediaKey: MediaKey,
      videoKey: String,
    ): String {
      require(VIDEO_KEY_PATTERN.matches(videoKey)) {
        "videoKey contains unsupported route characters"
      }
      return "video/${mediaKey.type.routeValue}/${mediaKey.id}/$videoKey"
    }

    private val VIDEO_KEY_PATTERN = Regex("[A-Za-z0-9_-]{1,128}")
  }
}
