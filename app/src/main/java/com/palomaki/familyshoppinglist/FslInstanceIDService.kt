package com.palomaki.familyshoppinglist

import android.content.Intent
import android.util.Log

import com.google.android.gms.iid.InstanceIDListenerService
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService

class FslInstanceIDService : InstanceIDListenerService() {

    override fun onTokenRefresh() {
        // Get updated InstanceID token.
        //String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        //Log.d(TAG, "Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        //sendRegistrationToServer(refreshedToken);

        Log.d(TAG, "Refreshing GCM Registration Token")

        val intent = Intent(this, RegistrationIntentService::class.java)
        startService(intent)

    }

    companion object {
        private val TAG = "FslInstanceIDService"
    }
}
