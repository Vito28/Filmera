package com.example.filmera.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDataSyncScheduler @Inject constructor(
  @ApplicationContext context: Context,
) : SyncRequestScheduler {
  private val workManager = WorkManager.getInstance(context)

  override fun schedule() {
    val request = OneTimeWorkRequestBuilder<UserDataSyncWorker>()
      .setConstraints(
        Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build(),
      )
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
      .build()
    workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
  }

  companion object {
    const val WORK_NAME = "user-data-cloud-sync"
  }
}
