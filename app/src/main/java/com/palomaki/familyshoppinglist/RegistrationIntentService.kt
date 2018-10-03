package com.palomaki.familyshoppinglist

import android.app.IntentService
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log

//import com.google.android.gms.gcm.GoogleCloudMessaging;
//import com.google.android.gms.iid.InstanceID;
import com.google.firebase.iid.FirebaseInstanceId
import com.microsoft.windowsazure.messaging.NotificationHub

class RegistrationIntentService : IntentService(TAG) {

    private val hub: NotificationHub? = null


    override fun onHandleIntent(intent: Intent?) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        var resultString: String? = null
        var regID: String? = null
        var storedToken: String? = null

        try {
            val FCM_token = FirebaseInstanceId.getInstance().token
            if (FCM_token == null) {
                return
            }
            //Log.d(TAG, "FCM Registration Token: " + FCM_token!!)

            // Storing the registration ID that indicates whether the generated token has been
            // sent to your server. If it is not stored, send the token to your server,
            // otherwise your server should have already received the token.
            regID = sharedPreferences.getString("registrationID", null)
            storedToken = sharedPreferences.getString("FCMtoken", "")
            if (regID == null) {

                val hub = NotificationHub(NotificationSettings.HubName,
                        NotificationSettings.HubListenConnectionString, this)
                Log.d(TAG, "Attempting a new registration with NH using FCM token : $FCM_token")
                regID = hub.register(FCM_token).registrationId

                // If you want to use tags...
                // Refer to : https://azure.microsoft.com/documentation/articles/notification-hubs-routing-tag-expressions/
                // regID = hub.register(token, "tag1,tag2").getRegistrationId();

                resultString = "New NH Registration Successfully - RegId : " + regID!!
                Log.d(TAG, resultString)

                sharedPreferences.edit().putString("registrationID", regID).apply()
                sharedPreferences.edit().putString("FCMtoken", FCM_token).apply()
            } else if (storedToken !== FCM_token) {

                val hub = NotificationHub(NotificationSettings.HubName,
                        NotificationSettings.HubListenConnectionString, this)
                Log.d(TAG, "NH Registration refreshing with token : $FCM_token")
                regID = hub.register(FCM_token).registrationId

                // If you want to use tags...
                // Refer to : https://azure.microsoft.com/documentation/articles/notification-hubs-routing-tag-expressions/
                // regID = hub.register(token, "tag1,tag2").getRegistrationId();

                resultString = "New NH Registration Successfully - RegId : " + regID!!
                Log.d(TAG, resultString)

                sharedPreferences.edit().putString("registrationID", regID).apply()
                sharedPreferences.edit().putString("FCMtoken", FCM_token).apply()
            } else {
                resultString = "Previously Registered Successfully - RegId : " + regID!!
            }// Check if the token may have been compromised and needs refreshing.
        } catch (e: Exception) {
            resultString = "Failed to complete registration"
            Log.e(TAG, resultString, e)
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
        }

        // Notify UI that registration has completed.
        //        if (ToDoActivity.myHAndleisVisible) {
        //          ToDoActivity.mainActivity.ToastNotify(resultString);
        //       }
    }

    companion object {

        private val TAG = "RegistrationIntentService"
    }
}