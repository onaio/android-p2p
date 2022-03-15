package org.smartregister.p2p.search.contract

import org.smartregister.p2p.authentication.model.DeviceRole

/**
 * Interface for functions used to make changes to the data transfer page UI
 */
interface P2pModeSelectContract {

    fun showP2PSelectPage(deviceRole: DeviceRole, deviceName: String)

    fun getDeviceRole(): DeviceRole
}