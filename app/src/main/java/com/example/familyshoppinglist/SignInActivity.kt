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
import com.example.familyshoppinglist.ToDoActivity.Companion.extraFromSignIn


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
        intent.putExtra(extraFromSignIn, true)
        startActivity(intent)
        finish()

    }


}