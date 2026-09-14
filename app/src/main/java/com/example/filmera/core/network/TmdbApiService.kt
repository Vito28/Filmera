package com.example.filmera.core.network

import com.example.filmera.core.network.dto.AccountStatesDto
import com.example.filmera.core.network.dto.AggregateCreditsResponseDto
import com.example.filmera.core.network.dto.AlternativeTitlesResponseDto
import com.example.filmera.core.network.dto.CatalogResponseDto
import com.example.filmera.core.network.dto.ContentRatingsResponseDto
import com.example.filmera.core.network.dto.CollectionDto
import com.example.filmera.core.network.dto.CreditsResponseDto
import com.example.filmera.core.network.dto.DatedCatalogResponseDto
import com.example.filmera.core.network.dto.EpisodeCreditsResponseDto
import com.example.filmera.core.network.dto.EpisodeGroupDetailsDto
import com.example.filmera.core.network.dto.EpisodeGroupsResponseDto
import com.example.filmera.core.network.dto.EpisodeImagesResponseDto
import com.example.filmera.core.network.dto.ExternalIdsDto
import com.example.filmera.core.network.dto.GenreListResponseDto
import com.example.filmera.core.network.dto.ImagesResponseDto
import com.example.filmera.core.network.dto.LanguageConfigurationDto
import com.example.filmera.core.network.dto.MovieDetailsDto
import com.example.filmera.core.network.dto.MovieDto
import com.example.filmera.core.network.dto.MovieKeywordsResponseDto
import com.example.filmera.core.network.dto.MultiSearchItemDto
import com.example.filmera.core.network.dto.PersonCombinedCreditsResponseDto
import com.example.filmera.core.network.dto.PersonDetailsDto
import com.example.filmera.core.network.dto.PersonDto
import com.example.filmera.core.network.dto.PersonImagesResponseDto
import com.example.filmera.core.network.dto.ReleaseDatesResponseDto
import com.example.filmera.core.network.dto.ReviewDto
import com.example.filmera.core.network.dto.SeasonDetailsDto
import com.example.filmera.core.network.dto.SeasonAccountStatesResponseDto
import com.example.filmera.core.network.dto.SeasonImagesResponseDto
import com.example.filmera.core.network.dto.TranslationResponseDto
import com.example.filmera.core.network.dto.TvDetailsDto
import com.example.filmera.core.network.dto.TvEpisodeDetailsDto
import com.example.filmera.core.network.dto.TvKeywordsResponseDto
import com.example.filmera.core.network.dto.TvShowDto
import com.example.filmera.core.network.dto.VideoResponseDto
import com.example.filmera.core.network.dto.WatchProvidersResponseDto
import com.example.filmera.core.network.dto.WatchProviderListResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * All TMDB Retrofit contracts live in this file, grouped by resource.
 *
 * Separate interfaces preserve interface segregation and make repositories
 * easy to fake in tests, while avoiding a service file for every resource.
 */

// region Catalog, trending, and discovery

interface TmdbCatalogApi {
  @GET("movie/now_playing")
  suspend fun getNowPlayingMovies(
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("region") region: String? = TmdbApiDefaults.REGION,
    @Query("page") page: Int = 1,
  ): DatedCatalogResponseDto<MovieDto>

  @GET("movie/popular")
  suspend fun getPopularMovies(
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("region") region: String? = TmdbApiDefaults.REGION,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<MovieDto>

  @GET("movie/top_rated")
  suspend fun getTopRatedMovies(
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("region") region: String? = TmdbApiDefaults.REGION,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<MovieDto>

  @GET("movie/upcoming")
  suspend fun getUpcomingMovies(
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("region") region: String? = TmdbApiDefaults.REGION,
    @Query("page") page: Int = 1,
  ): DatedCatalogResponseDto<MovieDto>

  @GET("tv/airing_today")
  suspend fun getAiringTodayTvShows(
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("timezone") timezone: String = TmdbApiDefaults.TIMEZONE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<TvShowDto>

  @GET("tv/on_the_air")
  suspend fun getOnTheAirTvShows(
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("timezone") timezone: String = TmdbApiDefaults.TIMEZONE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<TvShowDto>

  @GET("tv/popular")
  suspend fun getPopularTvShows(
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<TvShowDto>

  @GET("tv/top_rated")
  suspend fun getTopRatedTvShows(
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<TvShowDto>

  @GET("trending/movie/{time_window}")
  suspend fun getTrendingMovies(
    @Path("time_window") timeWindow: String = TmdbApiDefaults.TRENDING_WINDOW,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<MovieDto>

  @GET("trending/tv/{time_window}")
  suspend fun getTrendingTvShows(
    @Path("time_window") timeWindow: String = TmdbApiDefaults.TRENDING_WINDOW,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<TvShowDto>

  @GET("trending/all/{time_window}")
  suspend fun getTrendingAll(
    @Path("time_window") timeWindow: String = TmdbApiDefaults.TRENDING_WINDOW,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<MultiSearchItemDto>

  @GET("discover/movie")
  suspend fun discoverMovies(
    @QueryMap filters: Map<String, String>,
  ): CatalogResponseDto<MovieDto>

  @GET("discover/tv")
  suspend fun discoverTvShows(
    @QueryMap filters: Map<String, String>,
  ): CatalogResponseDto<TvShowDto>

  @GET("genre/movie/list")
  suspend fun getMovieGenres(
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
  ): GenreListResponseDto

  @GET("genre/tv/list")
  suspend fun getTvGenres(
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
  ): GenreListResponseDto

  @GET("configuration/languages")
  suspend fun getLanguages(): List<LanguageConfigurationDto>

  @GET("configuration/primary_translations")
  suspend fun getPrimaryTranslations(): List<String>

  @GET("watch/providers/movie")
  suspend fun getMovieWatchProviders(
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("watch_region") watchRegion: String = TmdbApiDefaults.REGION,
  ): WatchProviderListResponseDto

  @GET("watch/providers/tv")
  suspend fun getTvWatchProviders(
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("watch_region") watchRegion: String = TmdbApiDefaults.REGION,
  ): WatchProviderListResponseDto
}

/**
 * Resolves a product-level catalog feed into the correct TMDB endpoint.
 */
suspend fun TmdbCatalogApi.fetchCatalog(
  request: TmdbCatalogRequest,
): TmdbCatalogPageDto {
  val locale = request.locale
  val page = request.page
  return when (request.feed) {
    TmdbCatalogFeed.NOW_PLAYING_MOVIES -> TmdbCatalogPageDto.Movies(
      getNowPlayingMovies(locale.language, locale.region, page).toCatalogResponse(),
    )
    TmdbCatalogFeed.POPULAR_MOVIES -> TmdbCatalogPageDto.Movies(
      getPopularMovies(locale.language, locale.region, page),
    )
    TmdbCatalogFeed.UPCOMING_MOVIES -> TmdbCatalogPageDto.Movies(
      getUpcomingMovies(locale.language, locale.region, page).toCatalogResponse(),
    )
    TmdbCatalogFeed.TOP_RATED_MOVIES -> TmdbCatalogPageDto.Movies(
      getTopRatedMovies(locale.language, locale.region, page),
    )
    TmdbCatalogFeed.TRENDING_MOVIES -> TmdbCatalogPageDto.Movies(
      getTrendingMovies(request.timeWindow.apiValue, locale.language, page),
    )
    TmdbCatalogFeed.ANIME_MOVIES -> TmdbCatalogPageDto.Movies(
      discoverMovies(MovieDiscoverQuery.japaneseAnime(locale, page).toQueryMap()),
    )
    TmdbCatalogFeed.KOREAN_MOVIES -> TmdbCatalogPageDto.Movies(
      discoverMovies(MovieDiscoverQuery.korean(locale, page).toQueryMap()),
    )
    TmdbCatalogFeed.CHINESE_MOVIES -> TmdbCatalogPageDto.Movies(
      discoverMovies(MovieDiscoverQuery.chinese(locale, page).toQueryMap()),
    )
    TmdbCatalogFeed.SHORT_MOVIES -> TmdbCatalogPageDto.Movies(
      discoverMovies(
        MovieDiscoverQuery(
          locale = locale,
          page = page,
          maximumRuntimeMinutes = 40,
        ).toQueryMap(),
      ),
    )
    TmdbCatalogFeed.AIRING_TODAY_TV -> TmdbCatalogPageDto.TvShows(
      getAiringTodayTvShows(locale.language, TmdbApiDefaults.TIMEZONE, page),
    )
    TmdbCatalogFeed.ON_THE_AIR_TV -> TmdbCatalogPageDto.TvShows(
      getOnTheAirTvShows(locale.language, TmdbApiDefaults.TIMEZONE, page),
    )
    TmdbCatalogFeed.POPULAR_TV -> TmdbCatalogPageDto.TvShows(
      getPopularTvShows(locale.language, page),
    )
    TmdbCatalogFeed.TOP_RATED_TV -> TmdbCatalogPageDto.TvShows(
      getTopRatedTvShows(locale.language, page),
    )
    TmdbCatalogFeed.TRENDING_TV -> TmdbCatalogPageDto.TvShows(
      getTrendingTvShows(request.timeWindow.apiValue, locale.language, page),
    )
    TmdbCatalogFeed.ANIME_TV -> TmdbCatalogPageDto.TvShows(
      discoverTvShows(TvDiscoverQuery.japaneseAnime(locale, page).toQueryMap()),
    )
    TmdbCatalogFeed.WESTERN_TV -> TmdbCatalogPageDto.TvShows(
      discoverTvShows(
        TvDiscoverQuery(
          locale = locale,
          page = page,
          withOriginCountry = "US",
          withOriginalLanguage = "en",
        ).toQueryMap(),
      ),
    )
    TmdbCatalogFeed.KOREAN_DRAMA -> TmdbCatalogPageDto.TvShows(
      discoverTvShows(TvDiscoverQuery.koreanDrama(locale, page).toQueryMap()),
    )
    TmdbCatalogFeed.CHINESE_DRAMA -> TmdbCatalogPageDto.TvShows(
      discoverTvShows(TvDiscoverQuery.chineseDrama(locale, page).toQueryMap()),
    )
  }
}

private fun <T> DatedCatalogResponseDto<T>.toCatalogResponse(): CatalogResponseDto<T> =
  CatalogResponseDto(
    page = page,
    results = results,
    totalPages = totalPages,
    totalResults = totalResults,
  )

// endregion

// region Search

interface TmdbSearchApi {
  @GET("search/movie")
  suspend fun searchMovies(
    @Query("query") query: String,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("region") region: String = TmdbApiDefaults.REGION,
    @Query("page") page: Int = 1,
    @Query("include_adult") includeAdult: Boolean = false,
    @Query("year") year: Int? = null,
    @Query("primary_release_year") primaryReleaseYear: Int? = null,
  ): CatalogResponseDto<MovieDto>

  @GET("search/tv")
  suspend fun searchTvShows(
    @Query("query") query: String,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
    @Query("include_adult") includeAdult: Boolean = false,
    @Query("first_air_date_year") firstAirDateYear: Int? = null,
    @Query("year") year: Int? = null,
  ): CatalogResponseDto<TvShowDto>

  @GET("search/multi")
  suspend fun searchMulti(
    @Query("query") query: String,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
    @Query("include_adult") includeAdult: Boolean = false,
  ): CatalogResponseDto<MultiSearchItemDto>

  @GET("search/person")
  suspend fun searchPeople(
    @Query("query") query: String,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
    @Query("include_adult") includeAdult: Boolean = false,
  ): CatalogResponseDto<PersonDto>

  @GET("search/collection")
  suspend fun searchCollections(
    @Query("query") query: String,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
    @Query("include_adult") includeAdult: Boolean = false,
  ): CatalogResponseDto<CollectionDto>
}

// endregion

// region People

interface TmdbPersonApi {
  @GET("person/popular")
  suspend fun getPopularPeople(
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<PersonDto>

  @GET("person/{person_id}")
  suspend fun getPersonDetails(
    @Path("person_id") personId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("append_to_response") appendToResponse: String? = null,
  ): PersonDetailsDto

  @GET("person/{person_id}/combined_credits")
  suspend fun getPersonCombinedCredits(
    @Path("person_id") personId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
  ): PersonCombinedCreditsResponseDto

  @GET("person/{person_id}/external_ids")
  suspend fun getPersonExternalIds(
    @Path("person_id") personId: Int,
  ): ExternalIdsDto

  @GET("person/{person_id}/images")
  suspend fun getPersonImages(
    @Path("person_id") personId: Int,
  ): PersonImagesResponseDto

  @GET("person/{person_id}/translations")
  suspend fun getPersonTranslations(
    @Path("person_id") personId: Int,
  ): TranslationResponseDto
}

// endregion

// region Movie

interface TmdbMovieApi {
  @GET("movie/{movie_id}")
  suspend fun getMovieDetails(
    @Path("movie_id") movieId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("append_to_response") appendToResponse: String? = null,
    @Query("include_image_language")
    imageLanguages: String = TmdbApiDefaults.IMAGE_LANGUAGES,
    @Query("include_video_language")
    videoLanguages: String = TmdbApiDefaults.VIDEO_LANGUAGES,
    @Query("session_id") sessionId: String? = null,
  ): MovieDetailsDto

  @GET("movie/{movie_id}/account_states")
  suspend fun getMovieAccountStates(
    @Path("movie_id") movieId: Int,
    @Query("session_id") sessionId: String,
  ): AccountStatesDto

  @GET("movie/{movie_id}/alternative_titles")
  suspend fun getMovieAlternativeTitles(
    @Path("movie_id") movieId: Int,
    @Query("country") country: String? = null,
  ): AlternativeTitlesResponseDto

  @GET("movie/{movie_id}/credits")
  suspend fun getMovieCredits(
    @Path("movie_id") movieId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
  ): CreditsResponseDto

  @GET("movie/{movie_id}/external_ids")
  suspend fun getMovieExternalIds(
    @Path("movie_id") movieId: Int,
  ): ExternalIdsDto

  @GET("movie/{movie_id}/images")
  suspend fun getMovieImages(
    @Path("movie_id") movieId: Int,
    @Query("include_image_language")
    imageLanguages: String = TmdbApiDefaults.IMAGE_LANGUAGES,
  ): ImagesResponseDto

  @GET("movie/{movie_id}/keywords")
  suspend fun getMovieKeywords(
    @Path("movie_id") movieId: Int,
  ): MovieKeywordsResponseDto

  @GET("movie/{movie_id}/recommendations")
  suspend fun getMovieRecommendations(
    @Path("movie_id") movieId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<MovieDto>

  @GET("movie/{movie_id}/release_dates")
  suspend fun getMovieReleaseDates(
    @Path("movie_id") movieId: Int,
  ): ReleaseDatesResponseDto

  @GET("movie/{movie_id}/reviews")
  suspend fun getMovieReviews(
    @Path("movie_id") movieId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<ReviewDto>

  @GET("movie/{movie_id}/similar")
  suspend fun getSimilarMovies(
    @Path("movie_id") movieId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<MovieDto>

  @GET("movie/{movie_id}/translations")
  suspend fun getMovieTranslations(
    @Path("movie_id") movieId: Int,
  ): TranslationResponseDto

  @GET("movie/{movie_id}/videos")
  suspend fun getMovieVideos(
    @Path("movie_id") movieId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("include_video_language")
    videoLanguages: String = TmdbApiDefaults.VIDEO_LANGUAGES,
  ): VideoResponseDto

  @GET("movie/{movie_id}/watch/providers")
  suspend fun getMovieWatchProviders(
    @Path("movie_id") movieId: Int,
  ): WatchProvidersResponseDto
}

// endregion

// region TV Series

interface TmdbTvSeriesApi {
  @GET("tv/{series_id}")
  suspend fun getTvDetails(
    @Path("series_id") seriesId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("append_to_response") appendToResponse: String? = null,
    @Query("include_image_language")
    imageLanguages: String = TmdbApiDefaults.IMAGE_LANGUAGES,
    @Query("include_video_language")
    videoLanguages: String = TmdbApiDefaults.VIDEO_LANGUAGES,
    @Query("session_id") sessionId: String? = null,
  ): TvDetailsDto

  @GET("tv/{series_id}/account_states")
  suspend fun getTvAccountStates(
    @Path("series_id") seriesId: Int,
    @Query("session_id") sessionId: String,
  ): AccountStatesDto

  @GET("tv/{series_id}/aggregate_credits")
  suspend fun getTvAggregateCredits(
    @Path("series_id") seriesId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
  ): AggregateCreditsResponseDto

  @GET("tv/{series_id}/alternative_titles")
  suspend fun getTvAlternativeTitles(
    @Path("series_id") seriesId: Int,
  ): AlternativeTitlesResponseDto

  @GET("tv/{series_id}/content_ratings")
  suspend fun getTvContentRatings(
    @Path("series_id") seriesId: Int,
  ): ContentRatingsResponseDto

  @GET("tv/{series_id}/credits")
  suspend fun getTvCredits(
    @Path("series_id") seriesId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
  ): CreditsResponseDto

  @GET("tv/{series_id}/episode_groups")
  suspend fun getTvEpisodeGroups(
    @Path("series_id") seriesId: Int,
  ): EpisodeGroupsResponseDto

  @GET("tv/{series_id}/external_ids")
  suspend fun getTvExternalIds(
    @Path("series_id") seriesId: Int,
  ): ExternalIdsDto

  @GET("tv/{series_id}/images")
  suspend fun getTvImages(
    @Path("series_id") seriesId: Int,
    @Query("include_image_language")
    imageLanguages: String = TmdbApiDefaults.IMAGE_LANGUAGES,
  ): ImagesResponseDto

  @GET("tv/{series_id}/keywords")
  suspend fun getTvKeywords(
    @Path("series_id") seriesId: Int,
  ): TvKeywordsResponseDto

  @GET("tv/{series_id}/recommendations")
  suspend fun getTvRecommendations(
    @Path("series_id") seriesId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<TvShowDto>

  @GET("tv/{series_id}/reviews")
  suspend fun getTvReviews(
    @Path("series_id") seriesId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<ReviewDto>

  @GET("tv/{series_id}/similar")
  suspend fun getSimilarTvShows(
    @Path("series_id") seriesId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("page") page: Int = 1,
  ): CatalogResponseDto<TvShowDto>

  @GET("tv/{series_id}/translations")
  suspend fun getTvTranslations(
    @Path("series_id") seriesId: Int,
  ): TranslationResponseDto

  @GET("tv/{series_id}/videos")
  suspend fun getTvVideos(
    @Path("series_id") seriesId: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("include_video_language")
    videoLanguages: String = TmdbApiDefaults.VIDEO_LANGUAGES,
  ): VideoResponseDto

  @GET("tv/{series_id}/watch/providers")
  suspend fun getTvWatchProviders(
    @Path("series_id") seriesId: Int,
  ): WatchProvidersResponseDto
}

// endregion

// region TV Season

interface TmdbTvSeasonApi {
  @GET("tv/{series_id}/season/{season_number}")
  suspend fun getSeasonDetails(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("append_to_response") appendToResponse: String? = null,
    @Query("include_image_language")
    imageLanguages: String = TmdbApiDefaults.IMAGE_LANGUAGES,
    @Query("include_video_language")
    videoLanguages: String = TmdbApiDefaults.VIDEO_LANGUAGES,
  ): SeasonDetailsDto

  @GET("tv/{series_id}/season/{season_number}/account_states")
  suspend fun getSeasonAccountStates(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Query("session_id") sessionId: String,
  ): SeasonAccountStatesResponseDto

  @GET("tv/{series_id}/season/{season_number}/aggregate_credits")
  suspend fun getSeasonAggregateCredits(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
  ): AggregateCreditsResponseDto

  @GET("tv/{series_id}/season/{season_number}/credits")
  suspend fun getSeasonCredits(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
  ): CreditsResponseDto

  @GET("tv/{series_id}/season/{season_number}/external_ids")
  suspend fun getSeasonExternalIds(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
  ): ExternalIdsDto

  @GET("tv/{series_id}/season/{season_number}/images")
  suspend fun getSeasonImages(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Query("include_image_language")
    imageLanguages: String = TmdbApiDefaults.IMAGE_LANGUAGES,
  ): SeasonImagesResponseDto

  @GET("tv/{series_id}/season/{season_number}/translations")
  suspend fun getSeasonTranslations(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
  ): TranslationResponseDto

  @GET("tv/{series_id}/season/{season_number}/videos")
  suspend fun getSeasonVideos(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("include_video_language")
    videoLanguages: String = TmdbApiDefaults.VIDEO_LANGUAGES,
  ): VideoResponseDto

  @GET("tv/{series_id}/season/{season_number}/watch/providers")
  suspend fun getSeasonWatchProviders(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
  ): WatchProvidersResponseDto
}

// endregion

// region TV Episode and episode groups

interface TmdbTvEpisodeApi {
  @GET("tv/{series_id}/season/{season_number}/episode/{episode_number}")
  suspend fun getEpisodeDetails(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Path("episode_number") episodeNumber: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("append_to_response") appendToResponse: String? = null,
    @Query("include_image_language")
    imageLanguages: String = TmdbApiDefaults.IMAGE_LANGUAGES,
    @Query("include_video_language")
    videoLanguages: String = TmdbApiDefaults.VIDEO_LANGUAGES,
  ): TvEpisodeDetailsDto

  @GET("tv/{series_id}/season/{season_number}/episode/{episode_number}/account_states")
  suspend fun getEpisodeAccountStates(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Path("episode_number") episodeNumber: Int,
    @Query("session_id") sessionId: String,
  ): AccountStatesDto

  @GET("tv/{series_id}/season/{season_number}/episode/{episode_number}/credits")
  suspend fun getEpisodeCredits(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Path("episode_number") episodeNumber: Int,
  ): EpisodeCreditsResponseDto

  @GET("tv/{series_id}/season/{season_number}/episode/{episode_number}/external_ids")
  suspend fun getEpisodeExternalIds(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Path("episode_number") episodeNumber: Int,
  ): ExternalIdsDto

  @GET("tv/{series_id}/season/{season_number}/episode/{episode_number}/images")
  suspend fun getEpisodeImages(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Path("episode_number") episodeNumber: Int,
  ): EpisodeImagesResponseDto

  @GET("tv/{series_id}/season/{season_number}/episode/{episode_number}/translations")
  suspend fun getEpisodeTranslations(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Path("episode_number") episodeNumber: Int,
  ): TranslationResponseDto

  @GET("tv/{series_id}/season/{season_number}/episode/{episode_number}/videos")
  suspend fun getEpisodeVideos(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Path("episode_number") episodeNumber: Int,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
    @Query("include_video_language")
    videoLanguages: String = TmdbApiDefaults.VIDEO_LANGUAGES,
  ): VideoResponseDto

  @GET("tv/episode_group/{episode_group_id}")
  suspend fun getEpisodeGroupDetails(
    @Path("episode_group_id") episodeGroupId: String,
    @Query("language") language: String = TmdbApiDefaults.LANGUAGE,
  ): EpisodeGroupDetailsDto
}

// endregion

object TmdbApiDefaults {
  const val LANGUAGE = "en-US"
  const val REGION = "ID"
  const val TIMEZONE = "Asia/Jakarta"
  const val IMAGE_LANGUAGES = "en,null"
  const val VIDEO_LANGUAGES = "en"
  const val TRENDING_WINDOW = "week"
}

object TmdbAppendToResponse {
  const val PERSON_DETAILS =
    "combined_credits,external_ids,images,translations"

  const val MOVIE_DETAILS =
    "credits,videos,images,keywords,recommendations,similar,release_dates," +
      "watch/providers,external_ids,reviews,translations"

  const val TV_DETAILS =
    "aggregate_credits,credits,videos,images,keywords,recommendations,similar," +
      "content_ratings,watch/providers,external_ids,reviews,translations,episode_groups"

  const val SEASON_DETAILS =
    "aggregate_credits,credits,external_ids,images,translations,videos,watch/providers"

  const val EPISODE_DETAILS =
    "credits,external_ids,images,translations,videos"
}
