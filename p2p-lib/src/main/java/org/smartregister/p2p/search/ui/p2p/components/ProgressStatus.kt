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
