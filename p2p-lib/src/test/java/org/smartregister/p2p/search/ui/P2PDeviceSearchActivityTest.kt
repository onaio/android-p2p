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

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import org.robolectric.shadows._Activity_
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.reflector.Reflector
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.R
import org.smartregister.p2p.authentication.model.DeviceRole
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.OnDeviceFound
import org.smartregister.p2p.data_sharing.WifiDirectDataSharingStrategy
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.shadows.ShadowAppDatabase
import org.smartregister.p2p.shadows.ShadowLocationServices

/** Test for class [P2PDeviceSearchActivity] */
@Config(shadows = [ShadowAppDatabase::class, ShadowLocationServices::class])
class P2PDeviceSearchActivityTest : RobolectricTest() {

  @get:Rule val executorRule = InstantTaskExecutorRule()

  private lateinit var p2PDeviceSearchActivity: P2PDeviceSearchActivity
  private lateinit var p2PDeviceSearchActivityController:
    ActivityController<P2PDeviceSearchActivity>
  private lateinit var dataSharingStrategy: DataSharingStrategy
  private lateinit var deviceInfo: DeviceInfo

  @Before
  fun setUp() {
    P2PLibrary.init(
      P2PLibrary.Options(RuntimeEnvironment.application, "password", "demo", mockk(), mockk())
    )
    dataSharingStrategy = mockk(relaxed = true)
    every { dataSharingStrategy.setActivity(any()) } just runs
    every { dataSharingStrategy.onResume(any()) } just runs
    every { dataSharingStrategy.onPause() } just runs

    P2PLibrary.getInstance().dataSharingStrategy = dataSharingStrategy

    p2PDeviceSearchActivityController =
      Robolectric.buildActivity(P2PDeviceSearchActivity::class.java)
    p2PDeviceSearchActivity =
      spyk(
        ReflectionHelpers.getField<P2PDeviceSearchActivity>(
          p2PDeviceSearchActivityController,
          "component"
        ),
        recordPrivateCalls = true
      )
    val _component_ = Reflector.reflector(_Activity_::class.java, p2PDeviceSearchActivity)
    ReflectionHelpers.setField(
      p2PDeviceSearchActivityController,
      "component",
      p2PDeviceSearchActivity
    )
    ReflectionHelpers.setField(p2PDeviceSearchActivityController, "_component_", _component_)
    p2PDeviceSearchActivityController.create().resume().get()

    val wifiP2pDevice =
      WifiP2pDevice().apply {
        deviceName = "Google Pixel"
        deviceAddress = "00:00:5e:00:53:af"
      }
    deviceInfo = WifiDirectDataSharingStrategy.WifiDirectDevice(wifiP2pDevice)
  }

  @After
  fun tearDown() {
    p2PDeviceSearchActivityController.pause().stop().destroy()
  }

  @Test
  fun `clicking scan button should call requestLocationPermissionsAndEnableLocation()`() {
    every { p2PDeviceSearchActivity.requestLocationPermissionsAndEnableLocation() } just runs

    p2PDeviceSearchActivity.findViewById<Button>(R.id.scanDevicesBtn).callOnClick()

    verify { p2PDeviceSearchActivity.requestLocationPermissionsAndEnableLocation() }
  }

  @Test
  fun testGetDeviceRole() {
    ReflectionHelpers.setField(p2PDeviceSearchActivity, "isSender", false)
    Assert.assertEquals(DeviceRole.RECEIVER, p2PDeviceSearchActivity.getDeviceRole())

    ReflectionHelpers.setField(p2PDeviceSearchActivity, "isSender", true)
    Assert.assertEquals(DeviceRole.SENDER, p2PDeviceSearchActivity.getDeviceRole())
  }

  @Test
  fun `startScanning() should call keepScreenOn(), showScanningDialog() and dataSharingStrategy#searchDevices()`() {
    every { p2PDeviceSearchActivity.keepScreenOn(true) } just runs
    every { dataSharingStrategy.searchDevices(any(), any()) } just runs
    every { p2PDeviceSearchActivity.showScanningDialog() } just runs

    p2PDeviceSearchActivity.startScanning()

    verify { p2PDeviceSearchActivity.keepScreenOn(true) }
    verify { dataSharingStrategy.searchDevices(any(), any()) }
    verify { p2PDeviceSearchActivity.showScanningDialog() }
  }

  @Test
  fun `startScanning() should call showDevicesList() when onDeviceFound#deviceFound is called`() {
    every { p2PDeviceSearchActivity.keepScreenOn(true) } just runs
    every { p2PDeviceSearchActivity.showDevicesList(any()) } just runs
    every { p2PDeviceSearchActivity.showScanningDialog() } just runs
    val onDeviceFoundSlot = slot<OnDeviceFound>()
    every { dataSharingStrategy.searchDevices(capture(onDeviceFoundSlot), any()) } just runs

    p2PDeviceSearchActivity.startScanning()

    val devicesList = listOf(deviceInfo)
    onDeviceFoundSlot.captured.deviceFound(devicesList)

    verify { p2PDeviceSearchActivity.showDevicesList(devicesList) }
  }

  @Test
  fun `startScanning() should call showP2PSelectPage() and update currentConnectedDevice when pairing#onSuccess is called`() {
    every { p2PDeviceSearchActivity.keepScreenOn(true) } just runs
    every { p2PDeviceSearchActivity.showScanningDialog() } just runs
    every { p2PDeviceSearchActivity.showP2PSelectPage(any(), any()) } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    val pairingListenerSlot = slot<DataSharingStrategy.PairingListener>()
    every { dataSharingStrategy.searchDevices(any(), capture(pairingListenerSlot)) } just runs

    Assert.assertNull(ReflectionHelpers.getField(p2PDeviceSearchActivity, "currentConnectedDevice"))

    p2PDeviceSearchActivity.startScanning()

    pairingListenerSlot.captured.onSuccess(deviceInfo)

    verify { p2PDeviceSearchActivity.showP2PSelectPage(any(), deviceInfo.getDisplayName()) }
    Assert.assertEquals(
      deviceInfo,
      ReflectionHelpers.getField(p2PDeviceSearchActivity, "currentConnectedDevice")
    )
  }

  @Test
  fun `startScanning() should call removeScanningDialog() and keepScreenOn() when pairing#onFailure is called`() {
    every { p2PDeviceSearchActivity.keepScreenOn(true) } just runs
    every { p2PDeviceSearchActivity.showScanningDialog() } just runs
    every { p2PDeviceSearchActivity.removeScanningDialog() } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    val pairingListenerSlot = slot<DataSharingStrategy.PairingListener>()
    every { dataSharingStrategy.searchDevices(any(), capture(pairingListenerSlot)) } just runs

    p2PDeviceSearchActivity.startScanning()

    pairingListenerSlot.captured.onFailure(deviceInfo, Exception(""))

    verify { p2PDeviceSearchActivity.removeScanningDialog() }
    verify { p2PDeviceSearchActivity.keepScreenOn(false) }
  }

  @Test
  fun `startScanning() should call removeScanningDialog() and keepScreenOn() when pairing#onDisconnected  is called and requestDisconnection is false`() {
    every { p2PDeviceSearchActivity.keepScreenOn(true) } just runs
    every { p2PDeviceSearchActivity.showScanningDialog() } just runs
    every { p2PDeviceSearchActivity.removeScanningDialog() } just runs
    every { p2PDeviceSearchActivity.showToast(any()) } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    val pairingListenerSlot = slot<DataSharingStrategy.PairingListener>()
    every { dataSharingStrategy.searchDevices(any(), capture(pairingListenerSlot)) } just runs

    p2PDeviceSearchActivity.startScanning()

    verify { p2PDeviceSearchActivity.keepScreenOn(true) }

    p2PDeviceSearchActivity.requestDisconnection = false
    pairingListenerSlot.captured.onDisconnected()

    verify { p2PDeviceSearchActivity.removeScanningDialog() }
    verify { p2PDeviceSearchActivity.keepScreenOn(false) }
    verify { p2PDeviceSearchActivity.showToast(any()) }
  }

  @Test
  fun `startScanning() should call showTransferCompleteDialog() when pairing#onDisconnected is called and requestDisconnection is false and isSenderSyncComplete is true`() {
    every { p2PDeviceSearchActivity.keepScreenOn(true) } just runs
    every { p2PDeviceSearchActivity.showScanningDialog() } just runs
    every { p2PDeviceSearchActivity.removeScanningDialog() } just runs
    every { p2PDeviceSearchActivity.showToast(any()) } just runs
    every { p2PDeviceSearchActivity.showTransferCompleteDialog() } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    val pairingListenerSlot = slot<DataSharingStrategy.PairingListener>()
    every { dataSharingStrategy.searchDevices(any(), capture(pairingListenerSlot)) } just runs

    p2PDeviceSearchActivity.startScanning()

    p2PDeviceSearchActivity.requestDisconnection = false
    ReflectionHelpers.setField(p2PDeviceSearchActivity, "isSenderSyncComplete", true)
    pairingListenerSlot.captured.onDisconnected()

    verify { p2PDeviceSearchActivity.showTransferCompleteDialog() }
  }

  @Test
  fun showToast() {
    p2PDeviceSearchActivity.showToast("Somebody")

    Assert.assertEquals("Somebody", ShadowToast.getTextOfLatestToast())
  }

  @Test
  fun `requestLocationPermissionsAndEnableLocation() should call requestAccessFineLocationIfNotGranted() and checkLocationEnabled()`() {
    every { p2PDeviceSearchActivity.checkLocationEnabled() } just runs
    every { p2PDeviceSearchActivity.requestAccessFineLocationIfNotGranted() } just runs

    p2PDeviceSearchActivity.requestLocationPermissionsAndEnableLocation()

    verify { p2PDeviceSearchActivity.checkLocationEnabled() }
    verify { p2PDeviceSearchActivity.requestAccessFineLocationIfNotGranted() }
  }

  @Test
  fun `checkLocationEnabled() should call startScanning() when location request is successful`() {
    val settingsClient = mockk<SettingsClient>()
    val task = mockk<Task<LocationSettingsResponse>>()
    ShadowLocationServices.setSettingsClient(settingsClient)

    val onSuccessListenerSlot = slot<OnSuccessListener<LocationSettingsResponse?>>()

    every { settingsClient.checkLocationSettings(any()) } returns task
    every { p2PDeviceSearchActivity.startScanning() } just runs
    every { task.addOnSuccessListener(any<Activity>(), capture(onSuccessListenerSlot)) } returns
      mockk()
    every { task.addOnFailureListener(any<Activity>(), any()) } returns mockk()

    p2PDeviceSearchActivity.checkLocationEnabled()

    onSuccessListenerSlot.captured.onSuccess(mockk())

    verify { p2PDeviceSearchActivity.startScanning() }
  }

  @Test
  fun `checkLocationEnabled() should call resolvableError#startResolutionForResult when onFailure is called`() {
    val settingsClient = mockk<SettingsClient>()
    val task = mockk<Task<LocationSettingsResponse>>()
    ShadowLocationServices.setSettingsClient(settingsClient)

    val onFailureListenerSlot = slot<OnFailureListener>()
    val resolvableError = mockk<ResolvableApiException>()

    every { settingsClient.checkLocationSettings(any()) } returns task
    every { p2PDeviceSearchActivity.startScanning() } just runs
    every { task.addOnSuccessListener(any<Activity>(), any()) } returns mockk()
    every { task.addOnFailureListener(any<Activity>(), capture(onFailureListenerSlot)) } returns
      mockk()
    every { resolvableError.startResolutionForResult(any(), any()) } just runs

    p2PDeviceSearchActivity.checkLocationEnabled()

    onFailureListenerSlot.captured.onFailure(resolvableError)

    verify { resolvableError.startResolutionForResult(any(), any()) }
  }

  @Test
  fun createLocationRequest() {
    val locationRequest = p2PDeviceSearchActivity.createLocationRequest()

    Assert.assertEquals(3600000, locationRequest.interval)
    Assert.assertEquals(LocationRequest.PRIORITY_LOW_POWER, locationRequest.priority)
  }

  @Test
  fun `onActivityResult() should call requestLocationPermissionsAndEnableLocation when resultCode is RESULT_OK and requestCode is REQUEST_CHECK_LOCATION_ENABLED`() {
    every { p2PDeviceSearchActivity.requestLocationPermissionsAndEnableLocation() } just runs

    ReflectionHelpers.callInstanceMethod<Void>(
      p2PDeviceSearchActivity,
      "onActivityResult",
      ReflectionHelpers.ClassParameter.from(
        Int::class.java,
        p2PDeviceSearchActivity.REQUEST_CHECK_LOCATION_ENABLED
      ),
      ReflectionHelpers.ClassParameter.from(Int::class.java, Activity.RESULT_OK),
      ReflectionHelpers.ClassParameter.from(Intent::class.java, mockk())
    )

    verify { p2PDeviceSearchActivity.requestLocationPermissionsAndEnableLocation() }
  }

  @Test
  fun `onOptionsItemSelected() should call finish() and return true when home item is selected`() {
    val menuItem = mockk<MenuItem>()
    every { menuItem.itemId } returns android.R.id.home

    val itemSelectionHandled = p2PDeviceSearchActivity.onOptionsItemSelected(menuItem)

    verify { p2PDeviceSearchActivity.finish() }
    Assert.assertTrue(itemSelectionHandled)
  }

  @Test
  fun `onCreateOptionsMenu() should return true and call MenuInflater#inflate`() {
    val menu: Menu = mockk()
    val menuInflater = spyk(p2PDeviceSearchActivity.menuInflater)
    every { p2PDeviceSearchActivity.menuInflater } returns menuInflater
    every { menuInflater.inflate(any(), any()) } just runs

    val onCreateOptionsHandled = p2PDeviceSearchActivity.onCreateOptionsMenu(menu)

    Assert.assertTrue(onCreateOptionsHandled)
    verify { menuInflater.inflate(R.menu.menu_main, menu) }
  }

  @Test
  fun showScanningDialog() {
    val bottomSheetDialog: BottomSheetDialog = mockk()

    every { p2PDeviceSearchActivity.createBottomSheetDialog() } returns bottomSheetDialog

    val intSlot = slot<Int>()
    every { p2PDeviceSearchActivity.getString(capture(intSlot)) } answers
      {
        RuntimeEnvironment.application.getString(intSlot.captured)
      }
    every { bottomSheetDialog.setContentView(any<Int>()) } just runs
    every { bottomSheetDialog.setTitle(any<String>()) } just runs
    every {
      hint(TextView::class)
      bottomSheetDialog.findViewById<TextView>(any())
    } returns null
    every {
      hint(ImageButton::class)
      bottomSheetDialog.findViewById<ImageButton>(any())
    } returns null
    every { bottomSheetDialog.cancel() } just runs
    every { bottomSheetDialog.setCancelable(any()) } just runs
    every { bottomSheetDialog.show() } just runs

    p2PDeviceSearchActivity.showScanningDialog()

    verify { bottomSheetDialog.setContentView(any<Int>()) }
    verify { bottomSheetDialog.setTitle("Nearby devices") }
    verify { bottomSheetDialog.setCancelable(false) }
    verify { bottomSheetDialog.show() }
  }

  @Test
  fun `stopScanning() should call dataSharingStrategy#stopSearchingDevices when scanning is true`() {
    ReflectionHelpers.setField(p2PDeviceSearchActivity, "scanning", true)
    every { dataSharingStrategy.stopSearchingDevices(any()) } just runs

    p2PDeviceSearchActivity.stopScanning()

    verify { dataSharingStrategy.stopSearchingDevices(any()) }
  }

  @Test
  fun `stopScanning() should change scanning to false when operationListener#onSuccess is called`() {
    ReflectionHelpers.setField(p2PDeviceSearchActivity, "scanning", true)
    val operationListenerSlot = slot<DataSharingStrategy.OperationListener>()
    every { dataSharingStrategy.stopSearchingDevices(capture(operationListenerSlot)) } just runs

    p2PDeviceSearchActivity.stopScanning()

    operationListenerSlot.captured.onSuccess(deviceInfo)

    Assert.assertFalse(ReflectionHelpers.getField(p2PDeviceSearchActivity, "scanning"))
  }

  @Test
  fun `removeScanningDialog() should call interactiveDialog#dismiss`() {
    p2PDeviceSearchActivity.interactiveDialog = mockk()
    every { p2PDeviceSearchActivity.interactiveDialog.dismiss() } just runs

    p2PDeviceSearchActivity.removeScanningDialog()

    verify { p2PDeviceSearchActivity.interactiveDialog.dismiss() }
  }

  @Test
  fun `showDeviceList() should call initInteractiveDialog() and set Adapter`() {
    val recyclerView = mockk<RecyclerView>()
    val bottomSheetDialog = mockk<BottomSheetDialog>()
    val loadingDevicesLayout = mockk<ConstraintLayout>()
    val devicesListLayout = mockk<ConstraintLayout>()
    justRun { p2PDeviceSearchActivity invokeNoArgs "initInteractiveDialog" }
    every { p2PDeviceSearchActivity getProperty "interactiveDialog" } returns bottomSheetDialog
    every { loadingDevicesLayout.visibility = any() } just runs
    every { devicesListLayout.visibility = any() } just runs
    every { recyclerView.adapter = any() } just runs
    every { recyclerView.layoutManager = any() } just runs
    every {
      hint(ConstraintLayout::class)
      bottomSheetDialog.findViewById<ConstraintLayout>(R.id.loading_devices_layout)
    } returns loadingDevicesLayout
    every {
      hint(ConstraintLayout::class)
      bottomSheetDialog.findViewById<ConstraintLayout>(R.id.devices_list_layout)
    } returns devicesListLayout
    every {
      hint(RecyclerView::class)
      bottomSheetDialog.findViewById<RecyclerView>(R.id.devices_list_recycler_view)
    } returns recyclerView

    val devicesList = listOf(deviceInfo)
    p2PDeviceSearchActivity.showDevicesList(devicesList)

    verify { p2PDeviceSearchActivity invokeNoArgs "initInteractiveDialog" }
    verify { recyclerView.adapter = any() }
    verify { recyclerView.layoutManager = any() }
    verify { loadingDevicesLayout.visibility = View.GONE }
    verify { devicesListLayout.visibility = View.VISIBLE }
  }

  @Test
  fun `showSenderDialog() should call initInteractiveDialog() and show dialog`() {
    val bottomSheetDialog = mockk<BottomSheetDialog>(relaxed = true)
    val dialogTitle = mockk<TextView>(relaxed = true)
    val dialogDescription = mockk<TextView>(relaxed = true)
    justRun { p2PDeviceSearchActivity invokeNoArgs "initInteractiveDialog" }
    every { p2PDeviceSearchActivity getProperty "interactiveDialog" } returns bottomSheetDialog
    every { p2PDeviceSearchActivity.getString(R.string.start_sending_data) } returns
      "Start sending data"
    every { p2PDeviceSearchActivity.getString(R.string.start_sending_data_to) } returns
      "Start sending data to %s"
    every { bottomSheetDialog.setContentView(any<Int>()) } just runs
    every {
      hint(TextView::class)
      bottomSheetDialog.findViewById<TextView>(R.id.data_transfer_title)
    } returns dialogTitle
    every {
      hint(TextView::class)
      bottomSheetDialog.findViewById<TextView>(R.id.data_transfer_description)
    } returns dialogDescription
    every {
      hint(ImageButton::class)
      bottomSheetDialog.findViewById<ImageButton>(R.id.data_transfer_dialog_close)
    } returns null
    every {
      hint(Button::class)
      bottomSheetDialog.findViewById<Button>(R.id.dataTransferBtn)
    } returns null

    p2PDeviceSearchActivity.showSenderDialog("nurse01")

    verify { p2PDeviceSearchActivity invokeNoArgs "initInteractiveDialog" }
    verify { bottomSheetDialog.setContentView(R.layout.data_transfer_bottom_sheet) }
    verify { dialogTitle.setText("Start sending data") }
    verify { dialogDescription.setText("Start sending data to nurse01") }
    verify { bottomSheetDialog.setCancelable(false) }
    verify { bottomSheetDialog.show() }
  }

  @Test
  fun `showReceiverDialog() should call initInteractiveDialog(), keepScreenOn(), processSenderDeviceDetails() and show dialog`() {
    val bottomSheetDialog = mockk<BottomSheetDialog>(relaxed = true)
    val dataTransferIcon = mockk<ImageView>(relaxed = true)
    val dataTransferBtn = mockk<Button>(relaxed = true)
    val dialogTitle = mockk<TextView>(relaxed = true)
    val dialogDescription = mockk<TextView>(relaxed = true)
    val p2PReceiverViewModel = mockk<P2PReceiverViewModel>(relaxed = true)

    every { p2PDeviceSearchActivity getProperty "p2PReceiverViewModel" } returns
      p2PReceiverViewModel
    justRun { p2PDeviceSearchActivity invokeNoArgs "initInteractiveDialog" }
    every { p2PDeviceSearchActivity getProperty "interactiveDialog" } returns bottomSheetDialog
    every { p2PDeviceSearchActivity.getString(R.string.start_receiving_data) } returns
      "Start receiving data"
    every { p2PDeviceSearchActivity.getString(R.string.waiting_for_transfer_to_start) } returns
      "Waiting for data transfer to start"
    every { bottomSheetDialog.setContentView(any<Int>()) } just runs
    every {
      hint(TextView::class)
      bottomSheetDialog.findViewById<TextView>(R.id.data_transfer_title)
    } returns dialogTitle
    every {
      hint(TextView::class)
      bottomSheetDialog.findViewById<TextView>(R.id.data_transfer_description)
    } returns dialogDescription
    every {
      hint(ImageButton::class)
      bottomSheetDialog.findViewById<ImageButton>(R.id.data_transfer_dialog_close)
    } returns null
    every {
      hint(ImageView::class)
      bottomSheetDialog.findViewById<ImageView>(R.id.data_transfer_icon)
    } returns dataTransferIcon
    every {
      hint(Button::class)
      bottomSheetDialog.findViewById<Button>(R.id.dataTransferBtn)
    } returns dataTransferBtn

    p2PDeviceSearchActivity.showReceiverDialog()

    verify { p2PDeviceSearchActivity invokeNoArgs "initInteractiveDialog" }
    verify { p2PDeviceSearchActivity.keepScreenOn(true) }
    verify { p2PReceiverViewModel.processSenderDeviceDetails() }
    verify { bottomSheetDialog.setContentView(R.layout.data_transfer_bottom_sheet) }
    verify { dialogTitle.setText("Start receiving data") }
    verify { dialogDescription.setText("Waiting for data transfer to start") }
    verify { bottomSheetDialog.setCancelable(false) }
    verify { bottomSheetDialog.show() }
    verify { dataTransferIcon.visibility = View.GONE }
    verify { dataTransferBtn.visibility = View.GONE }
  }

  @Test
  fun `showTransferProgressDialog() should call initInteractiveDialog() and show dialog when role is sender`() {
    val bottomSheetDialog = mockk<BottomSheetDialog>(relaxed = true)
    val dataTransferBtn = mockk<Button>(relaxed = true)
    val dialogTitle = mockk<TextView>(relaxed = true)
    val dialogDescription = mockk<TextView>(relaxed = true)

    justRun { p2PDeviceSearchActivity invokeNoArgs "initInteractiveDialog" }
    ReflectionHelpers.setField(p2PDeviceSearchActivity, "isSender", true)
    every { p2PDeviceSearchActivity getProperty "interactiveDialog" } returns bottomSheetDialog
    every { bottomSheetDialog.setContentView(any<Int>()) } just runs
    every {
      hint(TextView::class)
      bottomSheetDialog.findViewById<TextView>(R.id.data_transfer_title)
    } returns dialogTitle
    every {
      hint(TextView::class)
      bottomSheetDialog.findViewById<TextView>(R.id.data_transfer_description)
    } returns dialogDescription
    every {
      hint(ImageButton::class)
      bottomSheetDialog.findViewById<ImageButton>(R.id.data_transfer_dialog_close)
    } returns null
    every {
      hint(Button::class)
      bottomSheetDialog.findViewById<Button>(R.id.dataTransferBtn)
    } returns dataTransferBtn

    p2PDeviceSearchActivity.showTransferProgressDialog()

    verify { p2PDeviceSearchActivity invokeNoArgs "initInteractiveDialog" }
    verify { bottomSheetDialog.setContentView(R.layout.data_transfer_bottom_sheet) }
    verify { dialogTitle.setText("Sending...") }
    verify { dialogDescription.setText("Sending data to ") }
    verify { bottomSheetDialog.setCancelable(false) }
    verify { bottomSheetDialog.show() }
  }

  @Test
  fun `showTransferCompleteDialog() should call initInteractiveDialog(), keepScreenOn() and show dialog when keepScreenOnCounter is greater than 0`() {
    val bottomSheetDialog = mockk<BottomSheetDialog>(relaxed = true)
    val dataTransferBtn = mockk<Button>(relaxed = true)
    val dialogTitle = mockk<TextView>(relaxed = true)
    val dialogDescription = mockk<TextView>(relaxed = true)

    justRun { p2PDeviceSearchActivity invokeNoArgs "initInteractiveDialog" }
    every { p2PDeviceSearchActivity getProperty "interactiveDialog" } returns bottomSheetDialog
    every { bottomSheetDialog.setContentView(any<Int>()) } just runs
    ReflectionHelpers.setField(p2PDeviceSearchActivity, "keepScreenOnCounter", 2)
    every {
      hint(TextView::class)
      bottomSheetDialog.findViewById<TextView>(R.id.data_transfer_title)
    } returns dialogTitle
    every {
      hint(TextView::class)
      bottomSheetDialog.findViewById<TextView>(R.id.data_transfer_description)
    } returns dialogDescription
    every {
      hint(ImageButton::class)
      bottomSheetDialog.findViewById<ImageButton>(R.id.data_transfer_dialog_close)
    } returns null
    every {
      hint(Button::class)
      bottomSheetDialog.findViewById<Button>(R.id.dataTransferBtn)
    } returns dataTransferBtn

    p2PDeviceSearchActivity.showTransferCompleteDialog()

    verify { p2PDeviceSearchActivity invokeNoArgs "initInteractiveDialog" }
    verify { bottomSheetDialog.setContentView(R.layout.data_transfer_bottom_sheet) }
    verify { dialogTitle.setText("Data transfer complete") }
    verify { dialogDescription.setText("Device data successfully sent") }
    verify { bottomSheetDialog.setCancelable(false) }
    verify { bottomSheetDialog.show() }
    verify(exactly = 2) { p2PDeviceSearchActivity.keepScreenOn(false) }
  }

  @Test
  fun `connectToDevice() should call dataSharingStrategy#connect()`() {
    every { dataSharingStrategy.connect(deviceInfo, any()) } just runs

    p2PDeviceSearchActivity.connectToDevice(deviceInfo)

    verify { dataSharingStrategy.connect(deviceInfo, any()) }
  }

  @Test
  fun `showP2PSelectPage() should call showReceiverDialog() when deviceRole is receiver`() {
    every { p2PDeviceSearchActivity.showReceiverDialog() } just runs
    every {
      p2PDeviceSearchActivity.getString(R.string.connect_to_other_device_to_start_transfer)
    } returns "Connect to the other device using wifi-direct to start transfer"

    p2PDeviceSearchActivity.showP2PSelectPage(DeviceRole.RECEIVER, "Google Pixel")

    verify { p2PDeviceSearchActivity.showReceiverDialog() }
  }

  @Test
  fun `showP2PSelectPage() should call showSenderDialog() when deviceRole is Sender`() {
    every { p2PDeviceSearchActivity.showSenderDialog(any()) } just runs
    every {
      p2PDeviceSearchActivity.getString(R.string.connect_to_other_device_to_start_transfer)
    } returns "Connect to the other device using wifi-direct to start transfer"

    p2PDeviceSearchActivity.showP2PSelectPage(DeviceRole.SENDER, "Google Pixel")

    verify { p2PDeviceSearchActivity.showSenderDialog("Google Pixel") }
  }

  @Test
  fun `showP2PSelectPage() should disable scan devices button and change description text`() {
    val view = mockk<View>()
    val button = mockk<Button>()
    val textView = mockk<TextView>()
    every { p2PDeviceSearchActivity.showSenderDialog(any()) } just runs
    // every { p2PDeviceSearchActivity getProperty "rootView" } returns view
    ReflectionHelpers.setField(p2PDeviceSearchActivity, "rootView\$delegate", lazy { view })
    every { button.visibility = any() } just runs
    every { textView.setText(any<String>()) } just runs
    every {
      p2PDeviceSearchActivity.getString(R.string.connect_to_other_device_to_start_transfer)
    } returns "Connect to the other device using wifi-direct to start transfer"
    every {
      hint(Button::class)
      view.findViewById<Button>(R.id.scanDevicesBtn)
    } returns button
    every {
      hint(TextView::class)
      view.findViewById<TextView>(R.id.description)
    } returns textView

    p2PDeviceSearchActivity.showP2PSelectPage(DeviceRole.SENDER, "Google Pixel")

    verify { button.visibility = View.GONE }
    verify { textView.setText("Connect to the other device using wifi-direct to start transfer") }
  }

  @Test
  fun `getCurrentConnectedDevice() should call dataSharingStrategy#getCurrentDevice()`() {
    p2PDeviceSearchActivity.getCurrentConnectedDevice()

    verify { dataSharingStrategy.getCurrentDevice() }
  }

  @Test
  fun `senderSyncComplete() should change isSenderSyncComplete flag to false when false is passed`() {
    ReflectionHelpers.setField(p2PDeviceSearchActivity, "isSenderSyncComplete", true)
    Assert.assertTrue(ReflectionHelpers.getField(p2PDeviceSearchActivity, "isSenderSyncComplete"))

    p2PDeviceSearchActivity.senderSyncComplete(false)

    Assert.assertFalse(ReflectionHelpers.getField(p2PDeviceSearchActivity, "isSenderSyncComplete"))
  }

  @Test
  fun `senderSyncComplete() should change isSenderSyncComplete flag to true when true is passed`() {
    Assert.assertFalse(ReflectionHelpers.getField(p2PDeviceSearchActivity, "isSenderSyncComplete"))

    p2PDeviceSearchActivity.senderSyncComplete(true)

    Assert.assertTrue(ReflectionHelpers.getField(p2PDeviceSearchActivity, "isSenderSyncComplete"))
  }

  @Test
  fun `keepScreenOn() should increase counter and set window flag when true is passed`() {
    Assert.assertTrue(
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.and(
        p2PDeviceSearchActivity.window.attributes.flags
      ) == 0
    )
    Assert.assertEquals(
      0,
      ReflectionHelpers.getField(p2PDeviceSearchActivity, "keepScreenOnCounter")
    )

    p2PDeviceSearchActivity.keepScreenOn(true)

    Assert.assertEquals(
      1,
      ReflectionHelpers.getField(p2PDeviceSearchActivity, "keepScreenOnCounter")
    )
    // If the flag is false then 0 is returned, otherwise ~128
    Assert.assertTrue(
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.and(
        p2PDeviceSearchActivity.window.attributes.flags
      ) > 0
    )
  }

  @Test
  fun `keepScreenOn() should decrease counter and unset window flag when false is passed and previously set to true`() {
    p2PDeviceSearchActivity.keepScreenOn(true)
    Assert.assertTrue(
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.and(
        p2PDeviceSearchActivity.window.attributes.flags
      ) > 0
    )
    Assert.assertEquals(
      1,
      ReflectionHelpers.getField(p2PDeviceSearchActivity, "keepScreenOnCounter")
    )

    p2PDeviceSearchActivity.keepScreenOn(false)

    Assert.assertEquals(
      0,
      ReflectionHelpers.getField(p2PDeviceSearchActivity, "keepScreenOnCounter")
    )
    // If the flag is false then 0 is returned, otherwise ~128
    Assert.assertTrue(
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.and(
        p2PDeviceSearchActivity.window.attributes.flags
      ) == 0
    )
  }

  @Test
  fun `keepScreenOn() should decrease counter only when false is passed and previously set to false`() {
    Assert.assertTrue(
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.and(
        p2PDeviceSearchActivity.window.attributes.flags
      ) == 0
    )
    Assert.assertEquals(
      0,
      ReflectionHelpers.getField(p2PDeviceSearchActivity, "keepScreenOnCounter")
    )

    p2PDeviceSearchActivity.keepScreenOn(false)

    Assert.assertEquals(
      -1,
      ReflectionHelpers.getField(p2PDeviceSearchActivity, "keepScreenOnCounter")
    )
    // If the flag is false then 0 is returned, otherwise ~128
    Assert.assertTrue(
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.and(
        p2PDeviceSearchActivity.window.attributes.flags
      ) == 0
    )
  }

  @Test
  fun `keepScreenOn() should increase counter only when true is passed and previously set to true`() {
    p2PDeviceSearchActivity.keepScreenOn(true)

    val flagValue =
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.and(
        p2PDeviceSearchActivity.window.attributes.flags
      )
    Assert.assertTrue(flagValue > 0)
    Assert.assertEquals(
      1,
      ReflectionHelpers.getField(p2PDeviceSearchActivity, "keepScreenOnCounter")
    )

    p2PDeviceSearchActivity.keepScreenOn(true)

    Assert.assertEquals(
      2,
      ReflectionHelpers.getField(p2PDeviceSearchActivity, "keepScreenOnCounter")
    )
    // If the flag is false then 0 is returned, otherwise ~128
    Assert.assertEquals(
      flagValue,
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.and(
        p2PDeviceSearchActivity.window.attributes.flags
      )
    )
  }

  @Test
  fun `updateTransferProgress() updates transfer description button`() {
    p2PDeviceSearchActivity.interactiveDialog = mockk(relaxed = true)
    val dialogDescription = mockk<TextView>(relaxed = true)
    every {
      p2PDeviceSearchActivity.interactiveDialog.findViewById<TextView>(
        R.id.data_transfer_description
      )
    } returns dialogDescription

    p2PDeviceSearchActivity.updateTransferProgress(
      resStringId = R.string.transferring_x_records,
      percentageTransferred = 25,
      totalRecords = 40
    )

    verify { dialogDescription.setText("Transferring 25% of 40 records") }
  }

  @Test
  fun `initChannel calls dataSharingStrategy#initChannel`() {
    every { dataSharingStrategy.initChannel(any(), any()) } just runs

    p2PDeviceSearchActivity.initChannel()

    verify { dataSharingStrategy.initChannel(any(), any()) }
  }

  @Test
  fun `OnDeviceFound passed to initChannel should call showDevicesList() when onDeviceFound#deviceFound is called`() {
    every { p2PDeviceSearchActivity.keepScreenOn(true) } just runs
    every { p2PDeviceSearchActivity.showDevicesList(any()) } just runs
    every { p2PDeviceSearchActivity.showScanningDialog() } just runs

    val onDeviceFoundSlot = slot<OnDeviceFound>()
    every { dataSharingStrategy.initChannel(capture(onDeviceFoundSlot), any()) } just runs

    p2PDeviceSearchActivity.initChannel()

    val devicesList = listOf(deviceInfo)
    onDeviceFoundSlot.captured.deviceFound(devicesList)
    verify { p2PDeviceSearchActivity.showDevicesList(devicesList) }
  }

  @Test
  fun `PairingListener passed to initChannel() should call showP2PSelectPage() and update currentConnectedDevice when pairing#onSuccess is called`() {
    every { p2PDeviceSearchActivity.keepScreenOn(true) } just runs
    every { p2PDeviceSearchActivity.showScanningDialog() } just runs
    every { p2PDeviceSearchActivity.showP2PSelectPage(any(), any()) } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    val pairingListenerSlot = slot<DataSharingStrategy.PairingListener>()
    every { dataSharingStrategy.initChannel(any(), capture(pairingListenerSlot)) } just runs

    Assert.assertNull(ReflectionHelpers.getField(p2PDeviceSearchActivity, "currentConnectedDevice"))

    p2PDeviceSearchActivity.initChannel()

    pairingListenerSlot.captured.onSuccess(deviceInfo)

    verify { p2PDeviceSearchActivity.showP2PSelectPage(any(), deviceInfo.getDisplayName()) }
    Assert.assertEquals(
      deviceInfo,
      ReflectionHelpers.getField(p2PDeviceSearchActivity, "currentConnectedDevice")
    )
  }

  fun Dialog.isCancellable(): Boolean {
    return ReflectionHelpers.getField<Boolean>(this, "mCancelable")
  }
}
