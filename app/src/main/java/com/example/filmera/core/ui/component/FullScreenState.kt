package com.example.filmera.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.filmera.R
import com.example.filmera.core.common.AppError
import com.example.filmera.core.ui.messageResource

@Composable
fun LoadingState(
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    CircularProgressIndicator()
    Text(
      text = stringResource(R.string.loading_content),
      modifier = Modifier.padding(top = 16.dp),
      style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@Composable
fun ErrorState(
  error: AppError,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  StateMessage(
    icon = Icons.Outlined.CloudOff,
    title = stringResource(R.string.unable_to_load_content),
    message = stringResource(error.messageResource()),
    actionLabel = stringResource(R.string.retry),
    onAction = onRetry,
    modifier = modifier,
  )
}

@Composable
fun EmptyState(
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  icon: ImageVector = Icons.Outlined.MovieFilter,
) {
  StateMessage(
    icon = icon,
    title = title,
    message = message,
    modifier = modifier,
  )
}

@Composable
private fun StateMessage(
  icon: ImageVector,
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = title,
      modifier = Modifier.padding(top = 16.dp),
      style = MaterialTheme.typography.titleLarge,
      textAlign = TextAlign.Center,
    )
    Text(
      text = message,
      modifier = Modifier.padding(top = 8.dp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
    )
    if (actionLabel != null && onAction != null) {
      Button(
        onClick = onAction,
        modifier = Modifier.padding(top = 20.dp),
      ) {
        Text(actionLabel)
      }
    }
  }
}
