package com.example.filmera.core.ui

import androidx.annotation.StringRes
import com.example.filmera.R
import com.example.filmera.core.common.AppError

@StringRes
fun AppError.messageResource(): Int =
  when (this) {
    AppError.MissingApiToken -> R.string.error_missing_api_token
    AppError.NetworkUnavailable -> R.string.error_network_unavailable
    AppError.Unauthorized -> R.string.error_unauthorized
    AppError.NotFound -> R.string.error_not_found
    AppError.RateLimited -> R.string.error_rate_limited
    AppError.ServerUnavailable -> R.string.error_server_unavailable
    AppError.InvalidResponse -> R.string.error_invalid_response
    AppError.Unknown -> R.string.error_unknown
  }
