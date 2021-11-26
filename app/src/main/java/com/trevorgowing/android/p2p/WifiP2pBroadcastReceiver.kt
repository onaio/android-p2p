package com.trevorgowing.android.p2p

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.core.app.ActivityCompat

class WifiP2pBroadcastReceiver(
  private val manager: WifiP2pManager,
  private val channel: WifiP2pManager.Channel,
  private val activity: MainActivity
) : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> handleConnectionChanged()
      WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION ->
        handleDiscoveryChanged(intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1))
      WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> handlePeersChanged()
      WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION ->
        handleStateChanged(intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1))
      WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> handleDeviceChanged()
    }
  }

  /**
   * https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager#WIFI_P2P_CONNECTION_CHANGED_ACTION
   */
  private fun handleConnectionChanged() {
    manager.requestConnectionInfo(channel, activity)
  }

  /**
   * https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager#WIFI_P2P_DISCOVERY_CHANGED_ACTION
   */
  private fun handleDiscoveryChanged(discoveryState: Int) =
    when (discoveryState) {
      WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED -> activity.handleP2pDiscoveryStarted()
      WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED -> activity.handleP2pDiscoveryStopped()
      else -> activity.handleUnexpectedWifiP2pDiscoveryState(discoveryState)
    }

  /**
   * https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager#WIFI_P2P_PEERS_CHANGED_ACTION
   */
  private fun handlePeersChanged() {
    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      // TODO: Consider calling
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
      activity.handleAccessFineLocationNotGranted()
      return
    }
    manager.requestPeers(channel) { activity.handleP2pPeersChanged(it) }
  }

  /**
   * https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager#WIFI_P2P_STATE_CHANGED_ACTION
   */
  private fun handleStateChanged(wifiState: Int) =
    when (wifiState) {
      WifiP2pManager.WIFI_P2P_STATE_DISABLED -> activity.handleWifiP2pDisabled()
      WifiP2pManager.WIFI_P2P_STATE_ENABLED -> activity.handleWifiP2pEnabled()
      else -> activity.handleUnexpectedWifiP2pState(wifiState)
    }

  /**
   * https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager#WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
   */
  private fun handleDeviceChanged() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) !=
          PackageManager.PERMISSION_GRANTED
      ) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        activity.handleAccessFineLocationNotGranted()
        return
      }
      manager.requestDeviceInfo(channel) {
        if (it == null) {
          activity.handleWifiP2pDisabled()
        } else {
          activity.handleWifiP2pDevice(it)
        }
      }
    } else {
      activity.handleMinimumSDKVersionNotMet(
          Build.VERSION_CODES.Q,)
    }
  }
}
