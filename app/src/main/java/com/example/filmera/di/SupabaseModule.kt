package com.example.filmera.di

import com.example.filmera.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
  @Provides
  @Singleton
  @OptIn(SupabaseExperimental::class)
  fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
  ) {
    install(Auth) {
      scheme = AUTH_DEEP_LINK_SCHEME
      host = AUTH_DEEP_LINK_HOST
      flowType = FlowType.PKCE
      defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
    }
    install(Postgrest)
    install(Storage)
    install(Realtime)
    install(Functions) {
      requireValidSession = true
    }
  }

  const val AUTH_DEEP_LINK_SCHEME = "filmera"
  const val AUTH_DEEP_LINK_HOST = "auth-callback"
}
