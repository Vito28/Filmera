package com.example.filmera.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthRoute(
  onAuthenticated: (AuthNextDestination) -> Unit,
  onBack: () -> Unit,
  initialMode: AuthMode = AuthMode.SIGN_IN,
  viewModel: AuthViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(viewModel, initialMode) {
    if (viewModel.uiState.value.mode != initialMode) {
      viewModel.onEvent(AuthEvent.ModeChanged(initialMode))
    }
  }

  LaunchedEffect(viewModel) {
    viewModel.effects.collect { effect ->
      when (effect) {
        is AuthEffect.Navigate -> onAuthenticated(effect.destination)
      }
    }
  }

  AuthScreen(
    state = state,
    onEvent = viewModel::onEvent,
    onBack = onBack,
  )
}
