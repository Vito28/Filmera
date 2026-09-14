package com.example.filmera.core.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(
  navController: NavHostController,
  destinations: List<BottomNavigationDestination>,
) {
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination

  NavigationBar {
    destinations.forEach { item ->
      val destination = item.destination
      val isSelected = currentDestination
        ?.hierarchy
        ?.any { it.route == destination.route } == true
      NavigationBarItem(
        selected = isSelected,
        onClick = {
          if (currentDestination?.route != destination.route) {
            navController.navigate(destination.route) {
              popUpTo(AppDestination.Home.route) {
                saveState = true
              }
              launchSingleTop = true
              restoreState = true
            }
          }
        },
        icon = {
          Icon(
            imageVector = if (isSelected) {
              item.selectedIcon
            } else {
              item.unselectedIcon
            },
            contentDescription = stringResource(item.labelResource),
          )
        },
        label = { Text(stringResource(item.labelResource)) },
      )
    }
  }
}
