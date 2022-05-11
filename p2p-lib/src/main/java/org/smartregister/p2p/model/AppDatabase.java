package org.smartregister.p2p.model;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

import org.smartregister.p2p.dao.P2pReceivedHistoryDao;

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 11-05-2022.
 */
@Database(entities = {P2PReceivedHistory.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;
    public static final String dbName = "p2p";

    public static AppDatabase getInstance(@NonNull Context context, @NonNull String passphrase) {
        if (instance == null) {
            SupportFactory safeHelperFactory = new SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()));
            instance =
                    Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, dbName)
                            .openHelperFactory(safeHelperFactory)
                            .build();
        }

        return instance;
    }

    public abstract P2pReceivedHistoryDao p2pReceivedHistoryDao();
}
