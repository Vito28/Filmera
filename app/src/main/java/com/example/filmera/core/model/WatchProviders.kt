package com.example.filmera.core.model

data class WatchProvider(
  val id: Int,
  val name: String,
  val logoPath: String?,
  val displayPriority: Int,
)

data class WatchProviderResult(
  val regionCode: String,
  val link: String?,
  val stream: List<WatchProvider> = emptyList(),
  val free: List<WatchProvider> = emptyList(),
  val ads: List<WatchProvider> = emptyList(),
  val rent: List<WatchProvider> = emptyList(),
  val buy: List<WatchProvider> = emptyList(),
) {
  val isEmpty: Boolean
    get() = stream.isEmpty() && free.isEmpty() && ads.isEmpty() && rent.isEmpty() && buy.isEmpty()
}
