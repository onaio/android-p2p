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

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.smartregister.p2p.search.ui.p2p.components.CIRCULAR_PROGRESS_INDICATOR_TEST_TAG
import org.smartregister.p2p.search.ui.p2p.components.PROGRESS_INDICATOR_ICON_TEST_TAG

class P2PScreenTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun transferProgressScreenRendersCorrectly() {
    composeRule.setContent {
      TransferProgressScreen(
        title = "Waiting to receive data",
        message = "Waiting for sender to initiate data sync",
        showCancelButton = true,
        onEvent = {},
        p2PUiState = P2PUiState()
      )
    }
    // ProgressStatusIndicator is rendered
    composeRule.onNodeWithTag(PROGRESS_INDICATOR_ICON_TEST_TAG).assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithTag(CIRCULAR_PROGRESS_INDICATOR_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()

    // ProgressStatusText is rendered
    composeRule.onNodeWithText("Waiting to receive data").assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithText("Waiting for sender to initiate data sync")
      .assertExists()
      .assertIsDisplayed()

    // cancel button is rendered
    composeRule
      .onNodeWithTag(CANCEL_BUTTON_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
    composeRule.onNodeWithText("Cancel").assertExists().assertIsDisplayed()
  }

  @OptIn(ExperimentalMaterialApi::class)
  @Test
  fun defaultScreenIsRenderedCorrectly() {
    composeRule.setContent {
      val modalBottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
      DefaultScreen(
        onEvent = {},
        modalBottomSheetState = modalBottomSheetState,
        updateDeviceRole = {},
        p2PUiState = P2PUiState()
      )
    }

    composeRule.onNodeWithTag(P2P_SYNC_IMAGE_TEST_TAG).assertExists().assertIsDisplayed()

    // Actionable send  button renders correctly
    composeRule.onNodeWithText("Send Data").assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithText("Tap to start sending data to nearby device")
      .assertExists()
      .assertIsDisplayed()

    // Actionable receive  button renders correctly
    composeRule.onNodeWithText("Receive Data").assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithText("Tap to start receiving data from nearby device")
      .assertExists()
      .assertIsDisplayed()
  }
}
