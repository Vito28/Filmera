package com.example.filmera.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.filmera.R
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType

@Composable
fun MediaPosterCard(
  item: MediaItem,
  isFavorite: Boolean,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier,
    shape = MaterialTheme.shapes.large,
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(2f / 3f),
      ) {
        TmdbImage(
          path = item.posterPath,
          contentDescription = stringResource(
            R.string.poster_content_description,
            item.title,
          ),
          modifier = Modifier.matchParentSize(),
        )
        IconButton(
          onClick = onToggleFavorite,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
        ) {
          Icon(
            imageVector = if (isFavorite) {
              Icons.Default.Favorite
            } else {
              Icons.Outlined.FavoriteBorder
            },
            contentDescription = stringResource(
              if (isFavorite) {
                R.string.remove_from_favorites
              } else {
                R.string.add_to_favorites
              },
            ),
            tint = if (isFavorite) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurface
            },
          )
        }
      }
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = item.title,
          minLines = 1,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = stringResource(R.string.rating),
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Text(
              text = stringResource(R.string.rating_value, item.voteAverage),
              modifier = Modifier.padding(start = 4.dp),
              style = MaterialTheme.typography.labelMedium,
            )
          }
          Text(
            text = if (item.type == MediaType.TV_SHOW) {
              stringResource(R.string.media_type_tv)
            } else {
              item.releaseDate?.take(4)
                ?: stringResource(R.string.not_available_short)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
    }
  }
}
