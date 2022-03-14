package org.smartregister.p2p.utils

/**
* Created by Ephraim Kigamba - nek.eam@gmail.com on 14-03-2022.
*/

interface Constants {

    interface Prefs {
        companion object {
            const val NAME = "android-p2p-sync"
            const val KEY_HASH = "hash-key"
        }
    }

    companion object {
        const val DEFAULT_SHARE_BATCH_SIZE = 20
        const val  DEFAULT_MIN_DEVICE_CONNECTION_RETRY_DURATION = 2 * 60 * 60
    }

}