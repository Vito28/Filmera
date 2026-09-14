package com.example.filmera.core.network

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class NetworkCallTest {
  @Test
  fun `io exception maps to network unavailable`() = runBlocking {
    val result = safeNetworkCall<Int> { throw IOException("offline") }

    assertEquals(DataResult.Error(AppError.NetworkUnavailable), result)
  }

  @Test
  fun `unauthorized response maps to unauthorized error`() = runBlocking {
    val response = Response.error<Int>(401, "".toResponseBody())

    val result = safeNetworkCall<Int> { throw HttpException(response) }

    assertEquals(DataResult.Error(AppError.Unauthorized), result)
  }

  @Test
  fun `invalid json maps to invalid response`() = runBlocking {
    val result = safeNetworkCall<Int> { throw JsonParseException("invalid") }

    assertEquals(DataResult.Error(AppError.InvalidResponse), result)
  }

  @Test
  fun `cancellation is never converted to an app error`() = runBlocking {
    try {
      safeNetworkCall<Int> { throw CancellationException("cancelled") }
      fail("CancellationException should be rethrown")
    } catch (_: CancellationException) {
      // Expected: structured concurrency cancellation must propagate.
    }
  }
}
