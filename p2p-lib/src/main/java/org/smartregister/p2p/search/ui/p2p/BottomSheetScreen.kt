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
package org.smartregister.p2p.search.ui.p2p

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.smartregister.p2p.R
import org.smartregister.p2p.authentication.model.DeviceRole
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.search.ui.p2p.components.PairDeviceRow
import org.smartregister.p2p.search.ui.p2p.components.ProgressStatusIndicator
import org.smartregister.p2p.search.ui.p2p.components.ProgressStatusText
import org.smartregister.p2p.search.ui.theme.DefaultColor

const val P2P_BOTTOM_SHEET_LIST = "p2PBottomSheetList"

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetScreen(
  modifier: Modifier = Modifier,
  p2PUiState: P2PUiState,
  deviceRole: DeviceRole,
  p2PViewModel: P2PViewModel,
  onEvent: (P2PEvent) -> Unit,
  modalBottomSheetState: ModalBottomSheetState
) {
  val coroutineScope = rememberCoroutineScope()
  val deviceList by p2PViewModel.deviceList.observeAsState(initial = listOf())
  val p2PState by p2PViewModel.p2PState.observeAsState(initial = P2PState.SEARCHING_FOR_RECIPIENT)

  Scaffold(
    modifier.wrapContentHeight(Alignment.CenterVertically).background(DefaultColor.copy(0.8f))
  ) {
    var bottomSheetTitle = ""
    var progressStatusTitle: String? = null
    var progressStatusMsg: String? = null
    var showCircularProgressIndicator: Boolean = true
    var transferCompleteMsg = ""
    var deviceName = p2PViewModel.getCurrentConnectedDevice()?.name() ?: ""
    when (deviceRole) {
      DeviceRole.SENDER -> {
        bottomSheetTitle = stringResource(id = R.string.searching_for_nearby_recipient)
        progressStatusMsg = stringResource(id = R.string.searching_nearby_device_as)
        transferCompleteMsg =
          stringResource(
            id = R.string.x_records_sent,
            p2PUiState.transferProgress.totalRecordCount,
            deviceName
          )
      }
      DeviceRole.RECEIVER -> {
        bottomSheetTitle = stringResource(id = R.string.waiting_to_pair)
        progressStatusMsg = stringResource(id = R.string.waiting_to_pair_with_sender)
        transferCompleteMsg =
          stringResource(
            id = R.string.x_records_received,
            p2PUiState.transferProgress.totalRecordCount,
            deviceName
          )
      }
    }

    when (p2PState) {
      P2PState.TRANSFER_COMPLETE -> {
        bottomSheetTitle = stringResource(id = R.string.send_data)
        progressStatusTitle = stringResource(id = R.string.device_data_successfully_sent)
        progressStatusMsg = transferCompleteMsg
        showCircularProgressIndicator = false
      }
      else -> {}
    }

    Column(
      modifier = modifier.wrapContentSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp, vertical = 16.dp)
      ) {
        Text(
          text = bottomSheetTitle,
          textAlign = TextAlign.Start,
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp,
        )
        Icon(
          imageVector = Icons.Filled.Clear,
          contentDescription = null,
          tint = DefaultColor.copy(0.8f),
          modifier =
            modifier.clickable {
              coroutineScope.launch {
                if (modalBottomSheetState.isVisible) modalBottomSheetState.hide()
              }
            }
        )
      }

      Spacer(modifier = Modifier.size(5.dp))
      ProgressStatusIndicator(
        showCircularProgressIndicator = showCircularProgressIndicator,
        p2PUiState = p2PUiState
      )

      Spacer(modifier = Modifier.size(5.dp))
      ProgressStatusText(title = progressStatusTitle, message = progressStatusMsg)

      Spacer(modifier = Modifier.size(5.dp))
      if (deviceRole == DeviceRole.SENDER) {
        LazyColumn(
          contentPadding = PaddingValues(vertical = 8.dp),
          modifier = Modifier.fillMaxWidth().testTag(P2P_BOTTOM_SHEET_LIST)
        ) {
          itemsIndexed(deviceList) { index, item ->
            PairDeviceRow(device = item, onEvent = onEvent)
          }
        }
      }

      Spacer(modifier = Modifier.size(5.dp))

      if (p2PState == P2PState.TRANSFER_COMPLETE) {
        Button(
          onClick = { onEvent(P2PEvent.DataTransferCompleteConfirmed) },
          modifier.padding(start = 10.dp, end = 10.dp).fillMaxWidth()
        ) { Text(text = stringResource(id = R.string.okay)) }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomSheetScreen() {
  // BottomSheetScreen(deviceRole = DeviceRole.SENDER)
}
