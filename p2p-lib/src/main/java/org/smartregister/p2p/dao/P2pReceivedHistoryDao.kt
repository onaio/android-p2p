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
package org.smartregister.p2p.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.smartregister.p2p.model.P2PReceivedHistory

/** Dao for manipulating [P2PReceivedHistory] records */
@Dao
interface P2pReceivedHistoryDao {

  @Insert fun addReceivedHistory(receivedP2PReceivedHistory: P2PReceivedHistory?)

  @Update fun updateReceivedHistory(receivedP2PReceivedHistory: P2PReceivedHistory?)

  @Query("DELETE FROM p2p_received_history WHERE app_lifetime_key = :appLifetimeKey")
  fun clearDeviceRecords(appLifetimeKey: String?): Int

  @Query("SELECT * FROM p2p_received_history WHERE app_lifetime_key = :appLifetimeKey")
  fun getDeviceReceivedHistory(appLifetimeKey: String?): List<P2PReceivedHistory?>?

  @Query(
    "SELECT * FROM p2p_received_history WHERE app_lifetime_key = :appLifetimeKey AND entity_type = :entityType LIMIT 1"
  )
  fun getHistory(appLifetimeKey: String?, entityType: String?): P2PReceivedHistory?
}
