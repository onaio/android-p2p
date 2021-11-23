package com.trevorgowing.android.p2p

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WifiP2pDeviceAdapter(private val devices: List<WifiP2pDevice>) :
    RecyclerView.Adapter<WifiP2pDeviceAdapter.WifiP2pDeviceViewHolder>() {

  class WifiP2pDeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val nameView: TextView = view.findViewById(R.id.wifi_p2p_device_name_value)
    val addressView: TextView = view.findViewById(R.id.wifi_p2p_device_address_value)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiP2pDeviceViewHolder {
    val deviceLayout =
        LayoutInflater.from(parent.context).inflate(R.layout.wifi_p2p_device, parent, false)
    return WifiP2pDeviceViewHolder(deviceLayout)
  }

  override fun onBindViewHolder(holder: WifiP2pDeviceViewHolder, position: Int) {
    val device = devices[position]
    holder.nameView.text = device.deviceName
    holder.addressView.text = device.deviceAddress
  }

  override fun getItemCount(): Int = devices.size
}
