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
package org.smartregister.p2p.search.ui.p2p.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.smartregister.p2p.model.ProgressIndicator
import org.smartregister.p2p.model.ProgressIndicatorState
import org.smartregister.p2p.search.ui.p2p.P2PUiState
import org.smartregister.p2p.search.ui.theme.DefaultColor

class DeviceListTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testSelectPairDeviceRowIsRendered() {
    val p2PUiState =
      P2PUiState(
        progressIndicator =
          ProgressIndicator(
            progressIndicatorState = ProgressIndicatorState.EMPTY,
            backgroundColor = DefaultColor
          )
      )

    composeTestRule.setContent { SelectPairDeviceRow(p2PUiState = p2PUiState) }
    composeTestRule.onNodeWithTag(SELECT_PAIR_DEVICE_TEXT_TAG).assertExists()
  }

  @Test
  fun testPairDeviceRowIsRendered() {
    composeTestRule.setContent { PairDeviceRow(device = null, onEvent = {}) }
    composeTestRule.onNodeWithTag(PAIR_DEVICE_ROW_ICON_TAG).assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithTag(PAIR_DEVICE_ROW_NAME_TEXT_TAG).assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithTag(PAIR_DEVICE_ROW_BUTTON_TAG).assertExists().assertIsDisplayed()
  }
}
