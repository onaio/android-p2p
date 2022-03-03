package org.smartregister.p2p.search.contract

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 25-02-2022.
 */
interface P2PManagerListener : WifiP2pManager.ConnectionInfoListener {

    fun handleWifiP2pDisabled()

    fun handleWifiP2pEnabled()

    fun handleUnexpectedWifiP2pState(wifiState: Int)

    fun handleWifiP2pDevice(device: WifiP2pDevice)

    fun handleP2pDiscoveryStarted()

    fun handleP2pDiscoveryStopped()

    fun handleUnexpectedWifiP2pDiscoveryState(discoveryState: Int)

    fun handleP2pPeersChanged(peerDeviceList: WifiP2pDeviceList)

    fun handleAccessFineLocationNotGranted()

    fun handleMinimumSDKVersionNotMet(minimumSdkVersion: Int)
}