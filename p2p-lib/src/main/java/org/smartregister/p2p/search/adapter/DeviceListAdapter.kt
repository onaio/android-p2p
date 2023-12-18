/*
 * License text copyright (c) 2020 MariaDB Corporation Ab, All Rights Reserved.
 * “Business Source License” is a trademark of MariaDB Corporation Ab.
 *
 * Parameters
 *
 * Licensor:             Ona Systems, Inc.
 * Licensed Work:        android-p2p. The Licensed Work is (c) 2023 Ona Systems, Inc.
 * Additional Use Grant: You may make production use of the Licensed Work,
 *                       provided such use does not include offering the Licensed Work
 *                       to third parties on a hosted or embedded basis which is
 *                       competitive with Ona Systems' products.
 * Change Date:          Four years from the date the Licensed Work is published.
 * Change License:       MPL 2.0
 *
 * For information about alternative licensing arrangements for the Licensed Work,
 * please contact licensing@ona.io.
 *
 * Notice
 *
 * Business Source License 1.1
 *
 * Terms
 *
 * The Licensor hereby grants you the right to copy, modify, create derivative
 * works, redistribute, and make non-production use of the Licensed Work. The
 * Licensor may make an Additional Use Grant, above, permitting limited production use.
 *
 * Effective on the Change Date, or the fourth anniversary of the first publicly
 * available distribution of a specific version of the Licensed Work under this
 * License, whichever comes first, the Licensor hereby grants you rights under
 * the terms of the Change License, and the rights granted in the paragraph
 * above terminate.
 *
 * If your use of the Licensed Work does not comply with the requirements
 * currently in effect as described in this License, you must purchase a
 * commercial license from the Licensor, its affiliated entities, or authorized
 * resellers, or you must refrain from using the Licensed Work.
 *
 * All copies of the original and modified Licensed Work, and derivative works
 * of the Licensed Work, are subject to this License. This License applies
 * separately for each version of the Licensed Work and the Change Date may vary
 * for each version of the Licensed Work released by Licensor.
 *
 * You must conspicuously display this License on each original or modified copy
 * of the Licensed Work. If you receive the Licensed Work in original or
 * modified form from a third party, the terms and conditions set forth in this
 * License apply to your use of that work.
 *
 * Any use of the Licensed Work in violation of this License will automatically
 * terminate your rights under this License for the current and all other
 * versions of the Licensed Work.
 *
 * This License does not grant you any right in any trademark or logo of
 * Licensor or its affiliates (provided that you may use a trademark or logo of
 * Licensor as expressly required by this License).
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE LICENSED WORK IS PROVIDED ON
 * AN “AS IS” BASIS. LICENSOR HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS,
 * EXPRESS OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND
 * TITLE.
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
