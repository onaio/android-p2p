package com.trevorgowing.android.p2p

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.trevorgowing.android.p2p.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), WifiP2pManager.ConnectionInfoListener {

  private lateinit var appBarConfiguration: AppBarConfiguration
  private lateinit var binding: ActivityMainBinding

  // Wifi P2p
  private val wifiP2pManager: WifiP2pManager by
    lazy(LazyThreadSafetyMode.NONE) { getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager }
  var wifiP2pChannel: WifiP2pManager.Channel? = null
  var wifiP2pReceiver: BroadcastReceiver? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)

    val navController = findNavController(R.id.nav_host_fragment_content_main)
    appBarConfiguration = AppBarConfiguration(navController.graph)
    setupActionBarWithNavController(navController, appBarConfiguration)

    binding.fab.setOnClickListener { initiatePeerDiscovery() }

    // Wifi P2p
    wifiP2pChannel = wifiP2pManager.initialize(this, mainLooper, null)
    wifiP2pChannel?.also { channel ->
      wifiP2pReceiver = WifiP2pBroadcastReceiver(wifiP2pManager, channel, this)
      wifiP2pManager.requestConnectionInfo(channel, this)
    }
  }

  override fun onResume() {
    super.onResume()
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

  override fun onPause() {
    super.onPause()
    wifiP2pReceiver?.also { unregisterReceiver(it) }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    return when (item.itemId) {
      R.id.action_settings -> true
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment_content_main)
    return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
  }

  fun handleWifiP2pDisabled() {
    val message = "Wifi P2P: Disabled"
    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    findViewById<TextView>(R.id.wifi_p2p_enabled_value).apply {
      text = getString(R.string.wifi_p2p_disabled_value)
    }
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
  }

  fun handleWifiP2pEnabled() {
    val message = "Wifi P2P: Enabled"
    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    findViewById<TextView>(R.id.wifi_p2p_enabled_value).apply {
      text = getString(R.string.wifi_p2p_enabled_value)
    }
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
  }

  fun handleUnexpectedWifiP2pState(wifiState: Int) {
    val message = "Wifi P2P: Unexpected state: $wifiState"
    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    findViewById<TextView>(R.id.wifi_p2p_enabled_value).apply {
      text = getString(R.string.wifi_p2p_unexpected_state, wifiState)
    }
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
  }

  fun handleWifiP2pDevice(device: WifiP2pDevice) {
    val message = "Wifi P2P: Device: $device"
    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    findViewById<TextView>(R.id.wifi_p2p_device_value).apply {
      text =
        getString(
          R.string.wifi_p2p_device_value,
          device.deviceAddress,
          device.deviceName,
          device.primaryDeviceType,
          device.secondaryDeviceType,
          device.status
        )
    }
    println(message)
  }

  fun handleNetworkChanged(network: NetworkInfo) {
    val message = "Wifi P2P: $network"
    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
  }

  private fun initiatePeerDiscovery() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      // TODO: Consider calling
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
      handleAccessFineLocationNotGranted()
      return
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
    val message = "Peer discovery initiated"
    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
  }

  private fun handleP2pDiscoverySuccess() {
    val message = "Wifi P2P: Peer discovery succeeded"
    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
  }

  private fun handleP2pDiscoveryFailure(reason: Int) {
    val message = "Wifi P2P: Peer discovery failed: $reason"
    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
  }

  fun handleP2pDiscoveryStarted() {
    val message = "Wifi P2P: Discovery started"
    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
  }

  fun handleP2pDiscoveryStopped() {
    val message = "Wifi P2P: Discovery stopped"
    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
  }

  fun handleUnexpectedWifiP2pDiscoveryState(discoveryState: Int) {
    val message = "Wifi P2P: Unexpected discovery state: $discoveryState"
    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
  }

  fun handleP2pPeersChanged(peerDeviceList: WifiP2pDeviceList) {
    val message = "Wifi P2P: Peers: ${peerDeviceList.deviceList}"
    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
  }

  fun handleAccessFineLocationNotGranted() {
    val message = "Wifi P2P: Access fine location permission not granted"
    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
  }

  fun handleMinimumSDKVersionNotMet() {
    val message = "Wifi P2P: Minimum SDK Version not met: ${Build.VERSION_CODES.Q}"
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
  }

  override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
    val message = "Connection info available: groupFormed = ${info.groupFormed}, isGroupOwner = ${info.isGroupOwner}"
    Log.d("Wifi P2P: ${this::class.simpleName}", message)
    findViewById<TextView>(R.id.wifi_p2p_group_formed_value).apply {
      text = resources.getString(if (info.groupFormed) R.string.wifi_p2p_value_yes else R.string.wifi_p2p_value_no)
    }
    findViewById<TextView>(R.id.wifi_p2p_group_owner_value).apply {
      text = resources.getString(if (info.isGroupOwner) R.string.wifi_p2p_value_yes else R.string.wifi_p2p_value_no)
    }
    findViewById<TextView>(R.id.wifi_p2p_group_owner_address_value).apply {
      text = if (info.groupOwnerAddress == null) resources.getString(R.string.wifi_p2p_group_owner_value_na) else info.groupOwnerAddress.hostAddress
    }
    if (info.groupFormed) {
      if (info.isGroupOwner) {
        // TODO: Start server.
      } else {
        // TODO: Connect to server.
      }
    }
  }
}
