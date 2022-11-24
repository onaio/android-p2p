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
import androidx.activity.compose.setContent
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
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.model.TransferProgress
import org.smartregister.p2p.search.adapter.DeviceListAdapter
import org.smartregister.p2p.search.contract.P2pModeSelectContract
import org.smartregister.p2p.search.ui.p2p.P2PScreen
import org.smartregister.p2p.search.ui.p2p.P2PViewModel
import org.smartregister.p2p.search.ui.theme.AppTheme
import org.smartregister.p2p.utils.DefaultDispatcherProvider
import org.smartregister.p2p.utils.getDeviceName
import org.smartregister.p2p.utils.isAppDebuggable
import org.smartregister.p2p.utils.startP2PScreen
import timber.log.Timber

/**
 * This is the exposed activity that provides access to all P2P operations and steps. It can be
 * called from other apps via [startP2PScreen] function.
 */
class P2PDeviceSearchActivity : AppCompatActivity(), P2pModeSelectContract.View {

  private val accessFineLocationPermissionRequestInt: Int = 12345
  private val p2PReceiverViewModel by viewModels<P2PReceiverViewModel> {
    P2PReceiverViewModel.Factory(
      context = this,
      dataSharingStrategy = dataSharingStrategy,
      DefaultDispatcherProvider()
    )
  }
  private val p2PSenderViewModel by viewModels<P2PSenderViewModel> {
    P2PSenderViewModel.Factory(
      context = this,
      dataSharingStrategy = dataSharingStrategy,
      DefaultDispatcherProvider()
    )
  }
  private val p2PViewModel by viewModels<P2PViewModel> {
    P2PViewModel.Factory(
      context = this,
      dataSharingStrategy = dataSharingStrategy,
      DefaultDispatcherProvider()
    )
  }
  private var scanning = false
  private var isSenderSyncComplete = false

  private lateinit var dataSharingStrategy: DataSharingStrategy

  private var keepScreenOnCounter = 0

  val REQUEST_CHECK_LOCATION_ENABLED = 2398

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Remaining setup for the DataSharingStrategy class
    dataSharingStrategy = P2PLibrary.getInstance().dataSharingStrategy
    dataSharingStrategy.setActivity(this)

    // use compose
    setContent {
      AppTheme {
        P2PScreen(
          p2PUiState = p2PViewModel.p2PUiState.value,
          onEvent = p2PViewModel::onEvent,
          p2PViewModel = p2PViewModel
        )
      }
    }

    if (Timber.treeCount == 0 && isAppDebuggable(this)) {
      Timber.plant(Timber.DebugTree())
    }

    title = getString(R.string.device_to_device_sync)

    /* findViewById<Button>(R.id.scanDevicesBtn).setOnClickListener {
      scanning = true
      requestLocationPermissionsAndEnableLocation()
    }*/
  }

  internal fun showToast(text: String) {
    Toast.makeText(this@P2PDeviceSearchActivity, text, Toast.LENGTH_LONG).show()
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
        p2PViewModel.startScanning()
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

  override fun onResume() {
    super.onResume()
    p2PViewModel.initChannel()
    dataSharingStrategy.onResume(isScanning = scanning)
  }

  override fun onPause() {
    super.onPause()

    dataSharingStrategy.onPause()
  }

  @RequiresApi(Build.VERSION_CODES.M)
  internal fun requestAccessFineLocationIfNotGranted() {
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

  fun sendDeviceDetails() {
    p2PSenderViewModel.sendDeviceDetails(getCurrentConnectedDevice())
  }

  fun processSenderDeviceDetails() {
    p2PReceiverViewModel.processSenderDeviceDetails()
  }

  override fun showTransferCompleteDialog() {
    p2PViewModel.showTransferCompleteDialog()
  }

  private fun logDebug(message: String) {
    // Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
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

  override fun updateTransferProgress(transferProgress: TransferProgress) {
    p2PViewModel.updateTransferProgress(transferProgress = transferProgress)
  }

  override fun notifyDataTransferStarting(deviceRole: DeviceRole) {
    when (deviceRole) {
      DeviceRole.SENDER -> p2PViewModel.updateP2PState(P2PState.TRANSFERRING_DATA)
      DeviceRole.RECEIVER -> p2PViewModel.updateP2PState(P2PState.RECEIVING_DATA)
    }
  }

  /**
   * Enables or disables the keep screen on flag to avoid the device going to sleep while there is a
   * sync happening
   *
   * @param enable `TRUE` to enable or `FALSE` disable
   */
  internal fun keepScreenOn(enable: Boolean) {
    if (enable) {
      keepScreenOnCounter++
      if (keepScreenOnCounter == 1) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    } else {
      keepScreenOnCounter--
      if (keepScreenOnCounter == 0) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    }
  }

  override fun onStop() {
    super.onStop()

    //dataSharingStrategy.onStop()
  }

}
