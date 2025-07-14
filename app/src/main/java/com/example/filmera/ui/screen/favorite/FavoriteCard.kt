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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.example.filmera.model.Movie

@Composable
fun FavoriteCard(
  movie: Movie,
  onClick: () -> Unit,
  onDeleteClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp)
      .clickable { onClick() },
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Poster
      Box(
        modifier = Modifier
          .size(70.dp, 100.dp)
          .clip(RoundedCornerShape(8.dp))
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
              .into(imageView)
          }
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Info
      Column(
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = movie.title ?: "N/A",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Rating",
            tint = Color(0xFFFFC107),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = movie.vote_average?.let { String.format("%.1f", it) } ?: "N/A",
            fontSize = 13.sp,
            color = Color.Gray
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = movie.release_date?.split("-")?.firstOrNull() ?: "N/A",
            fontSize = 13.sp,
            color = Color.Gray
          )
        }
      }

      // Delete icon
      IconButton(onClick = onDeleteClick) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete",
          tint = Color.Red
        )
      }
    }
  }
}
