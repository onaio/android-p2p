package org.smartregister.p2p.search.adapter

import android.net.wifi.p2p.WifiP2pDevice
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
import timber.log.Timber


/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 27-02-2022.
 */
class DeviceListAdapter(private val peerDeviceList: List<WifiP2pDevice>, private val onDeviceClick: (deviceAddress: WifiP2pDevice) -> Unit) : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DeviceListAdapter.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val contactView: View = inflater.inflate(R.layout.device_list_item, parent, false)
        return ViewHolder(contactView, onDeviceClick)
    }

    override fun onBindViewHolder(holder: DeviceListAdapter.ViewHolder, position: Int) {
        val peerDevice = peerDeviceList[position]

        holder.deviceName.setText(peerDevice.deviceName)
        holder.deviceAddress.setText(peerDevice.deviceAddress)
        holder.currentDevice = peerDevice
    }

    override fun getItemCount(): Int = peerDeviceList.size


    inner class ViewHolder(itemView: View, onDeviceClick: (deviceAddress: WifiP2pDevice) -> Unit) : RecyclerView.ViewHolder(itemView) {

        val deviceName = itemView.findViewById<TextView>(R.id.device_item_title)
        val deviceAddress = itemView.findViewById<TextView>(R.id.device_item_subtitle)
        var currentDevice: WifiP2pDevice? = null

        init {
            itemView.setOnClickListener { currentDevice?.let {
                Timber.e("Item ${it.deviceName} has been clicked")
                deviceAddress.setText(R.string.pairing)
                itemView.findViewById<ProgressBar>(R.id.device_item_pairing_icon).visibility = View.VISIBLE
                itemView.findViewById<ImageView>(R.id.device_item_icon).visibility = View.GONE

                onDeviceClick(it)
            }
            }
            itemView.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    itemView.setBackgroundColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.theme_blue
                        )
                    )
                } else if (event.action == MotionEvent.ACTION_UP) {
                    itemView.setBackgroundColor(
                        ContextCompat.getColor(
                            itemView.context,
                            android.R.color.transparent
                        )
                    )
                }
                false
            }
        }
    }
}

