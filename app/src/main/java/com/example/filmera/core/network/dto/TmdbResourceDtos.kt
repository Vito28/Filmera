package com.example.filmera.core.network.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class GenreListResponseDto(
  @SerializedName("genres") val genres: List<GenreDto> = emptyList(),
)

data class LanguageConfigurationDto(
  @SerializedName("iso_639_1") val languageCode: String = "",
  @SerializedName("english_name") val englishName: String = "",
  @SerializedName("name") val nativeName: String = "",
)

data class AccountStatesDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("favorite") val favorite: Boolean = false,
  @SerializedName("rated") val rated: JsonElement? = null,
  @SerializedName("watchlist") val watchlist: Boolean = false,
)

data class SeasonAccountStatesResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("results") val results: List<AccountStatesDto> = emptyList(),
)

data class ExternalIdsDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("imdb_id") val imdbId: String? = null,
  @SerializedName("tvdb_id") val tvdbId: Int? = null,
  @SerializedName("wikidata_id") val wikidataId: String? = null,
  @SerializedName("facebook_id") val facebookId: String? = null,
  @SerializedName("instagram_id") val instagramId: String? = null,
  @SerializedName("twitter_id") val twitterId: String? = null,
  @SerializedName("tiktok_id") val tiktokId: String? = null,
  @SerializedName("youtube_id") val youtubeId: String? = null,
)

data class ImagesResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("backdrops") val backdrops: List<ImageAssetDto> = emptyList(),
  @SerializedName("logos") val logos: List<ImageAssetDto> = emptyList(),
  @SerializedName("posters") val posters: List<ImageAssetDto> = emptyList(),
)

data class SeasonImagesResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("posters") val posters: List<ImageAssetDto> = emptyList(),
)

data class EpisodeImagesResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("stills") val stills: List<ImageAssetDto> = emptyList(),
)

data class ImageAssetDto(
  @SerializedName("aspect_ratio") val aspectRatio: Double = 0.0,
  @SerializedName("height") val height: Int = 0,
  @SerializedName("iso_639_1") val languageCode: String? = null,
  @SerializedName("file_path") val filePath: String? = null,
  @SerializedName("vote_average") val voteAverage: Double = 0.0,
  @SerializedName("vote_count") val voteCount: Int = 0,
  @SerializedName("width") val width: Int = 0,
)

data class KeywordDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("name") val name: String = "",
)

data class MovieKeywordsResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("keywords") val keywords: List<KeywordDto> = emptyList(),
)

data class TvKeywordsResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("results") val results: List<KeywordDto> = emptyList(),
)

data class AlternativeTitlesResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("titles") val titles: List<AlternativeTitleDto> = emptyList(),
  @SerializedName("results") val results: List<AlternativeTitleDto> = emptyList(),
) {
  val allTitles: List<AlternativeTitleDto>
    get() = (titles + results).distinctBy { "${it.countryCode}:${it.title}:${it.type}" }
}

data class AlternativeTitleDto(
  @SerializedName("iso_3166_1") val countryCode: String = "",
  @SerializedName("title") val title: String = "",
  @SerializedName("type") val type: String = "",
)

data class TranslationResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("translations") val translations: List<TranslationDto> = emptyList(),
)

data class TranslationDto(
  @SerializedName("iso_3166_1") val countryCode: String = "",
  @SerializedName("iso_639_1") val languageCode: String = "",
  @SerializedName("name") val localizedLanguageName: String = "",
  @SerializedName("english_name") val englishLanguageName: String = "",
  @SerializedName("data") val data: TranslationDataDto = TranslationDataDto(),
)

data class TranslationDataDto(
  @SerializedName("homepage") val homepage: String? = null,
  @SerializedName("overview") val overview: String? = null,
  @SerializedName("runtime") val runtime: Int? = null,
  @SerializedName("status") val status: String? = null,
  @SerializedName("tagline") val tagline: String? = null,
  @SerializedName("title") val title: String? = null,
  @SerializedName("name") val name: String? = null,
  @SerializedName("biography") val biography: String? = null,
  @SerializedName("translation") val translation: String? = null,
)

data class ReviewDto(
  @SerializedName("id") val id: String = "",
  @SerializedName("author") val author: String = "",
  @SerializedName("author_details") val authorDetails: ReviewAuthorDto? = null,
  @SerializedName("content") val content: String = "",
  @SerializedName("created_at") val createdAt: String? = null,
  @SerializedName("updated_at") val updatedAt: String? = null,
  @SerializedName("url") val url: String? = null,
)

data class ReviewAuthorDto(
  @SerializedName("name") val name: String? = null,
  @SerializedName("username") val username: String? = null,
  @SerializedName("avatar_path") val avatarPath: String? = null,
  @SerializedName("rating") val rating: Double? = null,
)

data class WatchProvidersResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("results")
  val results: Map<String, WatchProviderRegionDto> = emptyMap(),
)

data class WatchProviderListResponseDto(
  @SerializedName("results") val results: List<WatchProviderDto> = emptyList(),
)

data class WatchProviderRegionDto(
  @SerializedName("link") val link: String? = null,
  @SerializedName("flatrate") val streaming: List<WatchProviderDto>? = null,
  @SerializedName("free") val free: List<WatchProviderDto>? = null,
  @SerializedName("ads") val ads: List<WatchProviderDto>? = null,
  @SerializedName("rent") val rent: List<WatchProviderDto>? = null,
  @SerializedName("buy") val buy: List<WatchProviderDto>? = null,
)

data class WatchProviderDto(
  @SerializedName("logo_path") val logoPath: String? = null,
  @SerializedName("provider_id") val providerId: Int = 0,
  @SerializedName("provider_name") val providerName: String = "",
  @SerializedName("display_priority") val displayPriority: Int = Int.MAX_VALUE,
)

data class ReleaseDatesResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("results") val results: List<CountryReleaseDatesDto> = emptyList(),
)

data class CountryReleaseDatesDto(
  @SerializedName("iso_3166_1") val countryCode: String = "",
  @SerializedName("release_dates") val releaseDates: List<ReleaseDateDto> = emptyList(),
)

data class ReleaseDateDto(
  @SerializedName("certification") val certification: String = "",
  @SerializedName("descriptors") val descriptors: List<String> = emptyList(),
  @SerializedName("iso_639_1") val languageCode: String? = null,
  @SerializedName("note") val note: String = "",
  @SerializedName("release_date") val releaseDate: String? = null,
  @SerializedName("type") val type: Int = 0,
)

data class ContentRatingsResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("results") val results: List<ContentRatingDto> = emptyList(),
)

data class ContentRatingDto(
  @SerializedName("descriptors") val descriptors: List<String> = emptyList(),
  @SerializedName("iso_3166_1") val countryCode: String = "",
  @SerializedName("rating") val rating: String = "",
)

data class EpisodeGroupsResponseDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("results") val results: List<EpisodeGroupSummaryDto> = emptyList(),
)

data class EpisodeGroupSummaryDto(
  @SerializedName("description") val description: String = "",
  @SerializedName("episode_count") val episodeCount: Int = 0,
  @SerializedName("group_count") val groupCount: Int = 0,
  @SerializedName("id") val id: String = "",
  @SerializedName("name") val name: String = "",
  @SerializedName("network") val network: NetworkDto? = null,
  @SerializedName("type") val type: Int = 0,
)

data class EpisodeGroupDetailsDto(
  @SerializedName("description") val description: String = "",
  @SerializedName("episode_count") val episodeCount: Int = 0,
  @SerializedName("group_count") val groupCount: Int = 0,
  @SerializedName("groups") val groups: List<EpisodeGroupDto> = emptyList(),
  @SerializedName("id") val id: String = "",
  @SerializedName("name") val name: String = "",
  @SerializedName("network") val network: NetworkDto? = null,
  @SerializedName("type") val type: Int = 0,
)

data class EpisodeGroupDto(
  @SerializedName("id") val id: String = "",
  @SerializedName("name") val name: String = "",
  @SerializedName("order") val order: Int = 0,
  @SerializedName("episodes") val episodes: List<EpisodeSummaryDto> = emptyList(),
  @SerializedName("locked") val isLocked: Boolean = false,
)

data class NetworkDto(
  @SerializedName("id") val id: Int = 0,
  @SerializedName("logo_path") val logoPath: String? = null,
  @SerializedName("name") val name: String = "",
  @SerializedName("origin_country") val originCountry: String? = null,
)

data class SeasonSummaryDto(
  @SerializedName("air_date") val airDate: String? = null,
  @SerializedName("episode_count") val episodeCount: Int = 0,
  @SerializedName("id") val id: Int = 0,
  @SerializedName("name") val name: String = "",
  @SerializedName("overview") val overview: String = "",
  @SerializedName("poster_path") val posterPath: String? = null,
  @SerializedName("season_number") val seasonNumber: Int = 0,
  @SerializedName("vote_average") val voteAverage: Double = 0.0,
)

data class EpisodeSummaryDto(
  @SerializedName("air_date") val airDate: String? = null,
  @SerializedName("episode_number") val episodeNumber: Int = 0,
  @SerializedName("episode_type") val episodeType: String? = null,
  @SerializedName("id") val id: Int = 0,
  @SerializedName("name") val name: String = "",
  @SerializedName("overview") val overview: String = "",
  @SerializedName("production_code") val productionCode: String? = null,
  @SerializedName("runtime") val runtime: Int? = null,
  @SerializedName("season_number") val seasonNumber: Int = 0,
  @SerializedName("show_id") val showId: Int = 0,
  @SerializedName("still_path") val stillPath: String? = null,
  @SerializedName("vote_average") val voteAverage: Double = 0.0,
  @SerializedName("vote_count") val voteCount: Int = 0,
  @SerializedName("crew") val crew: List<CrewMemberDto> = emptyList(),
  @SerializedName("guest_stars") val guestStars: List<CastMemberDto> = emptyList(),
)

data class SeasonDetailsDto(
  @SerializedName("_id") val internalId: String? = null,
  @SerializedName("air_date") val airDate: String? = null,
  @SerializedName("episodes") val episodes: List<EpisodeSummaryDto> = emptyList(),
  @SerializedName("name") val name: String = "",
  @SerializedName("networks") val networks: List<NetworkDto> = emptyList(),
  @SerializedName("overview") val overview: String = "",
  @SerializedName("id") val id: Int = 0,
  @SerializedName("poster_path") val posterPath: String? = null,
  @SerializedName("season_number") val seasonNumber: Int = 0,
  @SerializedName("vote_average") val voteAverage: Double = 0.0,
  @SerializedName("aggregate_credits")
  val aggregateCredits: AggregateCreditsResponseDto? = null,
  @SerializedName("credits") val credits: CreditsResponseDto? = null,
  @SerializedName("external_ids") val externalIds: ExternalIdsDto? = null,
  @SerializedName("images") val images: SeasonImagesResponseDto? = null,
  @SerializedName("translations") val translations: TranslationResponseDto? = null,
  @SerializedName("videos") val videos: VideoResponseDto? = null,
  @SerializedName("watch/providers")
  val watchProviders: WatchProvidersResponseDto? = null,
)

data class TvEpisodeDetailsDto(
  @SerializedName("air_date") val airDate: String? = null,
  @SerializedName("crew") val crew: List<CrewMemberDto> = emptyList(),
  @SerializedName("episode_number") val episodeNumber: Int = 0,
  @SerializedName("guest_stars") val guestStars: List<CastMemberDto> = emptyList(),
  @SerializedName("name") val name: String = "",
  @SerializedName("overview") val overview: String = "",
  @SerializedName("id") val id: Int = 0,
  @SerializedName("production_code") val productionCode: String? = null,
  @SerializedName("runtime") val runtime: Int? = null,
  @SerializedName("season_number") val seasonNumber: Int = 0,
  @SerializedName("still_path") val stillPath: String? = null,
  @SerializedName("vote_average") val voteAverage: Double = 0.0,
  @SerializedName("vote_count") val voteCount: Int = 0,
  @SerializedName("credits") val credits: EpisodeCreditsResponseDto? = null,
  @SerializedName("external_ids") val externalIds: ExternalIdsDto? = null,
  @SerializedName("images") val images: EpisodeImagesResponseDto? = null,
  @SerializedName("translations") val translations: TranslationResponseDto? = null,
  @SerializedName("videos") val videos: VideoResponseDto? = null,
)
