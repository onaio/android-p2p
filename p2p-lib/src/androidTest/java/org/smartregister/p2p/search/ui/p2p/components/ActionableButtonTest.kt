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

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.p2p.R
import org.smartregister.p2p.model.ActionableButtonData

class ActionableButtonTest {

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setUp() {
    composeRule.setContent {
      ActionableButton(
        actionableButtonData =
          ActionableButtonData(
            title = stringResource(id = R.string.send_data),
            description = stringResource(id = R.string.tap_to_send_data_msg)
          ),
        onAction = { _, _ -> }
      )
    }
  }

  @Test
  fun testActionableButtonRendersCorrectly() {
    composeRule.onNodeWithText("Send Data").assertExists().assertIsDisplayed()
    composeRule
      .onNodeWithText("Tap to start sending data to nearby device")
      .assertExists()
      .assertIsDisplayed()
    composeRule
      .onNodeWithTag(ACTIONABLE_BUTTON_TAG)
      .assertExists()
      .assertIsDisplayed()
      .performClick()
  }
}
