package nl.julian.aerial.data

import android.os.Build
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import android.os.StatFs
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetKind { Ethernet, WiFi, Other, None }

fun networkFlow(context: Context): Flow<NetKind> = callbackFlow {
    val cm = context.getSystemService(ConnectivityManager::class.java)
    fun current(): NetKind {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return NetKind.None
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetKind.Ethernet
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetKind.WiFi
            else -> NetKind.Other
        }
    }
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(current()) }
        override fun onLost(network: Network) { trySend(current()) }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(current()) }
    }
    trySend(current())
    cm.registerDefaultNetworkCallback(callback)
    awaitClose { cm.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()

/** null = the device has no Bluetooth radio. */
fun bluetoothOnFlow(context: Context): Flow<Boolean?> = callbackFlow {
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    if (adapter == null) {
        trySend(null)
        awaitClose { }
        return@callbackFlow
    }
    trySend(runCatching { adapter.isEnabled }.getOrDefault(false))
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val state = i.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            trySend(state == BluetoothAdapter.STATE_ON)
        }
    }
    ContextCompat.registerReceiver(
        context, receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
        ContextCompat.RECEIVER_EXPORTED,
    )
    awaitClose { context.unregisterReceiver(receiver) }
}.distinctUntilChanged()

/** True when less than 10% of the TV's app storage is free; checked every 10 minutes. */
fun lowStorageFlow(context: Context): Flow<Boolean> = flow {
    while (true) {
        val stat = StatFs(context.filesDir.absolutePath)
        emit(stat.availableBytes < stat.totalBytes / 10)
        delay(600_000)
    }
}.distinctUntilChanged().flowOn(Dispatchers.IO)

/** Wi-Fi signal as 1 to 4 bars, or null when not on Wi-Fi or the TV doesn't report it (needs Android 10). */
fun wifiBarsFlow(context: Context): Flow<Int?> = callbackFlow {
    val cm = context.getSystemService(ConnectivityManager::class.java)
    fun bars(caps: NetworkCapabilities?): Int? {
        if (Build.VERSION.SDK_INT < 29 || caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        val rssi = caps.signalStrength
        if (rssi == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED) return null
        return when {
            rssi >= -55 -> 4
            rssi >= -67 -> 3
            rssi >= -78 -> 2
            else -> 1
        }
    }
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(bars(caps)) }
        override fun onLost(network: Network) { trySend(null) }
    }
    trySend(bars(cm.getNetworkCapabilities(cm.activeNetwork)))
    cm.registerDefaultNetworkCallback(callback)
    awaitClose { cm.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
