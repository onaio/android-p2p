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
package org.smartregister.p2p.utils

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 14-03-2022. */
interface Constants {

  /** SharedPreference keys */
  interface Prefs {
    companion object {
      const val NAME = "android-p2p-sync"
      const val KEY_HASH = "hash-key"
    }
  }

  companion object {
    const val DEFAULT_SHARE_BATCH_SIZE = 20
    const val DEFAULT_MIN_DEVICE_CONNECTION_RETRY_DURATION = 2 * 60 * 60
    const val SEND_SYNC_PARAMS = "SEND-SYNC-PARAMS"
    const val SYNC_COMPLETE = "SYNC-COMPLETE"
  }

  interface BasicDeviceDetails {
    companion object {
      const val KEY_DEVICE_ID = "device-id"
      const val KEY_APP_LIFETIME_KEY = "app-lifetime-key"
    }
  }

  enum class ConnectionLevel {
    AUTHENTICATED,
    RECEIPT_OF_RECEIVED_HISTORY
  }

}
