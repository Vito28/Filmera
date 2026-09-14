package com.example.filmera.core.navigation

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.feature.auth.presentation.AuthNextDestination
import com.example.filmera.feature.auth.presentation.AuthMode
import com.example.filmera.feature.auth.presentation.AuthRoute
import com.example.filmera.feature.auth.presentation.WelcomeRoute
import com.example.filmera.feature.community.presentation.home.CommunityRoute
import com.example.filmera.feature.community.presentation.review.ReviewDetailRoute
import com.example.filmera.feature.detail.presentation.DetailRoute
import com.example.filmera.feature.discover.presentation.DiscoverRoute
import com.example.filmera.feature.discover.presentation.RecommendedForYouRoute
import com.example.filmera.feature.home.presentation.HomeBrowseRoute
import com.example.filmera.feature.home.presentation.HomeRoute
import com.example.filmera.feature.home.presentation.NotificationsScreen
import com.example.filmera.feature.library.presentation.WatchlistRoute
import com.example.filmera.feature.person.presentation.PersonDetailRoute
import com.example.filmera.feature.profile.presentation.ProfileRoute
import com.example.filmera.feature.preferences.presentation.PreferenceOnboardingRoute
import com.example.filmera.feature.preferences.presentation.SplashRoute
import com.example.filmera.feature.preferences.presentation.StartupDestination
import com.example.filmera.feature.search.presentation.SearchRoute
import com.example.filmera.feature.trailer.presentation.VideoDetailRoute

@Composable
fun AppNavGraph(
  navController: NavHostController,
) {
  val openDetails: (com.example.filmera.core.model.MediaItem) -> Unit = { item ->
    navController.navigate(AppDestination.Detail.createRoute(item.key))
  }
  val openPersonDetails: (Int) -> Unit = { personId ->
    navController.navigate(AppDestination.PersonDetail.createRoute(personId))
  }
  val finishAuthentication: (AuthNextDestination) -> Unit = { destination ->
    val route = when (destination) {
      AuthNextDestination.PREFERENCES ->
        AppDestination.Preferences.createRoute(editing = false)
      AuthNextDestination.HOME -> AppDestination.Home.route
    }
    navController.navigate(route) {
      popUpTo(AppDestination.Welcome.route) {
        inclusive = true
      }
      launchSingleTop = true
    }
  }

  NavHost(
    navController = navController,
    startDestination = AppDestination.Splash.route,
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    composable(AppDestination.Splash.route) {
      SplashRoute(onDestinationReady = { destination ->
        val route = when (destination) {
          StartupDestination.AUTH -> AppDestination.Welcome.route
          StartupDestination.PREFERENCES ->
            AppDestination.Preferences.createRoute(editing = false)
          StartupDestination.HOME -> AppDestination.Home.route
        }
        navController.navigate(route) {
          popUpTo(AppDestination.Splash.route) {
            inclusive = true
          }
          launchSingleTop = true
        }
      })
    }
    composable(AppDestination.Welcome.route) {
      WelcomeRoute(
        onContinueWithEmail = {
          navController.navigate(AppDestination.Auth.route) {
            launchSingleTop = true
          }
        },
        onCreateAccount = {
          navController.navigate(AppDestination.AuthSignUp.route) {
            launchSingleTop = true
          }
        },
        onAuthenticated = finishAuthentication,
      )
    }
    composable(AppDestination.Auth.route) {
      AuthRoute(
        initialMode = AuthMode.SIGN_IN,
        onAuthenticated = finishAuthentication,
        onBack = { navController.popBackStack() },
      )
    }
    composable(AppDestination.AuthSignUp.route) {
      AuthRoute(
        initialMode = AuthMode.SIGN_UP,
        onAuthenticated = finishAuthentication,
        onBack = { navController.popBackStack() },
      )
    }
    composable(
      route = AppDestination.Preferences.route,
      arguments = listOf(
        navArgument(AppDestination.Preferences.EDITING_ARGUMENT) {
          type = NavType.BoolType
          defaultValue = false
        },
      ),
    ) { entry ->
      val isEditing = entry.arguments
        ?.getBoolean(AppDestination.Preferences.EDITING_ARGUMENT)
        ?: false
      PreferenceOnboardingRoute(
        onCompleted = {
          if (isEditing) {
            navController.popBackStack()
          } else {
            navController.navigate(AppDestination.Home.route) {
              popUpTo(AppDestination.Preferences.route) {
                inclusive = true
              }
              launchSingleTop = true
            }
          }
        },
        onNavigateBack = {
          if (isEditing) {
            navController.popBackStack()
          }
        },
      )
    }
    composable(AppDestination.Home.route) {
      HomeRoute(
        navController = navController,
        onMediaClick = openDetails,
        onPersonClick = openPersonDetails,
      )
    }
    composable(AppDestination.Notifications.route) {
      NotificationsScreen(onBack = { navController.popBackStack() })
    }
    composable(AppDestination.Discover.route) {
      DiscoverRoute(
        navController = navController,
        onMediaClick = openDetails,
        onPersonClick = openPersonDetails,
        onNotificationClick = {
          navController.navigate(AppDestination.Notifications.route) {
            launchSingleTop = true
          }
        },
        onOpenSearch = {
          navController.navigate(AppDestination.Search.route) {
            launchSingleTop = true
          }
        },
        onUpdatePreferences = {
          navController.navigate(AppDestination.Preferences.createRoute(editing = true)) {
            launchSingleTop = true
          }
        },
        onSeeAllRecommendations = { sectionId ->
          navController.navigate(AppDestination.RecommendedBrowse.createRoute(sectionId)) {
            launchSingleTop = true
          }
        },
      )
    }
    composable(AppDestination.Community.route) {
      CommunityRoute(
        navController = navController,
        onOpenSearch = {
          navController.navigate(AppDestination.Search.route) { launchSingleTop = true }
        },
        onOpenNotifications = {
          navController.navigate(AppDestination.Notifications.route) { launchSingleTop = true }
        },
      )
    }
    composable(
      route = AppDestination.ReviewDetail.route,
      arguments = listOf(
        navArgument(AppDestination.ReviewDetail.REVIEW_ID_ARGUMENT) {
          type = NavType.StringType
        },
      ),
    ) {
      ReviewDetailRoute(
        onBack = { navController.popBackStack() },
        onMediaClick = { key ->
          navController.navigate(AppDestination.Detail.createRoute(key))
        },
      )
    }
    composable(
      route = AppDestination.RecommendedBrowse.route,
      arguments = listOf(
        navArgument(AppDestination.RecommendedBrowse.SECTION_ID_ARGUMENT) {
          type = NavType.StringType
        },
      ),
    ) { entry ->
      RecommendedForYouRoute(
        sectionId = entry.arguments
          ?.getString(AppDestination.RecommendedBrowse.SECTION_ID_ARGUMENT)
          .orEmpty(),
        onBack = { navController.popBackStack() },
        onMediaClick = openDetails,
        onPersonClick = openPersonDetails,
      )
    }
    composable(
      route = AppDestination.HomeBrowse.route,
      arguments = listOf(
        navArgument(AppDestination.HomeBrowse.BROWSE_KIND_ARGUMENT) {
          type = NavType.StringType
        },
        navArgument(AppDestination.HomeBrowse.CHANNEL_ARGUMENT) {
          type = NavType.StringType
        },
        navArgument(AppDestination.HomeBrowse.ANIME_TOPIC_ARGUMENT) {
          type = NavType.StringType
        },
      ),
    ) {
      HomeBrowseRoute(
        onBack = { navController.popBackStack() },
        onMediaClick = openDetails,
        onPersonClick = openPersonDetails,
        onPlayTrailer = { trailer ->
          navController.navigate(
            AppDestination.Trailer.createRoute(
              mediaKey = trailer.media.key,
              videoKey = trailer.videoKey,
            ),
          )
        },
      )
    }
    composable(
      route = AppDestination.Search.route,
      enterTransition = {
        slideInVertically(tween(220)) { fullHeight -> fullHeight / 40 }
      },
      exitTransition = {
        slideOutVertically(tween(180)) { fullHeight -> -fullHeight / 40 }
      },
      popExitTransition = {
        slideOutVertically(tween(200)) { fullHeight -> fullHeight / 40 }
      },
    ) {
      SearchRoute(
        onBack = {
          if (!navController.popBackStack()) {
            navController.navigate(AppDestination.Home.route) {
              launchSingleTop = true
            }
          }
        },
        onMediaClick = openDetails,
        onPersonClick = openPersonDetails,
        bottomBar = {
          BottomNavigationBar(
            navController = navController,
            destinations = defaultBottomNavigationDestinations,
          )
        },
      )
    }
    composable(AppDestination.Watchlist.route) {
      WatchlistRoute(
        navController = navController,
        onMediaClick = openDetails,
        onExploreClick = {
          navController.navigate(AppDestination.Search.route) {
            launchSingleTop = true
          }
        },
      )
    }
    composable(AppDestination.Profile.route) {
      ProfileRoute(
        navController = navController,
        onOpenLibrary = {
          navController.navigate(AppDestination.Watchlist.route) {
            launchSingleTop = true
          }
        },
        onOpenPreferences = {
          navController.navigate(AppDestination.Preferences.createRoute(editing = true)) {
            launchSingleTop = true
          }
        },
      )
    }
    composable(
      route = AppDestination.Detail.route,
      arguments = listOf(
        navArgument(AppDestination.Detail.MEDIA_TYPE_ARGUMENT) { type = NavType.StringType },
        navArgument(AppDestination.Detail.MEDIA_ID_ARGUMENT) { type = NavType.IntType },
      ),
    ) { entry ->
      val detailMediaKey = MediaType
        .fromRoute(
          entry.arguments?.getString(AppDestination.Detail.MEDIA_TYPE_ARGUMENT),
        )
        ?.let { type ->
          entry.arguments
            ?.getInt(AppDestination.Detail.MEDIA_ID_ARGUMENT)
            ?.takeIf { it > 0 }
            ?.let { id -> MediaKey(id = id, type = type) }
        }
      DetailRoute(
        navController = navController,
        onMediaClick = openDetails,
        onPersonClick = openPersonDetails,
        onPlayTrailer = { videoKey ->
          detailMediaKey?.let { mediaKey ->
            navController.navigate(
              AppDestination.Trailer.createRoute(
                mediaKey = mediaKey,
                videoKey = videoKey,
              ),
            )
          }
        },
      )
    }
    composable(
      route = AppDestination.PersonDetail.route,
      arguments = listOf(
        navArgument(AppDestination.PersonDetail.PERSON_ID_ARGUMENT) {
          type = NavType.IntType
        },
      ),
    ) {
      PersonDetailRoute(
        navController = navController,
        onMediaClick = openDetails,
      )
    }
    composable(
      route = AppDestination.Trailer.route,
      arguments = listOf(
        navArgument(AppDestination.Trailer.MEDIA_TYPE_ARGUMENT) {
          type = NavType.StringType
        },
        navArgument(AppDestination.Trailer.MEDIA_ID_ARGUMENT) {
          type = NavType.IntType
        },
        navArgument(AppDestination.Trailer.VIDEO_KEY_ARGUMENT) { type = NavType.StringType },
      ),
    ) {
      VideoDetailRoute(
        onBack = { navController.popBackStack() },
        onMediaClick = openDetails,
        onPersonClick = openPersonDetails,
      )
    }
  }
}
