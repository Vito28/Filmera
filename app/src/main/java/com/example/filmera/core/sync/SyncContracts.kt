package com.example.filmera.core.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import javax.inject.Inject
import javax.inject.Singleton

interface CurrentUserProvider {
  fun currentUserId(): String?
}

@Singleton
class SupabaseCurrentUserProvider @Inject constructor(
  private val supabase: SupabaseClient,
) : CurrentUserProvider {
  override fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id
}

interface SyncRequestScheduler {
  fun schedule()
}

interface UserDataSyncer {
  suspend fun syncNow()
}

object LegacyCurrentUserProvider : CurrentUserProvider {
  override fun currentUserId(): String = com.example.filmera.core.database.LEGACY_OWNER_ID
}

object NoOpSyncRequestScheduler : SyncRequestScheduler {
  override fun schedule() = Unit
}

object NoOpUserDataSyncer : UserDataSyncer {
  override suspend fun syncNow() = Unit
}
