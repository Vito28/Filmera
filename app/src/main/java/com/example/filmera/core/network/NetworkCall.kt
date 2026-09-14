package com.example.filmera.core.network

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

suspend fun <T> safeNetworkCall(
  request: suspend () -> T,
): DataResult<T> =
  try {
    DataResult.Success(request())
  } catch (error: CancellationException) {
    throw error
  } catch (error: IOException) {
    DataResult.Error(AppError.NetworkUnavailable)
  } catch (error: HttpException) {
    DataResult.Error(error.toAppError())
  } catch (error: JsonParseException) {
    DataResult.Error(AppError.InvalidResponse)
  } catch (error: RuntimeException) {
    DataResult.Error(AppError.Unknown)
  }

private fun HttpException.toAppError(): AppError =
  when (code()) {
    401, 403 -> AppError.Unauthorized
    404 -> AppError.NotFound
    429 -> AppError.RateLimited
    in 500..599 -> AppError.ServerUnavailable
    else -> AppError.Unknown
  }
