package com.example.filmera.feature.auth.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionPolicy @Inject constructor(
  @ApplicationContext context: Context,
) {
  private val preferences = context.getSharedPreferences(
    PREFERENCES_NAME,
    Context.MODE_PRIVATE,
  )

  fun shouldPersistSession(): Boolean =
    if (preferences.contains(KEY_PERSIST_SESSION)) {
      preferences.getBoolean(KEY_PERSIST_SESSION, true)
    } else {
      true
    }

  fun setPersistSession(persist: Boolean) {
    preferences.edit {
      putBoolean(KEY_PERSIST_SESSION, persist)
    }
  }

  private companion object {
    const val PREFERENCES_NAME = "filmera_auth_policy"
    const val KEY_PERSIST_SESSION = "persist_session"
  }
}
