package com.example.filmera.feature.library.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.feature.library.domain.LibraryItem

@Composable
internal fun LibraryItemCard(
  item: LibraryItem,
  primaryActionLabel: String,
  statusLabel: String?,
  isMutating: Boolean,
  onClick: () -> Unit,
  onPrimaryAction: () -> Unit,
  onToggleFavorite: () -> Unit,
  onMore: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TmdbImage(
        path = item.media.posterPath,
        contentDescription = stringResource(
          R.string.poster_content_description,
          item.media.title,
        ),
        modifier = Modifier
          .size(width = 84.dp, height = 126.dp)
          .clip(RoundedCornerShape(12.dp)),
        size = "w342",
      )
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(start = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(verticalAlignment = Alignment.Top) {
          Text(
            text = item.media.title,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
          IconButton(
            onClick = onMore,
            enabled = !isMutating,
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = stringResource(
                R.string.library_more_actions,
                item.media.title,
              ),
            )
          }
        }
        Text(
          text = stringResource(
            R.string.media_type_year,
            stringResource(
              if (item.media.type == MediaType.TV_SHOW) {
                R.string.media_type_tv_show
              } else {
                R.string.media_type_movie
              },
            ),
            item.media.releaseDate?.take(4) ?: stringResource(R.string.not_available_short),
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodySmall,
        )
        statusLabel?.let { label ->
          Text(
            text = label,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.tertiary,
          )
          Text(
            text = stringResource(R.string.rating_value, item.media.voteAverage),
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelMedium,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FilledTonalButton(
            onClick = onPrimaryAction,
            enabled = !isMutating,
          ) {
            Text(
              text = primaryActionLabel,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(
            onClick = onToggleFavorite,
            enabled = !isMutating,
          ) {
            Icon(
              imageVector = if (item.isFavorite) {
                Icons.Default.Favorite
              } else {
                Icons.Outlined.FavoriteBorder
              },
              contentDescription = stringResource(
                if (item.isFavorite) {
                  R.string.remove_from_favorites
                } else {
                  R.string.add_to_favorites
                },
              ),
              tint = if (item.isFavorite) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            )
          }
        }
      }
    }
  }
}
