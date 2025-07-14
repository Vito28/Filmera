package com.example.filmera.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.selects.select

@Composable
fun BottomNavigationBar(navController: NavHostController) {
  val items = listOf(
    BottomNavItem.Home,
    BottomNavItem.Explore,
    BottomNavItem.Favorite,
    BottomNavItem.Profile
  )

  NavigationBar {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    items.forEach { item ->
      NavigationBarItem(
        selected = currentRoute == item.route,
        onClick = {
          if (currentRoute != item.route) {
            navController.navigate(item.route)
          }
        },
        icon = {
          item.icon?.let { Icon(it, contentDescription = item.label) }
        },
        label = {
          item.label?.let { Text(it) }
        }
      )
    }
  }
}