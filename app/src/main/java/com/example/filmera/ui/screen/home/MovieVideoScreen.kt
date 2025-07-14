package com.example.filmera.ui.screen.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieVideoScreen(videoKey: String, navController: NavController) {
  val context = LocalContext.current
  val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Watch Trailer") },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
          }
        }
      )
    }
  ) { padding ->
    AndroidView(
      factory = { ctx ->
        val youTubePlayerView = YouTubePlayerView(ctx)
        lifecycleOwner.lifecycle.addObserver(youTubePlayerView)

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
          override fun onReady(youTubePlayer: YouTubePlayer) {
            youTubePlayer.loadVideo(videoKey, 0f)
          }
        })

        youTubePlayerView
      },
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    )
  }
}
