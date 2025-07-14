package com.example.filmera.ui.screen.favorite

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.filmera.model.toMovie
import com.example.filmera.ui.screen.MainLayout
import com.example.filmera.viewModel.FavoriteMovieViewModel

@Composable
fun FavoriteScreen(
  navController: NavHostController,
  viewModel: FavoriteMovieViewModel = hiltViewModel()
) {
  val favorites = viewModel.favorites.collectAsState().value

  MainLayout(
    navController = navController,
    title = "Favorite Movies"
  ) { padding ->
    if (favorites.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No favorite movies yet.",
          style = MaterialTheme.typography.bodyLarge,
          color = Color.Gray
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(favorites) { favorite ->
          FavoriteCardLoaded(
            movieId = favorite.id,
            onClick = {
              navController.navigate("movie/${favorite.id}")
            },
            onDeleteClick = {
              viewModel.toggleFavorite(favorite)
            }
          )
        }
      }
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(favorites) { favorite ->
          FavoriteCardLoaded(
            movieId = favorite.id,
            onClick = {
              navController.navigate("movie/${favorite.id}")
            },
            onDeleteClick = {
              viewModel.toggleFavorite(favorite)
            }
          )
        }
      }
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(favorites) { favorite ->
          FavoriteCardLoaded(
            movieId = favorite.id,
            onClick = {
              navController.navigate("movie/${favorite.id}")
            },
            onDeleteClick = {
              viewModel.toggleFavorite(favorite)
            }
          )
        }
      }
    }
  }
}
