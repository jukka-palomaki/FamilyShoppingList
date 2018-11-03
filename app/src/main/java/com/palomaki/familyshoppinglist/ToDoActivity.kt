package com.palomaki.familyshoppinglist


import java.net.MalformedURLException
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
import android.os.Handler
import android.support.v4.widget.SwipeRefreshLayout
import android.widget.*

import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser

import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Toast


class ToDoActivity : Activity() {

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


    /**
     * Adapter to sync the items list with the view
     */
    private lateinit var mAdapter: ToDoItemAdapter

    private lateinit var mTextNewToDo: EditText

    private lateinit var swipeLayout: SwipeRefreshLayout

    private lateinit var mAddButton: Button

    private var userId = ""

    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable


    private val refreshDelay = 500L

    /**
     * Initializes the activity
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_to_do)

        mAddButton = findViewById(R.id.buttonAddToDo) as Button

        try {

            // Extend timeout from default of 10s to 20s
            mClient.setAndroidHttpClientFactory {
                val client = OkHttpClient()
                client.setReadTimeout(20, TimeUnit.SECONDS)
                client.setWriteTimeout(20, TimeUnit.SECONDS)
                client
            }

            authenticate()

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


        findViewById<EditText>(R.id.textNewToDo).setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    addItem(v)
                    true
                }
                else -> false
            }
        }

        // Initialize the handler instance
        mHandler = Handler()

        // Set an on refresh listener for swipe refresh layout
        swipeLayout = findViewById(R.id.swipe_refresh_layout)
        swipeLayout.setOnRefreshListener {
            refreshItemsFromTable()
        }

    }


    fun toastNotify(notificationMessage: String, long: Boolean, useUiThread: Boolean = true) {
        if (useUiThread) {
            runOnUiThread {
                toaster(notificationMessage, long)
            }
        } else {
            toaster(notificationMessage, long)
        }
    }

    private fun toaster(notificationMessage: String, longToast: Boolean) {
        val toast = Toast.makeText(
                this@ToDoActivity,
                notificationMessage,
                if (longToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
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
        } else if (item.itemId == R.id.menu_loginlogout) {
            loginLogout()
        }

        return true
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

        val task = object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                try {

                    checkItemInTable(item)
                    runOnUiThread {
                        if (item.isComplete) {
                            mAdapter.remove(item)
                            toastNotify("${item.text} removed", true, false)
                        } else {
                            mAdapter.sort({x, y -> x.compareTo(y)})
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

        item.text = mTextNewToDo.text.toString()
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
                            mAdapter.add(entity)
                            mAdapter.sort({x, y -> x.compareTo(y)})
                        }
                    }
                } catch (e: Exception) {
                    createAndShowDialogFromTask(e, "Error addItem")
                }

                return null
            }
        }

        runAsyncTask(task)

        mTextNewToDo.setText("")
    }



    /**
     * Add an item to the Mobile Service Table
     *
     * @param item
     * The item to Add
     */
    @Throws(ExecutionException::class, InterruptedException::class)
    fun addItemInTable(item: ToDoItem): ToDoItem {
        //myHandler.sendNotificationButtonOnClick()
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
                    //final List<ToDoItem> results = refreshItemsFromMobileServiceTableSyncTable()

                    runOnUiThread {
                        mAdapter.clear()

                        for (item in results) {
                            mAdapter.add(item)
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
                where().field("complete").eq(`val`(false)).and().field("userId").eq(`val`(mClient.currentUser.userId))
                .execute().get()

        val rowsSorted = rows.sortedBy { (!it.isHighPriority).toString() + it.text.trim()  }
        return rowsSorted
    }

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


            runOnUiThread {
                swipeLayout.isRefreshing  = true
            }

            val future = nextServiceFilterCallback.onNext(request)

            Futures.addCallback(future, object : FutureCallback<ServiceFilterResponse> {
                override fun onFailure(e: Throwable) {
                    resultFuture.setException(e)
                }

                override fun onSuccess(response: ServiceFilterResponse?) {
                    runOnUiThread {
                        mRunnable = Runnable {
                            // Update the text view text with a random number

                            // Hide swipe to refresh icon animation
                            swipeLayout.isRefreshing  = false
                        }

                        // Execute the task after specified time
                        mHandler.postDelayed(
                                mRunnable,
                                refreshDelay)
                    }

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
        client?.currentUser = user

        return true
    }


    fun loginLogout() {

        if (!mClient.isLoginInProgress) {
            if (mAdapter == null || mAdapter!!.isEmpty()) {
                // Sign in using the Google provider.
                mClient.login(MobileServiceAuthenticationProvider.Google, "familyshoppinglist", GOOGLE_LOGIN_REQUEST_CODE)
                mAddButton.isEnabled = true
            } else {
                val prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE)
                userId = prefs.getString(USERIDPREF, "") ?: return
                val user = MobileServiceUser(userId)
                user.authenticationToken = null
                cacheUserToken(user)
                mClient.logout()
                mAdapter.clear()
                toastNotify("Logged out", false)
                mAddButton.isEnabled = false

            }
        }
    }

    private fun authenticate() {
        // We first try to load a token cache if one exists.
        if (loadUserTokenCache(mClient)) {
            createTable()
        } else {
            //Else we show an empty table with login button
            mAdapter.clear()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // When request completes
        if (resultCode == RESULT_OK) {
            // Check the request code matches the one we send in the sign-in request
            if (requestCode == GOOGLE_LOGIN_REQUEST_CODE) {
                val result = mClient.onActivityResult(data)
                if (result.isLoggedIn) {
                    // sign-in succeeded
                    toastNotify("Login succeeded!", false)
                    cacheUserToken(mClient.currentUser)
                    createTable()
                } else {
                    // sign-in failed, check the error message
                    mClient.logout()
                    var errorMessage = result.errorMessage
                    if (errorMessage == null) {
                        errorMessage = "Not allowed user"
                    }
                    createAndShowDialog(errorMessage, "Error")
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