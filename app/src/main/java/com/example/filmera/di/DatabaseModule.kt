package com.example.filmera.di

import android.content.Context
import androidx.room.Room
import com.example.filmera.core.database.FilmeraDatabase
import com.example.filmera.core.database.LibraryItemDao
import com.example.filmera.core.database.RecommendationFeedbackDao
import com.example.filmera.core.database.RecentSearchDao
import com.example.filmera.core.database.UserPreferenceDao
import com.example.filmera.core.database.SyncMetadataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  @Provides
  @Singleton
  fun provideDatabase(
    @ApplicationContext context: Context,
  ): FilmeraDatabase = Room.databaseBuilder(
    context,
    FilmeraDatabase::class.java,
    "filmera.db",
  )
    .addMigrations(FilmeraDatabase.MIGRATION_2_3)
    .addMigrations(FilmeraDatabase.MIGRATION_3_4)
    .addMigrations(FilmeraDatabase.MIGRATION_4_5)
    .addMigrations(FilmeraDatabase.MIGRATION_5_6)
    .addMigrations(FilmeraDatabase.MIGRATION_6_7)
    .build()

  @Provides
  fun provideLibraryItemDao(database: FilmeraDatabase): LibraryItemDao =
    database.libraryItemDao()

  @Provides
  fun provideRecentSearchDao(database: FilmeraDatabase): RecentSearchDao =
    database.recentSearchDao()

  @Provides
  fun provideUserPreferenceDao(database: FilmeraDatabase): UserPreferenceDao =
    database.userPreferenceDao()

  @Provides
  fun provideRecommendationFeedbackDao(
    database: FilmeraDatabase,
  ): RecommendationFeedbackDao = database.recommendationFeedbackDao()

  @Provides
  fun provideSyncMetadataDao(database: FilmeraDatabase): SyncMetadataDao =
    database.syncMetadataDao()
}
