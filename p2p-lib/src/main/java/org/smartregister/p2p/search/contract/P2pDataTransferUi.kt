package org.smartregister.p2p.search.contract

import org.smartregister.p2p.authentication.model.DeviceRole

interface P2pDataTransferUi {

    fun showP2PSelectPage(deviceRole: DeviceRole, deviceName: String)

    fun getDeviceRole(): DeviceRole
}