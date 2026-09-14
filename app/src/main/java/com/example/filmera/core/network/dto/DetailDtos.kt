package com.example.filmera.core.network.dto

import com.google.gson.annotations.SerializedName

data class MovieDetailsDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("adult") val adult: Boolean = false,
  @SerializedName("title") val title: String? = null,
  @SerializedName("original_title") val originalTitle: String? = null,
  @SerializedName("overview") val overview: String? = null,
  @SerializedName("tagline") val tagline: String? = null,
  @SerializedName("poster_path") val posterPath: String? = null,
  @SerializedName("backdrop_path") val backdropPath: String? = null,
  @SerializedName("belongs_to_collection") val belongsToCollection: CollectionDto? = null,
  @SerializedName("release_date") val releaseDate: String? = null,
  @SerializedName("runtime") val runtime: Int? = null,
  @SerializedName("vote_average") val voteAverage: Double = 0.0,
  @SerializedName("vote_count") val voteCount: Int = 0,
  @SerializedName("popularity") val popularity: Double = 0.0,
  @SerializedName("original_language") val originalLanguage: String? = null,
  @SerializedName("status") val status: String? = null,
  @SerializedName("homepage") val homepage: String? = null,
  @SerializedName("imdb_id") val imdbId: String? = null,
  @SerializedName("budget") val budget: Long = 0L,
  @SerializedName("revenue") val revenue: Long = 0L,
  @SerializedName("genres") val genres: List<GenreDto> = emptyList(),
  @SerializedName("production_companies") val productionCompanies: List<ProductionCompanyDto> = emptyList(),
  @SerializedName("production_countries") val productionCountries: List<ProductionCountryDto> = emptyList(),
  @SerializedName("spoken_languages") val spokenLanguages: List<SpokenLanguageDto> = emptyList(),
  @SerializedName("credits") val credits: CreditsResponseDto? = null,
  @SerializedName("videos") val videos: VideoResponseDto? = null,
  @SerializedName("images") val images: ImagesResponseDto? = null,
  @SerializedName("keywords") val keywords: MovieKeywordsResponseDto? = null,
  @SerializedName("recommendations")
  val recommendations: CatalogResponseDto<MovieDto>? = null,
  @SerializedName("similar") val similar: CatalogResponseDto<MovieDto>? = null,
  @SerializedName("release_dates") val releaseDates: ReleaseDatesResponseDto? = null,
  @SerializedName("watch/providers")
  val watchProviders: WatchProvidersResponseDto? = null,
  @SerializedName("external_ids") val externalIds: ExternalIdsDto? = null,
  @SerializedName("reviews") val reviews: CatalogResponseDto<ReviewDto>? = null,
  @SerializedName("translations") val translations: TranslationResponseDto? = null,
)

data class TvDetailsDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("adult") val adult: Boolean = false,
  @SerializedName("name") val name: String? = null,
  @SerializedName("original_name") val originalName: String? = null,
  @SerializedName("overview") val overview: String? = null,
  @SerializedName("tagline") val tagline: String? = null,
  @SerializedName("poster_path") val posterPath: String? = null,
  @SerializedName("backdrop_path") val backdropPath: String? = null,
  @SerializedName("first_air_date") val firstAirDate: String? = null,
  @SerializedName("last_air_date") val lastAirDate: String? = null,
  @SerializedName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
  @SerializedName("vote_average") val voteAverage: Double = 0.0,
  @SerializedName("vote_count") val voteCount: Int = 0,
  @SerializedName("popularity") val popularity: Double = 0.0,
  @SerializedName("original_language") val originalLanguage: String? = null,
  @SerializedName("status") val status: String? = null,
  @SerializedName("homepage") val homepage: String? = null,
  @SerializedName("in_production") val isInProduction: Boolean = false,
  @SerializedName("number_of_episodes") val numberOfEpisodes: Int = 0,
  @SerializedName("number_of_seasons") val numberOfSeasons: Int = 0,
  @SerializedName("type") val type: String? = null,
  @SerializedName("genres") val genres: List<GenreDto> = emptyList(),
  @SerializedName("networks") val networks: List<NetworkDto> = emptyList(),
  @SerializedName("seasons") val seasons: List<SeasonSummaryDto> = emptyList(),
  @SerializedName("last_episode_to_air")
  val lastEpisodeToAir: EpisodeSummaryDto? = null,
  @SerializedName("next_episode_to_air")
  val nextEpisodeToAir: EpisodeSummaryDto? = null,
  @SerializedName("production_companies") val productionCompanies: List<ProductionCompanyDto> = emptyList(),
  @SerializedName("production_countries") val productionCountries: List<ProductionCountryDto> = emptyList(),
  @SerializedName("spoken_languages") val spokenLanguages: List<SpokenLanguageDto> = emptyList(),
  @SerializedName("aggregate_credits")
  val aggregateCredits: AggregateCreditsResponseDto? = null,
  @SerializedName("credits") val credits: CreditsResponseDto? = null,
  @SerializedName("videos") val videos: VideoResponseDto? = null,
  @SerializedName("images") val images: ImagesResponseDto? = null,
  @SerializedName("keywords") val keywords: TvKeywordsResponseDto? = null,
  @SerializedName("recommendations")
  val recommendations: CatalogResponseDto<TvShowDto>? = null,
  @SerializedName("similar") val similar: CatalogResponseDto<TvShowDto>? = null,
  @SerializedName("content_ratings")
  val contentRatings: ContentRatingsResponseDto? = null,
  @SerializedName("watch/providers")
  val watchProviders: WatchProvidersResponseDto? = null,
  @SerializedName("external_ids") val externalIds: ExternalIdsDto? = null,
  @SerializedName("reviews") val reviews: CatalogResponseDto<ReviewDto>? = null,
  @SerializedName("translations") val translations: TranslationResponseDto? = null,
  @SerializedName("episode_groups")
  val episodeGroups: EpisodeGroupsResponseDto? = null,
)

data class GenreDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("name") val name: String = "",
)

data class ProductionCompanyDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("name") val name: String = "",
  @SerializedName("logo_path") val logoPath: String? = null,
  @SerializedName("origin_country") val originCountry: String? = null,
)

data class ProductionCountryDto(
  @SerializedName("iso_3166_1") val countryCode: String? = null,
  @SerializedName("name") val name: String = "",
)

data class SpokenLanguageDto(
  @SerializedName("iso_639_1") val languageCode: String? = null,
  @SerializedName("english_name") val englishName: String? = null,
  @SerializedName("name") val name: String? = null,
)
