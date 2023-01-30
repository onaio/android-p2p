package org.smartregister.p2p.search.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.search.contract.P2pModeSelectContract
import timber.log.Timber

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 13-01-2023.
 */
open class BaseViewModel(
    private val view: P2pModeSelectContract.View,
    private val dataSharingStrategy: DataSharingStrategy
) : ViewModel() {

    fun handleSocketException() {
        view.showToast("An error occurred")
        view.restartActivity()
    }

    fun disconnect() {
        viewModelScope.launch {
            dataSharingStrategy.disconnect(
                dataSharingStrategy.getCurrentDevice()!!,
                object : DataSharingStrategy.OperationListener {
                    override fun onSuccess(device: DeviceInfo?) {
                        Timber.i("Disconnection successful")
                        view.updateP2PState(P2PState.DEVICE_DISCONNECTED)
                    }

                    override fun onFailure(device: DeviceInfo?, ex: Exception) {
                        Timber.e(ex, "P2P diconnection failed")
                        view.updateP2PState(P2PState.DEVICE_DISCONNECTED)
                    }
                }
            )
        }
    }
}