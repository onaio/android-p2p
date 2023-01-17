package org.smartregister.p2p.search.ui

import androidx.lifecycle.ViewModel
import org.smartregister.p2p.search.contract.P2pModeSelectContract

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 13-01-2023.
 */
open class BaseViewModel(
    private val view: P2pModeSelectContract.View) : ViewModel() {

    fun handleSocketException() {
        view.showToast("An error occurred")
        view.restartActivity()
    }
}