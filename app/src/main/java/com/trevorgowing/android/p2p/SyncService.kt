package com.trevorgowing.android.p2p

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class SyncService : Service() {

  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    // TODO: Get host info from intent.
    Log.d(this::class.simpleName, "Start")

    val notificationIntent = Intent(this, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      notificationIntent,
      0
    )
    val notification = NotificationCompat.Builder(
      this,
      App.SYNC_NOTIFICATION_CHANNEL_ID
    )
      .setContentTitle("P2P Sync")
      .setContentText("Syncing resources...")
      .setContentIntent(pendingIntent)
      .setSmallIcon(R.drawable.ic_android)
      .setProgress(100, 0, false)
      .build()

    startForeground(
      // TODO: Keep track of the id so that the notification can be updated.
      1,
      notification
    )
    // TODO: Do the work. N.B. Code here is executed in the UI thread.
    // TODO: Report status. See https://developer.android.com/training/run-background-service/report-status.
    // TODO: stopSelf() after work.
    return START_NOT_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }
}