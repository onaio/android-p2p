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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.p2p.R
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.model.ProgressIndicator
import org.smartregister.p2p.model.ProgressIndicatorState
import org.smartregister.p2p.search.ui.p2p.P2PEvent
import org.smartregister.p2p.search.ui.p2p.P2PUiState
import org.smartregister.p2p.search.ui.theme.DefaultColor
import org.smartregister.p2p.utils.annotation.ExcludeFromJacocoGeneratedReport

const val SELECT_PAIR_DEVICE_TEXT_TAG = "selectPairDeviceTextTestTag"
const val PAIR_DEVICE_ROW_ICON_TAG = "pairDeviceRowIconTestTag"
const val PAIR_DEVICE_ROW_NAME_TEXT_TAG = "pairDeviceRowNameTextTestTag"
const val PAIR_DEVICE_ROW_BUTTON_TAG = "pairDeviceRowButtonTestTag"
var pairingInitiated: Boolean = false

@Composable
fun SelectPairDeviceRow(modifier: Modifier = Modifier, p2PUiState: P2PUiState) {
  Column(
    modifier =
      modifier.wrapContentWidth(Alignment.Start).background(DefaultColor.copy(alpha = 0.2f))
  ) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
      Text(
        text = stringResource(id = R.string.select_recipient_device),
        modifier.testTag(SELECT_PAIR_DEVICE_TEXT_TAG)
      )
      ProgressStatusIndicator(p2PUiState = p2PUiState)
    }
  }
}

@Composable
fun PairDeviceRow(
  modifier: Modifier = Modifier,
  device: DeviceInfo?,
  onEvent: (P2PEvent) -> Unit,
  p2PState: P2PState
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth().padding(all = 16.dp)
  ) {
    Icon(
      imageVector = Icons.Filled.Phone,
      contentDescription = null,
      tint = DefaultColor.copy(0.8f),
      modifier = modifier.testTag(PAIR_DEVICE_ROW_ICON_TAG)
    )
    Column(modifier = modifier.wrapContentWidth(Alignment.Start)) {
      Text(
        text = "${device?.name()} ".plus(stringResource(id = R.string.phone)),
        modifier = modifier.testTag(PAIR_DEVICE_ROW_NAME_TEXT_TAG).width(120.dp),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
      )

      var pairText: String = stringResource(id = R.string.pair)
      when (p2PState) {
        P2PState.WIFI_AND_LOCATION_ENABLE -> {
          pairText = stringResource(id = R.string.pair)
        }
        else -> {
          if (pairingInitiated) {
            pairText = stringResource(id = R.string.pairing)
          }
        }
      }

      Text(
        text = pairText,
        color = DefaultColor,
        modifier = modifier.wrapContentWidth(Alignment.Start)
      )
    }
    Button(
      onClick = {
        device?.let { P2PEvent.PairWithDevice(it) }?.let { onEvent(it) }
        pairingInitiated = true
      },
      modifier = modifier.testTag(PAIR_DEVICE_ROW_BUTTON_TAG)
    ) { Text(text = stringResource(id = R.string.pair)) }
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun PreviewSelectPairDeviceRow() {
  SelectPairDeviceRow(
    p2PUiState =
      P2PUiState(
        progressIndicator =
          ProgressIndicator(
            progressIndicatorState = ProgressIndicatorState.EMPTY,
            backgroundColor = DefaultColor
          )
      )
  )
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun PreviewPairDeviceRow() {
  PairDeviceRow(onEvent = {}, device = null, p2PState = P2PState.PAIR_DEVICES_FOUND)
}
