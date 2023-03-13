/*
 * Copyright 2022-2023 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartregister.p2p.search.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.smartregister.p2p.R
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.model.P2PState
import timber.log.Timber

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 13-01-2023. */
open class BaseViewModel(private val dataSharingStrategy: DataSharingStrategy) : ViewModel() {

  val displayMessages: LiveData<Int>
    get() = _displayMessages
  val _displayMessages = MutableLiveData<Int>()

  val restartActivity: LiveData<Boolean>
    get() = _restartActivity
  val _restartActivity = MutableLiveData<Boolean>()

  val _p2pState = MutableLiveData<P2PState>()
  val p2pState: LiveData<P2PState>
    get() = _p2pState

  val p2pUiAction: LiveData<Pair<UIAction, Any?>>
    get() = _p2pUiAction
  val _p2pUiAction = MutableLiveData<Pair<UIAction, Any?>>()

  fun handleSocketException() {
    showToast(R.string.an_error_occurred_restarting_the_p2p_screen)
    restartActivity()
  }

  fun showToast(msgResId: Int) {
    _displayMessages.postValue(msgResId)
  }

  fun restartActivity() {
    _restartActivity.postValue(true)
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
