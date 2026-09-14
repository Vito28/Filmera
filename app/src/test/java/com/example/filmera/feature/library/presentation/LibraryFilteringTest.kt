package com.example.filmera.feature.library.presentation

import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.feature.library.domain.LibraryItem
import com.example.filmera.feature.library.domain.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFilteringTest {
  @Test
  fun `filter keeps selected tab media type and local query`() {
    val movie = libraryItem(
      id = 1,
      type = MediaType.MOVIE,
      title = "Dune",
      status = WatchStatus.WATCHLIST,
    )
    val tv = libraryItem(
      id = 2,
      type = MediaType.TV_SHOW,
      title = "Dune Prophecy",
      status = WatchStatus.WATCHLIST,
    )
    val completed = libraryItem(
      id = 3,
      type = MediaType.MOVIE,
      title = "Dune Part Two",
      status = WatchStatus.COMPLETED,
    )

    val result = filterAndSortLibrary(
      items = listOf(movie, tv, completed),
      tab = LibraryTab.WATCHLIST,
      mediaFilter = LibraryMediaFilter.MOVIES,
      query = "dune",
      sort = LibrarySort.TITLE_ASCENDING,
    )

    assertEquals(listOf(movie), result)
  }

  @Test
  fun `favorites tab does not depend on watch status`() {
    val favoriteWithoutStatus = libraryItem(
      id = 1,
      type = MediaType.MOVIE,
      title = "Arrival",
      status = WatchStatus.NONE,
      isFavorite = true,
    )

    val result = filterAndSortLibrary(
      items = listOf(favoriteWithoutStatus),
      tab = LibraryTab.FAVORITES,
      mediaFilter = LibraryMediaFilter.ALL,
      query = "",
      sort = LibrarySort.RECENTLY_ADDED,
    )

    assertEquals(listOf(favoriteWithoutStatus), result)
  }
}

private fun libraryItem(
  id: Int,
  type: MediaType,
  title: String,
  status: WatchStatus,
  isFavorite: Boolean = false,
): LibraryItem = LibraryItem(
  media = MediaItem(
    id = id,
    type = type,
    title = title,
    originalTitle = title,
    overview = "",
    posterPath = null,
    backdropPath = null,
    releaseDate = null,
    voteAverage = id.toDouble(),
    voteCount = 0,
    popularity = 0.0,
    adult = false,
    originalLanguage = "en",
    genreIds = emptyList(),
  ),
  watchStatus = status,
  isFavorite = isFavorite,
  addedAt = id.toLong(),
  updatedAt = id.toLong(),
)
