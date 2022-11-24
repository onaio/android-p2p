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
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class DialogViewTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun testButtonRowRendersCorrectly() {
    composeRule.setContent { ButtonRow(confirmText = "Yes", cancelText = "Cancel", onEvent = {}) }
    composeRule.onNodeWithText("Yes").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Cancel").assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithTag(P2P_CONFIRM_BUTTON_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
    composeRule
      .onNodeWithTag(P2P_CANCEL_BUTTON_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
  }

  @Test
  fun testP2PDialog() {
    composeRule.setContent {
      P2PDialog(
        dialogTitle = "Connection Break",
        dialogMessage = "Device might get unpaired if you exit",
        onEvent = {}
      )
    }
    composeRule.onNodeWithText("Connection Break").assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithText("Device might get unpaired if you exit")
      .assertExists()
      .assertIsDisplayed()
  }
}
