package com.ssuser.ss.sunstonechat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

// Pebble imports
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NewContactCreateActivity extends ActionBarActivity {
    private final String TAG = "NewContactCreateActivit";
    private boolean WATCH_BUSY = false;
    private final int WATCH_DELETE = 2;
    private final int WATCH_ADD    = 1;
    private int WATCH_FLAG = 0;

    ContactDatabaseHandler db = new ContactDatabaseHandler(this);

    PebbleKit.PebbleDataReceiver dataReceiver;
    PebbleKit.PebbleAckReceiver ackReceiver;
    PebbleKit.PebbleNackReceiver nackReceiver;

    private static final UUID SSCHAT_UUID = UUID.fromString("E30CF49A-497B-4FDB-B4CD-7D655C0C84FB");

    private ProgressDialog ringProgressDialog;
    // App Message key - new contact
    private static final int AM_NEW_CONTACT = 5;
    private static final int AM_DELETE_CONTACT = 3;
    private static final int AM_CREATED_CONTACT = 9;
    private static final int AM_RESULT_CODE = 50; // Add Contact result
    private static final int AM_CONTACT_ALREADY_EXISTS = 3;

    // Add result codes
    private static final int AM_SUCCESS = 0;
    private static final int AM_FULL    = 1;
    private static final int AM_CONTACT_NOT_FOUND = 4;

    private int deletedContactFlag = 0;
    private String contactNumber;
    private String sender;
    private String contactName;
    TextView contactBoxText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_contact_create);
        SharedPreferences prefs = getSharedPreferences(ContactListActivity.PREFS_NAME, 0);
        sender = prefs.getString(LoginActivity.PHONENUMBER_KEY,"");

        setTitle("New Contact");

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        final EditText nameTxt = (EditText) findViewById(R.id.contactNameForm);
        final EditText phoneTxt = (EditText) findViewById(R.id.contactNumberForm);

        nameTxt.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(TAG, "onFocusChange nameTxt");
                if(!hasFocus && !phoneTxt.hasFocus()){
                    Log.d(TAG, "Hiding nameTxt keyboard");
                    hideSoftKeyboard(v);
                }
            }
        });
        phoneTxt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(TAG, "onFocusChange phoneTxt");
                if(!hasFocus && !nameTxt.hasFocus()){
                    Log.d(TAG, "Hiding phoneTxt keyboard");
                    hideSoftKeyboard(v);
                }
            }
        });

        dataReceiver =  new PebbleKit.PebbleDataReceiver(SSCHAT_UUID) {
            @Override
            public void receiveData(final Context context, final int i, final PebbleDictionary data){
                Log.i(TAG, "Got message from pebble");
                PebbleKit.sendAckToPebble(getApplicationContext(), i);
                try {
                    ringProgressDialog.dismiss();
                } catch (NullPointerException e){
                    Log.e(TAG, "error: " + e);
                }
                if( data.contains(AM_CREATED_CONTACT)){
                    // Successfully added the contact in the watch
                    String key = data.getString(AM_CREATED_CONTACT);
                    Log.i(TAG, "Got key " + key + " from watch");
                    new MyAsyncTask().execute(sender,contactNumber,key);
                    Log.i(TAG, "Do some server stuff");
                } else if( data.contains(AM_RESULT_CODE)) {
                    int result = data.getUnsignedIntegerAsLong(AM_RESULT_CODE).intValue();
                    if (result == AM_FULL) {
                        Log.e(TAG, "Contact List was full");
                        contactBoxText.setText("Contact list on watch is full");
                        contactBoxText.setVisibility(View.VISIBLE);
                    } else if (result == AM_CONTACT_ALREADY_EXISTS) {
                        Log.e(TAG, "Contact already exists!");
                        contactBoxText.setText("Contact already exists");
                        contactBoxText.setVisibility(View.VISIBLE);
                    } else if (result == AM_SUCCESS) {
                        Log.d(TAG, "Successfully deleted contact from the watch");
                        deletedContactFlag = 1;
                    } else if (result == AM_CONTACT_NOT_FOUND){
                        Log.d(TAG, "Tried to delete contact, but it was not found on the watch.");
                    } else {
                        Log.e(TAG, "Got weird result code from pebble result=" + result);
                    }
                }
            }
        };

        nackReceiver =  new PebbleKit.PebbleNackReceiver(SSCHAT_UUID) {
            @Override
            public void receiveNack(Context context, int i){
                if( WATCH_FLAG == WATCH_ADD) {
                    onContactSubmit(null);
                } else if( WATCH_FLAG == WATCH_DELETE ){
                    Log.d(TAG, "WATCH DELETE");
                    deleteContact(contactNumber);
                } else {
                    Log.d(TAG, "Got NACK and watch flag was " + WATCH_FLAG);
                    Log.d(TAG, "This didn't correspond to a valid function code");
                }
                Log.i(TAG, "Received NACK for transaction " + i);
            }
        };

        ackReceiver =  new PebbleKit.PebbleAckReceiver(SSCHAT_UUID) {
            @Override
            public void receiveAck(Context context, int i) {
                Log.i(TAG, "Received ACK for transaction " + i);
                //Intent intent = new Intent(context, ContactListActivity.class);
                //startActivity(intent);
            }
        };
    }

    protected void onPause(){
        super.onPause();
        //LoginManager.setLastActive(getApplicationContext());
        try {
            getApplicationContext().unregisterReceiver(dataReceiver);
            getApplicationContext().unregisterReceiver(ackReceiver);
            getApplicationContext().unregisterReceiver(nackReceiver);
        } catch (java.lang.IllegalArgumentException e){
            Log.d(TAG, "Couldn't unregister dataReceiver");
        }
        //unregisterReceiver(ackReceiver);
        //unregisterReceiver(nackReceiver);
    }

    protected void onResume(){
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(ContactListActivity.PREFS_NAME, 0);
        sender = prefs.getString(LoginActivity.PHONENUMBER_KEY,"");
        contactBoxText = (TextView) findViewById(R.id.newContactBox);

        PebbleKit.registerReceivedAckHandler(getApplicationContext(),ackReceiver);
        PebbleKit.registerReceivedNackHandler(getApplicationContext(), nackReceiver);
        PebbleKit.registerReceivedDataHandler(getApplicationContext(), dataReceiver);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_contact_create, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*
        if (id == R.id.action_settings) {
            return true;
        }
        */
        return super.onOptionsItemSelected(item);
    }


    private void hideSoftKeyboard(View view){
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }


    public class MyAsyncTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params){
            String result = "";
            String sender = params[0];          // Users(Personal) Represented by Phone Numbers
            String contactNumber = params[1];            // Users(Contact) Represented by Phone Numbers
            String key = params[2];
            // Create a new HTTPClient and Post Header
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
            HttpPost httpPost = new HttpPost("http://192.168.1.200:8078/sunstonechat/linkContact/");

            Log.d(TAG, "SENDER FROM: " + sender);
            if( sender.equals("") ){
                //Toast.makeText(getApplicationContext(), "A Login is Required to Add Contact", Toast.LENGTH_LONG).show();
                return "loginRequired";
            }

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
                nameValuePairs.add(new BasicNameValuePair("sender", sender));
                nameValuePairs.add(new BasicNameValuePair("recipient", contactNumber));
                nameValuePairs.add(new BasicNameValuePair("key",key));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity entity = response.getEntity();

                result = EntityUtils.toString(entity);

                Log.d(TAG, "Response from post is: " +  result);
            } catch (ClientProtocolException e){
                Log.e(TAG, "ClientProtocolException error");
                result = "fail";
            } catch (IOException e){
                Log.e(TAG, "IOException error: " + e);
                result = "fail";
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result){
            ringProgressDialog.dismiss();
            Log.d(TAG, "Result: " + result);
            if(result.equals("loginRequired")){
                Toast.makeText(getApplicationContext(), "A Login is Required to Add Contact", Toast.LENGTH_LONG).show();
                deleteContact(contactNumber);
                return;
            }
            boolean success = false;
            String reason   = "";
            try {
                JSONObject jsonObject = new JSONObject(result);
                success  = jsonObject.getBoolean("result");
                reason   = jsonObject.getString("reason");
                Log.d(TAG, "Parsed return data: " + success + " " + reason);
            } catch (JSONException e) {
                Log.e(TAG, "Failed parsing JSON data from post response");
                // Shouldn't be able to fail the response here
                Log.e(TAG, result);
                //return;
            }

            // Check the result success of the contact link
            if( success ){
                // We successfully created or updated the link
                Log.d(TAG, "Successfully formed link. Returning from contact create");
                Contacts newContacts = new Contacts(contactName,contactNumber,false,true);      // ContactName, ContactNumber, isLinked, isValid
                db.addContacts(newContacts);
                finish();
            } else {
                // For some reason the link has failed.
                // Maybe we should also toast the reason for the user
                Toast toast = Toast.makeText(getApplicationContext(), "Failed to add contact: " + reason, Toast.LENGTH_LONG);
                toast.show();
                deleteContact(contactNumber);
            }
        }
    }


    public void deleteContact(String contactNumber){
        if(WATCH_BUSY){
            Log.d(TAG, "Watch is busy.");
            return;
        }
        WATCH_BUSY = true;
        WATCH_FLAG = WATCH_DELETE;
        //ringProgressDialog = ProgressDialog.show(NewContactCreateActivity.this, "Please wait...", "Deleting Contact...", true);
        //ringProgressDialog.setCancelable(true);

        boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
        if( !connected ){
            Log.e(TAG, "Watch is not connected.");
            WATCH_BUSY = false;
            return;
        }
        Log.d(TAG, "Starting Pebble App on Deletion side");
        PebbleKit.startAppOnPebble(getApplicationContext(), SSCHAT_UUID);
        Log.d(TAG, "Past startAppOnPebble");

        PebbleDictionary contact_data = new PebbleDictionary();

        contact_data.addString(AM_DELETE_CONTACT, contactNumber);

        PebbleKit.sendDataToPebble(getApplicationContext(), SSCHAT_UUID, contact_data);

        WATCH_BUSY = false;
    }

    public void onContactSubmit(View view){

        if(sender.equals("")){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(NewContactCreateActivity.this);
            alertDialogBuilder.setTitle("Login Required");
            alertDialogBuilder.setMessage("You need to be signed in to add a contact!");
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setPositiveButton("Login", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                }
            });
            alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(TAG, "Cancel");
                }
            });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

            return;
        }

        if(WATCH_BUSY){
            Log.d(TAG, "Watch is busy.");
            return;
        }
        WATCH_BUSY = true;
        WATCH_FLAG = WATCH_ADD;
        ringProgressDialog = ProgressDialog.show(NewContactCreateActivity.this, "Please wait...", "Saving Contact...", true);
        ringProgressDialog.setCancelable(true);

        EditText contactNameText = (EditText) findViewById(R.id.contactNameForm);
        EditText contactNumText = (EditText) findViewById(R.id.contactNumberForm);

        contactName = contactNameText.getText().toString();
        contactNumber = contactNumText.getText().toString();

        Log.d(TAG,"The string length of contactName is: " + contactName.length() +  " and the string length of contactNumber is: " + contactNumber.length() );
        if(contactName.length() == 0 || contactNumber.length() == 0){
            contactBoxText.setText("Incomplete Fields");
            contactBoxText.setVisibility(View.VISIBLE);
            WATCH_BUSY = false;
            ringProgressDialog.dismiss();
            return;
        }
        if(contactNumber.length() < 10){
            contactBoxText.setText("Phone Number too short");
            contactBoxText.setVisibility(View.VISIBLE);
            WATCH_BUSY = false;
            ringProgressDialog.dismiss();
            return;
        }

        Log.d(TAG, "contactName is " + contactName);
        Log.d(TAG, "contactNumber is " + contactNumber);

        boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
        if( !connected ){
            Log.e(TAG, "Watch is not currently connected. Can't create contact");
            WATCH_BUSY = false;
            ringProgressDialog.dismiss();
            Toast.makeText(getApplicationContext(), "Pebble is not connected!", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "Clicked new contact. Opening Pebble App");
        PebbleKit.startAppOnPebble(getApplicationContext(), SSCHAT_UUID);
        Log.d(TAG, "Past startAppOnPebble");

        PebbleDictionary contact_data = new PebbleDictionary();

        contact_data.addString(AM_NEW_CONTACT, contactNumber);

        PebbleKit.sendDataToPebble(getApplicationContext(), SSCHAT_UUID, contact_data);

        WATCH_BUSY = false;
    }
}
