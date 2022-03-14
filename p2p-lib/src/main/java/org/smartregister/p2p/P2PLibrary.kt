package org.smartregister.p2p

import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import java.util.UUID
import org.smartregister.p2p.model.AppDatabase
import org.smartregister.p2p.utils.Constants
import org.smartregister.p2p.utils.Settings
import timber.log.Timber


/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 14-03-2022.
 */
class P2PLibrary private constructor() {


    private lateinit var options: Options
    private var hashKey: String? = null
    private var deviceUniqueIdentifier: String? = null

    companion object {
        private var instance: P2PLibrary? = null

        @NonNull
        fun getInstance(): P2PLibrary? {
            checkNotNull(instance) {
                ("Instance does not exist!!! Call P2PLibrary.init method"
                        + "in the onCreate method of "
                        + "your Application class ")
            }
            return instance
        }
    }

    fun init(@NonNull options: Options) {
        instance = P2PLibrary(options)
    }

    private constructor(@NonNull options: Options) : this() {
        this.options = options

        // We should not override the host applications Timber trees
        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }
        hashKey = getHashKey()

        // Start the DB
        AppDatabase.getInstance(getContext(), options.dbPassphrase)
    }

    @NonNull
    fun getDb(): AppDatabase? {
        return AppDatabase.getInstance(getContext(), options.dbPassphrase)
    }

    @NonNull
    fun getHashKey(): String? {
        if (hashKey == null) {
            val settings = Settings(getContext())
            hashKey = settings.hashKey
            if (hashKey == null) {
                hashKey = generateHashKey()
                settings.saveHashKey(hashKey)
            }
        }
        return hashKey
    }

    private fun generateHashKey(): String {
        return UUID.randomUUID().toString()
    }


    fun setDeviceUniqueIdentifier(@NonNull deviceUniqueIdentifier: String?) {
        this.deviceUniqueIdentifier = deviceUniqueIdentifier
    }

    @Nullable
    fun getDeviceUniqueIdentifier(): String? {
        return deviceUniqueIdentifier
    }


    @NonNull
    fun getUsername(): String {
        return options.username
    }

    @NonNull
    fun getContext(): Context {
        return options.context
    }

    fun getBatchSize(): Int {
        return options.batchSize
    }

    class Options(
        val context: Context,
        val dbPassphrase: String,
        val username: String,
    ) {
        var batchSize: Int = Constants.DEFAULT_SHARE_BATCH_SIZE
    }
}