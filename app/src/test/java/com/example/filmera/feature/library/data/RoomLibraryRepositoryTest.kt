package com.example.filmera.feature.library.data

import com.example.filmera.core.database.LibraryItemDao
import com.example.filmera.core.database.LibraryItemEntity
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.feature.library.domain.WatchStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomLibraryRepositoryTest {
  @Test
  fun `favorite and watch status are independent`() = runBlocking {
    val dao = FakeLibraryItemDao()
    val repository = RoomLibraryRepository(dao)
    val media = sampleMedia()

    repository.setFavorite(media, true)
    repository.setWatchStatus(media, WatchStatus.COMPLETED)

    val saved = repository.observeLibrary().first().single()
    assertTrue(saved.isFavorite)
    assertEquals(WatchStatus.COMPLETED, saved.watchStatus)

    repository.setFavorite(media, false)

    val completed = repository.observeLibrary().first().single()
    assertFalse(completed.isFavorite)
    assertEquals(WatchStatus.COMPLETED, completed.watchStatus)
  }

  @Test
  fun `item is deleted only when it has no status and is not favorite`() = runBlocking {
    val dao = FakeLibraryItemDao()
    val repository = RoomLibraryRepository(dao)
    val media = sampleMedia()

    repository.setFavorite(media, true)
    repository.setWatchStatus(media, WatchStatus.WATCHLIST)
    repository.setFavorite(media, false)

    assertEquals(1, repository.observeLibrary().first().size)

    repository.setWatchStatus(media, WatchStatus.NONE)

    assertTrue(repository.observeLibrary().first().isEmpty())
  }

  @Test
  fun `movie and tv show with the same TMDB id remain separate`() = runBlocking {
    val repository = RoomLibraryRepository(FakeLibraryItemDao())
    val movie = sampleMedia(id = 7, type = MediaType.MOVIE)
    val tvShow = sampleMedia(id = 7, type = MediaType.TV_SHOW)

    repository.setFavorite(movie, true)
    repository.setFavorite(tvShow, true)
    repository.setFavorite(tvShow, false)

    val remaining = repository.observeLibrary().first().single()
    assertEquals(movie.key, remaining.media.key)
    assertTrue(remaining.isFavorite)
  }

  private class FakeLibraryItemDao : LibraryItemDao {
    private val items = MutableStateFlow<List<LibraryItemEntity>>(emptyList())

    override fun observeLibrary(ownerId: String): Flow<List<LibraryItemEntity>> =
      items.map { current -> current.filter { it.ownerId == ownerId && !it.isDeleted } }

    override suspend fun find(
      ownerId: String,
      mediaId: Int,
      mediaType: String,
    ): LibraryItemEntity? = items.value.firstOrNull {
      it.ownerId == ownerId && it.mediaId == mediaId && it.mediaType == mediaType
    }

    override suspend fun upsert(item: LibraryItemEntity) {
      items.value = items.value.filterNot {
        it.ownerId == item.ownerId &&
          it.mediaId == item.mediaId &&
          it.mediaType == item.mediaType
      } + item
    }

    override suspend fun pending(ownerId: String): List<LibraryItemEntity> =
      items.value.filter { it.ownerId == ownerId && it.syncState == "PENDING" }

    override suspend fun markSynced(
      ownerId: String,
      mediaId: Int,
      mediaType: String,
      expectedUpdatedAt: Long,
      remoteMediaId: Long,
    ): Int = 0

    override suspend fun copyLegacy(ownerId: String, legacyOwnerId: String) = Unit

    override suspend fun deleteLegacy(legacyOwnerId: String) = Unit
  }
}

private fun sampleMedia(
  id: Int = 42,
  type: MediaType = MediaType.TV_SHOW,
): MediaItem = MediaItem(
  id = id,
  type = type,
  title = "Example",
  originalTitle = "Example",
  overview = "",
  posterPath = null,
  backdropPath = null,
  releaseDate = "2026-01-01",
  voteAverage = 8.0,
  voteCount = 10,
  popularity = 20.0,
  adult = false,
  originalLanguage = "en",
  genreIds = emptyList(),
)
