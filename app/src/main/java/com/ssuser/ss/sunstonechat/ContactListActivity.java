package com.ssuser.ss.sunstonechat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ContactListActivity extends ActionBarActivity {
    // Intent extra string
    public static final String EXTRA_NUMBER = "com.sunstone.sunstonechat.extraNumber";
    public static final String EXTRA_NAME = "com.sunstone.sunstonechat.extraName";
    private static final int WATCH_CONTACT_BUSY = 1;
    private static final int WATCH_DELETE_BUSY = 2;
    private static final int WATCH_VALIDATE_BUSY = 3;

    public static final String PREFS_NAME = "SunstoneChatPrefs";

    ContactDatabaseHandler db;
    DatabaseHandler msgDB;

    private static int LAST_CALL_WATCH = 0;
    private boolean WATCH_BUSY = false;
    private PebbleKit.PebbleDataReceiver dataReceiver;
    private PebbleKit.PebbleAckReceiver ackReceiver;
    private PebbleKit.PebbleNackReceiver nackReceiver;

    private ProgressDialog ringProgressDialog;

    // Logging Tag
    private static final String TAG = "ContactListActivity";
    private ArrayList<Contacts> list = new ArrayList<Contacts>();
    ArrayAdapter adapter;
    private static final int AM_GET_CONTACTS = 4;
    private static final int AM_VERIFY_CONTACT = 8;
    private static final int AM_RESULT_KEY = 50;
    private static final int AM_SUCCESS = 0;
    private static final int AM_CONTACT_NOT_FOUND = 4;
    private static final int AM_DELETE_CONTACT = 3;
    private static final int AM_CONTACT_KEY_START = 20;
    private static final int AM_MAX_CONTACTS = 50;
    private static final int AM_SCRAMBLE_KEY = 19;

    // UUID for the sunstone chat app
    private static final UUID SSCHAT_UUID = UUID.fromString("E30CF49A-497B-4FDB-B4CD-7D655C0C84FB");

    ListView contactList;

    private Menu menu;

    private String val_num = null;
    private String val_key = null;
    private String contactNumber = "";
    private String contactName = "";
    private int val_position = -1;
    private boolean sendScrambleFlag = false;
    Contacts contactForDelete;
    Contacts contactForConversation;

    // The tooltip suggestion string for the add a contact message
    private final static String addContactTooltipSmall = "Click on Options Menu to add new contact";

    private class SmsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent){
            Log.d(TAG, "RECEIVED FROM HOME");
            onLoadingList();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        Log.d(TAG, "ContactList...a");

        if( !WordLibrary.getInstance().isLoaded() ){
            WordLibrary.getInstance().ensureLoaded(getResources());
        }

        // Set up the list view and array adapter
        contactList = (ListView) findViewById(R.id.contactListView);
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_activated_2, android.R.id.text1, list){
            @Override
            public View getView(int position, View convertView, ViewGroup parent){
                View view = super.getView(position, convertView, parent);
                // Get the contact at this position
                Contacts contact = list.get(position);

                // Get the fields from the contact
                String name = contact.getName();
                String number = contact.getPhoneNumber();
                msgDB = new DatabaseHandler(getApplicationContext());
                // Get references to the textviews in the list item
                TextView name_view = (TextView) view.findViewById(android.R.id.text1);
                TextView number_view = (TextView) view.findViewById(android.R.id.text2);
                //Button deleteButton = (Button) view.findViewById(R.id.button_custom);

                // Fill in the item text views
                name_view.setText(name);
                number_view.setText(number);

                // If this contact has unread messages, bold the name
                if(msgDB.hasUnreadMessages("+1" + number)){
                    Log.d(TAG, "number: " + number + "has unread messages");
                    name_view.setTypeface(null, Typeface.BOLD);
                    number_view.setTypeface(null, Typeface.BOLD);
                    view.setBackgroundColor(Color.GRAY);
                } else {
                    name_view.setTypeface(null, Typeface.NORMAL);
                    number_view.setTypeface(null, Typeface.NORMAL);
                    view.setBackgroundColor(Color.TRANSPARENT);
                }

                // If this contact hasn't been linked, make the row blue
                if(!contact.getValid()){
                    name_view.setTextColor(Color.RED);
                    number_view.setTextColor(Color.RED);
                    number_view.setText("Contact is not Valid. Click to Delete");
                } else if(!contact.getLinked()) {
                    name_view.setTextColor(Color.BLUE);
                    number_view.setTextColor(Color.BLUE);
                    number_view.setText("Click here to link");
                } else {
                    name_view.setTextColor(Color.BLACK);
                    number_view.setTextColor(Color.BLACK);
                }
                // Return the newly created view
                return view;
            }
        };

        contactList.setAdapter(adapter);

        final SwipeDetector swipeDetector = new SwipeDetector();
        contactList.setOnTouchListener(swipeDetector);

        contactList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Contacts contact = (Contacts) parent.getItemAtPosition(position);
                contactForDelete = contact;
                val_position = position;

                // Check if this is just the addContact message
                // If so we don't want to open the conversation activity for it
                if( contact.getPhoneNumber().equals(addContactTooltipSmall) ){
                    Log.i(TAG, "Clicked on the addContact suggestion");
                    return;
                }

                // Deleting Contacts Dialog Box
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ContactListActivity.this);
                alertDialogBuilder.setTitle("Delete Contact?");
                alertDialogBuilder.setMessage("Are you sure you want to delete?");
                alertDialogBuilder.setCancelable(true);
                alertDialogBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onDeleteContactClick(null);
                        Log.d(TAG,"DELETED CONTACT");
                    }
                });
                alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog alertDialog = alertDialogBuilder.create();

                // Checking Validity and Linkage Between Phone DB and WATCH

                if( !contact.getValid() ) {
                    onDeleteContactClick(null);
                    Toast toast = Toast.makeText(getApplicationContext(), "Contact Did Not exist on Watch, DELETED CONTACT!", Toast.LENGTH_LONG);
                    toast.show();
                }
                else if (!contact.getLinked() && !(swipeDetector.swipeDetected())) {
                    Log.d(TAG, "SWIPE NOT DETECTED");
                    ringProgressDialog = ProgressDialog.show(ContactListActivity.this, "Please wait...", "Contacting Server...", true);
                    ringProgressDialog.setCancelable(true);
                    ringProgressDialog.setCanceledOnTouchOutside(false);
                    Log.d(TAG, "Hit the server to check linked contact");
                    val_num = new String(contact.getPhoneNumber());
                    new MyAsyncTask().execute(contact.getPhoneNumber());
                    contactList.setItemChecked(position, false);
                }else if(swipeDetector.swipeDetected()){
                    alertDialog.show();
                }else {
                    contactForConversation = contactForDelete;
                    sendScramble();
                }

            }
        });

        // Pebble data receivers
        dataReceiver =  new PebbleKit.PebbleDataReceiver(SSCHAT_UUID) {
            @Override
            public void receiveData(final Context context, final int i, final PebbleDictionary data){
                Log.d(TAG, "Got data from Pebble");
                PebbleKit.sendAckToPebble(getApplicationContext(), i);
                if( data.contains(AM_SCRAMBLE_KEY)) {
                    byte[] scram_bytes = data.getBytes(AM_SCRAMBLE_KEY);
                    for( int j=0; j<scram_bytes.length; j++){
                        Log.d(TAG, "byte " + j + ": " + scram_bytes[j]);
                    }
                    ShuffleBytes.setShuffleBytes(context, scram_bytes);

                    Log.d(TAG, "data in bytes: " + data.getBytes(AM_SCRAMBLE_KEY));

                    Intent intent = new Intent(getApplicationContext(),ConversationActivity.class);
                    intent.putExtra(ConversationActivity.contactInfo, contactForConversation);
                    startActivity(intent);
                    Log.d(TAG, "Item is already linked!");

                    return;
                }
                if( data.contains(AM_RESULT_KEY)){
                    int result = data.getUnsignedIntegerAsLong(AM_RESULT_KEY).intValue();
                    if( result == AM_SUCCESS ) {
                        Contacts contacts = (Contacts) contactList.getItemAtPosition(val_position);
                        Log.d(TAG, "Contact position " + val_position);
                        if( contacts == null ){
                            Log.e(TAG, "Contact from th" +
                                    "e list was null");
                            onLoadingList();
                            return;
                        }
                        contacts.setLinked(true);
                        db.updateContact(contacts);
                        onLoadingList();
                        return;
                    }
                    if( result == AM_CONTACT_NOT_FOUND){
                        Log.e(TAG, "Contact to verify did not exist on the watch!");
                        return;
                    }
                    Log.e(TAG, "Got result code " + result);
                    Log.e(TAG, "Don't know what to do with it.");
                    return;
                }
                // else
                int j;
                ArrayList<String> watchList = new ArrayList<String>();
                list.clear();
                for(j=AM_CONTACT_KEY_START; j<AM_MAX_CONTACTS*2; j+=2){
                    if( data.contains(j) ){
                        // Add the users to the array list
                        watchList.add(data.getString(j));
                        int flag = data.getUnsignedIntegerAsLong(j+1).intValue();
                    } else {
                        Log.d(TAG, "Broke with j of " + j);
                        break;
                    }
                }
                // TODO:
                // We need to make a sanity check here to update the database
                // Basically double check that the "valid" contacts on the watch
                // all exist on the phone, and are marked as valid in the database
                //List<Contacts> dbQuery = db.getAllContacts();
                List<Contacts> dbQuery = db.getAllSortedContacts(getApplicationContext());
                for( Contacts contacts: dbQuery ) {
                    Log.d(TAG, "Contact info// Name: " + contacts.getName() +
                            " Phone Number: " + contacts.getPhoneNumber()
                            + "LINKED: " + contacts.getLinked()
                            + "ValiD: " + contacts.getValid());
                    if(watchList.contains(contacts.getPhoneNumber())) {          //LINKED with Phone and Watch (Returns true)
                        Log.d(TAG, "isLinked");
                        list.add(contacts);
                        watchList.remove(watchList.indexOf(contacts.getPhoneNumber()));
                    } else {                                                            //THINKS it's LINKED, but WATCH proves OTHERWISE
                        contacts.setValid(false);
                        db.updateContact(contacts);
                        list.add(contacts);
                        Log.e(TAG, "Watch contacts and phone contacts mismatch");
                        Log.e(TAG, "Contact db name:" + contacts.getName());
                        Log.e(TAG, "Contact db number" + contacts.getPhoneNumber());
                    }
                }
                for(int watchIndex=0; watchIndex < watchList.size(); watchIndex++){
                    Contacts missingContact = new Contacts();
                    missingContact.setPhoneNumber(watchList.get(watchIndex));
                    missingContact.setValid(false);
                    db.updateContact(missingContact);
                    list.add(missingContact);
                    Log.e(TAG, "Missing Contacts on phone side");
                }
                // Loop through the dictionary and get any returned users
                // update the adapter so the changed data will appear in the activity

                if(list.size() == 0){
                    Contacts addContact = new Contacts();
                    addContact.setName("Add New Contact");
                    addContact.setPhoneNumber(addContactTooltipSmall);
                    addContact.setValid(true);
                    addContact.setLinked(true);
                    list.add(addContact);
                    Log.d(TAG, "Empty Contact List, requesting add contact");
                }

                adapter.notifyDataSetChanged();
            }
        };
        nackReceiver =  new PebbleKit.PebbleNackReceiver(SSCHAT_UUID) {
            @Override
            public void receiveNack(Context context, int i) {
                if (LAST_CALL_WATCH == WATCH_CONTACT_BUSY) {
                    onLoadingList();
                } else if (LAST_CALL_WATCH == WATCH_DELETE_BUSY) {
                    Log.d(TAG, "onDeleteContactCLICKKKK");
                    onDeleteContactClick(null);
                } else if ( LAST_CALL_WATCH == WATCH_VALIDATE_BUSY){
                    Log.d(TAG, "Retrying to validate watch contact");
                    validateWatchContact();
                } else {
                    Log.d(TAG, "WATCH has no command");
                }
                Log.i(TAG, "Received NACK for transaction " + i);
            }
        };
        ackReceiver = new PebbleKit.PebbleAckReceiver(SSCHAT_UUID) {
            @Override
            public void receiveAck(Context context, int i) {
                Log.i(TAG, "Received ACK for transaction " + i);
            }
        };
    }

    private SmsReceiver mySmsReceiver = new SmsReceiver();

    protected void onPause(){
        super.onPause();
        db.close();

        try {
            getApplicationContext().unregisterReceiver(dataReceiver);
            getApplicationContext().unregisterReceiver(ackReceiver);
            getApplicationContext().unregisterReceiver(nackReceiver);
        } catch (java.lang.IllegalArgumentException e){
            Log.e(TAG, "Couldn't unregister dataReceiver");
        }

    }

    @Override
    public void onBackPressed(){
        Log.d(TAG, "OVERRIDE BUTTON");
        finish();
        return;
    }

    private void onLoadingList(){

        if(WATCH_BUSY){
            Log.d(TAG, "Watch is busy: " + WATCH_BUSY);
            return;
        }
        WATCH_BUSY = true;
        LAST_CALL_WATCH = WATCH_CONTACT_BUSY;
        // Check for a valid watch connection and get the contact list
        if(!PebbleKit.isWatchConnected(getApplicationContext())){
            final String msg = "Watch is not connected!";
            Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
            toast.show();
            WATCH_BUSY = false;
            return;
        }

        Log.d(TAG, "Retrieving Contact Lists");
        PebbleKit.startAppOnPebble(getApplicationContext(), SSCHAT_UUID);

        PebbleDictionary contact_data = new PebbleDictionary();

        contact_data.addString(AM_GET_CONTACTS, "0");

        PebbleKit.sendDataToPebble(getApplicationContext(), SSCHAT_UUID, contact_data);
        WATCH_BUSY = false;
    }


    protected void onResume(){
        super.onResume();
        db = new ContactDatabaseHandler(getApplicationContext());

        invalidateOptionsMenu();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        PebbleKit.registerReceivedAckHandler(getApplicationContext(), ackReceiver);
        PebbleKit.registerReceivedNackHandler(getApplicationContext(), nackReceiver);
        PebbleKit.registerReceivedDataHandler(getApplicationContext(), dataReceiver);

        getApplicationContext().registerReceiver(mySmsReceiver, intentFilter);
        list.clear();

        onLoadingList();


        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_contact_list, menu);

        this.menu = menu;
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
        updateLoginInfo();
        return true;
    }

    private void updateLoginInfo(){
        Log.d(TAG, "Updating action bar sign in buttons...");
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
        String loginInfo = prefs.getString(LoginActivity.PHONENUMBER_KEY, "");
        if(loginInfo.equals("")) {
            menu.findItem(R.id.login).setVisible(true);
            menu.findItem(R.id.logout).setVisible(false);
        }else{
            menu.findItem(R.id.login).setVisible(false);
            menu.findItem(R.id.logout).setVisible(true);
        }

    }

    private void logout(){
        Log.d(TAG, "Signing user out...");
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(LoginActivity.PHONENUMBER_KEY, "");
        editor.putString(LoginActivity.USERNAME_KEY, "");

        editor.commit();
        Toast.makeText(getApplicationContext(), "Successfully Logged Out", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id){
            case R.id.login:
                Intent intentLogin = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intentLogin);
                return true;
            case R.id.logout:
                logout();
                updateLoginInfo();
                return true;
            case R.id.action_add_person:
                Intent intentContact = new Intent(getApplicationContext(),NewContactCreateActivity.class);
                startActivity(intentContact);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onStart(){
        super.onStart();

    }

    public void onDeleteContactClick(View view){
        Log.d(TAG, "Contact Delete from Contact List");
        if(WATCH_BUSY){
            Log.d(TAG, "Watch is busy: " + WATCH_BUSY);
            return;
        }
        WATCH_BUSY = true;
        LAST_CALL_WATCH = WATCH_DELETE_BUSY;
        Log.d(TAG, "Clicked delete contacts");

        Contacts contact = contactForDelete;

        contactNumber = contact.getPhoneNumber();
        contactName = contact.getName();

        PebbleKit.startAppOnPebble(getApplicationContext(), SSCHAT_UUID);

        PebbleDictionary contact_data = new PebbleDictionary();

        contact_data.addString(AM_DELETE_CONTACT, contactNumber);

        PebbleKit.sendDataToPebble(getApplicationContext(), SSCHAT_UUID, contact_data);
        msgDB.deleteAllMessagesFrom("+1" + contactNumber);
        Log.d(TAG, "deletedAllMessages: " + msgDB.getMessagesCount());
        db.deleteContact(contact);

        WATCH_BUSY = false;
        Log.d(TAG, "onDeleteContactLOADING LIST");
        onLoadingList();
        val_position = -1;
    }

    public class MyAsyncTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params){
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
            String sender = prefs.getString(LoginActivity.PHONENUMBER_KEY, "");
            String result = "";
            String recipient = params[0];
            // Create a new HTTPClient and Post Header
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://192.168.1.200:8078/sunstonechat/linkCheck/");
            if( sender.equals("") ){
                return "invalidLogin";
            }

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("sender", sender));
                nameValuePairs.add(new BasicNameValuePair("recipient",recipient));
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
            Date date = new Date();
            ringProgressDialog.dismiss();
            boolean success = false;
            String reason   = "";

            if( result.equals("invalidLogin") ) {
                Toast.makeText(getApplicationContext(), "Sign in to add a contact!", Toast.LENGTH_LONG).show();
                return;
            }


            try {
                JSONObject jsonObject = new JSONObject(result);
                success  = jsonObject.getBoolean("result");
                reason   = jsonObject.getString("reason");
                Log.d(TAG, "Parsed return data: " + success + " " + reason);
            } catch (JSONException e) {
                Log.e(TAG, "Failed parsing JSON data from post response");
                // Should get a wrong password or username message here
                // Pass that info back to the user on the page

            }
            if(success){
                try{
                    JSONObject jsonObject = new JSONObject(result);
                    String key = jsonObject.getString("key");
                    val_key = key;
                    validateWatchContact();
                    return;
                } catch (JSONException e){
                    // Pass
                }
            }
            Toast toast = Toast.makeText(getApplicationContext(), reason, Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void sendScramble(){
        if(WATCH_BUSY){
            Log.d(TAG, "Watch is busy: " + WATCH_BUSY);
            return;
        }
        WATCH_BUSY = true;
        Log.d(TAG, "Send Scramble!!!");
        if(!PebbleKit.isWatchConnected(getApplicationContext())){
            final String msg = "Watch is not connected!";
            Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
            toast.show();
            WATCH_BUSY = false;
            return;
        }

        PebbleKit.startAppOnPebble(getApplicationContext(), SSCHAT_UUID);

        PebbleDictionary scrambleData = new PebbleDictionary();

        scrambleData.addString(AM_SCRAMBLE_KEY,"");

        PebbleKit.sendDataToPebble(getApplicationContext(), SSCHAT_UUID, scrambleData);

        sendScrambleFlag = true;
        WATCH_BUSY = false;
    }


    private void validateWatchContact(){

        if( WATCH_BUSY ) return;

        WATCH_BUSY = true;
        LAST_CALL_WATCH = WATCH_VALIDATE_BUSY;

        if(!PebbleKit.isWatchConnected(getApplicationContext())){
            Toast toast = Toast.makeText(getApplicationContext(), "Watch isn't connected!", Toast.LENGTH_LONG);
            toast.show();
            WATCH_BUSY = false;
            return;
        }

        if(val_num == null || val_key == null){
            Log.e(TAG, "Something wrong with the validate strings");
            WATCH_BUSY = false;
            return;
        }


        PebbleKit.startAppOnPebble(getApplicationContext(), SSCHAT_UUID);

        PebbleDictionary contact_data = new PebbleDictionary();

        contact_data.addString(AM_VERIFY_CONTACT, val_num + "|" + val_key);

        PebbleKit.sendDataToPebble(getApplicationContext(), SSCHAT_UUID, contact_data);
        WATCH_BUSY = false;
    }


}
