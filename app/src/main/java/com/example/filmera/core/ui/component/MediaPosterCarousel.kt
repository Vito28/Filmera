package com.example.filmera.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.ui.LazyLayoutKey

@Composable
fun MediaPosterCarousel(
  items: List<MediaItem>,
  favoriteKeys: Set<MediaKey>,
  onMediaClick: (MediaItem) -> Unit,
  onToggleFavorite: (MediaItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier,
    contentPadding = PaddingValues(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(
      items = items,
      key = { item -> LazyLayoutKey.media("media-poster-carousel", item) },
    ) { item ->
      MediaPosterCard(
        item = item,
        isFavorite = item.key in favoriteKeys,
        onClick = { onMediaClick(item) },
        onToggleFavorite = { onToggleFavorite(item) },
        modifier = Modifier.width(184.dp),
      )
    }
  }
}
