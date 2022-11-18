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
package org.smartregister.p2p.search.ui.p2p.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.smartregister.p2p.model.ProgressIndicator
import org.smartregister.p2p.model.ProgressIndicatorState
import org.smartregister.p2p.search.ui.p2p.P2PUiState

class ProgressStatusTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun testProgressStatusTextRendersCorrectly() {
    composeRule.setContent {
      ProgressStatusText(title = "Receiving data", message = "Transfer in progress")
    }
    composeRule.onNodeWithText("Receiving data").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Transfer in progress").assertExists().assertIsDisplayed()
  }

  @Test
  fun testProgressStatusIndicatorWithIconRendersCorrectly() {
    composeRule.setContent {
      ProgressStatusIndicator(
        p2PUiState =
          P2PUiState(
            progressIndicator =
              ProgressIndicator(progressIndicatorState = ProgressIndicatorState.SHOW_ICON)
          )
      )
    }
    composeRule.onNodeWithTag(PROGRESS_INDICATOR_ICON_TEST_TAG).assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithTag(CIRCULAR_PROGRESS_INDICATOR_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
    composeRule.onNodeWithTag(PERCENTAGE_TEXT_TEST_TAG).assertDoesNotExist()
  }

  @Test
  fun testProgressStatusIndicatorWithPercentageTextRendersCorrectly() {
    composeRule.setContent {
      ProgressStatusIndicator(
        p2PUiState =
          P2PUiState(
            progressIndicator =
              ProgressIndicator(progressIndicatorState = ProgressIndicatorState.SHOW_PERCENTAGE)
          )
      )
    }
    composeRule.onNodeWithTag(PERCENTAGE_TEXT_TEST_TAG).assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithTag(CIRCULAR_PROGRESS_INDICATOR_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
    composeRule.onNodeWithTag(PROGRESS_INDICATOR_ICON_TEST_TAG).assertDoesNotExist()
  }

  @Test
  fun testProgressStatusIndicatorWithoutPercentageTextOrIconRendersCorrectly() {
    composeRule.setContent {
      ProgressStatusIndicator(
        p2PUiState =
          P2PUiState(
            progressIndicator =
              ProgressIndicator(progressIndicatorState = ProgressIndicatorState.EMPTY)
          )
      )
    }
    composeRule.onNodeWithTag(PERCENTAGE_TEXT_TEST_TAG).assertDoesNotExist()
    composeRule
      .onNodeWithTag(CIRCULAR_PROGRESS_INDICATOR_TEST_TAG)
      .assertExists()
      .assertIsDisplayed()
    composeRule.onNodeWithTag(PROGRESS_INDICATOR_ICON_TEST_TAG).assertDoesNotExist()
  }
}
