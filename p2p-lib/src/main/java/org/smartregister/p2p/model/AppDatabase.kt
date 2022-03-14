package org.smartregister.p2p.model

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.annotation.NonNull
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.commonsware.cwac.saferoom.SafeHelperFactory;
import org.smartregister.p2p.dao.P2pReceivedHistoryDao


@Database(entities = [P2PReceivedHistory::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun p2pReceivedHistoryDao(): P2pReceivedHistoryDao?

    companion object {
        private var instance: AppDatabase? = null
        var dbName = "p2p"
        fun getInstance(@NonNull context: Context, @NonNull passphrase: String?): AppDatabase? {
            if (instance == null) {
                val safeHelperFactory: SafeHelperFactory =
                    SafeHelperFactory.fromUser(SpannableStringBuilder(passphrase))
                instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase::class.java, dbName
                )
                    .openHelperFactory(safeHelperFactory)
                    .build()
            }
            return instance
        }
    }
}