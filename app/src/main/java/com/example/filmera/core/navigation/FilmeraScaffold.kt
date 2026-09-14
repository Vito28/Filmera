package com.example.filmera.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.example.filmera.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmeraScaffold(
  title: String,
  navController: NavHostController,
  modifier: Modifier = Modifier,
  showBottomBar: Boolean = true,
  bottomNavigationDestinations: List<BottomNavigationDestination> =
    defaultBottomNavigationDestinations,
  onNavigateBack: (() -> Unit)? = null,
  content: @Composable (PaddingValues) -> Unit,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
          if (onNavigateBack != null) {
            IconButton(onClick = onNavigateBack) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back),
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        ),
      )
    },
    bottomBar = {
      if (showBottomBar) {
        BottomNavigationBar(
          navController = navController,
          destinations = bottomNavigationDestinations,
        )
      }
    },
    content = content,
  )
}
