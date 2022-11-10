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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.smartregister.p2p.R
import org.smartregister.p2p.authentication.model.DeviceRole
import org.smartregister.p2p.model.ActionableButtonData
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.search.ui.p2p.components.ActionableButton
import org.smartregister.p2p.search.ui.p2p.components.ProgressStatusIndicator
import org.smartregister.p2p.search.ui.p2p.components.ProgressStatusText

const val P2P_SCREEN_TOP_BAR_TEST_TAG = "p2pScreenTopBarTestTag"
const val P2P_SCREEN_TOP_BAR_ICON_TEST_TAG = "p2pScreenTopBarIconTestTag"
const val P2P_SYNC_IMAGE_TEST_TAG = "p2pSyncImageTestTag"

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "CoroutineCreationDuringComposition")
@Composable
fun P2PScreen(
  modifier: Modifier = Modifier,
  p2PViewModel: P2PViewModel,
  p2PUiState: P2PUiState,
  onEvent: (P2PEvent) -> Unit
) {

  val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
  val coroutineScope = rememberCoroutineScope()
  val p2PState by p2PViewModel.p2PState.observeAsState(initial = P2PState.SEARCHING_FOR_RECIPIENT)
  // bottom sheet updated
  var deviceRole by remember { mutableStateOf(DeviceRole.SENDER) }

  ModalBottomSheetLayout(
    sheetContent = {
      BottomSheetScreen(
        p2PUiState = p2PUiState,
        deviceRole = deviceRole,
        p2PViewModel = p2PViewModel,
        onEvent = onEvent,
        modalBottomSheetState = modalBottomSheetState
      )
    },
    sheetState = modalBottomSheetState,
    sheetShape = MaterialTheme.shapes.large
  ) {
    Scaffold(
      topBar = {
        TopAppBar(
          modifier = modifier.testTag(P2P_SCREEN_TOP_BAR_TEST_TAG),
          title = { Text(stringResource(id = R.string.transfer_data)) },
          backgroundColor = colorResource(id = R.color.top_appbar_bg),
          contentColor = colorResource(id = R.color.white),
          navigationIcon = {
            IconButton(onClick = {}) {
              Icon(
                Icons.Filled.Menu,
                null,
                modifier = modifier.testTag(P2P_SCREEN_TOP_BAR_ICON_TEST_TAG)
              )
            }
          },
          elevation = 0.dp
        )
      }
    ) {
      when (p2PState) {
        P2PState.TRANSFERRING_DATA -> {
          coroutineScope.launch { modalBottomSheetState.hide() }
          TransferProgressScreen(
            title = null,
            message = stringResource(id = R.string.transferring_x_records),
            showCancelButton = true,
            onEvent = onEvent
          )
        }
        P2PState.TRANSFER_COMPLETE -> {
          coroutineScope.launch { modalBottomSheetState.show() }
        }
        P2PState.RECEIVING_DATA -> {
          coroutineScope.launch { modalBottomSheetState.hide() }
          TransferProgressScreen(
            title = null,
            message = stringResource(id = R.string.receiving_x_records),
            showCancelButton = true,
            onEvent = onEvent
          )
        }
        P2PState.TRANSFER_CANCELLED -> {
          coroutineScope.launch { modalBottomSheetState.hide() }
        }
        else -> {
          Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Column(modifier = modifier.padding(4.dp), verticalArrangement = Arrangement.Center) {
              Image(
                painter = painterResource(R.drawable.ic_p2p),
                contentDescription = stringResource(id = R.string.device_to_device_sync_logo),
                modifier =
                  modifier
                    .align(Alignment.CenterHorizontally)
                    .requiredHeight(120.dp)
                    .requiredWidth(140.dp)
                    .testTag(P2P_SYNC_IMAGE_TEST_TAG),
              )

              Spacer(modifier = modifier.height(40.dp))
              ActionableButton(
                actionableButtonData =
                  ActionableButtonData(
                    title = stringResource(id = R.string.send_data),
                    description = stringResource(id = R.string.tap_to_send_data_msg)
                  ),
                onAction = { _, _ ->
                  deviceRole = DeviceRole.SENDER
                  onEvent(P2PEvent.StartScanning)
                  coroutineScope.launch {
                    modalBottomSheetState.animateTo(ModalBottomSheetValue.HalfExpanded)
                  }
                }
              )
              Spacer(modifier = modifier.height(20.dp))
              ActionableButton(
                actionableButtonData =
                  ActionableButtonData(
                    title = stringResource(id = R.string.receive_data),
                    description = stringResource(id = R.string.tap_to_receive_data_msg)
                  ),
                onAction = { _, _ ->
                  deviceRole = DeviceRole.RECEIVER
                  onEvent(P2PEvent.StartScanning)
                  coroutineScope.launch {
                    modalBottomSheetState.animateTo(ModalBottomSheetValue.HalfExpanded)
                  }
                }
              )
              Spacer(modifier = modifier.height(20.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
fun TransferProgressScreen(
  modifier: Modifier = Modifier,
  title: String?,
  message: String?,
  showCancelButton: Boolean = false,
  onEvent: (P2PEvent) -> Unit
) {
  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Spacer(modifier = Modifier.size(20.dp))
    ProgressStatusIndicator()
    Spacer(modifier = Modifier.size(20.dp))
    ProgressStatusText(title = title, message = message)
    Spacer(modifier = Modifier.size(20.dp))
    if (showCancelButton) {
      Button(onClick = { onEvent(P2PEvent.CancelDataTransfer) }) {
        Text(text = stringResource(id = R.string.cancel))
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTransferProgressScreen() {
  TransferProgressScreen(
    title = "Waiting to receive data",
    message = "Waiting for sender to initiate data sync",
    showCancelButton = true,
    onEvent = {}
  )
}
