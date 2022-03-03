package org.smartregister.p2p

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import org.smartregister.p2p.search.ui.P2PDeviceSearchActivity

const val PORT = 8988
const val SOCKET_TIMEOUT = 5_000

class SyncWorker(context: Context, parameters: WorkerParameters) :
  CoroutineWorker(context, parameters) {

  override suspend fun doWork(): Result {
    val groupOwner = inputData.getBoolean(GROUP_OWNER_KEY, false)
    Log.d("Wifi P2P: ${this::class.simpleName}", "Group Owner: $groupOwner")
    val groupOwnerAddress = inputData.getString(GROUP_OWNER_ADDRESS_KEY) ?: return Result.failure()
    Log.d("Wifi P2P: ${this::class.simpleName}", "Group Owner Address: $groupOwnerAddress")
    val sender = inputData.getBoolean(SENDER_KEY, false)
    Log.d("Wifi P2P: ${this::class.simpleName}", "Sender: $sender")
    setForeground(createForegroundInfo(groupOwnerAddress))
    return try {
      withContext(Dispatchers.IO) {
        if (groupOwner) {
          // Start a server to accept connections.
          acceptedConnectionsToServerSocket(sender)
        } else {
          // Connect to the server running on the group owner device.
          connectToServerSocket(groupOwnerAddress, sender)
        }
      }
      Result.success()
    } catch (e: Exception) {
      Log.e("Wifi P2P: ${this::class.simpleName}", e.message, e)
      Result.failure()
    }
  }

  private fun acceptedConnectionsToServerSocket(sender: Boolean): Result = try {
    ServerSocket(PORT).use { server ->
      server.accept().use { socket ->
        transmit(sender, socket)
      }
    }
    Result.success()
  } catch (e: Exception) {
    Log.e("Wifi P2P: ${this::class.simpleName}", e.message, e)
    Result.failure()
  }

  private fun connectToServerSocket(
    groupOwnerAddress: String,
    sender: Boolean
  ): Result = try {
    Socket().use { socket ->
      socket.bind(null)
      socket.connect(InetSocketAddress(groupOwnerAddress, PORT), SOCKET_TIMEOUT)
      transmit(sender, socket)
    }
    Result.success()
  } catch (e: Exception) {
    Log.e("Wifi P2P: ${this::class.simpleName}", e.message, e)
    Result.failure()
  }

  private fun transmit(sender: Boolean, socket: Socket) {
    if (sender) {
      val session: SenderSession = SocketSenderSession(socket)
      session.send()
    } else {
      val session: ReceiverSession = SocketReceiverSession(socket)
      session.receive()
    }
  }

  private fun createForegroundInfo(groupOwnerAddress: String): ForegroundInfo {
    val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

    val notificationIntent = Intent(applicationContext, P2PDeviceSearchActivity::class.java)
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
    const val GROUP_OWNER_KEY = "GROUP_OWNER"
    const val GROUP_OWNER_ADDRESS_KEY = "GROUP_OWNER_ADDRESS"
    const val SENDER_KEY = "SENDER"
  }
}
