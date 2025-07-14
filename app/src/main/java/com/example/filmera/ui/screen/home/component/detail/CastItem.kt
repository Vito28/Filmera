package com.example.filmera.ui.screen.home.component.detail

import android.widget.ImageView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.example.filmera.model.credit.Cast

@Composable
fun CastItem(cast: Cast) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.width(100.dp)
  ) {
    AndroidView(
      modifier = Modifier
        .size(100.dp)
        .clip(RoundedCornerShape(8.dp)),
      factory = { context ->
        ImageView(context).apply {
          scaleType = ImageView.ScaleType.CENTER_CROP
        }
      },
      update = { imageView ->
        val imageUrl = "https://image.tmdb.org/t/p/w185${cast.profile_path}"
        Glide.with(imageView.context)
          .load(imageUrl)
          .centerCrop()
          .into(imageView)
      }
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = cast.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    Text(text = cast.character, style = MaterialTheme.typography.labelSmall, maxLines = 1)
  }
}