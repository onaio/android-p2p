/*
 * Copyright 2022-2023 Ona Systems, Inc
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
package org.smartregister.p2p.search.ui.p2p

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.smartregister.p2p.authentication.model.DeviceRole
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.WifiDirectDataSharingStrategy
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.search.ui.p2p.components.CIRCULAR_PROGRESS_INDICATOR_TEST_TAG

class BottomSheetScreenTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  @OptIn(ExperimentalMaterialApi::class)
  fun bottomSheetScreenRendersCorrectlyForSenderDevice() {
    composeRule.setContent {
      BottomSheet(
        deviceList = listOf(populateDeviceInfo()),
        onEvent = {},
        modalBottomSheetState = ModalBottomSheetState(ModalBottomSheetValue.HalfExpanded),
        p2PUiState = P2PUiState(),
        deviceName = "John",
        deviceRole = DeviceRole.SENDER,
        p2PState = P2PState.SEARCHING_FOR_RECIPIENT
      )
    }

    composeRule.onNodeWithText("Searching for nearby recipient").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Searching nearby device as").assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithTag(BOTTOM_SHEET_CANCEL_ICON_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
    composeRule.onNodeWithTag(P2P_BOTTOM_SHEET_LIST).assertExists()
  }

  @Test
  @OptIn(ExperimentalMaterialApi::class)
  fun bottomSheetScreenRendersCorrectlyForReceiverDevice() {
    composeRule.setContent {
      BottomSheet(
        deviceList = emptyList(),
        onEvent = {},
        modalBottomSheetState = ModalBottomSheetState(ModalBottomSheetValue.HalfExpanded),
        p2PUiState = P2PUiState(),
        deviceName = "John",
        deviceRole = DeviceRole.RECEIVER,
        p2PState = P2PState.RECEIVING_DATA
      )
    }

    composeRule.onNodeWithText("Waiting to pair...").assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithText("Waiting to be paired with sender as")
      .assertExists()
      .assertIsDisplayed()
    composeRule
      .onNodeWithTag(BOTTOM_SHEET_CANCEL_ICON_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
  }

  @Test
  @OptIn(ExperimentalMaterialApi::class)
  fun bottomSheetScreenRendersCorrectlyForCompleteTransferStatus() {
    composeRule.setContent {
      BottomSheet(
        deviceList = emptyList(),
        onEvent = {},
        modalBottomSheetState = ModalBottomSheetState(ModalBottomSheetValue.HalfExpanded),
        p2PUiState = P2PUiState(),
        deviceName = "John",
        deviceRole = DeviceRole.RECEIVER,
        p2PState = P2PState.TRANSFER_COMPLETE
      )
    }

    composeRule.onNodeWithText("OKay").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Receive Data").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Device data successfully sent").assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithTag(BOTTOM_SHEET_BUTTON_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
    composeRule
      .onNodeWithTag(BOTTOM_SHEET_CANCEL_ICON_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
  }

  @Test
  @OptIn(ExperimentalMaterialApi::class)
  fun bottomSheetScreenRendersCorrectlyForPairDevicesFoundStatus() {
    composeRule.setContent {
      BottomSheet(
        deviceList = emptyList(),
        onEvent = {},
        modalBottomSheetState = ModalBottomSheetState(ModalBottomSheetValue.HalfExpanded),
        p2PUiState = P2PUiState(),
        deviceName = "John",
        deviceRole = DeviceRole.SENDER,
        p2PState = P2PState.PAIR_DEVICES_FOUND
      )
    }

    composeRule
      .onNodeWithTag(CIRCULAR_PROGRESS_INDICATOR_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  @OptIn(ExperimentalMaterialApi::class)
  fun bottomSheetScreenRendersCorrectlyForReceiveBasicDeviceDetailsFailedStatus() {
    composeRule.setContent {
      BottomSheet(
        deviceList = emptyList(),
        onEvent = {},
        modalBottomSheetState = ModalBottomSheetState(ModalBottomSheetValue.HalfExpanded),
        p2PUiState = P2PUiState(),
        deviceName = "John",
        deviceRole = DeviceRole.RECEIVER,
        p2PState = P2PState.RECEIVE_BASIC_DEVICE_DETAILS_FAILED
      )
    }

    composeRule.onNodeWithText("OKay").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Receiving device details failed").assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithText("Sorry could not receive device details")
      .assertExists()
      .assertIsDisplayed()
    composeRule
      .onNodeWithTag(BOTTOM_SHEET_BUTTON_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
    composeRule
      .onNodeWithTag(BOTTOM_SHEET_CANCEL_ICON_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
  }

  @Test
  @OptIn(ExperimentalMaterialApi::class)
  fun bottomSheetScreenRendersCorrectlyForPairDevicesSearchFailedStatus() {
    composeRule.setContent {
      BottomSheet(
        deviceList = emptyList(),
        onEvent = {},
        modalBottomSheetState = ModalBottomSheetState(ModalBottomSheetValue.HalfExpanded),
        p2PUiState = P2PUiState(),
        deviceName = "John",
        deviceRole = DeviceRole.SENDER,
        p2PState = P2PState.PAIR_DEVICES_SEARCH_FAILED
      )
    }

    composeRule.onNodeWithText("OKay").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Searching failed").assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithText("Sorry could not find devices to pair with")
      .assertExists()
      .assertIsDisplayed()
    composeRule
      .onNodeWithTag(BOTTOM_SHEET_BUTTON_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
    composeRule
      .onNodeWithTag(BOTTOM_SHEET_CANCEL_ICON_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
  }

  @Test
  @OptIn(ExperimentalMaterialApi::class)
  fun bottomSheetScreenRendersCorrectlyForConnectToDeviceFailedStatus() {
    composeRule.setContent {
      BottomSheet(
        deviceList = emptyList(),
        onEvent = {},
        modalBottomSheetState = ModalBottomSheetState(ModalBottomSheetValue.HalfExpanded),
        p2PUiState = P2PUiState(),
        deviceName = "John",
        deviceRole = DeviceRole.SENDER,
        p2PState = P2PState.CONNECT_TO_DEVICE_FAILED
      )
    }

    composeRule.onNodeWithText("OKay").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Pairing failed").assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithText("Sorry could not pair with device")
      .assertExists()
      .assertIsDisplayed()
    composeRule
      .onNodeWithTag(BOTTOM_SHEET_BUTTON_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
    composeRule
      .onNodeWithTag(BOTTOM_SHEET_CANCEL_ICON_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
  }

  private fun populateDeviceInfo(): DeviceInfo {
    val wifiP2pDevice =
      WifiP2pDevice().apply {
        deviceName = "Google Pixel 7 android 12"
        deviceAddress = "00:00:5e:00:53:af"
      }
    return WifiDirectDataSharingStrategy.WifiDirectDevice(wifiP2pDevice)
  }
}
