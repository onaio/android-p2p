package org.smartregister.p2p

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.net.Socket

class SocketReceiverSession(private val socket: Socket): ReceiverSession {
  override fun receive() {
    val reader = socket.getInputStream().bufferedReader()
    reader.forEachLine {
      val decoded = Json.decodeFromString<SyncPayload>(it)
      Log.d(this::class.simpleName, """Message received: $decoded""")
    }
  }
}

interface ReceiverSession {
  fun receive()
}