package com.example.filmera.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.filmera.ui.navigation.BottomNavigationBar
import com.example.filmera.ui.theme.DarkColorScheme
import com.example.filmera.ui.theme.LightColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
  navController: NavHostController,
  modifier: Modifier = Modifier,
  title: String = "🎬 Filmera",
  showBottomBar: Boolean = true,
  showTopBarBackButton: Boolean = false,
  onBackClick: (() -> Unit)? = null,
  content: @Composable (PaddingValues) -> Unit
) {
  val colorScheme = MaterialTheme.colorScheme

  Scaffold(
    containerColor = colorScheme.background,
    contentColor = colorScheme.onBackground,
    topBar = {
      TopAppBar(
        title = {
          Text(title, style = MaterialTheme.typography.headlineSmall)
        },
        navigationIcon = {
          if (showTopBarBackButton && onBackClick != null) {
            IconButton(onClick = onBackClick) {
              Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
            }
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = colorScheme.primary,
          titleContentColor = colorScheme.onPrimary
        )
      )
    },
    bottomBar = {
      if (showBottomBar) {
        BottomNavigationBar(navController)
      }
    },
    modifier = modifier
  ) { innerPadding ->
    content(innerPadding)
  }
}
