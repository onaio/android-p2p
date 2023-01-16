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

import org.smartregister.p2p.authentication.model.DeviceRole
import org.smartregister.p2p.model.P2PDialogState
import org.smartregister.p2p.model.ProgressIndicator
import org.smartregister.p2p.model.TransferProgress

data class P2PUiState(
  val state: String = "",
  val deviceRole: DeviceRole = DeviceRole.SENDER,
  val p2PDialogState: P2PDialogState = P2PDialogState(),
  val progressIndicator: ProgressIndicator = ProgressIndicator(),
  val transferProgress: TransferProgress = TransferProgress()
)

fun p2PUiStateOf(
  state: String = "",
  deviceRole: DeviceRole = DeviceRole.SENDER,
  p2PDialogState: P2PDialogState = P2PDialogState(),
  progressIndicator: ProgressIndicator,
  transferProgress: TransferProgress
): P2PUiState {
  return P2PUiState(
    state = state,
    deviceRole = deviceRole,
    p2PDialogState = p2PDialogState,
    progressIndicator = progressIndicator,
    transferProgress = transferProgress
  )
}
