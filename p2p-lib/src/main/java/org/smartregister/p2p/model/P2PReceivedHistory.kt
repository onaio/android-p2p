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

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "p2p_received_history", primaryKeys = ["entity_type", "app_lifetime_key"])
class P2PReceivedHistory {
  @NonNull @ColumnInfo(name = "app_lifetime_key") var appLifetimeKey: String? = null

  @NonNull @ColumnInfo(name = "entity_type") var entityType: String? = null

  @ColumnInfo(name = "last_updated_at") var lastUpdatedAt: Long = 0
}
