package com.palomaki.familyshoppinglist


import android.annotation.SuppressLint
import java.net.MalformedURLException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

import android.app.Activity
import android.app.AlertDialog
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View

import android.widget.EditText

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
import android.util.Log
import android.widget.*

import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser

import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.google.common.util.concurrent.*
import java.util.*


val oneDayMs = 1000*60*60*24L

class ToDoActivity : Activity() {

    private val TAG = "ToDoActivity"

    private val appUrl = "https://familyshoppinglist.azurewebsites.net"

    private val completeCol = "complete"
    private val textCol = "text"
    private val idCol = "id"
    private val userIdCol = "userId"
    private val updatedAt = "updatedAt"

    private var modeShowTrashBin = false

    /**
     * Mobile Service Client reference
     * Create the Mobile Service Client instance, using the provided
     * Mobile Service URL and key
     */
    private val mClient = MobileServiceClient(appUrl, this).withFilter(ProgressFilter())


    /**
     * Mobile Service Table used to access data
     */
    private var mToDoTable: MobileServiceTable<ToDoItem> = mClient.getTable(ToDoItem::class.java)


    /**
     * Adapter to sync the items list with the view
     */
    private lateinit var mAdapter: ToDoItemAdapter

    private lateinit var mTextNextShopItem: EditText

    private lateinit var mSwipeLayout: SwipeRefreshLayout

    private lateinit var mAddButton: Button

    private var userId = ""

    private lateinit var mHandler: Handler

    private lateinit var mRunnable: Runnable

    private val refreshDelay = 500L

    private lateinit var mProgressBar: ProgressBar

    /**
     * Initializes the activity
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_to_do)

        mAddButton = findViewById<Button>(R.id.buttonAddToDo)
        mProgressBar = findViewById<ProgressBar>(R.id.progressBar)
        mAdapter = ToDoItemAdapter(this, R.layout.row_list_to_do)
        mTextNextShopItem = findViewById<EditText>(R.id.textNewToDo)

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


            // Load the items from the Mobile Service
            if (mClient.getCurrentUser() != null) {
                refreshItemsFromTable()
            }

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
        mSwipeLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
        mSwipeLayout.setOnRefreshListener {
            mSwipeLayout.isRefreshing  = true
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
        when(item.itemId) {
            R.id.menu_refresh -> refreshItemsFromTable()
            R.id.menu_loginlogout -> loginLogout()
            R.id.menu_trashbin -> {
                item.isChecked = !item.isChecked
                modeShowTrashBin = item.isChecked
                refreshItemsFromTable()
            }
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val loginLogoutMenuItem = menu.findItem(R.id.menu_loginlogout)
        val refreshMenuItem = menu.findItem(R.id.menu_refresh)
        val trashMenuItem = menu.findItem(R.id.menu_trashbin)
        if (mClient.getCurrentUser() != null) {
            loginLogoutMenuItem.title = "Logout"
            refreshMenuItem.isEnabled = true
            trashMenuItem.isEnabled = true
        } else {
            loginLogoutMenuItem.title = "Login with Google"
            refreshMenuItem.isEnabled = false
            trashMenuItem.isEnabled = false
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

        val task = @SuppressLint("StaticFieldLeak")
        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                try {

                    checkItemInTable(item)
                    runOnUiThread {
                        if (item.isComplete) {
                            toastNotify("${item.text} removed", true, false)
                            Log.i(TAG, "${item.text} removed and list refreshed")
                        } else {
                            toastNotify("${item.text} returned to shopping list", true, false)
                        }
                        refreshItemsFromTable()
                    }
                } catch (e: Exception) {
                    createAndShowDialogFromTask(e)
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

        item.text = mTextNextShopItem.text.toString()
        if (item.text.trim().isEmpty()) {
            createAndShowDialog("Cannot add empty item","Error")
            return
        }
        item.isComplete = false
        item.userId = userId

        // Insert the new item
        val task = @SuppressLint("StaticFieldLeak")
        object : AsyncTask<Void, Void, Void>() {
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
                    createAndShowDialogFromTask(e)
                }

                return null
            }
        }

        runAsyncTask(task)

        mTextNextShopItem.setText("")
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

        val task = @SuppressLint("StaticFieldLeak")
        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {

                try {
                    val results = refreshItemsFromMobileServiceTable()

                    runOnUiThread {
                        mAdapter.clear()

                        for (item in results) {
                            mAdapter.add(item)
                        }
                    }
                } catch (e: Exception) {
                    createAndShowDialogFromTask(e)
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

        var rows = mToDoTable.where().field(completeCol).eq(`val`(false))
                    .and().field(userIdCol).eq(`val`(mClient.currentUser.userId))
                    .execute().get()

        if (modeShowTrashBin) {
            var trashRows = mToDoTable.where().field(completeCol).eq(`val`(true))
                    .and().field(userIdCol).eq(`val`(mClient.currentUser.userId))
                    .and().field(updatedAt).ge(`val`(java.sql.Date(System.currentTimeMillis() - oneDayMs)))
                    .execute().get()
            if (trashRows.size > 0) {
                rows.addAll(trashRows)
            }
        }

        val rowsSorted = rows.sortedBy {
            (it.isComplete).toString() + (!it.isHighPriority).toString() + it.text.trim()
        }
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

        val task = @SuppressLint("StaticFieldLeak")
        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                try {

                    val syncContext = mClient.syncContext

                    if (syncContext.isInitialized)
                        return null

                    val localStore = SQLiteLocalStore(mClient.context, "OfflineStore", null, 1)

                    val tableDefinition = HashMap<String, ColumnDataType>()
                    tableDefinition[idCol] = ColumnDataType.String
                    tableDefinition[textCol] = ColumnDataType.String
                    tableDefinition[completeCol] = ColumnDataType.Boolean

                    localStore.defineTable("ToDoItem", tableDefinition)

                    val handler = SimpleSyncHandler()

                    syncContext.initialize(localStore, handler).get()

                } catch (e: Exception) {
                    createAndShowDialogFromTask(e)
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
     */
    private fun createAndShowDialogFromTask(exception: Exception) {
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
        return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private inner class ProgressFilter : ServiceFilter {

        override fun handleRequest(request: ServiceFilterRequest, nextServiceFilterCallback: NextServiceFilterCallback): ListenableFuture<ServiceFilterResponse> {

            val resultFuture = SettableFuture.create<ServiceFilterResponse>()


            runOnUiThread {

                 if (!mSwipeLayout.isRefreshing) {
                     mProgressBar.visibility = View.VISIBLE
                 }
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
                            mSwipeLayout.isRefreshing  = false
                            mProgressBar.visibility = View.INVISIBLE
                        }

                        // Execute the task after specified time
                        mHandler.postDelayed(mRunnable, refreshDelay)
                    }

                    resultFuture.set(response)
                }
            }, MoreExecutors.directExecutor())

            return resultFuture
        }
    }


    private fun createTable() {

        // Get the table instance to use.
        mToDoTable = mClient.getTable(ToDoItem::class.java)

        mTextNextShopItem = findViewById<EditText>(R.id.textNewToDo)

        // Create an adapter to bind the items with the view.
        mAdapter = ToDoItemAdapter(this, R.layout.row_list_to_do)
        val listViewToDo = findViewById<ListView>(R.id.listViewToDo)
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
            if (mAdapter.isEmpty) {
                // Sign in using the Google provider.
                mClient.login(MobileServiceAuthenticationProvider.Google, "familyshoppinglist", GOOGLE_LOGIN_REQUEST_CODE)
            } else {
                val prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE)
                userId = prefs.getString(USERIDPREF, "") ?: return
                val user = MobileServiceUser(userId)
                user.authenticationToken = null
                cacheUserToken(user)
                mClient.logout()
                mAdapter.clear()
                toastNotify("Logged out", false)
                setUiStatus(false)
            }
        }
    }

    private fun authenticate() {
        // We first try to load a token cache if one exists.
        if (loadUserTokenCache(mClient)) {
            createTable()
            setUiStatus(true)
        } else {
            //Else we show an empty table with login button
            mAdapter.clear()
            setUiStatus(false)
        }
    }

    private fun setUiStatus(loggedIn: Boolean) {
        mAddButton.isEnabled = loggedIn
        mTextNextShopItem.isEnabled = loggedIn
        if (loggedIn)
            mTextNextShopItem.setHint(getString(R.string.add_textbox_hint))
        else
            mTextNextShopItem.setHint(getString(R.string.logged_out_hint))
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
                    setUiStatus(true)
                } else {
                    // sign-in failed, check the error message
                    mClient.logout()
                    var errorMessage = result.errorMessage
                    if (errorMessage == null) {
                        errorMessage = "Not allowed user"
                    }
                    createAndShowDialog(errorMessage, "Error")
                    setUiStatus(false)
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