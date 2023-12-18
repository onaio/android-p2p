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
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
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
import org.smartregister.p2p.model.ProgressIndicator
import org.smartregister.p2p.model.ProgressIndicatorState
import org.smartregister.p2p.search.ui.p2p.components.ActionableButton
import org.smartregister.p2p.search.ui.p2p.components.P2PDialog
import org.smartregister.p2p.search.ui.p2p.components.ProgressStatusIndicator
import org.smartregister.p2p.search.ui.p2p.components.ProgressStatusText
import org.smartregister.p2p.search.ui.theme.DefaultColor
import org.smartregister.p2p.utils.annotation.ExcludeFromJacocoGeneratedReport
import timber.log.Timber

const val P2P_SCREEN_TOP_BAR_TEST_TAG = "p2pScreenTopBarTestTag"
const val P2P_SCREEN_TOP_BAR_ICON_TEST_TAG = "p2pScreenTopBarIconTestTag"
const val P2P_SYNC_IMAGE_TEST_TAG = "p2pSyncImageTestTag"
const val CANCEL_BUTTON_TEST_TAG = "cancelButtonTestTag"

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
  val p2PState by p2PViewModel.p2PState.observeAsState(initial = P2PState.INITIATE_DATA_TRANSFER)
  // bottom sheet updated
  var deviceRole: DeviceRole by remember { mutableStateOf(DeviceRole.SENDER) }

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
          backgroundColor = colorResource(id = R.color.colorPrimaryLight),
          contentColor = colorResource(id = R.color.white),
          navigationIcon = {
            IconButton(onClick = { p2PViewModel.closeP2PScreen() }) {
              Icon(
                Icons.Filled.ArrowBack,
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
        P2PState.WIFI_AND_LOCATION_ENABLE -> {
          DefaultScreen(
            onEvent = onEvent,
            modalBottomSheetState = modalBottomSheetState,
            updateDeviceRole = {
              deviceRole = it
              p2PViewModel.deviceRole = it
            },
            p2PUiState = p2PUiState
          )
          coroutineScope.launch { modalBottomSheetState.show() }
        }
        P2PState.TRANSFERRING_DATA -> {
          coroutineScope.launch { modalBottomSheetState.hide() }
          TransferProgressScreen(
            title = null,
            message =
              stringResource(
                id = R.string.transferring_x_records,
                p2PUiState.transferProgress.transferredRecordCount
              ),
            showCancelButton = true,
            onEvent = onEvent,
            p2PUiState =
              p2PUiState.copy(
                progressIndicator =
                  ProgressIndicator(
                    backgroundColor = DefaultColor.copy(alpha = 0.2f),
                    icon = Icons.Filled.Upload,
                    progressIndicatorState = ProgressIndicatorState.SHOW_PERCENTAGE
                  )
              )
          )
        }
        P2PState.PREPARING_TO_SEND_DATA -> {
          coroutineScope.launch { modalBottomSheetState.hide() }
          var deviceName = p2PViewModel.getCurrentConnectedDevice()?.name() ?: ""
          TransferProgressScreen(
            title = null,
            message = stringResource(id = R.string.preparing_to_send_data_to, deviceName),
            showCancelButton = false,
            onEvent = onEvent,
            p2PUiState =
              p2PUiState.copy(
                progressIndicator =
                  ProgressIndicator(
                    backgroundColor = DefaultColor.copy(alpha = 0.2f),
                    icon = Icons.Filled.Upload
                  )
              )
          )
        }
        P2PState.TRANSFER_COMPLETE -> {
          coroutineScope.launch { modalBottomSheetState.show() }
        }
        P2PState.WAITING_TO_RECEIVE_DATA -> {
          coroutineScope.launch { modalBottomSheetState.hide() }
          TransferProgressScreen(
            title = stringResource(id = R.string.waiting_to_receive_data),
            message = stringResource(id = R.string.waiting_for_sender_to_initiate_sync),
            showCancelButton = false,
            onEvent = onEvent,
            p2PUiState =
              p2PUiState.copy(
                progressIndicator =
                  ProgressIndicator(
                    backgroundColor = DefaultColor.copy(alpha = 0.2f),
                    icon = Icons.Filled.Download
                  )
              )
          )
        }
        P2PState.RECEIVE_BASIC_DEVICE_DETAILS_FAILED -> {
          coroutineScope.launch { modalBottomSheetState.show() }
        }
        P2PState.RECEIVING_DATA -> {
          coroutineScope.launch { modalBottomSheetState.hide() }
          TransferProgressScreen(
            title = null,
            message =
              stringResource(
                id = R.string.receiving_x_records,
                p2PUiState.transferProgress.transferredRecordCount
              ),
            showCancelButton = true,
            onEvent = onEvent,
            p2PUiState =
              p2PUiState.copy(
                progressIndicator =
                  ProgressIndicator(
                    backgroundColor = DefaultColor.copy(alpha = 0.2f),
                    icon = Icons.Filled.Upload,
                    progressIndicatorState = ProgressIndicatorState.SHOW_PERCENTAGE
                  )
              )
          )
        }
        P2PState.TRANSFER_CANCELLED -> {
          if (modalBottomSheetState.isVisible) {
            coroutineScope.launch { modalBottomSheetState.hide() }
          }
          p2PViewModel.updateP2PState(P2PState.INITIATE_DATA_TRANSFER)
        }
        P2PState.INITIATE_DATA_TRANSFER -> {
          DefaultScreen(
            onEvent = onEvent,
            modalBottomSheetState = modalBottomSheetState,
            updateDeviceRole = {
              deviceRole = it
              p2PViewModel.deviceRole = it
            },
            p2PUiState = p2PUiState
          )
        }
        P2PState.PROMPT_NEXT_TRANSFER -> {
          if (modalBottomSheetState.isVisible) {
            coroutineScope.launch { modalBottomSheetState.hide() }
          }
          p2PViewModel.updateP2PState(P2PState.INITIATE_DATA_TRANSFER)
        }
        P2PState.DEVICE_DISCONNECTED, P2PState.CONNECT_TO_DEVICE_FAILED -> {
          coroutineScope.launch { modalBottomSheetState.show() }
        }
        else -> {
          Timber.e("Unhandled p2p state ${p2PState.name} inside P2PScreen")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DefaultScreen(
  modifier: Modifier = Modifier,
  onEvent: (P2PEvent) -> Unit,
  modalBottomSheetState: ModalBottomSheetState,
  updateDeviceRole: (DeviceRole) -> Unit,
  p2PUiState: P2PUiState
) {
  val coroutineScope = rememberCoroutineScope()
  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Column(modifier = modifier.padding(4.dp), verticalArrangement = Arrangement.Center) {
      Image(
        painter = painterResource(R.drawable.ic_default_p2p),
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
          updateDeviceRole(DeviceRole.SENDER)
          onEvent(P2PEvent.StartScanning)
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
          updateDeviceRole(DeviceRole.RECEIVER)
          onEvent(P2PEvent.StartScanning)
        }
      )
      Spacer(modifier = modifier.height(20.dp))
    }
  }
}

@Composable
fun TransferProgressScreen(
  modifier: Modifier = Modifier,
  title: String?,
  message: String?,
  showCancelButton: Boolean = false,
  onEvent: (P2PEvent) -> Unit,
  p2PUiState: P2PUiState
) {
  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Spacer(modifier = Modifier.size(20.dp))
    ProgressStatusIndicator(p2PUiState = p2PUiState)
    Spacer(modifier = Modifier.size(20.dp))
    ProgressStatusText(title = title, message = message)
    Spacer(modifier = Modifier.size(20.dp))
    if (showCancelButton) {
      Button(
        onClick = { onEvent(P2PEvent.CancelDataTransfer) },
        modifier = modifier.testTag(CANCEL_BUTTON_TEST_TAG)
      ) { Text(text = stringResource(id = R.string.cancel)) }
    }
    if (p2PUiState.showP2PDialog) {
      P2PDialog(onEvent = onEvent)
    }
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun PreviewTransferProgressScreen() {
  TransferProgressScreen(
    title = "Waiting to receive data",
    message = "Waiting for sender to initiate data sync",
    showCancelButton = true,
    onEvent = {},
    p2PUiState = P2PUiState()
  )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun PreviewDefaultScreen() {
  val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.HalfExpanded)
  DefaultScreen(
    onEvent = {},
    modalBottomSheetState = modalBottomSheetState,
    updateDeviceRole = {},
    p2PUiState = P2PUiState()
  )
}
