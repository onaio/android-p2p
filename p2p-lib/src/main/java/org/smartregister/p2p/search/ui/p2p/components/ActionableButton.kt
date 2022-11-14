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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.p2p.R
import org.smartregister.p2p.model.ActionableButtonData
import org.smartregister.p2p.search.ui.theme.DefaultColor

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
    modifier = modifier.fillMaxWidth().padding(top = 0.dp, start = 16.dp, end = 16.dp)
  ) {
    Row(modifier = modifier) {
      Column(modifier = modifier.padding(end = 10.dp), verticalArrangement = Arrangement.Center) {
        Icon(
          imageVector = Icons.Filled.SendToMobile,
          contentDescription = null,
          tint = colorResource(id = R.color.icon_blue_color),
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
