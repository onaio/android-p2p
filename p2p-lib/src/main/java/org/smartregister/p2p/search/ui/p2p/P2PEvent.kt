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
package org.smartregister.p2p.search.ui.p2p

import org.smartregister.p2p.data_sharing.DeviceInfo

sealed class P2PEvent {
  object StartScanning : P2PEvent()
  object CancelDataTransfer : P2PEvent()
  object ConnectionBreakConfirmed : P2PEvent()
  object DataTransferCompleteConfirmed : P2PEvent()
  object BottomSheetClosed : P2PEvent()
  object DismissConnectionBreakDialog : P2PEvent()
  data class PairWithDevice(val device: DeviceInfo) : P2PEvent()
}
