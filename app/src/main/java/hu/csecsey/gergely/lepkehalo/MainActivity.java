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

package hu.csecsey.gergely.lepkehalo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Main activity demonstrating how to pass extra parameters to an activity that
 * reads barcodes.
 */
public class MainActivity extends AppCompatActivity implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    // use a compound button so either checkbox or switch widgets work.
    private TextView historyHeader;

    //list
    private RecyclerView recyclerView;
    private BookListAdapter mAdapter;
    private List<Book> bookList = new ArrayList<>();

    private String konyvID = "";
    private static final int RC_BARCODE_CAPTURE = 9001;
    private static final String TAG = "BarcodeMain";
    private static final String API_KEY = "08b9582cd4212eb22d52e9ba4964bfae";
    private static final String STORAGE_KEY = "lepkehalo.list.state";

    //handle data storage
    Gson gson = new Gson();
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        //TODO remove
        historyHeader = (TextView) findViewById(R.id.history);

        //read preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String json = prefs.getString(STORAGE_KEY, "[]");
        Log.d(TAG, json);
        //type token for the gson call
        Type bookListType = new TypeToken<ArrayList<Book>>() {
        }.getType();
        bookList = gson.fromJson(json, bookListType);
        if (bookList == null) {
            bookList = new ArrayList<>();
        }
        Log.d(TAG, bookList.toString());

        mAdapter = new BookListAdapter(this, bookList,this);

//        listView = (ListView) findViewById(R.id.history_list);
//        adapter = new CustomListAdapter(this, bookList);
//        listView.setAdapter(adapter);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(mAdapter);
        registerForContextMenu(recyclerView);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mainactivity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.app_bar_scan:
                startScan();
                return true;
        }
        return false;
    }

    public void startScan() {
        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
//        intent.putExtra(BarcodeCaptureActivity.UseFlash, useFlash.isChecked());

        startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }

    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * {@link #RESULT_CANCELED} if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     * <p/>
     * <p>You will receive this call immediately before onResume() when your
     * activity is re-starting.
     * <p/>
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     * @see #startActivityForResult
     * @see #createPendingResult
     * @see #setResult(int)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    //TODO remove
                    historyHeader.setText(R.string.barcode_success);
                    sendISBNrequest(barcode.displayValue);
                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                } else {
                    historyHeader.setText(R.string.barcode_failure);
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {
                historyHeader.setText(String.format(getString(R.string.barcode_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    protected void startChromeTab(String url) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(Color.parseColor("#2f5f8f"));
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(url));
    }

    protected void sendISBNrequest(String barcode) {
        // Tag used to cancel the request
        String tag_json_obj = "json_obj_req";

        String url = "https://moly.hu/api/book_by_isbn.json?q=" + barcode + "&key=" + API_KEY;
        final ProgressDialog pDialog = new ProgressDialog(this);
        pDialog.setMessage(getString(R.string.loading));
        pDialog.show();

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());
                        pDialog.hide();
                        handleJSONResponse(response);
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                pDialog.hide();

                //TODO Not very elegant, change later
                if (error.getMessage().contains("JSONException")) {
                    Snackbar.make(historyHeader, getString(R.string.book_not_found),
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.dismiss), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            })
                            .show();
                } else {
                    Snackbar.make(historyHeader, getString(R.string.network_error),
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.dismiss), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            })
                            .show();
                }
            }
        });

        HttpRequests.getInstance().addToRequestQueue(jsonObjReq, tag_json_obj);
    }

    /*
    * Item is swiped callback
    */
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof BookListAdapter.MyViewHolder) {
            // get the removed item name to display it in snack bar
            String title = bookList.get(viewHolder.getAdapterPosition()).getTitle();

            // removed item data for Undo
            final Book deletedItem = bookList.get(viewHolder.getAdapterPosition());
            final int deletedIndex = viewHolder.getAdapterPosition();

            // remove the item from recycler view
            mAdapter.removeItem(viewHolder.getAdapterPosition());

            // showing snack bar with Undo option
            Snackbar.make(historyHeader, title + " törölve",
                    Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.undo), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mAdapter.restoreItem(deletedItem, deletedIndex);
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onTap(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof BookListAdapter.MyViewHolder) {
            String konyvID = bookList.get(viewHolder.getAdapterPosition()).getId();
            startChromeTab("https://moly.hu/konyvek/" + konyvID);
        }
    }

    protected void handleJSONResponse(JSONObject jObj) {
        String bookId ="";
        try {
            bookId = jObj.get("id").toString();
            int bookPos = inBooksList(bookId);
            Book b = new Book();
            if (bookPos == -1) {
                b.setId(bookId);
                b.setThumbnailUrl(jObj.get("cover").toString());
                b.setAuthor(jObj.get("author").toString());
                b.setTitle(jObj.get("title").toString());
                bookList.add(0, b);
            } else {
               b = bookList.remove(bookPos);
               bookList.add(0, b);
            }
            mAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
        }

        //save the list to data storage
        String json = gson.toJson(bookList);
        prefs.edit().putString(STORAGE_KEY, json).apply();

        //open chrome
        startChromeTab("https://moly.hu/konyvek/" + bookId);
    }

    private int inBooksList(String id) {
        if (bookList.isEmpty())
            return -1;

        for (int i = 0; i < bookList.size(); i++) {
            if (bookList.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

}
