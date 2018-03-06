/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.csecsey.gergely.lepkehalo

import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout

import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.vision.barcode.Barcode
import com.google.gson.Gson

import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList

/**
 * Main activity demonstrating how to pass extra parameters to an activity that
 * reads barcodes.
 */
class MainActivity : AppCompatActivity(), RecyclerItemTouchHelper.RecyclerItemTouchHelperListener, BookListAdapter.BookListListener {
    private var bookListPlaceholder: LinearLayout? = null

    //list
    private lateinit var recyclerView: RecyclerView
    private lateinit var mAdapter: BookListAdapter
    private lateinit var bookList: ArrayList<Book>

    private val konyvID = ""

    //handle data storage
    private var gson = Gson()
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView = findViewById<View>(R.id.recycler_view) as RecyclerView
        bookListPlaceholder = findViewById<View>(R.id.empty_booklist_placeholder) as LinearLayout

        mAdapter = BookListAdapter(this, this)
        mAdapter.addListener(this)

        //        listView = (ListView) findViewById(R.id.history_list);
        //        adapter = new CustomListAdapter(this, bookList);
        //        listView.setAdapter(adapter);

        val mLayoutManager = LinearLayoutManager(applicationContext)
        recyclerView.layoutManager = mLayoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = mAdapter
        registerForContextMenu(recyclerView)

        val itemTouchHelperCallback = RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this)
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_mainactivity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.app_bar_scan -> {
                startScan()
                return true
            }
        }
        return false
    }

    fun startScan() {
        val intent = Intent(this, BarcodeCaptureActivity::class.java)
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true)
        //        intent.putExtra(BarcodeCaptureActivity.UseFlash, useFlash.isChecked());
        intent.putExtra(BarcodeCaptureActivity.AutoCapture, true)
        startActivityForResult(intent, RC_BARCODE_CAPTURE)
    }

    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * [.RESULT_CANCELED] if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     *
     *
     *
     * You will receive this call immediately before onResume() when your
     * activity is re-starting.
     *
     *
     *
     * @param requestCode The integer request code originally supplied to
     * startActivityForResult(), allowing you to identify who this
     * result came from.
     * @param resultCode  The integer result code returned by the child activity
     * through its setResult().
     * @param data        An Intent, which can return result data to the caller
     * (various data can be attached to Intent "extras").
     * @see .startActivityForResult
     *
     * @see .createPendingResult
     *
     * @see .setResult
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    val barcode = data.getParcelableExtra<Barcode>(BarcodeCaptureActivity.BarcodeObject)
                    sendISBNrequest(barcode.displayValue)
                    Log.d(TAG, "Barcode read: " + barcode.displayValue)
                } else {
                    Log.d(TAG, "No barcode captured, intent data is null")
                }
            } else {
                Log.d(TAG, String.format(getString(R.string.barcode_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    protected fun startChromeTab(url: String) {
        val builder = CustomTabsIntent.Builder()
        builder.setToolbarColor(Color.parseColor("#2f5f8f"))
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
    }

    protected fun sendISBNrequest(barcode: String) {
        // Tag used to cancel the request
        val tag_json_obj = "json_obj_req"

        val url = "https://moly.hu/api/book_by_isbn.json?q=$barcode&key=$API_KEY"
        val pDialog = ProgressDialog(this)
        pDialog.setMessage(getString(R.string.loading))
        pDialog.show()

        val jsonObjReq = JsonObjectRequest(Request.Method.GET,
                url, null,
                Response.Listener { response ->
                    Log.d(TAG, response.toString())
                    pDialog.hide()
                    handleJSONResponse(response)
                }, Response.ErrorListener { error ->
            VolleyLog.d(TAG, "Error: " + error.message)
            pDialog.hide()

            //TODO Not very elegant, change later
            if (error.message!!.contains("JSONException")) {
                Snackbar.make(bookListPlaceholder!!, getString(R.string.book_not_found),
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.dismiss)) { }
                        .show()
            } else {
                Snackbar.make(bookListPlaceholder!!, getString(R.string.network_error),
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.dismiss)) { }
                        .show()
            }
        })

        HttpRequests.getInstance().addToRequestQueue(jsonObjReq, tag_json_obj)
    }

    /*
    * Item is swiped callback
    */
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int, position: Int) {
        if (viewHolder is BookListAdapter.MyViewHolder) {
            // get the removed item name to display it in snack bar
            val title = bookList!![viewHolder.getAdapterPosition()].title

            // removed item data for Undo
            val deletedItem = bookList!![viewHolder.getAdapterPosition()]
            val deletedIndex = viewHolder.getAdapterPosition()

            // remove the item from recycler view
            mAdapter!!.removeItem(viewHolder.getAdapterPosition())

            // showing snack bar with Undo option
            Snackbar.make(bookListPlaceholder!!, title + " törölve",
                    Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.undo)) {
                        mAdapter!!.restoreItem(deletedItem, deletedIndex)
                        //TODO refactor this to a dataclass
                    }
                    .show()

            //TODO refactor this to a dataclass
        }
    }

    override fun onTap(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (viewHolder is BookListAdapter.MyViewHolder) {
            val konyvID = bookList!![viewHolder.getAdapterPosition()].id
            startChromeTab("https://moly.hu/konyvek/" + konyvID)
        }
    }

    protected fun handleJSONResponse(jObj: JSONObject) {
        try {
            var b = Book()
            b.id = jObj.get("id").toString()
            b.thumbnailUrl = jObj.get("cover").toString()
            b.author = jObj.get("author").toString()
            b.title = jObj.get("title").toString()
            mAdapter.pushItem(b)
            startChromeTab("https://moly.hu/konyvek/" + b.id)
        } catch (e: JSONException) {}
    }

    companion object {
        private val RC_BARCODE_CAPTURE = 9001
        private val TAG = "BarcodeMain"
        private val API_KEY = "08b9582cd4212eb22d52e9ba4964bfae"
        private val STORAGE_KEY = "lepkehalo.list.state"
    }

    override fun onBookListChanged() {
        if (mAdapter.itemCount == 0) {
            recyclerView.visibility = View.GONE
            bookListPlaceholder!!.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            bookListPlaceholder!!.visibility = View.GONE
        }
    }

}
