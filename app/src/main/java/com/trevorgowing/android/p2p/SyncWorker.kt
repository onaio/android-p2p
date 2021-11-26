package com.trevorgowing.android.p2p

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

class SyncWorker(context: Context, parameters: WorkerParameters) :
  CoroutineWorker(context, parameters) {

  override suspend fun doWork(): Result {
    val groupOwnerAddress = inputData.getString(GROUP_OWNER_ADDRESS_KEY) ?: return Result.failure()
    setForeground(createForegroundInfo(groupOwnerAddress))
    // TODO: Do work.
    delay(30000)
    return Result.success()
  }

  private fun createForegroundInfo(groupOwnerAddress: String): ForegroundInfo {
    val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

    val notificationIntent = Intent(applicationContext, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
      applicationContext,
      0,
      notificationIntent,
      0
    )

    val notification =
      NotificationCompat.Builder(applicationContext, App.SYNC_NOTIFICATION_CHANNEL_ID)
        .setContentTitle("P2P Sync")
        .setContentText("""Syncing resources... $groupOwnerAddress""")
        .setContentIntent(pendingIntent)
        .setSmallIcon(R.drawable.ic_android)
        .setOngoing(true)
        .setProgress(100, 50, true)
        .addAction(android.R.drawable.ic_delete, "Cancel", cancelIntent)
        .build()

    return ForegroundInfo(
      // TODO: Keep track of the id so that the notification can be updated.
      1,
      notification
    )
  }

  companion object {
    const val GROUP_OWNER_ADDRESS_KEY = "GROUP_OWNER_ADDRESS"
  }
}
