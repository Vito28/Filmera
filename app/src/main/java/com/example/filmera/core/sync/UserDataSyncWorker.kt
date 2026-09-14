package com.example.filmera.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException

class UserDataSyncWorker(
  appContext: Context,
  workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
  override suspend fun doWork(): Result {
    val syncer = EntryPointAccessors.fromApplication(
      applicationContext,
      UserDataSyncEntryPoint::class.java,
    ).syncer()

    return try {
      syncer.syncNow()
      Result.success()
    } catch (error: CancellationException) {
      throw error
    } catch (_: Exception) {
      Result.retry()
    }
  }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserDataSyncEntryPoint {
  fun syncer(): UserDataSyncer
}
