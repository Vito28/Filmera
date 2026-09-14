package com.example.filmera.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilmeraDatabaseMigrationTest {
  @get:Rule
  val migrationTestHelper = MigrationTestHelper(
    instrumentation = InstrumentationRegistry.getInstrumentation(),
    databaseClass = FilmeraDatabase::class.java,
  )

  @Test
  fun migration4To5_preservesFavoriteAsIndependentLibraryItem() {
    migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 4).apply {
      execSQL(
        """
        INSERT INTO favorites (
          mediaId, mediaType, title, originalTitle, posterPath, backdropPath,
          overview, releaseDate, voteAverage, voteCount, popularity, originalLanguage
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf<Any>(
          550,
          "MOVIE",
          "Fight Club",
          "Fight Club",
          "/poster.jpg",
          "/backdrop.jpg",
          "Overview",
          "1999-10-15",
          8.4,
          30_000,
          61.4,
          "en",
        ),
      )
      close()
    }

    val migratedDatabase = migrationTestHelper.runMigrationsAndValidate(
      TEST_DATABASE_NAME,
      5,
      true,
      FilmeraDatabase.MIGRATION_4_5,
    )

    migratedDatabase.query(
      """
      SELECT mediaId, mediaType, title, watchStatus, isFavorite, addedAt, updatedAt
      FROM library_items
      """.trimIndent(),
    ).use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals(550, cursor.getInt(cursor.getColumnIndexOrThrow("mediaId")))
      assertEquals("MOVIE", cursor.getString(cursor.getColumnIndexOrThrow("mediaType")))
      assertEquals("Fight Club", cursor.getString(cursor.getColumnIndexOrThrow("title")))
      assertEquals("NONE", cursor.getString(cursor.getColumnIndexOrThrow("watchStatus")))
      assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isFavorite")))
      assertTrue(cursor.getLong(cursor.getColumnIndexOrThrow("addedAt")) > 0L)
      assertTrue(cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt")) > 0L)
    }

    migratedDatabase.close()
  }

  @Test
  fun migration5To6_addsPreferenceAndRecommendationFeedbackTables() {
    migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 5).close()

    val migratedDatabase = migrationTestHelper.runMigrationsAndValidate(
      TEST_DATABASE_NAME,
      6,
      true,
      FilmeraDatabase.MIGRATION_5_6,
    )

    migratedDatabase.query(
      "SELECT COUNT(*) FROM user_preferences",
    ).use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals(0, cursor.getInt(0))
    }
    migratedDatabase.query(
      "SELECT COUNT(*) FROM recommendation_feedback",
    ).use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals(0, cursor.getInt(0))
    }

    migratedDatabase.close()
  }

  @Test
  fun migration6To7_preservesLocalUserDataForFirstAuthenticatedOwner() {
    migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 6).apply {
      execSQL(
        """
        INSERT INTO library_items (
          mediaId, mediaType, title, originalTitle, posterPath, backdropPath,
          overview, releaseDate, voteAverage, voteCount, popularity,
          originalLanguage, watchStatus, isFavorite, addedAt, updatedAt
        ) VALUES (550, 'MOVIE', 'Fight Club', 'Fight Club', '/poster.jpg', NULL,
          'Overview', '1999-10-15', 8.4, 30000, 61.4, 'en',
          'WATCHLIST', 1, 1000, 2000)
        """.trimIndent(),
      )
      execSQL(
        """
        INSERT INTO user_preferences (
          profileId, preferredMediaTypes, preferredGenreIds, preferredCountries,
          preferredMovieIds, preferredTvIds, preferredPersonIds,
          preferredLanguageCodes, onboardingCompleted, updatedAt
        ) VALUES (1, 'MOVIE', '18,28,53', 'ID', '550', '', '', 'id', 1, 3000)
        """.trimIndent(),
      )
      execSQL("INSERT INTO recent_searches (query, searchedAt) VALUES ('Dune', 4000)")
      close()
    }

    val migratedDatabase = migrationTestHelper.runMigrationsAndValidate(
      TEST_DATABASE_NAME,
      7,
      true,
      FilmeraDatabase.MIGRATION_6_7,
    )

    migratedDatabase.query(
      "SELECT ownerId, remoteMediaId, isDeleted, syncState FROM library_items",
    ).use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals(LEGACY_OWNER_ID, cursor.getString(0))
      assertTrue(cursor.isNull(1))
      assertEquals(0, cursor.getInt(2))
      assertEquals(SYNC_STATE_PENDING, cursor.getString(3))
    }
    migratedDatabase.query(
      "SELECT ownerId, preferredGenreIds, syncState FROM user_preferences",
    ).use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals(LEGACY_OWNER_ID, cursor.getString(0))
      assertEquals("18,28,53", cursor.getString(1))
      assertEquals(SYNC_STATE_PENDING, cursor.getString(2))
    }
    migratedDatabase.query(
      "SELECT ownerId, normalizedQuery, query, isDeleted FROM recent_searches",
    ).use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals(LEGACY_OWNER_ID, cursor.getString(0))
      assertEquals("dune", cursor.getString(1))
      assertEquals("Dune", cursor.getString(2))
      assertEquals(0, cursor.getInt(3))
    }

    migratedDatabase.close()
  }

  @Test
  fun migration6To7_preservesLocalDataAsPendingLegacyData() {
    migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 6).apply {
      execSQL(
        """
        INSERT INTO user_preferences (
          profileId, preferredMediaTypes, preferredGenreIds, preferredCountries,
          preferredMovieIds, preferredTvIds, preferredPersonIds,
          preferredLanguageCodes, onboardingCompleted, updatedAt
        ) VALUES (1, 'MOVIE', '18,28,878', 'ID', '550', '', '', 'id', 1, 1234)
        """.trimIndent(),
      )
      execSQL("INSERT INTO recent_searches (query, searchedAt) VALUES ('Dune', 2345)")
      close()
    }

    val migratedDatabase = migrationTestHelper.runMigrationsAndValidate(
      TEST_DATABASE_NAME,
      7,
      true,
      FilmeraDatabase.MIGRATION_6_7,
    )

    migratedDatabase.query(
      "SELECT ownerId, preferredGenreIds, syncState FROM user_preferences",
    ).use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals(LEGACY_OWNER_ID, cursor.getString(0))
      assertEquals("18,28,878", cursor.getString(1))
      assertEquals(SYNC_STATE_PENDING, cursor.getString(2))
    }
    migratedDatabase.query(
      "SELECT ownerId, normalizedQuery, query, syncState FROM recent_searches",
    ).use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals(LEGACY_OWNER_ID, cursor.getString(0))
      assertEquals("dune", cursor.getString(1))
      assertEquals("Dune", cursor.getString(2))
      assertEquals(SYNC_STATE_PENDING, cursor.getString(3))
    }

    migratedDatabase.close()
  }

  private companion object {
    const val TEST_DATABASE_NAME = "filmera-migration-test"
  }
}
