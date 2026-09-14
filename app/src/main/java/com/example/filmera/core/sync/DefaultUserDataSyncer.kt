package com.example.filmera.core.sync

import com.example.filmera.core.database.LibraryItemDao
import com.example.filmera.core.database.LibraryItemEntity
import com.example.filmera.core.database.RecentSearchDao
import com.example.filmera.core.database.RecentSearchEntity
import com.example.filmera.core.database.SEARCH_CLEAR_METADATA_KEY
import com.example.filmera.core.database.SYNC_STATE_PENDING
import com.example.filmera.core.database.SYNC_STATE_SYNCED
import com.example.filmera.core.database.SyncMetadataDao
import com.example.filmera.core.database.UserPreferenceDao
import com.example.filmera.core.database.UserPreferenceEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class DefaultUserDataSyncer @Inject constructor(
  private val currentUserProvider: CurrentUserProvider,
  private val preferenceDao: UserPreferenceDao,
  private val libraryDao: LibraryItemDao,
  private val recentSearchDao: RecentSearchDao,
  private val syncMetadataDao: SyncMetadataDao,
  private val remote: UserDataSyncRemoteSource,
) : UserDataSyncer {
  private val syncMutex = Mutex()

  override suspend fun syncNow() {
    val ownerId = currentUserProvider.currentUserId() ?: return
    syncMutex.withLock {
      claimLegacyData(ownerId)
      pushPendingData(ownerId)
      pullCloudData(ownerId)
    }
  }

  private suspend fun claimLegacyData(ownerId: String) {
    preferenceDao.claimLegacy(ownerId)
    libraryDao.claimLegacy(ownerId)
    recentSearchDao.claimLegacy(ownerId)
  }

  private suspend fun pushPendingData(ownerId: String) {
    preferenceDao.pending(ownerId)?.let { pending ->
      mergePreference(ownerId, remote.syncPreference(pending))
    }

    libraryDao.pending(ownerId).forEach { pending ->
      val remoteMediaId = pending.remoteMediaId ?: remote.ensureMedia(pending)
      mergeLibrary(ownerId, remote.syncLibrary(pending, remoteMediaId))
    }

    syncMetadataDao.pending(ownerId)?.let { clearRequest ->
      val serverTimestamp = remote.clearSearches(clearRequest.timestampValue)
      syncMetadataDao.markSynced(
        ownerId = ownerId,
        expectedTimestamp = clearRequest.timestampValue,
        serverTimestamp = serverTimestamp,
      )
    }

    recentSearchDao.pending(ownerId).forEach { pending ->
      mergeSearch(ownerId, remote.syncSearch(pending))
    }
  }

  private suspend fun pullCloudData(ownerId: String) {
    remote.getPreferences()?.let { mergePreference(ownerId, it) }
    remote.getLibrary().forEach { mergeLibrary(ownerId, it) }
    remote.getSearches().forEach { mergeSearch(ownerId, it) }
  }

  private suspend fun mergePreference(ownerId: String, cloud: CloudPreferenceDto) {
    val remoteUpdatedAt = cloud.clientUpdatedAt.asEpochMillis()
    val local = preferenceDao.getProfile(ownerId)
    if (local?.syncState == SYNC_STATE_PENDING && local.updatedAt > remoteUpdatedAt) return

    preferenceDao.upsert(
      UserPreferenceEntity(
        ownerId = ownerId,
        preferredMediaTypes = cloud.preferredMediaTypes.sorted().joinToString(","),
        preferredGenreIds = cloud.preferredGenreIds.sorted().joinToString(","),
        preferredCountries = cloud.preferredCountries.sorted().joinToString(","),
        preferredMovieIds = cloud.preferredMovieIds.sorted().joinToString(","),
        preferredTvIds = cloud.preferredTvIds.sorted().joinToString(","),
        preferredPersonIds = cloud.preferredPersonIds.sorted().joinToString(","),
        preferredLanguageCodes = cloud.preferredLanguageCodes.sorted().joinToString(","),
        onboardingCompleted = cloud.onboardingCompleted,
        updatedAt = remoteUpdatedAt,
        syncState = SYNC_STATE_SYNCED,
      ),
    )
  }

  private suspend fun mergeLibrary(ownerId: String, cloud: CloudLibraryDto) {
    val localMediaType = when (cloud.mediaType) {
      "movie" -> "MOVIE"
      "tv" -> "TV_SHOW"
      else -> return
    }
    val remoteUpdatedAt = cloud.clientUpdatedAt.asEpochMillis()
    val local = libraryDao.find(ownerId, cloud.tmdbId, localMediaType)
    if (local?.syncState == SYNC_STATE_PENDING && local.updatedAt > remoteUpdatedAt) return

    libraryDao.upsert(
      LibraryItemEntity(
        ownerId = ownerId,
        mediaId = cloud.tmdbId,
        mediaType = localMediaType,
        remoteMediaId = cloud.mediaId,
        title = cloud.title,
        originalTitle = local?.originalTitle ?: cloud.title,
        posterPath = cloud.posterPath,
        backdropPath = local?.backdropPath,
        overview = local?.overview.orEmpty(),
        releaseDate = cloud.releaseDate,
        voteAverage = local?.voteAverage ?: 0.0,
        voteCount = local?.voteCount ?: 0,
        popularity = local?.popularity ?: 0.0,
        originalLanguage = cloud.originalLanguage ?: local?.originalLanguage.orEmpty(),
        watchStatus = cloud.watchStatus.uppercase(),
        isFavorite = cloud.isFavorite,
        addedAt = cloud.addedAt.asEpochMillis(),
        updatedAt = remoteUpdatedAt,
        isDeleted = cloud.deletedAt != null,
        syncState = SYNC_STATE_SYNCED,
      ),
    )
  }

  private suspend fun mergeSearch(ownerId: String, cloud: CloudSearchDto) {
    val remoteUpdatedAt = cloud.clientUpdatedAt.asEpochMillis()
    val local = recentSearchDao.find(ownerId, cloud.normalizedQuery)
    if (local?.syncState == SYNC_STATE_PENDING && local.updatedAt > remoteUpdatedAt) return

    recentSearchDao.insert(
      RecentSearchEntity(
        ownerId = ownerId,
        normalizedQuery = cloud.normalizedQuery,
        query = cloud.query,
        searchedAt = cloud.searchedAt.asEpochMillis(),
        updatedAt = remoteUpdatedAt,
        isDeleted = cloud.deletedAt != null,
        syncState = SYNC_STATE_SYNCED,
      ),
    )
  }
}
