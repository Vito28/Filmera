package com.example.filmera.core.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InlineMessageBanner(
  message: String,
  modifier: Modifier = Modifier,
  isError: Boolean = false,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = if (isError) {
      MaterialTheme.colorScheme.errorContainer
    } else {
      MaterialTheme.colorScheme.secondaryContainer
    },
    shape = MaterialTheme.shapes.medium,
  ) {
    Text(
      text = message,
      modifier = Modifier.padding(16.dp),
      color = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
      } else {
        MaterialTheme.colorScheme.onSecondaryContainer
      },
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}
