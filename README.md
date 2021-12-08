# A Prototype Implementation of the P2P Sync Transport using Wifi Direct

This repository contains a prototype of the [P2P Sync Transport](https://github.com/opensrp/fhircore/discussions/691) using Wifi Direct.
The intention of this work is to demonstrate the use of the [Android Wifi Direct APIs](https://developer.android.com/guide/topics/connectivity/wifip2p) to exchange data between two devices.
Please note that this is a prototype and NOT READY FOR PRODUCTION use.

## WifiP2pManager

The key class from the Android SDK is the [WifiP2pManager](https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager). 
This class enables an application to request relevant information, register for relevant information and initiate relevant actions related to establish a wifi direct connection to another device.

## WifiP2pBroadcastReceiver

The key class in this prototype is the [WifiP2pBroadcastReceiver](app/src/main/java/com/trevorgowing/android/p2p/WifiP2pBroadcastReceiver.kt).
The class receives the relevant intents containing information of interest related to establishing a connection to another device via wifi direct.
This class also uses the [WifiP2pManager](https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager) to request the relevant information at the relvant time.

An application is required to register to receive the relevant intents.
In this prototype this is achieved by registering an `IntentFilter` with the `Context` in `MainActivity.onResume()`.

```kotlin
class MainActivity : AppCompatActivity() {

  private val wifiP2pManager: WifiP2pManager by
      lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
      }
  private var wifiP2pChannel: WifiP2pManager.Channel? = null
  private var wifiP2pReceiver: BroadcastReceiver? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    wifiP2pChannel = wifiP2pManager.initialize(this, mainLooper, null)
    wifiP2pChannel?.also { channel ->
      wifiP2pReceiver = WifiP2pBroadcastReceiver(wifiP2pManager, channel, this)
    }
  }
  
  override fun onResume() {
    super.onResume()
    listenForWifiP2pIntents()
  }

  override fun onPause() {
    super.onPause()
    wifiP2pReceiver?.also { unregisterReceiver(it) }
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
          })
    }
  }
}
```

## Gotchas

There are a few challenges that were experienced during the development of this prototype.

### ACCESS_FINE_LOCATION Permission

The Wifi Direct APIs required the [ACCESS_FINE_LOCATION](https://developer.android.com/reference/android/Manifest.permission#ACCESS_FINE_LOCATION) permission which is a dangerous level permission.
This means that this permission must be requested just in time by the application, in newer version of the Android SDK.
In this prototype the permission is requested in the `MainActivity.onCreate` as well as prior to each sensitive API request.

```kotlin
class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestAccessFineLocationIfNotGranted()
    } else {
      handleMinimumSDKVersionNotMet(Build.VERSION_CODES.M)
    }
  }
  
  @RequiresApi(Build.VERSION_CODES.M)
  private fun requestAccessFineLocationIfNotGranted() {
    when (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
      PackageManager.PERMISSION_GRANTED -> logDebug("Wifi P2P: Access fine location granted")
      else -> {
        logDebug("Wifi P2P: Requesting access fine location permission")
        return requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            accessFineLocationPermissionRequestInt)
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
}
```
