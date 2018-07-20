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

class ToDoActivity : Activity() {

    /**
     * Mobile Service Client reference
     * Create the Mobile Service Client instance, using the provided
     * Mobile Service URL and key
     */
    private val mClient = MobileServiceClient(
    "https://familyshoppinglist.azurewebsites.net",
    this).withFilter(ProgressFilter())


    /**
     * Mobile Service Table used to access data
     */
    private var mToDoTable: MobileServiceTable<ToDoItem> = mClient.getTable(ToDoItem::class.java)

    //Offline Sync
    /**
     * Mobile Service Table used to access and Sync data
     */
    //private MobileServiceSyncTable<ToDoItem> mToDoTable;

    /**
     * Adapter to sync the items list with the view
     */
    private var mAdapter: ToDoItemAdapter? = null

    /**
     * EditText containing the "New To Do" text
     */
    private var mTextNewToDo: EditText? = null

    /**
     * Progress spinner to use for table operations
     */
    private var mProgressBar: ProgressBar? = null

    /**
     * Initializes the activity
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_to_do)

        mProgressBar = findViewById(R.id.loadingProgressBar) as ProgressBar

        // Initialize the progress bar
        mProgressBar!!.visibility = ProgressBar.GONE

        try {


            // Extend timeout from default of 10s to 20s
            mClient.setAndroidHttpClientFactory {
                val client = OkHttpClient()
                client.setReadTimeout(20, TimeUnit.SECONDS)
                client.setWriteTimeout(20, TimeUnit.SECONDS)
                client
            }

            authenticate()

            // Get the Mobile Service Table instance to use

            //mToDoTable = mClient.getTable(ToDoItem::class.java)

            // Offline Sync
            //mToDoTable = mClient.getSyncTable("ToDoItem", ToDoItem.class);

            //Init local storage
            initLocalStore().get()

            mTextNewToDo = findViewById(R.id.textNewToDo) as EditText

            // Create an adapter to bind the items with the view
            mAdapter = ToDoItemAdapter(this, R.layout.row_list_to_do)
            val listViewToDo = findViewById(R.id.listViewToDo) as ListView
            listViewToDo.adapter = mAdapter

            // Load the items from the Mobile Service
            refreshItemsFromTable()

        } catch (e: MalformedURLException) {
            createAndShowDialog(Exception("There was an error creating the Mobile Service. Verify the URL"), "Error")
        } catch (e: Exception) {
            createAndShowDialog(e, "Error onCreate")
        }

    }

    /**
     * Initializes the activity menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    /**
     * Select an option from the menu
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_refresh) {
            refreshItemsFromTable()
        }

        return true
    }



    /**
     * Mark an item as completed
     *
     * @param item
     * The item to mark
     */
    fun checkItem(item: ToDoItem) {
        if (mClient == null) {
            return
        }

        // Set the item as completed and update it in the table
        item.isComplete = true

        val task = object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                try {

                    checkItemInTable(item)
                    runOnUiThread {
                        if (item.isComplete) {
                            mAdapter!!.remove(item)
                        }
                    }
                } catch (e: Exception) {
                    createAndShowDialogFromTask(e, "Error checkItem")
                }

                return null
            }
        }

        runAsyncTask(task)

    }

    /**
     * Mark an item as completed in the Mobile Service Table
     *
     * @param item
     * The item to mark
     */
    @Throws(ExecutionException::class, InterruptedException::class)
    fun checkItemInTable(item: ToDoItem) {
        mToDoTable.update(item).get()
    }

    /**
     * Add a new item
     *
     * @param view
     * The view that originated the call
     */
    fun addItem(view: View) {
        if (mClient == null) {
            return
        }

        // Create a new item
        val item = ToDoItem()

        item.text = mTextNewToDo!!.text.toString()
        if (item.text.trim().isEmpty()) {
            createAndShowDialog("Cannot add empty item","Error")
            return
        }
        item.isComplete = false

        val prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE)

        item.userId = prefs.getString(USERIDPREF, null)


        // Insert the new item
        val task = object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                try {
                    val entity = addItemInTable(item)

                    runOnUiThread {
                        if (!entity.isComplete) {
                            mAdapter!!.add(entity)
                        }
                    }
                } catch (e: Exception) {
                    createAndShowDialogFromTask(e, "Error addItem")
                }

                return null
            }
        }

        runAsyncTask(task)

        mTextNewToDo!!.setText("")
    }



    /**
     * Add an item to the Mobile Service Table
     *
     * @param item
     * The item to Add
     */
    @Throws(ExecutionException::class, InterruptedException::class)
    fun addItemInTable(item: ToDoItem): ToDoItem {
        return mToDoTable.insert(item).get()
    }

    /**
     * Refresh the list with the items in the Table
     */
    private fun refreshItemsFromTable() {

        // Get the items that weren't marked as completed and add them in the
        // adapter

        val task = object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {

                try {
                    val results = refreshItemsFromMobileServiceTable()

                    //Offline Sync
                    //final List<ToDoItem> results = refreshItemsFromMobileServiceTableSyncTable();

                    runOnUiThread {
                        mAdapter!!.clear()

                        for (item in results) {
                            mAdapter!!.add(item)
                        }
                    }
                } catch (e: Exception) {
                    createAndShowDialogFromTask(e, "Error refreshItemsFromTable")
                }

                return null
            }
        }

        runAsyncTask(task)
    }

    /**
     * Refresh the list with the items in the Mobile Service Table
     */

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun refreshItemsFromMobileServiceTable(): List<ToDoItem> {
        return mToDoTable.where().field("complete").eq(`val`(false)).execute().get()
    }

    //Offline Sync
    /**
     * Refresh the list with the items in the Mobile Service Sync Table
     */
    /*private List<ToDoItem> refreshItemsFromMobileServiceTableSyncTable() throws ExecutionException, InterruptedException {
        //sync the data
        sync().get();
        Query query = QueryOperations.field("complete").
                eq(val(false));
        return mToDoTable.read(query).get();
    }*/

    /**
     * Initialize local storage
     * @return
     * @throws MobileServiceLocalStoreException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Throws(MobileServiceLocalStoreException::class, ExecutionException::class, InterruptedException::class)
    private fun initLocalStore(): AsyncTask<Void, Void, Void> {

        val task = object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                try {

                    val syncContext = mClient.syncContext

                    if (syncContext.isInitialized)
                        return null

                    val localStore = SQLiteLocalStore(mClient.context, "OfflineStore", null, 1)

                    val tableDefinition = HashMap<String, ColumnDataType>()
                    tableDefinition["id"] = ColumnDataType.String
                    tableDefinition["text"] = ColumnDataType.String
                    tableDefinition["complete"] = ColumnDataType.Boolean

                    localStore.defineTable("ToDoItem", tableDefinition)

                    val handler = SimpleSyncHandler()

                    syncContext.initialize(localStore, handler).get()

                } catch (e: Exception) {
                    createAndShowDialogFromTask(e, "Error initLocalStore")
                }

                return null
            }
        }

        return runAsyncTask(task)
    }

    //Offline Sync
    /**
     * Sync the current context and the Mobile Service Sync Table
     * @return
     */
    /*
    private AsyncTask<Void, Void, Void> sync() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    MobileServiceSyncContext syncContext = mClient.getSyncContext();
                    syncContext.push().get();
                    mToDoTable.pull(null).get();
                } catch (final Exception e) {
                    createAndShowDialogFromTask(e, "Error AsyncTask");
                }
                return null;
            }
        };
        return runAsyncTask(task);
    }
    */

    /**
     * Creates a dialog and shows it
     *
     * @param exception
     * The exception to show in the dialog
     * @param title
     * The dialog title
     */
    private fun createAndShowDialogFromTask(exception: Exception, title: String) {
        runOnUiThread { createAndShowDialog(exception, "Error createAndShowDialogFromTask") }
    }


    /**
     * Creates a dialog and shows it
     *
     * @param exception
     * The exception to show in the dialog
     * @param title
     * The dialog title
     */
    private fun createAndShowDialog(exception: Exception, title: String) {
        var ex: Throwable = exception
        if (exception.cause != null) {
            ex = exception.cause!!
        }
        createAndShowDialog(ex.message!!, title)
    }

    /**
     * Creates a dialog and shows it
     *
     * @param message
     * The dialog message
     * @param title
     * The dialog title
     */
    private fun createAndShowDialog(message: String, title: String) {
        val builder = AlertDialog.Builder(this)

        builder.setMessage(message)
        builder.setTitle(title)
        builder.create().show()
    }

    /**
     * Run an ASync task on the corresponding executor
     * @param task
     * @return
     */
    private fun runAsyncTask(task: AsyncTask<Void, Void, Void>): AsyncTask<Void, Void, Void> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            task.execute()
        }
    }

    private inner class ProgressFilter : ServiceFilter {

        override fun handleRequest(request: ServiceFilterRequest, nextServiceFilterCallback: NextServiceFilterCallback): ListenableFuture<ServiceFilterResponse> {

            val resultFuture = SettableFuture.create<ServiceFilterResponse>()


            runOnUiThread { if (mProgressBar != null) mProgressBar!!.visibility = ProgressBar.VISIBLE }

            val future = nextServiceFilterCallback.onNext(request)

            Futures.addCallback(future, object : FutureCallback<ServiceFilterResponse> {
                override fun onFailure(e: Throwable) {
                    resultFuture.setException(e)
                }

                override fun onSuccess(response: ServiceFilterResponse?) {
                    runOnUiThread { if (mProgressBar != null) mProgressBar!!.visibility = ProgressBar.GONE }

                    resultFuture.set(response)
                }
            })

            return resultFuture
        }
    }


    private fun createTable() {

        // Get the table instance to use.
        mToDoTable = mClient.getTable(ToDoItem::class.java)

        mTextNewToDo = findViewById(R.id.textNewToDo) as EditText

        // Create an adapter to bind the items with the view.
        mAdapter = ToDoItemAdapter(this, R.layout.row_list_to_do)
        val listViewToDo = findViewById(R.id.listViewToDo) as ListView
        listViewToDo.adapter = mAdapter

        // Load the items from Azure.
        refreshItemsFromTable()
    }

    //val SHAREDPREFFILE = "temp"
    //val USERIDPREF = "uid"
    //val TOKENPREF = "tkn"

    private fun cacheUserToken(user: MobileServiceUser) {
        val prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(USERIDPREF, user.userId)
        editor.putString(TOKENPREF, user.authenticationToken)
        editor.commit()
    }

    private fun loadUserTokenCache(client: MobileServiceClient?): Boolean {
        val prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE)
        val userId = prefs.getString(USERIDPREF, null) ?: return false
        val token = prefs.getString(TOKENPREF, null) ?: return false

        val user = MobileServiceUser(userId)
        user.authenticationToken = token
        client!!.currentUser = user

        return true
    }



    fun logout(view: View) {
        mClient.logout()

        val prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE)
        val userId = prefs.getString(USERIDPREF, null) ?: return

        val user = MobileServiceUser(userId)
        user.authenticationToken = null

        cacheUserToken(user)

        mClient.login(MobileServiceAuthenticationProvider.Google, "familyshoppinglist", GOOGLE_LOGIN_REQUEST_CODE)
    }

    private fun authenticate() {
        // We first try to load a token cache if one exists.
        if (loadUserTokenCache(mClient)) {
            createTable()
        } else {
            // Sign in using the Google provider.
            mClient.login(MobileServiceAuthenticationProvider.Google, "familyshoppinglist", GOOGLE_LOGIN_REQUEST_CODE)
        }// If we failed to load a token cache, sign in and create a token cache
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // When request completes
        if (resultCode == RESULT_OK) {
            // Check the request code matches the one we send in the sign-in request
            if (requestCode == GOOGLE_LOGIN_REQUEST_CODE) {
                val result = mClient.onActivityResult(data)
                if (result.isLoggedIn) {
                    // sign-in succeeded
                    //createAndShowDialog(String.format("You are now signed in - %1$2s", mClient.currentUser.userId), "Success")
                    Toast.makeText(this@ToDoActivity, "Login succeeded!", Toast.LENGTH_LONG).show()
                    cacheUserToken(mClient.currentUser)
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
    }


}