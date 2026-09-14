package com.example.filmera.core.common

sealed interface AppError {
  data object MissingApiToken : AppError
  data object NetworkUnavailable : AppError
  data object Unauthorized : AppError
  data object NotFound : AppError
  data object RateLimited : AppError
  data object ServerUnavailable : AppError
  data object InvalidResponse : AppError
  data object Unknown : AppError
}
