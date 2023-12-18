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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.smartregister.p2p.R
import org.smartregister.p2p.search.ui.p2p.P2PEvent
import org.smartregister.p2p.search.ui.theme.DefaultColor
import org.smartregister.p2p.utils.annotation.ExcludeFromJacocoGeneratedReport

const val P2P_CONFIRM_BUTTON_TAG = "confirmButtonTestTag"
const val P2P_CANCEL_BUTTON_TAG = "cancelButtonTestTag"
const val P2P_DIALOG_MSG_TAG = "p2pDialogProgressMsgTag"

@Composable
fun P2PDialog(
  modifier: Modifier = Modifier,
  dialogTitle: String = stringResource(id = R.string.connection_break_title),
  dialogMessage: String = stringResource(id = R.string.connection_break_msg),
  onEvent: (P2PEvent) -> Unit
) {
  val openDialog = remember { mutableStateOf(true) }
  if (openDialog.value) {
    Dialog(
      onDismissRequest = { openDialog.value = true },
      properties = DialogProperties(dismissOnBackPress = true)
    ) {
      Box(Modifier.size(480.dp, 240.dp)) {
        Column(
          modifier = modifier.padding(8.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Surface(color = Color.White, modifier = modifier.fillMaxSize()) {
            Column(
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                fontSize = 16.sp,
                color = Color.Black,
                text = dialogTitle,
                modifier = modifier.testTag(P2P_DIALOG_MSG_TAG).padding(vertical = 16.dp),
              )

              Text(
                fontSize = 14.sp,
                color = DefaultColor,
                text = dialogMessage,
                modifier = modifier.testTag(P2P_DIALOG_MSG_TAG).padding(vertical = 16.dp),
              )

              ButtonRow(onEvent = onEvent)
            }
          }
        }
      }
    }
  }
}

@Composable
fun ButtonRow(
  confirmText: String = stringResource(id = R.string.yes_exit),
  cancelText: String = stringResource(id = R.string.cancel),
  onEvent: (P2PEvent) -> Unit
) {
  Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.Center) {
    Button(
      onClick = { onEvent(P2PEvent.DismissConnectionBreakDialog) },
      modifier = Modifier.padding(end = 5.dp).testTag(P2P_CANCEL_BUTTON_TAG),
      shape = RoundedCornerShape(16.dp)
    ) { Text(text = cancelText, fontSize = 20.sp) }
    Button(
      onClick = { onEvent(P2PEvent.ConnectionBreakConfirmed) },
      modifier = Modifier.testTag(P2P_CONFIRM_BUTTON_TAG),
      shape = RoundedCornerShape(16.dp)
    ) {
      Text(
        text = confirmText,
        fontSize = 20.sp,
      )
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
private fun P2PDialogPreview() {
  P2PDialog(onEvent = {})
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
private fun PreviewButtonRow() {
  ButtonRow(onEvent = {})
}
