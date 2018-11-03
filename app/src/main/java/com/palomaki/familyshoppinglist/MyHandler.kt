package com.palomaki.familyshoppinglist

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.util.Base64
import android.widget.EditText
import com.microsoft.windowsazure.notifications.NotificationsHandler
import com.microsoft.windowsazure.messaging.NotificationHub

import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class MyHandler(private var ctx: Context?) : NotificationsHandler() {
    private var mNotificationManager: NotificationManager? = null
    private var mBuilder: NotificationCompat.Builder? = null

    private var HubEndpoint: String? = null
    private var HubSasKeyName: String? = null
    private var HubSasKeyValue: String? = null


    private val hub: NotificationHub? = null

    var todoActivity: ToDoActivity? = null

    override fun onReceive(context: Context, bundle: Bundle) {
        ctx = context
        val nhMessage = bundle.getString("message")
        sendNotification(nhMessage)
        if (isVisible!!) {
            todoActivity!!.toastNotify(nhMessage!!, true)
        }
        super.onReceive(context, bundle)
    }


    private fun sendNotification(msg: String?) {

        val intent = Intent(ctx, ToDoActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        mNotificationManager = ctx!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = PendingIntent.getActivity(ctx, 0,
                intent, PendingIntent.FLAG_ONE_SHOT)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        mBuilder = NotificationCompat.Builder(ctx)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Notification Hub Demo")
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(msg))
                .setSound(defaultSoundUri)
                .setContentText(msg)

        mBuilder!!.setContentIntent(contentIntent)
        mNotificationManager!!.notify(NOTIFICATION_ID, mBuilder!!.build())

        todoActivity!!.toastNotify(msg!!, true)
    }


    private fun generateSasToken(uri: String): String? {

        val targetUri: String
        var token: String? = null
        try {
            targetUri = URLEncoder
                    .encode(uri.toLowerCase(), "UTF-8")
                    .toLowerCase()

            var expiresOnDate = System.currentTimeMillis()
            val expiresInMins = 60 // 1 hour
            expiresOnDate += (expiresInMins * 60 * 1000).toLong()
            val expires = expiresOnDate / 1000
            val toSign = targetUri + "\n" + expires

            // Get an hmac_sha1 key from the raw key bytes
            val keyBytes = HubSasKeyValue!!.toByteArray(charset("UTF-8"))
            val signingKey = SecretKeySpec(keyBytes, "HmacSHA256")

            // Get an hmac_sha1 Mac instance and initialize with the signing key
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(signingKey)

            // Compute the hmac on input data bytes
            val rawHmac = mac.doFinal(toSign.toByteArray(charset("UTF-8")))

            // Using android.util.Base64 for Android Studio instead of
            // Apache commons codec
            val signature = URLEncoder.encode(
                    Base64.encodeToString(rawHmac, Base64.NO_WRAP).toString(), "UTF-8")

            // Construct authorization string
            token = ("SharedAccessSignature sr=" + targetUri + "&sig="
                    + signature + "&se=" + expires + "&skn=" + HubSasKeyName)
        } catch (e: Exception) {
            if (isVisible!!) {
                todoActivity!!.toastNotify("Exception Generating SaS : " + e.message.toString(), true)
            }
        }

        return token
    }


    /**
     * Send Notification button click handler. This method parses the
     * DefaultFullSharedAccess connection string and generates a SaS token. The
     * token is added to the Authorization header on the POST request to the
     * notification hub. The text in the editTextNotificationMessage control
     * is added as the JSON body for the request to add a GCM message to the hub.
     *
     */
    fun sendNotificationButtonOnClick() {
        val notificationText = todoActivity!!.findViewById<EditText>(R.id.editTextNotificationMessage)
        val json = "{\"data\":{\"message\":\"" + notificationText.text.toString() + "\"}}"
        sendNotification(notificationText.text.toString())

        object : Thread() {
            override fun run() {
                try {
                    // Based on reference documentation...
                    // http://msdn.microsoft.com/library/azure/dn223273.aspx
                    parseConnectionString()
                    val url = URL(HubEndpoint + NotificationSettings.HubName +
                            "/messages/?api-version=2015-01")

                    val urlConnection = url.openConnection() as HttpURLConnection

                    try {
                        // POST request
                        urlConnection.doOutput = true

                        // Authenticate the POST request with the SaS token
                        urlConnection.setRequestProperty("Authorization",
                                generateSasToken(url.toString()))

                        // Notification format should be GCM
                        urlConnection.setRequestProperty("ServiceBusNotification-Format", "gcm")

                        // Include any tags
                        // Example below targets 3 specific tags
                        // Refer to : https://azure.microsoft.com/en-us/documentation/articles/notification-hubs-routing-tag-expressions/
                        // urlConnection.setRequestProperty("ServiceBusNotification-Tags",
                        //        "tag1 || tag2 || tag3");

                        // Send notification message
                        urlConnection.setFixedLengthStreamingMode(json.length)
                        val bodyStream = BufferedOutputStream(urlConnection.outputStream)
                        bodyStream.write(json.toByteArray())
                        bodyStream.close()

                        // Get reponse
                        urlConnection.connect()
                        val responseCode = urlConnection.responseCode
                        if (responseCode != 200 && responseCode != 201) {
                            val br = BufferedReader(InputStreamReader(urlConnection.errorStream))
                            var line: String
                            val builder = StringBuilder("Send Notification returned " +
                                    responseCode + " : ")
                            line = br.readLine()
                            while (line != null) {
                                builder.append(line)
                                line = br.readLine()
                            }

                            todoActivity!!.toastNotify(builder.toString(), true)
                        }
                    } finally {
                        urlConnection.disconnect()
                    }
                } catch (e: Exception) {
                    if (isVisible!!) {
                        todoActivity!!.toastNotify("Exception Sending Notification : " + e.message.toString(), true)
                    }
                }

            }
        }.start()


    }

    /**
     * Example code from http://msdn.microsoft.com/library/azure/dn495627.aspx
     * to parse the connection string so a SaS authentication token can be
     * constructed.
     *
     */
    private fun parseConnectionString() {
        val connectionString = NotificationSettings.HubFullAccess
        val parts = connectionString.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size != 3)
            throw RuntimeException("Error parsing connection string: $connectionString")

        for (i in parts.indices) {
            if (parts[i].startsWith("Endpoint")) {
                HubEndpoint = "https" + parts[i].substring(11)
            } else if (parts[i].startsWith("SharedAccessKeyName")) {
                HubSasKeyName = parts[i].substring(20)
            } else if (parts[i].startsWith("SharedAccessKey")) {
                HubSasKeyValue = parts[i].substring(16)
            }
        }
    }

    companion object {
        val NOTIFICATION_ID = 1
        var isVisible: Boolean? = false
    }

}