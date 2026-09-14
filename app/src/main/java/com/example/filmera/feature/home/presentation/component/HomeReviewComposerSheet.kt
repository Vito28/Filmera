package com.example.filmera.feature.home.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.feature.home.presentation.HomeReviewComposerState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeReviewComposerSheetContent(
  state: HomeReviewComposerState,
  onRatingSelected: (Int) -> Unit,
  onHeadlineChanged: (String) -> Unit,
  onBodyChanged: (String) -> Unit,
  onSpoilerChanged: (Boolean) -> Unit,
  onPublish: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val media = state.media ?: return
  Column(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(start = 18.dp, end = 18.dp, bottom = 20.dp),
    verticalArrangement = Arrangement.spacedBy(15.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        stringResource(R.string.community_write_review_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      Text(
        stringResource(R.string.community_write_review_subtitle),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Row(
        modifier = Modifier.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TmdbImage(
          path = media.posterPath ?: media.backdropPath,
          contentDescription = null,
          modifier = Modifier
            .size(width = 52.dp, height = 70.dp)
            .clip(RoundedCornerShape(12.dp)),
          size = "w185",
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            media.title,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = media.releaseDate?.take(4).orEmpty(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
    }
    Text(
      stringResource(R.string.community_your_rating),
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
    )
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(7.dp),
      verticalArrangement = Arrangement.spacedBy(7.dp),
      maxItemsInEachRow = 5,
    ) {
      (1..10).forEach { rating ->
        val selected = state.rating == rating
        Surface(
          onClick = { onRatingSelected(rating) },
          modifier = Modifier.size(48.dp),
          enabled = !state.isSubmitting,
          shape = RoundedCornerShape(15.dp),
          color = if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.surfaceContainerHigh,
          contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurface,
          border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
          ),
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text(rating.toString(), fontWeight = FontWeight.Bold)
          }
        }
      }
    }
    if (state.rating == null) {
      Text(
        stringResource(R.string.community_rating_required),
        color = MaterialTheme.filmeraColors.warning,
        style = MaterialTheme.typography.labelMedium,
      )
    }
    OutlinedTextField(
      value = state.headline,
      onValueChange = onHeadlineChanged,
      modifier = Modifier.fillMaxWidth(),
      enabled = !state.isSubmitting,
      label = { Text(stringResource(R.string.community_review_headline)) },
      supportingText = {
        Text(stringResource(R.string.community_optional_character_count, state.headline.length, 120))
      },
      isError = state.headlineInvalid,
      singleLine = true,
      shape = RoundedCornerShape(18.dp),
    )
    OutlinedTextField(
      value = state.body,
      onValueChange = onBodyChanged,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 150.dp, max = 260.dp),
      enabled = !state.isSubmitting,
      label = { Text(stringResource(R.string.community_your_review)) },
      supportingText = {
        Text(stringResource(R.string.community_review_character_count, state.body.length))
      },
      isError = state.bodyInvalid,
      minLines = 6,
      maxLines = 12,
      shape = RoundedCornerShape(18.dp),
    )
    if (state.bodyInvalid) {
      Text(
        stringResource(R.string.community_review_body_error),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.labelMedium,
      )
    }
    Surface(
      onClick = { onSpoilerChanged(!state.containsSpoilers) },
      enabled = !state.isSubmitting,
      shape = RoundedCornerShape(16.dp),
      color = if (state.containsSpoilers) MaterialTheme.filmeraColors.warning.copy(alpha = 0.1f)
      else Color.Transparent,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Checkbox(
          checked = state.containsSpoilers,
          onCheckedChange = onSpoilerChanged,
          enabled = !state.isSubmitting,
        )
        Text(stringResource(R.string.community_contains_spoilers))
      }
    }
    Button(
      onClick = onPublish,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 54.dp),
      enabled = state.rating != null && state.body.trim().length >= 20 && !state.isSubmitting,
      shape = RoundedCornerShape(18.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ),
    ) {
      if (state.isSubmitting) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          color = MaterialTheme.colorScheme.onPrimary,
          strokeWidth = 2.dp,
        )
      } else {
        Icon(Icons.Filled.Star, contentDescription = null)
        Text(
          stringResource(R.string.community_publish_review),
          modifier = Modifier.padding(start = 8.dp),
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
