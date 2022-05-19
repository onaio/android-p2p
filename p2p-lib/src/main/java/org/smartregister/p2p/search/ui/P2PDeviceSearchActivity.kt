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
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
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
class P2PDeviceSearchActivity : AppCompatActivity(), P2pModeSelectContract.View {

  private val accessFineLocationPermissionRequestInt: Int = 12345
  private val p2PReceiverViewModel by viewModels<P2PReceiverViewModel> {
    P2PReceiverViewModel.Factory(context = this, dataSharingStrategy = dataSharingStrategy)
  }
  private val p2PSenderViewModel by viewModels<P2PSenderViewModel> {
    P2PSenderViewModel.Factory(context = this, dataSharingStrategy = dataSharingStrategy)
  }
  private var isSender = false
  private var scanning = false
  private var isSenderSyncComplete = false
  private lateinit var interactiveDialog: BottomSheetDialog
  private var currentConnectedDevice: DeviceInfo? = null

  private lateinit var dataSharingStrategy: DataSharingStrategy

  private var keepScreenOnCounter = 0

  private val rootView: View by lazy { findViewById(R.id.device_search_root_layout) }

  val REQUEST_CHECK_LOCATION_ENABLED = 2398
  var requestDisconnection = false

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
      requestLocationPermissionsAndEnableLocation()
    }
  }

  fun startScanning() {
    keepScreenOn(true)
    dataSharingStrategy.searchDevices(
      object : OnDeviceFound {
        override fun deviceFound(devices: List<DeviceInfo>) {
          showDevicesList(devices)
        }

        override fun failed(ex: Exception) {
          // TODO implement handling of search for devices failure
        }
      },
      object : DataSharingStrategy.PairingListener {

        override fun onSuccess(device: DeviceInfo?) {

          if (currentConnectedDevice == null) {
            Timber.e("Devices paired with another: DeviceInfo is null")
          }

          currentConnectedDevice = device
          val displayName = device?.getDisplayName() ?: "Unknown"
          showP2PSelectPage(getDeviceRole(), displayName)
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          keepScreenOn(false)
          Timber.e("Devices searching failed")
          Timber.e(ex)
          removeScanningDialog()
        }

        override fun onDisconnected() {
          if (!requestDisconnection) {
            removeScanningDialog()
            Toast.makeText(
                this@P2PDeviceSearchActivity,
                "Connection was disconnected",
                Toast.LENGTH_LONG
              )
              .show()

            if (isSenderSyncComplete) {
              showTransferCompleteDialog()
            }
            Timber.e("Successful on disconnect")
            Timber.e("isSenderSyncComplete $isSenderSyncComplete")
            // But use a flag to determine if sync was completed
          }
        }
      }
    )

    showScanningDialog()
  }

  fun requestLocationPermissionsAndEnableLocation() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestAccessFineLocationIfNotGranted()
    }

    checkLocationEnabled()
  }

  /**
   * Checks if location is currently enabled
   *
   * @param activity
   */
  fun checkLocationEnabled() {
    val builder = LocationSettingsRequest.Builder().addLocationRequest(createLocationRequest())
    val result = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
    result.addOnSuccessListener(
      this,
      OnSuccessListener<LocationSettingsResponse?> {
        // All location settings are satisfied. The client can initialize
        // location requests here.
        startScanning()
      }
    )
    result.addOnFailureListener(
      this,
      OnFailureListener { e ->
        if (e is ResolvableApiException) {
          // Location settings are not satisfied, but this can be fixed
          // by showing the user a dialog.
          try {
            // Show the dialog by calling startResolutionForResult(),
            // and check the result in onActivityResult().
            val resolvable = e as ResolvableApiException
            resolvable.startResolutionForResult(
              this@P2PDeviceSearchActivity,
              REQUEST_CHECK_LOCATION_ENABLED
            )
          } catch (sendEx: IntentSender.SendIntentException) {
            // Ignore the error.
            Timber.e(sendEx)
          }
        }
      }
    )
  }

  fun createLocationRequest(): LocationRequest {
    return LocationRequest.create().apply {
      interval = 3600000
      fastestInterval = 3600000
      priority = LocationRequest.PRIORITY_LOW_POWER
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == REQUEST_CHECK_LOCATION_ENABLED && resultCode == RESULT_OK) {
      requestLocationPermissionsAndEnableLocation()
    }
  }

  /* DO NOT DELETE THIS ->> FOR USE LATER
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

    dataSharingStrategy.onResume(isScanning = scanning)
  }

  override fun onPause() {
    super.onPause()

    dataSharingStrategy.onPause()
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
      stopScanning()
    }

    interactiveDialog.setCancelable(false)
    interactiveDialog.show()
  }

  fun stopScanning() {
    if (scanning) {
      dataSharingStrategy.stopSearchingDevices(
        object : DataSharingStrategy.OperationListener {
          override fun onSuccess(device: DeviceInfo?) {
            scanning = false
            Timber.e("Searching stopped successfully")
          }

          override fun onFailure(device: DeviceInfo?, ex: Exception) {
            Timber.e(ex)
          }
        }
      )
    }
  }

  fun removeScanningDialog() {
    if (::interactiveDialog.isInitialized) {
      interactiveDialog.dismiss()
    }
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
          scanning = false
          currentConnectedDevice = device
          showP2PSelectPage(getDeviceRole(), currentConnectedDevice!!.getDisplayName())
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          // TODO: Not yet implemented
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
      keepScreenOn(true)
      p2PSenderViewModel.sendDeviceDetails(getCurrentConnectedDevice())
      showTransferProgressDialog()
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
    keepScreenOn(true)
    p2PReceiverViewModel.processSenderDeviceDetails()
  }

  fun showTransferProgressDialog() {
    initInteractiveDialog()
    interactiveDialog.setContentView(R.layout.data_transfer_bottom_sheet)

    val transferTitle =
      if (isSender) this.getString(R.string.sending) else this.getString(R.string.receiving)
    interactiveDialog.findViewById<TextView>(R.id.data_transfer_title)?.setText(transferTitle)

    val transferDescription =
      if (isSender) String.format(getString(R.string.sending_data_to), "")
      else String.format(getString(R.string.receiving_data_from), "")
    interactiveDialog
      .findViewById<TextView>(R.id.data_transfer_description)
      ?.setText(transferDescription)

    interactiveDialog.findViewById<ImageButton>(R.id.data_transfer_dialog_close)
      ?.setOnClickListener { interactiveDialog.cancel() }

    interactiveDialog.findViewById<Button>(R.id.dataTransferBtn)?.apply {
      setOnClickListener {
        // close wifi direct connection
      }
      setText(getString(R.string.cancel))
    }

    interactiveDialog.setCancelable(false)
    interactiveDialog.show()
  }

  override fun showTransferCompleteDialog() {
    while (keepScreenOnCounter > 0) {
      keepScreenOn(false)
    }

    initInteractiveDialog()
    interactiveDialog.setContentView(R.layout.data_transfer_bottom_sheet)

    interactiveDialog
      .findViewById<TextView>(R.id.data_transfer_title)
      ?.setText(getString(R.string.data_transfer_comlete))

    interactiveDialog
      .findViewById<TextView>(R.id.data_transfer_description)
      ?.setText(String.format(getString(R.string.device_data_successfully_sent)))

    interactiveDialog.findViewById<ImageButton>(R.id.data_transfer_dialog_close)
      ?.setOnClickListener { interactiveDialog.cancel() }

    interactiveDialog.findViewById<Button>(R.id.dataTransferBtn)?.apply {
      setOnClickListener {
        // close wifi direct connection
        finish()
      }
      setText(getString(R.string.okay))
    }

    interactiveDialog.setCancelable(false)
    interactiveDialog.show()
  }

  private fun initInteractiveDialog() {
    if (!this::interactiveDialog.isInitialized) {
      interactiveDialog = BottomSheetDialog(this)
    }
  }

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

  private fun logDebug(message: String) {
    Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
    Timber.d(message)
  }

  fun sendSyncParams() {
    // Respond with the acceptable data types each with its lastUpdated timestamp and batch size
  }

  override fun getCurrentConnectedDevice(): DeviceInfo? {
    return dataSharingStrategy.getCurrentDevice()
  }

  override fun senderSyncComplete(complete: Boolean) {
    isSenderSyncComplete = complete
    Timber.e("sender sync complete $isSenderSyncComplete")
  }

  /**
   * Enables or disables the keep screen on flag to avoid the device going to sleep while there is a
   * sync happening
   *
   * @param enable `TRUE` to enable or `FALSE` disable
   */
  protected fun keepScreenOn(enable: Boolean) {
    if (enable) {
      keepScreenOnCounter++
      if (keepScreenOnCounter == 1) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    } else {
      keepScreenOnCounter--
      if (keepScreenOnCounter == 0) {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    }
  }
}
