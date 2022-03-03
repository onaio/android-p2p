package org.smartregister.p2p

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      // Don't need to create a notification channel on older versions.
      return
    }
    val syncNotificationChannel = NotificationChannel(
      SYNC_NOTIFICATION_CHANNEL_ID,
      "Sync Service Channel",
      NotificationManager.IMPORTANCE_DEFAULT
    )
    val notificationManager = getSystemService(NotificationManager::class.java)
    notificationManager.createNotificationChannel(syncNotificationChannel)
  }

  companion object {
    const val SYNC_NOTIFICATION_CHANNEL_ID = "syncNotificationChannel"
  }
}