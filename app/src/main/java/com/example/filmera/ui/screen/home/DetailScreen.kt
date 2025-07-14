package com.example.filmera.ui.screen.home

import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.Glide
import com.example.filmera.model.Movie
import com.example.filmera.model.detail.MovieDetail
import com.example.filmera.model.video.VideoItem
import com.example.filmera.ui.screen.MainLayout
import com.example.filmera.ui.screen.home.component.MovieCatalogScreen
import com.example.filmera.ui.screen.home.component.detail.CreditSection
import com.example.filmera.viewModel.MovieViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
  movieId: Int,
  navController: NavHostController,
  movieViewModel: MovieViewModel = hiltViewModel()
) {
  // Load data once
  LaunchedEffect(movieId) {
    movieViewModel.loadMovieDetail(movieId)
    movieViewModel.loadRecommendations(movieId)
    movieViewModel.loadMovieTrailer(movieId)
    movieViewModel.loadCredits(movieId)
  }

  val movieDetail = movieViewModel.movieDetailMap[movieId]
  val recommendationMovies = movieViewModel.recommendations.collectAsState().value
  val movieVideo = movieViewModel.movieTrailer.collectAsState().value
  val movieCredits = movieViewModel.movieCredits.collectAsState().value

  movieDetail?.let { movie ->
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
      delay(150)
      contentVisible = true
    }

    MainLayout(
      navController = navController,
      title = movie.title.ifBlank { "Movie" },
      showBottomBar = false,
      showTopBarBackButton = true,
      onBackClick = { navController.popBackStack() }
    ) { innerPadding ->
      AnimatedVisibility(
        visible = contentVisible,
        enter = fadeIn()
      ) {
        LazyColumn(
          modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp)
            .background(Color.Transparent),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          item { DetailSection(movie = movie, movieVideo = movieVideo, navController = navController) }
          item { TaglineSection(movie = movie) }
          item { InfoBlockSection(movie = movie) }
          if (movieCredits != null) {
            item {
              CreditSection(
                castList = movieCredits.cast,
                crewList = movieCredits.crew
              )
            }
          }
          item { RecommendationSection(navController = navController, movies = recommendationMovies) }
        }
      }
    }
  }
}

// Section 1: Detail Section (Poster, Title, Rating, Watch Trailer Button)
@Composable
fun DetailSection(
  movie: MovieDetail,
  movieVideo: VideoItem?,
  navController: NavHostController
) {
  Row(modifier = Modifier.fillMaxWidth()) {
    // Movie Poster
    Box(
      modifier = Modifier
        .height(180.dp)
        .width(120.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color.LightGray) // Placeholder color
        .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp)) // Add shadow for depth
    ) {
      AndroidView(
        modifier = Modifier.matchParentSize(),
        factory = { context ->
          ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
          }
        },
        update = { imageView ->
          val imageUrl = "https://image.tmdb.org/t/p/w500${movie.poster_path}"
          Glide.with(imageView.context)
            .load(imageUrl)
            .centerCrop()
            .into(imageView)
        }
      )
    }

    Spacer(modifier = Modifier.width(16.dp))

    // Movie Details
    Column(
      modifier = Modifier.weight(1f)
    ) {
      Text(
        text = movie.title,
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      )
      Text(
        text = "(${movie.release_date?.take(4) ?: "N/A"})",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
      )
      Text(
        text = "⭐ ${String.format("%.1f", movie.vote_average)} • ${movie.runtime ?: 0} min",
        style = MaterialTheme.typography.bodyMedium
      )

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          movieVideo?.key?.let { key ->
            navController.navigate("trailer/$key")
          }
        },
        enabled = movieVideo != null,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          disabledContainerColor = Color.Gray
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "🎬 Watch Trailer",
          fontSize = 14.sp,
          style = MaterialTheme.typography.labelLarge
        )
      }
    }
  }
}

// Section 2: Tagline Section
@Composable
fun TaglineSection(movie: MovieDetail) {
  Column(modifier = Modifier.padding(vertical = 8.dp)) {
    movie.tagline?.let {
      Text(
        text = "\"$it\"",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Medium,
          fontStyle = FontStyle.Italic
        ),
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = movie.overview ?: "-",
      style = MaterialTheme.typography.bodyLarge
    )
  }
}

// Section 3: Info Block Section
@Composable
fun InfoBlockSection(movie: MovieDetail) {
  Column {
    Divider(
      modifier = Modifier
        .padding(vertical = 8.dp)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
      thickness = 1.dp
    )
    Column {
      InfoBlock(title = "Original Title", value = movie.original_title)
      InfoBlock(title = "Genre", value = movie.genres.joinToString { it.name })
      InfoBlock(title = "Language", value = movie.spoken_languages.joinToString { it.english_name })
      InfoBlock(title = "Country", value = movie.production_countries.joinToString { it.name })
      InfoBlock(title = "Companies", value = movie.production_companies.joinToString { it.name })
      InfoBlock(title = "Budget", value = "$${movie.budget}")
      InfoBlock(title = "Revenue", value = "$${movie.revenue}")
      InfoBlock(title = "Popularity", value = "${movie.popularity}")
      InfoBlock(title = "Vote Average", value = "${movie.vote_average}/10")
      InfoBlock(title = "Vote Count", value = "${movie.vote_count}")
      InfoBlock(title = "Status", value = movie.status)
      InfoBlock(title = "Release Date", value = movie.release_date ?: "-")
      InfoBlock(title = "IMDB ID", value = movie.imdb_id ?: "-")
      InfoBlock(title = "Homepage", value = movie.homepage ?: "-")
    }
    Spacer(modifier = Modifier.height(8.dp))
  }
}

// Section 4: Recommendation Section
@Composable
fun RecommendationSection(
  navController: NavHostController,
  movies: List<Movie>
) {
  Column() {
    Text(
      text = "Recommended Movies",
      style = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
    )
    Spacer(modifier = Modifier.height(8.dp))
    MovieCatalogScreen(
      navController = navController,
      movies = movies
    )
  }
}

// Helper Composable for Info Blocks
@Composable
fun InfoBlock(title: String, value: String?) {
  val isValid = !value.isNullOrBlank() &&
          value != "-" &&
          value != "0" &&
          value != "0.0" &&
          value != "$0" &&
          value != "$0.0"

  if (isValid) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Medium,
          color = Color.Gray
        )
      )
      Text(
        text = value!!,
        style = MaterialTheme.typography.bodyMedium
      )
    }
  }
}

