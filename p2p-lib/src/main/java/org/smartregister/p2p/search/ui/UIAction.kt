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
package org.smartregister.p2p.search.ui

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 02-02-2023. */
enum class UIAction {
  SHOW_TRANSFER_COMPLETE_DIALOG,
  NOTIFY_DATA_TRANSFER_STARTING,
  SHOW_CANCEL_TRANSFER_DIALOG,
  SENDER_SYNC_COMPLETE,
  UPDATE_TRANSFER_PROGRESS,
  REQUEST_LOCATION_PERMISSIONS_ENABLE_LOCATION,
  FINISH,
  KEEP_SCREEN_ON,
  PROCESS_SENDER_DEVICE_DETAILS,
  SEND_DEVICE_DETAILS
}
