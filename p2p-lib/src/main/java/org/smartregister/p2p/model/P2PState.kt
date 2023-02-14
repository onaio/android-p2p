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
package org.smartregister.p2p.model

/** Enum showing the state of the p2p process */
enum class P2PState {
  INITIATE_DATA_TRANSFER,
  RECEIVING_DATA,
  PREPARING_TO_SEND_DATA,
  PROMPT_NEXT_TRANSFER,
  SEARCHING_FOR_RECIPIENT,
  TRANSFER_CANCELLED,
  TRANSFER_COMPLETE,
  TRANSFERRING_DATA,
  WAITING_TO_RECEIVE_DATA,
  PAIR_DEVICES_FOUND,
  PAIR_DEVICES_SEARCH_FAILED,
  CONNECT_TO_DEVICE_FAILED,
  WIFI_AND_LOCATION_ENABLE,
  RECEIVE_BASIC_DEVICE_DETAILS_FAILED,
  DATA_UP_TO_DATE,
  DEVICE_DISCONNECTED
}
