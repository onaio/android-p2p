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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SendToMobile
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.p2p.R
import org.smartregister.p2p.model.ActionableButtonData
import org.smartregister.p2p.search.ui.theme.DefaultColor
import org.smartregister.p2p.utils.annotation.ExcludeFromJacocoGeneratedReport

const val ACTION_BUTTON_ICON_TEST_TAG = "actionButtonIconTestTag"
const val ACTIONABLE_BUTTON_TAG = "actionButtonTestTag"

@Composable
fun ActionableButton(
  actionableButtonData: ActionableButtonData,
  modifier: Modifier = Modifier,
  onAction: (String, String?) -> Unit
) {
  OutlinedButton(
    onClick = { onAction("send", "test") },
    colors =
      ButtonDefaults.buttonColors(
        backgroundColor = actionableButtonData.contentColor.copy(alpha = 0.1f)
      ),
    modifier =
      modifier
        .fillMaxWidth()
        .padding(top = 0.dp, start = 16.dp, end = 16.dp)
        .testTag(ACTIONABLE_BUTTON_TAG)
  ) {
    Row(modifier = modifier) {
      Column(modifier = modifier.padding(end = 10.dp), verticalArrangement = Arrangement.Center) {
        Icon(
          imageVector = Icons.Filled.SendToMobile,
          contentDescription = null,
          tint = colorResource(id = R.color.icon_blue_color),
          modifier = modifier.testTag(ACTION_BUTTON_ICON_TEST_TAG)
        )
      }
      Column() {
        Text(text = actionableButtonData.title, color = colorResource(id = R.color.icon_blue_color))
        Text(text = actionableButtonData.description, color = DefaultColor)
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewActionableButton() {
  Column {
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
