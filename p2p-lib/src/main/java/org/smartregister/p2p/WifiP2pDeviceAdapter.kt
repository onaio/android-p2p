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
