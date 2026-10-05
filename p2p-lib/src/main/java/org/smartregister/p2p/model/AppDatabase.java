package org.smartregister.p2p.model;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

import org.smartregister.p2p.dao.P2pReceivedHistoryDao;

import java.nio.charset.StandardCharsets;

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 11-05-2022.
 */
@Database(entities = {P2PReceivedHistory.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;
    public static final String DB_NAME = "p2p";

    @NonNull
    public static AppDatabase getInstance(@NonNull Context context, @NonNull String passphrase) {
        if (instance == null) {
            // net.zetetic:sqlcipher-android replaces android-database-sqlcipher (EOL at 4.5.4,
            // whose libsqlcipher.so is only 4 KB aligned and so fails Android's 16 KB page-size
            // requirement). SupportFactory -> SupportOpenHelperFactory, and the removed
            // SQLiteDatabase.getBytes(char[]) was plain UTF-8 encoding, so existing databases
            // still open with the same key bytes.
            SupportOpenHelperFactory safeHelperFactory =
                    new SupportOpenHelperFactory(passphrase.getBytes(StandardCharsets.UTF_8));
            instance =
                    Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME)
                            .openHelperFactory(safeHelperFactory)
                            .build();
        }

        return instance;
    }

    @NonNull
    public abstract P2pReceivedHistoryDao p2pReceivedHistoryDao();
}
