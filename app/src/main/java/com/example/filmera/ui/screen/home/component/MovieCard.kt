package com.example.filmera.ui.screen.home.components

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.example.filmera.model.Movie
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder

@Composable
fun MovieCard(
  movie: Movie,
  modifier: Modifier = Modifier,
  isBookmarked: Boolean = false,
  onClick: () -> Unit = {},
  onBookmarkClick: (Movie) -> Unit = {}
) {
  Card(
    modifier = modifier
      .padding(8.dp)
      .width(250.dp)
      .clickable { onClick() },
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth()
    ) {
      // Image Section with padding
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 14.dp, start = 14.dp, end = 14.dp, bottom = 10.dp)
          .aspectRatio(2f / 3f)
          .clip(RoundedCornerShape(12.dp))
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

        // Bookmark icon with better positioning
        IconButton(
          onClick = { onBookmarkClick(movie) },
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp)
            .size(40.dp)
            .background(
              Color.Black.copy(alpha = 0.3f),
              shape = RoundedCornerShape(50)
            )
        ) {
          Icon(
            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Bookmark",
            tint = if (isBookmarked) Color.Red else Color.White,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      // Content Section
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 0.dp, start = 12.dp, end = 12.dp, bottom = 8.dp)
      ) {
        // Title
        Text(
          text = movie.title ?: "N/A",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontSize = 16.sp
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Overview
        Text(
          text = movie.overview.take(60) + if (movie.overview.length > 60) "..." else "N/A",
          style = MaterialTheme.typography.bodySmall.copy(
            color = Color.Gray,
            fontSize = 12.sp,
            lineHeight = 16.sp
          ),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Metrics Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          MetricItem(
            icon = Icons.Default.Star,
            iconTint = Color(0xFFFFC107),
            value = movie.vote_average?.let { String.format("%.1f", it) } ?: "N/A",
            label = "Rating"
          )
          Divider(color = Color.LightGray, modifier = Modifier.height(20.dp).width(1.dp))

          MetricItem(
            value = movie.popularity?.let { String.format("%.0f", it) } ?: "N/A",
            label = "Popularity"
          )
          Divider(color = Color.LightGray, modifier = Modifier.height(20.dp).width(1.dp))

          MetricItem(
            value = movie.release_date?.split("-")?.firstOrNull() ?: "N/A",
            label = "Year"
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }
}

@Composable
fun MetricItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  iconTint: Color = Color.Unspecified,
  value: String,
  label: String
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.width(IntrinsicSize.Min)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
    ) {
      icon?.let {
        Icon(
          imageVector = it,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
      }
      Text(
        text = value,
        style = MaterialTheme.typography.bodySmall.copy(
          fontWeight = FontWeight.Bold,
          color = Color.Black,
          fontSize = 12.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        color = Color.Gray,
        fontSize = 10.sp
      ),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}