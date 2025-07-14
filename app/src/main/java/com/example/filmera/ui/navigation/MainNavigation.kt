import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.example.filmera.ui.navigation.BottomNavItem
import com.example.filmera.ui.screen.favorite.FavoriteScreen
import com.example.filmera.ui.screen.home.DetailScreen
import com.example.filmera.ui.screen.home.HomeScreen
import com.example.filmera.ui.screen.home.ExploreScreen
import com.example.filmera.ui.screen.home.MovieVideoScreen

@Composable
fun MainNavigation(navController: NavHostController) {
  NavHost(navController = navController, startDestination = BottomNavItem.Home.route) {
    composable(BottomNavItem.Home.route) {
      HomeScreen(navController)
    }
    composable(BottomNavItem.Explore.route) {
       ExploreScreen(navController)
    }
    composable(BottomNavItem.Favorite.route) {
       FavoriteScreen(navController)
    }
    composable(BottomNavItem.Profile.route) {
      // ProfileScreen(navController)
    }

    composable(
      route = BottomNavItem.Detail.route,
      arguments = listOf(navArgument("movieId") { type = NavType.IntType })
    ) { backStackEntry ->
      val movieId = backStackEntry.arguments?.getInt("movieId") ?: return@composable
      DetailScreen(
        movieId = movieId,
        navController = navController
      )
    }

    composable("trailer/{videoKey}") { backStackEntry ->
      val key = backStackEntry.arguments?.getString("videoKey") ?: ""
      MovieVideoScreen(videoKey = key, navController = navController)
    }
  }
}
