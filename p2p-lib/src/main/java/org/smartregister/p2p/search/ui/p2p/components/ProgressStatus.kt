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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.p2p.model.ProgressIndicator
import org.smartregister.p2p.model.ProgressIndicatorState
import org.smartregister.p2p.search.ui.p2p.P2PUiState
import org.smartregister.p2p.search.ui.theme.DefaultColor
import org.smartregister.p2p.utils.annotation.ExcludeFromJacocoGeneratedReport

const val CIRCULAR_PROGRESS_INDICATOR_TEST_TAG = "circularProgressIndicatorTestTag"
const val PROGRESS_INDICATOR_ICON_TEST_TAG = "ProgressIndicatorIconTestTag"
const val PERCENTAGE_TEXT_TEST_TAG = "percentageTextTestTag"

@Composable
fun ProgressStatusIndicator(
  modifier: Modifier = Modifier,
  showCircularProgressIndicator: Boolean = true,
  p2PUiState: P2PUiState
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .wrapContentSize()
        .background(p2PUiState.progressIndicator.backgroundColor, shape = CircleShape),
  ) {
    when (p2PUiState.progressIndicator.progressIndicatorState) {
      ProgressIndicatorState.SHOW_PERCENTAGE -> {
        Text(
          text = "${p2PUiState.transferProgress.percentageTransferred}%",
          modifier = modifier.wrapContentWidth(Alignment.Start).testTag(PERCENTAGE_TEXT_TEST_TAG)
        )
      }
      ProgressIndicatorState.SHOW_ICON -> {
        Icon(
          imageVector = p2PUiState.progressIndicator.icon,
          contentDescription = null,
          tint = DefaultColor.copy(0.8f),
          modifier = modifier.testTag(PROGRESS_INDICATOR_ICON_TEST_TAG)
        )
      }
      ProgressIndicatorState.EMPTY -> {}
    }

    if (showCircularProgressIndicator) {
      CircularProgressIndicator(
        modifier = modifier.size(40.dp).testTag(CIRCULAR_PROGRESS_INDICATOR_TEST_TAG),
        strokeWidth = 2.dp
      )
    }
  }
}

@Composable
fun ProgressStatusText(modifier: Modifier = Modifier, title: String?, message: String?) {
  Column(modifier = modifier.wrapContentWidth(Alignment.CenterHorizontally)) {
    if (!title.isNullOrBlank()) {
      Text(text = title!!, fontWeight = FontWeight.Bold)
    }

    if (!message.isNullOrBlank()) {
      Text(
        text = message,
        color = DefaultColor,
        modifier = modifier.wrapContentWidth(Alignment.Start)
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun PreviewProgressStatusIndicatorWithIcon() {
  ProgressStatusIndicator(
    p2PUiState =
      P2PUiState(
        progressIndicator =
          ProgressIndicator(progressIndicatorState = ProgressIndicatorState.SHOW_ICON)
      )
  )
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun PreviewProgressStatusIndicatorWithoutIcon() {
  ProgressStatusIndicator(
    p2PUiState =
      P2PUiState(
        progressIndicator = ProgressIndicator(progressIndicatorState = ProgressIndicatorState.EMPTY)
      )
  )
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun PreviewProgressStatusText() {
  ProgressStatusText(title = "sample title", message = "sample message")
}
