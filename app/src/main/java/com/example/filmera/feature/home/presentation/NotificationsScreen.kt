package com.example.filmera.feature.home.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.filmera.R
import com.example.filmera.core.ui.component.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.notifications_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = stringResource(R.string.browse_back),
            )
          }
        },
      )
    },
  ) { innerPadding ->
    EmptyState(
      icon = Icons.Outlined.NotificationsNone,
      title = stringResource(R.string.notifications_empty_title),
      message = stringResource(R.string.notifications_empty_message),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    )
  }
}
