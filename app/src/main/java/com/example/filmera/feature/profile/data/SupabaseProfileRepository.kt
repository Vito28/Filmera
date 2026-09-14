package com.example.filmera.feature.profile.data

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.feature.profile.domain.ProfileRepository
import com.example.filmera.feature.profile.domain.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.plugins.HttpRequestTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Singleton
class SupabaseProfileRepository @Inject constructor(
  private val supabase: SupabaseClient,
) : ProfileRepository {
  override suspend fun getCurrentProfile(): DataResult<UserProfile> {
    val userId = supabase.auth.currentUserOrNull()?.id
      ?: return DataResult.Error(AppError.Unauthorized)

    return profileCall {
      val profile = supabase.from(PROFILES_TABLE)
        .select(
          columns = Columns.list(
            "username",
            "display_name",
            "avatar_url",
            "bio",
          ),
        ) {
          filter { eq("id", userId) }
          limit(1)
        }
        .decodeList<ProfileDto>()
        .singleOrNull()
        ?: throw MissingProfileException()

      UserProfile(
        displayName = profile.displayName.trim(),
        username = profile.username.trim(),
        avatarUrl = profile.avatarUrl?.trim()?.takeIf(String::isNotEmpty),
        bio = profile.bio?.trim()?.takeIf(String::isNotEmpty),
      )
    }
  }

  private suspend inline fun <T> profileCall(
    crossinline block: suspend () -> T,
  ): DataResult<T> = try {
    DataResult.Success(block())
  } catch (error: CancellationException) {
    throw error
  } catch (_: MissingProfileException) {
    DataResult.Error(AppError.NotFound)
  } catch (_: HttpRequestTimeoutException) {
    DataResult.Error(AppError.NetworkUnavailable)
  } catch (_: HttpRequestException) {
    DataResult.Error(AppError.NetworkUnavailable)
  } catch (error: PostgrestRestException) {
    DataResult.Error(
      when (error.statusCode) {
        401, 403 -> AppError.Unauthorized
        404 -> AppError.NotFound
        429 -> AppError.RateLimited
        in 500..599 -> AppError.ServerUnavailable
        else -> AppError.Unknown
      },
    )
  } catch (_: SerializationException) {
    DataResult.Error(AppError.InvalidResponse)
  } catch (_: Exception) {
    DataResult.Error(AppError.Unknown)
  }

  private companion object {
    const val PROFILES_TABLE = "profiles"
  }
}

@Serializable
private data class ProfileDto(
  val username: String,
  @SerialName("display_name") val displayName: String,
  @SerialName("avatar_url") val avatarUrl: String? = null,
  val bio: String? = null,
)

private class MissingProfileException : RuntimeException()
