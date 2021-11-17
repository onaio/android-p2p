package com.trevorgowing.android.p2p

import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.trevorgowing.android.p2p.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

  private lateinit var appBarConfiguration: AppBarConfiguration
  private lateinit var binding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)

    val navController = findNavController(R.id.nav_host_fragment_content_main)
    appBarConfiguration = AppBarConfiguration(navController.graph)
    setupActionBarWithNavController(navController, appBarConfiguration)

    binding.fab.setOnClickListener { view ->
      Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        .setAction("Action", null).show()
    }
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
    TODO("Not yet implemented")
    println("Wifi P2P: Disabled")
  }

  fun handleWifiP2pEnabled() {
    TODO("Not yet implemented")
    println("Wifi P2P: Enabled")
  }

  fun handleUnexpectedWifiP2pState(wifiState: Int) {
    TODO("Not yet implemented")
    println("Wifi P2P: Unexpected state: $wifiState")
  }

  fun handleWifiP2pDevice(device: WifiP2pDevice) {
    TODO("Not yet implemented")
    println("Wifi P2P: Device: $device")
  }

  fun handleNetworkChanged(network: NetworkInfo) {
    TODO("Not yet implemented")
    println("Wifi P2P: $network")
  }

  fun handleP2pDiscoveryStarted() {
    TODO("Not yet implemented")
    println("Wifi P2P: Discovery started")
  }

  fun handleP2pDiscoveryStopped() {
    TODO("Not yet implemented")
    println("Wifi P2P: Discovery stopped")
  }

  fun handleUnexpectedWifiP2pDiscoveryState(discoveryState: Int) {
    TODO("Not yet implemented")
    println("Wifi P2P: Unexpected discovery state: $discoveryState")
  }

  fun handleP2pPeersChanged(peerDeviceList: WifiP2pDeviceList) {
    TODO("Not yet implemented")
    println("Wifi P2P: Peers changes")
  }

  fun handleAccessFineLocationNotGranted() {
    TODO("Not yet implemented")
    println("Wifi P2P: Access find location permission not granted")
  }

  fun handleMinimumSDKVersionNotMet() {
    TODO("Not yet implemented")
    println("Wifi P2P: Minimum SDK Version not met: ${Build.VERSION_CODES.Q}")
  }
}
