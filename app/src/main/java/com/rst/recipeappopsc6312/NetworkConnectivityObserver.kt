package com.rst.recipeappopsc6312

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class NetworkConnectivityObserver(context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val networkStatus: Flow<Boolean> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                launch { send(true) } // Network is available
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                launch { send(false) } // Network is lost
            }
        }

        // Register the callback
        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        // Initial check
        val isConnected = connectivityManager.activeNetwork != null
        launch { send(isConnected) }

        // Cleanup when the flow is cancelled
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged()
}
