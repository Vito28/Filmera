package com.example.filmera.ui.screen.home.component

import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.bumptech.glide.Glide
import com.example.filmera.model.Movie
import com.example.filmera.ui.navigation.BottomNavItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrendingMovies(
  movies: List<Movie>,
  navController: NavController,
  modifier: Modifier = Modifier
) {
  val pagerState = rememberPagerState(pageCount = { movies.size })

  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.isScrollInProgress }
      .collect { isScrolling ->
        if (!isScrolling) {
          delay(2500)
          val nextPage = (pagerState.currentPage + 1) % movies.size
          pagerState.animateScrollToPage(nextPage)
        }
      }
  }

  HorizontalPager(
    state = pagerState,
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(16f / 9f)
  ) { page ->
    val movie = movies[page]
    val imageUrl = "https://image.tmdb.org/t/p/w780${movie.backdrop_path ?: movie.poster_path}"

    Box(
      modifier = Modifier
        .fillMaxSize()
        .clickable {
          navController.navigate(BottomNavItem.Detail.createRoute(movie.id))
        }
    ) {
      AndroidView(
        modifier = Modifier.matchParentSize(),
        factory = { context ->
          ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
          }
        },
        update = { imageView ->
          Glide.with(imageView.context)
            .load(imageUrl)
            .centerCrop()
            .into(imageView)
        }
      )
    }
  }
}

