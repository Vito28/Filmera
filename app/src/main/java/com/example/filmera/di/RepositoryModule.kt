package com.example.filmera.di

import com.example.filmera.feature.detail.data.TmdbDetailRepository
import com.example.filmera.feature.detail.domain.DetailRepository
import com.example.filmera.feature.auth.data.SupabaseAuthRepository
import com.example.filmera.feature.auth.domain.AuthRepository
import com.example.filmera.feature.community.data.SupabaseCommunityRepository
import com.example.filmera.feature.community.domain.CommunityRepository
import com.example.filmera.feature.discover.data.TmdbExploreRepository
import com.example.filmera.feature.discover.data.TmdbRecommendationRepository
import com.example.filmera.feature.discover.data.TmdbRankingRepository
import com.example.filmera.feature.discover.domain.ExploreRepository
import com.example.filmera.feature.discover.domain.RankingRepository
import com.example.filmera.feature.discover.domain.RecommendationRepository
import com.example.filmera.feature.home.data.TmdbHomeRepository
import com.example.filmera.feature.home.domain.HomeRepository
import com.example.filmera.feature.library.data.RoomLibraryRepository
import com.example.filmera.feature.library.domain.LibraryRepository
import com.example.filmera.feature.person.data.TmdbPersonRepository
import com.example.filmera.feature.person.domain.PersonRepository
import com.example.filmera.feature.preferences.data.RoomPreferenceRepository
import com.example.filmera.feature.preferences.domain.PreferenceRepository
import com.example.filmera.feature.profile.data.SupabaseProfileRepository
import com.example.filmera.feature.profile.domain.ProfileRepository
import com.example.filmera.feature.search.data.RoomRecentSearchRepository
import com.example.filmera.feature.search.data.TmdbSearchRepository
import com.example.filmera.feature.search.domain.RecentSearchRepository
import com.example.filmera.feature.search.domain.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.filmera.core.sync.CurrentUserProvider
import com.example.filmera.core.sync.DefaultUserDataSyncer
import com.example.filmera.core.sync.SupabaseCurrentUserProvider
import com.example.filmera.core.sync.SyncRequestScheduler
import com.example.filmera.core.sync.UserDataSyncScheduler
import com.example.filmera.core.sync.UserDataSyncer

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
  @Binds
  @Singleton
  abstract fun bindCurrentUserProvider(
    implementation: SupabaseCurrentUserProvider,
  ): CurrentUserProvider

  @Binds
  @Singleton
  abstract fun bindSyncRequestScheduler(
    implementation: UserDataSyncScheduler,
  ): SyncRequestScheduler

  @Binds
  @Singleton
  abstract fun bindUserDataSyncer(implementation: DefaultUserDataSyncer): UserDataSyncer

  @Binds
  @Singleton
  abstract fun bindAuthRepository(implementation: SupabaseAuthRepository): AuthRepository

  @Binds
  @Singleton
  abstract fun bindProfileRepository(
    implementation: SupabaseProfileRepository,
  ): ProfileRepository

  @Binds
  @Singleton
  abstract fun bindCommunityRepository(
    implementation: SupabaseCommunityRepository,
  ): CommunityRepository

  @Binds
  @Singleton
  abstract fun bindHomeRepository(implementation: TmdbHomeRepository): HomeRepository

  @Binds
  @Singleton
  abstract fun bindDetailRepository(implementation: TmdbDetailRepository): DetailRepository

  @Binds
  @Singleton
  abstract fun bindSearchRepository(implementation: TmdbSearchRepository): SearchRepository

  @Binds
  @Singleton
  abstract fun bindRecentSearchRepository(
    implementation: RoomRecentSearchRepository,
  ): RecentSearchRepository

  @Binds
  @Singleton
  abstract fun bindLibraryRepository(implementation: RoomLibraryRepository): LibraryRepository

  @Binds
  @Singleton
  abstract fun bindPersonRepository(implementation: TmdbPersonRepository): PersonRepository

  @Binds
  @Singleton
  abstract fun bindPreferenceRepository(
    implementation: RoomPreferenceRepository,
  ): PreferenceRepository

  @Binds
  @Singleton
  abstract fun bindRecommendationRepository(
    implementation: TmdbRecommendationRepository,
  ): RecommendationRepository

  @Binds
  @Singleton
  abstract fun bindExploreRepository(
    implementation: TmdbExploreRepository,
  ): ExploreRepository

  @Binds
  @Singleton
  abstract fun bindRankingRepository(
    implementation: TmdbRankingRepository,
  ): RankingRepository
}
