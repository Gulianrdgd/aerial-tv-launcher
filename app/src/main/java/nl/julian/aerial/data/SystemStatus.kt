package nl.julian.aerial.data

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
