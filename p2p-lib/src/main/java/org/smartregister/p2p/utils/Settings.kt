package org.smartregister.p2p.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.NonNull
import androidx.annotation.Nullable


/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */
class Settings(@NonNull context: Context) {
    private val sharedPreferences: SharedPreferences

    @get:Nullable
    val hashKey: String?
        get() = sharedPreferences.getString(Constants.Prefs.KEY_HASH, null)

    fun saveHashKey(@NonNull hashKey: String?) {
        sharedPreferences.edit()
            .putString(Constants.Prefs.KEY_HASH, hashKey)
            .apply()
    }

    init {
        sharedPreferences = context
            .getSharedPreferences(Constants.Prefs.NAME, Context.MODE_PRIVATE)
    }
}