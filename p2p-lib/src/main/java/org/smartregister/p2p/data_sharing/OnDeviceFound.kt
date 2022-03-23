package org.smartregister.p2p.data_sharing

import java.lang.Exception

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 21-03-2022.
 */
interface OnDeviceFound {

    fun deviceFound(devices : List<DeviceInfo>)

    fun onDeviceFound(devices: List<DeviceInfo>)

    fun failed(ex: Exception)
}