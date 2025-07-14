package com.example.filmera.network

import com.example.filmera.model.CatalogResponse
import com.example.filmera.model.credit.CreditsResponse
import com.example.filmera.model.detail.MovieDetail
import com.example.filmera.model.search.SearchResponse
import com.example.filmera.model.search.SearchResult
import com.example.filmera.model.video.VideoResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TMDBApi {

  // MOVIES
  @GET("movie/popular")
  suspend fun getPopularMovies(
    @Query("page") page: Int = 1,
    @Query("language") language: String? = null
  ): CatalogResponse

  @GET("movie/now_playing")
  suspend fun getNowPlayingMovies(
    @Query("page") page: Int = 1,
    @Query("language") language: String? = null
  ): CatalogResponse

  @GET("movie/upcoming")
  suspend fun getUpcomingMovies(
    @Query("page") page: Int = 1,
    @Query("language") language: String? = null
  ): CatalogResponse

  @GET("movie/top_rated")
  suspend fun getTopRatedMovies(
    @Query("page") page: Int = 1,
    @Query("language") language: String? = null
  ): CatalogResponse

  @GET("trending/movie/week")
  suspend fun getTrendingMovies(
    @Query("language") language: String? = null
  ): CatalogResponse

  // RECOMMENDATION
  @GET("movie/{movie_id}/recommendations")
  suspend fun getMovieRecommendations(
    @Path("movie_id") movieId: Int,
    @Query("page") page: Int = 1,
    @Query("language") language: String? = null
  ): CatalogResponse

  // MOVIE DETAIL
  @GET("movie/{movie_id}")
  suspend fun getMovieDetail(
    @Path("movie_id") movieId: Int,
    @Query("language") language: String? = null
  ): MovieDetail

  // SEARCH MOVIE
  @GET("search/movie")
  suspend fun searchMovies(
    @Query("query") query: String,
    @Query("page") page: Int = 1,
    @Query("language") language: String? = null,
    @Query("include_adult") includeAdult: Boolean = false
  ): SearchResponse

  // TV SHOWS
  @GET("discover/tv")
  suspend fun getWesternTV(
    @Query("with_origin_country") country: String = "US",
    @Query("page") page: Int = 1,
    @Query("language") language: String? = null
  ): CatalogResponse

  @GET("discover/tv")
  suspend fun getKDrama(
    @Query("with_origin_country") country: String = "KR",
    @Query("with_genres") genre: Int = 18, // Drama
    @Query("page") page: Int = 1,
    @Query("language") language: String? = null
  ): CatalogResponse

  @GET("discover/movie")
  suspend fun getShortMovies(
    @Query("with_runtime.lte") maxRuntime: Int = 40,
    @Query("page") page: Int = 1,
    @Query("language") language: String? = null
  ): CatalogResponse

  @GET("discover/movie")
  suspend fun getAnimeMovies(
    @Query("with_genres") genre: Int = 16, // Animation
    @Query("with_keywords") keyword: String? = null,
    @Query("page") page: Int = 1,
    @Query("language") language: String? = null
  ): CatalogResponse

  @GET("tv/{tv_id}")
  suspend fun getTvDetail(
    @Path("tv_id") tvId: Int,
    @Query("language") language: String? = null
  ): MovieDetail

  @GET("search/tv")
  suspend fun searchTv(
    @Query("query") query: String,
    @Query("page") page: Int = 1,
    @Query("language") language: String? = null
  ): SearchResponse

  @GET("search/multi")
  suspend fun searchMulti(
    @Query("query") query: String,
    @Query("page") page: Int = 1,
    @Query("language") language: String? = null,
    @Query("include_adult") includeAdult: Boolean = false
  ): SearchResponse

  @GET("movie/{movie_id}/videos")
  suspend fun getMovieVideos(
    @Path("movie_id") movieId: Int
  ): VideoResponse

  @GET("movie/{movie_id}/credits")
  suspend fun getMovieCredits(
    @Path("movie_id") movieId: Int
  ): CreditsResponse

}
