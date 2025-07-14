package com.example.filmera.ui.screen.favorite

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.example.filmera.model.Movie
import com.example.filmera.model.detail.MovieDetail
import com.example.filmera.viewModel.MovieViewModel
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.filmera.model.detail.toMovie

@Composable
fun FavoriteCardLoaded(
  movieId: Int,
  onClick: () -> Unit,
  onDeleteClick: () -> Unit,
  movieViewModel: MovieViewModel = hiltViewModel()
) {
  // Load movie detail (if not cached)
  LaunchedEffect(movieId) {
    movieViewModel.loadMovieDetail(movieId)
  }

  // Observe cache map (will recompose when updated)
  val detail = movieViewModel.movieDetailMap[movieId]

  if (detail == null) {
    ShimmerCardPlaceholder()
  } else {
    FavoriteCard(
      movie = detail.toMovie(),
      onClick = onClick,
      onDeleteClick = onDeleteClick
    )
  }
}
