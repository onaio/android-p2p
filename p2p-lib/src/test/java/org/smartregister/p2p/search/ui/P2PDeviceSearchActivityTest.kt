/*
 * License text copyright (c) 2020 MariaDB Corporation Ab, All Rights Reserved.
 * “Business Source License” is a trademark of MariaDB Corporation Ab.
 *
 * Parameters
 *
 * Licensor:             Ona Systems, Inc.
 * Licensed Work:        android-p2p. The Licensed Work is (c) 2023 Ona Systems, Inc.
 * Additional Use Grant: You may make production use of the Licensed Work,
 *                       provided such use does not include offering the Licensed Work
 *                       to third parties on a hosted or embedded basis which is
 *                       competitive with Ona Systems' products.
 * Change Date:          Four years from the date the Licensed Work is published.
 * Change License:       MPL 2.0
 *
 * For information about alternative licensing arrangements for the Licensed Work,
 * please contact licensing@ona.io.
 *
 * Notice
 *
 * Business Source License 1.1
 *
 * Terms
 *
 * The Licensor hereby grants you the right to copy, modify, create derivative
 * works, redistribute, and make non-production use of the Licensed Work. The
 * Licensor may make an Additional Use Grant, above, permitting limited production use.
 *
 * Effective on the Change Date, or the fourth anniversary of the first publicly
 * available distribution of a specific version of the Licensed Work under this
 * License, whichever comes first, the Licensor hereby grants you rights under
 * the terms of the Change License, and the rights granted in the paragraph
 * above terminate.
 *
 * If your use of the Licensed Work does not comply with the requirements
 * currently in effect as described in this License, you must purchase a
 * commercial license from the Licensor, its affiliated entities, or authorized
 * resellers, or you must refrain from using the Licensed Work.
 *
 * All copies of the original and modified Licensed Work, and derivative works
 * of the Licensed Work, are subject to this License. This License applies
 * separately for each version of the Licensed Work and the Change Date may vary
 * for each version of the Licensed Work released by Licensor.
 *
 * You must conspicuously display this License on each original or modified copy
 * of the Licensed Work. If you receive the Licensed Work in original or
 * modified form from a third party, the terms and conditions set forth in this
 * License apply to your use of that work.
 *
 * Any use of the Licensed Work in violation of this License will automatically
 * terminate your rights under this License for the current and all other
 * versions of the Licensed Work.
 *
 * This License does not grant you any right in any trademark or logo of
 * Licensor or its affiliates (provided that you may use a trademark or logo of
 * Licensor as expressly required by this License).
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE LICENSED WORK IS PROVIDED ON
 * AN “AS IS” BASIS. LICENSOR HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS,
 * EXPRESS OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND
 * TITLE.
 */
package org.smartregister.p2p.search.ui

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.view.WindowManager
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
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.authentication.model.DeviceRole
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.WifiDirectDataSharingStrategy
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.model.TransferProgress
import org.smartregister.p2p.robolectric.ActivityRobolectricTest
import org.smartregister.p2p.search.ui.p2p.P2PViewModel
import org.smartregister.p2p.shadows.ShadowAppDatabase
import org.smartregister.p2p.shadows.ShadowLocationServices

/** Test for class [P2PDeviceSearchActivity] */
@Config(shadows = [ShadowAppDatabase::class, ShadowLocationServices::class])
class P2PDeviceSearchActivityTest : ActivityRobolectricTest() {

  @get:Rule val executorRule = InstantTaskExecutorRule()

  private lateinit var p2PDeviceSearchActivity: P2PDeviceSearchActivity
  private lateinit var p2PDeviceSearchActivityController:
    ActivityController<P2PDeviceSearchActivity>
  private lateinit var dataSharingStrategy: DataSharingStrategy
  private lateinit var deviceInfo: DeviceInfo
  private lateinit var p2PViewModel: P2PViewModel
  private lateinit var p2PReceiverViewModel: P2PReceiverViewModel
  private lateinit var p2PSenderViewModel: P2PSenderViewModel

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

    p2PDeviceSearchActivity = spyk(p2PDeviceSearchActivityController.create().resume().get())

    val wifiP2pDevice =
      WifiP2pDevice().apply {
        deviceName = "Google Pixel"
        deviceAddress = "00:00:5e:00:53:af"
      }
    deviceInfo = WifiDirectDataSharingStrategy.WifiDirectDevice(wifiP2pDevice)

    p2PViewModel = mockk(relaxed = true)
    every { p2PDeviceSearchActivity getProperty "p2PViewModel" } returns p2PViewModel

    p2PReceiverViewModel = mockk(relaxed = true)
    every { p2PDeviceSearchActivity getProperty "p2PReceiverViewModel" } returns
      p2PReceiverViewModel

    p2PSenderViewModel = mockk(relaxed = true)
    every { p2PDeviceSearchActivity getProperty "p2PSenderViewModel" } returns p2PSenderViewModel
  }

  @After
  override fun tearDown() {
    p2PDeviceSearchActivityController.pause().stop().destroy()
  }

  override fun getActivity(): Activity {
    return p2PDeviceSearchActivity
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
    every { p2PDeviceSearchActivity.hasPermission(any()) } returns true

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
    every { p2PViewModel.startScanning() } just runs
    every { task.addOnSuccessListener(any<Activity>(), capture(onSuccessListenerSlot)) } returns
      mockk()
    every { task.addOnFailureListener(any<Activity>(), any()) } returns mockk()

    p2PDeviceSearchActivity.checkLocationEnabled()

    onSuccessListenerSlot.captured.onSuccess(mockk())

    verify { p2PViewModel.startScanning() }
  }

  @Test
  fun `checkLocationEnabled() should call resolvableError#startResolutionForResult when onFailure is called`() {
    val settingsClient = mockk<SettingsClient>()
    val task = mockk<Task<LocationSettingsResponse>>()
    ShadowLocationServices.setSettingsClient(settingsClient)

    val onFailureListenerSlot = slot<OnFailureListener>()
    val resolvableError = mockk<ResolvableApiException>()

    every { settingsClient.checkLocationSettings(any()) } returns task
    every { p2PViewModel.startScanning() } just runs
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
  fun `getCurrentConnectedDevice() should call dataSharingStrategy#getCurrentDevice()`() {
    p2PDeviceSearchActivity.getCurrentConnectedDevice()

    verify { dataSharingStrategy.getCurrentDevice() }
  }

  @Test
  fun `senderSyncComplete() should call p2PViewModel#updateSenderSyncComplete when false is passed`() {
    p2PDeviceSearchActivity.senderSyncComplete(false)

    verify { p2PViewModel.updateSenderSyncComplete(false) }
  }

  @Test
  fun `senderSyncComplete() should call p2PViewModel#updateSenderSyncComplete when true is passed`() {
    p2PDeviceSearchActivity.senderSyncComplete(true)

    verify { p2PViewModel.updateSenderSyncComplete(true) }
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

    val transferProgress =
      TransferProgress(
        totalRecordCount = 100,
        transferredRecordCount = 50,
        percentageTransferred = 50
      )
    p2PDeviceSearchActivity.updateTransferProgress(transferProgress)

    val transferProgressSlot = slot<TransferProgress>()
    verify { p2PViewModel.updateTransferProgress(capture(transferProgressSlot)) }
  }

  @Test
  fun `notifyDataTransferStarting() should call p2PViewModel#updateP2PState() with TRANSFERRING_DATA param for sender device`() {
    p2PDeviceSearchActivity.notifyDataTransferStarting(DeviceRole.SENDER)
    verify { p2PViewModel.updateP2PState(P2PState.TRANSFERRING_DATA) }
  }

  @Test
  fun `notifyDataTransferStarting() should call p2PViewModel#updateP2PState() with RECEIVING_DATA param for receiver device`() {
    p2PDeviceSearchActivity.notifyDataTransferStarting(DeviceRole.RECEIVER)
    verify { p2PViewModel.updateP2PState(P2PState.RECEIVING_DATA) }
  }

  @Test
  fun `sendDeviceDetails() calls p2PSenderViewModel#sendDeviceDetails()`() {
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    p2PDeviceSearchActivity.sendDeviceDetails()
    verify { p2PSenderViewModel.sendDeviceDetails(deviceInfo) }
  }

  @Test
  fun `processSenderDeviceDetails() calls p2PReceiverViewModel#processSenderDeviceDetails()`() {
    p2PDeviceSearchActivity.processSenderDeviceDetails()
    verify { p2PReceiverViewModel.processSenderDeviceDetails() }
  }

  @Test
  fun `showTransferCompleteDialog() calls p2PViewModel#showTransferCompleteDialog()`() {
    p2PDeviceSearchActivity.showTransferCompleteDialog(P2PState.TRANSFER_COMPLETE)
    verify { p2PViewModel.showTransferCompleteDialog(P2PState.TRANSFER_COMPLETE) }
  }

  @Test
  fun `updateP2PState() should call p2PViewModel#updateP2PState() with correct value`() {
    p2PDeviceSearchActivity.updateP2PState(P2PState.RECEIVING_DATA)
    verify { p2PViewModel.updateP2PState(P2PState.RECEIVING_DATA) }
  }

  fun Dialog.isCancellable(): Boolean {
    return ReflectionHelpers.getField<Boolean>(this, "mCancelable")
  }
}
