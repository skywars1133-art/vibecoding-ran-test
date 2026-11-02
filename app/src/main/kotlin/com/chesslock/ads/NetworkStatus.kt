package com.chesslock.ads

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Lightweight connectivity check used to disable hints with an explanatory note while offline. */
object NetworkStatus {
    fun isOnline(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
