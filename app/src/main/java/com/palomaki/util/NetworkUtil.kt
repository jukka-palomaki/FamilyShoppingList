package com.palomaki.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

object NetworkUtil {

    fun isInternetOk(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting ?: false
    }
}