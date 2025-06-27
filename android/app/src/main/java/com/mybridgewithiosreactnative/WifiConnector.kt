package com.mybridgewithiosreactnative


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = WifiConnector.NAME)
class WifiConnector(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    companion object {
        const val NAME = "WifiConnector"
    }

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun getName(): String = NAME

    @ReactMethod
    fun connectToWifi(ssid: String, password: String, isWEP: Boolean, promise: Promise) {
        if (!hasRequiredPermissions(promise)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectWithSpecifier(ssid, password, promise)
        } else {
            connectLegacy(ssid, password, isWEP, promise)
        }
    }

    private fun hasRequiredPermissions(promise: Promise): Boolean {
        val context = reactApplicationContext
        val hasChangeNetwork = context.checkSelfPermission(Manifest.permission.CHANGE_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
        val canWriteSettings = Settings.System.canWrite(context)

        if (!hasChangeNetwork) {
            promise.reject("PERMISSION_DENIED", "CHANGE_NETWORK_STATE permission not granted")
            return false
        }

        if (!canWriteSettings) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(NAME, "Unable to launch WRITE_SETTINGS screen", e)
            }
            promise.reject("WRITE_SETTINGS_REQUIRED", "WRITE_SETTINGS permission not granted. Please enable it in settings.")
            return false
        }

        return true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connectWithSpecifier(ssid: String, password: String, promise: Promise) {
        val builder = WifiNetworkSpecifier.Builder().setSsid(ssid)
        if (password.isNotEmpty()) builder.setWpa2Passphrase(password)

        val networkSpecifier = builder.build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(networkSpecifier)
            .build()

        val connectivityManager = reactApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                connectivityManager.bindProcessToNetwork(network)

                // Optional: Check actual connectivity
                Thread {
                    try {
                        val socket = network.socketFactory.createSocket("8.8.8.8", 53)
                        socket.close()
                        promise.resolve("Connected to $ssid (App-Local)")
                    } catch (e: Exception) {
                        promise.reject("NO_INTERNET", "Connected but no internet access.")
                    }
                }.start()
            }

            override fun onUnavailable() {
                super.onUnavailable()
                promise.reject("CONNECTION_FAILED", "Could not connect to $ssid")
            }
        }

        connectivityManager.requestNetwork(request, networkCallback!!)
    }

    private fun connectLegacy(ssid: String, password: String, isWEP: Boolean, promise: Promise) {
        try {
            val wifiManager = reactApplicationContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!wifiManager.isWifiEnabled) wifiManager.isWifiEnabled = true

            val config = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                if (password.isEmpty()) {
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                } else if (isWEP) {
                    wepKeys[0] = "\"$password\""
                    wepTxKeyIndex = 0
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                    allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                } else {
                    preSharedKey = "\"$password\""
                }
            }

            val netId = wifiManager.addNetwork(config)
            if (netId == -1) {
                promise.reject("ADD_NETWORK_FAILED", "Failed to add network config")
                return
            }

            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()

            promise.resolve("Connected to $ssid (Legacy)")
        } catch (e: Exception) {
            promise.reject("LEGACY_ERROR", e.message)
        }
    }

    override fun invalidate() {
        networkCallback?.let {
            val cm = reactApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }
}

//
//import android.content.Context
//import android.net.*
//import android.net.wifi.WifiConfiguration
//import android.net.wifi.WifiManager
//import android.net.wifi.WifiNetworkSpecifier
//import android.os.Build
//import android.util.Log
//import androidx.annotation.RequiresApi
//import com.facebook.react.bridge.*
//import java.lang.Exception
//
//class WifiConnector(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
//    private var networkCallback: ConnectivityManager.NetworkCallback? = null
//
//    override fun getName(): String = "WifiConnector"
//
//    @ReactMethod
//    fun connectToWifi(ssid: String, password: String, isWEP: Boolean, promise: Promise) {
//        Log.d("TAG", "wifi trying to connect with: ");
//        Log.d("TAG", "ssid: $ssid pass :$password");
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            connectWithSpecifier(ssid, password, promise)
//        } else {
//            connectLegacy(ssid, password, isWEP, promise)
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun connectWithSpecifier(ssid: String, password: String, promise: Promise) {
//        val specifierBuilder = WifiNetworkSpecifier.Builder().setSsid(ssid)
//        if (password.isNotEmpty()) {
//            specifierBuilder.setWpa2Passphrase(password)
//        }
//
//        val wifiNetworkSpecifier = specifierBuilder.build()
//        val networkRequest = NetworkRequest.Builder()
//            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//            .setNetworkSpecifier(wifiNetworkSpecifier)
//            .build()
//
//        val connectivityManager =
//            reactApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//
//        networkCallback = object : ConnectivityManager.NetworkCallback() {
//            override fun onAvailable(network: Network) {
//                super.onAvailable(network)
//                connectivityManager.bindProcessToNetwork(network)
//                promise.resolve("Connected to $ssid")
//            }
//
//            override fun onUnavailable() {
//                super.onUnavailable()
//                promise.reject("UNAVAILABLE", "Could not connect to $ssid")
//            }
//        }
//
//        try {
//            connectivityManager.requestNetwork(networkRequest, networkCallback!!)
//        } catch (e: Exception) {
//            promise.reject("ERROR", "Failed to request network: ${e.message}")
//        }
//    }
//
//    private fun connectLegacy(ssid: String, password: String, isWEP: Boolean, promise: Promise) {
//        try {
//            val wifiManager = reactApplicationContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//            if (!wifiManager.isWifiEnabled) {
//                wifiManager.isWifiEnabled = true
//            }
//
//            val wifiConfig = WifiConfiguration().apply {
//                SSID = "\"$ssid\""
//                if (password.isEmpty()) {
//                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
//                } else {
//                    if (isWEP) {
//                        wepKeys[0] = "\"$password\""
//                        wepTxKeyIndex = 0
//                        allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
//                        allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
//                    } else {
//                        preSharedKey = "\"$password\""
//                    }
//                }
//            }
//
//            val netId = wifiManager.addNetwork(wifiConfig)
//            if (netId == -1) {
//                promise.reject("ADD_NETWORK_FAILED", "Failed to add network config for $ssid")
//                return
//            }
//
//            wifiManager.disconnect()
//            val enabled = wifiManager.enableNetwork(netId, true)
//            wifiManager.reconnect()
//
//            if (enabled) {
//                promise.resolve("Connected to $ssid")
//            } else {
//                promise.reject("ENABLE_FAILED", "Failed to enable network $ssid")
//            }
//
//        } catch (e: Exception) {
//            promise.reject("ERROR", "Failed to connect: ${e.message}")
//        }
//    }
//
//    override fun invalidate() {
//        networkCallback?.let {
//            val cm = reactApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//            cm.unregisterNetworkCallback(it)
//            networkCallback = null
//        }
//    }
//}
//

//
//class WifiConnector {
//}