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
import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.ModalBottomSheetValue
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
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.WifiDirectDataSharingStrategy
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.model.ProgressIndicator
import org.smartregister.p2p.model.ProgressIndicatorState
import org.smartregister.p2p.search.ui.p2p.components.PairDeviceRow
import org.smartregister.p2p.search.ui.p2p.components.ProgressStatusIndicator
import org.smartregister.p2p.search.ui.p2p.components.ProgressStatusText
import org.smartregister.p2p.search.ui.p2p.components.SelectPairDeviceRow
import org.smartregister.p2p.search.ui.theme.DangerColor
import org.smartregister.p2p.search.ui.theme.DefaultColor
import org.smartregister.p2p.search.ui.theme.WhiteColor
import org.smartregister.p2p.utils.annotation.ExcludeFromJacocoGeneratedReport
import timber.log.Timber

const val P2P_BOTTOM_SHEET_LIST = "p2PBottomSheetList"
const val BOTTOM_SHEET_CANCEL_ICON_TEST_TAG = "bottomSheetCancelIconTestTag"
const val BOTTOM_SHEET_BUTTON_TEST_TAG = "bottomSheetButtonTestTag"

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetScreen(
  p2PUiState: P2PUiState,
  deviceRole: DeviceRole,
  p2PViewModel: P2PViewModel,
  onEvent: (P2PEvent) -> Unit,
  modalBottomSheetState: ModalBottomSheetState
) {

  val deviceList by p2PViewModel.deviceList.observeAsState(initial = listOf())
  val p2PState by p2PViewModel.p2PState.observeAsState(initial = P2PState.SEARCHING_FOR_RECIPIENT)
  var deviceName = p2PViewModel.getCurrentConnectedDevice()?.name() ?: ""

  BottomSheet(
    deviceList = deviceList,
    onEvent = onEvent,
    modalBottomSheetState = modalBottomSheetState,
    p2PUiState = p2PUiState,
    deviceName = deviceName,
    deviceRole = deviceRole,
    p2PState = p2PState
  )
}

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun BottomSheet(
  modifier: Modifier = Modifier,
  deviceList: List<DeviceInfo>,
  onEvent: (P2PEvent) -> Unit,
  modalBottomSheetState: ModalBottomSheetState,
  p2PUiState: P2PUiState,
  deviceName: String,
  deviceRole: DeviceRole,
  p2PState: P2PState
) {
  val coroutineScope = rememberCoroutineScope()
  Scaffold(
    modifier.wrapContentHeight(Alignment.CenterVertically),
    backgroundColor = DefaultColor.copy(alpha = 0.2f)
  ) {
    var bottomSheetTitle = ""
    var progressStatusTitle: String? = null
    var progressStatusMsg: String? = null
    var showCircularProgressIndicator: Boolean = true
    var transferCompleteMsg = ""
    when (deviceRole) {
      DeviceRole.SENDER -> {
        bottomSheetTitle = stringResource(id = R.string.searching_for_nearby_recipient)

        when (p2PState) {
          P2PState.PAIR_DEVICES_SEARCH_FAILED -> {
            progressStatusTitle = stringResource(id = R.string.searching_failed_dialog_title)
            progressStatusMsg = stringResource(id = R.string.searching_failed_dialog_msg)
          }
          P2PState.CONNECT_TO_DEVICE_FAILED -> {
            progressStatusTitle = stringResource(id = R.string.pairing_failed)
            progressStatusMsg = stringResource(id = R.string.pairing_failed_msg)
          }
          P2PState.DATA_UP_TO_DATE -> {
            bottomSheetTitle = stringResource(id = R.string.send_data)
            progressStatusTitle = stringResource(id = R.string.sender_data_upto_date)
            progressStatusMsg = stringResource(id = R.string.sender_data_upto_date_msg)
          }
          P2PState.DEVICE_DISCONNECTED -> {
            bottomSheetTitle = stringResource(id = R.string.send_data)
            progressStatusTitle = stringResource(id = R.string.device_disconnected)
            progressStatusMsg = stringResource(id = R.string.device_disconnected_msg)
          }
          else -> {
            progressStatusMsg = stringResource(id = R.string.searching_nearby_device_as)
          }
        }

        transferCompleteMsg =
          stringResource(
            id = R.string.x_records_sent,
            p2PUiState.transferProgress.transferredRecordCount,
            deviceName
          )
      }
      DeviceRole.RECEIVER -> {
        when (p2PState) {
          P2PState.RECEIVE_BASIC_DEVICE_DETAILS_FAILED -> {
            bottomSheetTitle = stringResource(id = R.string.receiving)
            progressStatusTitle = stringResource(id = R.string.receive_device_details_failed)
            progressStatusMsg = stringResource(id = R.string.receive_device_details_failed_msg)
          }
          P2PState.DATA_UP_TO_DATE -> {
            bottomSheetTitle = stringResource(id = R.string.receive_data)
            progressStatusTitle = stringResource(id = R.string.receiver_data_upto_date)
            progressStatusMsg = stringResource(id = R.string.receiver_data_upto_date_msg)
          }
          P2PState.DEVICE_DISCONNECTED -> {
            bottomSheetTitle = stringResource(id = R.string.receive_data)
            progressStatusTitle = stringResource(id = R.string.device_disconnected)
            progressStatusMsg = stringResource(id = R.string.device_disconnected_msg)
          }
          else -> {
            bottomSheetTitle = stringResource(id = R.string.waiting_to_pair)
            progressStatusMsg = stringResource(id = R.string.waiting_to_pair_with_sender)
          }
        }

        transferCompleteMsg =
          stringResource(
            id = R.string.x_records_received,
            p2PUiState.transferProgress.transferredRecordCount,
            deviceName
          )
      }
    }

    when (p2PState) {
      P2PState.TRANSFER_COMPLETE -> {
        bottomSheetTitle =
          when (deviceRole) {
            DeviceRole.SENDER -> {
              stringResource(id = R.string.send_data)
            }
            DeviceRole.RECEIVER -> {
              stringResource(id = R.string.receive_data)
            }
          }
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
            .height(IntrinsicSize.Max)
            .background(WhiteColor)
            .padding(all = 16.dp)
      ) {
        Text(
          text = bottomSheetTitle,
          textAlign = TextAlign.Start,
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp
        )
        Icon(
          imageVector = Icons.Filled.Clear,
          contentDescription = null,
          tint = DefaultColor.copy(0.8f),
          modifier =
            modifier
              .clickable {
                coroutineScope.launch {
                  if (modalBottomSheetState.isVisible) modalBottomSheetState.hide()
                }
                onEvent(P2PEvent.BottomSheetClosed)
              }
              .testTag(BOTTOM_SHEET_CANCEL_ICON_TEST_TAG)
        )
      }

      if (p2PState != P2PState.PAIR_DEVICES_FOUND) {
        Spacer(modifier = Modifier.size(5.dp))

        when (p2PState) {
          P2PState.PAIR_DEVICES_SEARCH_FAILED -> {
            ProgressStatusIndicator(
              p2PUiState =
                p2PUiState.copy(
                  progressIndicator =
                    ProgressIndicator(
                      backgroundColor = DangerColor.copy(alpha = 0.2f),
                      icon = Icons.Filled.Clear
                    )
                )
            )
          }
          P2PState.CONNECT_TO_DEVICE_FAILED -> {
            ProgressStatusIndicator(
              p2PUiState =
                p2PUiState.copy(
                  progressIndicator =
                    ProgressIndicator(
                      backgroundColor = DangerColor.copy(alpha = 0.2f),
                      icon = Icons.Filled.Clear
                    )
                )
            )
          }
          P2PState.RECEIVE_BASIC_DEVICE_DETAILS_FAILED -> {
            ProgressStatusIndicator(
              p2PUiState =
                p2PUiState.copy(
                  progressIndicator =
                    ProgressIndicator(
                      backgroundColor = DangerColor.copy(alpha = 0.2f),
                      icon = Icons.Filled.Clear
                    )
                )
            )
          }
          P2PState.DEVICE_DISCONNECTED -> {
            ProgressStatusIndicator(
              showCircularProgressIndicator = false,
              p2PUiState =
                p2PUiState.copy(
                  progressIndicator =
                    ProgressIndicator(
                      backgroundColor = DangerColor.copy(alpha = 0.2f),
                      icon = Icons.Filled.Clear
                    )
                )
            )
          }
          P2PState.DATA_UP_TO_DATE -> {}
          else -> {
            ProgressStatusIndicator(
              showCircularProgressIndicator = showCircularProgressIndicator,
              p2PUiState = p2PUiState
            )
          }
        }

        Spacer(modifier = Modifier.size(5.dp))
        ProgressStatusText(title = progressStatusTitle, message = progressStatusMsg)
      }

      Spacer(modifier = Modifier.size(5.dp))
      Timber.d("BottomSheetScreen: Device Role: $deviceRole")
      Timber.d("BottomSheetScreen: P2P State: $p2PState")
      Timber.d("BottomSheetScreen: Devices list ${deviceList.size}")

      if (deviceRole == DeviceRole.SENDER) {
        if (p2PState == P2PState.PAIR_DEVICES_FOUND) {
          SelectPairDeviceRow(
            p2PUiState =
              p2PUiState.copy(
                progressIndicator =
                  ProgressIndicator(
                    backgroundColor = DefaultColor.copy(alpha = 0.2f),
                    progressIndicatorState = ProgressIndicatorState.EMPTY
                  )
              )
          )
        }
        when (p2PState) {
          P2PState.WIFI_AND_LOCATION_ENABLE,
          P2PState.SEARCHING_FOR_RECIPIENT,
          P2PState.PAIR_DEVICES_FOUND -> DisplayDeviceList(deviceList, onEvent = onEvent, p2PState)
          else -> {
            Timber.d("Device list Else p2p state. State is ${p2PState.name}")
          }
        }
      }

      Spacer(modifier = Modifier.size(5.dp))

      when (p2PState) {
        P2PState.TRANSFER_COMPLETE,
        P2PState.PAIR_DEVICES_SEARCH_FAILED,
        P2PState.CONNECT_TO_DEVICE_FAILED,
        P2PState.RECEIVE_BASIC_DEVICE_DETAILS_FAILED,
        P2PState.DATA_UP_TO_DATE,
        P2PState.DEVICE_DISCONNECTED -> DisplayButton(onEvent = onEvent)
        else -> {
          Timber.i("p2p state $p2PState is not handled")
        }
      }
    }
  }
}

@Composable
fun DisplayButton(modifier: Modifier = Modifier, onEvent: (P2PEvent) -> Unit) {
  Button(
    onClick = { onEvent(P2PEvent.DataTransferCompleteConfirmed) },
    modifier
      .padding(start = 10.dp, end = 10.dp)
      .fillMaxWidth()
      .testTag(BOTTOM_SHEET_BUTTON_TEST_TAG)
  ) { Text(text = stringResource(id = R.string.okay)) }
}

@Composable
fun DisplayDeviceList(
  deviceList: List<DeviceInfo>,
  onEvent: (P2PEvent) -> Unit,
  p2PState: P2PState
) {
  LazyColumn(
    modifier = Modifier.fillMaxWidth().testTag(P2P_BOTTOM_SHEET_LIST).background(WhiteColor)
  ) {
    itemsIndexed(deviceList) { index, item ->
      PairDeviceRow(device = item, onEvent = onEvent, p2PState = p2PState)
    }
  }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun PreviewBottomSheetScreen() {
  BottomSheet(
    deviceList = listOf(populateDeviceInfo()),
    onEvent = {},
    modalBottomSheetState = ModalBottomSheetState(ModalBottomSheetValue.HalfExpanded),
    p2PUiState = P2PUiState(),
    deviceName = "John",
    deviceRole = DeviceRole.SENDER,
    p2PState = P2PState.SEARCHING_FOR_RECIPIENT
  )
}

private fun populateDeviceInfo(): DeviceInfo {
  val wifiP2pDevice =
    WifiP2pDevice().apply {
      deviceName = "Google Pixel 7 android 12"
      deviceAddress = "00:00:5e:00:53:af"
    }
  return WifiDirectDataSharingStrategy.WifiDirectDevice(wifiP2pDevice)
}
