package com.example.filmera.di

import com.example.filmera.BuildConfig
import com.example.filmera.core.network.TmdbCatalogApi
import com.example.filmera.core.network.TmdbConfig
import com.example.filmera.core.network.TmdbMovieApi
import com.example.filmera.core.network.TmdbPersonApi
import com.example.filmera.core.network.TmdbSearchApi
import com.example.filmera.core.network.TmdbTvEpisodeApi
import com.example.filmera.core.network.TmdbTvSeasonApi
import com.example.filmera.core.network.TmdbTvSeriesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
  @Provides
  @Singleton
  fun provideTmdbConfig(): TmdbConfig = TmdbConfig(
    bearerToken = BuildConfig.TMDB_BEARER_TOKEN,
  )

  @Provides
  @Singleton
  fun provideOkHttpClient(config: TmdbConfig): OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .addInterceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
          .header("Accept", "application/json")
        if (config.isConfigured && chain.request().url.host == config.trustedHost) {
          requestBuilder.header("Authorization", "Bearer ${config.bearerToken}")
        }
        chain.proceed(requestBuilder.build())
      }
      .build()

  @Provides
  @Singleton
  fun provideRetrofit(
    client: OkHttpClient,
    config: TmdbConfig,
  ): Retrofit = Retrofit.Builder()
    .baseUrl(config.baseUrl)
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

  @Provides
  @Singleton
  fun provideTmdbCatalogApi(retrofit: Retrofit): TmdbCatalogApi =
    retrofit.create(TmdbCatalogApi::class.java)

  @Provides
  @Singleton
  fun provideTmdbSearchApi(retrofit: Retrofit): TmdbSearchApi =
    retrofit.create(TmdbSearchApi::class.java)

  @Provides
  @Singleton
  fun provideTmdbMovieApi(retrofit: Retrofit): TmdbMovieApi =
    retrofit.create(TmdbMovieApi::class.java)

  @Provides
  @Singleton
  fun provideTmdbPersonApi(retrofit: Retrofit): TmdbPersonApi =
    retrofit.create(TmdbPersonApi::class.java)

  @Provides
  @Singleton
  fun provideTmdbTvSeriesApi(retrofit: Retrofit): TmdbTvSeriesApi =
    retrofit.create(TmdbTvSeriesApi::class.java)

  @Provides
  @Singleton
  fun provideTmdbTvSeasonApi(retrofit: Retrofit): TmdbTvSeasonApi =
    retrofit.create(TmdbTvSeasonApi::class.java)

  @Provides
  @Singleton
  fun provideTmdbTvEpisodeApi(retrofit: Retrofit): TmdbTvEpisodeApi =
    retrofit.create(TmdbTvEpisodeApi::class.java)
}
