package org.smartregister.p2p.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity


@Entity(tableName = "p2p_received_history", primaryKeys = ["entity_type", "app_lifetime_key"])
class P2PReceivedHistory {
    @NonNull
    @ColumnInfo(name = "app_lifetime_key")
    var appLifetimeKey: String? = null

    @NonNull
    @ColumnInfo(name = "entity_type")
    var entityType: String? = null

    @ColumnInfo(name = "last_updated_at")
    var lastUpdatedAt: Long = 0
}