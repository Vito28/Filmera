package com.example.filmera.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector? = null, val label: String? = null) {
  data object Home : BottomNavItem("home", Icons.Default.Home, "Home")
  data object Explore : BottomNavItem("explore", Icons.Default.Search, "Explore")
  data object Favorite : BottomNavItem("favorite", Icons.Default.Favorite, "Favorite")
  data object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
  data object Detail : BottomNavItem("movie_detail/{movieId}") {
    fun createRoute(movieId: Int) = "movie_detail/$movieId"
  }

}