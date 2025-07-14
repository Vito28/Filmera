package com.example.filmera.ui.screen.home.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.filmera.model.Movie
import com.example.filmera.model.toFavoriteMovie
import com.example.filmera.ui.navigation.BottomNavItem
import com.example.filmera.ui.screen.home.components.MovieCard
import com.example.filmera.viewModel.FavoriteMovieViewModel

@Composable
fun MovieCatalogScreen(
  navController: NavController,
  movies: List<Movie>,
  modifier: Modifier = Modifier,
  favoriteViewModel: FavoriteMovieViewModel = hiltViewModel()
) {
  val favoriteIds = favoriteViewModel.favorites.collectAsState().value.map { it.id }

  LazyRow(modifier = modifier) {
    item {
      Spacer(modifier = Modifier.width(8.dp))
    }

    items(movies) { movie ->
      MovieCard(
        movie = movie,
        isBookmarked = favoriteIds.contains(movie.id),
        onClick = {
          navController.navigate(BottomNavItem.Detail.createRoute(movie.id))
        },
        onBookmarkClick = { movieClicked ->
          favoriteViewModel.toggleFavorite(movieClicked.toFavoriteMovie())
        }
      )
    }

    item {
      Spacer(modifier = Modifier.width(8.dp))
    }
  }
}
