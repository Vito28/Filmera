package com.example.filmera.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.filmera.ui.screen.MainLayout
import com.example.filmera.ui.screen.home.component.MovieCatalogScreen
import com.example.filmera.ui.screen.home.component.TrendingMovies
import com.example.filmera.ui.screen.home.components.SectionHeader
import com.example.filmera.viewModel.MovieViewModel

@Composable
fun HomeScreen(
  navController: NavHostController,
  movieViewModel: MovieViewModel = hiltViewModel()
) {
  val isLoading = movieViewModel.isLoading.collectAsState().value

  LaunchedEffect(Unit) {
    movieViewModel.loadAllHomeMovies()
  }

  val trendingMovies = movieViewModel.trendingMovies.collectAsState().value
  val nowPlaying = movieViewModel.nowPlayingMovies.collectAsState().value
  val popularMovies = movieViewModel.popularMovies.collectAsState().value
  val upcomingMovies = movieViewModel.upcomingMovies.collectAsState().value
  val topRatedMovies = movieViewModel.topRatedMovies.collectAsState().value
  val westernTv = movieViewModel.westernTv.collectAsState().value
  val kDrama = movieViewModel.kDrama.collectAsState().value
  val shortMovies = movieViewModel.shortMovies.collectAsState().value
  val animeMovies = movieViewModel.animeMovies.collectAsState().value

  MainLayout(
    navController = navController,
    title = "Filmera"
  ) { innerPadding ->
    if (isLoading) {
      // Loading Screen
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
    } else {
      // Content Screen
      Column(
        modifier = Modifier
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .fillMaxSize()
      ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (trendingMovies.isNotEmpty()) {
          SectionHeader(title = "Trending")
          TrendingMovies(
            movies = trendingMovies,
            navController = navController,
            modifier = Modifier
              .fillMaxWidth()
              .height(280.dp)
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (nowPlaying.isNotEmpty()) {
          SectionHeader(title = "Now Playing")
          MovieCatalogScreen(navController = navController, movies = nowPlaying)
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (westernTv.isNotEmpty()) {
          SectionHeader(title = "Western TV")
          MovieCatalogScreen(navController = navController, movies = westernTv)
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (popularMovies.isNotEmpty()) {
          SectionHeader(title = "Popular Movies")
          MovieCatalogScreen(navController = navController, movies = popularMovies)
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (upcomingMovies.isNotEmpty()) {
          SectionHeader(title = "Upcoming")
          MovieCatalogScreen(navController = navController, movies = upcomingMovies)
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (animeMovies.isNotEmpty()) {
          SectionHeader(title = "Anime")
          MovieCatalogScreen(navController = navController, movies = animeMovies)
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (topRatedMovies.isNotEmpty()) {
          SectionHeader(title = "Top Rated")
          MovieCatalogScreen(navController = navController, movies = topRatedMovies)
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (shortMovies.isNotEmpty()) {
          SectionHeader(title = "Short Movies")
          MovieCatalogScreen(navController = navController, movies = shortMovies)
        }

        Spacer(modifier = Modifier.height(32.dp))
      }
    }
  }
}
