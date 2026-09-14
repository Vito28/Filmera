package com.example.filmera.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    LibraryItemEntity::class,
    RecentSearchEntity::class,
    UserPreferenceEntity::class,
    RecommendationFeedbackEntity::class,
    SyncMetadataEntity::class,
  ],
  version = 7,
  exportSchema = true,
)
abstract class FilmeraDatabase : RoomDatabase() {
  abstract fun libraryItemDao(): LibraryItemDao
  abstract fun recentSearchDao(): RecentSearchDao
  abstract fun userPreferenceDao(): UserPreferenceDao
  abstract fun recommendationFeedbackDao(): RecommendationFeedbackDao
  abstract fun syncMetadataDao(): SyncMetadataDao

  companion object {
    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS favorites_new (
            mediaId INTEGER NOT NULL,
            mediaType TEXT NOT NULL,
            title TEXT NOT NULL,
            originalTitle TEXT NOT NULL,
            posterPath TEXT,
            backdropPath TEXT,
            overview TEXT NOT NULL,
            releaseDate TEXT,
            voteAverage REAL NOT NULL,
            voteCount INTEGER NOT NULL,
            popularity REAL NOT NULL,
            originalLanguage TEXT NOT NULL,
            PRIMARY KEY(mediaId, mediaType)
          )
          """.trimIndent(),
        )
        db.execSQL(
          """
          INSERT INTO favorites_new (
            mediaId, mediaType, title, originalTitle, posterPath, backdropPath,
            overview, releaseDate, voteAverage, voteCount, popularity, originalLanguage
          )
          SELECT id, 'MOVIE', title, title, posterPath, NULL,
            overview, NULL, 0.0, 0, 0.0, 'en'
          FROM favorites
          """.trimIndent(),
        )
        db.execSQL("DROP TABLE favorites")
        db.execSQL("ALTER TABLE favorites_new RENAME TO favorites")
      }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS recent_searches (
            query TEXT NOT NULL,
            searchedAt INTEGER NOT NULL,
            PRIMARY KEY(query)
          )
          """.trimIndent(),
        )
      }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS library_items (
            mediaId INTEGER NOT NULL,
            mediaType TEXT NOT NULL,
            title TEXT NOT NULL,
            originalTitle TEXT NOT NULL,
            posterPath TEXT,
            backdropPath TEXT,
            overview TEXT NOT NULL,
            releaseDate TEXT,
            voteAverage REAL NOT NULL,
            voteCount INTEGER NOT NULL,
            popularity REAL NOT NULL,
            originalLanguage TEXT NOT NULL,
            watchStatus TEXT NOT NULL,
            isFavorite INTEGER NOT NULL,
            addedAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            PRIMARY KEY(mediaId, mediaType)
          )
          """.trimIndent(),
        )
        db.execSQL(
          """
          INSERT INTO library_items (
            mediaId, mediaType, title, originalTitle, posterPath, backdropPath,
            overview, releaseDate, voteAverage, voteCount, popularity, originalLanguage,
            watchStatus, isFavorite, addedAt, updatedAt
          )
          SELECT
            mediaId, mediaType, title, originalTitle, posterPath, backdropPath,
            overview, releaseDate, voteAverage, voteCount, popularity, originalLanguage,
            'NONE', 1,
            CAST(strftime('%s', 'now') AS INTEGER) * 1000,
            CAST(strftime('%s', 'now') AS INTEGER) * 1000
          FROM favorites
          """.trimIndent(),
        )
        db.execSQL("DROP TABLE favorites")
      }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS user_preferences (
            profileId INTEGER NOT NULL,
            preferredMediaTypes TEXT NOT NULL,
            preferredGenreIds TEXT NOT NULL,
            preferredCountries TEXT NOT NULL,
            preferredMovieIds TEXT NOT NULL,
            preferredTvIds TEXT NOT NULL,
            preferredPersonIds TEXT NOT NULL,
            preferredLanguageCodes TEXT NOT NULL,
            onboardingCompleted INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            PRIMARY KEY(profileId)
          )
          """.trimIndent(),
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS recommendation_feedback (
            mediaId INTEGER NOT NULL,
            mediaType TEXT NOT NULL,
            action TEXT NOT NULL,
            updatedAt INTEGER NOT NULL,
            PRIMARY KEY(mediaId, mediaType)
          )
          """.trimIndent(),
        )
      }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS library_items_new (
            ownerId TEXT NOT NULL,
            mediaId INTEGER NOT NULL,
            mediaType TEXT NOT NULL,
            remoteMediaId INTEGER,
            title TEXT NOT NULL,
            originalTitle TEXT NOT NULL,
            posterPath TEXT,
            backdropPath TEXT,
            overview TEXT NOT NULL,
            releaseDate TEXT,
            voteAverage REAL NOT NULL,
            voteCount INTEGER NOT NULL,
            popularity REAL NOT NULL,
            originalLanguage TEXT NOT NULL,
            watchStatus TEXT NOT NULL,
            isFavorite INTEGER NOT NULL,
            addedAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            isDeleted INTEGER NOT NULL,
            syncState TEXT NOT NULL,
            PRIMARY KEY(ownerId, mediaId, mediaType)
          )
          """.trimIndent(),
        )
        db.execSQL(
          """
          INSERT INTO library_items_new (
            ownerId, mediaId, mediaType, remoteMediaId, title, originalTitle,
            posterPath, backdropPath, overview, releaseDate, voteAverage, voteCount,
            popularity, originalLanguage, watchStatus, isFavorite, addedAt, updatedAt,
            isDeleted, syncState
          )
          SELECT
            '$LEGACY_OWNER_ID', mediaId, mediaType, NULL, title, originalTitle,
            posterPath, backdropPath, overview, releaseDate, voteAverage, voteCount,
            popularity, originalLanguage, watchStatus, isFavorite, addedAt, updatedAt,
            0, '$SYNC_STATE_PENDING'
          FROM library_items
          """.trimIndent(),
        )
        db.execSQL("DROP TABLE library_items")
        db.execSQL("ALTER TABLE library_items_new RENAME TO library_items")

        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS user_preferences_new (
            ownerId TEXT NOT NULL,
            preferredMediaTypes TEXT NOT NULL,
            preferredGenreIds TEXT NOT NULL,
            preferredCountries TEXT NOT NULL,
            preferredMovieIds TEXT NOT NULL,
            preferredTvIds TEXT NOT NULL,
            preferredPersonIds TEXT NOT NULL,
            preferredLanguageCodes TEXT NOT NULL,
            onboardingCompleted INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            syncState TEXT NOT NULL,
            PRIMARY KEY(ownerId)
          )
          """.trimIndent(),
        )
        db.execSQL(
          """
          INSERT INTO user_preferences_new (
            ownerId, preferredMediaTypes, preferredGenreIds, preferredCountries,
            preferredMovieIds, preferredTvIds, preferredPersonIds,
            preferredLanguageCodes, onboardingCompleted, updatedAt, syncState
          )
          SELECT
            '$LEGACY_OWNER_ID', preferredMediaTypes, preferredGenreIds,
            preferredCountries, preferredMovieIds, preferredTvIds,
            preferredPersonIds, preferredLanguageCodes, onboardingCompleted,
            updatedAt, '$SYNC_STATE_PENDING'
          FROM user_preferences
          WHERE profileId = 1
          """.trimIndent(),
        )
        db.execSQL("DROP TABLE user_preferences")
        db.execSQL("ALTER TABLE user_preferences_new RENAME TO user_preferences")

        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS recent_searches_new (
            ownerId TEXT NOT NULL,
            normalizedQuery TEXT NOT NULL,
            query TEXT NOT NULL,
            searchedAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            isDeleted INTEGER NOT NULL,
            syncState TEXT NOT NULL,
            PRIMARY KEY(ownerId, normalizedQuery)
          )
          """.trimIndent(),
        )
        db.execSQL(
          """
          INSERT OR REPLACE INTO recent_searches_new (
            ownerId, normalizedQuery, query, searchedAt, updatedAt, isDeleted, syncState
          )
          SELECT
            '$LEGACY_OWNER_ID', lower(trim(query)), trim(query), searchedAt,
            searchedAt, 0, '$SYNC_STATE_PENDING'
          FROM recent_searches
          WHERE trim(query) <> ''
          """.trimIndent(),
        )
        db.execSQL("DROP TABLE recent_searches")
        db.execSQL("ALTER TABLE recent_searches_new RENAME TO recent_searches")

        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS sync_metadata (
            ownerId TEXT NOT NULL,
            metadataKey TEXT NOT NULL,
            timestampValue INTEGER NOT NULL,
            syncState TEXT NOT NULL,
            PRIMARY KEY(ownerId, metadataKey)
          )
          """.trimIndent(),
        )
      }
    }
  }
}
