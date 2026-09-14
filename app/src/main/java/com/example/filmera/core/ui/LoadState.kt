package com.example.filmera.core.ui

import com.example.filmera.core.common.AppError

sealed interface LoadState<out T> {
  data object Loading : LoadState<Nothing>
  data object Empty : LoadState<Nothing>
  data class Success<T>(val value: T) : LoadState<T>
  data class Error(val error: AppError) : LoadState<Nothing>
}
