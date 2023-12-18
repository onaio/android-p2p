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
