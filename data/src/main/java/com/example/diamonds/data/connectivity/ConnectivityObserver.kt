package com.example.diamonds.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.diamonds.common.util.ConnectivityState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Observes network connectivity changes
 */
class ConnectivityObserver(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observeConnectivityState(): Flow<ConnectivityState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val state = when {
                    capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true -> {
                        ConnectivityState.ONLINE
                    }
                    else -> ConnectivityState.METERED
                }
                trySend(state)
            }

            override fun onLost(network: Network) {
                trySend(ConnectivityState.OFFLINE)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val state = when {
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> {
                        ConnectivityState.ONLINE
                    }
                    else -> ConnectivityState.METERED
                }
                trySend(state)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)
        trySend(getCurrentConnectivityState())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getCurrentConnectivityState(): ConnectivityState {
        val network = connectivityManager.activeNetwork ?: return ConnectivityState.OFFLINE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectivityState.OFFLINE

        return when {
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> ConnectivityState.ONLINE
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> ConnectivityState.METERED
            else -> ConnectivityState.OFFLINE
        }
    }
}
