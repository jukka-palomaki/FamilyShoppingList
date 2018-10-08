package com.palomaki.familyshoppinglist


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

import android.widget.EditText;

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.microsoft.windowsazure.mobileservices.MobileServiceClient
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.ColumnDataType
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.MobileServiceLocalStoreException
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.SQLiteLocalStore
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.SimpleSyncHandler
import com.squareup.okhttp.OkHttpClient
import com.microsoft.windowsazure.mobileservices.table.query.QueryOperations.*

import android.content.Context
import android.content.Intent
import android.widget.*

import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.microsoft.windowsazure.notifications.NotificationsManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging


class ToDoActivity : Activity() {


    //var todoActivity: ToDoActivity? = null
    //var static isVisible: Boolean? = false
    //private val GoogleCloudMessaging gcm
    private val PLAY_SERVICES_RESOLUTION_REQUEST = 9000

    private val TAG = "ToDoActivity"





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

    private var mLogInOutButton: Button? = null

    private var mAddButton: Button? = null

    private var userId = ""

    private var myHandler: MyHandler? = null


    /**
     * Initializes the activity
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_to_do)

        myHandler = MyHandler(this)
        myHandler?.todoActivity = this
        NotificationsManager.handleNotifications(this, NotificationSettings.SenderId, MyHandler::class.java)
        registerWithNotificationHubs()

        mProgressBar = findViewById(R.id.loadingProgressBar) as ProgressBar
        mLogInOutButton = findViewById(R.id.buttonLogInOut) as Button
        mAddButton = findViewById(R.id.buttonAddToDo) as Button

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

            // Load the items from the Mobile Service
            refreshItemsFromTable()

        } catch (e: MalformedURLException) {
            createAndShowDialog(Exception("There was an error creating the Mobile Service. Verify the URL"), "Error")
        } catch (e: Exception) {
            createAndShowDialog(e, "Error onCreate")
        }

        /*

        FirebaseMessaging.getInstance().subscribeToTopic("firstFamily").addOnCompleteListener(object : OnCompleteListener<Void> {
            override fun onComplete(task: Task<Void>) {
                var msg = "Subscribed"//getString(R.string.msg_subscribed);
                if (!task.isSuccessful) {
                    msg = "Subscription failed"//getString(R.string.msg_subscribe_failed);
                }
                Log.d(TAG, msg)
                ToastNotify(msg, false)
            }
        })

*/

    }

    override fun onStart() {
        super.onStart()
        MyHandler.isVisible = true
    }

    override fun onPause() {
        super.onPause()
        MyHandler.isVisible = false
    }

    override fun onResume() {
        super.onResume()
        MyHandler.isVisible = true
    }

    override fun onStop() {
        super.onStop()
        MyHandler.isVisible = false
    }


    fun ToastNotify(notificationMessage: String, long: Boolean) {
        runOnUiThread {
            if (long) {
                Toast.makeText(this@ToDoActivity, notificationMessage, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@ToDoActivity, notificationMessage, Toast.LENGTH_SHORT).show()
            }
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

    fun sendNotificationButtonOnClick(view: View) {
        myHandler?.sendNotificationButtonOnClick()
    }


    /**
     * Mark an item as completed
     *
     * @param item
     * The item to mark
     */
    fun updateItem(item: ToDoItem) {
        if (mClient == null) {
            return
        }

        // Set the item as completed and update it in the table
        //item.isComplete = true

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

        //val prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE)

        item.userId = userId //prefs.getString(USERIDPREF, null)


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
        //myHandler?.sendNotificationButtonOnClick()
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

        val rows = mToDoTable.
                where().field("complete").eq(`val`(false)).and().field("userId").eq(`val`(GoogleLoginSettings.sid1))
                .or().field("complete").eq(`val`(false)).and().field("userId").eq(`val`(GoogleLoginSettings.sid2))
                .or().field("complete").eq(`val`(false)).and().field("userId").eq(`val`(GoogleLoginSettings.sid3))
                .execute().get()

        val rowsSorted = rows.sortedBy { it.text.trim() }
        return rowsSorted
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


    private fun cacheUserToken(user: MobileServiceUser) {
        val prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(USERIDPREF, user.userId)
        userId = user.userId
        editor.putString(TOKENPREF, user.authenticationToken)
        editor.commit()
    }

    private fun loadUserTokenCache(client: MobileServiceClient?): Boolean {
        val prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE)
        userId = prefs.getString(USERIDPREF, "") ?: return false
        val token = prefs.getString(TOKENPREF, null) ?: return false

        val user = MobileServiceUser(userId)
        user.authenticationToken = token
        client!!.currentUser = user

        return true
    }


    fun loginLogout(view: View) {

        if (view is Button && !mClient.isLoginInProgress) {
            if (mAdapter!!.isEmpty()) {
                // Sign in using the Google provider.
                mClient.login(MobileServiceAuthenticationProvider.Google, "familyshoppinglist", GOOGLE_LOGIN_REQUEST_CODE)
                mAddButton!!.isEnabled = true
            } else {
                val prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE)
                userId = prefs.getString(USERIDPREF, "") ?: return
                val user = MobileServiceUser(userId)
                user.authenticationToken = null
                cacheUserToken(user)
                mClient.logout()
                mAdapter!!.clear()
                ToastNotify("Logged out", false)
                mAddButton!!.isEnabled = false

            }
        }
    }

    private fun authenticate() {
        // We first try to load a token cache if one exists.
        if (loadUserTokenCache(mClient)) {
            createTable()
        } else {
            //Else we show an empty table with login button
            if (mAdapter != null) {
                mAdapter!!.clear()
            }
        }
    }


    private fun isAllowedUser(sid: String): Boolean {
        when (sid) {
            GoogleLoginSettings.sid1, GoogleLoginSettings.sid2, GoogleLoginSettings.sid3 -> return true
            else -> return false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // When request completes
        if (resultCode == RESULT_OK) {
            // Check the request code matches the one we send in the sign-in request
            if (requestCode == GOOGLE_LOGIN_REQUEST_CODE) {
                val result = mClient.onActivityResult(data)
                if (result.isLoggedIn && isAllowedUser(mClient.currentUser.userId)) {
                    // sign-in succeeded
                    ToastNotify("Login succeeded!", false)
                    cacheUserToken(mClient.currentUser)
                    createTable()
                } else {
                    // sign-in failed, check the error message
                    mClient.logout()
                    var errorMessage = result.errorMessage
                    if (errorMessage == null) {
                        errorMessage = "Not allowed user"
                    }
                    createAndShowDialog(errorMessage, "Erroria")
                }
            }
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show()
            } else {
                Log.i(TAG, "This device is not supported by Google Play Services.")
                ToastNotify("This device is not supported by Google Play Services", true)
                finish()
            }
            return false
        }
        //ToastNotify("Google Play Services ok", false)
        return true
    }



    fun registerWithNotificationHubs() {
        if (checkPlayServices()) {
            // Start IntentService to register this application with FCM.
            val intent = Intent(this, RegistrationIntentService::class.java)
            startService(intent)
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