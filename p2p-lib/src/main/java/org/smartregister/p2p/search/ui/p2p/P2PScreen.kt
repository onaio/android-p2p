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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SendToMobile
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import org.smartregister.p2p.search.ui.p2p.components.ActionableButton
import org.smartregister.p2p.search.ui.theme.DefaultColor

const val P2P_SCREEN_TOP_BAR_TEST_TAG = "p2pScreenTopBarTestTag"
const val P2P_SCREEN_TOP_BAR_ICON_TEST_TAG = "p2pScreenTopBarIconTestTag"
const val P2P_SYNC_IMAGE_TEST_TAG = "p2pSyncImageTestTag"

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun P2PScreen(modifier: Modifier = Modifier, p2PViewModel: P2PViewModel) {

  val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
  val coroutineScope = rememberCoroutineScope()

  // bottom sheet updated
  var deviceRole by remember { mutableStateOf(DeviceRole.SENDER) }

  BottomSheetScaffold(
    sheetContent = { BottomSheetScreen(p2PViewModel = p2PViewModel, deviceRole = deviceRole) },
    scaffoldState = bottomSheetScaffoldState,
    sheetPeekHeight = 0.dp,
    sheetGesturesEnabled = true
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
              coroutineScope.launch { bottomSheetScaffoldState.bottomSheetState.expand() }
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
              coroutineScope.launch { bottomSheetScaffoldState.bottomSheetState.expand() }
            }
          )
          Spacer(modifier = modifier.height(20.dp))
        }
      }
    }
  }
}

@Composable
fun TransferComponent(modifier: Modifier, title: String, description: String) {
  Row(modifier = modifier) {
    Column(modifier = modifier.padding(end = 10.dp), verticalArrangement = Arrangement.Center) {
      Icon(
        imageVector = Icons.Filled.SendToMobile,
        contentDescription = null,
        tint = DefaultColor.copy(0.8f),
      )
    }
    Column() {
      Text(text = title)
      Text(text = description, color = DefaultColor)
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewP2PScreen() {
  // P2PScreen()
}
