package com.example.filmera.core.sync

import com.example.filmera.core.database.LibraryItemEntity
import com.example.filmera.core.database.RecentSearchEntity
import com.example.filmera.core.database.UserPreferenceEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.call.body
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Singleton
class UserDataSyncRemoteSource @Inject constructor(
  private val supabase: SupabaseClient,
) {
  suspend fun syncPreference(entity: UserPreferenceEntity): CloudPreferenceDto =
    supabase.postgrest.rpc(
      function = "sync_user_preferences",
      parameters = buildJsonObject {
        put("p_preferred_media_types", entity.preferredMediaTypes.csvStrings().stringsJson())
        put("p_preferred_genre_ids", entity.preferredGenreIds.csvInts().intsJson())
        put("p_preferred_countries", entity.preferredCountries.csvStrings().stringsJson())
        put("p_preferred_movie_ids", entity.preferredMovieIds.csvInts().intsJson())
        put("p_preferred_tv_ids", entity.preferredTvIds.csvInts().intsJson())
        put("p_preferred_person_ids", entity.preferredPersonIds.csvInts().intsJson())
        put(
          "p_preferred_language_codes",
          entity.preferredLanguageCodes.csvStrings().stringsJson(),
        )
        put("p_onboarding_completed", entity.onboardingCompleted)
        put("p_client_updated_at", entity.updatedAt.asInstantString())
      },
    ).decodeList<CloudPreferenceDto>().single()

  suspend fun getPreferences(): CloudPreferenceDto? =
    supabase.postgrest.rpc("get_user_preferences_sync")
      .decodeList<CloudPreferenceDto>()
      .singleOrNull()

  suspend fun ensureMedia(entity: LibraryItemEntity): Long =
    supabase.functions.invoke(
      function = "ensure-media",
      body = buildJsonObject {
        put("media_type", if (entity.mediaType == "TV_SHOW") "tv" else "movie")
        put("tmdb_id", entity.mediaId)
      },
      headers = Headers.build {
        append(HttpHeaders.ContentType, "application/json")
      },
    ).body<EnsureMediaSyncResponse>().mediaId

  suspend fun syncLibrary(
    entity: LibraryItemEntity,
    remoteMediaId: Long,
  ): CloudLibraryDto = supabase.postgrest.rpc(
    function = "sync_user_library",
    parameters = buildJsonObject {
      put("p_media_id", remoteMediaId)
      put("p_watch_status", entity.watchStatus.lowercase())
      put("p_is_favorite", entity.isFavorite)
      put("p_added_at", entity.addedAt.asInstantString())
      put("p_deleted", entity.isDeleted)
      put("p_client_updated_at", entity.updatedAt.asInstantString())
    },
  ).decodeList<CloudLibraryDto>().single()

  suspend fun getLibrary(): List<CloudLibraryDto> =
    supabase.postgrest.rpc("get_user_library_sync").decodeList()

  suspend fun syncSearch(entity: RecentSearchEntity): CloudSearchDto =
    supabase.postgrest.rpc(
      function = "sync_search_history",
      parameters = buildJsonObject {
        put("p_query", entity.query)
        put("p_searched_at", entity.searchedAt.asInstantString())
        put("p_deleted", entity.isDeleted)
        put("p_client_updated_at", entity.updatedAt.asInstantString())
      },
    ).decodeList<CloudSearchDto>().single()

  suspend fun clearSearches(updatedAt: Long): Long {
    val timestamp = supabase.postgrest.rpc(
      function = "clear_search_history",
      parameters = buildJsonObject {
        put("p_client_updated_at", updatedAt.asInstantString())
      },
    ).decodeAs<String>()
    return timestamp.asEpochMillis()
  }

  suspend fun getSearches(): List<CloudSearchDto> =
    supabase.postgrest.rpc("get_user_search_history_sync").decodeList()
}

@Serializable
data class CloudPreferenceDto(
  @SerialName("preferred_media_types") val preferredMediaTypes: List<String>,
  @SerialName("preferred_genre_ids") val preferredGenreIds: List<Int>,
  @SerialName("preferred_countries") val preferredCountries: List<String>,
  @SerialName("preferred_movie_ids") val preferredMovieIds: List<Int>,
  @SerialName("preferred_tv_ids") val preferredTvIds: List<Int>,
  @SerialName("preferred_person_ids") val preferredPersonIds: List<Int>,
  @SerialName("preferred_language_codes") val preferredLanguageCodes: List<String>,
  @SerialName("onboarding_completed") val onboardingCompleted: Boolean,
  @SerialName("client_updated_at") val clientUpdatedAt: String,
)

@Serializable
data class CloudLibraryDto(
  @SerialName("media_id") val mediaId: Long,
  @SerialName("media_type") val mediaType: String,
  @SerialName("tmdb_id") val tmdbId: Int,
  val title: String,
  @SerialName("poster_path") val posterPath: String? = null,
  @SerialName("release_date") val releaseDate: String? = null,
  @SerialName("original_language") val originalLanguage: String? = null,
  @SerialName("watch_status") val watchStatus: String,
  @SerialName("is_favorite") val isFavorite: Boolean,
  @SerialName("added_at") val addedAt: String,
  @SerialName("deleted_at") val deletedAt: String? = null,
  @SerialName("client_updated_at") val clientUpdatedAt: String,
)

@Serializable
data class CloudSearchDto(
  @SerialName("normalized_query") val normalizedQuery: String,
  val query: String,
  @SerialName("searched_at") val searchedAt: String,
  @SerialName("deleted_at") val deletedAt: String? = null,
  @SerialName("client_updated_at") val clientUpdatedAt: String,
)

@Serializable
private data class EnsureMediaSyncResponse(
  @SerialName("media_id") val mediaId: Long,
)

internal fun String.asEpochMillis(): Long = Instant.parse(this).toEpochMilli()

private fun Long.asInstantString(): String = Instant.ofEpochMilli(this).toString()

private fun String.csvStrings(): List<String> =
  split(',').map(String::trim).filter(String::isNotEmpty)

private fun String.csvInts(): List<Int> = csvStrings().mapNotNull(String::toIntOrNull)

private fun List<String>.stringsJson(): JsonArray = JsonArray(map(::JsonPrimitive))

private fun List<Int>.intsJson(): JsonArray = JsonArray(map(::JsonPrimitive))
