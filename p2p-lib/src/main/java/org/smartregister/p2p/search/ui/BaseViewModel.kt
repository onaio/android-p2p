package org.smartregister.p2p.search.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    private val dataSharingStrategy: DataSharingStrategy
) : ViewModel() {

    val displayMessages = MutableLiveData<String>()
    val restartActivity = MutableLiveData<Boolean>()
    val _p2pState =  MutableLiveData<P2PState>()
    val p2pState :  LiveData<P2PState>
            get() = _p2pState
    val p2pUiAction : LiveData<Pair<UIAction, Any?>>
            get() = _p2pUiAction
    val _p2pUiAction = MutableLiveData<Pair<UIAction, Any?>>()

    /**
     - update state
     - restart the activity
     - show toast
     */

    fun handleSocketException() {
        showToast("An error occurred")
        restartActivity()
    }

    fun showToast(msg: String) {
        displayMessages.postValue(msg)
    }

    fun restartActivity() {
        restartActivity.postValue(true)
    }

    open fun updateP2PState(state: P2PState) {
        _p2pState.postValue(state)
    }

    fun postUIAction(p2PUiState: UIAction, data: Any? = null) {
        _p2pUiAction.postValue(Pair(p2PUiState, data))
    }

    fun disconnect() {
        viewModelScope.launch {
            dataSharingStrategy.disconnect(
                dataSharingStrategy.getCurrentDevice()!!,
                object : DataSharingStrategy.OperationListener {
                    override fun onSuccess(device: DeviceInfo?) {
                        Timber.i("Disconnection successful")
                        updateP2PState(P2PState.DEVICE_DISCONNECTED)
                    }

                    override fun onFailure(device: DeviceInfo?, ex: Exception) {
                        Timber.e(ex, "P2P diconnection failed")
                        updateP2PState(P2PState.DEVICE_DISCONNECTED)
                    }
                }
            )
        }
    }
}