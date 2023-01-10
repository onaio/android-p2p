/*
 * Copyright 2022 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartregister.p2p

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import androidx.core.app.ActivityCompat
import org.smartregister.p2p.search.contract.P2PManagerListener

class WifiP2pBroadcastReceiver(
  private val manager: WifiP2pManager,
  private val channel: WifiP2pManager.Channel,
  private val listener: P2PManagerListener,
  private val context: Context
) : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
        val p2pGroupInfo =
          intent.getParcelableExtra<WifiP2pGroup>(WifiP2pManager.EXTRA_WIFI_P2P_GROUP)
        val wifiP2pInfo = intent.getParcelableExtra<WifiP2pInfo>(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
        listener.onConnectionInfoAvailable(wifiP2pInfo!!, p2pGroupInfo)
      }
      WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION ->
        handleDiscoveryChanged(intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1))
      WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> handlePeersChanged()
      WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION ->
        handleStateChanged(intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1))
      WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
        val device =
          intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE) as WifiP2pDevice?
        handleDeviceChanged(device = device)
      }
    }
  }

  /**
   * https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager#WIFI_P2P_CONNECTION_CHANGED_ACTION
   */
  private fun handleConnectionChanged() {
    manager.requestConnectionInfo(channel, listener)
  }

  /**
   * https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager#WIFI_P2P_DISCOVERY_CHANGED_ACTION
   */
  private fun handleDiscoveryChanged(discoveryState: Int) =
    when (discoveryState) {
      WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED -> listener.handleP2pDiscoveryStarted()
      WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED -> listener.handleP2pDiscoveryStopped()
      else -> listener.handleUnexpectedWifiP2pDiscoveryState(discoveryState)
    }

  /**
   * https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager#WIFI_P2P_PEERS_CHANGED_ACTION
   */
  private fun handlePeersChanged() {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      // TODO: Consider calling
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
      listener.handleAccessFineLocationNotGranted()
      return
    }
    manager.requestPeers(channel) { listener.handleP2pPeersChanged(it) }
  }

  /**
   * https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager#WIFI_P2P_STATE_CHANGED_ACTION
   */
  private fun handleStateChanged(wifiState: Int) =
    when (wifiState) {
      WifiP2pManager.WIFI_P2P_STATE_DISABLED -> listener.handleWifiP2pDisabled()
      WifiP2pManager.WIFI_P2P_STATE_ENABLED -> listener.handleWifiP2pEnabled()
      else -> listener.handleUnexpectedWifiP2pState(wifiState)
    }

  /**
   * https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager#WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
   */
  private fun handleDeviceChanged(device: WifiP2pDevice?) {
    if (device == null) {
      listener.handleWifiP2pDisabled()
    } else {
      listener.handleWifiP2pDevice(device)
    }
  }
}
