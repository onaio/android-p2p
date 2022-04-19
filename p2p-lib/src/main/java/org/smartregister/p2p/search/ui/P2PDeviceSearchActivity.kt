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
package org.smartregister.p2p.search.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.R
import org.smartregister.p2p.authentication.model.DeviceRole
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.OnDeviceFound
import org.smartregister.p2p.search.adapter.DeviceListAdapter
import org.smartregister.p2p.search.contract.P2pModeSelectContract
import org.smartregister.p2p.utils.getDeviceName
import org.smartregister.p2p.utils.startP2PScreen
import timber.log.Timber

/**
 * This is the exposed activity that provides access to all P2P operations and steps. It can be
 * called from other apps via [startP2PScreen] function.
 */
class P2PDeviceSearchActivity : AppCompatActivity(), P2pModeSelectContract {
  /*
  private val wifiP2pManager: WifiP2pManager by lazy(LazyThreadSafetyMode.NONE) {
    getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
  }
  private var wifiP2pChannel: WifiP2pManager.Channel? = null
  private var wifiP2pReceiver: BroadcastReceiver? = null*/
  private val accessFineLocationPermissionRequestInt: Int = 12345
  private val p2PReceiverViewModel by viewModels<P2PReceiverViewModel> {
    P2PReceiverViewModel.Factory(context = this, dataSharingStrategy = dataSharingStrategy)
  }
  private val p2PSenderViewModel by viewModels<P2PSenderViewModel> {
    P2PSenderViewModel.Factory(context = this, dataSharingStrategy = dataSharingStrategy)
  }
  private var isSender = false
  private var scanning = false
  private lateinit var interactiveDialog: BottomSheetDialog
  private var currentConnectedDevice: DeviceInfo? = null

  private lateinit var dataSharingStrategy: DataSharingStrategy

  private val rootView: View by lazy { findViewById(R.id.device_search_root_layout) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_p2_pdevice_search)

    if (Timber.treeCount == 0) {
      Timber.plant(Timber.DebugTree())
    }

    title = getString(R.string.device_to_device_sync)
    supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel)

    // Remaining setup for the DataSharingStrategy class
    dataSharingStrategy = P2PLibrary.getInstance().dataSharingStrategy
    dataSharingStrategy.setActivity(this)

    findViewById<Button>(R.id.scanDevicesBtn).setOnClickListener {
      scanning = true
      startScanning()
    }
  }

  fun startScanning() {
    // Wifi P2p
    /*wifiP2pChannel = wifiP2pManager.initialize(this, mainLooper, null)
    wifiP2pChannel?.also { channel ->
      wifiP2pReceiver = WifiP2pBroadcastReceiver(wifiP2pManager, channel, this, this)
    }

    // renameWifiDirectName();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestAccessFineLocationIfNotGranted()
    }

    listenForWifiP2pIntents()
    initiatePeerDiscovery()
    */

    dataSharingStrategy.searchDevices(
      object : OnDeviceFound {
        override fun deviceFound(devices: List<DeviceInfo>) {
          showDevicesList(devices)
        }

        override fun failed(ex: Exception) {
          TODO("Not yet implemented")
        }
      },
      object: DataSharingStrategy.PairingListener {

        override fun onSuccess(device: DeviceInfo?) {

          if (currentConnectedDevice == null) {
            Timber.e("Devices paired with another: DeviceInfo is null")
          }

          currentConnectedDevice = device
          val displayName = device?.getDisplayName() ?: "Unknown"
          showP2PSelectPage(getDeviceRole(), displayName)
        }
      }
    )

    showScanningDialog()
  }
  /*
  fun renameWifiDirectName() {
    val deviceName = getDeviceName(this)

    if (Build.VERSION.SDK_INT > 24) {
      // Copy the text to the Clipboard
      val clipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText(getString(R.string.wifi_direct_name), deviceName)
      clipboard.setPrimaryClip(clip)

      Toast.makeText(
          this,
          getString(R.string.wifi_direct_name_setup_instructions),
          Toast.LENGTH_LONG
        )
        .show()

      val turnWifiOn = Intent(Settings.ACTION_WIFI_SETTINGS)
      startActivity(turnWifiOn)
    } else {
      val setDeviceNameMethod =
        wifiP2pManager.javaClass.getMethod(
          "setDeviceName",
          wifiP2pChannel!!.javaClass,
          String::class.java,
          WifiP2pManager.ActionListener::class.java
        )
      setDeviceNameMethod.invoke(
        wifiP2pManager,
        wifiP2pChannel,
        deviceName,
        object : WifiP2pManager.ActionListener {
          override fun onSuccess() {
            Timber.e("Change device name worked")
          }

          override fun onFailure(reason: Int) {
            Timber.e("Change device name did not work")
          }
        }
      )
    }
  }*/

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
      return true
    }

    return super.onOptionsItemSelected(item)
  }

  override fun onResume() {
    super.onResume()

    /*if (scanning) {
      listenForWifiP2pIntents()
      initiatePeerDiscoveryOnceAccessFineLocationGranted()
      requestDeviceInfo()
      requestConnectionInfo()
    }*/

    // TODO: Add this to the DataSharingStrategy interface
    // dataSharingStrategy.onResume()
  }
  /*
  private fun listenForWifiP2pIntents() {
    wifiP2pReceiver?.also {
      registerReceiver(
        it,
        IntentFilter().apply {
          addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
          addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
          addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
          addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
          addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
      )
    }
  }*/
  /*
  private fun initiatePeerDiscoveryOnceAccessFineLocationGranted() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        requestAccessFineLocationIfNotGranted()
      } else {
        handleMinimumSDKVersionNotMet(Build.VERSION_CODES.M)
      }
    } else {
      initiatePeerDiscovery()
    }
  }*/
  /*
  private fun requestDeviceInfo() {
    wifiP2pChannel?.also { wifiP2pChannel ->
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
          PackageManager.PERMISSION_GRANTED
      ) {
        return handleAccessFineLocationNotGranted()
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        wifiP2pManager.requestDeviceInfo(wifiP2pChannel) {
          if (it != null) {
            handleWifiP2pDevice(it)
          }
        }
      } else {
        // TODO: Handle fetching device details
      }
    }
  }

  private fun requestConnectionInfo() {
    wifiP2pManager.requestConnectionInfo(wifiP2pChannel) { onConnectionInfoAvailable(it) }
  }*/

  override fun onPause() {
    super.onPause()
    /*wifiP2pReceiver?.also { unregisterReceiver(it) }*/

    // TODO: Fix this onPause
    // dataSharingStrategy.onPause()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  @RequiresApi(Build.VERSION_CODES.M)
  private fun requestAccessFineLocationIfNotGranted() {
    when (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
      PackageManager.PERMISSION_GRANTED -> logDebug("Wifi P2P: Access fine location granted")
      else -> {
        logDebug("Wifi P2P: Requesting access fine location permission")
        return requestPermissions(
          arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
          accessFineLocationPermissionRequestInt
        )
      }
    }
  }
  /*
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    if (requestCode == accessFineLocationPermissionRequestInt) {
      val accessFineLocationPermissionIndex =
        permissions.indexOfFirst { it == Manifest.permission.ACCESS_FINE_LOCATION }
      if (grantResults[accessFineLocationPermissionIndex] == PackageManager.PERMISSION_GRANTED) {
        return logDebug("Wifi P2P: Access fine location granted")
      }
    }
  }

  override fun handleWifiP2pDisabled() {
    val message = "Wifi P2P: Disabled"
    Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
    Timber.d(message)
  }

  override fun handleWifiP2pEnabled() {
    val message = "Wifi P2P: Enabled"
    Timber.d(message)
  }

  override fun handleUnexpectedWifiP2pState(wifiState: Int) {
    val message = "Wifi P2P: Unexpected state: $wifiState"
    Timber.d(message)
  }

  override fun handleWifiP2pDevice(device: WifiP2pDevice) {
    if (device.deviceName != getDeviceName(this)) {
      // renameWifiDirectName()
    }

    Timber.d("Wifi P2P: Device: ${device.deviceAddress}")
  }

  private fun initiatePeerDiscovery() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      return handleAccessFineLocationNotGranted()
    }

    wifiP2pManager.discoverPeers(
      wifiP2pChannel,
      object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
          handleP2pDiscoverySuccess()
        }

        override fun onFailure(reason: Int) {
          handleP2pDiscoveryFailure(reason)
        }
      }
    )
    Timber.d("Peer discovery initiated")
  }

  private fun handleP2pDiscoverySuccess() {
    val message = "Wifi P2P: Peer discovery succeeded"
    Timber.d(message)
  }

  private fun handleP2pDiscoveryFailure(reasonInt: Int) {
    val reason = getWifiP2pReason(reasonInt)
    val message = "Wifi P2P: Peer discovery failed: $reason"
    Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
    Timber.d(message)
  }

  override fun handleP2pDiscoveryStarted() {
    Timber.d("Wifi P2P: Peer discovery started")
  }

  override fun handleP2pDiscoveryStopped() {
    Timber.d("Wifi P2P: Peer discovery stopped")
  }

  override fun handleUnexpectedWifiP2pDiscoveryState(discoveryState: Int) {
    Timber.d("Wifi P2P: Unexpected discovery state: $discoveryState")
  }

  override fun handleP2pPeersChanged(peerDeviceList: WifiP2pDeviceList) {
    Timber.d("Wifi P2P: Peers x ${peerDeviceList.deviceList.size}")
    showDevicesList(peerDeviceList)
  }*/

  fun showScanningDialog() {
    interactiveDialog = BottomSheetDialog(this)
    interactiveDialog.setContentView(R.layout.devices_list_bottom_sheet)
    interactiveDialog.setTitle(getString(R.string.nearby_devices))

    interactiveDialog
      .findViewById<TextView>(R.id.device_search_description)
      ?.setText(
        String.format(getString(R.string.looking_for_nearby_devices_as), getDeviceName(this))
      )

    interactiveDialog.findViewById<ImageButton>(R.id.dialog_close)?.setOnClickListener {
      interactiveDialog.cancel()
      // stopScanning()
    }

    interactiveDialog.setCancelable(false)
    interactiveDialog.show()
  }

  fun showDevicesList(peerDeviceList: List<DeviceInfo>) {
    initInteractiveDialog()
    interactiveDialog.findViewById<ConstraintLayout>(R.id.loading_devices_layout)?.visibility =
      View.GONE
    interactiveDialog.findViewById<ConstraintLayout>(R.id.devices_list_layout)?.visibility =
      View.VISIBLE
    val devicesListRecyclerView =
      interactiveDialog.findViewById<RecyclerView>(R.id.devices_list_recycler_view)
    if (devicesListRecyclerView != null) {
      devicesListRecyclerView.adapter = DeviceListAdapter(peerDeviceList, { connectToDevice(it) })
      devicesListRecyclerView.layoutManager = LinearLayoutManager(this)
    }
  }

  fun connectToDevice(device: DeviceInfo) {
    isSender = true
    dataSharingStrategy.connect(
      device,
      object : DataSharingStrategy.OperationListener {
        override fun onSuccess(device: DeviceInfo?) {
          currentConnectedDevice = device
          showP2PSelectPage(getDeviceRole(), currentConnectedDevice!!.getDisplayName())
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          TODO("Not yet implemented")
        }
      }
    )
  }

  fun showSenderDialog(practitionerName: String) {
    initInteractiveDialog()
    interactiveDialog.setContentView(R.layout.data_transfer_bottom_sheet)

    interactiveDialog
      .findViewById<TextView>(R.id.data_transfer_title)
      ?.setText(getString(R.string.start_sending_data))

    interactiveDialog
      .findViewById<TextView>(R.id.data_transfer_description)
      ?.setText(String.format(getString(R.string.start_sending_data_to), practitionerName))

    interactiveDialog.findViewById<ImageButton>(R.id.data_transfer_dialog_close)
      ?.setOnClickListener { interactiveDialog.cancel() }

    interactiveDialog.findViewById<Button>(R.id.dataTransferBtn)?.setOnClickListener {
      // initiate data transfer
      p2PSenderViewModel.requestSyncParams(getCurrentConnectedDevice())
    }

    interactiveDialog.setCancelable(false)
    interactiveDialog.show()
  }

  fun showReceiverDialog() {
    initInteractiveDialog()
    interactiveDialog.setContentView(R.layout.data_transfer_bottom_sheet)

    interactiveDialog
      .findViewById<TextView>(R.id.data_transfer_title)
      ?.setText(getString(R.string.start_receiving_data))

    interactiveDialog
      .findViewById<TextView>(R.id.data_transfer_description)
      ?.setText(getString(R.string.waiting_for_transfer_to_start))

    interactiveDialog.findViewById<ImageButton>(R.id.data_transfer_dialog_close)
      ?.setOnClickListener { interactiveDialog.cancel() }

    interactiveDialog.findViewById<ImageView>(R.id.data_transfer_icon)?.visibility = View.GONE

    interactiveDialog.findViewById<Button>(R.id.dataTransferBtn)?.visibility = View.GONE

    interactiveDialog.setCancelable(false)
    interactiveDialog.show()

    // listen for messages
    p2PReceiverViewModel.processSyncParamsRequest()
  }

  private fun initInteractiveDialog() {
    if (!this::interactiveDialog.isInitialized) {
      interactiveDialog = BottomSheetDialog(this)
    }
  }
  /*
  private fun connectToDevice(device: WifiP2pDevice) {
    Timber.d("Wifi P2P: Initiating connection to device: ${device.deviceName}")
    isSender = true
    val wifiP2pConfig = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
    wifiP2pChannel?.also { wifiP2pChannel ->
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
          PackageManager.PERMISSION_GRANTED
      ) {
        return handleAccessFineLocationNotGranted()
      }
      wifiP2pManager.connect(
        wifiP2pChannel,
        wifiP2pConfig,
        object : WifiP2pManager.ActionListener {
          override fun onSuccess() {
            handleDeviceConnectionSuccess(device)
          }

          override fun onFailure(reason: Int) {
            handleDeviceConnectionFailure(device, reason)
          }
        }
      )
    }
  }

  private fun handleDeviceConnectionSuccess(device: WifiP2pDevice) {
    Timber.d("Wifi P2P: Successfully connected to device: ${device.deviceAddress}")
    currentConnectedDevice = DeviceInfo(device)
    showP2PSelectPage(getDeviceRole(), device.deviceName)
  }

  private fun handleDeviceConnectionFailure(device: WifiP2pDevice, reasonInt: Int) {
    val reason = getWifiP2pReason(reasonInt)
    Timber.d("Wifi P2P: Failed to connect to device: ${device.deviceAddress} due to: $reason")
  }

  override fun handleAccessFineLocationNotGranted() {
    val message = "Wifi P2P: Access fine location permission not granted"
    Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
    Timber.d(message)
  }

  override fun handleMinimumSDKVersionNotMet(minimumSdkVersion: Int) {
    logDebug("Wifi P2P: Minimum SDK Version not met: $minimumSdkVersion")
  }

  override fun onConnectionInfoAvailable(info: WifiP2pInfo, wifiP2pGroup: WifiP2pGroup?) {
    val message =
      "Connection info available: groupFormed = ${info.groupFormed}, isGroupOwner = ${info.isGroupOwner}"
    Timber.d(message)
    if (info.groupFormed && !isSender) {
      // Start syncing given the ip addresses
      showReceiverDialog()
    }
  }*/

  override fun showP2PSelectPage(deviceRole: DeviceRole, deviceName: String) {
    rootView
      .findViewById<TextView>(R.id.description)
      ?.setText(getString(R.string.connect_to_other_device_to_start_transfer))
    rootView.findViewById<Button>(R.id.scanDevicesBtn)?.visibility = View.GONE

    when (deviceRole) {
      DeviceRole.RECEIVER -> showReceiverDialog()
      DeviceRole.SENDER -> showSenderDialog(deviceName)
    }
  }

  override fun getDeviceRole(): DeviceRole {
    return if (isSender) DeviceRole.SENDER else DeviceRole.RECEIVER
  }
  /*

    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
      onConnectionInfoAvailable(info, null)
    }

    private fun getWifiP2pReason(reasonInt: Int): String =
      when (reasonInt) {
        0 -> "Error"
        1 -> "Unsupported"
        2 -> "Busy"
        else -> "Unknown"
      }
  */

  private fun logDebug(message: String) {
    Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
    Timber.d(message)
  }

  fun sendSyncParams() {
    // Respond with the acceptable data types each with its lastUpdated timestamp and batch size
  }

  fun getCurrentConnectedDevice(): DeviceInfo? {
    return dataSharingStrategy.getCurrentDevice()
  }
}
