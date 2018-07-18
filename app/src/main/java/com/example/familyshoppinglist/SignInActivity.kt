package com.example.familyshoppinglist


import java.net.MalformedURLException
import java.util.HashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

import android.app.Activity
import android.app.AlertDialog
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.microsoft.windowsazure.mobileservices.MobileServiceClient
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback
import com.microsoft.windowsazure.mobileservices.http.OkHttpClientFactory
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable
import com.microsoft.windowsazure.mobileservices.table.query.Query
import com.microsoft.windowsazure.mobileservices.table.query.QueryOperations
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncContext
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.ColumnDataType
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.MobileServiceLocalStoreException
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.SQLiteLocalStore
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.SimpleSyncHandler
import com.squareup.okhttp.OkHttpClient
import com.microsoft.windowsazure.mobileservices.table.query.QueryOperations.*

import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.widget.Toast


import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;

class SignInActivity : Activity() {




    /**
     * Initializes the activity
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in)


    }

    /**
     * Initializes the activity menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }


    fun signIn(view: View) {
        val intent = Intent(this, ToDoActivity::class.java)
        startActivity(intent)
        finish()

    }



/*
    private fun authenticate() {
        // We first try to load a token cache if one exists.
        if (loadUserTokenCache(mClient)) {
            createTable()
        } else {
            // Sign in using the Google provider.
            mClient!!.login(MobileServiceAuthenticationProvider.Google, "familyshoppinglist", GOOGLE_LOGIN_REQUEST_CODE)
        }// If we failed to load a token cache, sign in and create a token cache
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // When request completes
        if (resultCode == RESULT_OK) {
            // Check the request code matches the one we send in the sign-in request
            if (requestCode == GOOGLE_LOGIN_REQUEST_CODE) {
                val result = mClient!!.onActivityResult(data)
                if (result.isLoggedIn) {
                    // sign-in succeeded
                    //createAndShowDialog(String.format("You are now signed in - %1$2s", mClient!!.currentUser.userId), "Success")
                    Toast.makeText(this@SignInActivity, "Login succeeded!", Toast.LENGTH_LONG).show()
                    cacheUserToken(mClient!!.currentUser)
                    createTable()
                } else {
                    // sign-in failed, check the error message
                    val errorMessage = result.errorMessage
                    createAndShowDialog(errorMessage, "Erroria")
                }
            }
        }
    }

    companion object {


        // You can choose any unique number here to differentiate auth providers from each other. Note this is the same code at login() and onActivityResult().
        val GOOGLE_LOGIN_REQUEST_CODE = 1


        val SHAREDPREFFILE = "temp"
        val USERIDPREF = "uid"
        val TOKENPREF = "tkn"
    }*/


}