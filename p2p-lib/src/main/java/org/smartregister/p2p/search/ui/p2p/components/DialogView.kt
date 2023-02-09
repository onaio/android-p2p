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
