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
