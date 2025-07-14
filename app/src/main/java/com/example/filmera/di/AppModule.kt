package com.example.filmera.di

import android.content.Context
import androidx.room.Room
import com.example.filmera.data.AppDatabase
import com.example.filmera.data.local.dao.FavoriteDao
import com.example.filmera.env.ApiKey
import com.example.filmera.network.TMDBApi
import com.example.filmera.repository.MovieRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Provides
  fun provideBaseUrl(): String = ApiKey.BASE_URL

  @Provides
  fun provideOkHttpClient(): OkHttpClient =
    OkHttpClient.Builder()
      .addInterceptor { chain ->
        val request = chain.request().newBuilder()
          .addHeader("Authorization", ApiKey.BEARER_TOKEN)
          .build()
        chain.proceed(request)
      }.build()

  @Provides
  fun provideRetrofit(client: OkHttpClient, baseUrl: String): Retrofit =
    Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(client)
      .addConverterFactory(GsonConverterFactory.create())
      .build()

  @Provides
  fun provideApi(retrofit: Retrofit): TMDBApi =
    retrofit.create(TMDBApi::class.java)

  @Provides
  @Singleton
  fun provideMovieRepository(api: TMDBApi): MovieRepository {
    return MovieRepository(api)
  }

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
    return Room.databaseBuilder(
      context,
      AppDatabase::class.java,
      "filmera.db"
    )
      .fallbackToDestructiveMigration()
      .build()
  }

  @Provides
  fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()
}
