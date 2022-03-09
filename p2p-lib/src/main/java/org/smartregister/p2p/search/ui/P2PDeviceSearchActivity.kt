package org.smartregister.p2p.search.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import org.smartregister.p2p.R
import org.smartregister.p2p.WifiP2pBroadcastReceiver
import org.smartregister.p2p.search.adapter.DeviceListAdapter
import org.smartregister.p2p.search.contract.P2PManagerListener
import org.smartregister.p2p.utils.getDeviceName
import org.smartregister.p2p.utils.startP2PScreen
import timber.log.Timber


/**
 * This is the exposed activity that provides access to all P2P operations and steps. It can be
 * called from other apps via [startP2PScreen] function.
 */
class P2PDeviceSearchActivity : AppCompatActivity(), P2PManagerListener {

    private val wifiP2pManager: WifiP2pManager by
    lazy(LazyThreadSafetyMode.NONE) { getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager }
    private var wifiP2pChannel: WifiP2pManager.Channel? = null
    private var wifiP2pReceiver: BroadcastReceiver? = null
    private val accessFineLocationPermissionRequestInt: Int = 12345
    private var sender = false
    private var scanning = false
    private lateinit var interactiveDialog: BottomSheetDialog

    private val rootView : View by lazy {
        findViewById(R.id.device_search_root_layout)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_p2_pdevice_search)

        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }

        title = getString(R.string.device_to_device_sync)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel)

        findViewById<Button>(R.id.scanDevicesBtn).setOnClickListener {
            scanning = true
            startScanning()
        }
    }

    fun startScanning() {
        // Wifi P2p
        wifiP2pChannel = wifiP2pManager.initialize(this, mainLooper, null)
        wifiP2pChannel?.also { channel ->
            wifiP2pReceiver = WifiP2pBroadcastReceiver(wifiP2pManager, channel, this, this)
        }

        renameWifiDirectName();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestAccessFineLocationIfNotGranted()
        }

        listenForWifiP2pIntents()
        initiatePeerDiscovery()

        showScanningDialog()

    }

    fun renameWifiDirectName() {
        val deviceName = getDeviceName(this)

        // Copy the text to the Clipboard
        val clipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Wifi-direct name", deviceName)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this,  "Go to Wifi-Direct settings > Rename device and paste into the text box", Toast.LENGTH_LONG)
            .show()

        if (Build.VERSION.SDK_INT > 24) {
            val turnWifiOn = Intent(Settings.ACTION_WIFI_SETTINGS)
            startActivity(turnWifiOn)
        } else {
            val setDeviceNameMethod = wifiP2pManager.javaClass.getMethod(
                "setDeviceName",
                    wifiP2pChannel!!.javaClass,
                    String::class.java,
                    WifiP2pManager.ActionListener::class.java
            )
            setDeviceNameMethod.invoke(
                wifiP2pManager,
                wifiP2pChannel,
                deviceName,
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Timber.e("Change device name worked")
                    }

                    override fun onFailure(reason: Int) {
                        Timber.e("Change device name did not work")
                    }
                })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        if (scanning) {
            listenForWifiP2pIntents()
            initiatePeerDiscoveryOnceAccessFineLocationGranted()
            requestDeviceInfo()
            requestConnectionInfo()
        }
    }

    private fun listenForWifiP2pIntents() {
        wifiP2pReceiver?.also {
            registerReceiver(
                it,
                IntentFilter().apply {
                    addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
                }
            )
        }
    }

    private fun initiatePeerDiscoveryOnceAccessFineLocationGranted() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestAccessFineLocationIfNotGranted()
            } else {
                handleMinimumSDKVersionNotMet(Build.VERSION_CODES.M)
            }
        } else {
            initiatePeerDiscovery()
        }
    }

    private fun requestDeviceInfo() {
        wifiP2pChannel?.also { wifiP2pChannel ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return handleAccessFineLocationNotGranted()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiP2pManager.requestDeviceInfo(wifiP2pChannel) {
                    if (it != null) {
                        handleWifiP2pDevice(it)
                    }
                }
            } else {
                // TODO: Handle fetching device details
            }
        }
    }

    private fun requestConnectionInfo() {
        wifiP2pManager.requestConnectionInfo(wifiP2pChannel) {
            onConnectionInfoAvailable(it)
        }
    }

    override fun onPause() {
        super.onPause()
        wifiP2pReceiver?.also { unregisterReceiver(it) }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestAccessFineLocationIfNotGranted() {
        when (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> logDebug("Wifi P2P: Access fine location granted")
            else -> {
                logDebug("Wifi P2P: Requesting access fine location permission")
                return requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    accessFineLocationPermissionRequestInt
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == accessFineLocationPermissionRequestInt) {
            val accessFineLocationPermissionIndex =
                permissions.indexOfFirst { it == Manifest.permission.ACCESS_FINE_LOCATION }
            if (grantResults[accessFineLocationPermissionIndex] == PackageManager.PERMISSION_GRANTED) {
                return logDebug("Wifi P2P: Access fine location granted")
            }
        }
    }

    override fun handleWifiP2pDisabled() {
        val message = "Wifi P2P: Disabled"
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
        /*findViewById<TextView>(R.id.wifi_p2p_enabled_value).apply {
            text = getString(R.string.wifi_p2p_disabled_value)
        }*/
        Timber.d(message)
    }

    override fun handleWifiP2pEnabled() {
        val message = "Wifi P2P: Enabled"
        /*findViewById<TextView>(R.id.wifi_p2p_enabled_value).apply {
            text = getString(R.string.wifi_p2p_enabled_value)
        }*/
        Timber.d(message)
    }

    override fun handleUnexpectedWifiP2pState(wifiState: Int) {
        val message = "Wifi P2P: Unexpected state: $wifiState"
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
        /*findViewById<TextView>(R.id.wifi_p2p_enabled_value).apply {
            text = getString(R.string.wifi_p2p_unexpected_state_value, wifiState)
        }*/
        Timber.d(message)
    }

    override fun handleWifiP2pDevice(device: WifiP2pDevice) {
        if (device.deviceName != getDeviceName(this)) {
            renameWifiDirectName()
        }

        Timber.d("Wifi P2P: Device: ${device.deviceAddress}")
    }

    private fun initiatePeerDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return handleAccessFineLocationNotGranted()
        }

        wifiP2pManager.discoverPeers(
            wifiP2pChannel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    handleP2pDiscoverySuccess()
                }

                override fun onFailure(reason: Int) {
                    handleP2pDiscoveryFailure(reason)
                }
            }
        )
        Timber.d("Peer discovery initiated")
    }

    private fun handleP2pDiscoverySuccess() {
        val message = "Wifi P2P: Peer discovery succeeded"
        Timber.d(message)
    }

    private fun handleP2pDiscoveryFailure(reasonInt: Int) {
        val reason = getWifiP2pReason(reasonInt)
        val message = "Wifi P2P: Peer discovery failed: $reason"
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
        Timber.d(message)
    }

    override fun handleP2pDiscoveryStarted() {
        /*findViewById<TextView>(R.id.wifi_p2p_discovery_value).apply {
            text = getString(R.string.wifi_p2p_discovery_started_value)
        }*/
        Timber.d("Wifi P2P: Peer discovery started")
    }

    override fun handleP2pDiscoveryStopped() {
        /*findViewById<TextView>(R.id.wifi_p2p_discovery_value).apply {
            text = getString(R.string.wifi_p2p_discovery_started_value)
        }*/
        Timber.d("Wifi P2P: Peer discovery stopped")
    }

    override fun handleUnexpectedWifiP2pDiscoveryState(discoveryState: Int) {
        /*findViewById<TextView>(R.id.wifi_p2p_discovery_value).apply {
            text = getString(R.string.wifi_p2p_unexpected_state_value, discoveryState)
        }*/
        Timber.d("Wifi P2P: Unexpected discovery state: $discoveryState")
    }

    override fun handleP2pPeersChanged(peerDeviceList: WifiP2pDeviceList) {
        Timber.d("Wifi P2P: Peers x ${peerDeviceList.deviceList.size}")

        showDevicesList(peerDeviceList)
    }


    fun showScanningDialog() {
        interactiveDialog = BottomSheetDialog(this)
        interactiveDialog.setContentView(R.layout.devices_list_bottom_sheet)
        interactiveDialog.setTitle(getString(R.string.nearby_devices))

        interactiveDialog.findViewById<TextView>(R.id.device_search_description)
            ?.setText(String.format(getString(R.string.looking_for_nearby_devices_as), getDeviceName(this)))

        interactiveDialog.findViewById<ImageButton>(R.id.dialog_close)
            ?.setOnClickListener {
                interactiveDialog.cancel()
                //stopScanning()
            }

        interactiveDialog.setCancelable(false)
        interactiveDialog.show()
    }

    fun showDevicesList(peerDeviceList: WifiP2pDeviceList) {

        interactiveDialog.findViewById<ConstraintLayout>(R.id.loading_devices_layout)?.visibility = View.GONE
        interactiveDialog.findViewById<ConstraintLayout>(R.id.devices_list_layout)?.visibility = View.VISIBLE
        val devicesListRecyclerView = interactiveDialog.findViewById<RecyclerView>(R.id.devices_list_recycler_view)
        if (devicesListRecyclerView != null) {
            devicesListRecyclerView.adapter = DeviceListAdapter(peerDeviceList.deviceList.toList(), { connectToDevice(it) })
            devicesListRecyclerView.layoutManager = LinearLayoutManager(this)
        }
    }

    fun showTransferDialog(practitionerName: String) {
        interactiveDialog = BottomSheetDialog(this)
        interactiveDialog.setContentView(R.layout.data_transfer_bottom_sheet)
        interactiveDialog.setTitle(getString(R.string.start_sending_data))

        interactiveDialog.findViewById<TextView>(R.id.data_transfer_description)
            ?.setText(String.format(getString(R.string.start_sending_data_to), practitionerName))

        interactiveDialog.findViewById<ImageButton>(R.id.data_transfer_dialog_close)
            ?.setOnClickListener {
                interactiveDialog.cancel()
            }

        interactiveDialog.setCancelable(false)
        interactiveDialog.show()
    }

    private fun connectToDevice(device: WifiP2pDevice) {
        Timber.d("Wifi P2P: Initiating connection to device: ${device.deviceName}")
        sender = true
        val wifiP2pConfig = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
        wifiP2pChannel?.also { wifiP2pChannel ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return handleAccessFineLocationNotGranted()
            }
            wifiP2pManager.connect(wifiP2pChannel, wifiP2pConfig, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    handleDeviceConnectionSuccess(device)
                }

                override fun onFailure(reason: Int) {
                    handleDeviceConnectionFailure(device, reason)
                }
            })
        }
    }

    private fun handleDeviceConnectionSuccess(device: WifiP2pDevice) {
        /*findViewById<TextView>(R.id.wifi_p2p_connection_value).apply {
            text = device.deviceName
        }*/
        Timber.d("Wifi P2P: Successfully connected to device: ${device.deviceAddress}")
    }

    private fun handleDeviceConnectionFailure(device: WifiP2pDevice, reasonInt: Int) {
        val reason = getWifiP2pReason(reasonInt)
        /*findViewById<TextView>(R.id.wifi_p2p_connection_value).apply {
            text = getString(R.string.wifi_p2p_connection_failed_value, reason)
        }*/
        Timber.d("Wifi P2P: Failed to connect to device: ${device.deviceAddress} due to: $reason")
    }

    override fun handleAccessFineLocationNotGranted() {
        val message = "Wifi P2P: Access fine location permission not granted"
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
        Timber.d(message)
    }

    override fun handleMinimumSDKVersionNotMet(minimumSdkVersion: Int) {
        logDebug("Wifi P2P: Minimum SDK Version not met: $minimumSdkVersion")
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        val message = "Connection info available: groupFormed = ${info.groupFormed}, isGroupOwner = ${info.isGroupOwner}"
        Timber.d(message)
        /*findViewById<TextView>(R.id.wifi_p2p_group_formed_value).apply {
            text = resources.getString(if (info.groupFormed) R.string.wifi_p2p_value_yes else R.string.wifi_p2p_value_no)
        }
        findViewById<TextView>(R.id.wifi_p2p_group_owner_value).apply {
            text = resources.getString(if (info.isGroupOwner) R.string.wifi_p2p_value_yes else R.string.wifi_p2p_value_no)
        }
        findViewById<TextView>(R.id.wifi_p2p_group_owner_address_value).apply {
            text = if (info.groupOwnerAddress == null) resources.getString(R.string.wifi_p2p_group_owner_value_na) else info.groupOwnerAddress.hostAddress
        }*/
        if (info.groupFormed) {
            // Start syncing given the ip addresses
        }
    }

    private fun getWifiP2pReason(reasonInt: Int): String =
        when (reasonInt) {
            0 -> "Error"
            1 -> "Unsupported"
            2 -> "Busy"
            else -> "Unknown"
        }

    private fun logDebug(message: String) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
        Timber.d(message)
    }
}