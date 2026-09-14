package com.example.filmera.core.network

import java.net.URI

data class TmdbConfig(
  val bearerToken: String,
  val baseUrl: String = "https://api.themoviedb.org/3/",
) {
  init {
    val uri = runCatching { URI(baseUrl) }.getOrNull()
    require(
      uri?.scheme == "https" && uri.host == TRUSTED_TMDB_HOST,
    ) {
      "TMDB base URL must use the trusted HTTPS API host"
    }
  }

  val isConfigured: Boolean = bearerToken.isNotBlank()

  val trustedHost: String = TRUSTED_TMDB_HOST

  private companion object {
    const val TRUSTED_TMDB_HOST = "api.themoviedb.org"
  }
}
