package com.example.filmera.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.filmera.R

data class BottomNavigationDestination(
  val destination: AppDestination,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  @param:StringRes val labelResource: Int,
)

val defaultBottomNavigationDestinations = listOf(
  BottomNavigationDestination(
    destination = AppDestination.Home,
    selectedIcon = Icons.Filled.Home,
    unselectedIcon = Icons.Outlined.Home,
    labelResource = R.string.navigation_home,
  ),
  BottomNavigationDestination(
    destination = AppDestination.Discover,
    selectedIcon = Icons.Filled.Explore,
    unselectedIcon = Icons.Outlined.Explore,
    labelResource = R.string.navigation_explore,
  ),
  BottomNavigationDestination(
    destination = AppDestination.Community,
    selectedIcon = Icons.Filled.Forum,
    unselectedIcon = Icons.Outlined.Forum,
    labelResource = R.string.navigation_community,
  ),
  BottomNavigationDestination(
    destination = AppDestination.Watchlist,
    selectedIcon = Icons.Filled.Bookmarks,
    unselectedIcon = Icons.Outlined.Bookmarks,
    labelResource = R.string.navigation_watchlist,
  ),
  BottomNavigationDestination(
    destination = AppDestination.Profile,
    selectedIcon = Icons.Filled.AccountCircle,
    unselectedIcon = Icons.Outlined.AccountCircle,
    labelResource = R.string.navigation_profile,
  ),
)
