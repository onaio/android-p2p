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
package org.smartregister.p2p.search.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.p2p.R
import org.smartregister.p2p.data_sharing.DeviceInfo
import timber.log.Timber

/** Recycler view adapter used to list discovered devices on the device list bottom sheet */
class DeviceListAdapter(
  private val peerDeviceList: List<DeviceInfo>,
  private val onDeviceClick: (deviceAddress: DeviceInfo) -> Unit
) : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceListAdapter.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val contactView: View = inflater.inflate(R.layout.device_list_item, parent, false)
    return ViewHolder(contactView, onDeviceClick)
  }

  override fun onBindViewHolder(holder: DeviceListAdapter.ViewHolder, position: Int) {
    val peerDevice = peerDeviceList[position]

    holder.deviceName.text = peerDevice.name()
    holder.deviceAddress.text = peerDevice.address()
    holder.currentDevice = peerDevice
  }

  override fun getItemCount(): Int = peerDeviceList.size

  /** View holder used by [DeviceListAdapter] */
  inner class ViewHolder(itemView: View, onDeviceClick: (device: DeviceInfo) -> Unit) :
    RecyclerView.ViewHolder(itemView) {

    val deviceName = itemView.findViewById<TextView>(R.id.device_item_title)
    val deviceAddress = itemView.findViewById<TextView>(R.id.device_item_subtitle)
    var currentDevice: DeviceInfo? = null

    init {
      itemView.setOnClickListener {
        currentDevice?.let {
          Timber.e("Item ${it.getDisplayName()} has been clicked")
          deviceAddress.setText(R.string.pairing)
          itemView.findViewById<ProgressBar>(R.id.device_item_pairing_icon).visibility =
            View.VISIBLE
          itemView.findViewById<ImageView>(R.id.device_item_icon).visibility = View.GONE

          onDeviceClick(it)
        }
      }
      itemView.setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
          itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.theme_blue))
        } else if (event.action == MotionEvent.ACTION_UP) {
          itemView.setBackgroundColor(
            ContextCompat.getColor(itemView.context, android.R.color.transparent)
          )
        }
        false
      }
    }
  }
}
