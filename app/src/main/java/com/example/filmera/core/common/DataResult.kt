package com.example.filmera.core.common

sealed interface DataResult<out T> {
  data class Success<T>(val value: T) : DataResult<T>
  data class Error(val error: AppError) : DataResult<Nothing>
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> =
  when (this) {
    is DataResult.Success -> DataResult.Success(transform(value))
    is DataResult.Error -> this
  }

fun <T> DataResult<T>.getOrNull(): T? =
  (this as? DataResult.Success)?.value
