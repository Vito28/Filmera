package com.example.filmera.feature.home.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.feature.home.presentation.HomeRatingSheetState

@Composable
fun HomeRatingBottomSheetContent(
  state: HomeRatingSheetState,
  onRatingSelected: (Int) -> Unit,
  onSubmit: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Surface(
      modifier = Modifier.size(58.dp),
      shape = CircleShape,
      color = MaterialTheme.filmeraColors.rating.copy(alpha = 0.14f),
      border = BorderStroke(1.dp, MaterialTheme.filmeraColors.rating.copy(alpha = 0.42f)),
    ) {
      Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = null,
        modifier = Modifier.padding(14.dp),
        tint = MaterialTheme.filmeraColors.rating,
      )
    }
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Text(
        text = stringResource(R.string.community_rate_sheet_title, state.mediaTitle),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = stringResource(R.string.community_rate_sheet_prompt),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
      verticalArrangement = Arrangement.spacedBy(9.dp),
      maxItemsInEachRow = 5,
    ) {
      (1..10).forEach { rating ->
        RatingScore(
          rating = rating,
          selected = state.selectedRating == rating,
          enabled = !state.isSubmitting,
          onClick = { onRatingSelected(rating) },
        )
      }
    }
    Text(
      text = state.selectedRating?.let { rating ->
        stringResource(R.string.community_your_rating_value, rating)
      } ?: stringResource(R.string.community_rating_not_selected),
      color = if (state.selectedRating != null) MaterialTheme.colorScheme.primary
      else MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
    )
    Button(
      onClick = onSubmit,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp),
      enabled = state.selectedRating != null && !state.isSubmitting,
      shape = RoundedCornerShape(18.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ),
    ) {
      if (state.isSubmitting) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          strokeWidth = 2.dp,
          color = MaterialTheme.colorScheme.onPrimary,
        )
      } else {
        Text(
          text = if (state.existingRating == null) {
            stringResource(R.string.community_save_rating)
          } else {
            stringResource(R.string.community_update_rating)
          },
          fontWeight = FontWeight.Bold,
        )
      }
    }
    if (state.existingRating != null) {
      OutlinedButton(
        onClick = onRemove,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp),
        enabled = !state.isSubmitting,
        shape = RoundedCornerShape(18.dp),
      ) {
        Text(stringResource(R.string.community_remove_rating))
      }
    }
  }
}

@Composable
private fun RatingScore(
  rating: Int,
  selected: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  val background by animateColorAsState(
    targetValue = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceContainerHigh,
    animationSpec = tween(200),
    label = "rating score background",
  )
  val scale by animateFloatAsState(
    targetValue = if (selected) 1.07f else 1f,
    animationSpec = tween(150),
    label = "rating score scale",
  )
  Surface(
    onClick = {
      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      onClick()
    },
    modifier = Modifier
      .size(52.dp)
      .scale(scale)
      .semantics {
        this.selected = selected
        contentDescription = "$rating out of 10"
      },
    enabled = enabled,
    shape = RoundedCornerShape(17.dp),
    color = background,
    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface,
    border = BorderStroke(
      1.dp,
      if (selected) MaterialTheme.colorScheme.primary
      else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.54f),
    ),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = rating.toString(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}
