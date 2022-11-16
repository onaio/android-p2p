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
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.just
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
import org.smartregister.p2p.data_sharing.WifiDirectDataSharingStrategy
import org.smartregister.p2p.model.TransferProgress
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.search.ui.p2p.P2PViewModel
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
  private lateinit var p2PViewModel: P2PViewModel

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

    p2PViewModel = mockk(relaxed = true)

    /*every { p2PDeviceSearchActivity getProperty "p2PViewModel" } returns
    p2PViewModel*/
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
    every { p2PViewModel.startScanning(any()) } just runs
    every { task.addOnSuccessListener(any<Activity>(), capture(onSuccessListenerSlot)) } returns
      mockk()
    every { task.addOnFailureListener(any<Activity>(), any()) } returns mockk()

    p2PDeviceSearchActivity.checkLocationEnabled()

    onSuccessListenerSlot.captured.onSuccess(mockk())

    verify { p2PViewModel.startScanning(any()) }
  }

  @Test
  fun `checkLocationEnabled() should call resolvableError#startResolutionForResult when onFailure is called`() {
    val settingsClient = mockk<SettingsClient>()
    val task = mockk<Task<LocationSettingsResponse>>()
    ShadowLocationServices.setSettingsClient(settingsClient)

    val onFailureListenerSlot = slot<OnFailureListener>()
    val resolvableError = mockk<ResolvableApiException>()

    every { settingsClient.checkLocationSettings(any()) } returns task
    every { p2PViewModel.startScanning(any()) } just runs
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

    val transferProgress =
      TransferProgress(
        totalRecordCount = 100,
        transferredRecordCount = 50,
        percentageTransferred = 50
      )
    p2PDeviceSearchActivity.updateTransferProgress(transferProgress)

    verify { p2PDeviceSearchActivity.updateTransferProgress(transferProgress) }
  }

  @Test
  fun `initChannel calls dataSharingStrategy#initChannel`() {
    every { dataSharingStrategy.initChannel(any(), any()) } just runs

    p2PDeviceSearchActivity.initChannel()

    verify { dataSharingStrategy.initChannel(any(), any()) }
  }

  fun Dialog.isCancellable(): Boolean {
    return ReflectionHelpers.getField<Boolean>(this, "mCancelable")
  }
}
