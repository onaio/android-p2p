/*
 * Copyright 2022 Ona Systems, Inc
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
package org.smartregister.p2p

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WifiP2pDeviceAdapter(
  private val devices: List<WifiP2pDevice>,
  private val deviceClickHandler: (device: WifiP2pDevice) -> Unit
) : RecyclerView.Adapter<WifiP2pDeviceAdapter.WifiP2pDeviceViewHolder>() {

  class WifiP2pDeviceViewHolder(view: View, onDeviceClick: (device: WifiP2pDevice) -> Unit) :
    RecyclerView.ViewHolder(view) {
    private val nameView: TextView = view.findViewById(R.id.wifi_p2p_device_name_value)
    private val addressView: TextView = view.findViewById(R.id.wifi_p2p_device_address_value)
    private var currentDevice: WifiP2pDevice? = null

    init {
      view.setOnClickListener { currentDevice?.let { onDeviceClick(it) } }
    }

    fun bind(device: WifiP2pDevice) {
      currentDevice = device
      nameView.text = device.deviceName
      addressView.text = device.deviceAddress
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiP2pDeviceViewHolder {
    val deviceLayout =
      LayoutInflater.from(parent.context).inflate(R.layout.wifi_p2p_device, parent, false)
    return WifiP2pDeviceViewHolder(deviceLayout, deviceClickHandler)
  }

  override fun onBindViewHolder(holder: WifiP2pDeviceViewHolder, position: Int) =
    holder.bind(devices[position])

  override fun getItemCount(): Int = devices.size
}
