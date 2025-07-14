package com.example.filmera

import MainNavigation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.filmera.ui.theme.FilmeraTheme
import com.example.filmera.viewModel.MovieViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private val movieViewModel: MovieViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

//    WindowCompat.setDecorFitsSystemWindows(window, false)
    enableEdgeToEdge()

    setContent {
      FilmeraTheme {
        val navController = rememberNavController()
        MainNavigation(navController = navController)
      }
    }
  }
}
