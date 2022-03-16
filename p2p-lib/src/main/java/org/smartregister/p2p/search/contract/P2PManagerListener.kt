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
package org.smartregister.p2p.search.contract

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 25-02-2022. */
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
