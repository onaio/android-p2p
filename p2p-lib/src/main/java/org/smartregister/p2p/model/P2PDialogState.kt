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
package org.smartregister.p2p.model

class P2PDialogState(
  val showCancelTransferDialog: Boolean = false,
  val closeConnection: Boolean = true
)

fun p2PDialogStateOf(
  showTransferDialog: Boolean = false,
  closeConnection: Boolean = true
): P2PDialogState {
  return P2PDialogState(
    showCancelTransferDialog = showTransferDialog,
    closeConnection = closeConnection
  )
}
