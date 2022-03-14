package org.smartregister.p2p.dao

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.smartregister.p2p.model.P2PReceivedHistory

@Dao
open interface P2pReceivedHistoryDao {
    
    @Insert
    fun addReceivedHistory(@NonNull receivedP2PReceivedHistory: P2PReceivedHistory?)

    @Update
    fun updateReceivedHistory(@NonNull receivedP2PReceivedHistory: P2PReceivedHistory?)

    @Query("DELETE FROM p2p_received_history WHERE app_lifetime_key = :appLifetimeKey")
    fun clearDeviceRecords(@NonNull appLifetimeKey: String?): Int

    @Query("SELECT * FROM p2p_received_history WHERE app_lifetime_key = :appLifetimeKey")
    fun getDeviceReceivedHistory(@NonNull appLifetimeKey: String?): List<P2PReceivedHistory?>?

    @Nullable
    @Query("SELECT * FROM p2p_received_history WHERE app_lifetime_key = :appLifetimeKey AND entity_type = :entityType LIMIT 1")
    fun getHistory(
        @NonNull appLifetimeKey: String?,
        @NonNull entityType: String?
    ): P2PReceivedHistory?
}